#!/usr/bin/env bash
# MinIO POC — curl demo script (Linux / macOS / Git Bash)
set -euo pipefail

API="${API:-http://localhost:8080}"
PUBLIC_BASE="${PUBLIC_BASE:-http://localhost/dobox-dev-bucket}"
KEY="${KEY:-curl-demo.txt}"

echo "=== 1. Health check ==="
curl -s "$API/health" | jq .

echo ""
echo "=== 2. Anonymous public read (seeded welcome.txt) ==="
curl -s "$PUBLIC_BASE/public/welcome.txt"
echo ""

echo ""
echo "=== 3. Presigned PUT (public/) ==="
UPLOAD_JSON=$(curl -s -X POST "$API/v1/public/minio/upload" \
  -H "Content-Type: application/json" \
  -d "{\"objectKey\":\"$KEY\",\"contentType\":\"text/plain\",\"expirySeconds\":3600}")
echo "$UPLOAD_JSON" | jq .
PUT_URL=$(echo "$UPLOAD_JSON" | jq -r .presignedUrl)
echo "hello from curl demo" | curl -s -X PUT "$PUT_URL" -H "Content-Type: text/plain" -d @-

echo ""
echo "=== 4. Anonymous GET public object ==="
curl -s "$PUBLIC_BASE/public/$KEY"
echo ""

echo ""
echo "=== 5. Presigned PUT (private/) ==="
PRIV_KEY="secret-$KEY"
PRIV_UPLOAD=$(curl -s -X POST "$API/v1/private/minio/upload" \
  -H "Content-Type: application/json" \
  -d "{\"objectKey\":\"$PRIV_KEY\",\"contentType\":\"text/plain\"}")
echo "$PRIV_UPLOAD" | jq .
PRIV_PUT=$(echo "$PRIV_UPLOAD" | jq -r .presignedUrl)
echo "private content" | curl -s -X PUT "$PRIV_PUT" -H "Content-Type: text/plain" -d @-

echo ""
echo "=== 6. Direct private URL should fail (403/404) ==="
curl -s -o /dev/null -w "HTTP %{http_code}\n" "$PUBLIC_BASE/private/$PRIV_KEY" || true

echo ""
echo "=== 7. Presigned GET (private/) ==="
PRIV_DL=$(curl -s -X POST "$API/v1/private/minio/download" \
  -H "Content-Type: application/json" \
  -d "{\"objectKey\":\"$PRIV_KEY\"}")
echo "$PRIV_DL" | jq .
curl -s "$(echo "$PRIV_DL" | jq -r .presignedUrl)"
echo ""

echo ""
echo "=== 8. Presigned DELETE (private/) ==="
PRIV_DEL=$(curl -s -X POST "$API/v1/private/minio/delete" \
  -H "Content-Type: application/json" \
  -d "{\"objectKey\":\"$PRIV_KEY\"}")
echo "$PRIV_DEL" | jq .
curl -s -X DELETE "$(echo "$PRIV_DEL" | jq -r .presignedUrl)" -w "DELETE HTTP %{http_code}\n" -o /dev/null

echo ""
echo "Done."
