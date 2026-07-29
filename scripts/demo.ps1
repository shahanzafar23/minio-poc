$ErrorActionPreference = "Stop"
$Api = "http://localhost:8080"
$PublicBase = "http://localhost/dobox-dev-bucket"
$Key = "curl-demo.txt"

Write-Host "=== Health ===" -ForegroundColor Cyan
Invoke-RestMethod "$Api/health" | ConvertTo-Json

Write-Host "`n=== Public welcome.txt ===" -ForegroundColor Cyan
(Invoke-WebRequest "$PublicBase/public/welcome.txt").Content

Write-Host "`n=== Presigned public upload ===" -ForegroundColor Cyan
$upload = Invoke-RestMethod -Method Post -Uri "$Api/v1/public/minio/upload" -ContentType "application/json" `
  -Body (@{ objectKey = $Key; contentType = "text/plain" } | ConvertTo-Json)
Invoke-RestMethod -Method Put -Uri $upload.presignedUrl -ContentType "text/plain" -Body "hello"
(Invoke-WebRequest "$PublicBase/public/$Key").Content

Write-Host "`nDone." -ForegroundColor Green
