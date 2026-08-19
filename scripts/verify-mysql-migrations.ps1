param(
  [string] $Url = $env:THREADMIND_MYSQL_IT_URL,
  [string] $Username = $env:THREADMIND_MYSQL_IT_USERNAME,
  [string] $Password = $env:THREADMIND_MYSQL_IT_PASSWORD
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot

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

if (-not $Url) {
  $Url = "jdbc:mysql://localhost:3306/threadmind_migration_test?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false"
}
if (-not $Username) {
  $Username = "threadmind_test"
}
if (-not $Password) {
  $Password = "threadmind_test"
}

$env:THREADMIND_MYSQL_IT_URL = $Url
$env:THREADMIND_MYSQL_IT_USERNAME = $Username
$env:THREADMIND_MYSQL_IT_PASSWORD = $Password

$maven = Find-Maven
if (-not $maven) {
  throw "Maven not found. Install Maven or keep .tools\apache-maven available."
}

Write-Host "Running MySQL/Flyway migration integration test"
Write-Host "Target: $env:THREADMIND_MYSQL_IT_URL"
Write-Host "The Java test cleans the target schema and refuses database names that do not contain 'test'."

Push-Location (Join-Path $root "backend-java")
try {
  & $maven -Pmysql-migration-test test
  if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
  }
} finally {
  Pop-Location
}
