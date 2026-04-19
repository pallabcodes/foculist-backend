# --- Build Stage ---
FROM python:3.11-slim-bullseye as builder
WORKDIR /app
COPY requirements.txt .
RUN pip install --user --no-cache-dir -r requirements.txt

# --- Final Production Stage ---
FROM python:3.11-slim-bullseye
WORKDIR /app

# Non-root security best practice
RUN useradd -m foculist
USER foculist
ENV PATH="/home/foculist/.local/bin:${PATH}"

COPY --from=builder /home/foculist/.local /home/foculist/.local
COPY . .

ENV PYTHONUNBUFFERED=1
ENV RABBITMQ_HOST=rabbitmq
ENV AI_PROXY_URL=http://ollama-mock:11434/api/generate

ENTRYPOINT ["python", "app.py"]
