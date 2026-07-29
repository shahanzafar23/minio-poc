# MinIO Storage POC — Share / Onboarding Email

Use this as the body of your email (adjust To/Subject as needed).

---

**Subject:** MinIO Storage POC — S3-Compatible Object Storage (Docker + Spring Boot Demo)

Hi,

Please find our **MinIO Storage POC** for evaluating S3-compatible object storage (public/private buckets, presigned URLs, and API integration):

**Repository:** https://github.com/shahanzafar23/minio-poc

---

## What it includes

- **MinIO** — S3-compatible object storage (same API model as AWS S3)
- **Spring Boot demo API** — presigned upload/download/delete (mirrors our `cs-attachments` pattern)
- **Demo Web UI** — upload & download flows at http://localhost:8088
- **nginx** — simulates future EKS Ingress routing
- **Local persistence** — files stored under `./data/minio` on disk (POC); EKS uses EBS PVC (see below)

---

## How to run (quick steps)

**Prerequisites:** Docker Desktop, JDK 17+, Maven

```powershell
# 1. Clone
git clone https://github.com/shahanzafar23/minio-poc.git
cd minio-poc

# 2. Config
copy .env.example .env

# 3. Build API (Maven runs locally — not inside Docker)
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17.0.7"   # adjust path
cd demo-api
mvn -DskipTests package
cd ..

# 4. Start stack
docker compose up -d --build
```

**URLs after startup:**

| Service | URL |
|---------|-----|
| Demo UI | http://localhost:8088 |
| Demo API | http://localhost:8080 |
| MinIO Console | http://localhost:9001 (login: `demo-admin` / `demo-admin-secret`) |
| Public file example | http://localhost/dobox-dev-bucket/public/welcome.txt |

Full details: see `README.md` and `api/README.md` in the repo.

---

## Key APIs (demo-api)

Base URL: **`http://localhost:8080`**

| Action | Method | Endpoint |
|--------|--------|----------|
| Health | GET | `/health` |
| Upload (public) | POST | `/v1/public/minio/upload` → returns presigned **PUT** URL |
| Upload (private) | POST | `/v1/private/minio/upload` → returns presigned **PUT** URL |
| Download (public) | POST | `/v1/public/minio/download` → presigned **GET** URL |
| Download (private) | POST | `/v1/private/minio/download` → presigned **GET** URL |
| Delete | POST | `/v1/public/minio/delete` or `/v1/private/minio/delete` |

**Direct download (public files only, no API):**

```http
GET http://localhost/dobox-dev-bucket/public/{filename}
```

**Example — upload then download (public):**

```bash
# Get presigned upload URL
curl -s -X POST http://localhost:8080/v1/public/minio/upload \
  -H "Content-Type: application/json" \
  -d '{"objectKey":"demo.txt","contentType":"text/plain"}'

# PUT file to presignedUrl from response, then:
curl http://localhost/dobox-dev-bucket/public/demo.txt
```

More cURL examples: **`api/README.md`**

---

## S3 SDK compatibility

MinIO implements the **Amazon S3 API**. Our production **`cs-attachments`** service already uses:

- AWS SDK v1 — `AmazonS3`, `GeneratePresignedUrlRequest`, custom endpoint
- Bucket: `dobox-dev-bucket`, path-style URLs

This POC uses **AWS SDK v2** (`software.amazon.awssdk:s3`) with the same concepts:

- Custom endpoint (`minio.endpoint` / `MINIO_ENDPOINT`)
- Path-style access (`pathStyleAccessEnabled(true)`)
- Presigned PUT / GET / DELETE
- Public URL via Ingress/nginx (`MINIO_PUBLIC_URL` → later `https://minio.hubplatform.com.sa`)

**Migration path:** Change endpoint + credentials in config — **no rewrite** of attachment flow logic (unlike Firebase/GCS).

---

## EKS & persistence — important consideration

| Environment | Storage |
|-------------|---------|
| **POC (Docker)** | Local folder bind mount: `./data/minio` |
| **EKS (production-like)** | **PersistentVolumeClaim (EBS gp3)** mounted at `/data` |

**Con:** On EKS, MinIO pod storage is **not free or automatic** — you must provision and manage **EBS volumes** (or MinIO distributed mode with multiple PVCs for HA). Pod restarts without PVC **lose data**.

**Alternative on EKS:** Skip self-hosted MinIO and use **managed S3** (AWS S3, Cloudflare R2) — no EBS for the storage layer, but vendor cost and less control.

---

## Why MinIO vs other options

| Option | S3 / AWS SDK compatible | Presigned URLs | Public + private | Self-host on EKS | Fit for `cs-attachments` |
|--------|-------------------------|----------------|------------------|-------------------|-------------------------|
| **MinIO** | Yes | Yes | Yes (bucket policy + prefixes) | Yes (+ EBS PVC) | **Best match** |
| **AWS S3** | Native | Yes | Yes | N/A (managed) | **Best match** (already similar) |
| **Cloudflare R2** | S3-compatible | Yes | Yes | N/A (managed) | Good match |
| **Firebase Cloud Storage** | No (GCS/Firebase SDK) | GCS signed URLs | Firebase rules | No | **Requires rewrite** |
| **Azure Blob** | No (Azure SDK) | SAS URLs | Yes | Partial | **Requires rewrite** |

**Why MinIO is beneficial for us:**

1. **Drop-in for existing code** — same S3 API and presigned URL model as `cs-attachments`
2. **Public + private in one bucket** — `public/` and `private/` prefixes (matches our design)
3. **On-prem / EKS control** — data stays in our cluster (with EBS), Ingress for public URLs
4. **POC → prod path** — Docker POC today; same env vars (`MINIO_PUBLIC_URL`, bucket name) on EKS Ingress later
5. **No vendor lock-in to Firebase/GCP** auth and security rules model

**Firebase / GCS downside:** Different API, Firebase Security Rules instead of bucket policies, GCS signed URLs instead of S3 presigned URLs — **not compatible** with current `AmazonS3` integration without a full service rewrite.

---

## Summary

This POC validates object storage for attachments using **MinIO + S3 SDK + presigned URLs**, aligned with our existing **`cs-attachments`** architecture. Recommended next step: run the POC, test public/private flows via Demo UI, then plan EKS deployment with **Ingress + EBS PVC** (or evaluate managed S3/R2 if we want to avoid operating storage on-cluster).

Regards,  
[Your Name]
