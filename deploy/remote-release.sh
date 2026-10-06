#!/usr/bin/env bash
# 在服务器上执行：先备份，再换 JAR 和静态页，巡检失败则换回上一版文件。
# 数据库口令从服务器上的环境文件读取，不从 GitHub 日志传入。
set -euo pipefail

STAGE="${1:?用法: remote-release.sh <含 JAR、lib 与 dist 的目录>}"
: "${WEB_ROOT:=/opt/warehouse/front}"
: "${APP_DIR:=/opt/warehouse/warehouse-pro}"
: "${JAR_NAME:=warehouse.jar}"
: "${RESTART_MODE:=java}"
: "${PHARMA_SERVICE:=pharma-ims}"
: "${BASE_URL:=https://yaoheng.cloud}"
: "${PHARMA_ENV_FILE:=/etc/pharma-ims.env}"
: "${APP_LOG:=/opt/warehouse/app.log}"
SERVICE="$PHARMA_SERVICE"
ENV_FILE="$PHARMA_ENV_FILE"

if [[ ! -f "$STAGE/$JAR_NAME" || ! -d "$STAGE/lib" || ! -f "$STAGE/dist/index.html" ]]; then
  echo "发布目录缺少 $JAR_NAME、lib 或 dist/index.html"
  exit 1
fi

if [[ -f "$ENV_FILE" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
fi

if [[ ! -f "$APP_DIR/$JAR_NAME" ]]; then
  echo "服务器上没有 $APP_DIR/$JAR_NAME，停止发布，避免写到错误目录"
  exit 1
fi

if [[ "$RESTART_MODE" == "systemd" ]] && ! sudo systemctl cat "$SERVICE" >/dev/null 2>&1; then
  echo "systemd 服务 $SERVICE 不存在，停止发布"
  exit 1
fi

if ! command -v rsync >/dev/null 2>&1; then
  echo "服务器未安装 rsync"
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
echo "开始备份数据库"
sudo \
  MYSQL_HOST="${MYSQL_HOST:?请在 $ENV_FILE 中配置 MYSQL_HOST}" \
  MYSQL_PORT="${MYSQL_PORT:-3306}" \
  MYSQL_DB="${MYSQL_DB:-pharma_ims}" \
  MYSQL_USERNAME="${MYSQL_USERNAME:?请在 $ENV_FILE 中配置 MYSQL_USERNAME}" \
  MYSQL_PASSWORD="${MYSQL_PASSWORD:?请在 $ENV_FILE 中配置 MYSQL_PASSWORD}" \
  bash "$SCRIPT_DIR/backup.sh"

restart_app() {
  if [[ "$RESTART_MODE" == "java" ]]; then
    if [[ -f "$ENV_FILE" ]]; then
      set -a
      # shellcheck disable=SC1090
      source "$ENV_FILE"
      set +a
    fi
    export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-21}"
    export PATH="$JAVA_HOME/bin:$PATH"
    pkill -f "java .*${JAR_NAME}" || true
    sleep 2
    cd "$APP_DIR"
    nohup java -Xms256m -Xmx512m -Dspring.devtools.restart.enabled=false -jar "$APP_DIR/$JAR_NAME" >> "$APP_LOG" 2>&1 &
    sleep 1
    return 0
  fi
  sudo systemctl restart "$SERVICE"
}

PREV="$(mktemp -d /tmp/pharma-prev.XXXXXX)"
sudo cp -a "$APP_DIR/$JAR_NAME" "$PREV/$JAR_NAME"
sudo cp -a "$APP_DIR/lib" "$PREV/lib"
sudo tar -C "$WEB_ROOT" -czf "$PREV/html.tgz" .

rolled=0
rollback() {
  if [[ "$rolled" -eq 1 ]]; then
    return
  fi
  rolled=1
  echo "巡检失败，换回上一版 JAR、依赖和静态页"
  sudo cp -a "$PREV/$JAR_NAME" "$APP_DIR/$JAR_NAME"
  sudo rm -rf "$APP_DIR/lib"
  sudo cp -a "$PREV/lib" "$APP_DIR/lib"
  sudo find "$WEB_ROOT" -mindepth 1 -delete
  sudo tar -C "$WEB_ROOT" -xzf "$PREV/html.tgz"
  restart_app || true
}

echo "换上新版本"
sudo install -m 644 "$STAGE/$JAR_NAME" "$APP_DIR/$JAR_NAME"
sudo rm -rf "$APP_DIR/lib"
sudo cp -a "$STAGE/lib" "$APP_DIR/lib"
sudo rsync -a --delete "$STAGE/dist/" "$WEB_ROOT/"
restart_app
trap rollback ERR

echo "等待接口就绪"
ready=0
for _ in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18; do
  if curl -fsS -m 5 -o /dev/null "http://127.0.0.1:8899/warehouse/login/publicKey"; then
    ready=1
    break
  fi
  sleep 5
done

if [[ "$ready" -ne 1 ]]; then
  echo "接口在一分半内没有恢复"
  exit 1
fi

curl -fsS -m 15 --resolve yaoheng.cloud:443:127.0.0.1 -o /dev/null "$BASE_URL/"
curl -fsS -m 15 "http://127.0.0.1:8899/warehouse/login/publicKey" | grep -q '"code"'

trap - ERR
echo "发布完成"
rm -rf "$PREV"
