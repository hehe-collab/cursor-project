"""
场景切分模块
使用 PySceneDetect 检测每集的场景边界
"""

from pathlib import Path
from typing import List, Tuple, Union

from scenedetect import detect, ContentDetector, AdaptiveDetector


def detect_scenes(
    video_path: Union[str, Path],
    threshold: float = 27.0,
    min_scene_len: int = 15,
    detector: str = "content",
) -> List[Tuple[float, float]]:
    """
    检测视频中的场景切分点

    Args:
        video_path: 视频文件路径
        threshold: 内容变化阈值，越大场景越少
        min_scene_len: 最小场景长度（帧数）
        detector: "content" 或 "adaptive"

    Returns:
        [(start_sec, end_sec), ...] 每个场景的起止时间（秒）
    """
    video_path = Path(video_path)
    if not video_path.exists():
        raise FileNotFoundError(f"视频不存在: {video_path}")

    if detector == "adaptive":
        scene_detector = AdaptiveDetector(adaptive_threshold=threshold)
    else:
        scene_detector = ContentDetector(threshold=threshold, min_scene_len=min_scene_len)

    scene_list = detect(str(video_path), scene_detector)

    result = []
    for i, (start, end) in enumerate(scene_list):
        start_sec = start.get_seconds()
        end_sec = end.get_seconds()
        result.append((start_sec, end_sec))

    return result


def get_scene_midpoint(scenes: List[Tuple[float, float]], index: int) -> float:
    """获取某场景中点时间（用于抽帧）"""
    start, end = scenes[index]
    return (start + end) / 2


if __name__ == "__main__":
    import sys

    if len(sys.argv) < 2:
        print("用法: python scene_detect.py <视频路径>")
        sys.exit(1)

    scenes = detect_scenes(sys.argv[1])
    print(f"检测到 {len(scenes)} 个场景:")
    for i, (s, e) in enumerate(scenes[:10]):
        print(f"  场景{i+1}: {s:.1f}s - {e:.1f}s")
    if len(scenes) > 10:
        print(f"  ... 共 {len(scenes)} 个")
