# 部署脚本

与 [`docs/delivery/05-运维交付包.md`](../docs/delivery/05-运维交付包.md) 一起使用。不要把数据库口令写进本目录。

| 文件 | 作用 |
|------|------|
| `nginx-https.conf.example` | `yaoheng.cloud` / `www` 的 HTTPS 配置 |
| `backup.sh` | `mysqldump` 压缩备份 |
| `restore.sh` | 从 `.sql.gz` 恢复（覆盖库） |
| `healthcheck.sh` | 前端、公钥接口、磁盘、最近备份 |
| `fix-batch-stock-menu.sql` | 补「批号库存」侧栏菜单 |

巡检时 `BASE_URL=https://yaoheng.cloud`。

## 网页前端 URL（必须相对路径）

生产 Nginx：站点根目录放 `warehouse-front/dist`，接口反代 `/warehouse/` → `127.0.0.1:8899/warehouse/`。

- 打包：`cd warehouse-front && npm ci && npm run build:prod`
- `.env.production` 里 `VITE_APP_BASE_URL=/warehouse`（**不要**写成 `http://IP:8899` 或带端口的绝对地址）
- Vite `base` 保持默认 `/`，静态资源是 `/static/...`，与 `location /` + `try_files` 一致
- 不要把 `dist` 提交进 Git；在本机或 CI 打好包后 rsync 到 `/usr/share/nginx/html`

服务器拉代码后只编后端 JAR 并重启 Java；前端仍是「本机 `build:prod` → 上传静态文件」。重启 Java 后演示数据种子会自动补信誉额等（不覆盖业务改过的同名单据）。

