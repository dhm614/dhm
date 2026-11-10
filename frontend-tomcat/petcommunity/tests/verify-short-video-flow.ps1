$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$utf8 = [System.Text.Encoding]::UTF8

function Read-Utf8([string]$Path) {
    [System.IO.File]::ReadAllText($Path, $utf8)
}

function Assert-Markers([string]$Name, [string[]]$Markers) {
    $path = Join-Path $root $Name
    if (-not (Test-Path -LiteralPath $path)) {
        throw "Missing file: $Name"
    }
    $content = Read-Utf8 $path
    foreach ($marker in $Markers) {
        if (-not $content.Contains($marker)) {
            throw "$Name missing marker: $marker"
        }
    }
}

function Assert-Contains([string]$Content, [string]$Marker, [string]$Message) {
    if (-not $Content.Contains($Marker)) {
        throw $Message
    }
}

function Assert-NotContains([string]$Content, [string[]]$Markers, [string]$Message) {
    foreach ($marker in $Markers) {
        if ($Content.Contains($marker)) {
            throw "$Message Found: $marker"
        }
    }
}

function Assert-Ordered([string]$Content, [string[]]$Markers, [string]$Message) {
    $position = -1
    foreach ($marker in $Markers) {
        $next = $Content.IndexOf($marker, $position + 1, [System.StringComparison]::Ordinal)
        if ($next -lt 0) {
            throw "$Message Missing or out of order: $marker"
        }
        $position = $next
    }
}

function Get-Section([string]$Content, [string]$Start, [string]$End) {
    $startIndex = $Content.IndexOf($Start, [System.StringComparison]::Ordinal)
    $endIndex = $Content.IndexOf($End, $startIndex + $Start.Length, [System.StringComparison]::Ordinal)
    if ($startIndex -lt 0 -or $endIndex -lt 0) {
        throw "Cannot locate section: $Start"
    }
    return $Content.Substring($startIndex, $endIndex - $startIndex)
}

Assert-Markers 'videos.html' @(
    'data-page="videos"',
    'id="videoList"',
    'href="video-publish.html"'
)
Assert-Markers 'video-publish.html' @(
    'data-page="video-publish"',
    'id="videoForm"',
    'name="title"',
    'name="content"',
    'id="videoCoverFile"',
    'name="cover"',
    'id="shortVideoFile"',
    'accept="video/mp4,.mp4"',
    'name="videoUrl"',
    'name="topic"',
    'id="videoTopicChoices"'
)
Assert-Markers 'video-detail.html' @(
    'data-page="video-detail"',
    'id="videoDetail"',
    'id="videoCommentTree"'
)
Assert-Markers 'admin.html' @('id="auditVideoRows"')
Assert-Markers 'assets\app.js' @(
    'initVideosPage',
    'initVideoPublishPage',
    'initVideoDetailPage',
    'createShortVideo',
    'loadAdminVideos',
    '/api/videos',
    '/api/post-interactions/likes',
    '/api/post-interactions/collects',
    '/api/reports',
    '/api/post-interactions/comments/tree',
    '<video controls preload="metadata"',
    '/api/admin/audit/videos/',
    '/api/admin/videos/'
)

$app = Read-Utf8 (Join-Path $root 'assets\app.js')
$styles = Read-Utf8 (Join-Path $root 'assets\styles.css')
$videoCard = Get-Section $app 'function videoCardHtml(video)' 'function loadSelectableVideoTopics()'
$createVideo = Get-Section $app 'function createShortVideo(form)' 'function searchPosts()'

Assert-Contains $videoCard 'text(img(video.cover))' 'Video list cover URL must be escaped for the HTML attribute.'
Assert-Ordered $videoCard @(
    '<article class="video-card"><a href="video-detail.html?id=',
    '<img src="',
    '<h2>'
) 'Video list card must contain only the linked cover followed by the title.'
Assert-NotContains $videoCard @(
    '<video',
    'video.content',
    'likeCount',
    'commentCount',
    'collectCount',
    'data-video-action',
    '<p'
) 'Video list card contains detail or action markup.'

Assert-Ordered $createVideo @(
    'uploadFile("videoCoverFile", "short-video-cover")',
    'form.elements.cover.value = coverFile.url;',
    'uploadFile("shortVideoFile", "short-video")',
    'form.elements.videoUrl.value = videoFile.url;',
    'request("/api/videos", {method: "POST", body: payload})'
) 'Short video publishing steps must keep cover upload, video upload, and create request in order.'

$videoButtons = Get-Section $app 'function bindVideoDetailButtons(videoId)' 'function submitVideoComment(form)'
$videoComment = Get-Section $app 'function submitVideoComment(form)' 'function commentTreeHtml(comment)'
$adminVideoAuditList = Get-Section $app 'function loadAdminVideos()' 'function bindAdminVideoButtons()'
$adminVideoActions = Get-Section $app 'function bindAdminVideoButtons()' 'function loadApprovedPosts()'
$videoCardStyles = Get-Section $styles '.video-card img {' '.video-card h2 {'
$videoPlayerStyles = Get-Section $styles '.video-detail-content video {' '.video-detail-copy h2 {'

Assert-Contains $videoComment 'body: {postId: videoId, userId: userId(), content: content}' 'Video comment request body must use the explicit content variable.'
Assert-Contains $videoButtons 'report("POST", videoId);' 'Video report must keep targetType POST.'
Assert-Contains $adminVideoAuditList 'data-audit-detail="video"' 'Pending videos must open the independent audit detail page.'
Assert-Contains $adminVideoActions 'path = "/api/admin/videos/" + videoId + "/delete";' 'Admin video delete path must be explicit.'
Assert-Contains $adminVideoActions 'path = "/api/admin/videos/" + videoId + "/restore";' 'Admin video restore path must be explicit.'

Assert-Contains $videoCardStyles 'aspect-ratio: 16 / 9;' 'Video cover CSS must keep a 16:9 aspect ratio.'
Assert-Contains $videoPlayerStyles 'object-fit: contain;' 'Video player CSS must preserve the original aspect ratio.'
$mobileStart = $styles.IndexOf('@media (max-width: 560px)', [System.StringComparison]::Ordinal)
if ($mobileStart -lt 0) {
    throw 'Missing mobile media query.'
}
$mobileStyles = $styles.Substring($mobileStart)
Assert-Ordered $mobileStyles @(
    '.video-grid {',
    'grid-template-columns: minmax(0, 1fr);'
) 'Video grid must switch to one column on mobile.'

$navPages = @(Get-ChildItem -LiteralPath $root -Filter '*.html' -File | Where-Object {
    (Read-Utf8 $_.FullName).Contains('<nav class="top-nav"')
})
if ($navPages.Count -eq 0) {
    throw 'No public navigation pages found.'
}
foreach ($page in $navPages) {
    if (-not (Read-Utf8 $page.FullName).Contains('data-nav-page="videos" href="videos.html"')) {
        throw "Navigation link missing from $($page.Name)"
    }
}

Write-Host 'Short video frontend flow checks passed.'
