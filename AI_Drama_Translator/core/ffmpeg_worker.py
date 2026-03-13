import subprocess
import shutil
import os

class FFmpegWorker:
    def __init__(self):
        # 核心：确保 FFmpeg 路径正确初始化
        self.ffmpeg_bin = shutil.which("ffmpeg") or "/opt/homebrew/bin/ffmpeg"

    def run_command(self, command):
        """执行命令并支持外部中止"""
        try:
            process = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
            return process
        except Exception as e:
            print(f"FFmpeg Popen Error: {e}")
            return None

    def only_erase(self, video_path, output_path, erase_rect, bitrate="1.3M"):
        """此方法保留用于兼容，但推荐使用 run_command 配合进程管理"""
        if not erase_rect:
            return False, "未划定擦除区域"
        
        ex, ey, ew, eh = erase_rect
        ex, ey, ew, eh = ex & ~1, ey & ~1, ew & ~1, eh & ~1
        
        command = [
            self.ffmpeg_bin, '-y',
            '-i', video_path,
            '-vf', f"delogo=x={ex}:y={ey}:w={ew}:h={eh}:show=0",
            '-c:v', 'h264_videotoolbox', 
            '-b:v', bitrate, 
            '-profile:v', 'main',
            '-pix_fmt', 'yuv420p',
            '-c:a', 'copy', 
            output_path
        ]
        try:
            res = subprocess.run(command, capture_output=True, text=True)
            if res.returncode != 0:
                command[command.index('h264_videotoolbox')] = 'libx264'
                res = subprocess.run(command, capture_output=True, text=True)
            return res.returncode == 0, res.stderr
        except Exception as e:
            return False, str(e)

    def only_burn(self, video_path, srt_path, output_path, bitrate="1.3M"):
        abs_srt = os.path.abspath(srt_path)
        escaped_srt = abs_srt.replace(":", "\\:").replace("\\", "/")
        style = "FontName=Thonburi,FontSize=18,PrimaryColour=&H00FFFFFF&,OutlineColour=&H00000000&,BorderStyle=1,Outline=1,Shadow=0"
        
        command = [
            self.ffmpeg_bin, '-y',
            '-i', video_path,
            '-vf', f"subtitles='{escaped_srt}':force_style='{style}'",
            '-c:v', 'h264_videotoolbox', 
            '-b:v', bitrate, 
            '-profile:v', 'main',
            '-pix_fmt', 'yuv420p',
            '-c:a', 'copy', 
            output_path
        ]
        try:
            res = subprocess.run(command, capture_output=True, text=True)
            if res.returncode != 0:
                command[command.index('h264_videotoolbox')] = 'libx264'
                res = subprocess.run(command, capture_output=True, text=True)
            return res.returncode == 0, res.stderr
        except Exception as e:
            return False, str(e)
