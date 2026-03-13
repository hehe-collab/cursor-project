"""
查看分析结果 - 以可读格式输出
用法: python src/view_report.py [analysis.json]
"""

import json
import sys
from pathlib import Path
from typing import Union


def view(analysis_path: Union[str, Path] = "analysis.json") -> None:
    path = Path(analysis_path)
    if not path.exists():
        print(f"文件不存在: {path}")
        return

    with open(path, encoding="utf-8") as f:
        data = json.load(f)

    print("\n" + "=" * 60)
    print("短剧素材分析报告")
    print("=" * 60)

    # 每集详细分析
    for ep in data.get("episodes", []):
        ep_num = ep.get("episode_num", 0)
        ep_path = ep.get("path", "")
        print(f"\n【第 {ep_num} 集】{ep_path}")
        print("-" * 50)

        ana = ep.get("analysis", {})
        scenes = ana.get("scenes", [])
        if not scenes:
            print("  (无场景分析)")
            continue

        for s in scenes:
            stype = s.get("type", "normal")
            icon = "⭐" if stype == "highlight" else "🎣" if stype == "hook" else "·"
            print(f"  {icon} {s.get('start_sec', 0):.1f}s - {s.get('end_sec', 0):.1f}s  "
                  f"评分{s.get('score', 0)} | {s.get('reason', '')} [{stype}]")

        if "episode_end_highlight" in ana:
            h = ana["episode_end_highlight"]
            print(f"  ⭐ {h.get('start_sec', 0):.1f}s - {h.get('end_sec', 0):.1f}s  "
                  f"评分{h.get('score', 0)} | 集尾高亮")

    # 汇总：起量片段
    print("\n" + "=" * 60)
    print("【起量片段 Top 10】")
    print("-" * 50)
    for i, h in enumerate(data.get("top_highlights", [])[:10], 1):
        print(f"  {i}. 第{h.get('episode', '?')}集 "
              f"{h.get('start_sec', 0):.1f}s-{h.get('end_sec', 0):.1f}s "
              f"评分{h.get('score', 0)} | {h.get('reason', '')}")

    # 汇总：钩子
    print("\n" + "=" * 60)
    print("【钩子片段 Top 10】")
    print("-" * 50)
    for i, h in enumerate(data.get("top_hooks", [])[:10], 1):
        print(f"  {i}. 第{h.get('episode', '?')}集 "
              f"{h.get('start_sec', 0):.1f}s-{h.get('end_sec', 0):.1f}s "
              f"评分{h.get('score', 0)} | {h.get('reason', '')}")

    print("\n" + "=" * 60 + "\n")


if __name__ == "__main__":
    path = sys.argv[1] if len(sys.argv) > 1 else "analysis.json"
    view(path)
