"""
生成素材组合方案
每条素材：钩子 + 主体（可配置 15-30 分钟）
支持多集短剧（如 30 集）分析后拼接
"""

import json
from pathlib import Path
from typing import Any, List, Optional, Tuple, Union


def load_config() -> dict:
    """加载配置"""
    config_path = Path(__file__).parent.parent / "config.yaml"
    if config_path.exists():
        try:
            import yaml
            with open(config_path, encoding="utf-8") as f:
                return yaml.safe_load(f) or {}
        except Exception:
            pass
    return {}


def load_analysis(analysis_path: Union[str, Path]) -> dict:
    """加载分析结果"""
    with open(analysis_path, encoding="utf-8") as f:
        return json.load(f)


def _build_segment_pool(episodes: list) -> List[dict]:
    """从所有剧集构建片段池（按集数、时间排序）"""
    pool = []
    for ep in episodes:
        path = ep.get("path", "")
        ep_num = ep.get("episode_num", 0)
        for start, end in ep.get("scenes", []):
            if end > start:
                pool.append({
                    "path": path,
                    "episode": ep_num,
                    "start_sec": start,
                    "end_sec": end,
                    "duration": end - start,
                })
    return pool


def _find_highlight_segment_index(sorted_pool: List[dict], highlight: dict) -> int:
    """找到高能片段在排序池中的索引（用于前后扩展），未找到返回 -1"""
    ep = highlight.get("episode", 1)
    mid = (highlight.get("start_sec", 0) + highlight.get("end_sec", 0)) / 2
    path = highlight.get("path", "")

    best_idx = -1
    best_dist = float("inf")
    for i, seg in enumerate(sorted_pool):
        if seg["path"] != path or seg["episode"] != ep:
            continue
        seg_mid = (seg["start_sec"] + seg["end_sec"]) / 2
        dist = abs(seg_mid - mid)
        if dist < best_dist:
            best_dist = dist
            best_idx = i
    return best_idx


def _pick_body_segments_around_highlight(
    sorted_pool: List[dict],
    highlight: dict,
    target_min_sec: float,
    target_max_sec: float,
) -> List[dict]:
    """
    在高能片段附近做前后扩展，保证剧情连贯
    以 highlight 为中心，向前、向后选取相邻片段
    """
    n = len(sorted_pool)
    if n == 0:
        return []

    center_idx = _find_highlight_segment_index(sorted_pool, highlight)
    if center_idx < 0:
        return []
    result = []
    total = 0.0

    # 先加入高能片段所在段
    center_seg = sorted_pool[center_idx]
    result.append({
        "path": center_seg["path"],
        "episode": center_seg["episode"],
        "start_sec": center_seg["start_sec"],
        "end_sec": center_seg["end_sec"],
    })
    total += center_seg["duration"]

    # 向前扩展（剧情前置）
    back_idx = center_idx - 1
    while total < target_max_sec and back_idx >= 0:
        seg = sorted_pool[back_idx]
        if total + seg["duration"] <= target_max_sec:
            result.insert(0, {
                "path": seg["path"],
                "episode": seg["episode"],
                "start_sec": seg["start_sec"],
                "end_sec": seg["end_sec"],
            })
            total += seg["duration"]
        back_idx -= 1

    # 向后扩展（剧情后置）
    fwd_idx = center_idx + 1
    while total < target_max_sec:
        idx = fwd_idx % n
        if idx == center_idx:
            break
        seg = sorted_pool[idx]
        if total + seg["duration"] <= target_max_sec:
            result.append({
                "path": seg["path"],
                "episode": seg["episode"],
                "start_sec": seg["start_sec"],
                "end_sec": seg["end_sec"],
            })
            total += seg["duration"]
        fwd_idx += 1
        if fwd_idx - center_idx > n:
            break

    return result if total >= target_min_sec else []


def _pick_body_segments(
    pool: List[dict],
    target_min_sec: float,
    target_max_sec: float,
    start_episode: int,
    offset: int = 0,
) -> List[dict]:
    """
    从片段池选取片段，使总时长在 target_min ~ target_max 之间
    按 episode、时间顺序选取，保证连贯性
    """
    result = []
    total = 0.0

    sorted_pool = sorted(pool, key=lambda x: (x["episode"], x["start_sec"]))
    n = len(sorted_pool)
    if n == 0:
        return []

    # 从 start_episode 的第一段开始，用 offset 错开不同素材
    start_idx = next((j for j, s in enumerate(sorted_pool) if s["episode"] == start_episode), 0)
    start_idx = (start_idx + offset) % n

    for i in range(n):
        idx = (start_idx + i) % n
        seg = sorted_pool[idx]
        dur = seg["duration"]
        if total + dur <= target_max_sec:
            result.append({
                "path": seg["path"],
                "episode": seg["episode"],
                "start_sec": seg["start_sec"],
                "end_sec": seg["end_sec"],
            })
            total += dur
        if total >= target_min_sec:
            break

    return result


def _get_duration_targets(
    materials_count: int,
    short_ratio: float,
    short_min: float,
    short_max: float,
    long_min: float,
    long_max: float,
) -> List[Tuple[float, float]]:
    """
    按比例分配每条素材的时长目标
    返回 [(min_sec, max_sec), ...]
    """
    short_count = int(materials_count * short_ratio)
    long_count = materials_count - short_count
    targets = []
    for _ in range(short_count):
        targets.append((short_min * 60, short_max * 60))
    for _ in range(long_count):
        targets.append((long_min * 60, long_max * 60))
    return targets


def generate_plan(
    analysis: dict,
    materials_count: int = 20,
    target_duration_min: float = 10,
    target_duration_max: float = 20,
    hook_max_seconds: float = 30,
    duration_short_ratio: float = 0.4,
    duration_short_min: float = 15,
    duration_short_max: float = 20,
    duration_long_min: float = 20,
    duration_long_max: float = 30,
) -> list:
    """
    生成素材组合方案

    时长：40% 为 15-20 分钟，60% 为 20-30 分钟（可配置）
    核心素材：围绕高能片段前后扩展，保证剧情连贯
    素材多样性：显式控制 body 起始错开，减少重叠
    """
    highlights = analysis.get("top_highlights", [])
    hooks = analysis.get("top_hooks", [])
    episodes = analysis.get("episodes", [])

    if not episodes:
        return []

    pool = _build_segment_pool(episodes)
    if not pool:
        return []

    sorted_pool = sorted(pool, key=lambda x: (x["episode"], x["start_sec"]))
    n_pool = len(sorted_pool)

    # 时长目标：按比例分配
    duration_targets = _get_duration_targets(
        materials_count,
        duration_short_ratio,
        duration_short_min,
        duration_short_max,
        duration_long_min,
        duration_long_max,
    )

    hook_candidates = hooks[:10] if hooks else []

    # 核心起量片段：取 top 2，围绕其做前后扩展
    core_highlights = highlights[:2]
    secondary = highlights[2:6] if len(highlights) > 2 else []
    core_count = int(materials_count * 0.65)
    other_count = materials_count - core_count

    # 素材多样性：offset 步长按池大小均匀分布，减少 body 重叠
    offset_step = max(1, n_pool // materials_count)

    plans = []

    # 1. 围绕核心起量片段的素材（高能片段前后扩展，剧情连贯）
    for i in range(core_count):
        target_min_sec, target_max_sec = duration_targets[i]
        plan = {"id": i + 1, "type": "core", "hook": None, "body": []}
        if hook_candidates and i < len(hook_candidates):
            h = hook_candidates[i]
            plan["hook"] = {
                "path": h.get("path"),
                "episode": h.get("episode"),
                "start_sec": h.get("start_sec"),
                "end_sec": h.get("end_sec"),
            }
        # 优先从高能片段前后扩展
        hl = core_highlights[i % len(core_highlights)] if core_highlights else None
        body = []
        if hl:
            body = _pick_body_segments_around_highlight(
                sorted_pool, hl, target_min_sec, target_max_sec
            )
        if not body:
            start_ep = core_highlights[i % len(core_highlights)].get("episode", 1) if core_highlights else 1
            body = _pick_body_segments(
                pool, target_min_sec, target_max_sec,
                start_ep, i * offset_step,
            )
        plan["body"] = body
        plans.append(plan)

    # 2. 其他高能内容的素材（多样性：offset 错开）
    for i in range(other_count):
        idx = core_count + i
        target_min_sec, target_max_sec = duration_targets[idx]
        plan = {"id": idx + 1, "type": "secondary", "hook": None, "body": []}
        if hook_candidates:
            hook = hook_candidates[idx % len(hook_candidates)]
            plan["hook"] = {
                "path": hook.get("path"),
                "episode": hook.get("episode"),
                "start_sec": hook.get("start_sec"),
                "end_sec": hook.get("end_sec"),
            }
        # 优先从次高能片段扩展，否则用 offset 错开
        hl = secondary[i % len(secondary)] if secondary else None
        body = []
        if hl:
            body = _pick_body_segments_around_highlight(
                sorted_pool, hl, target_min_sec, target_max_sec
            )
        if not body:
            start_ep = secondary[i % len(secondary)].get("episode", 1) if secondary else (i % len(episodes)) + 1
            body = _pick_body_segments(
                pool, target_min_sec, target_max_sec,
                start_ep, idx * offset_step,
            )
        plan["body"] = body
        plans.append(plan)

    return plans


def run_plan(
    analysis_path: Union[str, Path],
    output_path: Optional[Union[str, Path]] = None,
    config: Optional[dict] = None,
    override: Optional[dict] = None,
) -> list:
    """从分析结果生成方案并保存"""
    analysis = load_analysis(analysis_path)
    cfg = config or load_config()
    out_cfg = cfg.get("output", {})
    analyze_cfg = cfg.get("analyze", {})
    override = override or {}

    # 配置：命令行/覆盖优先；无新字段时回退到 target_duration_min/max
    target_min = override.get("target_duration_min") or out_cfg.get("target_duration_min", 10)
    target_max = override.get("target_duration_max") or out_cfg.get("target_duration_max", 20)
    materials_count = override.get("materials_count") or out_cfg.get("materials_count", 20)
    if "duration_short_ratio" in out_cfg or "duration_short_min" in override or "duration_short_min" in out_cfg:
        short_ratio = override.get("duration_short_ratio") or out_cfg.get("duration_short_ratio", 0.4)
        short_min = override.get("duration_short_min") or out_cfg.get("duration_short_min", 15)
        short_max = override.get("duration_short_max") or out_cfg.get("duration_short_max", 20)
        long_min = override.get("duration_long_min") or out_cfg.get("duration_long_min", 20)
        long_max = override.get("duration_long_max") or out_cfg.get("duration_long_max", 30)
    else:
        short_ratio, short_min, short_max = 1.0, target_min, target_max
        long_min, long_max = target_min, target_max

    plans = generate_plan(
        analysis,
        materials_count=materials_count,
        target_duration_min=target_min,
        target_duration_max=target_max,
        hook_max_seconds=analyze_cfg.get("hook_max_seconds", 30),
        duration_short_ratio=short_ratio,
        duration_short_min=short_min,
        duration_short_max=short_max,
        duration_long_min=long_min,
        duration_long_max=long_max,
    )

    # 检查内容是否足够
    pool = _build_segment_pool(analysis.get("episodes", []))
    total_content_min = sum(s["duration"] for s in pool) / 60
    min_target = min(short_min, long_min)
    if total_content_min < min_target:
        print(
            f"\n⚠️ 警告：分析结果总内容仅 {total_content_min:.1f} 分钟，"
            f"目标为 {short_min}-{short_max} 分钟（40%）及 {long_min}-{long_max} 分钟（60%）。\n"
            f"  建议：增加剧集后重新运行 analyze，再重新生成 plan。\n"
        )

    if output_path:
        output_path = Path(output_path)
        output_path.parent.mkdir(parents=True, exist_ok=True)
        with open(output_path, "w", encoding="utf-8") as f:
            json.dump(plans, f, ensure_ascii=False, indent=2)
        # 打印前 8 条时长，便于确认短/长分布
        short_cnt = int(materials_count * short_ratio)
        for i, p in enumerate(plans[:8]):
            body_sec = sum(b["end_sec"] - b["start_sec"] for b in p.get("body", []))
            tag = "短" if i < short_cnt else "长"
            print(f"  素材{p['id']}: 主体 {body_sec/60:.1f} 分钟 [{tag}]")

    return plans


if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(
        description="生成素材组合方案（40%% 15-20min，60%% 20-30min）"
    )
    parser.add_argument("--input", "-i", required=True, help="分析结果 JSON 路径")
    parser.add_argument("--output", "-o", help="输出方案 JSON 路径")
    parser.add_argument("--materials", "-n", type=int, help="素材数量（覆盖 config）")
    parser.add_argument("--target-min", type=float, help="目标时长下限（分钟，覆盖 config）")
    parser.add_argument("--target-max", type=float, help="目标时长上限（分钟，覆盖 config）")
    parser.add_argument("--short-ratio", type=float, help="短时长占比 0-1（覆盖 config）")
    parser.add_argument("--short-min", type=float, help="短时长下限（分钟）")
    parser.add_argument("--short-max", type=float, help="短时长上限（分钟）")
    parser.add_argument("--long-min", type=float, help="长时长下限（分钟）")
    parser.add_argument("--long-max", type=float, help="长时长上限（分钟）")
    args = parser.parse_args()

    override = {}
    if args.materials is not None:
        override["materials_count"] = args.materials
    if args.target_min is not None:
        override["target_duration_min"] = args.target_min
    if args.target_max is not None:
        override["target_duration_max"] = args.target_max
    if args.short_ratio is not None:
        override["duration_short_ratio"] = args.short_ratio
    if args.short_min is not None:
        override["duration_short_min"] = args.short_min
    if args.short_max is not None:
        override["duration_short_max"] = args.short_max
    if args.long_min is not None:
        override["duration_long_min"] = args.long_min
    if args.long_max is not None:
        override["duration_long_max"] = args.long_max

    run_plan(args.input, args.output, override=override)
