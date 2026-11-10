#!/bin/sh

ollama serve &
OLLAMA_PID=$!

echo "Waiting for Ollama..."

until ollama list >/dev/null 2>&1; do
    sleep 1
done

echo "Ollama is ready."

ollama pull qwen3:8b

echo "Model qwen3:8b is ready."

wait $OLLAMA_PID
