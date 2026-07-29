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

---

**Quick comparison**

| Option | S3 / AWS SDK | Presigned URLs | Self-host EKS | Fits `cs-attachments` | Main trade-off |
|--------|:------------:|:--------------:|:-------------:|:---------------------:|----------------|
| **MinIO** | Yes | Yes | Yes (+ EBS PVC) | **Yes** | Operate storage on-cluster |
| **AWS S3** | Yes | Yes | N/A (managed) | **Yes** | AWS cost & vendor lock-in |
| **Cloudflare R2** | Yes | Yes | N/A (managed) | **Yes** | Managed vendor dependency |
| **Firebase Storage** | No | GCS signed URLs | No | **No — rewrite** | Different SDK & security model |
| **Azure Blob** | No | SAS URLs | Partial | **No — rewrite** | Different SDK & API |

**Why MinIO:** Best fit for our existing attachment code, public + private prefixes in one bucket, presigned URLs, and a clear POC → EKS path (Ingress + EBS).

Regards,  
[Your Name]
