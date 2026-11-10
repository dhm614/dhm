$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$community = [System.IO.File]::ReadAllText((Join-Path $root 'community.html'), [System.Text.Encoding]::UTF8)
$app = [System.IO.File]::ReadAllText((Join-Path $root 'assets\app.js'), [System.Text.Encoding]::UTF8)
$styles = [System.IO.File]::ReadAllText((Join-Path $root 'assets\styles.css'), [System.Text.Encoding]::UTF8)
$publishPath = Join-Path $root 'community-publish.html'
$topicApplyPath = Join-Path $root 'community-topic-apply.html'
$detailPath = Join-Path $root 'post-detail.html'

function Assert-Contains {
    param([string]$Content, [string]$Needle, [string]$Message)
    if (-not $Content.Contains($Needle)) {
        throw $Message
    }
}

function Assert-NotContains {
    param([string]$Content, [string]$Needle, [string]$Message)
    if ($Content.Contains($Needle)) {
        throw $Message
    }
}

if (-not (Test-Path -LiteralPath $publishPath)) {
    throw 'Missing community-publish.html.'
}
if (-not (Test-Path -LiteralPath $detailPath)) {
    throw 'Missing post-detail.html.'
}
if (-not (Test-Path -LiteralPath $topicApplyPath)) {
    throw 'Missing community-topic-apply.html.'
}

$publish = [System.IO.File]::ReadAllText($publishPath, [System.Text.Encoding]::UTF8)
$topicApply = [System.IO.File]::ReadAllText($topicApplyPath, [System.Text.Encoding]::UTF8)
$detail = [System.IO.File]::ReadAllText($detailPath, [System.Text.Encoding]::UTF8)

Assert-Contains $community 'href="community-publish.html"' 'community page needs a post publishing entry.'
Assert-Contains $community 'href="community-topic-apply.html"' 'community page needs a separate topic application entry.'
Assert-Contains $community 'data-action="searchPosts"' 'community search must use post-only search.'
Assert-Contains $community 'class="card-grid post-grid" id="postList"' 'community page needs a fixed-width post grid.'
Assert-NotContains $community 'id="postForm"' 'post form must move off the community home page.'
Assert-NotContains $community 'id="topicForm"' 'topic form must move off the community home page.'
Assert-NotContains $community 'id="searchResults"' 'search results must replace the post list instead of rendering below it.'

Assert-Contains $publish 'data-page="community-publish"' 'publish page has an invalid page marker.'
Assert-Contains $publish 'id="postForm"' 'publish page is missing the post form.'
Assert-Contains $publish 'id="selectedPostTopic" name="topic" readonly' 'publish page needs a read-only selected topic field.'
Assert-Contains $publish 'id="topicChoices"' 'publish page is missing selectable topics.'
Assert-NotContains $publish 'id="topicForm"' 'publish page must not contain the topic application form.'
Assert-Contains $topicApply 'data-page="community-topic-apply"' 'topic application page has an invalid page marker.'
Assert-Contains $topicApply 'id="topicForm"' 'topic application page is missing the topic form.'
Assert-NotContains $topicApply 'id="postForm"' 'topic application page must not contain the post form.'
Assert-Contains $detail 'data-page="post-detail"' 'post detail page has an invalid page marker.'
Assert-Contains $detail 'id="postDetail"' 'post detail page is missing the post container.'
Assert-Contains $detail 'id="postCommentTree"' 'post detail page is missing the comment tree container.'

Assert-Contains $app 'searchPosts: searchPosts' 'post search action is not registered.'
Assert-Contains $app 'function initCommunityPublishPage()' 'publish page initializer is missing.'
Assert-Contains $app 'function initCommunityTopicApplyPage()' 'topic application page initializer is missing.'
Assert-Contains $app 'selectPostTopic: selectPostTopic' 'topic selection action is not registered.'
Assert-Contains $app 'function loadSelectableTopics()' 'publish page does not load selectable topics.'
Assert-Contains $app 'function selectPostTopic(button)' 'publish page cannot select a topic.'
Assert-Contains $app 'function initPostDetailPage()' 'post detail initializer is missing.'
Assert-Contains $app 'request("/api/search/posts?keyword="' 'community search must call the post-only endpoint.'
Assert-Contains $app 'renderCards("postList", list, postCard);' 'search results must replace the main post list.'
Assert-Contains $app '"post-detail.html?id=" + post.id' 'clicking a post must open its detail page.'
Assert-Contains $app 'request("/api/post-interactions/comments/tree?postId=" + postId)' 'post detail must load the comment tree.'
Assert-Contains $styles '.post-grid {' 'community posts need a dedicated fixed-width grid.'

Write-Host 'Community page flow checks passed.'
