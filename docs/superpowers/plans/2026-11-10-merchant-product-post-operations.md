# 商户审核、商品申请与帖子运营实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 增加商户资格审核、商户数据归属、商品操作逐次审核，以及管理员帖子置顶和加精能力，并完成对应前端入口与数据库迁移。

**Architecture:** 保留现有 `USER/ADMIN` 角色，通过用户表商户字段表达业务资格。商户商品写操作先写入申请快照表，管理员事务审核通过后再修改正式商品表；门店、服务和排班写接口统一校验商户资格与门店归属。帖子运营状态直接保存在 `post` 表，由管理员接口维护，公开列表按置顶优先排序。

**Tech Stack:** Java 17、Spring Boot 3.5、MyBatis-Plus、MySQL 8、JUnit 5、Mockito、原生 HTML/CSS/JavaScript、Tomcat 7 静态前端。

**Spec:** `docs/superpowers/specs/2026-11-10-merchant-product-approval-design.md`

## Global Constraints

- 所有新增页面文字和错误信息使用中文。
- 权限不能只靠前端隐藏，所有写接口必须在后端校验 `X-User-Id`。
- 商城保持模拟结算，不接入真实支付。
- 修改表结构时同步更新主建表脚本和独立迁移脚本。
- 只修改本需求直接涉及的代码，不重构无关模块。
- 当前目录不是 Git 工作区，不执行提交步骤，以测试结果和文件检查作为阶段验收。

---

### Task 1: 数据模型与迁移脚本

**Files:**
- Create: `src/main/resources/sql/merchant_product_post_operations.sql`
- Modify: `src/main/resources/sql/pet_community_schema.sql`
- Modify: `src/main/java/com/example/petcommunity3/entity/User.java`
- Modify: `src/main/java/com/example/petcommunity3/entity/Store.java`
- Modify: `src/main/java/com/example/petcommunity3/entity/Product.java`
- Modify: `src/main/java/com/example/petcommunity3/entity/Post.java`
- Create: `src/main/java/com/example/petcommunity3/entity/MerchantApplication.java`
- Create: `src/main/java/com/example/petcommunity3/entity/ProductOperationApplication.java`
- Create: `src/main/java/com/example/petcommunity3/mapper/MerchantApplicationMapper.java`
- Create: `src/main/java/com/example/petcommunity3/mapper/ProductOperationApplicationMapper.java`
- Test: `src/main/resources/sql/tests/verify-merchant-product-post-schema.ps1`

**Interfaces:**
- Produces: 用户商户字段、门店和商品归属字段、两张审核申请表、帖子运营字段。

- [ ] **Step 1: 编写失败的结构检查脚本**
- [ ] **Step 2: 运行结构检查并确认失败**
- [ ] **Step 3: 增加实体、Mapper、主建表和幂等迁移脚本**
- [ ] **Step 4: 重新运行结构检查并确认通过**

### Task 2: 商户资格申请与管理员审核

**Files:**
- Create: `src/main/java/com/example/petcommunity3/dto/MerchantApplicationCreateDTO.java`
- Create: `src/main/java/com/example/petcommunity3/dto/ApplicationReviewDTO.java`
- Create: `src/main/java/com/example/petcommunity3/service/MerchantApplicationService.java`
- Create: `src/main/java/com/example/petcommunity3/service/impl/MerchantApplicationServiceImpl.java`
- Create: `src/main/java/com/example/petcommunity3/controller/MerchantController.java`
- Modify: `src/main/java/com/example/petcommunity3/controller/AdminController.java`
- Modify: `src/main/java/com/example/petcommunity3/service/AdminService.java`
- Modify: `src/main/java/com/example/petcommunity3/service/impl/AdminServiceImpl.java`
- Test: `src/test/java/com/example/petcommunity3/service/impl/MerchantApplicationFlowTest.java`

**Interfaces:**
- Produces: `POST /api/merchant/applications`、`GET /api/merchant/applications/my`、`GET /api/admin/merchant-applications`、`PUT /api/admin/merchant-applications/{id}/review`。

- [ ] **Step 1: 编写商户申请和审核失败测试**
- [ ] **Step 2: 运行测试并确认因服务不存在而失败**
- [ ] **Step 3: 实现申请、查询和事务审核逻辑**
- [ ] **Step 4: 运行商户申请测试并确认通过**

### Task 3: 门店、服务、排班归属权限

**Files:**
- Create: `src/main/java/com/example/petcommunity3/service/MerchantPermissionService.java`
- Create: `src/main/java/com/example/petcommunity3/service/impl/MerchantPermissionServiceImpl.java`
- Modify: `src/main/java/com/example/petcommunity3/controller/StoreController.java`
- Modify: `src/main/java/com/example/petcommunity3/controller/ServiceItemController.java`
- Modify: `src/main/java/com/example/petcommunity3/controller/ServiceScheduleController.java`
- Modify: corresponding service interfaces and implementations
- Test: `src/test/java/com/example/petcommunity3/service/impl/MerchantOwnershipAuthorizationTest.java`

**Interfaces:**
- Produces: `requireMerchantOrAdmin(Long)`、`requireStoreOwnerOrAdmin(Long, Long)`；所有相关写方法接收操作者 ID。

- [ ] **Step 1: 编写普通用户拒绝、商户本人允许、跨商户拒绝测试**
- [ ] **Step 2: 运行测试并确认当前无权限校验而失败**
- [ ] **Step 3: 实现统一权限服务并接入三个模块的写接口**
- [ ] **Step 4: 运行权限测试并确认通过**

### Task 4: 商品操作申请与审核生效

**Files:**
- Create: `src/main/java/com/example/petcommunity3/dto/ProductOperationCreateDTO.java`
- Create: `src/main/java/com/example/petcommunity3/service/ProductOperationApplicationService.java`
- Create: `src/main/java/com/example/petcommunity3/service/impl/ProductOperationApplicationServiceImpl.java`
- Create: `src/main/java/com/example/petcommunity3/controller/MerchantProductController.java`
- Modify: `src/main/java/com/example/petcommunity3/controller/AdminController.java`
- Modify: `src/main/java/com/example/petcommunity3/controller/ProductController.java`
- Test: `src/test/java/com/example/petcommunity3/service/impl/ProductOperationApplicationFlowTest.java`

**Interfaces:**
- Produces: 商户商品列表、申请列表、`CREATE/UPDATE/ENABLE/DISABLE` 申请接口和管理员审核接口。

- [ ] **Step 1: 编写四种操作申请的失败测试**
- [ ] **Step 2: 运行测试并确认失败**
- [ ] **Step 3: 实现申请快照、归属校验和事务审核**
- [ ] **Step 4: 禁止原 `/api/products` 写接口被商户绕过审核**
- [ ] **Step 5: 运行商品申请测试并确认通过**

### Task 5: 帖子置顶、加精与排序

**Files:**
- Modify: `src/main/java/com/example/petcommunity3/service/AdminService.java`
- Modify: `src/main/java/com/example/petcommunity3/service/impl/AdminServiceImpl.java`
- Modify: `src/main/java/com/example/petcommunity3/controller/AdminController.java`
- Modify: `src/main/java/com/example/petcommunity3/service/impl/PostServiceImpl.java`
- Modify: `src/main/java/com/example/petcommunity3/mapper/PostMapper.java`
- Test: `src/test/java/com/example/petcommunity3/service/impl/PostOperationManagementTest.java`

**Interfaces:**
- Produces: `pinPost`、`unpinPost`、`featurePost`、`unfeaturePost`；公开列表置顶优先。

- [ ] **Step 1: 编写置顶、加精、下架清理和排序失败测试**
- [ ] **Step 2: 运行测试并确认失败**
- [ ] **Step 3: 实现四个管理员接口与状态约束**
- [ ] **Step 4: 调整公开列表和全文搜索字段映射**
- [ ] **Step 5: 运行帖子运营测试及既有审核测试**

### Task 6: 文件上传权限与前端页面

**Files:**
- Modify: `src/main/java/com/example/petcommunity3/controller/FileController.java`
- Modify: `frontend-tomcat/petcommunity/profile.html`
- Modify: `frontend-tomcat/petcommunity/services.html`
- Modify: `frontend-tomcat/petcommunity/shop.html`
- Create: `frontend-tomcat/petcommunity/merchant-product-create.html`
- Create: `frontend-tomcat/petcommunity/merchant-products.html`
- Modify: `frontend-tomcat/petcommunity/admin.html`
- Modify: `frontend-tomcat/petcommunity/assets/app.js`
- Modify: `frontend-tomcat/petcommunity/assets/styles.css`
- Test: `frontend-tomcat/petcommunity/tests/verify-merchant-product-approval.ps1`
- Test: `frontend-tomcat/petcommunity/tests/verify-post-operations.ps1`

**Interfaces:**
- Consumes: Tasks 2-5 的接口。
- Produces: 个人主页商户申请、商户管理区域、商品申请页面、后台审核区域、帖子运营按钮和社区标签。

- [ ] **Step 1: 编写前端静态失败检查并确认失败**
- [ ] **Step 2: 接入商户状态并按资格隐藏或显示管理区域**
- [ ] **Step 3: 新增商品申请页面和后台审核区域**
- [ ] **Step 4: 接入帖子置顶加精按钮及社区标签**
- [ ] **Step 5: 为 `module=product` 上传增加商户或管理员校验**
- [ ] **Step 6: 运行 JS 语法与前端静态检查**

### Task 7: 数据库迁移、全量验证和部署

**Files:**
- Deploy: `frontend-tomcat/petcommunity/**` to `D:/apache-tomcat-7.0.109/webapps/petcommunity/`

- [ ] **Step 1: 运行 Maven 全量测试**
- [ ] **Step 2: 运行全部前端静态检查和 JS 语法检查**
- [ ] **Step 3: 执行幂等数据库迁移脚本并核对 `lisi` 归属**
- [ ] **Step 4: 重新打包并重启 8082 后端**
- [ ] **Step 5: 同步前端到 Tomcat 并确认 8081 页面可访问**
- [ ] **Step 6: 使用管理员、商户和普通用户分别进行接口冒烟验证**

