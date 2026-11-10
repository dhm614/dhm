# AGENTS.md

## 项目概览

这是“宠趣社区”项目，一个基于 Spring Boot + MyBatis-Plus + MySQL 的一站式宠物社区与服务平台。后端补充使用 Redis、MinIO 可选对象存储、MySQL FULLTEXT 和 Java JUC。

核心方向：
- 前台：登录注册、主页、社区发帖、话题申请、宠物档案、门店预约、模拟商城、个人主页。
- 后台：内容审核、话题审核与管理、举报和敏感词、预约订单查看、统计看板。
- 前端运行在 Tomcat 8081：`http://localhost:8081/petcommunity/index.html`
- 后端运行在 Spring Boot 8082：`http://localhost:8082`
- 数据库：`pet_community`

## 技术落地点

- 社区评论：`post_comment.parent_id` 使用邻接表保存父子关系，`/api/post-interactions/comments/tree` 返回树形结构；`post.like_count/comment_count/collect_count` 继续做冗余统计。
- 预约并发：`AppointmentSlotLockService` 优先使用 Redis `setIfAbsent` 分布式锁；Redis 不可用时使用 JUC `ReentrantLock` 本机兜底，数据库仍用 `booked_count < capacity` 原子更新防超约。
- 文件上传：`FileStorageServiceImpl` 默认本地保存到 `uploads`，配置 `petcommunity.storage.type=minio` 后使用 MinIO；图片上传会生成 `thumb_` 缩略图并返回 `thumbnailUrl`。
- 搜索：帖子和商品 Mapper 使用 MySQL `FULLTEXT` 索引查询；全文搜索无结果或现有库还没迁移时，服务层退回原来的 `LIKE` 查询。

## 项目结构

```text
D:\ideawork\petcommunity3
|-- src\main\java\com\example\petcommunity3
|   |-- common       # 统一返回对象 Result
|   |-- config       # CORS、上传静态资源映射等配置
|   |-- controller   # REST 接口入口
|   |-- dto          # 前端请求体对象
|   |-- entity       # 数据库实体
|   |-- exception    # 全局异常处理
|   |-- mapper       # MyBatis-Plus Mapper
|   |-- service      # 业务接口和实现
|   `-- vo           # 返回给前端的视图对象
|-- src\main\resources
|   |-- application.properties
|   `-- sql
|       |-- pet_community_schema.sql  # 主建表和初始化数据脚本
|       |-- admin_user_fields.sql     # 管理员字段迁移脚本
|       `-- feature_19_28.sql         # 功能 19-28 相关 SQL
|-- frontend-tomcat\petcommunity
|   |-- assets
|   |   |-- app.js       # 前端主要业务逻辑
|   |   |-- styles.css   # 页面样式
|   |   `-- images       # 本地图片资源
|   |-- tests           # 前端静态检查脚本
|   |-- index.html      # 登录页
|   |-- register.html   # 注册页
|   |-- home.html       # 主页
|   |-- community.html  # 社区页
|   |-- pets.html       # 宠物档案页
|   |-- services.html   # 门店预约页
|   |-- shop.html       # 模拟商城页
|   |-- profile.html    # 个人主页
|   |-- dashboard.html  # 管理员总览
|   `-- admin.html      # 后台管理页
|-- uploads            # 后端本地上传文件目录
|-- pom.xml
|-- mvnw
`-- mvnw.cmd
```

## 常用账号

初始账号密码均为 `123456`。

```text
管理员：admin / 123456
普通用户：zhangsan / 123456
普通用户：lisi / 123456
```

管理员接口依赖当前登录用户 ID，请求头必须带：

```http
X-User-Id: 3
```

其中 `3` 是当前数据库里的 `admin` 用户 ID。

## 常用命令

### 后端编译

```powershell
$env:JAVA_HOME='C:\Users\31334\.jdks\ms-17.0.19'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
& 'C:\Users\31334\.m2\wrapper\dists\apache-maven-3.9.12\59fe215c0ad6947fea90184bf7add084544567b927287592651fda3782e0e798\bin\mvn.cmd' -q -DskipTests compile
```

### 后端测试

```powershell
$env:JAVA_HOME='C:\Users\31334\.jdks\ms-17.0.19'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
& 'C:\Users\31334\.m2\wrapper\dists\apache-maven-3.9.12\59fe215c0ad6947fea90184bf7add084544567b927287592651fda3782e0e798\bin\mvn.cmd' -q test
```

### 启动后端

```powershell
$env:JAVA_HOME='C:\Users\31334\.jdks\ms-17.0.19'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
& 'C:\Users\31334\.m2\wrapper\dists\apache-maven-3.9.12\59fe215c0ad6947fea90184bf7add084544567b927287592651fda3782e0e798\bin\mvn.cmd' spring-boot:run
```

后端默认端口：`8082`。

### 查看 8082 端口进程

```powershell
netstat -ano | Select-String ':8082'
```

### 停止后端进程

```powershell
Stop-Process -Id <PID> -Force
```

### 启动 Tomcat 前端

Tomcat 目录：

```text
D:\apache-tomcat-7.0.109
```

启动命令：

```powershell
cmd /c D:\apache-tomcat-7.0.109\bin\startup.bat
```

前端访问地址：

```text
http://localhost:8081/petcommunity/index.html
```

### 部署前端到 Tomcat

修改 `frontend-tomcat\petcommunity` 后，需要复制到：

```text
D:\apache-tomcat-7.0.109\webapps\petcommunity
```

示例：

```powershell
Copy-Item -LiteralPath 'D:\ideawork\petcommunity3\frontend-tomcat\petcommunity\assets\app.js' -Destination 'D:\apache-tomcat-7.0.109\webapps\petcommunity\assets\app.js' -Force
Copy-Item -LiteralPath 'D:\ideawork\petcommunity3\frontend-tomcat\petcommunity\services.html' -Destination 'D:\apache-tomcat-7.0.109\webapps\petcommunity\services.html' -Force
```

### 前端检查

```powershell
powershell -ExecutionPolicy Bypass -File frontend-tomcat\petcommunity\tests\verify-pages.ps1
powershell -ExecutionPolicy Bypass -File frontend-tomcat\petcommunity\tests\verify-home-page.ps1
powershell -ExecutionPolicy Bypass -File frontend-tomcat\petcommunity\tests\verify-services-page-order.ps1
powershell -ExecutionPolicy Bypass -File frontend-tomcat\petcommunity\tests\verify-service-image-upload.ps1
powershell -ExecutionPolicy Bypass -File frontend-tomcat\petcommunity\tests\verify-ios-visual-style.ps1
powershell -ExecutionPolicy Bypass -File frontend-tomcat\petcommunity\tests\verify-audit-permissions.ps1
powershell -ExecutionPolicy Bypass -File frontend-tomcat\petcommunity\tests\verify-topic-audit.ps1
```

### JS 语法检查

```powershell
& 'C:\Users\31334\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' --check 'frontend-tomcat\petcommunity\assets\app.js'
```

### MySQL 常用命令

```powershell
& 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe' -uroot -p123456 --default-character-set=utf8mb4 -D pet_community -e "SELECT id,username,role,status FROM sys_user ORDER BY id;"
```

查看话题：

```powershell
& 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe' -uroot -p123456 --default-character-set=utf8mb4 -D pet_community -e "SELECT id,name,sort,status FROM topic ORDER BY id;"
```

## 前后端连接方式

前端统一通过 `frontend-tomcat\petcommunity\assets\app.js` 的 `request()` 请求后端：

```javascript
request("/api/stores", {method: "POST", body: payload})
```

实际访问：

```text
http://localhost:8082/api/stores
```

普通 JSON 请求会自动设置：

```http
Content-Type: application/json
```

文件上传使用 `FormData`，不要手动设置 `Content-Type`：

```javascript
var formData = new FormData();
formData.append("file", input.files[0]);
formData.append("module", module);
request("/api/files/upload", {method: "POST", body: formData})
```

## 重要业务规则

### 登录和权限

- 登录页分管理员登录和普通用户登录。
- 管理员账号只能从管理员入口登录。
- 普通用户不能看到“总览”和“后台管理”入口。
- 后台页面和 `/api/admin/**` 接口都必须校验管理员身份。
- 管理员接口前端请求必须带 `X-User-Id`。

### 社区帖子审核

- 普通用户发布帖子后状态为 `0`，表示待审核。
- 社区公开列表只显示 `status = 1` 的帖子。
- 管理员在后台审核通过后，帖子才会在社区展示。
- 审核驳回或下架使用 `status = 2`。

### 话题审核

- 普通用户可以申请话题，但不能填写排序值。
- 普通用户申请的话题状态为 `0`，排序值强制为 `0`。
- 公开话题列表只显示 `status = 1`。
- 管理员审核话题时必须填写排序值。
- 管理员可以直接新增话题，新增后直接展示。
- 管理员删除话题时使用 `status = 2` 隐藏，不物理删除。

话题状态含义：

```text
0 = 待审核
1 = 已通过
2 = 驳回/删除
```

### 文件上传

- 文件上传接口：`POST /api/files/upload`
- 后端本地上传目录：`uploads`
- 默认存储类型是 `petcommunity.storage.type=local`；配置为 `minio` 并填写 MinIO endpoint/key/bucket 后走对象存储。
- 图片上传返回 `url` 和 `thumbnailUrl`，业务表单仍优先保存 `url`。
- 前端上传后，只保存返回的 URL 到业务表单字段，例如 `cover` 或 `avatar`。
- 门店图片模块名：`store`
- 服务图片模块名：`service`
- 帖子图片模块名：`post`
- 宠物头像模块名：`pet`
- 商品图片模块名：`product`

### 门店预约

- 门店预约页最上方优先展示门店图片和在线预约。
- 门店入驻和新增服务项目放在页面底部。
- 附近门店使用经纬度计算距离，并按距离升序排序。
- 预约使用服务排班 ID，避免只靠手填时间导致排班不一致。
- 预约创建和恢复有效状态时必须经过 `AppointmentSlotLockService` 时间槽锁。

### 模拟商城

- 商城是模拟结算，不接真实支付。
- 商品图片走文件上传，表单保存图片 URL。

### 搜索

- 帖子和商品表有 `FULLTEXT` 索引：`ft_post_search`、`ft_product_search`。
- 搜索接口仍是 `/api/search`，前端不用改地址。

## 开发规则

- 页面显示文本尽量使用中文。
- 改前端后，要同步复制到 Tomcat 的 `webapps\petcommunity`。
- 改后端接口、服务、实体、Mapper 后，要重新编译并重启 8082 后端。
- 新增或修改功能时优先补对应测试或静态检查脚本。
- 不要把普通用户能力接到 `/api/admin/**`。
- 不要只靠前端隐藏做权限控制，后端也必须校验。
- 不要使用真实支付能力，商城只做模拟结算。
- 修改数据库结构时，同步更新 `src\main\resources\sql\pet_community_schema.sql` 和必要的迁移脚本。
- PowerShell 执行中文脚本容易出现编码问题，测试脚本里的输出和报错优先使用 ASCII 文本。
- 当前项目不是标准 Git 工作区时，不要依赖 `git diff` 做最终确认，直接用文件检查和测试命令验证。

## 推荐验证顺序

后端改动：

```powershell
$env:JAVA_HOME='C:\Users\31334\.jdks\ms-17.0.19'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
& 'C:\Users\31334\.m2\wrapper\dists\apache-maven-3.9.12\59fe215c0ad6947fea90184bf7add084544567b927287592651fda3782e0e798\bin\mvn.cmd' -q test
```

前端改动：

```powershell
& 'C:\Users\31334\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' --check 'frontend-tomcat\petcommunity\assets\app.js'
powershell -ExecutionPolicy Bypass -File frontend-tomcat\petcommunity\tests\verify-pages.ps1
```

服务页相关改动：

```powershell
powershell -ExecutionPolicy Bypass -File frontend-tomcat\petcommunity\tests\verify-services-page-order.ps1
powershell -ExecutionPolicy Bypass -File frontend-tomcat\petcommunity\tests\verify-service-image-upload.ps1
```

审核相关改动：

```powershell
powershell -ExecutionPolicy Bypass -File frontend-tomcat\petcommunity\tests\verify-audit-permissions.ps1
powershell -ExecutionPolicy Bypass -File frontend-tomcat\petcommunity\tests\verify-topic-audit.ps1
```
