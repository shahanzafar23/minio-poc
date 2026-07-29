# MinIO POC — Share Email (short)

**Subject:** MinIO Storage POC — S3-Compatible Object Storage

Hi,

Sharing our **MinIO Storage POC** for S3-compatible object storage (public/private files, presigned URLs):

**Repo:** https://github.com/shahanzafar23/minio-poc

---

**Run it**

```powershell
git clone https://github.com/shahanzafar23/minio-poc.git
cd minio-poc
copy .env.example .env
cd demo-api && mvn -DskipTests package && cd ..
docker compose up -d --build
```

- Demo UI: http://localhost:8088  
- API: http://localhost:8080  
- Console: http://localhost:9001 (`demo-admin` / `demo-admin-secret`)

---

**APIs**

| Action | Endpoint |
|--------|----------|
| Upload (public / private) | `POST /v1/public/minio/upload` · `POST /v1/private/minio/upload` |
| Download | `POST /v1/public/minio/download` · `POST /v1/private/minio/download` |
| Delete | `POST /v1/public/minio/delete` · `POST /v1/private/minio/delete` |
| Direct GET (public only) | `GET http://localhost/dobox-dev-bucket/public/{file}` |

cURL examples: `api/README.md`

---

**S3 SDK:** MinIO uses the Amazon S3 API — compatible with our `cs-attachments` service (AWS SDK + presigned URLs). Config change only; no rewrite like Firebase/GCS.

**EKS note:** POC stores data in `./data/minio` locally. On EKS we need **EBS PVC** for persistence (pods alone lose data).

**Why MinIO:** S3-compatible, public + private prefixes, presigned URLs, fits existing attachment code. Firebase/Azure need different SDKs and a service rewrite.

Regards,  
[Your Name]
