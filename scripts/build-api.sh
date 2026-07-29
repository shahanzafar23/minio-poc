#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../demo-api"
mvn -DskipTests package
echo "JAR ready: demo-api/target/minio-demo-api-1.0.0.jar"
