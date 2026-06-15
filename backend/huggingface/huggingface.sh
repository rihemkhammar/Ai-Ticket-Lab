# https://huggingface.co/models?inference_provider=hf-inference&sort=trending - Find models provided by the shared api - use reference_provider=hf-inference and...

PROMPT="summarize: In today's Q3 planning meeting, the team discussed improving both technical reliability and user experience for our mobile payment solution. E"

# brew install jq
# Using jq to construct the JSON payload safely. jq is a lightweight command-line tool for processing, filtering, and manipulating JSON data. It allows you to co

# Final payload
JSON_PAYLOAD=$(jq -n --arg inputs "$PROMPT" '{
  inputs: $inputs,
  parameters: { max_new_tokens: 20, temperature: 0.7 },
  options: { wait_for_model: true }
}')

echo "Request payload:"
echo "$JSON_PAYLOAD" | jq .

# https://huggingface.co/facebook/bart-large-cnn?inference_provider=hf-inference
MODEL="facebook/bart-large-cnn"

# Call the shared Inference API
curl https://router.huggingface.co/hf-inference/models/$MODEL \
  -H "Authorization: Bearer $HUGGINGFACE_API_KEY" \
  -H "Content-Type: application/json" \
  -d "$JSON_PAYLOAD"