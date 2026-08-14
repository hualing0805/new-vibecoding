$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$backend = Join-Path $root "backend-java"
$jar = Join-Path $backend "target\forum-0.1.0-SNAPSHOT.jar"
$log = Join-Path $backend "spring.process.log"

if (-not (Test-Path $jar)) {
  Write-Host "Backend jar not found at $jar"
  Write-Host "Run backend packaging first: .tools\apache-maven\bin\mvn.cmd -f backend-java\pom.xml package"
  exit 1
}

$pathValue = [Environment]::GetEnvironmentVariable("Path", "Process")
[Environment]::SetEnvironmentVariable("PATH", $null, "Process")
[Environment]::SetEnvironmentVariable("Path", $pathValue, "Process")

$javaPath = if ($env:JAVA_EXE -and (Test-Path $env:JAVA_EXE)) {
  $env:JAVA_EXE
} elseif ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME "bin\java.exe"))) {
  Join-Path $env:JAVA_HOME "bin\java.exe"
} elseif (Test-Path "D:\Java\bin\java.exe") {
  "D:\Java\bin\java.exe"
} else {
  (Get-Command java -ErrorAction Stop).Source
}

$arguments = "-jar `"$jar`" --spring.profiles.active=demo"

$process = Start-Process `
  -FilePath $javaPath `
  -ArgumentList $arguments `
  -WorkingDirectory $backend `
  -WindowStyle Hidden `
  -RedirectStandardOutput $log `
  -RedirectStandardError (Join-Path $backend "spring.process.err.log") `
  -PassThru

Write-Host "Java backend starting in demo profile. PID: $($process.Id)"
Write-Host "Logs: $log"
