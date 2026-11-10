$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$utf8 = [System.Text.Encoding]::UTF8

function Read-Utf8([string]$Path) {
    [System.IO.File]::ReadAllText($Path, $utf8)
}

function Assert-Contains([string]$Content, [string]$Marker, [string]$Message) {
    if (-not $Content.Contains($Marker)) {
        throw $Message
    }
}

$profile = Read-Utf8 (Join-Path $root 'profile.html')
$app = Read-Utf8 (Join-Path $root 'assets\app.js')

Assert-Contains $profile 'id="profileFollowAction"' 'Profile follow action container is missing.'
Assert-Contains $profile 'id="followingList"' 'Following list is missing.'
Assert-Contains $profile 'id="followerList"' 'Follower list is missing.'
Assert-Contains $app 'function toggleProfileFollow()' 'Profile follow toggle is missing.'
Assert-Contains $app 'function loadProfileSocialLists(' 'Profile social list loader is missing.'
Assert-Contains $app 'new URLSearchParams(window.location.search).get("userId")' 'Profile must read the target user from the URL.'
Assert-Contains $app 'request("/api/follows", {method: "POST"' 'Follow request is missing.'
Assert-Contains $app 'request("/api/follows?followerId="' 'Unfollow request is missing.'
Assert-Contains $app 'request("/api/follows/followings?userId="' 'Following list request is missing.'
Assert-Contains $app 'request("/api/follows/followers?userId="' 'Follower list request is missing.'

Write-Host 'Profile follow flow checks passed.'
