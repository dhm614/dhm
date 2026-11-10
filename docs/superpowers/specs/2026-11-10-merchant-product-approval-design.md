# 商户资格与商品操作审核设计

## 目标

在现有普通用户和管理员体系上增加商户资格审核，并确保只有已审核商户能够管理自己的门店、服务、排班和商品。商品新增、修改、上架、下架必须逐次提交管理员审核，审核通过后才真正修改商城数据。同时补充帖子置顶和加精能力，由管理员维护社区内容展示优先级。

## 角色与权限

系统继续使用 `sys_user.role` 区分 `USER` 和 `ADMIN`，不新增第三种角色值。商户是普通用户的一种业务资格，通过 `is_merchant` 和 `merchant_apply_status` 表示。

- 普通用户：浏览门店、服务、排班和商品，创建预约、购物车和订单；不能调用管理接口。
- 待审核商户申请用户：保留普通用户权限，不能进入商户管理页面。
- 已审核商户：管理自己的门店、服务、排班；提交自己商品的操作申请。
- 管理员：审核商户资格和商品操作申请，并保留平台管理权限。

帖子置顶和加精属于管理员社区运营权限，普通用户和商户都不能调用对应写接口。

后端继续通过 `X-User-Id` 获取当前操作者。前端隐藏只用于改善体验，所有写接口必须执行后端权限和数据归属校验。

## 数据模型

### 用户表

`sys_user` 增加：

- `is_merchant TINYINT NOT NULL DEFAULT 0`：`0` 非商户，`1` 已审核商户。
- `merchant_apply_status TINYINT NOT NULL DEFAULT 0`：`0` 未申请，`1` 待审核，`2` 已通过，`3` 已驳回。

管理员通过申请时，同时设置 `is_merchant = 1`、`merchant_apply_status = 2`。驳回时设置 `is_merchant = 0`、`merchant_apply_status = 3`。

### 商户资格申请表

新增 `merchant_application`：

- `id`
- `user_id`
- `reason`
- `status`：`0` 待审核，`1` 已通过，`2` 已驳回
- `reviewer_id`
- `review_remark`
- `review_time`
- `create_time`
- `update_time`

同一用户只能存在一条待审核申请。已驳回用户可以重新提交新申请。

### 数据归属

`store` 增加 `merchant_user_id BIGINT`，`product` 增加 `merchant_user_id BIGINT`。

- 商户只能修改 `merchant_user_id` 等于自己的门店和商品。
- 管理员可以管理全部门店、服务和排班；管理员直接创建的门店允许 `merchant_user_id` 为空，表示平台门店。
- 服务项目和排班通过 `store_id` 反查门店归属。
- 普通公开查询不按商户过滤，只按原有业务状态过滤。

初始化迁移：

- `lisi` 设置为已审核商户。
- 现有门店和商品的 `merchant_user_id` 设置为 `lisi` 的用户 ID。
- 现有服务项目和排班通过门店自然归属 `lisi`。
- `zhangsan` 保持普通用户，`admin` 保持管理员。

### 商品操作申请表

新增 `product_operation_application`：

- `id`
- `merchant_user_id`
- `product_id`：新增商品时为空
- `operation_type`：`CREATE`、`UPDATE`、`ENABLE`、`DISABLE`
- `product_name`
- `category`
- `price`
- `stock`
- `cover`
- `description`
- `status`：`0` 待审核，`1` 已通过，`2` 已驳回
- `reviewer_id`
- `review_remark`
- `review_time`
- `create_time`
- `update_time`

申请表使用结构化字段保存待生效数据，不使用 JSON。`CREATE` 和 `UPDATE` 保存完整商品快照；`ENABLE` 和 `DISABLE` 保存目标商品 ID。

同一商品同一时间只允许存在一条待审核操作，避免多个申请覆盖顺序不明确。

### 帖子运营字段

`post` 增加：

- `is_pinned TINYINT NOT NULL DEFAULT 0`：`0` 普通，`1` 置顶。
- `is_featured TINYINT NOT NULL DEFAULT 0`：`0` 普通，`1` 精选。
- `pinned_time DATETIME DEFAULT NULL`：最近一次置顶时间，用于多个置顶帖之间排序。

置顶和加精是两个互不排斥的状态，同一帖子可以同时置顶并加精。只有 `status = 1` 的已审核帖子可以设置；帖子被驳回或下架时自动清除置顶和加精状态。

## 后端接口

### 商户资格

- `POST /api/merchant/applications`：当前普通用户提交申请。
- `GET /api/merchant/applications/my`：查询当前用户最新申请及资格状态。
- `GET /api/admin/merchant-applications`：管理员查询申请队列。
- `PUT /api/admin/merchant-applications/{id}/review`：管理员通过或驳回。

### 商户门店管理

门店、服务和排班公开查询接口保持不变。以下写接口必须读取 `X-User-Id`：

- 门店新增、修改、停用：要求已审核商户或管理员。
- 服务新增、修改、停用：要求已审核商户或管理员，并校验门店归属。
- 排班新增、修改、停用：要求已审核商户或管理员，并校验门店及服务归属。

商户新增门店时，后端强制写入当前用户 ID，不能接受前端指定其他商户。

### 商品操作申请

- `POST /api/merchant/product-applications`：新增商品申请。
- `PUT /api/merchant/product-applications/{productId}`：修改商品申请。
- `POST /api/merchant/product-applications/{productId}/enable`：上架申请。
- `POST /api/merchant/product-applications/{productId}/disable`：下架申请。
- `GET /api/merchant/products`：当前商户商品列表。
- `GET /api/merchant/product-applications`：当前商户操作申请列表。
- `GET /api/admin/product-applications`：管理员审核队列。
- `PUT /api/admin/product-applications/{id}/review`：管理员审核。

现有商品公开查询接口继续返回已上架商品。商户不能再通过原有商品写接口直接修改商品。

管理员审核通过时使用事务：

- `CREATE`：插入商品，设置所属商户和上架状态。
- `UPDATE`：校验商品归属后覆盖申请快照。
- `ENABLE`：设置商品状态为 `1`。
- `DISABLE`：设置商品状态为 `0`。
- 最后更新申请状态、审核人、审核意见和审核时间。

驳回只修改申请状态，不修改商品。

### 帖子置顶与加精

- `PUT /api/admin/posts/{id}/pin`：管理员置顶已审核帖子。
- `PUT /api/admin/posts/{id}/unpin`：管理员取消置顶。
- `PUT /api/admin/posts/{id}/feature`：管理员设为精选。
- `PUT /api/admin/posts/{id}/unfeature`：管理员取消精选。

接口继续使用 `X-User-Id` 校验管理员身份。公开社区列表按照 `is_pinned DESC`、`pinned_time DESC`、`create_time DESC` 排序，保证置顶帖优先展示；精选状态不强制改变排序，只作为内容标识和后台运营条件。

### 文件上传

`POST /api/files/upload` 继续服务帖子、宠物、门店、服务和商品图片。`module=product` 时要求当前用户是已审核商户或管理员。上传文件只返回 URL，不代表商品已经上架；商品仍需提交操作申请并等待管理员审核。

## 前端页面

### 个人主页

增加商户申请区域：

- 未申请：显示申请原因输入和“申请成为商户”。
- 待审核：显示待审核状态并禁止重复提交。
- 已通过：显示商户标识。
- 已驳回：显示驳回状态并允许重新申请。

### 门店预约页

普通用户只看到门店、在线预约、服务项目和排班查询。

排班创建、门店入驻、新增服务项目放入商户管理区域，仅已审核商户和管理员可见。商户页面只提供自己的门店作为管理目标。

### 模拟商城

移除普通商城页面中的商品创建表单。所有用户继续看到商品、购物车、结算和订单。

页面底部增加仅已审核商户可见的两个按钮，管理员不使用商户商品页面，而是在后台负责审核：

- “商品上传”进入 `merchant-product-create.html`。
- “商品管理”进入 `merchant-products.html`。

`merchant-product-create.html` 提交商品新增申请，不直接新增商品。

`merchant-products.html` 展示当前商户商品和申请记录，修改、上架、下架操作均生成审核申请。

### 后台管理页

增加：

- 商户资格申请列表及通过、驳回按钮。
- 商品操作申请列表及通过、驳回按钮。
- 已审核帖子列表增加“置顶/取消置顶”和“加精/取消加精”操作。

审核完成后刷新对应列表。商品操作通过后，商城公开列表按商品最终状态变化。

### 社区帖子展示

公开帖子卡片根据字段显示“置顶”和“精选”标识。普通用户只能查看标识，不能看到运营按钮。置顶帖始终排在普通帖子之前。

## 错误处理

- 缺少 `X-User-Id`：返回“用户身份不能为空”。
- 非商户调用商户接口：返回“当前用户不是已审核商户”。
- 操作其他商户数据：返回“无权操作该商户数据”。
- 重复待审核申请：返回“已有待审核申请”。
- 审核已处理申请：返回“申请已处理”。
- 更新或上下架不存在商品：返回“商品不存在”。

错误通过现有全局异常处理转换为统一 `Result` 响应。

## 测试与迁移

- 增加数据库结构静态检查和迁移脚本。
- 增加商户资格申请与审核服务测试。
- 增加普通用户拒绝写操作、商户数据归属、管理员放行测试。
- 增加商品四种操作申请的审核生效测试。
- 增加帖子置顶、取消置顶、加精、取消加精和下架清理状态测试。
- 增加公开帖子列表置顶优先排序测试。
- 增加前端普通用户隐藏、商户显示和后台审核入口检查。
- 执行迁移后核对 `lisi`、门店和商品归属。
- 运行 Maven 测试、前端静态检查和 JS 语法检查。
- 重启后端并同步前端文件到 Tomcat 8081。

## 非目标

- 不接入真实支付。
- 不引入 JWT、Spring Security 登录会话或新的前端框架。
- 不为被驳回的商品图片实现自动文件清理。
- 不改变普通用户浏览、购物车、结算和订单接口。
