$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot

Write-Host "Checking backend-java"
if (Get-Command mvn -ErrorAction SilentlyContinue) {
  Push-Location "$root\backend-java"
  mvn test
  Pop-Location
} else {
  Write-Host "mvn not found, skipped Java tests"
}

Write-Host "Checking agent-python"
if (Get-Command python -ErrorAction SilentlyContinue) {
  Push-Location "$root\agent-python"
  python -m pytest
  Pop-Location
} else {
  Write-Host "python not found, skipped Python tests"
}

Write-Host "Checking frontend-vue"
if (Get-Command npm -ErrorAction SilentlyContinue) {
  Push-Location "$root\frontend-vue"
  npm run typecheck
  Pop-Location
} else {
  Write-Host "npm not found, skipped frontend typecheck"
}

