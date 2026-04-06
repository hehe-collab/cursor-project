#!/usr/bin/env python3
"""
检测视频是否带字幕轨（软字幕）
用法: python check_subtitle_track.py <视频路径>
"""
import sys
import subprocess
import json
import shutil

def check_subtitle_track(video_path):
    ffprobe = shutil.which("ffprobe") or "/opt/homebrew/bin/ffprobe"
    try:
        result = subprocess.run(
            [ffprobe, "-v", "quiet", "-print_format", "json", "-show_streams", video_path],
            capture_output=True,
            text=True,
            timeout=10,
        )
        if result.returncode != 0:
            print(f"❌ ffprobe 执行失败: {result.stderr}")
            return False

        data = json.loads(result.stdout)
        streams = data.get("streams", [])

        subtitle_streams = [s for s in streams if s.get("codec_type") == "subtitle"]
        video_streams = [s for s in streams if s.get("codec_type") == "video"]

        print(f"📹 视频: {video_path}")
        print(f"   视频流: {len(video_streams)} 个")
        print(f"   字幕流: {len(subtitle_streams)} 个")
        print()

        if not subtitle_streams:
            print("❌ 未检测到字幕轨（软字幕）")
            print("   说明: 该视频可能使用硬字幕（烧录在画面上），需用 OCR 提取")
            return False

        for i, s in enumerate(subtitle_streams):
            codec = s.get("codec_name", "unknown")
            lang = s.get("tags", {}).get("language", "未知")
            print(f"✅ 字幕轨 {i + 1}: 编码={codec}, 语言={lang}")
        print()
        print("💡 若有字幕轨，可用 ffmpeg 直接提取，无需 OCR")
        return True

    except FileNotFoundError:
        print("❌ 未找到 ffprobe，请确保已安装 FFmpeg")
        return False
    except json.JSONDecodeError as e:
        print(f"❌ 解析 ffprobe 输出失败: {e}")
        return False

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("用法: python check_subtitle_track.py <视频路径>")
        print("示例: python check_subtitle_track.py /path/to/video.mp4")
        sys.exit(1)

    video_path = sys.argv[1]
    check_subtitle_track(video_path)
