# 不力管理系统 - 数据分析工具

短剧出海后台导出数据的**解析**工具。你在后台筛选并导出 xlsx 后，用本工具解析为结构化数据，便于后续分析。

## 📁 项目结构

```
不力管理系统数据分析工具/
├── README.md                 # 本文件
├── 剧名列表.xlsx             # 剧名配置（推荐，用 Excel 编辑）
├── config.yaml               # 分析配置（备选）
├── run_parse.sh              # 解析 xlsx → CSV
├── run_analyze.sh            # 剧分析：每日消耗与利润
├── 数据分析工具规划建议.md    # 规划与方向建议
├── requirements.txt          # Python 依赖
├── .venv/                    # 虚拟环境
├── data/                     # xlsx、CSV、分析结果表格与图表
└── src/
    ├── parse_export.py       # 解析 xlsx → CSV
    └── analyze.py            # 剧分析脚本
```

## 🔗 数据来源

- **后台**：https://adminxyz.dramahub8.com/statistics
- **操作**：在后台按需筛选（日期、国家、推广等）→ 点击「导出」
- **格式**：.xlsx

## 🚀 快速开始

```bash
cd "/Volumes/存钱罐/cursor项目/不力管理系统数据分析工具"
bash setup_venv.sh   # 首次使用或项目移动后需运行
```

### 解析 xlsx

```bash
# 方式一：用 run_parse.sh（推荐，无需激活 venv）
bash run_parse.sh /path/to/export.xlsx
bash run_parse.sh                    # 解析 data/ 下最新的 xlsx
bash run_parse.sh --profile 周投放数据  # 按数据类型规则解析（默认即周投放数据）

# 方式二：激活 venv 后用 python3
source .venv/bin/activate
python3 src/parse_export.py /path/to/export.xlsx
python3 src/parse_export.py
```

**数据类型规则（--profile）**：不同文件类型使用不同解析规则。当前支持 `周投放数据`（三投放组 ABC、印尼+泰国，自动提取组别、国家、剧名纯并过滤无效行）。

### 指定输出路径

```bash
bash run_parse.sh export.xlsx -o result.csv
# 或
python3 src/parse_export.py export.xlsx -o result.csv
```

## 📤 解析输出

- **默认**：`data/statistics.csv`（固定命名，分析时自动识别）
- 使用 `-o`：保存到指定路径

---

## 📊 剧分析（每日消耗与利润）

分析一部剧在一段日期内每天的消耗、利润变化，输出表格 + 图表。

### 步骤

1. **解析数据**（若尚未解析）：`bash run_parse.sh`
2. **列出剧名**：`bash run_analyze.sh --list`，从输出中复制要分析的剧名
3. **编辑配置**：打开 `config.yaml`，将剧名填入 `剧名` 字段
4. **运行分析**：`bash run_analyze.sh`

### 配置说明

**推荐：剧名列表.xlsx**（用 Excel/WPS 编辑，更方便）

| 列 | 说明 |
|------|------|
| 剧名 | 每行一部剧，支持模糊匹配 |
| 开始日期 | 第一行有效值作为全局开始日期，留空用数据全部 |
| 结束日期 | 第一行有效值作为全局结束日期，留空用数据全部 |
| 数据文件 | 第一行有效值，留空则用 `data/statistics.csv`，可填 `xxx.csv` 或绝对路径 |

首次使用：`bash run_analyze.sh --init-excel` 创建模板，然后编辑填入剧名。

**备选：config.yaml**（当 剧名列表.xlsx 不存在时使用）

| 字段 | 说明 |
|------|------|
| 剧名 | 单部：`"倾国倾城的爱"`；多部：`"剧A, 剧B, 剧C"` |
| 开始日期 | 留空则用数据全部日期 |
| 结束日期 | 留空则用数据全部日期 |
| 数据文件 | 留空则用 `data/statistics.csv`，可填 `xxx.csv` 或绝对路径 |

### 输出

**单部剧：**
- 终端表格 + `data/daily_剧名_日期.csv` + `data/daily_剧名_日期.png`

**多部剧（合并为一个 Excel 文件）：**
- **宽表**：日期 \| 各剧消耗/利润/利润率（含汇总行）
- **长表**：日期 \| 剧名 \| 消耗 \| 利润 \| 利润率
- **消耗利润排行**：各剧排名明细
- **各剧 sheet**：每部剧的 daily 数据 + 折线图
