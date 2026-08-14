$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$pidDir = Join-Path $root "logs\local-stack\pids"

if (-not (Test-Path $pidDir)) {
  Write-Host "No local stack pid directory found."
  exit 0
}

Get-ChildItem -Path $pidDir -Filter "*.pid" | ForEach-Object {
  $name = [System.IO.Path]::GetFileNameWithoutExtension($_.Name)
  $rawPid = (Get-Content -Path $_.FullName -ErrorAction SilentlyContinue | Select-Object -First 1)
  if (-not $rawPid) {
    return
  }
  $processId = [int] $rawPid
  $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
  if ($process) {
    Write-Host "Stopping $name process $processId..."
    Stop-Process -Id $processId -Force
  } else {
    Write-Host "$name process $processId is not running."
  }
  Remove-Item -LiteralPath $_.FullName -Force
}

Write-Host "Local stack processes stopped."
