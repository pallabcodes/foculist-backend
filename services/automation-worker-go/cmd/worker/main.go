package main

import (
	"context"
	"log"
	"os"
	"os/signal"
	"syscall"
	"time"
	"encoding/json"

	amqp "github.com/rabbitmq/amqp091-go"
	"github.com/yourorg/foculist/automation-worker-go/internal/worker"
)

func main() {
	registry := worker.NewRegistry()
	registry.Register("task.enrichment", &worker.TaskEnrichmentHandler{})

	rabbitURL := os.Getenv("RABBITMQ_URL")
	if rabbitURL == "" {
		rabbitURL = "amqp://guest:guest@localhost:5672/"
	}

	queueName := os.Getenv("QUEUE_NAME")
	if queueName == "" {
		queueName = "foculist.automation.jobs"
	}

	log.Printf("Starting automation-worker; queue=%s, url=%s", queueName, rabbitURL)

	// Keep-alive connection setup
	conn, err := connectWithRetry(rabbitURL)
	if err != nil {
		log.Fatalf("Failed to connect to RabbitMQ: %v", err)
	}
	defer conn.Close()

	ch, err := conn.Channel()
	if err != nil {
		log.Fatalf("Failed to open a channel: %v", err)
	}
	defer ch.Close()

	q, err := ch.QueueDeclare(
		queueName,
		true,  // durable
		false, // delete when unused
		false, // exclusive
		false, // no-wait
		nil,   // arguments
	)
	if err != nil {
		log.Fatalf("Failed to declare a queue: %v", err)
	}

	err = ch.Qos(
		5,     // prefetch count
		0,     // prefetch size
		false, // global
	)
	if err != nil {
		log.Fatalf("Failed to set QoS: %v", err)
	}

	msgs, err := ch.Consume(
		q.Name,
		"automation-worker", // consumer
		false,             // auto-ack
		false,             // exclusive
		false,             // no-local
		false,             // no-wait
		nil,               // args
	)
	if err != nil {
		log.Fatalf("Failed to register a consumer: %v", err)
	}

	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()

	log.Println(" [*] Waiting for messages. To exit press CTRL+C")

	go func() {
		for d := range msgs {
			log.Printf("Received message")

			var job worker.Job
			if err := json.Unmarshal(d.Body, &job); err != nil {
				log.Printf("Error unmarshaling job: %v", err)
				d.Nack(false, false) // Reject, don't requeue if malformed
				continue
			}

			handler, ok := registry.Get(job.Type)
			if !ok {
				log.Printf("No handler for job type: %s", job.Type)
				d.Ack(false) // Ack to remove from queue if no handler
				continue
			}

			if err := handler.Handle(ctx, job); err != nil {
				log.Printf("Error handling job %s: %v", job.ID, err)
				d.Nack(false, true) // Nack and requeue once for retry
				continue
			}

			d.Ack(false)
		}
	}()

	<-ctx.Done()
	log.Println("Shutting down gracefully...")
}

func connectWithRetry(url string) (*amqp.Connection, error) {
	var counts int
	for {
		conn, err := amqp.Dial(url)
		if err != nil {
			log.Printf("RabbitMQ not ready... retrying (%d): %v", counts, err)
			counts++
			if counts > 10 {
				return nil, err
			}
			time.Sleep(5 * time.Second)
			continue
		}
		return conn, nil
	}
}
