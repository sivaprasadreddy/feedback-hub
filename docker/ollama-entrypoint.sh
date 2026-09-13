#!/bin/sh

ollama serve &
OLLAMA_PID=$!

echo "Waiting for Ollama..."

until ollama list >/dev/null 2>&1; do
    sleep 1
done

echo "Ollama is ready."

ollama pull gemma3:270m

echo "Model gemma3:270m is ready."

wait $OLLAMA_PID
