#!/usr/bin/env python3
"""
已有一份 match_report.json（含 episode_timeline 与 timeline_start_sample）时，
按 scenes.json 的镜头顺序，将爆款上每一镜映射到全剧时间轴，再裁印尼、拼接。
依赖与「整条爆款」相同的全局平移假设；若某镜跨集，会自动拆成多段 concat。
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any, Dict, List, Tuple

from match_viral_timeline import concat_demuxer_list, cut_indo_clip, global_to_cuts


def _load_timeline_report(path: Path) -> Tuple[int, int, List[Tuple[Path, int, int]], Dict[str, Any]]:
    with open(path, encoding="utf-8") as f:
        r = json.load(f)
    if "episode_timeline" not in r:
        print(
            "match_report 缺少 episode_timeline，请用当前版本 viral_clone/match_viral_timeline.py 重新生成。",
            file=sys.stderr,
        )
        sys.exit(3)
    sr = int(r["sample_rate"])
    t0 = int(r["timeline_start_sample"])
    meta = [
        (Path(row["path"]), int(row["start_sample"]), int(row["end_sample"]))
        for row in r["episode_timeline"]
    ]
    return sr, t0, meta, r


def main() -> None:
    ap = argparse.ArgumentParser(
        description="match_report + scenes.json → 按镜头顺序导出印尼成片",
    )
    ap.add_argument("--match-report", type=Path, required=True)
    ap.add_argument("--scenes", type=Path, required=True)
    ap.add_argument("--indo-dir", type=Path, required=True)
    ap.add_argument("--output", type=Path, required=True)
    ap.add_argument(
        "--scratch",
        type=Path,
        default=Path("./viral_clone/_scratch_indo_scenes"),
    )
    ap.add_argument(
        "--stream-copy",
        action="store_true",
        help="分段裁切流复制（快，入点可能偏关键帧）",
    )
    ap.add_argument(
        "--reencode-concat",
        action="store_true",
        help="最终 concat 整轨重编码（片段参数不一致时）",
    )
    ap.add_argument(
        "--cut-seek-margin",
        type=float,
        default=0.0,
        help="与 match_viral_timeline 一致：默认 0 单次输入 -ss；>0 为混合 seek 回退秒数",
    )
    args = ap.parse_args()

    sr, t_anchor, meta, _report = _load_timeline_report(args.match_report.resolve())
    indo_dir = args.indo_dir.resolve()
    scratch = args.scratch.resolve()
    scratch.mkdir(parents=True, exist_ok=True)
    out = args.output.resolve()
    out.parent.mkdir(parents=True, exist_ok=True)

    with open(args.scenes, encoding="utf-8") as f:
        scene_doc = json.load(f)
    segs = scene_doc.get("segments") or []
    if not segs:
        print("scenes.json 无 segments", file=sys.stderr)
        sys.exit(4)

    seg_paths: List[Path] = []
    part_i = 0
    for sc in segs:
        vs0 = float(sc["start_sec"])
        vs1 = float(sc["end_sec"])
        gs0 = t_anchor + int(round(vs0 * sr))
        gs1 = t_anchor + int(round(vs1 * sr))
        if gs1 <= gs0:
            continue
        cuts_cn = global_to_cuts(meta, gs0, gs1, sr)
        if not cuts_cn:
            print(
                f"  警告: 镜头 index={sc.get('index')} 未映射到任何原剧集片段，跳过",
                file=sys.stderr,
            )
            continue
        for cn_p, ct0, ct1 in cuts_cn:
            ido = indo_dir / f"{cn_p.stem}{cn_p.suffix}"
            if not ido.is_file():
                print(f"缺少印尼文件: {ido}", file=sys.stderr)
                sys.exit(5)
            seg_path = scratch / f"indo_seg_{part_i:04d}.mp4"
            print(
                f"  片段 [{part_i + 1}] 镜{sc.get('index')} {cn_p.name} "
                f"{ct0:.2f}s–{ct1:.2f}s …",
                flush=True,
            )
            cut_indo_clip(
                ido,
                ct0,
                ct1,
                seg_path,
                args.stream_copy,
                cut_seek_margin_sec=args.cut_seek_margin,
            )
            seg_paths.append(seg_path)
            part_i += 1

    if not seg_paths:
        print("未生成任何裁切片段", file=sys.stderr)
        sys.exit(6)

    list_txt = scratch / "concat_list.txt"
    with open(list_txt, "w", encoding="utf-8") as f:
        for sp in seg_paths:
            ps = str(sp.resolve()).replace("'", "'\\''")
            f.write(f"file '{ps}'\n")
    print("  concat 合并成片 …", flush=True)
    concat_demuxer_list(list_txt, out, args.reencode_concat)
    print(f"  印尼成片: {out}", flush=True)


if __name__ == "__main__":
    main()
