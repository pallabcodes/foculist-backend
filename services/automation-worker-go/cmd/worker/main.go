package main

import (
	"log"
	"os"
)

func main() {
	queue := os.Getenv("QUEUE_NAME")
	if queue == "" {
		queue = "foculist-automation-jobs"
	}
	log.Printf("automation-worker-go started; queue=%s", queue)
	log.Println("TODO: wire SQS/SNS or RabbitMQ consumer for background automations")
}
