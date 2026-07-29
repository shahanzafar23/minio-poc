# Build demo-api JAR (run before docker compose build)
# Requires JDK 17+

Set-Location $PSScriptRoot\..\demo-api
mvn -DskipTests package
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host "JAR ready: demo-api/target/minio-demo-api-1.0.0.jar" -ForegroundColor Green
