"""
生成素材组合方案
每条素材：钩子 + 主体（可配置 15-30 分钟）
支持多集短剧（如 30 集）分析后拼接

主体时长：在配置的 [min, max] 内随机抽取目标时长（可配置为旧版「顶格 max」）。
"""

import json
import random
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple, Union

from subtitle_plan import (
    count_episodes_with_subs,
    load_subtitle_blocks_by_episode_path,
    pick_body_subtitle_blocks,
    pick_body_subtitle_blocks_cross_episode,
    resolve_srt_dir,
)


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
    target_sec: float,
) -> List[dict]:
    """
    在高能片段附近做前后扩展，保证剧情连贯
    以 highlight 为中心，向前、向后选取相邻片段

    target_sec: 本条素材主体目标总时长（秒），在 [target_min_sec, target_max_sec] 内抽样得到；
    先尽量在不超过 target_sec 的前提下扩展；若仍低于 target_min_sec，则在不超过 target_max_sec 下补足。
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

    cap = min(target_sec, target_max_sec)

    # 向前扩展（剧情前置）
    back_idx = center_idx - 1
    while total < cap and back_idx >= 0:
        seg = sorted_pool[back_idx]
        if total + seg["duration"] <= cap:
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
    while total < cap:
        idx = fwd_idx % n
        if idx == center_idx:
            break
        seg = sorted_pool[idx]
        if total + seg["duration"] <= cap:
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

    # 若仍低于时长下限，在不超过 target_max 的前提下补足（应对切片粒度导致达不到 target_sec）
    if total < target_min_sec:
        cap = target_max_sec
        # 从当前 result 两端继续向两侧扩展，直到 >= min 或无法再增
        max_passes = n * 2
        passes = 0
        while total < target_min_sec and passes < max_passes:
            passes += 1
            progressed = False
            # 再向前：第一段在 result[0] 之前
            first = result[0]
            fi = next(
                (j for j, s in enumerate(sorted_pool)
                 if s["path"] == first["path"] and s["start_sec"] == first["start_sec"]),
                None,
            )
            if fi is not None and fi > 0:
                seg = sorted_pool[fi - 1]
                if total + seg["duration"] <= cap:
                    result.insert(0, {
                        "path": seg["path"],
                        "episode": seg["episode"],
                        "start_sec": seg["start_sec"],
                        "end_sec": seg["end_sec"],
                    })
                    total += seg["duration"]
                    progressed = True
                    if total >= target_min_sec:
                        break
            # 再向后
            last = result[-1]
            li = next(
                (j for j, s in enumerate(sorted_pool)
                 if s["path"] == last["path"] and s["start_sec"] == last["start_sec"]),
                None,
            )
            if li is not None and li + 1 < n:
                seg = sorted_pool[li + 1]
                if total + seg["duration"] <= cap:
                    result.append({
                        "path": seg["path"],
                        "episode": seg["episode"],
                        "start_sec": seg["start_sec"],
                        "end_sec": seg["end_sec"],
                    })
                    total += seg["duration"]
                    progressed = True
            if not progressed:
                break

    return result if total >= target_min_sec else []


def _pick_body_segments(
    pool: List[dict],
    target_min_sec: float,
    target_max_sec: float,
    target_sec: float,
    start_episode: int,
    offset: int = 0,
) -> List[dict]:
    """
    从片段池选取片段，使总时长尽量接近 target_sec（且在 [min,max] 内），
    不足 min 时在不超过 max 下补足。按 episode、时间顺序选取。
    """
    result = []
    total = 0.0
    seen = set()

    sorted_pool = sorted(pool, key=lambda x: (x["episode"], x["start_sec"]))
    n = len(sorted_pool)
    if n == 0:
        return []

    start_idx = next((j for j, s in enumerate(sorted_pool) if s["episode"] == start_episode), 0)
    start_idx = (start_idx + offset) % n

    caps = (min(target_sec, target_max_sec), target_max_sec)
    for phase, cap in enumerate(caps):
        if phase == 1 and total >= target_min_sec:
            break
        for i in range(n):
            if phase == 1 and total >= target_min_sec:
                break
            idx = (start_idx + i) % n
            seg = sorted_pool[idx]
            key = (seg["path"], seg["start_sec"])
            if key in seen:
                continue
            dur = seg["duration"]
            if total + dur <= cap:
                seen.add(key)
                result.append({
                    "path": seg["path"],
                    "episode": seg["episode"],
                    "start_sec": seg["start_sec"],
                    "end_sec": seg["end_sec"],
                })
                total += dur

    return result if total >= target_min_sec else []


def _sample_target_sec(
    duration_min_sec: float,
    duration_max_sec: float,
    mode: str,
    bias: str = "upper",
) -> float:
    """
    在 [min,max] 内抽取本条素材的主体目标时长（秒）。
    mode=max 为旧版顶格上限。
    bias 仅对 random 生效：
      uniform — 全区间均匀随机
      upper   — 在区间上半段 [mid, max] 抽样，尽量不偏短（默认）
      lower   — 在区间下半段 [min, mid] 抽样
    """
    if mode == "max":
        return duration_max_sec
    if duration_min_sec >= duration_max_sec:
        return duration_min_sec
    lo, hi = duration_min_sec, duration_max_sec
    mid = (lo + hi) / 2.0
    if bias == "lower":
        return random.uniform(lo, mid)
    if bias == "upper":
        return random.uniform(mid, hi)
    return random.uniform(lo, hi)


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


def _duration_fit_tag(
    body_sec: float,
    target_min_sec: float,
    target_max_sec: float,
) -> str:
    """主体时长相对本条配置区间的标签（便于投放筛选）。"""
    if body_sec + 1.0 < target_min_sec:
        return "short_ok"
    if body_sec > target_max_sec + 1.0:
        return "long_ok"
    return "in_range"


def _body_for_highlight(
    hl: Optional[dict],
    sorted_pool: List[dict],
    pool: List[dict],
    subtitle_blocks_by_path: Optional[Dict[str, List[dict]]],
    episodes_ordered: List[dict],
    plan_mode: str,
    target_min_sec: float,
    target_max_sec: float,
    target_sec: float,
    start_ep: int,
    offset: int,
) -> Tuple[List[dict], str]:
    """
    返回 (body 片段列表, 来源: subtitle | scene)
    """
    use_sub = (
        plan_mode == "subtitle_first"
        and subtitle_blocks_by_path
        and hl
    )
    path = hl.get("path", "") if hl else ""
    ep_num = int(hl.get("episode", 1)) if hl else start_ep
    blocks = subtitle_blocks_by_path.get(path) if (use_sub and path) else None

    if use_sub and blocks and hl:
        sub_body = pick_body_subtitle_blocks(
            path,
            ep_num,
            blocks,
            hl,
            target_min_sec,
            target_max_sec,
            target_sec,
        )
        if sub_body:
            return sub_body, "subtitle"
        if subtitle_blocks_by_path and episodes_ordered:
            sub_body = pick_body_subtitle_blocks_cross_episode(
                subtitle_blocks_by_path,
                episodes_ordered,
                hl,
                target_min_sec,
                target_max_sec,
                target_sec,
            )
            if sub_body:
                return sub_body, "subtitle"

    if hl:
        body = _pick_body_segments_around_highlight(
            sorted_pool, hl, target_min_sec, target_max_sec, target_sec
        )
        if body:
            return body, "scene"
    body = _pick_body_segments(
        pool, target_min_sec, target_max_sec, target_sec,
        start_ep, offset,
    )
    return body, "scene"


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
    duration_target_mode: str = "random",
    duration_random_seed: Optional[int] = None,
    duration_target_bias: str = "upper",
    subtitle_blocks_by_path: Optional[Dict[str, List[dict]]] = None,
    plan_mode: str = "subtitle_first",
) -> list:
    """
    生成素材组合方案

    时长：40% 为 15-20 分钟，60% 为 20-30 分钟（可配置）
    每条主体在对应区间内随机目标时长（duration_target_mode=random），避免条条顶格；
    duration_target_mode=max 时仍顶到区间上限（与旧版一致）。
    duration_target_bias=upper 时在区间上半段抽样，尽量不偏短。

    plan_mode=subtitle_first 且提供字幕块时：主体以印尼语字幕块边界扩展（内容优先），失败则回退场景切分。
    plan_mode=scene_only：仅用场景切分（旧逻辑）。

    核心素材：围绕高能片段前后扩展，保证剧情连贯
    素材多样性：显式控制 body 起始错开，减少重叠
    """
    if duration_random_seed is not None:
        random.seed(duration_random_seed)
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
        target_sec = _sample_target_sec(
            target_min_sec, target_max_sec, duration_target_mode, duration_target_bias
        )
        plan = {
            "id": i + 1,
            "type": "core",
            "hook": None,
            "body": [],
            "body_target_sec": round(target_sec, 2),
            "duration_target_bias": duration_target_bias,
        }
        if hook_candidates and i < len(hook_candidates):
            h = hook_candidates[i]
            plan["hook"] = {
                "path": h.get("path"),
                "episode": h.get("episode"),
                "start_sec": h.get("start_sec"),
                "end_sec": h.get("end_sec"),
            }
        if core_highlights:
            hl = core_highlights[i % len(core_highlights)]
            start_ep = core_highlights[i % len(core_highlights)].get("episode", 1)
        else:
            hl = None
            start_ep = 1
        body, body_src = _body_for_highlight(
            hl,
            sorted_pool,
            pool,
            subtitle_blocks_by_path,
            episodes,
            plan_mode,
            target_min_sec,
            target_max_sec,
            target_sec,
            start_ep,
            i * offset_step,
        )
        plan["body"] = body
        plan["body_source"] = body_src
        body_sec = sum(b["end_sec"] - b["start_sec"] for b in body) if body else 0.0
        plan["duration_fit"] = _duration_fit_tag(
            body_sec, target_min_sec, target_max_sec
        )
        plans.append(plan)

    # 2. 其他高能内容的素材（多样性：offset 错开）
    for i in range(other_count):
        idx = core_count + i
        target_min_sec, target_max_sec = duration_targets[idx]
        target_sec = _sample_target_sec(
            target_min_sec, target_max_sec, duration_target_mode, duration_target_bias
        )
        plan = {
            "id": idx + 1,
            "type": "secondary",
            "hook": None,
            "body": [],
            "body_target_sec": round(target_sec, 2),
            "duration_target_bias": duration_target_bias,
        }
        if hook_candidates:
            hook = hook_candidates[idx % len(hook_candidates)]
            plan["hook"] = {
                "path": hook.get("path"),
                "episode": hook.get("episode"),
                "start_sec": hook.get("start_sec"),
                "end_sec": hook.get("end_sec"),
            }
        if secondary:
            hl = secondary[i % len(secondary)]
            start_ep = secondary[i % len(secondary)].get("episode", 1)
        else:
            hl = None
            start_ep = (i % len(episodes)) + 1 if episodes else 1
        body, body_src = _body_for_highlight(
            hl,
            sorted_pool,
            pool,
            subtitle_blocks_by_path,
            episodes,
            plan_mode,
            target_min_sec,
            target_max_sec,
            target_sec,
            start_ep,
            idx * offset_step,
        )
        plan["body"] = body
        plan["body_source"] = body_src
        body_sec = sum(b["end_sec"] - b["start_sec"] for b in body) if body else 0.0
        plan["duration_fit"] = _duration_fit_tag(
            body_sec, target_min_sec, target_max_sec
        )
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

    duration_target_mode = (
        override.get("duration_target_mode")
        or out_cfg.get("duration_target_mode", "random")
    )
    if duration_target_mode not in ("random", "max"):
        duration_target_mode = "random"
    duration_random_seed = override.get("duration_random_seed")
    if duration_random_seed is None:
        duration_random_seed = out_cfg.get("duration_random_seed")

    duration_target_bias = (
        override.get("duration_target_bias")
        or out_cfg.get("duration_target_bias", "upper")
    )
    if duration_target_bias not in ("uniform", "upper", "lower"):
        duration_target_bias = "upper"

    plan_mode = override.get("plan_mode") or out_cfg.get("plan_mode", "subtitle_first")
    if plan_mode not in ("subtitle_first", "scene_only"):
        plan_mode = "subtitle_first"

    sub_cfg = cfg.get("subtitles") or {}
    srt_dir = resolve_srt_dir(sub_cfg, analysis_path)
    subtitle_map: Optional[Dict[str, List[dict]]] = None
    if srt_dir is not None:
        merge_gap = float(sub_cfg.get("merge_gap_sec", 2.0))
        subtitle_map = load_subtitle_blocks_by_episode_path(
            analysis.get("episodes", []), srt_dir, merge_gap
        )
        n_hit, n_tot = count_episodes_with_subs(
            subtitle_map, analysis.get("episodes", [])
        )
        print(f"字幕：已加载 {n_hit}/{n_tot} 集（{srt_dir}）")
    elif sub_cfg.get("enabled") is True:
        print(
            "⚠️ 字幕已启用但 subtitles.dir 无效或目录不存在，主体将按场景切分。"
            "请设置与 analysis.json 同目录相对路径或绝对路径。"
        )

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
        duration_target_mode=duration_target_mode,
        duration_random_seed=duration_random_seed,
        duration_target_bias=duration_target_bias,
        subtitle_blocks_by_path=subtitle_map,
        plan_mode=plan_mode,
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
        # 打印前 8 条时长，便于确认短/长分布与目标抽样
        short_cnt = int(materials_count * short_ratio)
        for i, p in enumerate(plans[:8]):
            body_sec = sum(b["end_sec"] - b["start_sec"] for b in p.get("body", []))
            tag = "短" if i < short_cnt else "长"
            tgt = p.get("body_target_sec", 0)
            print(
                f"  素材{p['id']}: 主体 {body_sec/60:.1f} 分钟 "
                f"(目标 {tgt/60:.1f} 分钟) [{tag}]"
            )
        print(
            f"  主体目标模式: {duration_target_mode}（random=随机目标，max=顶格上限）"
        )
        print(
            f"  随机区间抽样: {duration_target_bias}（upper=区间上半段，尽量不偏短；uniform=全区间均匀）"
        )
        print(
            f"  主体规划模式: {plan_mode}（subtitle_first=字幕块优先，scene_only=仅场景）"
        )
        sub_cnt = sum(
            1 for p in plans if p.get("body_source") == "subtitle"
        )
        print(f"  主体来源：字幕块 {sub_cnt}/{len(plans)} 条，其余为场景切分")

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
    parser.add_argument(
        "--duration-mode",
        choices=["random", "max"],
        help="主体目标时长：random=在区间内随机，max=顶格上限（旧行为）",
    )
    parser.add_argument(
        "--random-seed",
        type=int,
        help="随机种子（仅 random 模式），相同种子可复现 plan",
    )
    parser.add_argument(
        "--duration-bias",
        choices=["uniform", "upper", "lower"],
        help="random 模式下：upper=区间上半段（默认，尽量不偏短）；uniform=全区间均匀",
    )
    parser.add_argument(
        "--plan-mode",
        choices=["subtitle_first", "scene_only"],
        help="subtitle_first=字幕块定边界（需 config 字幕目录）；scene_only=仅场景切分",
    )
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
    if args.duration_mode is not None:
        override["duration_target_mode"] = args.duration_mode
    if args.random_seed is not None:
        override["duration_random_seed"] = args.random_seed
    if args.duration_bias is not None:
        override["duration_target_bias"] = args.duration_bias
    if args.plan_mode is not None:
        override["plan_mode"] = args.plan_mode

    run_plan(args.input, args.output, override=override)
