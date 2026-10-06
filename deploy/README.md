# 部署脚本

与 [`docs/delivery/05-运维交付包.md`](../docs/delivery/05-运维交付包.md) 一起使用。不要把数据库口令写进本目录。

| 文件 | 作用 |
|------|------|
| `nginx-https.conf.example` | `yaoheng.cloud` / `www` 的 HTTPS 配置 |
| `backup.sh` | `mysqldump` 压缩备份 |
| `restore.sh` | 从 `.sql.gz` 恢复（覆盖库） |
| `healthcheck.sh` | 前端、公钥接口、磁盘、最近备份 |
| `remote-release.sh` | 服务器上换 JAR 与静态页；巡检失败则换回上一版 |
| `fix-batch-stock-menu.sql` | 补「批号库存」侧栏菜单 |

巡检时 `BASE_URL=https://yaoheng.cloud`。

## 网页前端 URL（必须相对路径）

生产 Nginx：站点根目录放 `warehouse-front/dist`，接口反代 `/warehouse/` → `127.0.0.1:8899/warehouse/`。

- 打包：`cd warehouse-front && npm ci && npm run build:prod`
- `.env.production` 里 `VITE_APP_BASE_URL=/warehouse`（**不要**写成 `http://IP:8899` 或带端口的绝对地址）
- Vite `base` 保持默认 `/`，静态资源是 `/static/...`，与 `location /` + `try_files` 一致
- 不要把 `dist` 提交进 Git；在本机或 CI 打好包后 rsync 到 `/usr/share/nginx/html`

服务器拉代码后只编后端 JAR 并重启 Java；前端仍是「本机 `build:prod` → 上传静态文件」。重启 Java 后演示数据种子会自动补信誉额等（不覆盖业务改过的同名单据）。

## 自动发布（GitHub Actions）

推送到 `master` 或 `main`，且 Backend、Frontend 两个作业都通过之后，`Deploy` 作业会打包生产 JAR 与 `dist`，经 SSH 上传，并在服务器执行 `remote-release.sh`。Pull Request 只做测试和构建，不发布。

仓库 Secrets：

| 名称 | 内容 |
|------|------|
| `SSH_HOST` | 服务器地址，现为 `123.57.134.57` |
| `SSH_USER` | 发布账号，现为 `root` |
| `SSH_PRIVATE_KEY` | 只给 Actions 使用的私钥全文 |
| `SSH_PORT` | `2222`。主机没有开放 22 |

服务器上要先有这些，路径不对时脚本会停，不会覆盖别的目录：

- 正在运行的 JAR：`/opt/warehouse/warehouse-pro/warehouse.jar`，依赖在同级 `lib/`（仓库变量 `APP_DIR`、`JAR_NAME`）
- 静态页目录：`/opt/warehouse/front`（`WEB_ROOT`）
- 重启方式：`RESTART_MODE=java`，按现有进程重新执行 `java -jar`。没有单独的 systemd 服务
- 环境文件 `/etc/pharma-ims.env`（`PHARMA_ENV_FILE`），里面有 `MYSQL_HOST`、`MYSQL_USERNAME`、`MYSQL_PASSWORD`、`JAVA_HOME`

