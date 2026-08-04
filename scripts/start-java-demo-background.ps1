$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$backend = Join-Path $root "backend-java"
$jar = Join-Path $backend "target\forum-0.1.0-SNAPSHOT.jar"
$log = Join-Path $backend "spring.process.log"
$err = Join-Path $backend "spring.process.err.log"

if (-not (Test-Path $jar)) {
  Write-Host "Backend jar not found at $jar"
  Write-Host "Run backend packaging first: .tools\apache-maven\bin\mvn.cmd -f backend-java\pom.xml package"
  exit 1
}

$javaCommand = Get-Command java -ErrorAction Stop
$javaPath = $javaCommand.Source

$pathValue = [Environment]::GetEnvironmentVariable("Path", "Process")
[Environment]::SetEnvironmentVariable("PATH", $null, "Process")
[Environment]::SetEnvironmentVariable("Path", $pathValue, "Process")

$process = Start-Process `
  -FilePath $javaPath `
  -ArgumentList @("-jar", $jar, "--spring.profiles.active=demo") `
  -WorkingDirectory $backend `
  -WindowStyle Hidden `
  -RedirectStandardOutput $log `
  -RedirectStandardError $err `
  -PassThru

Write-Host "Java backend starting in demo profile. PID: $($process.Id)"
Write-Host "Logs: $log"
