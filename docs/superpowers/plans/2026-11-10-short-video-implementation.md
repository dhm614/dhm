# 短视频模块实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为宠趣社区增加经过管理员审核的 MP4 短视频上传、列表、在线播放、互动和后台管理功能。

**Architecture:** 短视频保存为 `post` 表中的 `VIDEO` 类型内容，复用现有点赞、收藏、评论和举报表。普通社区接口固定查询 `POST` 类型，视频接口固定查询 `VIDEO` 类型；本地上传继续通过 Spring MVC 静态资源映射提供 HTTP Range 播放。

**Tech Stack:** Java 17、Spring Boot 3.5.3、MyBatis-Plus、MySQL 8、JUnit 5、Mockito、HTML5 Video、原生 JavaScript、Apache Tomcat 7 静态前端。

**Spec:** `docs/superpowers/specs/2026-11-10-short-video-design.md`

## Global Constraints

- 页面文本使用中文。
- 短视频只允许 `.mp4`，单文件最大 `100MB`。
- 不实现视频转码、压缩、时长解析、HLS 或推荐算法。
- 视频提交后状态强制为 `0`，审核通过状态为 `1`，删除或下架状态为 `2`。
- 普通帖子列表、话题帖子列表和帖子搜索不得返回短视频。
- 后台接口必须通过 `X-User-Id` 校验管理员身份。
- 点赞、收藏、评论和举报继续使用视频对应的 `post.id`。
- 修改数据库结构时同步更新主建表脚本和独立迁移脚本。
- 修改前端后同步部署到 `D:\apache-tomcat-7.0.109\webapps\petcommunity`。
- 当前目录不是 Git 工作区，每个任务以测试通过和文件检查作为检查点，不执行 Git commit。

---

## 文件结构

### 新建文件

- `src/main/java/com/example/petcommunity3/dto/ShortVideoCreateDTO.java`：短视频发布请求。
- `src/main/java/com/example/petcommunity3/service/ShortVideoService.java`：短视频公开业务接口。
- `src/main/java/com/example/petcommunity3/service/impl/ShortVideoServiceImpl.java`：视频校验、创建、公开列表和详情。
- `src/main/java/com/example/petcommunity3/controller/ShortVideoController.java`：`/api/videos` 接口。
- `src/test/java/com/example/petcommunity3/service/impl/ShortVideoServiceTest.java`：视频创建和公开查询测试。
- `src/test/java/com/example/petcommunity3/service/impl/ShortVideoAdminTest.java`：管理员审核、删除、恢复和普通帖子隔离测试。
- `src/test/java/com/example/petcommunity3/service/impl/ShortVideoUploadTest.java`：MP4 格式和大小限制测试。
- `src/main/resources/sql/short_video_module.sql`：现有数据库幂等迁移脚本。
- `frontend-tomcat/petcommunity/videos.html`：短视频封面列表页。
- `frontend-tomcat/petcommunity/video-publish.html`：短视频上传页。
- `frontend-tomcat/petcommunity/video-detail.html`：短视频播放与互动页。
- `frontend-tomcat/petcommunity/tests/verify-short-video-flow.ps1`：前端结构和调用检查。

### 修改文件

- `src/main/java/com/example/petcommunity3/entity/Post.java`：增加 `contentType`、`videoUrl`。
- `src/main/java/com/example/petcommunity3/service/impl/PostServiceImpl.java`：普通帖子固定为 `POST` 并过滤视频。
- `src/main/java/com/example/petcommunity3/mapper/PostMapper.java`：全文搜索只查普通帖子并返回新增字段。
- `src/main/java/com/example/petcommunity3/service/impl/SearchServiceImpl.java`：LIKE 回退只查普通帖子。
- `src/main/java/com/example/petcommunity3/service/AdminService.java`：增加视频管理方法。
- `src/main/java/com/example/petcommunity3/service/impl/AdminServiceImpl.java`：视频审核、删除、恢复和通知。
- `src/main/java/com/example/petcommunity3/controller/AdminController.java`：增加管理员视频接口。
- `src/main/java/com/example/petcommunity3/service/impl/FileStorageServiceImpl.java`：模块级 MP4、封面和 100MB 校验。
- `src/main/resources/application.yml`：Multipart 上传上限。
- `src/main/resources/sql/pet_community_schema.sql`：主建表字段和索引。
- `frontend-tomcat/petcommunity/assets/app.js`：视频页面逻辑和后台操作。
- `frontend-tomcat/petcommunity/assets/styles.css`：视频卡片、播放器和上传布局。
- `frontend-tomcat/petcommunity/admin.html`：短视频审核管理区域。
- `frontend-tomcat/petcommunity/admin.html`
- `frontend-tomcat/petcommunity/checkout.html`
- `frontend-tomcat/petcommunity/community.html`
- `frontend-tomcat/petcommunity/community-publish.html`
- `frontend-tomcat/petcommunity/community-topic-apply.html`
- `frontend-tomcat/petcommunity/dashboard.html`
- `frontend-tomcat/petcommunity/home.html`
- `frontend-tomcat/petcommunity/merchant-apply.html`
- `frontend-tomcat/petcommunity/merchant-product-create.html`
- `frontend-tomcat/petcommunity/merchant-products.html`
- `frontend-tomcat/petcommunity/pets.html`
- `frontend-tomcat/petcommunity/post-detail.html`
- `frontend-tomcat/petcommunity/profile.html`
- `frontend-tomcat/petcommunity/services.html`
- `frontend-tomcat/petcommunity/shop.html`

以上 15 个现有公共页面均增加“短视频”导航，新建的三个视频页面也使用同一导航结构。
- `frontend-tomcat/petcommunity/tests/verify-pages.ps1`：登记三个新页面。

---

### Task 1: 扩展帖子模型并隔离普通帖子查询

**Files:**
- Modify: `src/main/java/com/example/petcommunity3/entity/Post.java`
- Modify: `src/main/java/com/example/petcommunity3/service/impl/PostServiceImpl.java`
- Modify: `src/main/java/com/example/petcommunity3/mapper/PostMapper.java`
- Modify: `src/main/java/com/example/petcommunity3/service/impl/SearchServiceImpl.java`
- Modify: `src/main/resources/sql/pet_community_schema.sql`
- Create: `src/main/resources/sql/short_video_module.sql`
- Modify: `src/test/java/com/example/petcommunity3/service/impl/PostAuditFlowTest.java`
- Modify: `src/test/java/com/example/petcommunity3/service/impl/SearchFullTextTest.java`

**Interfaces:**
- Produces: `Post.getContentType()/setContentType(String)` 和 `Post.getVideoUrl()/setVideoUrl(String)`。
- Produces: 内容类型常量值 `POST` 与 `VIDEO`，数据库默认值为 `POST`。
- Guarantees: `/api/posts`、话题帖子和帖子搜索只返回 `POST`。

- [ ] **Step 1: 为普通帖子类型和查询隔离编写失败测试**

在 `PostAuditFlowTest` 增加创建普通帖子时类型固定为 `POST` 的断言：

```java
assertThat(postCaptor.getValue().getContentType()).isEqualTo("POST");
assertThat(postCaptor.getValue().getVideoUrl()).isNull();
```

增加列表查询包装器测试，捕获 `postMapper.selectList()` 的 `LambdaQueryWrapper<Post>`，断言参数包含 `1` 和 `POST`：

```java
service.list();
verify(postMapper).selectList(wrapperCaptor.capture());
assertThat(wrapperCaptor.getValue().getParamNameValuePairs()).containsValue(1).containsValue("POST");
```

在 `SearchFullTextTest` 保留全文搜索调用测试，并通过方法注解反射检查 SQL：

```java
Select select = PostMapper.class
        .getMethod("searchApprovedByFullText", String.class, int.class)
        .getAnnotation(Select.class);
assertThat(String.join(" ", select.value())).contains("content_type = 'POST'");
```

LIKE 回退测试让 `searchApprovedByFullText()` 返回空列表，捕获随后 `selectList()` 的查询包装器，并断言参数值包含 `POST`。

- [ ] **Step 2: 运行定向测试并确认失败**

```powershell
$env:JAVA_HOME='C:\Users\31334\.jdks\ms-17.0.19'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
& 'C:\Users\31334\.m2\wrapper\dists\apache-maven-3.9.12\59fe215c0ad6947fea90184bf7add084544567b927287592651fda3782e0e798\bin\mvn.cmd' -q '-Dtest=PostAuditFlowTest,SearchFullTextTest' test
```

Expected: FAIL，因为 `Post` 还没有内容类型字段，查询也没有类型过滤。

- [ ] **Step 3: 扩展实体和普通帖子业务**

在 `Post` 增加：

```java
private String contentType;
private String videoUrl;
```

在 `PostServiceImpl.create()` 中强制：

```java
post.setContentType("POST");
post.setVideoUrl(null);
```

在 `list()`、`getDetail()` 和 `listByTopic()` 中增加 `contentType = POST` 条件。`getDetail()` 对 `VIDEO` 返回“帖子不存在”。

在 `SearchServiceImpl.searchPostsByLike()` 增加：

```java
.eq(Post::getContentType, "POST")
```

在 `PostMapper.searchApprovedByFullText()` 的字段列表增加 `content_type,video_url`，WHERE 增加：

```sql
AND content_type = 'POST'
```

- [ ] **Step 4: 更新主建表和幂等迁移脚本**

主建表 `post` 增加：

```sql
content_type VARCHAR(20) NOT NULL DEFAULT 'POST' COMMENT 'POST普通帖子 VIDEO短视频',
video_url VARCHAR(500) DEFAULT NULL COMMENT '短视频MP4地址',
KEY idx_post_content_status_time (content_type, status, create_time),
```

`short_video_module.sql` 使用 `information_schema.columns` 和 `information_schema.statistics` 分别判断字段及索引是否存在，再通过 `PREPARE/EXECUTE` 幂等添加。

- [ ] **Step 5: 运行定向测试并确认通过**

运行 Step 2 命令。Expected: PASS。

- [ ] **Step 6: 检查点**

```powershell
rg -n "contentType|videoUrl|content_type = 'POST'|idx_post_content_status_time" src/main/java src/main/resources/sql src/test
```

确认普通帖子创建、列表、详情、话题和搜索都明确隔离视频。

---

### Task 2: 实现短视频发布、公开列表和详情接口

**Files:**
- Create: `src/main/java/com/example/petcommunity3/dto/ShortVideoCreateDTO.java`
- Create: `src/main/java/com/example/petcommunity3/service/ShortVideoService.java`
- Create: `src/main/java/com/example/petcommunity3/service/impl/ShortVideoServiceImpl.java`
- Create: `src/main/java/com/example/petcommunity3/controller/ShortVideoController.java`
- Create: `src/test/java/com/example/petcommunity3/service/impl/ShortVideoServiceTest.java`

**Interfaces:**
- Consumes: `Post.contentType`, `Post.videoUrl` 和现有 `PostMapper`、`TopicMapper`、`SensitiveWordService`。
- Produces: `void ShortVideoService.create(ShortVideoCreateDTO dto)`。
- Produces: `List<Post> ShortVideoService.list(Integer limit)`，limit 默认 8、最大 12。
- Produces: `Post ShortVideoService.getDetail(Long id)`。
- Produces: REST `POST /api/videos`、`GET /api/videos?limit=8`、`GET /api/videos/{id}`。

- [ ] **Step 1: 编写短视频业务失败测试**

`ShortVideoServiceTest` 使用 Mockito 创建 `PostMapper`、`TopicMapper` 和 `SensitiveWordService`。覆盖：

```java
@Test
void createVideoStoresPendingVideoPost() {
    ShortVideoCreateDTO dto = validDto();
    service.create(dto);
    verify(postMapper).insert(postCaptor.capture());
    assertThat(postCaptor.getValue().getContentType()).isEqualTo("VIDEO");
    assertThat(postCaptor.getValue().getVideoUrl()).isEqualTo("/uploads/short-video/demo.mp4");
    assertThat(postCaptor.getValue().getStatus()).isZero();
}
```

另写测试确认缺少标题、简介、封面、视频 URL、用户 ID 时抛出中文错误；视频 URL 不是 `.mp4` 时拒绝；话题存在时必须查询 `status = 1`；列表包装器包含 `VIDEO` 和 `status = 1`；详情拒绝待审核视频及普通帖子。

- [ ] **Step 2: 运行测试并确认失败**

```powershell
$env:JAVA_HOME='C:\Users\31334\.jdks\ms-17.0.19'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
& 'C:\Users\31334\.m2\wrapper\dists\apache-maven-3.9.12\59fe215c0ad6947fea90184bf7add084544567b927287592651fda3782e0e798\bin\mvn.cmd' -q -Dtest=ShortVideoServiceTest test
```

Expected: FAIL，因为短视频 DTO、Service 和 Controller 尚不存在。

- [ ] **Step 3: 创建 DTO 和 Service 接口**

`ShortVideoCreateDTO` 字段：

```java
private Long userId;
private String title;
private String content;
private String cover;
private String videoUrl;
private String topic;
```

`ShortVideoService` 精确签名：

```java
void create(ShortVideoCreateDTO dto);
List<Post> list(Integer limit);
Post getDetail(Long id);
```

- [ ] **Step 4: 实现最小视频业务**

`ShortVideoServiceImpl.create()` 执行以下顺序：校验 DTO 和必填字段、校验 URL 以 `.mp4` 结尾、校验已通过话题、调用 `sensitiveWordService.checkPost(title, content)`、构造 `Post`。

固定字段：

```java
post.setContentType("VIDEO");
post.setLikeCount(0);
post.setCommentCount(0);
post.setCollectCount(0);
post.setIsPinned(0);
post.setIsFeatured(0);
post.setStatus(0);
```

视频不会修改 `topic.post_count`，该字段继续表示普通帖子数量。

`list(limit)` 查询 `contentType = VIDEO AND status = 1`，按 `createTime DESC`，使用 `LIMIT` 将数量限制在 1 到 12，默认 8。

`getDetail(id)` 仅接受 `contentType = VIDEO AND status = 1`。

- [ ] **Step 5: 创建 REST Controller**

```java
@RestController
@RequestMapping("/api/videos")
public class ShortVideoController {
    @PostMapping
    public Result<Void> create(@RequestBody ShortVideoCreateDTO dto) { ... }

    @GetMapping
    public Result<List<Post>> list(@RequestParam(defaultValue = "8") Integer limit) { ... }

    @GetMapping("/{id}")
    public Result<Post> detail(@PathVariable Long id) { ... }
}
```

- [ ] **Step 6: 运行测试并确认通过**

运行 Step 2 命令。Expected: PASS。

- [ ] **Step 7: 编译检查**

```powershell
$env:JAVA_HOME='C:\Users\31334\.jdks\ms-17.0.19'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
& 'C:\Users\31334\.m2\wrapper\dists\apache-maven-3.9.12\59fe215c0ad6947fea90184bf7add084544567b927287592651fda3782e0e798\bin\mvn.cmd' -q -DskipTests compile
```

Expected: BUILD SUCCESS。

---

### Task 3: 增加管理员短视频审核、删除和恢复

**Files:**
- Modify: `src/main/java/com/example/petcommunity3/service/AdminService.java`
- Modify: `src/main/java/com/example/petcommunity3/service/impl/AdminServiceImpl.java`
- Modify: `src/main/java/com/example/petcommunity3/controller/AdminController.java`
- Create: `src/test/java/com/example/petcommunity3/service/impl/ShortVideoAdminTest.java`
- Modify: `src/test/java/com/example/petcommunity3/controller/AdminControllerAuthorizationTest.java`

**Interfaces:**
- Produces: `List<Post> AdminService.listVideos(Integer status, String keyword)`。
- Produces: `void AdminService.reviewVideo(Long id, StatusUpdateDTO dto)`。
- Produces: `void AdminService.deleteVideo(Long id)`。
- Produces: `void AdminService.restoreVideo(Long id)`。
- Produces: `/api/admin/audit/videos`、`/api/admin/audit/videos/{id}/review`、`/api/admin/videos/{id}/delete`、`/api/admin/videos/{id}/restore`。

- [ ] **Step 1: 编写管理员业务失败测试**

`ShortVideoAdminTest` 覆盖：

- `listPosts(null, null)` 的包装器包含 `contentType = POST`，普通审核列表不出现视频。
- `listVideos(null, null)` 默认查询 `status = 0` 和 `contentType = VIDEO`。
- `reviewVideo(id, status=1)` 将视频状态更新为 1。
- `reviewVideo` 对普通帖子抛出“短视频不存在”。
- `deleteVideo` 将状态设为 2，并清除置顶、精选和置顶时间。
- `restoreVideo` 将状态设为 1。

`AdminControllerAuthorizationTest` 增加无 `X-User-Id` 调用 `GET /api/admin/audit/videos` 时，在调用 `listVideos` 前被拒绝的测试。

- [ ] **Step 2: 运行测试并确认失败**

```powershell
$env:JAVA_HOME='C:\Users\31334\.jdks\ms-17.0.19'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
& 'C:\Users\31334\.m2\wrapper\dists\apache-maven-3.9.12\59fe215c0ad6947fea90184bf7add084544567b927287592651fda3782e0e798\bin\mvn.cmd' -q '-Dtest=ShortVideoAdminTest,AdminControllerAuthorizationTest' test
```

Expected: FAIL，因为管理员视频接口不存在。

- [ ] **Step 3: 实现 AdminService 视频方法和普通帖子隔离**

`listPosts()` 增加 `contentType = POST`。

`listVideos()` 使用：

```java
wrapper.eq(Post::getContentType, "VIDEO");
wrapper.eq(Post::getStatus, status == null ? STATUS_PENDING : status);
```

关键词匹配标题、简介和话题。新增私有 `getVideo(id)`，必须验证 `contentType = VIDEO`。

审核只允许状态 1 或 2；删除固定状态 2；恢复固定状态 1。通知文本使用“短视频”，目标类型仍为 `POST`。

- [ ] **Step 4: 增加管理员 Controller 路由**

所有新路由第一步调用现有 `requireAdmin(operatorId)`：

```java
@GetMapping("/audit/videos")
@PutMapping("/audit/videos/{id}/review")
@PutMapping("/videos/{id}/delete")
@PutMapping("/videos/{id}/restore")
```

- [ ] **Step 5: 运行测试并确认通过**

运行 Step 2 命令。Expected: PASS。

- [ ] **Step 6: 运行帖子审核回归测试**

```powershell
$env:JAVA_HOME='C:\Users\31334\.jdks\ms-17.0.19'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
& 'C:\Users\31334\.m2\wrapper\dists\apache-maven-3.9.12\59fe215c0ad6947fea90184bf7add084544567b927287592651fda3782e0e798\bin\mvn.cmd' -q '-Dtest=PostAuditFlowTest,PostOperationManagementTest,ShortVideoAdminTest' test
```

Expected: PASS。

---

### Task 4: 限制 MP4 上传并配置 100MB 请求

**Files:**
- Modify: `src/main/java/com/example/petcommunity3/service/impl/FileStorageServiceImpl.java`
- Modify: `src/main/resources/application.yml`
- Create: `src/test/java/com/example/petcommunity3/service/impl/ShortVideoUploadTest.java`

**Interfaces:**
- Consumes: `FileStorageService.upload(MultipartFile file, String module)`。
- Guarantees: `module=short-video` 只接受 `.mp4` 且 `size <= 104857600`。
- Guarantees: `module=short-video-cover` 只接受图片扩展名。
- Preserves: 现有图片、商品、商户申请文档和 MinIO 行为。

- [ ] **Step 1: 编写上传校验失败测试**

`ShortVideoUploadTest` 覆盖：

```java
@Test
void shortVideoAcceptsMp4() {
    MockMultipartFile file = new MockMultipartFile("file", "demo.mp4", "video/mp4", new byte[]{1, 2, 3});
    assertThat(service.upload(file, "short-video").getUrl()).startsWith("/uploads/short-video/");
}

@Test
void shortVideoRejectsMov() {
    MockMultipartFile file = new MockMultipartFile("file", "demo.mov", "video/quicktime", new byte[]{1});
    assertThatThrownBy(() -> service.upload(file, "short-video")).hasMessage("短视频仅支持MP4格式");
}
```

超限测试使用 Mockito 模拟 `MultipartFile.getSize()` 返回 `104857601L`，并断言服务在调用 `getBytes()` 前抛出“短视频不能超过100MB”。封面模块用 `.mp4` 时应抛出“短视频封面必须是图片”。

- [ ] **Step 2: 运行测试并确认失败**

```powershell
$env:JAVA_HOME='C:\Users\31334\.jdks\ms-17.0.19'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
& 'C:\Users\31334\.m2\wrapper\dists\apache-maven-3.9.12\59fe215c0ad6947fea90184bf7add084544567b927287592651fda3782e0e798\bin\mvn.cmd' -q '-Dtest=ShortVideoUploadTest,FileStorageThumbnailTest' test
```

Expected: FAIL，因为当前 `short-video` 仍接受 MOV/AVI 且没有大小限制。

- [ ] **Step 3: 在读取文件内容前增加模块级校验**

在 `FileStorageServiceImpl` 增加：

```java
private static final long SHORT_VIDEO_MAX_SIZE = 100L * 1024 * 1024;
private static final Set<String> IMAGE_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");
```

在 `readBytes(file)` 之前调用 `validateModuleFile(file, safeModule, extension)`：

```java
if ("short-video".equals(module)) {
    if (!".mp4".equals(extension)) throw new RuntimeException("短视频仅支持MP4格式");
    if (file.getSize() > SHORT_VIDEO_MAX_SIZE) throw new RuntimeException("短视频不能超过100MB");
}
if ("short-video-cover".equals(module) && !IMAGE_EXTENSIONS.contains(extension)) {
    throw new RuntimeException("短视频封面必须是图片");
}
```

- [ ] **Step 4: 配置 Spring Multipart 上限**

在 `application.yml` 的 `spring` 下增加：

```yaml
servlet:
  multipart:
    max-file-size: 100MB
    max-request-size: 110MB
```

- [ ] **Step 5: 运行测试并确认通过**

运行 Step 2 命令。Expected: PASS。

- [ ] **Step 6: 检查现有上传回归**

```powershell
$env:JAVA_HOME='C:\Users\31334\.jdks\ms-17.0.19'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
& 'C:\Users\31334\.m2\wrapper\dists\apache-maven-3.9.12\59fe215c0ad6947fea90184bf7add084544567b927287592651fda3782e0e798\bin\mvn.cmd' -q '-Dtest=FileStorageThumbnailTest,MerchantApplicationFlowTest,ShortVideoUploadTest' test
```

Expected: PASS。

---

### Task 5: 实现短视频前端和后台管理界面

**Files:**
- Create: `frontend-tomcat/petcommunity/videos.html`
- Create: `frontend-tomcat/petcommunity/video-publish.html`
- Create: `frontend-tomcat/petcommunity/video-detail.html`
- Modify: `frontend-tomcat/petcommunity/admin.html`
- Modify: `frontend-tomcat/petcommunity/assets/app.js`
- Modify: `frontend-tomcat/petcommunity/assets/styles.css`
- Modify: 所有包含 `<nav class="top-nav">` 的前端 HTML 页面
- Create: `frontend-tomcat/petcommunity/tests/verify-short-video-flow.ps1`
- Modify: `frontend-tomcat/petcommunity/tests/verify-pages.ps1`

**Interfaces:**
- Consumes: `/api/videos`、`/api/topics`、`/api/files/upload`、现有帖子互动接口和举报接口。
- Consumes: 管理员视频审核、删除和恢复接口。
- Produces: 页面标识 `videos`、`video-publish`、`video-detail`。
- Produces: 前台地址 `http://localhost:8081/petcommunity/videos.html`。

- [ ] **Step 1: 编写前端失败检查脚本**

`verify-short-video-flow.ps1` 读取 UTF-8 文件并断言：

- 三个页面存在。
- `videos.html` 包含 `data-page="videos"`、`id="videoList"` 和 `href="video-publish.html"`。
- `video-publish.html` 包含标题、简介、封面文件、`accept="video/mp4,.mp4"` 的视频文件、话题选择区域和 `id="videoForm"`。
- `video-detail.html` 包含 `data-page="video-detail"`、`id="videoDetail"`、`id="videoCommentTree"`。
- `admin.html` 包含 `id="auditVideoRows"`。
- `app.js` 包含 `initVideosPage`、`initVideoPublishPage`、`initVideoDetailPage`、`createShortVideo`、`loadAdminVideos`。
- `app.js` 调用 `/api/videos`、`/api/post-interactions/likes`、`/api/post-interactions/collects`、`/api/reports` 和评论树接口。
- `app.js` 生成 `<video controls preload="metadata"`。
- 所有公共导航页面包含 `data-nav-page="videos" href="videos.html"`。

- [ ] **Step 2: 运行静态检查并确认失败**

```powershell
powershell -ExecutionPolicy Bypass -File frontend-tomcat\petcommunity\tests\verify-short-video-flow.ps1
```

Expected: FAIL，因为页面和脚本尚不存在。

- [ ] **Step 3: 创建三个页面**

`videos.html` 只提供页面标题、上传入口、通知区域和：

```html
<section class="video-grid" id="videoList"></section>
```

`video-publish.html` 使用一个表单，输入名精确为 `title`、`content`、`cover`、`videoUrl`、`topic`。封面文件 ID 为 `videoCoverFile`，视频文件 ID 为 `shortVideoFile`，话题区域 ID 为 `videoTopicChoices`。

`video-detail.html` 提供返回视频列表按钮、`videoDetail` 和 `videoCommentTree` 容器。

- [ ] **Step 4: 扩展页面初始化和导航**

在 `app.js` 中：

```javascript
if (page === "video-publish" || page === "video-detail") {
    navPage = "videos";
}
```

增加三个初始化分支。上传页绑定 `videoForm` 并加载已有话题；详情页从查询参数读取 `id`。

所有公共导航在“社区”后增加：

```html
<a data-nav-page="videos" href="videos.html">短视频</a>
```

- [ ] **Step 5: 实现列表、上传和详情逻辑**

列表调用 `GET /api/videos?limit=8`，卡片只输出封面和标题，点击跳转：

```javascript
"video-detail.html?id=" + video.id
```

上传流程按顺序调用：

```javascript
uploadFile("videoCoverFile", "short-video-cover")
uploadFile("shortVideoFile", "short-video")
request("/api/videos", {method: "POST", body: payload})
```

详情调用视频接口与评论树接口，渲染：

```html
<video controls preload="metadata" poster="..." src="..."></video>
```

播放器下方按钮依次为点赞、收藏、举报；评论区使用现有评论提交接口和树形评论渲染。举报请求保持：

```javascript
{targetType: "POST", targetId: video.id, userId: userId(), reason: reason}
```

- [ ] **Step 6: 增加后台视频表格和操作**

`admin.html` 增加“短视频审核与管理”表格。`loadAdminData()` 调用 `loadAdminVideos()`。

后台默认加载待审核、已通过、已删除三种状态并合并展示。按钮调用：

```text
PUT /api/admin/audit/videos/{id}/review  body {status: 1}
PUT /api/admin/videos/{id}/delete
PUT /api/admin/videos/{id}/restore
```

成功后重新加载视频列表，并在页面通知区域显示中文结果。

- [ ] **Step 7: 增加视频样式**

在 `styles.css` 增加固定尺寸视频网格、`aspect-ratio: 16 / 9` 的封面、最大宽度播放器和移动端单列。卡片圆角不超过 8px，不嵌套卡片，播放器不拉伸原视频比例。

- [ ] **Step 8: 运行前端检查**

```powershell
& 'C:\Users\31334\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' --check 'frontend-tomcat\petcommunity\assets\app.js'
powershell -ExecutionPolicy Bypass -File frontend-tomcat\petcommunity\tests\verify-short-video-flow.ps1
powershell -ExecutionPolicy Bypass -File frontend-tomcat\petcommunity\tests\verify-pages.ps1
powershell -ExecutionPolicy Bypass -File frontend-tomcat\petcommunity\tests\verify-community-page-flow.ps1
```

Expected: 全部 PASS。

---

### Task 6: 应用迁移、全量验证、部署并检查 HTTP Range

**Files:**
- Execute: `src/main/resources/sql/short_video_module.sql`
- Deploy: `frontend-tomcat/petcommunity` 到 Tomcat webapps。
- Runtime artifact: `uploads/short-video/range-test.mp4`，验证后删除。

**Interfaces:**
- Consumes: Tasks 1-5 的数据库、后端和前端成果。
- Produces: 可访问的前端和运行中的后端。

- [ ] **Step 1: 运行全部后端测试**

```powershell
$env:JAVA_HOME='C:\Users\31334\.jdks\ms-17.0.19'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
& 'C:\Users\31334\.m2\wrapper\dists\apache-maven-3.9.12\59fe215c0ad6947fea90184bf7add084544567b927287592651fda3782e0e798\bin\mvn.cmd' -q test
```

Expected: BUILD SUCCESS，无失败测试。

- [ ] **Step 2: 应用幂等数据库迁移**

```powershell
cmd /c ""C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -uroot -p123456 --default-character-set=utf8mb4 pet_community < "D:\ideawork\petcommunity3\src\main\resources\sql\short_video_module.sql""
```

查询确认：

```powershell
& 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe' -uroot -p123456 --default-character-set=utf8mb4 -D pet_community -e "SHOW COLUMNS FROM post LIKE 'content_type'; SHOW COLUMNS FROM post LIKE 'video_url'; SHOW INDEX FROM post WHERE Key_name='idx_post_content_status_time';"
```

Expected: 两个字段和一个索引均存在，现有帖子 `content_type` 为 `POST`。

- [ ] **Step 3: 重启 8082 后端**

先用 `netstat -ano | Select-String ':8082'` 找到现有 PID，只停止确认属于当前 Java 后端的进程。随后运行：

```powershell
$env:JAVA_HOME='C:\Users\31334\.jdks\ms-17.0.19'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
& 'C:\Users\31334\.m2\wrapper\dists\apache-maven-3.9.12\59fe215c0ad6947fea90184bf7add084544567b927287592651fda3782e0e798\bin\mvn.cmd' spring-boot:run
```

Expected: 8082 监听且 `/api/videos` 返回统一成功结构。

- [ ] **Step 4: 同步前端到 Tomcat**

将 `frontend-tomcat\petcommunity` 下发生变更的 HTML、`assets/app.js` 和 `assets/styles.css` 复制到：

```text
D:\apache-tomcat-7.0.109\webapps\petcommunity
```

若 8081 未监听，运行：

```powershell
cmd /c D:\apache-tomcat-7.0.109\bin\startup.bat
```

- [ ] **Step 5: 验证页面和接口可访问**

```powershell
(Invoke-WebRequest -UseBasicParsing 'http://localhost:8081/petcommunity/videos.html').StatusCode
(Invoke-WebRequest -UseBasicParsing 'http://localhost:8081/petcommunity/video-publish.html').StatusCode
(Invoke-WebRequest -UseBasicParsing 'http://localhost:8081/petcommunity/video-detail.html?id=1').StatusCode
(Invoke-WebRequest -UseBasicParsing 'http://localhost:8082/api/videos').StatusCode
```

Expected: 全部返回 200。

- [ ] **Step 6: 验证本地上传静态资源支持 HTTP Range**

在项目的 `uploads\short-video` 中创建一个明确命名的临时二进制文件 `range-test.mp4`，内容不少于 32 字节。请求：

```powershell
curl.exe -i -H "Range: bytes=0-9" http://localhost:8082/uploads/short-video/range-test.mp4
```

Expected: HTTP `206 Partial Content`，响应包含 `Content-Range: bytes 0-9/...` 和长度 10。验证后只删除 `uploads\short-video\range-test.mp4`，不递归删除目录。

- [ ] **Step 7: 最终回归检查**

```powershell
& 'C:\Users\31334\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' --check 'frontend-tomcat\petcommunity\assets\app.js'
powershell -ExecutionPolicy Bypass -File frontend-tomcat\petcommunity\tests\verify-short-video-flow.ps1
powershell -ExecutionPolicy Bypass -File frontend-tomcat\petcommunity\tests\verify-pages.ps1
powershell -ExecutionPolicy Bypass -File frontend-tomcat\petcommunity\tests\verify-audit-permissions.ps1
powershell -ExecutionPolicy Bypass -File frontend-tomcat\petcommunity\tests\verify-community-page-flow.ps1
```

Expected: 全部通过。最终提供测试地址：`http://localhost:8081/petcommunity/videos.html`。
