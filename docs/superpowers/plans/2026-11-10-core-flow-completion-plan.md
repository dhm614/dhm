# 宠趣社区核心流程补全 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 补全预约权限与处理、后台举报和评论操作、关注粉丝前端、增强统计看板，并部署到 Tomcat。

**Architecture:** 沿用现有 Spring Boot REST 接口、MyBatis-Plus 服务层和原生前端结构。预约权限集中在 `AppointmentServiceImpl`，前端新增两个预约页面，其余功能在现有个人主页、后台页和统计页内增量实现。

**Tech Stack:** Java 17、Spring Boot、MyBatis-Plus、JUnit 5、Mockito、MockMvc、HTML、CSS、原生 JavaScript、PowerShell 静态检查、Tomcat 7。

**Spec:** `docs/superpowers/specs/2026-11-10-core-flow-completion-design.md`

## Global Constraints

- 本轮不接入地图 API，不配置 MinIO。
- 不修改数据库结构，不新增表或字段。
- 页面显示文本使用中文。
- 后端必须执行权限校验，不能只依赖前端隐藏。
- 使用 `X-User-Id` 作为当前登录身份，不信任请求体中的操作者 ID。
- 当前目录不是 Git 工作区，各任务以测试通过作为检查点，不执行提交命令。
- 手工编辑使用 `apply_patch`，只修改与本轮需求直接相关的文件。

---

### Task 1: 预约接口身份与门店归属权限

**Files:**
- Create: `src/test/java/com/example/petcommunity3/controller/AppointmentControllerAuthorizationTest.java`
- Create: `src/test/java/com/example/petcommunity3/service/impl/AppointmentAuthorizationTest.java`
- Modify: `src/test/java/com/example/petcommunity3/service/impl/AppointmentSlotLockTest.java`
- Modify: `src/main/java/com/example/petcommunity3/controller/AppointmentController.java`
- Modify: `src/main/java/com/example/petcommunity3/service/AppointmentService.java`
- Modify: `src/main/java/com/example/petcommunity3/service/impl/AppointmentServiceImpl.java`

**Interfaces:**
- `void create(Long operatorId, AppointmentCreateDTO dto)`
- `List<Appointment> listByUserId(Long operatorId, Long userId)`
- `List<Appointment> listByStoreId(Long operatorId, Long storeId)`
- `void cancel(Long operatorId, Long id, Long userId)`
- `void finish(Long operatorId, Long id)`
- `void updateStatus(Long operatorId, Long id, StatusUpdateDTO dto)`
- Uses `MerchantPermissionService.requireActiveUser(Long)` and `requireStoreOwnerOrAdmin(Long, Long)`.

- [x] **Step 1: 编写 Controller 失败测试**

测试 Controller 必须读取 `X-User-Id` 并传给 Service：

```java
@Test
void storeAppointmentListPassesOperatorHeaderToService() throws Exception {
    AppointmentService service = mock(AppointmentService.class);
    MockMvc mvc = mockMvc(service);

    mvc.perform(get("/api/appointments/store")
                    .header("X-User-Id", "7")
                    .param("storeId", "9"))
            .andExpect(jsonPath("$.code").value(200));

    verify(service).listByStoreId(7L, 9L);
}
```

同时覆盖创建、用户列表、取消、完成和状态更新。

- [x] **Step 2: 运行 Controller 测试并确认失败**

Run:

```powershell
$env:JAVA_HOME='C:\Users\31334\.jdks\ms-17.0.19'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
& 'C:\Users\31334\.m2\wrapper\dists\apache-maven-3.9.12\59fe215c0ad6947fea90184bf7add084544567b927287592651fda3782e0e798\bin\mvn.cmd' -q -Dtest=AppointmentControllerAuthorizationTest test
```

Expected: FAIL，因为当前 Service 方法不接收 `operatorId`。

- [x] **Step 3: 编写 Service 权限失败测试**

覆盖以下真实行为：

```java
assertThatThrownBy(() -> service.listByUserId(2L, 8L))
        .hasMessage("无权查看其他用户预约");

assertThatThrownBy(() -> service.listByStoreId(2L, 9L))
        .hasMessage("无权操作该商户数据");

service.listByStoreId(7L, 9L);
verify(merchantPermissionService).requireStoreOwnerOrAdmin(7L, 9L);
```

再覆盖商户处理自己门店预约、不能处理其他门店预约、管理员可以处理任意门店预约。

- [x] **Step 4: 运行 Service 权限测试并确认失败**

Run:

```powershell
& 'C:\Users\31334\.m2\wrapper\dists\apache-maven-3.9.12\59fe215c0ad6947fea90184bf7add084544567b927287592651fda3782e0e798\bin\mvn.cmd' -q -Dtest=AppointmentAuthorizationTest test
```

Expected: FAIL，因为当前预约服务没有操作者身份和门店归属校验。

- [x] **Step 5: 修改预约接口签名和 Controller**

所有接口增加：

```java
@RequestHeader(value = "X-User-Id", required = false) Long operatorId
```

并调用新的 Service 签名。创建预约时必须把 Header 身份传入，不再只信任 `dto.userId`。

- [x] **Step 6: 在 Service 中实现最小权限校验**

注入 `MerchantPermissionService`。增加两个私有方法：

```java
private User requireSelfOrAdmin(Long operatorId, Long targetUserId) {
    User operator = merchantPermissionService.requireActiveUser(operatorId);
    if (!"ADMIN".equals(operator.getRole()) && !operatorId.equals(targetUserId)) {
        throw new RuntimeException("无权查看其他用户预约");
    }
    return operator;
}

private Appointment requireManageableAppointment(Long operatorId, Long appointmentId) {
    Appointment appointment = appointmentMapper.selectById(appointmentId);
    if (appointment == null) {
        throw new RuntimeException("预约不存在");
    }
    merchantPermissionService.requireStoreOwnerOrAdmin(operatorId, appointment.getStoreId());
    return appointment;
}
```

创建、查看和取消用户预约调用 `requireSelfOrAdmin`；查看门店预约、完成和状态修改调用门店归属校验。状态通知中的操作者使用 Header 的 `operatorId`，不使用请求体提供的操作者 ID。

- [x] **Step 7: 运行预约测试**

Run:

```powershell
& 'C:\Users\31334\.m2\wrapper\dists\apache-maven-3.9.12\59fe215c0ad6947fea90184bf7add084544567b927287592651fda3782e0e798\bin\mvn.cmd' -q '-Dtest=AppointmentControllerAuthorizationTest,AppointmentAuthorizationTest,AppointmentSlotLockTest,RedisAppointmentSlotLockServiceTest' test
```

Expected: PASS。

### Task 2: 用户和商户预约页面

**Files:**
- Create: `frontend-tomcat/petcommunity/my-appointments.html`
- Create: `frontend-tomcat/petcommunity/merchant-appointments.html`
- Create: `frontend-tomcat/petcommunity/tests/verify-appointment-management.ps1`
- Modify: `frontend-tomcat/petcommunity/profile.html`
- Modify: `frontend-tomcat/petcommunity/services.html`
- Modify: `frontend-tomcat/petcommunity/assets/app.js`
- Modify: `frontend-tomcat/petcommunity/assets/styles.css`
- Modify: `frontend-tomcat/petcommunity/tests/verify-pages.ps1`

**Interfaces:**
- User list: `GET /api/appointments/user?userId={currentUserId}`.
- User cancel: `PUT /api/appointments/{id}/cancel?userId={currentUserId}`.
- Merchant stores: `GET /api/stores`;商户前端只保留 `merchantUserId === currentUserId` 的门店，管理员保留全部门店。预约列表接口仍在后端执行门店归属校验。
- Store appointments: `GET /api/appointments/store?storeId={storeId}`.
- Merchant status: `PUT /api/appointments/{id}/status` with `{status, remark}`.

- [x] **Step 1: 编写前端失败检查**

检查脚本必须断言：

```powershell
Assert-Contains $profile 'href="my-appointments.html"' 'My appointments entry is missing.'
Assert-Contains $services 'href="merchant-appointments.html"' 'Merchant appointment entry is missing.'
Assert-Contains $app 'function initMyAppointmentsPage()' 'My appointments initializer is missing.'
Assert-Contains $app 'function initMerchantAppointmentsPage()' 'Merchant appointments initializer is missing.'
Assert-Contains $app 'request("/api/appointments/user?userId="' 'User appointment request is missing.'
Assert-Contains $app 'request("/api/appointments/store?storeId="' 'Store appointment request is missing.'
```

- [x] **Step 2: 运行检查并确认失败**

Run:

```powershell
powershell -ExecutionPolicy Bypass -File frontend-tomcat\petcommunity\tests\verify-appointment-management.ps1
```

Expected: FAIL，两个页面和初始化函数尚不存在。

- [x] **Step 3: 创建用户预约页面**

页面包含 `data-page="my-appointments"`、返回个人主页按钮、刷新按钮和 `id="myAppointmentList"`。列表展示门店、服务、宠物、时间、备注和状态；仅状态 `1` 显示取消按钮。

- [x] **Step 4: 创建商户预约页面**

页面包含 `data-page="merchant-appointments"`、门店选择器 `merchantAppointmentStoreId` 和列表 `merchantAppointmentList`。使用 `requireMerchant()` 阻止普通用户进入，管理员也可进入。

- [x] **Step 5: 接入 app.js 页面初始化和操作**

增加：

```javascript
function initMyAppointmentsPage() {}
function loadMyAppointments() {}
function cancelMyAppointment(id) {}
function initMerchantAppointmentsPage() {}
function loadMerchantAppointmentStores() {}
function loadMerchantAppointments() {}
function updateMerchantAppointment(id, status) {}
```

操作成功后重新加载列表。页面状态使用现有 `appointmentStatus()` 格式化函数。

- [x] **Step 6: 增加入口和样式**

个人主页加入“我的预约”。服务页商户区域加入带 `data-merchant-only` 的“预约处理”链接。样式只补列表操作栏和状态标签，不调整全站主题。

- [x] **Step 7: 更新页面清单并运行前端检查**

Run:

```powershell
powershell -ExecutionPolicy Bypass -File frontend-tomcat\petcommunity\tests\verify-appointment-management.ps1
powershell -ExecutionPolicy Bypass -File frontend-tomcat\petcommunity\tests\verify-pages.ps1
```

Expected: PASS。

### Task 3: 举报状态修复、举报处理和评论管理

**Files:**
- Create: `src/test/java/com/example/petcommunity3/service/impl/ContentReportHandlingTest.java`
- Create: `frontend-tomcat/petcommunity/tests/verify-admin-report-comment-management.ps1`
- Modify: `src/main/java/com/example/petcommunity3/service/impl/ContentReportServiceImpl.java`
- Modify: `frontend-tomcat/petcommunity/admin.html`
- Modify: `frontend-tomcat/petcommunity/assets/app.js`
- Modify: `frontend-tomcat/petcommunity/assets/styles.css`

**Interfaces:**
- Reports: `GET /api/reports`, `PUT /api/reports/{id}/handle`.
- Comments: `GET /api/admin/audit/comments`, `PUT /api/admin/audit/comments/{id}/hide`, `PUT /api/admin/audit/comments/{id}/restore`.

- [x] **Step 1: 编写举报处理失败测试**

```java
@Test
void acceptedPostReportUsesAdministratorDisabledStatus() {
    service.handle(5L, handleDto(1, "举报成立"));
    assertThat(post.getStatus()).isEqualTo(3);
}

@Test
void rejectedReportDoesNotChangeTargetStatus() {
    service.handle(5L, handleDto(2, "证据不足"));
    assertThat(post.getStatus()).isEqualTo(1);
}
```

- [x] **Step 2: 运行测试并确认第一个用例失败**

Run:

```powershell
& 'C:\Users\31334\.m2\wrapper\dists\apache-maven-3.9.12\59fe215c0ad6947fea90184bf7add084544567b927287592651fda3782e0e798\bin\mvn.cmd' -q -Dtest=ContentReportHandlingTest test
```

Expected: FAIL，当前举报成立把帖子状态设置为 `2`。

- [x] **Step 3: 最小修复帖子举报状态**

将 `punishTarget()` 中帖子状态改为：

```java
post.setStatus(3);
```

不修改评论隐藏状态。

- [x] **Step 4: 编写后台前端失败检查**

断言后台存在 `commentManagementSection`、`adminCommentRows`，并且 `app.js` 包含 `loadAdminComments`、`data-admin-comment-action`、`data-report-handle` 和 `/handle` 请求。

- [x] **Step 5: 运行前端检查并确认失败**

Run:

```powershell
powershell -ExecutionPolicy Bypass -File frontend-tomcat\petcommunity\tests\verify-admin-report-comment-management.ps1
```

Expected: FAIL，后台尚无评论表格和举报操作按钮。

- [x] **Step 6: 增加评论管理区域和操作**

在 `admin.html` 增加评论表格。`loadAdminComments()` 渲染状态和隐藏/恢复按钮；按钮调用对应接口并刷新列表。

- [x] **Step 7: 增加举报成立和驳回操作**

待处理举报渲染两个按钮。点击后使用 `prompt("请输入处理说明")` 获取说明，并发送：

```javascript
request("/api/reports/" + id + "/handle", {
    method: "PUT",
    body: {status: status, handleResult: handleResult}
})
```

已处理记录只显示处理状态和结果。

- [x] **Step 8: 运行相关检查**

Run:

```powershell
& 'C:\Users\31334\.m2\wrapper\dists\apache-maven-3.9.12\59fe215c0ad6947fea90184bf7add084544567b927287592651fda3782e0e798\bin\mvn.cmd' -q '-Dtest=ContentReportHandlingTest,ContentReportControllerAuthorizationTest,PostCommentVisibilityTest' test
powershell -ExecutionPolicy Bypass -File frontend-tomcat\petcommunity\tests\verify-admin-report-comment-management.ps1
powershell -ExecutionPolicy Bypass -File frontend-tomcat\petcommunity\tests\verify-admin-section-navigation.ps1
```

Expected: PASS。

### Task 4: 个人主页关注与粉丝操作

**Files:**
- Create: `frontend-tomcat/petcommunity/tests/verify-profile-follow-flow.ps1`
- Modify: `frontend-tomcat/petcommunity/profile.html`
- Modify: `frontend-tomcat/petcommunity/assets/app.js`
- Modify: `frontend-tomcat/petcommunity/assets/styles.css`

**Interfaces:**
- Profile: `GET /api/users/{id}/profile?viewerId={currentUserId}`.
- Follow: `POST /api/follows` with `{followerId, followedId}`.
- Unfollow: `DELETE /api/follows?followerId=...&followedId=...`.
- Lists: `/api/follows/followings` and `/api/follows/followers`.

- [x] **Step 1: 编写关注流程失败检查**

断言 `profile.html` 包含 `profileFollowAction`、`followingList`、`followerList`；`app.js` 包含 `toggleProfileFollow`、`loadProfileSocialLists`、关注和取消关注请求。

- [x] **Step 2: 运行检查并确认失败**

Run:

```powershell
powershell -ExecutionPolicy Bypass -File frontend-tomcat\petcommunity\tests\verify-profile-follow-flow.ps1
```

Expected: FAIL。

- [x] **Step 3: 扩展个人主页结构**

增加关注操作容器和两列列表。保留现有“我的帖子”“我的短视频”“我的预约”和商户资格区域。

- [x] **Step 4: 实现查看目标用户和关注操作**

`initProfilePage()` 优先读取 `new URLSearchParams(location.search).get("userId")`。加载资料时始终传当前登录用户为 `viewerId`。

查看其他用户时显示按钮：

```javascript
button.textContent = profile.followedByViewer ? "取消关注" : "关注";
```

查看自己时隐藏按钮。操作成功后重新调用 `loadProfile()`。

- [x] **Step 5: 实现关注和粉丝列表**

并行请求两个列表，渲染用户昵称、用户名和：

```html
<a href="profile.html?userId=目标用户ID">查看主页</a>
```

- [x] **Step 6: 运行关注流程和现有个人主页检查**

Run:

```powershell
powershell -ExecutionPolicy Bypass -File frontend-tomcat\petcommunity\tests\verify-profile-follow-flow.ps1
powershell -ExecutionPolicy Bypass -File frontend-tomcat\petcommunity\tests\verify-profile-owned-content.ps1
```

Expected: PASS。

### Task 5: 增强数据统计看板展示

**Files:**
- Create: `frontend-tomcat/petcommunity/tests/verify-dashboard-enhanced.ps1`
- Modify: `frontend-tomcat/petcommunity/dashboard.html`
- Modify: `frontend-tomcat/petcommunity/assets/app.js`
- Modify: `frontend-tomcat/petcommunity/assets/styles.css`

**Interfaces:**
- Reuse `GET /api/statistics/dashboard`.
- Read `data.enhanced.appointmentConversion`, `data.enhanced.hotPosts`, `data.productTop10`, `data.appointmentStatus`, `data.orderStatus`.

- [x] **Step 1: 编写看板失败检查**

断言页面包含：

```text
todayMetrics
appointmentConversion
productTop10Rows
hotPostRows
appointmentStatusList
orderStatusList
```

并断言 `app.js` 使用 `todayActiveUserCount`、`finishRate`、`userAppointmentRate` 和 `productTop10`。

- [x] **Step 2: 运行检查并确认失败**

Run:

```powershell
powershell -ExecutionPolicy Bypass -File frontend-tomcat\petcommunity\tests\verify-dashboard-enhanced.ps1
```

Expected: FAIL。

- [x] **Step 3: 增加看板容器**

保留原基础指标和趋势表，新增今日指标、转化率、商品排行、帖子排行、预约状态和订单状态区域。

- [x] **Step 4: 扩展 loadDashboard()**

渲染：

```javascript
var enhanced = data.enhanced || {};
var conversion = enhanced.appointmentConversion || {};
```

百分比显示为 `number(conversion.finishRate) + "%"`。排行和状态为空时使用现有空列表渲染方式，不填充模拟数据。

- [x] **Step 5: 运行看板检查**

Run:

```powershell
powershell -ExecutionPolicy Bypass -File frontend-tomcat\petcommunity\tests\verify-dashboard-enhanced.ps1
powershell -ExecutionPolicy Bypass -File frontend-tomcat\petcommunity\tests\verify-pages.ps1
```

Expected: PASS。

### Task 6: 全量验证、部署和运行检查

**Files:**
- Modify only if verification exposes defects in files already listed above.
- Deploy: `D:\apache-tomcat-7.0.109\webapps\petcommunity`.

- [x] **Step 1: 运行 JS 语法和全部前端检查**

Run:

```powershell
& 'C:\Users\31334\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' --check 'frontend-tomcat\petcommunity\assets\app.js'
Get-ChildItem -LiteralPath 'frontend-tomcat\petcommunity\tests' -Filter '*.ps1' | ForEach-Object { powershell -ExecutionPolicy Bypass -File $_.FullName }
```

Expected: 所有脚本通过。

- [x] **Step 2: 运行完整后端测试**

Run:

```powershell
$env:JAVA_HOME='C:\Users\31334\.jdks\ms-17.0.19'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
& 'C:\Users\31334\.m2\wrapper\dists\apache-maven-3.9.12\59fe215c0ad6947fea90184bf7add084544567b927287592651fda3782e0e798\bin\mvn.cmd' -q test
```

Expected: 0 failures，0 errors。

- [x] **Step 3: 编译后端**

Run:

```powershell
& 'C:\Users\31334\.m2\wrapper\dists\apache-maven-3.9.12\59fe215c0ad6947fea90184bf7add084544567b927287592651fda3782e0e798\bin\mvn.cmd' -q -DskipTests package
```

Expected: BUILD SUCCESS。

- [x] **Step 4: 部署前端到 Tomcat**

复制整个前端目录内容到：

```text
D:\apache-tomcat-7.0.109\webapps\petcommunity
```

保留 Tomcat 目录本身，不删除其他 Web 应用。

- [x] **Step 5: 重启后端并检查接口**

停止现有 8082 Java 进程，使用项目 Maven 启动后端。检查：

```text
http://localhost:8082/api/statistics/dashboard
```

管理员接口请求必须携带 `X-User-Id: 3`。

- [x] **Step 6: 检查 Tomcat 页面地址**

验证以下地址返回成功：

```text
http://localhost:8081/petcommunity/profile.html
http://localhost:8081/petcommunity/my-appointments.html
http://localhost:8081/petcommunity/merchant-appointments.html
http://localhost:8081/petcommunity/admin.html
http://localhost:8081/petcommunity/dashboard.html
```

- [x] **Step 7: 最终验收记录**

报告完成的五项功能、测试结果、页面地址，以及仍按计划暂缓的地图 API 和 MinIO 配置。
