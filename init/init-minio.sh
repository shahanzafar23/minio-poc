#!/bin/sh
set -e

echo "Waiting for MinIO..."
until mc alias set local "$MINIO_ENDPOINT" "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD" 2>/dev/null; do
  sleep 2
done
echo "MinIO is ready."

mc mb "local/${MINIO_BUCKET}" --ignore-existing
mc anonymous set download "local/${MINIO_BUCKET}/public"

cat > /tmp/demo-app-policy.json <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject",
        "s3:DeleteObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::${MINIO_BUCKET}",
        "arn:aws:s3:::${MINIO_BUCKET}/*"
      ]
    }
  ]
}
EOF

mc admin user add local "$MINIO_DEMO_APP_USER" "$MINIO_DEMO_APP_SECRET" 2>/dev/null || true
mc admin policy create local demo-app-policy /tmp/demo-app-policy.json 2>/dev/null || \
  mc admin policy update local demo-app-policy /tmp/demo-app-policy.json
mc admin policy attach local demo-app-policy --user "$MINIO_DEMO_APP_USER" 2>/dev/null || true

mc admin user add local "$MINIO_DEMO_ADMIN_USER" "$MINIO_DEMO_ADMIN_SECRET" 2>/dev/null || true
mc admin policy attach local readwrite --user "$MINIO_DEMO_ADMIN_USER" 2>/dev/null || \
  mc admin policy attach local consoleAdmin --user "$MINIO_DEMO_ADMIN_USER" 2>/dev/null || true

echo "MinIO POC demo - public object" | mc pipe "local/${MINIO_BUCKET}/public/welcome.txt" 2>/dev/null || true

echo "Init complete."
echo "  Bucket:     ${MINIO_BUCKET}"
echo "  Public URL: ${MINIO_PUBLIC_URL}/${MINIO_BUCKET}/public/welcome.txt"
echo "  Data dir:   ./data/minio (host bind mount)"
