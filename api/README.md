# MinIO POC — API Reference & Sample cURLs

Base URL (demo-api): **`http://localhost:8080`**

Storage (via nginx / Ingress sim): **`http://localhost/dobox-dev-bucket`**

Bucket: **`dobox-dev-bucket`**

| Prefix | UI scope | Upload via UI | Download |
|--------|----------|---------------|----------|
| `public/` | Public | `POST /v1/public/minio/upload` → presigned PUT | Direct URL **or** presigned GET |
| `private/` | Private | `POST /v1/private/minio/upload` → presigned PUT | Presigned GET **only** |

---

## Download APIs (files uploaded through Demo UI)

These are **all ways to download** a file after uploading it from http://localhost:8088 .

### Summary

| # | API / URL | Method | Public files (`public/…`) | Private files (`private/…`) | Auth |
|---|-----------|--------|----------------------------|----------------------------|------|
| 1 | `http://localhost/dobox-dev-bucket/public/{objectKey}` | **GET** | Yes | No | None (anonymous) |
| 2 | `POST /v1/public/minio/download` → use `presignedUrl` | **GET** | Yes | No | Presigned (time-limited) |
| 3 | `POST /v1/private/minio/download` → use `presignedUrl` | **GET** | No | Yes | Presigned (required) |
| 4 | `publicDirectUrl` from upload response | **GET** | Yes | No | None (anonymous) |

> **UI upload mapping:** Demo UI uses `objectKey` = filename (e.g. `demo.txt`). Stored as `public/demo.txt` or `private/demo.txt`.

---

## 1. Health check

```bash
curl -s http://localhost:8080/health
```

```json
{"status":"UP"}
```

---

## 2. Upload (same flow as Demo UI)

### 2a. Public upload — `public/`

**Step 1 — Get presigned PUT URL**

```bash
curl -s -X POST http://localhost:8080/v1/public/minio/upload \
  -H "Content-Type: application/json" \
  -d '{
    "objectKey": "demo.txt",
    "contentType": "text/plain",
    "expirySeconds": 3600
  }'
```

**Sample response**

```json
{
  "presignedUrl": "http://localhost/dobox-dev-bucket/public/demo.txt?X-Amz-Algorithm=...",
  "objectKey": "public/demo.txt",
  "bucket": "dobox-dev-bucket",
  "method": "PUT",
  "expirySeconds": 3600,
  "publicDirectUrl": "http://localhost/dobox-dev-bucket/public/demo.txt",
  "note": "After upload, anonymous GET works at publicDirectUrl."
}
```

**Step 2 — Upload file (same as UI “Presigned PUT + upload”)**

```bash
curl -X PUT "<PRESIGNED_URL_FROM_STEP_1>" \
  -H "Content-Type: text/plain" \
  --data-binary "@demo.txt"
```

**Windows (PowerShell) — create test file and upload**

```powershell
"hello from UI equivalent" | Out-File -Encoding utf8 demo.txt
$upload = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/v1/public/minio/upload" `
  -ContentType "application/json" `
  -Body '{"objectKey":"demo.txt","contentType":"text/plain"}'
Invoke-RestMethod -Method Put -Uri $upload.presignedUrl -ContentType "text/plain" -InFile demo.txt
```

---

### 2b. Private upload — `private/`

**Step 1 — Get presigned PUT URL**

```bash
curl -s -X POST http://localhost:8080/v1/private/minio/upload \
  -H "Content-Type: application/json" \
  -d '{
    "objectKey": "secret-demo.txt",
    "contentType": "text/plain",
    "expirySeconds": 3600
  }'
```

**Step 2 — Upload file**

```bash
curl -X PUT "<PRESIGNED_URL_FROM_STEP_1>" \
  -H "Content-Type: text/plain" \
  --data-binary "@secret-demo.txt"
```

---

## 3. Download (all options)

### 3a. Direct anonymous GET — **public UI uploads only**

No demo-api call. Works after public upload completes.

```bash
curl -s http://localhost/dobox-dev-bucket/public/demo.txt
```

**Seeded file (from init):**

```bash
curl -s http://localhost/dobox-dev-bucket/public/welcome.txt
```

**Browser:** open `http://localhost/dobox-dev-bucket/public/demo.txt`

---

### 3b. Presigned GET — public — `POST /v1/public/minio/download`

Use when you need a **time-limited** link (same as UI “Presigned GET” with scope **public**).

```bash
curl -s -X POST http://localhost:8080/v1/public/minio/download \
  -H "Content-Type: application/json" \
  -d '{
    "objectKey": "demo.txt",
    "expirySeconds": 3600
  }'
```

**Sample response**

```json
{
  "presignedUrl": "http://localhost/dobox-dev-bucket/public/demo.txt?X-Amz-Algorithm=...",
  "objectKey": "public/demo.txt",
  "bucket": "dobox-dev-bucket",
  "method": "GET",
  "expirySeconds": 3600,
  "publicDirectUrl": "http://localhost/dobox-dev-bucket/public/demo.txt",
  "note": "Public prefix: direct URL may work without signature."
}
```

**Download using presigned URL**

```bash
curl -s "<PRESIGNED_URL_FROM_ABOVE>" -o downloaded-demo.txt
```

---

### 3c. Presigned GET — private — `POST /v1/private/minio/download`

**Required** for private UI uploads (direct URL returns 403/404).

```bash
curl -s -X POST http://localhost:8080/v1/private/minio/download \
  -H "Content-Type: application/json" \
  -d '{
    "objectKey": "secret-demo.txt",
    "expirySeconds": 3600
  }'
```

**Download using presigned URL**

```bash
curl -s "<PRESIGNED_URL_FROM_ABOVE>" -o downloaded-secret.txt
```

**Verify direct access fails (expected)**

```bash
curl -s -o /dev/null -w "HTTP %{http_code}\n" \
  http://localhost/dobox-dev-bucket/private/secret-demo.txt
```

---

## 4. Delete

### 4a. Public — presigned DELETE

```bash
curl -s -X POST http://localhost:8080/v1/public/minio/delete \
  -H "Content-Type: application/json" \
  -d '{"objectKey": "demo.txt", "expirySeconds": 3600}'
```

```bash
curl -X DELETE "<PRESIGNED_URL_FROM_ABOVE>"
```

### 4b. Private — presigned DELETE

```bash
curl -s -X POST http://localhost:8080/v1/private/minio/delete \
  -H "Content-Type: application/json" \
  -d '{"objectKey": "secret-demo.txt", "expirySeconds": 3600}'
```

```bash
curl -X DELETE "<PRESIGNED_URL_FROM_ABOVE>"
```

---

## 5. Full end-to-end scripts (match Demo UI)

### Public: upload via API → download 3 ways

```bash
KEY="ui-public-test.txt"
API="http://localhost:8080"
BASE="http://localhost/dobox-dev-bucket"

# Upload (UI equivalent: scope=public, Presigned PUT + upload)
UPLOAD=$(curl -s -X POST "$API/v1/public/minio/upload" \
  -H "Content-Type: application/json" \
  -d "{\"objectKey\":\"$KEY\",\"contentType\":\"text/plain\"}")
echo "$UPLOAD" | jq .
PUT_URL=$(echo "$UPLOAD" | jq -r .presignedUrl)
echo "uploaded via UI flow" | curl -s -X PUT "$PUT_URL" -H "Content-Type: text/plain" -d @-

echo "=== Download 1: direct anonymous GET ==="
curl -s "$BASE/public/$KEY"

echo ""
echo "=== Download 2: publicDirectUrl from upload response ==="
curl -s $(echo "$UPLOAD" | jq -r .publicDirectUrl)

echo ""
echo "=== Download 3: presigned GET ==="
DL=$(curl -s -X POST "$API/v1/public/minio/download" \
  -H "Content-Type: application/json" \
  -d "{\"objectKey\":\"$KEY\"}")
curl -s $(echo "$DL" | jq -r .presignedUrl)
echo ""
```

### Private: upload via API → download presigned only

```bash
KEY="ui-private-test.txt"
API="http://localhost:8080"
BASE="http://localhost/dobox-dev-bucket"

UPLOAD=$(curl -s -X POST "$API/v1/private/minio/upload" \
  -H "Content-Type: application/json" \
  -d "{\"objectKey\":\"$KEY\",\"contentType\":\"text/plain\"}")
PUT_URL=$(echo "$UPLOAD" | jq -r .presignedUrl)
echo "private ui content" | curl -s -X PUT "$PUT_URL" -H "Content-Type: text/plain" -d @-

echo "=== Direct GET (should fail) ==="
curl -s -o /dev/null -w "HTTP %{http_code}\n" "$BASE/private/$KEY"

echo "=== Download: presigned GET (UI equivalent) ==="
DL=$(curl -s -X POST "$API/v1/private/minio/download" \
  -H "Content-Type: application/json" \
  -d "{\"objectKey\":\"$KEY\"}")
curl -s $(echo "$DL" | jq -r .presignedUrl)
echo ""
```

---

## 6. Request / response reference

### Request body (upload, download, delete)

| Field | Required | Description |
|-------|----------|-------------|
| `objectKey` | Yes | Filename as entered in UI (e.g. `demo.txt`). API adds `public/` or `private/` prefix. |
| `contentType` | No | Recommended for upload (PUT). Example: `text/plain`, `application/pdf`. |
| `expirySeconds` | No | Presigned URL TTL. Default: private **3600** (1h), public **172800** (48h). Max **86400** (24h) for custom values. |

### Presign response fields

| Field | Description |
|-------|-------------|
| `presignedUrl` | Use with HTTP method in `method` (PUT / GET / DELETE) |
| `objectKey` | Full key in bucket (e.g. `public/demo.txt`) |
| `bucket` | `dobox-dev-bucket` |
| `method` | `PUT`, `GET`, or `DELETE` |
| `expirySeconds` | URL validity in seconds |
| `publicDirectUrl` | Anonymous GET URL (public uploads only; `null` for private) |
| `note` | Human-readable hint |

---

## 7. All demo-api endpoints (quick index)

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/health` | Health check |
| POST | `/v1/public/minio/upload` | Presigned PUT → `public/` |
| POST | `/v1/public/minio/download` | **Presigned GET** → download public UI file |
| POST | `/v1/public/minio/delete` | Presigned DELETE → `public/` |
| POST | `/v1/private/minio/upload` | Presigned PUT → `private/` |
| POST | `/v1/private/minio/download` | **Presigned GET** → download private UI file |
| POST | `/v1/private/minio/delete` | Presigned DELETE → `private/` |

**Not on demo-api (still valid download paths for public UI uploads):**

| Method | URL | Purpose |
|--------|-----|---------|
| GET | `http://localhost/dobox-dev-bucket/public/{objectKey}` | **Direct anonymous download** |

---

## 8. Configuration (.env)

```env
MINIO_PUBLIC_URL=http://localhost
MINIO_BUCKET=dobox-dev-bucket
DEMO_API_PORT=8080
```

Presigned URLs are signed for `MINIO_PUBLIC_URL`. Use `localhost` consistently (not `127.0.0.1`).
