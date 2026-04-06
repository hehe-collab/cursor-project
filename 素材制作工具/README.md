# 短剧素材制作工具

> 通过完整短剧剧集，AI 自动制作可用于 TikTok 投放的视频广告素材。

---

## 项目目标

- **输入**：原汁原味的多集短剧（每集 1–3 分钟）
- **输出**：约 20 条广告素材（40% 为 15–20 分钟，60% 为 20–30 分钟），可直接投放 TikTok

---

## 需求规格（已确认）

| 维度 | 说明 |
|------|------|
| 单集时长 | 1–3 分钟 |
| 素材用途 | 广告展示 |
| 剧情连贯 | 按顺序，能看懂，前面可加钩子 |
| 产出数量 | 约 20 条 |
| 内容占比 | 60–70% 围绕 1–2 个起量片段，30–40% 其他高能内容（降低相似度） |
| 重复允许 | 同一高能片段可出现在多条素材 |

---

## 单条素材结构

```
[钩子 可选] + [主体内容 按顺序]
```

- **钩子**：几秒–几十秒，从剧内其他高能点剪
- **主体**：15–30 分钟（可配置），围绕高能片段前后扩展，保证剧情连贯

---

## AI 处理流程

```
阶段1：全剧分析
├── 每集/每场景打分（冲突、悬念、情绪）
├── 输出：1–2 个核心起量片段
├── 输出：3–5 个次高能片段
└── 输出：5–10 个候选钩子

阶段2：生成 20 条组合方案
├── 12–14 条：围绕起量片段
├── 6–8 条：围绕次高能/其他内容
└── （可选）印尼语 SRT：主体以字幕块为边界，再套时长目标；无字幕则按场景切分

阶段3：批量裁剪拼接
└── FFmpeg 输出成片
```

---

## 技术栈

- Python 3.10+
- PySceneDetect（场景切分）
- FFmpeg（裁剪拼接）
- OpenAI API（GPT-4o 分析，可在 config 中改）

---

## 项目结构

```
素材制作工具/
├── README.md           # 本文件
├── 需求规格.md          # 详细需求
├── requirements.txt   # 依赖
├── src/
│   ├── scene_detect.py   # 场景切分
│   ├── srt_parse.py       # 解析 SRT
│   ├── subtitle_plan.py   # 字幕块 + 主体选片
│   ├── analyze.py       # AI 分析
│   ├── plan.py          # 生成 20 条方案
│   └── export.py        # 裁剪拼接
└── config.yaml         # 配置
```

---

## 使用方式

```bash
# 1. 创建虚拟环境并安装依赖
python3 -m venv venv
source venv/bin/activate   # Windows: venv\Scripts\activate
pip install -r requirements.txt

# 2. 配置 OpenAI API Key
cp config.example.yaml config.yaml
# 编辑 config.yaml 填入 api_key，或设置环境变量 OPENAI_API_KEY

# 3. 分析一部剧（输入：剧集目录）
python src/analyze.py --input /path/to/drama_episodes --output analysis.json

# 测试时限制集数：--max-episodes 2
python src/analyze.py -i /path/to/episodes -o analysis.json -n 2

# 4. 生成 20 条组合方案（必须基于最新 analysis.json）
#    若增加剧集后重新 analyze，务必重新执行 plan，否则素材时长会偏短
python src/plan.py --input analysis.json --output plan.json

# 命令行覆盖时长：--short-ratio 0.4 --short-min 15 --short-max 20 --long-min 20 --long-max 30
# 或 --materials 20 指定素材数量
# 主体时长：默认在配置的短/长区间内「随机目标」（避免条条顶格）；旧行为：--duration-mode max
# 随机抽样默认偏区间上半段（尽量不偏短）：config 里 duration_target_bias: upper；全区间均匀：uniform
# 可复现随机：--random-seed 42（配合 random 模式）
# 字幕（印尼语 SRT，与视频同集文件名：1.mp4 ↔ 1.srt）
# 在 config.yaml 中设置 subtitles.enabled: true 与 subtitles.dir: 字幕目录
# 规划模式：subtitle_first（默认，有字幕则字幕块优先）| scene_only
# 仅场景：python src/plan.py -i analysis.json -o plan.json --plan-mode scene_only

# 5. 导出成片（需安装 ffmpeg）
python src/export.py --plan plan.json --output ./output
```

或使用 `run.sh`（会自动创建 venv 并安装依赖）：

```bash
./run.sh analyze /path/to/episodes
./run.sh plan analysis.json
```

---

*创建时间：2026年3月9日*
