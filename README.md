# MinIO Storage POC

Project root: `D:\STC\minio-poc`

## Build & run (two steps)

### 1. Build the Spring Boot JAR (local Maven — JDK 17+)

```powershell
cd minio-poc\demo-api
mvn -DskipTests package
```

Or: `.\scripts\build-api.ps1`

Docker **does not** run Maven. It only copies `demo-api/target/minio-demo-api-1.0.0.jar`.

### 2. Start stack

```powershell
cd minio-poc
Copy-Item .env.example .env
docker compose up -d --build
```

---

## Port layout (POC)

| Port | Service | Notes |
|------|---------|-------|
| **80** | nginx → MinIO S3 API | Public object URLs / presigned URLs use this |
| **8080** | demo-api | Presign endpoints |
| **8088** | demo-ui | Web demo |
| **9010** | MinIO S3 API (direct) | Console login + admin API (`MINIO_API_PORT`) |
| **9001** | MinIO Console | Web UI (`MINIO_CONSOLE_PORT`) |

Host **9000** is intentionally unused (often occupied on Windows). Console needs the API on **9010**.

If **9001** is taken:

```env
MINIO_CONSOLE_PORT=19001
```

---

## Storage: local folder (POC) vs EBS (EKS)

**POC** — bind mount to your project folder:

```yaml
volumes:
  - ./data/minio:/data
```

Files land in `minio-poc/data/minio/` on your machine. No EBS, no Docker named volume.

**EKS** — swap the mount only:

```yaml
# docker-compose (POC)
- ./data/minio:/data

# Kubernetes (EKS)
volumeMounts:
  - name: minio-data
    mountPath: /data
volumes:
  - name: minio-data
    persistentVolumeClaim:
      claimName: minio-data-pvc   # backed by EBS gp3
```

Same MinIO image and `/data` path; only the volume source changes.

---

## URLs

| What | URL |
|------|-----|
| Console | http://localhost:9001 (`demo-admin` / `demo-admin-secret`) |
| S3 via Ingress sim | http://localhost/dobox-dev-bucket/... |
| Demo API | http://localhost:8080 |
| Demo UI | http://localhost:8088 |
| Public seed file | http://localhost/dobox-dev-bucket/public/welcome.txt |

---

## API

```http
POST /v1/public/minio/upload|download|delete
POST /v1/private/minio/download|upload|delete
```

Body:

```json
{ "objectKey": "demo.txt", "contentType": "text/plain", "expirySeconds": 3600 }
```

Demo script: `.\scripts\demo.ps1`

**Full API reference + cURLs:** [`api/README.md`](api/README.md)

---

## Reset data

```powershell
docker compose down
Remove-Item -Recurse -Force .\data\minio\*
docker compose up -d
```

---

## Troubleshooting

| Error | Fix |
|-------|-----|
| Console: `unable to login due to network error` / 503 on `/api/v1/login` | Fixed via `MINIO_SERVER_URL=http://host.docker.internal:9010` (Console runs inside container; `localhost:9010` is not reachable there). Hard-refresh browser. |
| `port 9000 not available` | Default API host port is **9010**, not 9000 |
| `port 9001 not available` | Set `MINIO_CONSOLE_PORT=19001` in `.env` |
| Docker build: JAR not found | Run `mvn package` in `demo-api` first |
| Presign signature mismatch | Use `localhost` consistently (not `127.0.0.1`) in `MINIO_PUBLIC_URL` |
