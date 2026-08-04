$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$maven = Join-Path $root ".tools\apache-maven\bin\mvn.cmd"

if (-not (Test-Path $maven)) {
  Write-Host "Maven not found at $maven"
  Write-Host "Install Maven or place it under .tools\apache-maven, then run this script again."
  exit 1
}

Push-Location "$root\backend-java"
& $maven spring-boot:run "-Dspring-boot.run.profiles=demo"
Pop-Location

