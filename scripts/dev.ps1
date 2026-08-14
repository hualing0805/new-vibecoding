$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot

Write-Host "ThreadMind local development"
Write-Host "Quick stack scripts:"
Write-Host "  .\scripts\start-local-stack.ps1"
Write-Host "  .\scripts\verify-local-flow.ps1"
Write-Host "  .\scripts\stop-local-stack.ps1"

Write-Host ""
Write-Host "Manual commands:"
Write-Host "  cd $root\infra; docker compose up -d"
Write-Host "  cd $root\backend-java; ..\.tools\apache-maven\bin\mvn.cmd spring-boot:run"
Write-Host "  cd $root\agent-python; python -m venv .venv; .\.venv\Scripts\Activate.ps1; pip install -e .[dev]; uvicorn app.main:app --reload --port 8000"
Write-Host "  cd $root\frontend-vue; npm install; npm.cmd run dev"
