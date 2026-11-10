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
$myPosts = Read-Utf8 (Join-Path $root 'my-posts.html')
$myVideos = Read-Utf8 (Join-Path $root 'my-videos.html')
$app = Read-Utf8 (Join-Path $root 'assets\\app.js')

Assert-Contains $profile 'href="my-posts.html"' 'My posts page link is missing.'
Assert-Contains $profile 'href="my-videos.html"' 'My videos page link is missing.'
if ($profile.Contains('id="ownedContentPanel"')) {
    throw 'Profile page must not render the owned content list inline.'
}
Assert-Contains $myPosts 'data-page="my-posts"' 'My posts page marker is missing.'
Assert-Contains $myPosts 'id="ownedContentList"' 'My posts list is missing.'
Assert-Contains $myVideos 'data-page="my-videos"' 'My videos page marker is missing.'
Assert-Contains $myVideos 'id="ownedContentList"' 'My videos list is missing.'
Assert-Contains $app 'function initMyPostsPage()' 'My posts page initializer is missing.'
Assert-Contains $app 'function initMyVideosPage()' 'My videos page initializer is missing.'
Assert-Contains $app 'function loadMyPosts()' 'My posts loader is missing.'
Assert-Contains $app 'function loadMyVideos()' 'My videos loader is missing.'
Assert-Contains $app 'function ownedContentStatus(status)' 'Owned content status formatter is missing.'
Assert-Contains $app 'status.audit' 'Audit status must be displayed.'
Assert-Contains $app 'status.ban' 'Ban status must be displayed.'
Assert-Contains $app 'number(item.likeCount)' 'Like count must be displayed.'
Assert-Contains $app 'number(item.collectCount)' 'Collect count must be displayed.'
Assert-Contains $app 'number(item.commentCount)' 'Comment count must be displayed.'

Write-Host 'Profile owned content checks passed.'
