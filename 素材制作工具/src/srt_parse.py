"""
解析 SRT 字幕（UTF-8），输出 cue 列表：start_sec, end_sec, text
"""

import re
from pathlib import Path
from typing import List, Union


_TS = re.compile(
    r"(\d{2}):(\d{2}):(\d{2})[,.](\d{3})\s*-->\s*(\d{2}):(\d{2}):(\d{2})[,.](\d{3})"
)


def _ts_to_sec(h: str, m: str, s: str, ms: str) -> float:
    return int(h) * 3600 + int(m) * 60 + int(s) + int(ms) / 1000.0


def parse_srt_file(path: Union[str, Path]) -> List[dict]:
    """
    解析 .srt 文件，返回按时间排序的 cue：
    [{"start_sec": float, "end_sec": float, "text": str}, ...]
    """
    path = Path(path)
    if not path.exists():
        return []
    raw = path.read_text(encoding="utf-8-sig", errors="replace")
    cues: List[dict] = []
    blocks = re.split(r"\n\s*\n+", raw.strip())
    for block in blocks:
        lines = [ln.strip() for ln in block.splitlines() if ln.strip()]
        if not lines:
            continue
        # 可选的首行序号
        i = 0
        if lines[0].isdigit():
            i = 1
        if i >= len(lines):
            continue
        m = _TS.search(lines[i])
        if not m:
            continue
        t0 = _ts_to_sec(m.group(1), m.group(2), m.group(3), m.group(4))
        t1 = _ts_to_sec(m.group(5), m.group(6), m.group(7), m.group(8))
        text_lines = lines[i + 1 :] if i + 1 < len(lines) else []
        text = " ".join(text_lines).strip()
        if t1 > t0:
            cues.append({"start_sec": t0, "end_sec": t1, "text": text})
    cues.sort(key=lambda c: (c["start_sec"], c["end_sec"]))
    return cues


def cues_to_blocks(
    cues: List[dict],
    merge_gap_sec: float = 2.0,
) -> List[dict]:
    """
    将相邻 cue 合并为「块」：若两条 cue 之间的间隙 > merge_gap_sec，则新开一块。
    每块：start_sec, end_sec, duration, text
    """
    if not cues:
        return []
    blocks = []
    cur_start = cues[0]["start_sec"]
    cur_end = cues[0]["end_sec"]
    cur_texts = [cues[0]["text"]]

    for c in cues[1:]:
        gap = c["start_sec"] - cur_end
        if gap > merge_gap_sec:
            blocks.append({
                "start_sec": cur_start,
                "end_sec": cur_end,
                "duration": cur_end - cur_start,
                "text": " ".join(cur_texts).strip(),
            })
            cur_start = c["start_sec"]
            cur_end = c["end_sec"]
            cur_texts = [c["text"]]
        else:
            cur_end = max(cur_end, c["end_sec"])
            if c["text"]:
                cur_texts.append(c["text"])

    blocks.append({
        "start_sec": cur_start,
        "end_sec": cur_end,
        "duration": cur_end - cur_start,
        "text": " ".join(cur_texts).strip(),
    })
    return blocks
