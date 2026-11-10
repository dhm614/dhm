# Technical Requirements Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 补齐项目要求中的评论树、Redis 时间槽锁、MinIO/本地上传缩略图、MySQL 全文搜索。

**Architecture:** 在不破坏现有前端和接口的前提下补后端能力。外部组件采用可配置接入：Redis 可用时使用 Redis 锁，Redis 不可用时退回本机 JUC 锁；MinIO 配置启用时走对象存储，否则继续本地文件存储。

**Tech Stack:** Spring Boot 3.5.3、Java 17、MyBatis-Plus、MySQL FULLTEXT、Spring Data Redis、MinIO Java SDK、Java ImageIO、JUnit 5、Mockito。

**Spec:** 用户在会话中确认的 4 项技术要求。

## Global Constraints

- 页面和接口提示文本使用中文。
- 不接真实支付。
- 后端改动必须有可运行测试或编译验证。
- SQL 结构改动同步更新主 SQL 和迁移 SQL。
- 不让项目在没有 MinIO 服务时无法启动。

---

### Task 1: 评论邻接表树形输出

**Files:**
- Create: `src/main/java/com/example/petcommunity3/vo/PostCommentTreeVO.java`
- Modify: `src/main/java/com/example/petcommunity3/service/PostInteractionService.java`
- Modify: `src/main/java/com/example/petcommunity3/service/impl/PostInteractionServiceImpl.java`
- Modify: `src/main/java/com/example/petcommunity3/controller/PostInteractionController.java`
- Test: `src/test/java/com/example/petcommunity3/service/impl/PostCommentTreeTest.java`

- [ ] 写失败测试：同一帖子下的一级评论和回复应组装为 `children`。
- [ ] 实现 `listCommentTree(Long postId)`，按 `parent_id` 组树。
- [ ] 新增 `GET /api/post-interactions/comments/tree?postId=...`。
- [ ] 运行单测验证。

### Task 2: Redis 时间槽锁

**Files:**
- Modify: `pom.xml`
- Create: `src/main/java/com/example/petcommunity3/service/AppointmentSlotLockService.java`
- Create: `src/main/java/com/example/petcommunity3/service/impl/RedisAppointmentSlotLockService.java`
- Modify: `src/main/java/com/example/petcommunity3/service/impl/AppointmentServiceImpl.java`
- Test: `src/test/java/com/example/petcommunity3/service/impl/AppointmentSlotLockTest.java`

- [ ] 写失败测试：创建预约时必须通过时间槽锁包裹排班锁定逻辑。
- [ ] 引入 Spring Data Redis。
- [ ] 实现 Redis `setIfAbsent` 加锁、Lua/值校验释放，本机 JUC 锁兜底。
- [ ] 在预约创建和状态恢复有效时使用锁。
- [ ] 运行单测验证。

### Task 3: MinIO/本地上传和缩略图

**Files:**
- Modify: `pom.xml`
- Modify: `src/main/resources/application.properties`
- Modify: `src/main/java/com/example/petcommunity3/vo/UploadFileVO.java`
- Modify: `src/main/java/com/example/petcommunity3/service/impl/FileStorageServiceImpl.java`
- Test: `src/test/java/com/example/petcommunity3/service/impl/FileStorageThumbnailTest.java`

- [ ] 写失败测试：上传图片返回原图 URL 和缩略图 URL。
- [ ] 引入 MinIO Java SDK。
- [ ] 本地存储生成 `thumb_` 缩略图。
- [ ] MinIO 配置启用时上传原图和缩略图到 bucket。
- [ ] 运行单测验证。

### Task 4: 数据库全文搜索

**Files:**
- Modify: `src/main/java/com/example/petcommunity3/mapper/PostMapper.java`
- Modify: `src/main/java/com/example/petcommunity3/mapper/ProductMapper.java`
- Modify: `src/main/java/com/example/petcommunity3/service/impl/SearchServiceImpl.java`
- Modify: `src/main/resources/sql/pet_community_schema.sql`
- Modify: `src/main/resources/sql/feature_19_28.sql`
- Test: `src/test/java/com/example/petcommunity3/service/impl/SearchFullTextTest.java`

- [ ] 写失败测试：帖子和商品搜索应调用全文搜索 Mapper 方法。
- [ ] 新增 MySQL `FULLTEXT` 索引。
- [ ] Mapper 增加 `MATCH ... AGAINST` 查询。
- [ ] Service 优先使用全文搜索结果。
- [ ] 运行单测和整体编译。
