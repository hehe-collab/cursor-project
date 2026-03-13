"""
帧提取模块
从视频指定时间点提取一帧，用于 AI 分析
"""

import base64
import subprocess
from pathlib import Path
from typing import Union


def extract_frame(
    video_path: Union[str, Path],
    timestamp_sec: float,
    size: int = 384,
    format: str = "jpeg",
    quality: int = 85,
) -> bytes:
    """
    从视频指定时间点提取一帧

    Args:
        video_path: 视频路径
        timestamp_sec: 时间点（秒）
        size: 输出尺寸（正方形边长）
        format: jpeg 或 png
        quality: jpeg 质量 1-100

    Returns:
        图片的 bytes
    """
    video_path = Path(video_path)
    if not video_path.exists():
        raise FileNotFoundError(f"视频不存在: {video_path}")

    # 时间格式 HH:MM:SS.ms
    h = int(timestamp_sec // 3600)
    m = int((timestamp_sec % 3600) // 60)
    s = timestamp_sec % 60
    time_str = f"{h:02d}:{m:02d}:{s:03.1f}"

    cmd = [
        "ffmpeg",
        "-y",
        "-ss", time_str,
        "-i", str(video_path),
        "-vframes", "1",
        "-vf", f"scale={size}:{size}:force_original_aspect_ratio=decrease,pad={size}:{size}:(ow-iw)/2:(oh-ih)/2",
        "-f", "image2",
        "-"
    ]
    if format == "jpeg":
        cmd.insert(-2, "-q:v")
        cmd.insert(-2, str(round(31 - quality * 31 / 100)))  # ffmpeg q:v 2-31, 低=好

    result = subprocess.run(
        cmd,
        capture_output=True,
        timeout=30,
    )
    if result.returncode != 0:
        raise RuntimeError(f"ffmpeg 失败: {result.stderr.decode()[:200]}")

    return result.stdout


def frame_to_base64(data: bytes) -> str:
    """将图片 bytes 转为 base64，用于 OpenAI API"""
    return base64.standard_b64encode(data).decode("ascii")
