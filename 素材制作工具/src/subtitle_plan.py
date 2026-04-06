"""
基于字幕块（SRT）生成主体时间轴：内容边界优先，再套时长目标。
"""

from pathlib import Path
from typing import Dict, List, Optional, Tuple, Union

from srt_parse import cues_to_blocks, parse_srt_file


def load_subtitle_blocks_by_episode_path(
    episodes: list,
    srt_dir: Union[str, Path],
    merge_gap_sec: float = 2.0,
) -> Dict[str, List[dict]]:
    """
    为每集加载字幕块。key 与 analysis 中 episode['path'] 一致（字符串）。
    查找规则：srt_dir / {视频文件 stem}.srt
    """
    srt_dir = Path(srt_dir)
    out: Dict[str, List[dict]] = {}
    for ep in episodes:
        p = ep.get("path", "")
        if not p:
            continue
        stem = Path(p).stem
        srt_path = srt_dir / f"{stem}.srt"
        if not srt_path.is_file():
            continue
        cues = parse_srt_file(srt_path)
        blocks = cues_to_blocks(cues, merge_gap_sec)
        if blocks:
            out[p] = blocks
    return out


def _find_block_index_for_time(blocks: List[dict], t_sec: float) -> int:
    """包含 t 的块优先，否则取时间中心离 t 最近的块。"""
    best = -1
    best_dist = float("inf")
    for i, b in enumerate(blocks):
        if b["start_sec"] <= t_sec <= b["end_sec"]:
            return i
        mid = (b["start_sec"] + b["end_sec"]) / 2
        d = abs(mid - t_sec)
        if d < best_dist:
            best_dist = d
            best = i
    return best


def pick_body_subtitle_blocks(
    path: str,
    episode_num: int,
    blocks: List[dict],
    highlight: dict,
    target_min_sec: float,
    target_max_sec: float,
    target_sec: float,
) -> List[dict]:
    """
    以高能时刻为锚，在字幕块上前后扩展，使总时长尽量接近 target_sec 且不超过 cap；
    不足下限则在不超过 target_max 下补足。返回单条或多条连续区间（通常一条连续合并）。
    """
    if not blocks:
        return []
    if highlight.get("path") and highlight.get("path") != path:
        return []

    mid = (highlight.get("start_sec", 0) + highlight.get("end_sec", 0)) / 2
    ci = _find_block_index_for_time(blocks, mid)
    if ci < 0:
        return []

    n = len(blocks)
    total = blocks[ci]["duration"]
    cap = min(target_sec, target_max_sec)

    lo, hi = ci, ci

    bi = ci - 1
    while total < cap and bi >= 0:
        seg = blocks[bi]
        if total + seg["duration"] <= cap:
            lo = bi
            total += seg["duration"]
        bi -= 1

    fi = ci + 1
    while total < cap and fi < n:
        seg = blocks[fi]
        if total + seg["duration"] <= cap:
            hi = fi
            total += seg["duration"]
        fi += 1

    if total < target_min_sec:
        cap = target_max_sec
        bi = lo - 1
        while total < target_min_sec and bi >= 0:
            seg = blocks[bi]
            if total + seg["duration"] <= cap:
                lo = bi
                total += seg["duration"]
            bi -= 1
        fi = hi + 1
        while total < target_min_sec and fi < n:
            seg = blocks[fi]
            if total + seg["duration"] <= cap:
                hi = fi
                total += seg["duration"]
            fi += 1

    if total < target_min_sec:
        return []

    return [
        {
            "path": path,
            "episode": episode_num,
            "start_sec": blocks[lo]["start_sec"],
            "end_sec": blocks[hi]["end_sec"],
        }
    ]


def pick_body_subtitle_blocks_cross_episode(
    subtitle_map: Dict[str, List[dict]],
    episodes_ordered: List[dict],
    highlight: dict,
    target_min_sec: float,
    target_max_sec: float,
    target_sec: float,
) -> List[dict]:
    """
    单集字幕总时长远小于目标（如 2 分钟 vs 15 分钟）时，从高光所在集起，
    按集序向后（必要时再向前）拼接「整集字幕覆盖区间」，直到达到 target_min～cap。
    每集一条：首块 start → 末块 end。
    """
    path = highlight.get("path", "")
    if not path or not subtitle_map:
        return []

    paths_order = [e.get("path") for e in episodes_ordered if e.get("path")]
    path_to_ep = {e.get("path"): e.get("episode_num", 0) for e in episodes_ordered}
    try:
        anchor_i = paths_order.index(path)
    except ValueError:
        return []

    # 先尝试单集内（含高光锚点）
    blocks = subtitle_map.get(path) or []
    ep_num = int(highlight.get("episode", path_to_ep.get(path, 1)))
    single = pick_body_subtitle_blocks(
        path,
        ep_num,
        blocks,
        highlight,
        target_min_sec,
        target_max_sec,
        target_sec,
    )
    if single:
        return single

    cap = min(target_sec, target_max_sec)
    segments: List[dict] = []
    total = 0.0

    # 从高光集开始向后接整集字幕，直到够长或到 cap
    j = anchor_i
    while j < len(paths_order) and total < cap:
        if total + 0.001 >= cap:
            break
        pth = paths_order[j]
        bl = subtitle_map.get(pth) or []
        if not bl:
            j += 1
            continue
        s0, s1 = bl[0]["start_sec"], bl[-1]["end_sec"]
        dur = s1 - s0
        if total + dur > cap:
            need = cap - total
            if need <= 0:
                break
            end_partial = s0 + min(need, dur)
            segments.append({
                "path": pth,
                "episode": int(path_to_ep.get(pth, j + 1)),
                "start_sec": s0,
                "end_sec": end_partial,
            })
            total = cap
            break
        segments.append({
            "path": pth,
            "episode": int(path_to_ep.get(pth, j + 1)),
            "start_sec": s0,
            "end_sec": s1,
        })
        total += dur
        j += 1
        if total >= target_min_sec:
            break

    # 仍不足则向前集补
    j = anchor_i - 1
    while total < target_min_sec and j >= 0 and total < cap:
        pth = paths_order[j]
        bl = subtitle_map.get(pth) or []
        if not bl:
            j -= 1
            continue
        s0, s1 = bl[0]["start_sec"], bl[-1]["end_sec"]
        dur = s1 - s0
        room = cap - total
        if room <= 0:
            break
        if dur <= room:
            segments.insert(0, {
                "path": pth,
                "episode": int(path_to_ep.get(pth, j + 1)),
                "start_sec": s0,
                "end_sec": s1,
            })
            total += dur
        else:
            start_partial = s1 - room
            segments.insert(0, {
                "path": pth,
                "episode": int(path_to_ep.get(pth, j + 1)),
                "start_sec": start_partial,
                "end_sec": s1,
            })
            total = cap
            break
        j -= 1

    if total < target_min_sec - 0.5:
        return []

    # 超长则裁掉最后一集末尾（从后往前减）
    while total > target_max_sec + 0.5 and segments:
        last = segments[-1]
        over = total - min(total, target_max_sec)
        new_end = last["end_sec"] - over
        if new_end <= last["start_sec"] + 0.5:
            segments.pop()
            total = sum(s["end_sec"] - s["start_sec"] for s in segments)
        else:
            last["end_sec"] = new_end
            total = sum(s["end_sec"] - s["start_sec"] for s in segments)
        break

    return segments


def resolve_srt_dir(
    cfg_subtitles: dict,
    analysis_path: Union[str, Path],
) -> Optional[Path]:
    """从配置解析字幕目录绝对路径；未启用或无效返回 None。"""
    if not cfg_subtitles:
        return None
    if cfg_subtitles.get("enabled") is False:
        return None
    d = cfg_subtitles.get("dir") or ""
    if not str(d).strip():
        return None
    srt_dir = Path(d)
    if not srt_dir.is_absolute():
        srt_dir = (Path(analysis_path).resolve().parent / srt_dir).resolve()
    if not srt_dir.is_dir():
        return None
    return srt_dir


def count_episodes_with_subs(
    subtitle_map: Dict[str, List[dict]],
    episodes: list,
) -> Tuple[int, int]:
    """有字幕块的集数 / 总集数"""
    paths = {ep.get("path") for ep in episodes if ep.get("path")}
    hit = sum(1 for p in paths if p in subtitle_map and subtitle_map[p])
    return hit, len(paths)
