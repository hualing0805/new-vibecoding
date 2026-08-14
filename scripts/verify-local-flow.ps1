param(
  [string] $BaseUrl = "http://localhost:8080/api",
  [string] $DemoUsername = "demo",
  [string] $DemoPassword = "ThreadMind@123"
)

$ErrorActionPreference = "Stop"

function Invoke-Api {
  param(
    [string] $Method,
    [string] $Path,
    [object] $Body = $null,
    [string] $Token = ""
  )

  $headers = @{}
  if ($Token) {
    $headers.Authorization = "Bearer $Token"
  }
  $uri = "$BaseUrl$Path"
  $params = @{
    Method = $Method
    Uri = $uri
    Headers = $headers
  }
  if ($null -ne $Body) {
    $params.ContentType = "application/json"
    $params.Body = ($Body | ConvertTo-Json -Depth 8)
  }

  try {
    return Invoke-RestMethod @params
  } catch {
    Write-Host "Request failed: $Method $uri"
    if ($_.Exception.Response) {
      Write-Host "Status: $([int] $_.Exception.Response.StatusCode)"
    }
    throw
  }
}

function Assert-Ok {
  param(
    [object] $Response,
    [string] $Step
  )
  if (-not $Response -or $Response.code -ne "OK") {
    throw "$Step failed. Expected code OK."
  }
  Write-Host "OK - $Step"
}

Write-Host "ThreadMind local flow verification"
Write-Host "BaseUrl: $BaseUrl"

$health = Invoke-Api -Method "GET" -Path "/health"
Assert-Ok -Response $health -Step "backend health"

$adminLogin = Invoke-Api -Method "POST" -Path "/auth/login" -Body @{
  username = $DemoUsername
  password = $DemoPassword
}
Assert-Ok -Response $adminLogin -Step "demo admin login"
$adminToken = $adminLogin.data.token

$suffix = Get-Random -Minimum 100000 -Maximum 999999
$username = "m3_$suffix"
$password = "ThreadMind@123"

$register = Invoke-Api -Method "POST" -Path "/auth/register" -Body @{
  username = $username
  email = "$username@threadmind.local"
  password = $password
  nickname = "M3 Verification User $suffix"
}
Assert-Ok -Response $register -Step "register verification user"
$userToken = $register.data.token

$me = Invoke-Api -Method "GET" -Path "/users/me" -Token $userToken
Assert-Ok -Response $me -Step "current user with bearer token"

$categories = Invoke-Api -Method "GET" -Path "/categories"
Assert-Ok -Response $categories -Step "list categories"
if (-not $categories.data -or $categories.data.Count -eq 0) {
  throw "No categories returned."
}
$categoryId = $categories.data[0].id

$topic = Invoke-Api -Method "POST" -Path "/topics" -Token $userToken -Body @{
  categoryId = $categoryId
  title = "M3 local verification $suffix"
  content = "This topic is created by scripts/verify-local-flow.ps1 for local verification."
  tagNames = @("M3", "verify")
}
Assert-Ok -Response $topic -Step "create topic"
$topicId = $topic.data.id

$detail = Invoke-Api -Method "GET" -Path "/topics/$topicId"
Assert-Ok -Response $detail -Step "get created topic"

$reply = Invoke-Api -Method "POST" -Path "/topics/$topicId/replies" -Token $userToken -Body @{
  content = "This reply is created by the local verification script."
}
Assert-Ok -Response $reply -Step "create reply"

$like = Invoke-Api -Method "POST" -Path "/topics/$topicId/like" -Token $userToken
Assert-Ok -Response $like -Step "like topic"

$status = Invoke-Api -Method "GET" -Path "/topics/$topicId/interaction-status" -Token $userToken
Assert-Ok -Response $status -Step "get topic interaction status"
if (-not $status.data.liked) {
  throw "Topic should be liked after like operation."
}

$unlike = Invoke-Api -Method "DELETE" -Path "/topics/$topicId/like" -Token $userToken
Assert-Ok -Response $unlike -Step "unlike topic"

$favorite = Invoke-Api -Method "POST" -Path "/topics/$topicId/favorite" -Token $userToken
Assert-Ok -Response $favorite -Step "favorite topic"

$favorites = Invoke-Api -Method "GET" -Path "/users/me/favorites" -Token $userToken
Assert-Ok -Response $favorites -Step "list favorite topics"

$unfavorite = Invoke-Api -Method "DELETE" -Path "/topics/$topicId/favorite" -Token $userToken
Assert-Ok -Response $unfavorite -Step "unfavorite topic"

$notifications = Invoke-Api -Method "GET" -Path "/notifications" -Token $userToken
Assert-Ok -Response $notifications -Step "list current user notifications"

$unread = Invoke-Api -Method "GET" -Path "/notifications/unread-count" -Token $userToken
Assert-Ok -Response $unread -Step "get unread notification count"

$notificationItems = @($notifications.data)
if ($notificationItems.Count -gt 0) {
  $read = Invoke-Api -Method "POST" -Path "/notifications/$($notificationItems[0].id)/read" -Token $userToken
  Assert-Ok -Response $read -Step "mark notification read"
}

$report = Invoke-Api -Method "POST" -Path "/reports" -Token $userToken -Body @{
  targetType = "TOPIC"
  targetId = $topicId
  reason = "M3 local verification"
  detail = "Verify report creation and admin handling flow."
}
Assert-Ok -Response $report -Step "create report"
$reportId = $report.data.id

$reports = Invoke-Api -Method "GET" -Path "/admin/reports" -Token $adminToken
Assert-Ok -Response $reports -Step "admin list reports"

$handled = Invoke-Api -Method "POST" -Path "/admin/reports/$reportId/handle" -Token $adminToken -Body @{
  action = "IGNORE"
  note = "M3 local verification handled."
}
Assert-Ok -Response $handled -Step "admin handle report"

Write-Host ""
Write-Host "Local flow verification completed."
Write-Host "Verification user: $username"
Write-Host "Created topic id: $topicId"
