# 爆款结构复制（中印同片 · 仅字幕不同 · 音轨不变）

前提（你已确认）：同一套正片；画面仅烧录字幕差异；**音轨与中版一致**。  
因此可对**任一侧**抽音频建索引，用「中文爆款」里的音轨在原剧里定位时间码，再对**印尼轨画面**在同一时间码裁切拼接。

### 图形界面（推荐本机操作）

**桌面版 · PyQt6（与 `AI_Drama_Translator` 同栈，推荐）**

```bash
cd /path/to/素材制作工具
source venv/bin/activate   # Windows 用 venv\Scripts\activate
pip install -r viral_clone/requirements.txt   # 含 PyQt6
python viral_clone/gui_qt.py
```

布局与逻辑同旧版 tk：`原剧 / 印尼 / 爆款（可文件夹列表）/ scratch / 输出`、选项与「按镜头」分页；日志为 `QPlainTextEdit` 实时输出子进程。选项中含 **裁切 seek 回退（秒）**：**默认 0**（与已验证的 `天马1_印尼结构复制_v2` 路径一致：`ffmpeg` 单次输入侧 `-ss` + 重编码）；需要时可调到 **3** 尝试混合 seek。

**桌面版（tkinter，标准库）**

```bash
python viral_clone/gui.py
```

窗口内可浏览选择：**时间轴原剧目录**（一般中文版，亦可选与爆款同片的印尼目录建轴）、**印尼原剧目录**、**爆款单文件或整夹导入 mp4 列表**、缓存与输出路径；日志区实时显示子进程输出。第二页为「按镜头导出」进阶流程。

**若 `gui.py` 一启动就 abort，并出现** `macOS 26 (2603) or later required, have instead 16 (1603)`：  
这是 **Xcode / Command Line Tools 自带的 Python** 所带的 **Tcl/Tk** 在做版本判断时与当前 macOS 不匹配（显示逻辑亦易误导），**不是**你的项目代码问题。可选：

1. **浏览器版（推荐，零 tk 依赖）**：`python viral_clone/gui_web.py`，会在本机打开 `http://127.0.0.1:8775/`。端口可用 `--port` 或环境变量 `VIRAL_GUI_WEB_PORT`；加 `--no-browser` 则只起服务、不弹浏览器。  
2. 安装 **python.org** 官方 macOS 安装包或 **Homebrew** 的 `python`/`python-tk` 后，用该解释器重建 venv，再运行 `gui.py`。

## 你需要提供给我（跑真实业务时）

1. **中文原剧目录**：每集一个 `mp4`（或与下项同名的稳定命名）。
2. **印尼原剧目录**：与中文 **逐集文件名一致**（如 `01.mp4`↔`01.mp4`）。
3. **至少 1 条中文爆款**：`mp4`，且剪辑母带来自上述正片（未改音轨、未加速变调）。
4. **命名约定**：中英印三处同一集必须用 **同一 stem**（推荐和你现在一样：`01.mp4`）。

> 音轨不变时，建库可只用 **印尼目录** 或只用 **中文目录** 的音频二选一；爆款仍用中文画面 + 中文音去做匹配即可。

提供路径给协作者时，可复制 `paths.local.example.txt` 为 **`paths.local.txt`** 填三行绝对路径，或在聊天里直接贴三行（见项目根对话说明）。

## 本地自测（无需你额外文件）

仓库内会用 `短剧文件夹/01.mp4` 切一段「伪爆款」→ 在全目录 71 集中定位 → 在原文件上裁出同段，验证链路。

```bash
cd /path/to/素材制作工具
source venv/bin/activate
pip install -r viral_clone/requirements.txt   # 若尚未装 scipy
python viral_clone/poc_match.py --episodes-dir ./短剧文件夹 --scratch ./viral_clone/_scratch
```

## 真实爆款（可跨多集）

当爆款时长大于单集时，用**全剧按集顺序拼接音轨**再与整条爆款做互相关：

```bash
python viral_clone/match_viral_timeline.py \
  --episodes-dir /path/to/流星中文 \
  --viral /path/to/爆款.mp4 \
  --scratch ./viral_clone/_scratch_run \
  --indo-dir /path/to/流星印尼 \
  --output ./viral_clone/_scratch_run/out_id.mp4

# 每条爆款请用不同的 --output，否则后跑会覆盖先跑的成片。
# 默认：整段 NCC < --min-ncc（默认 0.30）时中止导出印尼片（退出码 7），仅写入 match_report.json，
# 避免「天马2 类」互相关≈无效仍拼出一盘错片。坚持导出可加 --force-low-ncc。
# 可选：起点在 ±6s 内步长 0.05s 再扫整段 NCC（默认 0.1s）
# python viral_clone/match_viral_timeline.py ... --refine-step-sec 0.05
```

生成 `match_report.json`。**裁切默认用重编码**；重编码分段默认 **`--cut-seek-margin 0`**：与已跑通的天马1 印尼成片（`v2`）一致，即 **单次输入侧 `-ss`** 后重编码。若个别素材入点仍晃，可试 **`--cut-seek-margin 3`**（先回退再解码流精切）。若加 `--stream-copy` 会快很多，但 **流复制只能从关键帧起剪**，偏差可能更大。

**PyQt6 界面**：若列表/日志在 macOS 上仍是白字白底，请更新到当前 `gui_qt.py`（已强制调色板 + 样式表）。

同源连续剪辑整段 NCC 通常 **≥0.85**；明显偏低请人工对画面与听感。  
终端会先打 **爆款文件名**；若 **NCC 远低于阈值**，导出会被拦下（见上 `--min-ncc`）。

### 看起来「卡住」时

- **77 集抽音轨**：终端会打印 `音轨 [k/77] …`，每集一次 ffmpeg，整段可能要几分钟，属正常。
- **互相关**：默认 **降采样粗搜 + 窄窗精搜**，避免一次性分配千万级 `correlate` 输出（旧逻辑易造成内存飙高、机器像死机）。若需与旧版完全一致可 `--full-correlate`（不推荐）。
- **多段 libx264 重编码**：会打印 `导出印尼片段 [i/n] …`，每段可能要几分钟。
- 互相关窗宽可调：`--correlate-pad-sec`（默认 45）、粗采样 `--coarse-decim`（默认 8）。若怀疑粗峰偏了可多留窗 `--correlate-pad-sec 90`。
- **`concat` 报错或音画异常**：对主流程加 `--reencode-concat`，整轨再编一次 mux。

### 按镜头裁印尼（可选，对齐《爆款复制提示》Step2）

前提：已用 **`match_viral_timeline.py` 生成 `match_report.json`**（内含 `episode_timeline`；旧报告需重跑一次匹配）。

1. 爆款镜头表：

```bash
python viral_clone/scene_detect_viral.py \
  --video /path/to/爆款.mp4 \
  --scratch ./viral_clone/_scenes
# 得到 ./_scenes/<stem>_scenes.json
```

2. 按镜头顺序裁印尼（时间轴仍来自整段爆款互相关；每镜 = 爆款上 `[start_sec,end_sec)` 平移到原剧）：

```bash
python viral_clone/indo_from_scenes.py \
  --match-report ./viral_clone/_scratch_run/match_report.json \
  --scenes ./viral_clone/_scenes/爆款名_scenes.json \
  --indo-dir /path/to/流星印尼 \
  --output ./viral_clone/_scratch_run/out_by_scenes.mp4 \
  --scratch ./viral_clone/_scratch_indo_scenes

# 与主流程一致，默认 --cut-seek-margin 0；需要时可改为 3
```

`ContentDetector` 的 `--threshold`（默认 27）、`--min-scene-len` 可按素材快慢切微调。

## 后续（未在你本次任务中实现）

- 逐镜头 **独立指纹/互相关**（跳剪爆款）；当前按镜切仅拆分导出顺序，时间轴仍是一套全局 `timeline_start_sample`。
- 指纹库（audfprint/dejavu）在长素材、多剧复用时再接入；PoC 用互相关已可验证「同音轨可平移时间码」。
