$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot

function Invoke-Checked {
  param(
    [string] $Name,
    [string] $WorkingDirectory,
    [scriptblock] $Command
  )

  Push-Location $WorkingDirectory
  try {
    & $Command
    if ($LASTEXITCODE -ne 0) {
      throw "$Name failed with exit code $LASTEXITCODE"
    }
  } finally {
    Pop-Location
  }
}

Write-Host "Checking backend-java"
$maven = Get-Command mvn -ErrorAction SilentlyContinue
$localMaven = Join-Path $root ".tools\apache-maven\bin\mvn.cmd"
if ($maven) {
  Invoke-Checked -Name "Java tests" -WorkingDirectory "$root\backend-java" -Command { & $maven.Source test }
} elseif (Test-Path $localMaven) {
  Invoke-Checked -Name "Java tests" -WorkingDirectory "$root\backend-java" -Command { & $localMaven test }
} else {
  throw "Maven not found. Install Maven or keep .tools\apache-maven available."
}

Write-Host "Checking agent-python"
$venvPython = Join-Path $root "agent-python\.venv\Scripts\python.exe"
$python = Get-Command python -ErrorAction SilentlyContinue
$pyLauncher = Get-Command py -ErrorAction SilentlyContinue
if (Test-Path $venvPython) {
  Invoke-Checked -Name "Python tests" -WorkingDirectory "$root\agent-python" -Command { & $venvPython -m pytest }
} elseif ($python) {
  Invoke-Checked -Name "Python tests" -WorkingDirectory "$root\agent-python" -Command { & $python.Source -m pytest }
} elseif ($pyLauncher) {
  $pyVersions = & $pyLauncher.Source -0p 2>$null
  if ($LASTEXITCODE -eq 0 -and $pyVersions) {
    Invoke-Checked -Name "Python tests" -WorkingDirectory "$root\agent-python" -Command { & $pyLauncher.Source -3 -m pytest }
  } else {
    throw "Python 3 not found."
  }
} else {
  throw "Python 3 not found."
}

Write-Host "Checking frontend-vue"
$npm = Get-Command npm.cmd -ErrorAction SilentlyContinue
if (-not $npm) {
  $npm = Get-Command npm -ErrorAction SilentlyContinue
}
if ($npm) {
  Invoke-Checked -Name "Frontend build" -WorkingDirectory "$root\frontend-vue" -Command { & $npm.Source run build }
} else {
  throw "npm not found. Install Node.js before running checks."
}

Write-Host "All project checks passed."
