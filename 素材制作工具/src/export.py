"""
导出模块
根据 plan.json 裁剪拼接，输出成片
"""

import json
import subprocess
from pathlib import Path
from typing import Any, List, Optional, Union


def load_plan(plan_path: Union[str, Path]) -> list:
    with open(plan_path, encoding="utf-8") as f:
        return json.load(f)


def _resolve_path(path_str: str, base_dir: Path) -> Path:
    """将 plan 中的相对路径解析为绝对路径"""
    p = Path(path_str)
    if p.is_absolute():
        return p
    return (base_dir / p).resolve()


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
    segment_paths: List[Path],
    output_path: Path,
) -> Path:
    """拼接多个片段"""
    output_path = Path(output_path)
    output_path.parent.mkdir(parents=True, exist_ok=True)

    # 创建 concat 列表文件（路径中单引号需转义）
    list_path = output_path.with_suffix(".txt")
    with open(list_path, "w") as f:
        for p in segment_paths:
            path_str = str(p.absolute()).replace("'", "'\\''")
            f.write(f"file '{path_str}'\n")

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
    base_dir: Optional[Path] = None,
) -> Path:
    """
    导出单条素材
    plan_item: { hook: {...}, body: [...] }
    base_dir: 相对路径的解析基准（默认 plan 所在目录）
    """
    output_dir = Path(output_dir)
    temp_dir = output_dir / "temp"
    temp_dir.mkdir(parents=True, exist_ok=True)

    segments = []

    # 1. 钩子
    if plan_item.get("hook"):
        h = plan_item["hook"]
        hook_path = temp_dir / f"m{material_id}_hook.mp4"
        src = _resolve_path(h["path"], base_dir) if base_dir else Path(h["path"])
        cut_segment(
            src,
            h["start_sec"],
            h["end_sec"],
            hook_path,
        )
        segments.append(hook_path)

    # 2. 主体
    for b in plan_item.get("body", []):
        body_path = temp_dir / f"m{material_id}_body_{len(segments)}.mp4"
        src = _resolve_path(b["path"], base_dir) if base_dir else Path(b["path"])
        cut_segment(
            src,
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


def _collect_video_paths(plan: list) -> List[str]:
    """收集 plan 中所有视频路径"""
    paths = []
    for item in plan:
        if item.get("hook"):
            paths.append(item["hook"]["path"])
        for b in item.get("body", []):
            paths.append(b["path"])
    return list(set(paths))


def run_export(
    plan_path: Union[str, Path],
    output_dir: Union[str, Path],
    base_dir: Optional[Union[str, Path]] = None,
) -> list:
    """批量导出所有素材"""
    plan_path = Path(plan_path)
    plan = load_plan(plan_path)
    output_dir = Path(output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    # 路径解析基准：base_dir 或 plan 所在目录
    base = Path(base_dir) if base_dir else plan_path.parent

    # 导出前检查：所有引用文件是否存在
    missing = []
    for p in _collect_video_paths(plan):
        resolved = _resolve_path(p, base)
        if not resolved.exists():
            missing.append(str(resolved))
    if missing:
        print("\n⚠️ 以下视频文件不存在，plan 可能基于不同剧集生成：")
        for m in sorted(set(missing))[:10]:
            print(f"   - {m}")
        if len(missing) > 10:
            print(f"   ... 共 {len(set(missing))} 个文件缺失")
        print("\n请先运行：python src/plan.py -i analysis.json -o plan.json")
        print("  再执行导出。\n")
        raise FileNotFoundError(f"plan 引用了 {len(set(missing))} 个不存在的文件")

    results = []
    for item in plan:
        mid = item.get("id", len(results) + 1)
        try:
            path = export_material(item, output_dir, mid, base_dir=base)
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
    parser.add_argument("--base-dir", "-b", help="视频路径基准目录（默认 plan 所在目录）")
    args = parser.parse_args()

    run_export(args.plan, args.output, base_dir=args.base_dir)
