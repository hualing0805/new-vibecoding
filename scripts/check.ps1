$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot

Write-Host "Checking backend-java"
$maven = Get-Command mvn -ErrorAction SilentlyContinue
$localMaven = Join-Path $root ".tools\apache-maven\bin\mvn.cmd"
if ($maven) {
  Push-Location "$root\backend-java"
  & $maven.Source test
  Pop-Location
} elseif (Test-Path $localMaven) {
  Push-Location "$root\backend-java"
  & $localMaven test
  Pop-Location
} else {
  Write-Host "Maven not found, skipped Java tests"
}

Write-Host "Checking agent-python"
$venvPython = Join-Path $root "agent-python\.venv\Scripts\python.exe"
$python = Get-Command python -ErrorAction SilentlyContinue
$pyLauncher = Get-Command py -ErrorAction SilentlyContinue
if (Test-Path $venvPython) {
  Push-Location "$root\agent-python"
  & $venvPython -m pytest
  Pop-Location
} elseif ($python) {
  Push-Location "$root\agent-python"
  & $python.Source -m pytest
  Pop-Location
} elseif ($pyLauncher) {
  $pyVersions = & $pyLauncher.Source -0p 2>$null
  if ($LASTEXITCODE -eq 0 -and $pyVersions) {
    Push-Location "$root\agent-python"
    & $pyLauncher.Source -3 -m pytest
    Pop-Location
  } else {
    Write-Host "Python not found, skipped Python tests"
  }
} else {
  Write-Host "Python not found, skipped Python tests"
}

Write-Host "Checking frontend-vue"
$npm = Get-Command npm.cmd -ErrorAction SilentlyContinue
if (-not $npm) {
  $npm = Get-Command npm -ErrorAction SilentlyContinue
}
if ($npm) {
  Push-Location "$root\frontend-vue"
  & $npm.Source run typecheck
  Pop-Location
} else {
  Write-Host "npm not found, skipped frontend typecheck"
}
