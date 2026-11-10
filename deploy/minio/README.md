# MinIO 本机部署

当前部署约定：

- MinIO 程序：`D:\minio\bin\minio.exe`
- MinIO 客户端：`D:\minio\bin\mc.exe`
- 对象数据：`D:\minio\data`
- 环境变量：`D:\minio\petcommunity.env.ps1`
- S3 API：`http://localhost:9000`
- 管理控制台：`http://localhost:9001`
- Bucket：`pet-community`
- Server：从 MinIO 官方标签 `RELEASE.2025-10-15T17-29-55Z` 构建
- Client：`mc RELEASE.2025-08-13T08-35-41Z`

启动顺序：

```powershell
powershell -ExecutionPolicy Bypass -File deploy\minio\start-minio.ps1
powershell -ExecutionPolicy Bypass -File deploy\minio\start-backend-minio.ps1
```

Bucket 仅开放对象读取，上传、删除和列表仍需要 MinIO 凭据。原 `uploads` 目录保留用于回退，不再作为启用 MinIO 后的新文件存储位置。

现有文件 URL 迁移：

```powershell
& 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe' -uroot -p123456 --default-character-set=utf8mb4 -D pet_community -e "source D:/ideawork/petcommunity3/src/main/resources/sql/minio_existing_uploads.sql;"
```

数据库 URL 回退：

```powershell
& 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe' -uroot -p123456 --default-character-set=utf8mb4 -D pet_community -e "source D:/ideawork/petcommunity3/src/main/resources/sql/minio_existing_uploads_rollback.sql;"
```

完整代码回退基线为 Git 标签 `before-minio-deployment-20261111`。MinIO 登录用户名固定为 `petcommunityadmin`，随机密码保存在 `D:\minio\petcommunity.env.ps1`，该文件不进入 Git。
