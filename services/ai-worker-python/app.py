import pika
import json
import requests
import os
import time

# Configuration
RABBITMQ_HOST = os.getenv('RABBITMQ_HOST', 'localhost')
AI_PROXY_URL = os.getenv('AI_PROXY_URL', 'http://ollama-mock:11434/api/generate')
QUEUE_NAME = 'foculist.tasks.enrichment'

def process_task(ch, method, properties, body):
    try:
        task_event = json.loads(body)
        print(f"👻 Processing AI Enrichment for Task: {task_event.get('title')}")
        
        # Simulate AI logic via our proxy
        prompt = f"Enrich this task: {task_event.get('title')} with priority and milestones."
        
        response = requests.post(AI_PROXY_URL, json={
            "model": "foculist-ai",
            "prompt": prompt,
            "stream": False
        })
        
        enrichment = response.json().get('response')
        print(f"✅ AI Result: {enrichment}")
        
        # Ack the message
        ch.basic_ack(delivery_tag=method.delivery_tag)
    except Exception as e:
        print(f"❌ Error in AI worker: {e}")
        # Reject and requeue
        ch.basic_nack(delivery_tag=method.delivery_tag, requeue=True)

def start_worker():
    print(f"🚀 Starting Python AI Worker on {RABBITMQ_HOST}...")
    
    # Wait for RabbitMQ to be ready
    connection = None
    while not connection:
        try:
            connection = pika.BlockingConnection(pika.ConnectionParameters(host=RABBITMQ_HOST))
        except Exception:
            print("⏳ Waiting for RabbitMQ...")
            time.sleep(2)

    channel = connection.channel()
    channel.queue_declare(queue=QUEUE_NAME, durable=True)
    channel.basic_qos(prefetch_count=1)
    channel.basic_consume(queue=QUEUE_NAME, on_message_callback=process_task)

    print(f"🎧 Listening for events on {QUEUE_NAME}...")
    channel.start_consuming()

if __name__ == "__main__":
    start_worker()
