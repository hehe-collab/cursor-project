#!/usr/bin/env python3
"""
爆款复制 · 浏览器界面（仅依赖 Python 标准库，不加载 tkinter）。
适用于 macOS 上 Apple 自带 Python/Tcl 报「macOS xx required」导致 tkinter 直接 abort 的环境。

用法：
  cd 素材制作工具 && source venv/bin/activate
  python viral_clone/gui_web.py

浏览器访问 http://127.0.0.1:8775/

可选参数：`--port 8775`、`--no-browser`（不自动打开浏览器）。亦可用环境变量 `VIRAL_GUI_WEB_PORT`。
"""
from __future__ import annotations

import argparse
import json
import subprocess
import sys
import threading
import time
import webbrowser
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from typing import Any, Dict, List, Optional
from urllib.parse import urlparse

TOOL_ROOT = Path(__file__).resolve().parent.parent
VIRAL_CLONE = TOOL_ROOT / "viral_clone"
DEFAULT_PORT = 8775

_state_lock = threading.Lock()
_state: Dict[str, Any] = {
    "running": False,
    "log": "",
    "exit_code": None,
}


def _append_log(text: str) -> None:
    with _state_lock:
        _state["log"] += text


def _reset_job() -> None:
    with _state_lock:
        _state["log"] = ""
        _state["exit_code"] = None


def _set_running(v: bool) -> None:
    with _state_lock:
        _state["running"] = v


def _snapshot() -> Dict[str, Any]:
    with _state_lock:
        return {
            "running": _state["running"],
            "log": _state["log"],
            "exit_code": _state["exit_code"],
        }


def _run_proc(cmd: List[str]) -> None:
    _set_running(True)
    _append_log("$ " + " ".join(cmd) + "\n")
    try:
        env = dict(**__import__("os").environ)
        env.setdefault("PYTHONUTF8", "1")
        p = subprocess.Popen(
            cmd,
            cwd=str(TOOL_ROOT),
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            bufsize=1,
            env=env,
        )
        assert p.stdout is not None
        for line in p.stdout:
            _append_log(line)
        p.wait()
        with _state_lock:
            _state["exit_code"] = p.returncode
        _append_log(f"\n[退出码 {p.returncode}]\n")
    except Exception as e:
        _append_log(f"\n[异常] {e}\n")
        with _state_lock:
            _state["exit_code"] = -1
    finally:
        _set_running(False)


def _build_match_cmd(body: Dict[str, Any]) -> Optional[List[str]]:
    ep = (body.get("episodes_dir") or "").strip()
    vir = (body.get("viral") or "").strip()
    scratch = (body.get("scratch") or "").strip()
    if not ep or not Path(ep).is_dir():
        return None
    if not vir or not Path(vir).is_file():
        return None
    if not scratch:
        return None
    Path(scratch).mkdir(parents=True, exist_ok=True)

    cmd: List[str] = [
        sys.executable,
        str(VIRAL_CLONE / "match_viral_timeline.py"),
        "--episodes-dir",
        ep,
        "--viral",
        vir,
        "--scratch",
        scratch,
    ]
    export = body.get("export_indo", True)
    if export:
        indo = (body.get("indo_dir") or "").strip()
        out = (body.get("output") or "").strip()
        if not indo or not Path(indo).is_dir():
            return None
        if not out:
            return None
        cmd.extend(["--indo-dir", indo, "--output", out])
    if body.get("stream_copy"):
        cmd.append("--stream-copy")
    if body.get("reencode_concat"):
        cmd.append("--reencode-concat")
    if body.get("no_refine"):
        cmd.append("--no-refine-lag")
    if export and body.get("force_low_ncc"):
        cmd.append("--force-low-ncc")
    cm = body.get("cut_seek_margin")
    if cm is not None and cm != "":
        try:
            cmd.extend(["--cut-seek-margin", str(float(cm))])
        except (TypeError, ValueError):
            pass
    return cmd


def _build_scene_cmd(body: Dict[str, Any]) -> Optional[List[str]]:
    v = (body.get("viral") or "").strip()
    sd = (body.get("scene_detect_dir") or "").strip()
    if not v or not Path(v).is_file():
        return None
    if not sd:
        return None
    Path(sd).mkdir(parents=True, exist_ok=True)
    return [
        sys.executable,
        str(VIRAL_CLONE / "scene_detect_viral.py"),
        "--video",
        v,
        "--scratch",
        sd,
    ]


def _build_indo_scenes_cmd(body: Dict[str, Any]) -> Optional[List[str]]:
    rpt = (body.get("match_report") or "").strip()
    scn = (body.get("scenes_json") or "").strip()
    indo = (body.get("indo_dir") or "").strip()
    out = (body.get("output") or "").strip()
    scratch = (body.get("scratch_segments") or "").strip()
    if not Path(rpt).is_file() or not Path(scn).is_file():
        return None
    if not indo or not Path(indo).is_dir():
        return None
    if not out or not scratch:
        return None
    Path(scratch).mkdir(parents=True, exist_ok=True)
    cmd: List[str] = [
        sys.executable,
        str(VIRAL_CLONE / "indo_from_scenes.py"),
        "--match-report",
        rpt,
        "--scenes",
        scn,
        "--indo-dir",
        indo,
        "--output",
        out,
        "--scratch",
        scratch,
    ]
    if body.get("stream_copy"):
        cmd.append("--stream-copy")
    if body.get("reencode_concat"):
        cmd.append("--reencode-concat")
    cm = body.get("cut_seek_margin")
    if cm is not None and cm != "":
        try:
            cmd.extend(["--cut-seek-margin", str(float(cm))])
        except (TypeError, ValueError):
            pass
    return cmd


HTML = """<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1"/>
<title>爆款复制 · Web</title>
<style>
  body{font-family:system-ui,-apple-system,sans-serif;margin:16px;max-width:920px;background:#fafafa;color:#111}
  h1{font-size:1.25rem}
  section{background:#fff;border:1px solid #ddd;border-radius:8px;padding:14px;margin-bottom:16px}
  label{display:block;font-weight:600;margin:8px 0 4px;font-size:.9rem}
  input[type=text],input[type=password]{width:100%;box-sizing:border-box;padding:8px;border:1px solid #ccc;border-radius:4px}
  .row{display:flex;gap:12px;flex-wrap:wrap;align-items:center;margin:6px 0}
  button{background:#0a58ca;color:#fff;border:0;padding:10px 16px;border-radius:6px;cursor:pointer;font-size:.95rem}
  button:disabled{opacity:.55;cursor:not-allowed}
  pre#log{white-space:pre-wrap;background:#1e1e1e;color:#e0e0e0;padding:12px;border-radius:6px;min-height:220px;max-height:480px;overflow:auto;font-size:12px}
  .hint{color:#666;font-size:.85rem;margin:4px 0 0}
  nav a{margin-right:14px;color:#0a58ca}
</style>
</head>
<body>
<h1>爆款复制 · 浏览器界面</h1>
<p class="hint">不依赖 tkinter。若桌面版 <code>gui.py</code> 报 macOS 版本后 abort，请用本页。</p>
<nav>
  <a href="#main">主流程</a>
  <a href="#advanced">进阶 · 按镜头</a>
</nav>

<section id="main">
  <h2>主流程：匹配 + 导出</h2>
  <label>时间轴原剧目录（建议中文版）</label>
  <input type="text" id="episodes_dir" placeholder="/path/to/流星中文"/>
  <label>印尼原剧目录</label>
  <input type="text" id="indo_dir" placeholder="/path/to/流星印尼"/>
  <label>爆款视频 mp4（完整路径）</label>
  <input type="text" id="viral" placeholder="/path/to/爆款.mp4"/>
  <label>scratch 缓存目录</label>
  <input type="text" id="scratch" value="__SCRATCH_DEFAULT__"/>
  <label>印尼成片输出 .mp4</label>
  <input type="text" id="output" placeholder="自动：scratch 下 文件名_indo.mp4 可运行前手写"/>
  <div class="row">
    <label><input type="checkbox" id="export_indo" checked/> 导出印尼成片</label>
    <label><input type="checkbox" id="stream_copy"/> 分段流复制</label>
    <label><input type="checkbox" id="reencode_concat"/> 合并整轨重编码</label>
    <label><input type="checkbox" id="no_refine"/> 关闭起点细搜</label>
    <label><input type="checkbox" id="force_low_ncc"/> NCC 过低仍导出（不推荐）</label>
  </div>
  <button type="button" id="btn_match">开始运行</button>
  <span id="status" class="hint"></span>
</section>

<section id="advanced">
  <h2>进阶：镜头检测 / 按镜头导出</h2>
  <label>match_report.json</label>
  <input type="text" id="match_report" value="__REPORT_DEFAULT__"/>
  <label>镜头检测输出目录</label>
  <input type="text" id="scene_detect_dir" value="__SCENES_DIR_DEFAULT__"/>
  <label>镜头检测用爆款 mp4</label>
  <input type="text" id="viral_scene" placeholder="可与主流程爆款相同"/>
  <button type="button" id="btn_scene">仅运行镜头检测</button>
  <hr style="margin:16px 0;border:0;border-top:1px solid #eee"/>
  <label>导引用 · 镜头表 JSON</label>
  <input type="text" id="scenes_json" placeholder="/path/to/xxx_scenes.json"/>
  <label>片段缓存 scratch</label>
  <input type="text" id="scratch_segments" value="__SEG_DEFAULT__"/>
  <label>按镜头导出 · 输出 mp4</label>
  <input type="text" id="output2" placeholder="/path/to/out_by_scenes.mp4"/>
    <label>印尼原剧目录（与主流程可相同）</label>
  <input type="text" id="indo_dir2" placeholder="/path/to/流星印尼"/>
  <div class="row">
    <label><input type="checkbox" id="sc_stream"/> 分段流复制</label>
    <label><input type="checkbox" id="sc_reenc"/> 合并整轨重编码</label>
  </div>
  <button type="button" id="btn_indo_scenes">按镜头导出印尼成片</button>
</section>

<section>
  <h2>日志</h2>
  <pre id="log"></pre>
</section>

<script>
const SCRATCH_DEFAULT="__SCRATCH_DEFAULT__";
const POLL_MS=400;
function jsonBool(id){ const e=document.getElementById(id); return !!(e&&e.checked); }
async function pollLog(){
  const r=await fetch('/api/status'); const j=await r.json();
  document.getElementById('log').textContent=j.log||'';
  document.getElementById('btn_match').disabled=j.running;
  document.getElementById('btn_scene').disabled=j.running;
  document.getElementById('btn_indo_scenes').disabled=j.running;
  const st=document.getElementById('status');
  st.textContent=j.running?'运行中…':(j.exit_code!==null?'上次退出码 '+j.exit_code:'就绪');
}
setInterval(pollLog,POLL_MS); pollLog();

document.getElementById('btn_match').onclick=async()=>{
  const body={
    episodes_dir:document.getElementById('episodes_dir').value.trim(),
    indo_dir:document.getElementById('indo_dir').value.trim(),
    viral:document.getElementById('viral').value.trim(),
    scratch:document.getElementById('scratch').value.trim()||SCRATCH_DEFAULT,
    output:document.getElementById('output').value.trim(),
    export_indo:jsonBool('export_indo'),
    stream_copy:jsonBool('stream_copy'),
    reencode_concat:jsonBool('reencode_concat'),
    no_refine:jsonBool('no_refine'),
    force_low_ncc:jsonBool('force_low_ncc'),
  };
  const r=await fetch('/api/match',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(body)});
  const t=await r.text();
  if(!r.ok)alert('请求失败: '+t);
};
document.getElementById('btn_scene').onclick=async()=>{
  const body={
    viral:(document.getElementById('viral_scene').value.trim()||document.getElementById('viral').value.trim()),
    scene_detect_dir:document.getElementById('scene_detect_dir').value.trim(),
  };
  const r=await fetch('/api/scene',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(body)});
  if(!r.ok)alert(await r.text());
};
document.getElementById('btn_indo_scenes').onclick=async()=>{
  const body={
    match_report:document.getElementById('match_report').value.trim(),
    scenes_json:document.getElementById('scenes_json').value.trim(),
    indo_dir:document.getElementById('indo_dir2').value.trim(),
    output:document.getElementById('output2').value.trim(),
    scratch_segments:document.getElementById('scratch_segments').value.trim(),
    stream_copy:jsonBool('sc_stream'),
    reencode_concat:jsonBool('sc_reenc'),
  };
  const r=await fetch('/api/indo_scenes',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(body)});
  if(!r.ok)alert(await r.text());
};
</script>
</body>
</html>
"""


class Handler(BaseHTTPRequestHandler):
    def log_message(self, fmt: str, *args: Any) -> None:
        pass

    def _send(self, code: int, body: bytes, ctype: str) -> None:
        self.send_response(code)
        self.send_header("Content-Type", ctype + "; charset=utf-8")
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self) -> None:
        pu = urlparse(self.path)
        if pu.path == "/api/status":
            snap = _snapshot()
            self._send(200, json.dumps(snap, ensure_ascii=False).encode("utf-8"), "application/json")
            return
        if pu.path == "/" or pu.path == "/index.html":
            html = HTML.replace(
                "__SCRATCH_DEFAULT__",
                str(TOOL_ROOT / "viral_clone" / "_scratch_gui").replace("\\", "/"),
            )
            html = html.replace(
                "__REPORT_DEFAULT__",
                str(TOOL_ROOT / "viral_clone" / "_scratch_gui" / "match_report.json").replace(
                    "\\", "/"
                ),
            )
            html = html.replace(
                "__SCENES_DIR_DEFAULT__",
                str(TOOL_ROOT / "viral_clone" / "_scenes").replace("\\", "/"),
            )
            html = html.replace(
                "__SEG_DEFAULT__",
                str(TOOL_ROOT / "viral_clone" / "_scratch_indo_scenes").replace("\\", "/"),
            )
            self._send(200, html.encode("utf-8"), "text/html")
            return
        self._send(404, b"not found", "text/plain")

    def do_POST(self) -> None:
        pu = urlparse(self.path)
        length = int(self.headers.get("Content-Length", "0") or "0")
        raw = self.rfile.read(length) if length > 0 else b"{}"
        try:
            body = json.loads(raw.decode("utf-8"))
        except json.JSONDecodeError:
            self._send(400, b"invalid json", "text/plain")
            return

        if pu.path == "/api/match":
            if _snapshot()["running"]:
                self._send(409, b"busy", "text/plain")
                return
            cmd = _build_match_cmd(body)
            if cmd is None:
                self._send(
                    400,
                    b"missing or invalid episodes_dir / viral / scratch, or export fields",
                    "text/plain",
                )
                return
            _reset_job()
            threading.Thread(target=_run_proc, args=(cmd,), daemon=True).start()
            self._send(200, b"ok", "text/plain")
            return

        if pu.path == "/api/scene":
            if _snapshot()["running"]:
                self._send(409, b"busy", "text/plain")
                return
            cmd = _build_scene_cmd(body)
            if cmd is None:
                self._send(400, b"invalid viral or scene_detect_dir", "text/plain")
                return
            _reset_job()
            threading.Thread(target=_run_proc, args=(cmd,), daemon=True).start()
            self._send(200, b"ok", "text/plain")
            return

        if pu.path == "/api/indo_scenes":
            if _snapshot()["running"]:
                self._send(409, b"busy", "text/plain")
                return
            cmd = _build_indo_scenes_cmd(body)
            if cmd is None:
                self._send(400, b"invalid report/scenes/indo/output/scratch", "text/plain")
                return
            _reset_job()
            threading.Thread(target=_run_proc, args=(cmd,), daemon=True).start()
            self._send(200, b"ok", "text/plain")
            return

        self._send(404, b"not found", "text/plain")


def main() -> None:
    import os

    ap = argparse.ArgumentParser(description="爆款复制 · 浏览器界面（无 tkinter）")
    ap.add_argument(
        "--port",
        type=int,
        default=None,
        help=f"监听端口（默认 {DEFAULT_PORT}，或环境变量 VIRAL_GUI_WEB_PORT）",
    )
    ap.add_argument(
        "--no-browser",
        action="store_true",
        help="启动后不自动打开系统浏览器",
    )
    args = ap.parse_args()
    port = args.port
    if port is None:
        port = int(os.environ.get("VIRAL_GUI_WEB_PORT", str(DEFAULT_PORT)))
    server = ThreadingHTTPServer(("127.0.0.1", port), Handler)
    th = threading.Thread(target=server.serve_forever, daemon=True)
    th.start()
    url = f"http://127.0.0.1:{port}/"
    print(f"爆款复制 Web UI：{url}", flush=True)
    print("仅监听本机 127.0.0.1；按 Ctrl+C 退出", flush=True)
    if not args.no_browser:
        webbrowser.open(url)
    try:
        while True:
            time.sleep(0.5)
    except KeyboardInterrupt:
        print("\n退出中…", flush=True)
        server.shutdown()


if __name__ == "__main__":
    main()
