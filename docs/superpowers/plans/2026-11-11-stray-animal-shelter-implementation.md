# 流浪宠物收留模块 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现普通用户上报、管理员审核发布、附近地图展示、用户申请收留、管理员审核申请和专业联系方式授权的完整闭环。

**Architecture:** 新增上报、公开动物和收留申请三组领域对象及 Mapper，由三个专用 Service 分别负责上报审核、公开查询和申请状态。用户接口集中在 `StrayAnimalController`，管理员接口集中在 `StrayAnimalAdminController`，复用现有管理员鉴权、文件上传和百度地图能力。

**Tech Stack:** Java 17、Spring Boot 3.5.3、MyBatis-Plus 3.5.12、MySQL 8、JUnit 5、Mockito、原生 HTML/CSS/JavaScript、百度地图 GL 4.0、Tomcat 7。

**Spec:** `docs/superpowers/specs/2026-11-11-stray-animal-shelter-design.md`

## Global Constraints

- 页面文本和错误信息使用中文。
- 用户上报经管理员审核后才能公开。
- 专业人士完整电话只对管理员和申请已通过的用户返回。
- 公开动物只包含启用且处于待救助或收留申请中的记录。
- 同步更新主建表脚本和独立迁移脚本。
- 不修改 `release/petcommunity3-submit-20261111.zip`，不重构无关模块。
- 前端完成后同步到 `D:\apache-tomcat-7.0.109\webapps\petcommunity`。

## File Map

```text
src/main/java/com/example/petcommunity3/
|-- controller/StrayAnimalController.java
|-- controller/StrayAnimalAdminController.java
|-- entity/StrayAnimalReport.java
|-- entity/StrayAnimal.java
|-- entity/StrayAdoptionApplication.java
|-- mapper/StrayAnimalReportMapper.java
|-- mapper/StrayAnimalMapper.java
|-- mapper/StrayAdoptionApplicationMapper.java
|-- service/StrayAnimalReportService.java
|-- service/StrayAnimalService.java
|-- service/StrayAdoptionService.java
|-- service/impl/StrayAnimalReportServiceImpl.java
|-- service/impl/StrayAnimalServiceImpl.java
|-- service/impl/StrayAdoptionServiceImpl.java
|-- dto/StrayAnimalReportCreateDTO.java
|-- dto/StrayAnimalReportReviewDTO.java
|-- dto/StrayAnimalUpdateDTO.java
|-- dto/StrayAdoptionCreateDTO.java
|-- dto/StrayAdoptionReviewDTO.java
|-- vo/StrayAnimalDistanceVO.java
`-- vo/StrayAnimalDetailVO.java

frontend-tomcat/petcommunity/
|-- stray-animals.html
|-- stray-report.html
|-- admin-stray-report-detail.html
`-- tests/verify-stray-*.ps1
```

---

### Task 1: 建立领域模型与 Mapper

**Files:**
- Create: three entity and three Mapper files listed in File Map
- Create: five DTO and two VO files listed in File Map
- Test: `src/test/java/com/example/petcommunity3/service/impl/StrayAnimalModelTest.java`

**Interfaces:**
- Produces entity status constants used by all later tasks.
- Produces `StrayAnimalDistanceVO { StrayAnimalDetailVO animal; Double distanceKm; }`.
- `StrayAnimalDetailVO` lists public fields explicitly and keeps `professionalPhone` nullable.

- [ ] **Step 1: Write the failing model test**

```java
@Test
void entitiesExposeStableStatusConstants() {
    assertThat(StrayAnimalReport.STATUS_PENDING).isZero();
    assertThat(StrayAnimalReport.STATUS_APPROVED).isEqualTo(1);
    assertThat(StrayAnimal.RESCUE_WAITING).isZero();
    assertThat(StrayAnimal.RESCUE_ADOPTION_PENDING).isEqualTo(1);
    assertThat(StrayAnimal.RESCUE_ADOPTED).isEqualTo(2);
    assertThat(StrayAdoptionApplication.STATUS_CANCELLED).isEqualTo(3);
}
```

- [ ] **Step 2: Run and confirm failure**

Run `mvn -q -Dtest=StrayAnimalModelTest test` with the project JDK 17 Maven command. Expected: FAIL because the entities do not exist.

- [ ] **Step 3: Implement minimal model classes**

Use `@Data`, `@TableName`, `@TableId(type = IdType.AUTO)`, `BigDecimal` coordinates and `LocalDateTime` timestamps. Define report and adoption statuses `0 pending / 1 approved / 2 rejected`, adoption cancelled `3`, and rescue statuses `0 waiting / 1 adoption pending / 2 adopted / 3 closed`.

- [ ] **Step 4: Run the model test and commit**

Run the focused Maven test, then commit the entity, mapper, DTO, VO and model test files with message `feat: 建立流浪动物领域模型`.

### Task 2: 实现用户上报与管理员审核发布

**Files:**
- Create: `service/StrayAnimalReportService.java`
- Create: `service/impl/StrayAnimalReportServiceImpl.java`
- Test: `src/test/java/com/example/petcommunity3/service/impl/StrayAnimalReportFlowTest.java`

**Interfaces:** `submit(userId, dto)`, `listMine(userId)`, `listForAdmin(status)`, `getForAdmin(id)`, `review(reportId, reviewerId, dto)`.

- [ ] **Step 1: Write failing tests**

Assert submission inserts status `0`; approval inserts a public animal with `reportId` and rescue status `0`; missing photo, invalid coordinates, rejection without remark, approval without professional phone, and a duplicate within 24 hours and 0.2 km each throw an exact Chinese error.

- [ ] **Step 2: Run `StrayAnimalReportFlowTest` and confirm failure**

Expected: FAIL because the service is absent.

- [ ] **Step 3: Implement validation and duplicate detection**

Required messages are `用户不存在或已被禁用`, `请完整填写流浪动物上报信息`, `经纬度不合法`, and `24小时内已提交过附近同类动物`. Query the same user's pending or approved records created after `now().minusHours(24)` and reject spherical distance at or below `0.2` km.

- [ ] **Step 4: Implement transactional review**

Annotate `review` with `@Transactional`. Approval requires health, danger and all professional assistance fields. Rejection requires `reviewRemark`. A processed record throws `上报记录已处理`.

- [ ] **Step 5: Run the focused test and commit**

Expected: PASS. Commit service and test files with `feat: 实现流浪动物上报审核`.

### Task 3: 实现附近动物查询与联系方式授权

**Files:**
- Create: `service/StrayAnimalService.java`
- Create: `service/impl/StrayAnimalServiceImpl.java`
- Test: `src/test/java/com/example/petcommunity3/service/impl/StrayAnimalPublicQueryTest.java`

**Interfaces:** `listNearby(longitude, latitude, limit, viewerId)`, `getDetail(id, viewerId)`, `listForAdmin(rescueStatus)`, `updateForAdmin(id, dto)`.

- [ ] **Step 1: Write failing privacy and distance tests**

Mock a far animal before a near animal and assert the result distances are sorted. Assert a normal viewer receives `professionalPhone = null`; mock one approved application and assert that applicant receives `025-86189999`.

- [ ] **Step 2: Run the test and confirm failure**

Expected: FAIL because the public query service is absent.

- [ ] **Step 3: Implement filtering and VO mapping**

Query only enabled animals with rescue status in `0, 1`. Default limit to `20` and clamp it to `1..100`. Use the Haversine calculation already used by nearby stores. Only set the phone when an approved application exists for `viewerId` and the animal.

- [ ] **Step 4: Implement admin update validation**

Allow rescue statuses `0..3`; keep health and professional fields nonblank; missing records throw `流浪动物不存在`.

- [ ] **Step 5: Run test and commit**

Expected: PASS. Commit with `feat: 实现附近流浪动物查询`.

### Task 4: 实现收留申请状态流程

**Files:**
- Create: `service/StrayAdoptionService.java`
- Create: `service/impl/StrayAdoptionServiceImpl.java`
- Test: `src/test/java/com/example/petcommunity3/service/impl/StrayAdoptionFlowTest.java`

**Interfaces:** `submit(userId, dto)`, `listMine(userId)`, `cancel(applicationId, userId)`, `listForAdmin(status)`, `review(applicationId, reviewerId, dto)`.

- [ ] **Step 1: Write failing workflow tests**

Assert submit creates status `0` and moves the animal to rescue status `1`. Assert duplicate pending/approved applications are rejected, adopted/closed animals throw `当前动物不可申请收留`, only the owner can cancel, and processed applications cannot be reviewed again.

- [ ] **Step 2: Run test and confirm failure**

Expected: FAIL because the service is absent.

- [ ] **Step 3: Implement transactional submit, cancel and review**

Require an enabled user and complete contact, living condition and reason fields. Approval leaves the animal in status `1`. On reject or cancel, count remaining pending/approved applications; when zero, restore animal status `0`. Final status `2` is set by administrator animal management after handover.

- [ ] **Step 4: Run test and commit**

Expected: PASS. Commit with `feat: 实现流浪动物收留申请`.

### Task 5: 暴露用户与管理员 REST 接口

**Files:**
- Create: `controller/StrayAnimalController.java`
- Create: `controller/StrayAnimalAdminController.java`
- Modify: `controller/FileController.java`
- Test: `controller/StrayAnimalControllerTest.java`
- Test: `controller/StrayAnimalAdminControllerAuthorizationTest.java`
- Modify test: `controller/FileControllerAuthorizationTest.java`

**Interfaces:** Produces every `/api/stray-*` and `/api/admin/stray-*` route from the spec. `stray-animal` uploads require an active logged-in user.

- [ ] **Step 1: Write failing MockMvc tests**

Verify public nearby works without a header. Verify report submission without a user is rejected by the service. Verify every administrator route calls `adminService.requireAdmin` before its domain service. Verify `module=stray-animal` calls `merchantPermissionService.requireActiveUser`.

```java
mvc.perform(get("/api/stray-animals/nearby")
        .param("longitude", "118.904104")
        .param("latitude", "31.908685"))
        .andExpect(jsonPath("$.code").value(200));
```

- [ ] **Step 2: Run controller tests and confirm failure**

Expected: FAIL because routes are absent.

- [ ] **Step 3: Implement both controllers**

Public detail and nearby accept an optional `X-User-Id`. Submit, my-list and cancel pass the header to services for active-user validation. Every admin method calls `requireAdmin` first. Keep admin routes in the dedicated controller instead of enlarging `AdminController`.

- [ ] **Step 4: Protect file upload and run tests**

```java
if ("stray-animal".equalsIgnoreCase(safeModule)) {
    merchantPermissionService.requireActiveUser(userId);
}
```

Expected: controller and file authorization tests PASS. Commit with `feat: 提供流浪动物接口`.

### Task 6: 增加数据库迁移与模拟数据

**Files:**
- Modify: `src/main/resources/sql/pet_community_schema.sql`
- Create: `src/main/resources/sql/stray_animal_module.sql`
- Create: `frontend-tomcat/petcommunity/tests/verify-stray-database-schema.ps1`

- [ ] **Step 1: Write a failing schema marker check**

Require all three `CREATE TABLE` statements, indexes for report status/time, animal status/rescue/coordinates and application animal/user/status, plus the `弘景大道99号` seed marker.

- [ ] **Step 2: Run the check and confirm failure**

Expected: FAIL on the first missing table.

- [ ] **Step 3: Add tables in dependency-safe order**

Drop application, animal and report tables before `sys_user`. Use `DECIMAL(10,6)` coordinates and defaults matching entity constants. The migration uses `CREATE TABLE IF NOT EXISTS`.

- [ ] **Step 4: Add four distinct seed animals**

Use the existing local files `pet-doubao.jpg`, `pet-naicha.jpg`, `pet-wangcai.jpg`, and `default-post.jpg`, with different coordinates within about 3 km of `118.904104, 31.908685`. Use professional contact `江宁暖心动物救助协作组 / 025-86189999`.

- [ ] **Step 5: Run check, apply migration and verify**

Apply `stray_animal_module.sql` to `pet_community`, then run `SELECT COUNT(*) FROM stray_animal`; expected count is at least four. Commit with `feat: 增加流浪动物数据库脚本`.

### Task 7: 实现用户端地图、上报和个人记录

**Files:**
- Create: `stray-animals.html`, `stray-report.html`
- Modify: authenticated navigation blocks, `assets/app.js`, `assets/styles.css`, `tests/verify-pages.ps1`
- Create: `tests/verify-stray-animal-pages.ps1`
- Create: `tests/stray-map-card-focus.test.js`

**Interfaces:** Produces `initStrayAnimalsPage`, `initStrayReportPage`, `locateNearbyStrayAnimals`, `uploadStrayAnimalPhoto`, `loadMyStrayReports`, and `loadMyAdoptionApplications`.

- [ ] **Step 1: Write failing page checks**

Require both pages, current Baidu AK, map IDs `strayAnimalMap` and `strayReportMap`, forms and `data-page` markers. Require `stray-animals.html` in every main navigation.

- [ ] **Step 2: Write failing map-card test**

Follow `store-map-card-focus.test.js`. Assert selector `[data-stray-animal-id="42"]`, smooth center scrolling, class `stray-animal-card-highlight`, and removal after 5000 ms.

- [ ] **Step 3: Build the public list page**

Order: heading/actions, notice, full-width map, nearby cards, collapsed “我的上报”, collapsed “我的收留申请”, adoption form. Cards show photo, species, sex, age, distance, found time, health and rescue status.

- [ ] **Step 4: Build the report page**

Include image upload, required animal fields, hidden coordinates, map selection, address geocoding and a confirmation when selected location is over 50 km from current location.

- [ ] **Step 5: Extend app initialization**

Allow `stray-animals` before `requireLogin()`. Make `initHeader` null-safe, hide admin links and logout for visitors, and show a login link. Keep report and adoption actions login-protected. Reuse existing Baidu map helpers and add separate stray map state.

- [ ] **Step 6: Implement requests and upload**

Use `/api/stray-animals/nearby?...&limit=20`, `/api/stray-animal-reports`, `/api/stray-adoption-applications`, and `uploadFile("strayAnimalPhotoFile", "stray-animal")`.

- [ ] **Step 7: Run frontend verification and commit**

Run Node `--check`, `verify-pages.ps1`, `verify-stray-animal-pages.ps1`, and the map-card Node test. Expected: all PASS. Commit with `feat: 增加流浪动物用户端页面`.

### Task 8: 实现管理员页面并完成部署验收

**Files:**
- Create: `frontend-tomcat/petcommunity/admin-stray-report-detail.html`
- Modify: `admin.html`, `assets/app.js`, `assets/styles.css`, `tests/verify-pages.ps1`
- Create: `tests/verify-stray-admin-flow.ps1`

**Interfaces:** Produces `initAdminStrayReportDetailPage`, `loadAdminStrayReports`, `loadAdminStrayAnimals`, and `loadAdminStrayApplications`.

- [ ] **Step 1: Write failing admin checks**

Require section IDs `strayReportAuditSection`, `strayAnimalManagementSection`, `strayAdoptionAuditSection`; require detail containers `strayReportDetail` and `strayReportReviewForm`; require all admin endpoint strings in `app.js`.

- [ ] **Step 2: Add admin lists and navigation**

Toolbar buttons jump directly to each section. Report rows navigate to `admin-stray-report-detail.html?id=<id>`.

- [ ] **Step 3: Build independent report audit page**

Display source user, image, discovery time/address/coordinates, original descriptions and map marker. Approval requires health status, danger level, professional name, organization, phone, capture instruction, deworming advice and vaccine advice. Rejection requires only review remark.

- [ ] **Step 4: Add animal management and application review**

Animal management changes rescue state among `待救助/收留申请中/已收留/已关闭`. Application rows show applicant fields and approve/reject controls with a required review remark.

- [ ] **Step 5: Run focused and full verification**

```powershell
& 'C:\Users\31334\.m2\wrapper\dists\apache-maven-3.9.12\59fe215c0ad6947fea90184bf7add084544567b927287592651fda3782e0e798\bin\mvn.cmd' -q test
& 'C:\Users\31334\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' --check 'frontend-tomcat\petcommunity\assets\app.js'
powershell -ExecutionPolicy Bypass -File frontend-tomcat\petcommunity\tests\verify-pages.ps1
powershell -ExecutionPolicy Bypass -File frontend-tomcat\petcommunity\tests\verify-stray-animal-pages.ps1
powershell -ExecutionPolicy Bypass -File frontend-tomcat\petcommunity\tests\verify-stray-admin-flow.ps1
powershell -ExecutionPolicy Bypass -File frontend-tomcat\petcommunity\tests\verify-stray-database-schema.ps1
```

Expected: all PASS.

- [ ] **Step 6: Deploy and smoke test**

Copy the frontend tree to Tomcat, restart the backend on `8082`, then request:

```text
http://localhost:8081/petcommunity/stray-animals.html
http://localhost:8081/petcommunity/stray-report.html
http://localhost:8082/api/stray-animals/nearby?longitude=118.904104&latitude=31.908685&limit=20
```

Expected: HTTP `200`; nearby API returns at least four records sorted by `distanceKm`.

- [ ] **Step 7: Commit and record final state**

Commit with `feat: 完成流浪动物后台审核`. Run `git status --short` and confirm only the pre-existing `release/` directory remains. Do not regenerate the ZIP until user acceptance.

## Focused Test Commands

Use these exact commands at the test step of Tasks 1-5:

```powershell
$mvn = 'C:\Users\31334\.m2\wrapper\dists\apache-maven-3.9.12\59fe215c0ad6947fea90184bf7add084544567b927287592651fda3782e0e798\bin\mvn.cmd'
& $mvn -q -Dtest=StrayAnimalModelTest test
& $mvn -q -Dtest=StrayAnimalReportFlowTest test
& $mvn -q -Dtest=StrayAnimalPublicQueryTest test
& $mvn -q -Dtest=StrayAdoptionFlowTest test
& $mvn -q '-Dtest=StrayAnimalControllerTest,StrayAnimalAdminControllerAuthorizationTest,FileControllerAuthorizationTest' test
```

Each command must fail before its implementation step and pass afterward. Task 8 still runs the complete `mvn -q test` suite.
