#!/usr/bin/env bash
# Drama Admin: 前端发版脚本
# 创建于 2026-04-20
# 作用与 deploy-backend.sh 对齐，只是部署的是 vite dist 静态目录
#
# 用法：
#   deploy-frontend.sh <new-dist-dir>   # 部署新 dist（含自动回滚）
#   deploy-frontend.sh --check          # 检查环境
#   deploy-frontend.sh --rollback       # 手动回滚到上一个版本
#   deploy-frontend.sh --list           # 列出可回滚的备份
#
# 流程：
#   1. 校验新 dist 目录（必须包含 index.html）
#   2. 备份当前 /data/web/dist → /data/backup/web/dist_YYYYMMDD_HHMMSS
#   3. rsync 新 dist → /data/web/dist
#   4. nginx -t（校验配置）
#   5. nginx -s reload（不中断现有连接）
#   6. curl 校验 /index.html 200
#   7. 失败自动回滚（rsync 备份回原位 + reload）
#
# 注意：本脚本部署到 ECS 的 /data/scripts/ 下，由 deploy 用户执行；
# 涉及 nginx -t / reload 需要 sudo（已配 visudo NOPASSWD）。

set -uo pipefail

DIST_PATH=/data/web/dist
BACKUP_DIR=/data/backup/web
LOG_FILE=/data/logs/deploy-frontend.log
HEALTH_URL=https://admin.hookedshorts.com/index.html
HEALTH_TIMEOUT=20
HEALTH_INTERVAL=2
KEEP_BACKUPS=10

mkdir -p "$BACKUP_DIR"
mkdir -p "$(dirname "$LOG_FILE")"

log()  { echo "[$(date '+%F %T')] $*" | tee -a "$LOG_FILE"; }
fail() { log "❌ $*"; exit 1; }
ok()   { log "✅ $*"; }

usage() {
  sed -n '4,16p' "$0" | sed 's/^# \?//'
  exit 0
}

check_environment() {
  log "------ 环境检查 ------"
  [[ -d "$DIST_PATH" ]] || fail "当前 dist 不存在: $DIST_PATH"
  [[ -f "$DIST_PATH/index.html" ]] || fail "当前 dist/index.html 不存在: $DIST_PATH"
  log "  当前 dist: $DIST_PATH ($(du -sh "$DIST_PATH" 2>/dev/null | cut -f1))"

  command -v nginx >/dev/null || fail "nginx 未安装"
  command -v rsync >/dev/null || fail "rsync 未安装"
  command -v curl  >/dev/null || fail "curl 未安装"

  log "  备份目录: $BACKUP_DIR ($(ls -1 "$BACKUP_DIR" 2>/dev/null | wc -l) 份历史)"
  log "  健康端点: $HEALTH_URL"
  ok "环境检查通过"
}

list_backups() {
  log "------ 可回滚的 dist 备份 ------"
  if ! ls -dt "$BACKUP_DIR"/dist_* 2>/dev/null | head -20 | while read -r d; do
    printf "  %s  %s  %s\n" \
      "$(date -r "$d" '+%F %H:%M')" \
      "$(du -sh "$d" 2>/dev/null | cut -f1)" \
      "$(basename "$d")"
  done; then
    log "  (暂无备份)"
  fi
}

validate_new_dist() {
  local d=$1
  [[ -d "$d" ]] || fail "新 dist 目录不存在: $d"
  [[ -f "$d/index.html" ]] || fail "新 dist/index.html 不存在（不像合法 vite 产物）: $d"
  local size; size=$(du -sb "$d" 2>/dev/null | cut -f1)
  (( size > 100 * 1024 )) || fail "新 dist 太小（< 100KB），不像合法构建: $d ($size bytes)"
  ok "新 dist 校验通过 ($(du -sh "$d" | cut -f1))"
}

reload_nginx() {
  log "校验 nginx 配置..."
  local nginx_t_out
  nginx_t_out=$(sudo nginx -t 2>&1) || true
  echo "$nginx_t_out" | tee -a "$LOG_FILE" >/dev/null
  if echo "$nginx_t_out" | grep -E "syntax is ok|test is successful" >/dev/null; then
    ok "nginx -t 通过"
  else
    log "nginx -t 输出: $nginx_t_out"
    fail "nginx -t 失败"
  fi

  log "reload nginx（不中断现有连接）..."
  local reload_out
  reload_out=$(sudo nginx -s reload 2>&1) || true
  echo "$reload_out" | tee -a "$LOG_FILE" >/dev/null
  # nginx -s reload 成功时无输出；失败时 stderr 非空且 exit 非零
  if [[ -z "$reload_out" ]] || echo "$reload_out" | grep -E "signal process started" >/dev/null; then
    ok "nginx reload 成功"
  else
    log "nginx reload 输出: $reload_out"
    fail "nginx reload 失败"
  fi
  sleep 1
}

wait_healthy() {
  local elapsed=0
  log "等待 $HEALTH_URL 200（最多 ${HEALTH_TIMEOUT}s）..."
  while (( elapsed < HEALTH_TIMEOUT )); do
    local code; code=$(curl -s -o /dev/null -w '%{http_code}' -m 5 "$HEALTH_URL" 2>/dev/null || echo "000")
    if [[ "$code" == "200" ]]; then
      ok "健康检查通过 (${elapsed}s, HTTP 200)"
      return 0
    fi
    sleep "$HEALTH_INTERVAL"
    elapsed=$(( elapsed + HEALTH_INTERVAL ))
    printf "  ... %ds elapsed (HTTP %s)\n" "$elapsed" "$code" | tee -a "$LOG_FILE"
  done
  log "❌ 健康检查超时（${HEALTH_TIMEOUT}s）"
  return 1
}

deploy() {
  local NEW_DIST=$1
  log ""
  log "============================================================"
  log "  前端部署开始 @ $(date '+%F %T')"
  log "  新 dist: $NEW_DIST"
  log "============================================================"

  check_environment
  validate_new_dist "$NEW_DIST"

  local STAMP; STAMP=$(date +%Y%m%d_%H%M%S)
  local BAK="$BACKUP_DIR/dist_${STAMP}"
  log "备份当前 dist -> $BAK"
  cp -a "$DIST_PATH" "$BAK" || fail "备份失败"
  ok "备份成功 ($(du -sh "$BAK" | cut -f1))"

  log "rsync 新 dist 到 $DIST_PATH..."
  if rsync -a --delete "$NEW_DIST/" "$DIST_PATH/"; then
    ok "rsync 成功"
  else
    log "rsync 失败，回滚 dist..."
    rsync -a --delete "$BAK/" "$DIST_PATH/" || fail "回滚也失败！dist 可能损坏"
    fail "rsync 失败已回滚"
  fi

  reload_nginx

  if wait_healthy; then
    log ""
    log "🎉 前端部署成功！"
    log "  新版本生效: $(stat -c %y "$DIST_PATH/index.html" | cut -d. -f1)"
    log "  备份位置: $BAK"

    local cnt; cnt=$(ls -1d "$BACKUP_DIR"/dist_* 2>/dev/null | wc -l)
    if (( cnt > KEEP_BACKUPS )); then
      log "清理多余备份（保留最近 ${KEEP_BACKUPS} 份）..."
      ls -dt "$BACKUP_DIR"/dist_* | tail -n "+$((KEEP_BACKUPS+1))" | while read -r d; do
        log "  删除旧备份: $(basename "$d")"
        rm -rf "$d"
      done
    fi
    log "============================================================"
    return 0
  else
    log ""
    log "🚨 前端部署失败！开始自动回滚..."
    log "  恢复 dist: $BAK -> $DIST_PATH"
    rsync -a --delete "$BAK/" "$DIST_PATH/" || fail "回滚也失败！请人工介入"
    reload_nginx
    if wait_healthy; then
      ok "回滚成功"
      log "============================================================"
      exit 2
    else
      fail "回滚后仍不健康！请立即排查 nginx error log"
    fi
  fi
}

rollback() {
  log ""
  log "============================================================"
  log "  手动回滚 @ $(date '+%F %T')"
  log "============================================================"
  check_environment

  local LATEST_BAK
  LATEST_BAK=$(ls -dt "$BACKUP_DIR"/dist_* 2>/dev/null | head -1)
  [[ -n "$LATEST_BAK" ]] || fail "没有可回滚的备份"

  log "将回滚到: $LATEST_BAK"
  log "  备份时间: $(stat -c %y "$LATEST_BAK" | cut -d. -f1)"

  read -r -p "确认回滚？(y/N) " ans
  [[ "$ans" == "y" || "$ans" == "Y" ]] || { log "已取消"; exit 0; }

  rsync -a --delete "$LATEST_BAK/" "$DIST_PATH/" || fail "rsync 失败"
  ok "dist 已恢复"
  reload_nginx

  if wait_healthy; then
    ok "回滚成功"
    log "============================================================"
  else
    fail "回滚后仍不健康！"
  fi
}

case "${1:-}" in
  ""|-h|--help)  usage ;;
  --check)       check_environment; list_backups ;;
  --list)        list_backups ;;
  --rollback)    rollback ;;
  *)             deploy "$1" ;;
esac
