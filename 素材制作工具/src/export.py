"""
导出模块
根据 plan.json 裁剪拼接，输出成片
"""

import json
import subprocess
from pathlib import Path
from typing import Any, Union


def load_plan(plan_path: Union[str, Path]) -> list:
    with open(plan_path, encoding="utf-8") as f:
        return json.load(f)


def cut_segment(
    video_path: Union[str, Path],
    start_sec: float,
    end_sec: float,
    output_path: Union[str, Path],
) -> Path:
    """裁剪视频片段"""
    video_path = Path(video_path)
    output_path = Path(output_path)
    output_path.parent.mkdir(parents=True, exist_ok=True)

    cmd = [
        "ffmpeg", "-y",
        "-ss", str(start_sec),
        "-i", str(video_path),
        "-t", str(end_sec - start_sec),
        "-c", "copy",
        str(output_path),
    ]
    subprocess.run(cmd, check=True, capture_output=True)
    return output_path


def concat_segments(
    segment_paths: list[Path],
    output_path: Path,
) -> Path:
    """拼接多个片段"""
    output_path = Path(output_path)
    output_path.parent.mkdir(parents=True, exist_ok=True)

    # 创建 concat 列表文件
    list_path = output_path.with_suffix(".txt")
    with open(list_path, "w") as f:
        for p in segment_paths:
            f.write(f"file '{p.absolute()}'\n")

    cmd = [
        "ffmpeg", "-y",
        "-f", "concat", "-safe", "0",
        "-i", str(list_path),
        "-c", "copy",
        str(output_path),
    ]
    subprocess.run(cmd, check=True, capture_output=True)
    list_path.unlink(missing_ok=True)
    return output_path


def export_material(
    plan_item: dict,
    output_dir: Path,
    material_id: int,
) -> Path:
    """
    导出单条素材
    plan_item: { hook: {...}, body: [...] }
    """
    output_dir = Path(output_dir)
    temp_dir = output_dir / "temp"
    temp_dir.mkdir(parents=True, exist_ok=True)

    segments = []

    # 1. 钩子
    if plan_item.get("hook"):
        h = plan_item["hook"]
        hook_path = temp_dir / f"m{material_id}_hook.mp4"
        cut_segment(
            h["path"],
            h["start_sec"],
            h["end_sec"],
            hook_path,
        )
        segments.append(hook_path)

    # 2. 主体
    for b in plan_item.get("body", []):
        body_path = temp_dir / f"m{material_id}_body_{len(segments)}.mp4"
        cut_segment(
            b["path"],
            b["start_sec"],
            b["end_sec"],
            body_path,
        )
        segments.append(body_path)

    if not segments:
        raise ValueError(f"素材 {material_id} 无有效片段")

    # 3. 拼接
    out_path = output_dir / f"material_{material_id:02d}.mp4"
    concat_segments(segments, out_path)

    # 清理临时文件
    for s in segments:
        s.unlink(missing_ok=True)

    return out_path


def run_export(
    plan_path: Union[str, Path],
    output_dir: Union[str, Path],
) -> list:
    """批量导出所有素材"""
    plan = load_plan(plan_path)
    output_dir = Path(output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    results = []
    for item in plan:
        mid = item.get("id", len(results) + 1)
        try:
            path = export_material(item, output_dir, mid)
            results.append(path)
            print(f"已导出: {path.name}")
        except Exception as e:
            print(f"素材 {mid} 导出失败: {e}")

    return results


if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(description="导出素材成片")
    parser.add_argument("--plan", "-p", required=True, help="方案 JSON 路径")
    parser.add_argument("--output", "-o", required=True, help="输出目录")
    args = parser.parse_args()

    run_export(args.plan, args.output)
