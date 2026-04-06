import os
import time
import subprocess
import glob
import re
import shutil
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
            trim_mode = self.config.get('trim_mode', 'brightness')
            compatible_merge = self.config.get('compatible_merge', False)
            check_seconds = self.config['check_seconds']
            brightness_threshold = self.config['brightness_threshold']
            delta_threshold = self.config['delta_threshold']
            fixed_trim_seconds = self.config.get('fixed_trim_seconds', 0)
            skip_last_episode_trim = self.config.get('skip_last_episode_trim', True)
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
            mode_desc = "固定秒数裁剪" if trim_mode == 'fixed_seconds' else "智能亮度检测"
            self.progress.emit(f"📐 裁剪模式: {mode_desc}")
            if compatible_merge:
                self.progress.emit("🛡️ 高兼容合并: 已开启（裁剪时重编码，避免交界处卡顿）")
            self.progress.emit(f"🔥 启动 {max_workers} 个并发处理核心...")
            self.progress.emit("")
            
            # 准备并行任务参数
            tasks = []
            temp_files = []
            for i, v in enumerate(video_files):
                temp_output = os.path.join(output_folder, f"temp_{os.path.basename(v)}")
                is_last = (i == len(video_files) - 1)
                tasks.append((v, temp_output, trim_mode, compatible_merge, check_seconds, brightness_threshold,
                              delta_threshold, fixed_trim_seconds, skip_last_episode_trim, is_last))
                temp_files.append(temp_output)
            
            # 开始并行处理
            start_time = time.time()
            
            with ProcessPoolExecutor(max_workers=max_workers) as executor:
                for i, task in enumerate(tasks):
                    video_name = os.path.basename(task[0])
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
            
            ffmpeg_bin = shutil.which("ffmpeg") or "/opt/homebrew/bin/ffmpeg"
            
            # -fflags +genpts -avoid_negative_ts make_zero 修复片段交界处时间戳不连续导致的播放卡顿
            subprocess.run([
                ffmpeg_bin, '-y', '-f', 'concat', '-safe', '0',
                '-fflags', '+genpts', '-avoid_negative_ts', 'make_zero',
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
    独立工作者：负责单集视频的裁剪（智能亮度检测 或 固定秒数裁剪）
    这个函数会在独立进程中运行
    video_info: (v_path, temp_path, trim_mode, compatible_merge, check_seconds, brightness_threshold,
                 delta_threshold, fixed_trim_seconds, skip_last_episode_trim, is_last)
    """
    (v_path, temp_path, trim_mode, compatible_merge, check_seconds, brightness_threshold, delta_threshold,
     fixed_trim_seconds, skip_last_episode_trim, is_last) = video_info
    
    ffmpeg_bin = shutil.which("ffmpeg") or "/opt/homebrew/bin/ffmpeg"
    
    try:
        if trim_mode == 'fixed_seconds':
            return _trim_fixed_seconds(v_path, temp_path, fixed_trim_seconds,
                                       skip_last_episode_trim, is_last, ffmpeg_bin, compatible_merge)
        else:
            return _trim_brightness(v_path, temp_path, check_seconds,
                                    brightness_threshold, delta_threshold, ffmpeg_bin, compatible_merge)
    except Exception as e:
        print(f"处理失败 {os.path.basename(v_path)}: {e}")
        return False


def _trim_fixed_seconds(v_path, temp_path, trim_seconds, skip_last, is_last, ffmpeg_bin, compatible_merge=False):
    """固定秒数裁剪：每集末尾切除指定秒数（参考短剧剧集合并工具）"""
    if trim_seconds <= 0:
        shutil.copy2(v_path, temp_path)
        return True
    if is_last and skip_last:
        shutil.copy2(v_path, temp_path)
        return True
    
    cap = cv2.VideoCapture(v_path)
    fps = cap.get(cv2.CAP_PROP_FPS)
    total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
    cap.release()
    
    if fps <= 0 or total_frames <= 0:
        return False
    
    duration = total_frames / fps
    cut_point = duration - trim_seconds
    
    if cut_point <= 0:
        return False
    
    if compatible_merge:
        # 使用重编码确保每段以关键帧开头，避免合并后交界处播放卡顿
        subprocess.run([
            ffmpeg_bin, '-y', '-i', v_path,
            '-t', str(cut_point),
            '-c:v', 'libx264', '-preset', 'ultrafast', '-crf', '18',
            '-c:a', 'copy', temp_path
        ], stdout=subprocess.DEVNULL, stderr=subprocess.STDOUT)
    else:
        subprocess.run([
            ffmpeg_bin, '-y', '-i', v_path,
            '-t', str(cut_point),
            '-c', 'copy', temp_path
        ], stdout=subprocess.DEVNULL, stderr=subprocess.STDOUT)
    
    return os.path.exists(temp_path) and os.path.getsize(temp_path) > 1000


def _trim_brightness(v_path, temp_path, check_seconds, brightness_threshold, delta_threshold, ffmpeg_bin, compatible_merge=False):
    """智能亮度检测：分析末尾亮度，自动识别片尾并裁剪"""
    cap = cv2.VideoCapture(v_path)
    fps = cap.get(cv2.CAP_PROP_FPS)
    if fps == 0:
        cap.release()
        return False
    
    total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
    start_frame = max(0, total_frames - int(fps * check_seconds))
    cap.set(cv2.CAP_PROP_POS_FRAMES, start_frame)
    
    brightness_history = []
    frame_idx = start_frame
    cut_frame = total_frames
    
    while True:
        ret, frame = cap.read()
        if not ret:
            break
        
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
    
    if compatible_merge:
        # 使用重编码确保每段以关键帧开头，避免合并后交界处播放卡顿
        subprocess.run([
            ffmpeg_bin, '-y', '-i', v_path,
            '-to', str(cut_time),
            '-c:v', 'libx264', '-preset', 'ultrafast', '-crf', '18',
            '-c:a', 'copy', temp_path
        ], stdout=subprocess.DEVNULL, stderr=subprocess.STDOUT)
    else:
        subprocess.run([
            ffmpeg_bin, '-y', '-i', v_path,
            '-to', str(cut_time),
            '-c', 'copy', temp_path
        ], stdout=subprocess.DEVNULL, stderr=subprocess.STDOUT)
    
    return True

