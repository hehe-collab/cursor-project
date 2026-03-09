"""
生成 20 条素材组合方案
基于 analyze 的输出，按 60-70% 围绕起量片段、30-40% 其他内容的规则生成
"""

import json
from pathlib import Path
from typing import Any, Optional, Union


def load_analysis(analysis_path: Union[str, Path]) -> dict:
    """加载分析结果"""
    with open(analysis_path, encoding="utf-8") as f:
        return json.load(f)


def generate_plan(analysis: dict, materials_count: int = 20) -> list[dict]:
    """
    生成 20 条素材的组合方案

    规则：
    - 60-70% (12-14条): 围绕 top 1-2 起量片段
    - 30-40% (6-8条): 围绕次高能片段或其他内容
    """
    highlights = analysis.get("top_highlights", [])
    hooks = analysis.get("top_hooks", [])
    episodes = analysis.get("episodes", [])

    if not highlights and not episodes:
        return []

    # 核心起量片段：取 top 2
    core_highlights = highlights[:2]
    # 次高能：取 3-6
    secondary = highlights[2:6] if len(highlights) > 2 else []
    # 钩子候选
    hook_candidates = hooks[:10] if hooks else []

    core_count = int(materials_count * 0.65)  # 约 13 条
    other_count = materials_count - core_count  # 约 7 条

    plans = []

    # 1. 围绕核心起量片段的素材
    for i in range(core_count):
        plan = {
            "id": i + 1,
            "type": "core",
            "hook": None,
            "body": [],
        }
        if core_highlights:
            h = core_highlights[i % len(core_highlights)]
            plan["body"] = [{
                "path": h.get("path"),
                "episode": h.get("episode"),
                "start_sec": h.get("start_sec"),
                "end_sec": h.get("end_sec"),
            }]
        if hook_candidates and i < len(hook_candidates):
            hook = hook_candidates[i]
            plan["hook"] = {
                "path": hook.get("path"),
                "episode": hook.get("episode"),
                "start_sec": hook.get("start_sec"),
                "end_sec": hook.get("end_sec"),
            }
        plans.append(plan)

    # 2. 其他高能内容的素材
    for i in range(other_count):
        plan = {
            "id": core_count + i + 1,
            "type": "secondary",
            "hook": None,
            "body": [],
        }
        if secondary:
            s = secondary[i % len(secondary)]
            plan["body"] = [{
                "path": s.get("path"),
                "episode": s.get("episode"),
                "start_sec": s.get("start_sec"),
                "end_sec": s.get("end_sec"),
            }]
        elif episodes:
            # 回退：用前几集
            ep = episodes[min(i, len(episodes) - 1)]
            scenes = ep.get("scenes", [])
            ep_path = ep.get("path", "")
            ep_num = ep.get("episode_num", i + 1)
            if scenes:
                s, e = scenes[0]
                plan["body"] = [{
                    "path": ep_path,
                    "episode": ep_num,
                    "start_sec": s,
                    "end_sec": e,
                }]
        if hook_candidates:
            hook = hook_candidates[(core_count + i) % len(hook_candidates)]
            plan["hook"] = {
                "path": hook.get("path"),
                "episode": hook.get("episode"),
                "start_sec": hook.get("start_sec"),
                "end_sec": hook.get("end_sec"),
            }
        plans.append(plan)

    return plans


def run_plan(analysis_path: Union[str, Path], output_path: Optional[Union[str, Path]] = None) -> list:
    """从分析结果生成方案并保存"""
    analysis = load_analysis(analysis_path)
    plans = generate_plan(analysis)

    if output_path:
        output_path = Path(output_path)
        output_path.parent.mkdir(parents=True, exist_ok=True)
        with open(output_path, "w", encoding="utf-8") as f:
            json.dump(plans, f, ensure_ascii=False, indent=2)

    return plans


if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(description="生成 20 条素材组合方案")
    parser.add_argument("--input", "-i", required=True, help="分析结果 JSON 路径")
    parser.add_argument("--output", "-o", help="输出方案 JSON 路径")
    args = parser.parse_args()

    run_plan(args.input, args.output)
