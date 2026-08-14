$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$logDir = Join-Path $root "logs\local-stack"
$pidDir = Join-Path $logDir "pids"

New-Item -ItemType Directory -Force -Path $logDir | Out-Null
New-Item -ItemType Directory -Force -Path $pidDir | Out-Null

function Save-Pid {
  param(
    [string] $Name,
    [System.Diagnostics.Process] $Process
  )
  Set-Content -Encoding UTF8 -Path (Join-Path $pidDir "$Name.pid") -Value $Process.Id
}

function Find-Maven {
  $maven = Get-Command mvn.cmd -ErrorAction SilentlyContinue
  if ($maven) {
    return $maven.Source
  }
  $maven = Get-Command mvn -ErrorAction SilentlyContinue
  if ($maven) {
    return $maven.Source
  }
  $localMaven = Join-Path $root ".tools\apache-maven\bin\mvn.cmd"
  if (Test-Path $localMaven) {
    return $localMaven
  }
  return $null
}

function Find-Npm {
  $npm = Get-Command npm.cmd -ErrorAction SilentlyContinue
  if ($npm) {
    return $npm.Source
  }
  $npm = Get-Command npm -ErrorAction SilentlyContinue
  if ($npm) {
    return $npm.Source
  }
  return $null
}

function Find-Python {
  $venvPython = Join-Path $root "agent-python\.venv\Scripts\python.exe"
  if (Test-Path $venvPython) {
    return $venvPython
  }
  $python = Get-Command python -ErrorAction SilentlyContinue
  if ($python) {
    return $python.Source
  }
  $pyLauncher = Get-Command py -ErrorAction SilentlyContinue
  if ($pyLauncher) {
    $versions = & $pyLauncher.Source -0p 2>$null
    if ($LASTEXITCODE -eq 0 -and $versions) {
      return $pyLauncher.Source
    }
  }
  return $null
}

Write-Host "ThreadMind local stack"
Write-Host "Logs: $logDir"

$docker = Get-Command docker -ErrorAction SilentlyContinue
if ($docker) {
  Write-Host "Starting MySQL with docker compose..."
  Push-Location (Join-Path $root "infra")
  & $docker.Source compose up -d
  Pop-Location
} else {
  Write-Host "Docker not found, skipped MySQL startup. Start MySQL manually before backend."
}

$maven = Find-Maven
if ($maven) {
  Write-Host "Starting Java backend on http://localhost:8080..."
  $process = Start-Process `
    -FilePath $maven `
    -ArgumentList @("spring-boot:run") `
    -WorkingDirectory (Join-Path $root "backend-java") `
    -WindowStyle Hidden `
    -RedirectStandardOutput (Join-Path $logDir "java.out.log") `
    -RedirectStandardError (Join-Path $logDir "java.err.log") `
    -PassThru
  Save-Pid -Name "java" -Process $process
} else {
  Write-Host "Maven not found, skipped Java backend startup."
}

$python = Find-Python
if ($python) {
  Write-Host "Starting Python Agent on http://localhost:8000..."
  $pythonArgs = if ((Split-Path -Leaf $python) -eq "py.exe") {
    @("-3", "-m", "uvicorn", "app.main:app", "--reload", "--port", "8000")
  } else {
    @("-m", "uvicorn", "app.main:app", "--reload", "--port", "8000")
  }
  $process = Start-Process `
    -FilePath $python `
    -ArgumentList $pythonArgs `
    -WorkingDirectory (Join-Path $root "agent-python") `
    -WindowStyle Hidden `
    -RedirectStandardOutput (Join-Path $logDir "agent.out.log") `
    -RedirectStandardError (Join-Path $logDir "agent.err.log") `
    -PassThru
  Save-Pid -Name "agent" -Process $process
} else {
  Write-Host "Python not found, skipped Agent startup."
}

$npm = Find-Npm
if ($npm) {
  Write-Host "Starting Vue frontend on http://localhost:5173..."
  $process = Start-Process `
    -FilePath $npm `
    -ArgumentList @("run", "dev") `
    -WorkingDirectory (Join-Path $root "frontend-vue") `
    -WindowStyle Hidden `
    -RedirectStandardOutput (Join-Path $logDir "frontend.out.log") `
    -RedirectStandardError (Join-Path $logDir "frontend.err.log") `
    -PassThru
  Save-Pid -Name "frontend" -Process $process
} else {
  Write-Host "npm not found, skipped frontend startup."
}

Write-Host ""
Write-Host "Started available services. Use scripts\stop-local-stack.ps1 to stop script-managed processes."
Write-Host "Run scripts\verify-local-flow.ps1 after Java is healthy."
