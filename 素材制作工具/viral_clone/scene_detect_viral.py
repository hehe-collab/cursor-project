#!/usr/bin/env python3
"""
爆款镜头切分（PySceneDetect · ContentDetector），输出 JSON。
与《爆款复制提示》Step2 对齐；后续用 indo_from_scenes.py + match_report.json 逐镜头裁印尼。
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path


def main() -> None:
    try:
        from scenedetect import ContentDetector, detect
    except ImportError:
        print("请安装: pip install 'scenedetect[opencv]'", file=sys.stderr)
        sys.exit(1)

    ap = argparse.ArgumentParser(description="爆款 mp4 镜头检测 → scenes.json")
    ap.add_argument("--video", type=Path, required=True)
    ap.add_argument(
        "--output",
        type=Path,
        default=None,
        help="JSON 输出路径（默认 scratch/<stem>_scenes.json）",
    )
    ap.add_argument(
        "--threshold",
        type=float,
        default=27.0,
        help="ContentDetector 阈值；切太碎则调高，切太粗则调低",
    )
    ap.add_argument(
        "--min-scene-len",
        type=int,
        default=15,
        help="检测到切点后至少隔多少帧再允许下一刀（防碎切）",
    )
    ap.add_argument(
        "--scratch",
        type=Path,
        default=Path("./viral_clone/_scenes"),
        help="默认 JSON 落盘目录",
    )
    args = ap.parse_args()

    video = args.video.resolve()
    if not video.is_file():
        print(f"找不到文件: {video}", file=sys.stderr)
        sys.exit(2)

    scratch = args.scratch.resolve()
    scratch.mkdir(parents=True, exist_ok=True)
    out = (args.output or (scratch / f"{video.stem}_scenes.json")).resolve()
    out.parent.mkdir(parents=True, exist_ok=True)

    print(f"镜头检测: {video.name}（threshold={args.threshold}）…", flush=True)
    pairs = detect(
        str(video),
        ContentDetector(
            threshold=args.threshold,
            min_scene_len=args.min_scene_len,
        ),
        show_progress=True,
    )
    segments = []
    for i, (st, en) in enumerate(pairs):
        segments.append(
            {
                "index": i,
                "start_sec": round(float(st.get_seconds()), 4),
                "end_sec": round(float(en.get_seconds()), 4),
            }
        )
    doc = {
        "video": str(video),
        "threshold": args.threshold,
        "min_scene_len_frames": args.min_scene_len,
        "segment_count": len(segments),
        "segments": segments,
    }
    with open(out, "w", encoding="utf-8") as f:
        json.dump(doc, f, ensure_ascii=False, indent=2)
    print(f"已写 {out}（共 {len(segments)} 个镜头）", flush=True)


if __name__ == "__main__":
    main()
