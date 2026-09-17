# 01 生产形态：独立域名 + HTTPS

## 结论

当前公网 `http://123.57.134.57` **不能作为生产交付**。

实测（2026-09-14 16:52 CST）：

| 检查 | 结果 |
|------|------|
| `http://123.57.134.57/` | nginx/1.24.0，HTTP/1.1 200 |
| `https://123.57.134.57/` | **无响应**（443 未开通） |
| 登录页 | 账号 `admin` / `123456` 可登录超管 |
| 接口 | `/warehouse/*` 走同一明文 HTTP |

问题不在「能不能打开」，而在：

1. **明文传输**：口令虽经 RSA 封装再提交，信道仍是 HTTP，会话 Cookie 可被中间人截取。
2. **弱口令长期有效**：超管与采购/仓管/销售/财务种子账号口令同为 `123456`。
3. **无独立域名**：无法备案、无法给证书、无法在微信等环境按正式站点使用。

登录传输加密 ≠ HTTPS。前者只保护 POST 里的密码字段，不保护 Cookie、接口 JSON、追溯码与单据。

## 生产开通门槛（全部满足才算交付）

1. 备案域名 **`yaoheng.cloud`**（`www` 已解析到同一主机）。
2. DNS A 记录已指向 `123.57.134.57`。
3. 安全组放行 **TCP 443**，证书用 Let's Encrypt 签发；HTTP 仅做 301 跳转。
4. 关闭或重定向 IP 直连；HSTS 开启。
5. 按 06 完成岗位账号，**废止**长期超管弱口令。

**域名已确认：** `yaoheng.cloud` / `www.yaoheng.cloud` 的 A 记录均为 `123.57.134.57`（阿里云万网 DNS）。HTTP 用域名已能打开站点；**443 仍无响应**，证书尚未签发。剩余工作在云主机安全组放行 443 后执行下面命令。

## 推荐 Nginx（与现网一致：静态页 + `/warehouse` 反代）

完整示例：`deploy/nginx-https.conf.example`。要点：

- `listen 80` 仅 ACME 与跳转 HTTPS
- `listen 443 ssl`；TLS 1.2+；HSTS
- `location /warehouse/` → `http://127.0.0.1:8899/warehouse/`
- 前端 `try_files` 回 `index.html`

证书申请（在云主机上执行，先开 443）：

```bash
# 先临时让 80 能完成 ACME 校验（不要先把 80 全部 301 掉，或保留 acme-challenge）
mkdir -p /var/www/certbot
apt update && apt install -y certbot python3-certbot-nginx
certbot --nginx -d yaoheng.cloud -d www.yaoheng.cloud
nginx -t && systemctl reload nginx
```

签发成功后对外入口为 `https://yaoheng.cloud`。`www` 与裸域 HTTP 均跳转到该地址。

回滚：恢复 80 端口旧配置并 `nginx -s reload`（见 05）。
