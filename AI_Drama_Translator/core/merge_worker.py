import os
import time
import subprocess
import glob
import re
import cv2
import numpy as np
from PyQt6.QtCore import QThread, pyqtSignal
from concurrent.futures import ProcessPoolExecutor
import logging

logger = logging.getLogger("AI_Drama")


class MergeWorker(QThread):
    """剧集合并工作线程"""
    progress = pyqtSignal(str)  # 进度消息
    finished = pyqtSignal(bool, str)  # (成功, 输出文件路径)
    
    def __init__(self, config):
        super().__init__()
        self.config = config
        self.is_running = True
    
    def run(self):
        """执行合并任务"""
        try:
            input_folder = self.config['input_folder']
            output_folder = self.config['output_folder']
            check_seconds = self.config['check_seconds']
            brightness_threshold = self.config['brightness_threshold']
            delta_threshold = self.config['delta_threshold']
            max_workers = self.config['max_workers']
            
            # 创建输出文件夹
            if not os.path.exists(output_folder):
                os.makedirs(output_folder)
                self.progress.emit(f"📁 创建输出文件夹: {output_folder}")
            
            # 扫描视频文件
            video_files = []
            for ext in ['*.mp4', '*.mov', '*.mkv', '*.avi']:
                video_files.extend(glob.glob(os.path.join(input_folder, ext)))
            
            if not video_files:
                self.progress.emit("❌ 错误：输入文件夹中没有视频文件")
                self.finished.emit(False, "")
                return
            
            # 自然排序
            def natural_sort_key(s):
                return [int(text) if text.isdigit() else text.lower()
                        for text in re.split(r'(\d+)', s)]
            
            video_files.sort(key=lambda x: natural_sort_key(os.path.basename(x)))
            
            self.progress.emit(f"📦 发现 {len(video_files)} 个分段文件")
            self.progress.emit(f"🔥 启动 {max_workers} 个并发处理核心...")
            self.progress.emit("")
            
            # 准备并行任务参数
            tasks = []
            temp_files = []
            for v in video_files:
                temp_output = os.path.join(output_folder, f"temp_{os.path.basename(v)}")
                tasks.append((v, temp_output, check_seconds, brightness_threshold, delta_threshold))
                temp_files.append(temp_output)
            
            # 开始并行处理
            start_time = time.time()
            
            with ProcessPoolExecutor(max_workers=max_workers) as executor:
                for i, (video_path, temp_path, _, _, _) in enumerate(tasks):
                    video_name = os.path.basename(video_path)
                    self.progress.emit(f"⚡ [{i+1}/{len(tasks)}] 处理中: {video_name}")
                
                results = list(executor.map(analyze_and_trim_video, tasks))
            
            elapsed = time.time() - start_time
            success_count = sum(results)
            self.progress.emit("")
            self.progress.emit(f"✅ 并行分析与切片完成，耗时: {elapsed:.2f} 秒")
            self.progress.emit(f"📊 成功: {success_count}/{len(tasks)} 个文件")
            self.progress.emit("")
            
            # 执行最终合并
            concat_list_path = os.path.join(output_folder, "list.txt")
            with open(concat_list_path, "w") as f:
                for t in temp_files:
                    if os.path.exists(t):  # 只添加成功处理的文件
                        f.write(f"file '{os.path.abspath(t)}'\n")
            
            final_output = os.path.join(output_folder, "merged_output.mp4")
            self.progress.emit("🎬 正在执行最后的无损拼合...")
            
            # 获取ffmpeg路径
            import shutil
            ffmpeg_bin = shutil.which("ffmpeg") or "/opt/homebrew/bin/ffmpeg"
            
            subprocess.run([
                ffmpeg_bin, '-y', '-f', 'concat', '-safe', '0',
                '-i', concat_list_path,
                '-c', 'copy',
                final_output
            ], stdout=subprocess.DEVNULL, stderr=subprocess.STDOUT)
            
            # 清理临时文件
            self.progress.emit("🧹 清理临时数据...")
            for t in temp_files:
                if os.path.exists(t):
                    try:
                        os.remove(t)
                    except:
                        pass
            
            if os.path.exists(concat_list_path):
                try:
                    os.remove(concat_list_path)
                except:
                    pass
            
            total_time = time.time() - start_time
            self.progress.emit("")
            self.progress.emit(f"⏱️ 全局总耗时: {total_time:.2f} 秒")
            
            self.finished.emit(True, final_output)
            
        except Exception as e:
            logger.error(f"合并任务异常: {e}")
            import traceback
            logger.error(traceback.format_exc())
            self.progress.emit(f"❌ 错误: {str(e)}")
            self.finished.emit(False, "")


def analyze_and_trim_video(video_info):
    """
    独立工作者：负责单集视频的亮度预判分析 + FFmpeg 切片
    这个函数会在独立进程中运行
    """
    v_path, temp_path, check_seconds, brightness_threshold, delta_threshold = video_info
    
    try:
        cap = cv2.VideoCapture(v_path)
        fps = cap.get(cv2.CAP_PROP_FPS)
        if fps == 0:
            cap.release()
            return False
        
        total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
        
        # 检查最后 N 秒
        start_frame = max(0, total_frames - int(fps * check_seconds))
        cap.set(cv2.CAP_PROP_POS_FRAMES, start_frame)
        
        brightness_history = []
        frame_idx = start_frame
        cut_frame = total_frames
        
        while True:
            ret, frame = cap.read()
            if not ret:
                break
            
            # 极简缩放，仅为亮度计算
            small_gray = cv2.cvtColor(cv2.resize(frame, (100, 56)), cv2.COLOR_BGR2GRAY)
            avg_brightness = np.mean(small_gray)
            brightness_history.append(avg_brightness)
            
            if len(brightness_history) > int(fps):
                brightness_history.pop(0)
                recent_frames = int(fps / 2)
                if len(brightness_history) >= recent_frames:
                    delta = brightness_history[-1] - brightness_history[-recent_frames]
                    if delta > delta_threshold and avg_brightness > 150:
                        cut_frame = frame_idx - int(fps * 0.6)
                        break
            
            if avg_brightness > brightness_threshold:
                cut_frame = frame_idx - int(fps * 0.5)
                break
            
            frame_idx += 1
        
        cap.release()
        cut_time = max(0, cut_frame / fps)
        
        # 调用 FFmpeg 执行切片
        import shutil
        ffmpeg_bin = shutil.which("ffmpeg") or "/opt/homebrew/bin/ffmpeg"
        
        subprocess.run([
            ffmpeg_bin, '-y', '-i', v_path,
            '-to', str(cut_time),
            '-c', 'copy', temp_path
        ], stdout=subprocess.DEVNULL, stderr=subprocess.STDOUT)
        
        return True
    
    except Exception as e:
        print(f"处理失败 {os.path.basename(v_path)}: {e}")
        return False

