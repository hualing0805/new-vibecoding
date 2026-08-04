$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot

Write-Host "ThreadMind local development"
Write-Host "1. Start MySQL from infra/docker-compose.yml"
Write-Host "2. Start Java backend on http://localhost:8080"
Write-Host "3. Start Python Agent on http://localhost:8000"
Write-Host "4. Start Vue frontend on http://localhost:5173"

Write-Host ""
Write-Host "Commands:"
Write-Host "  cd $root\infra; docker compose up -d"
Write-Host "  cd $root\backend-java; mvn spring-boot:run"
Write-Host "  cd $root\agent-python; python -m venv .venv; .\.venv\Scripts\Activate.ps1; pip install -e .[dev]; uvicorn app.main:app --reload --port 8000"
Write-Host "  cd $root\frontend-vue; npm install; npm run dev"

