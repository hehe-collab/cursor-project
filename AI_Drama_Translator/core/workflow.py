import os
import cv2
import time
import subprocess
import logging
import shutil
import difflib
import numpy as np
from PIL import Image, ImageDraw, ImageFont
from concurrent.futures import ThreadPoolExecutor, as_completed, CancelledError
from PyQt6.QtCore import QThread, pyqtSignal, QWaitCondition, QMutex
from core.ocr_engine import OCREngine
from core.translator import TranslatorEngine
from core.ffmpeg_worker import FFmpegWorker

logger = logging.getLogger("AI_Drama")

class TranslationWorkflow(QThread):
    progress = pyqtSignal(int, str)
    finished = pyqtSignal(str)
    subtitle_extracted = pyqtSignal(str, list)  # (video_path, subtitle_data)

    def __init__(self, video_files, config):
        super().__init__()
        self.video_files = video_files
        self.config = config
        # 统一标识位
        self.single_step = config.get('step') or config.get('single_step')
        
        self.ocr = OCREngine()
        self.translator = TranslatorEngine(model=config.get('model', 'llama3'))
        self.ffmpeg = FFmpegWorker()
        
        self.is_running = True
        self.is_paused = False
        self.mutex = QMutex()
        self.condition = QWaitCondition()
        
        self.file_progress = {f: 0 for f in video_files}
        self.parallel_count = config.get('parallel_count', 5)
        
        self.active_processes = []
        self.proc_lock = QMutex()
        self.progress_lock = QMutex()
        self.processed_frames = {}
        self._executor = None  # ThreadPoolExecutor，供 stop() 取消未开始的任务

    def stop(self):
        self.is_running = False
        try:
            n = len(self.video_files) or 1
            cur = int(sum(self.file_progress.values()) / n)
            self.progress.emit(min(max(cur, 0), 99), "🛑 正在停止...")
        except Exception:
            self.progress.emit(0, "🛑 正在停止...")
        self.resume()
        self.proc_lock.lock()
        for p in self.active_processes:
            try:
                if p.poll() is None: p.terminate()
            except:
                try: p.kill()
                except: pass
        self.active_processes = []
        self.proc_lock.unlock()
        ex = self._executor
        if ex is not None:
            try:
                ex.shutdown(wait=False, cancel_futures=True)
            except TypeError:
                ex.shutdown(wait=False)
            except Exception as e:
                logger.debug(f"executor shutdown: {e}")

    def resume(self):
        self.mutex.lock()
        self.is_paused = False
        self.condition.wakeAll()
        self.mutex.unlock()

    def pause(self):
        self.mutex.lock()
        self.is_paused = True
        self.mutex.unlock()

    def check_pause(self):
        self.mutex.lock()
        while self.is_paused:
            self.condition.wait(self.mutex)
        self.mutex.unlock()

    def run(self):
        if not self.video_files:
            self.progress.emit(100, "❌ 未选择文件")
            return

        max_workers = self.parallel_count
        executor = ThreadPoolExecutor(max_workers=max_workers)
        self._executor = executor
        try:
            future_to_file = {executor.submit(self.process_single_file, f): f for f in self.video_files}
            for future in as_completed(future_to_file):
                if not self.is_running:
                    break
                try:
                    result = future.result()
                    logger.debug(f"任务完成: 返回 {result}")
                except CancelledError:
                    logger.debug("单个文件任务已取消")
                except Exception as e:
                    logger.error(f"线程执行异常: {e}", exc_info=True)
        finally:
            self._executor = None
            try:
                executor.shutdown(wait=True)
            except Exception:
                pass

        status = "✅ 批量任务已完成" if self.is_running else "🛑 任务已停止"
        self.progress.emit(100, status)
        self.finished.emit("Done")

    def process_single_file(self, video_path):
        if not self.is_running: return
        video_full_name = os.path.basename(video_path)
        
        sub_folder = {"ocr": "OCR_Result", "erase": "Clean_Video", "translate": "Translated_SRT", "burn": "Final_Thai", "scale": "Scaled_Video"}.get(self.single_step, "Output")
        final_out_dir = os.path.join(os.path.dirname(video_path), sub_folder)
        os.makedirs(final_out_dir, exist_ok=True)
        
        out_file_name = os.path.splitext(video_full_name)[0] + (".srt" if self.single_step in ["ocr", "translate"] else ".mp4")
        final_output_path = os.path.join(final_out_dir, out_file_name)
        bitrate = self.config.get('bitrate', '1.3M')

        try:
            self.check_pause()
            if not self.is_running: return

            if self.single_step == 'erase':
                rects = self.config.get('erase_rects')
                if not rects or len(rects) == 0: 
                    logger.error(f"[画面擦除] {video_full_name}: 配置中未找到擦除区域参数")
                    self.update_overall_progress(video_path, 0, "❌ 未找到擦除区域")
                    return
                
                logger.info(f"[画面擦除] {video_full_name}: 收到 {len(rects)} 个擦除区域")
                
                # --- 多区域擦除：偶数对齐 + 边界收缩 + 级联滤镜 ---
                cap = cv2.VideoCapture(video_path)
                vw = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
                vh = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
                cap.release()

                validated_rects = []
                for i, rect in enumerate(rects):
                    x, y, w, h = rect
                    x = max(0, int(x)); y = max(0, int(y))
                    w = min(vw - x, int(w)); h = min(vh - y, int(h))
                    
                    if x % 2 != 0: x += 1
                    if y % 2 != 0: y += 1
                    if w % 2 != 0: w -= 1
                    if h % 2 != 0: h -= 1
                    
                    if w >= 4 and h >= 4:
                        validated_rects.append((x, y, w, h))
                        logger.debug(f"[画面擦除] 擦除框 {i+1}: 校正后坐标 x={x}, y={y}, w={w}, h={h} ✅")
                    else:
                        logger.warning(f"[画面擦除] 擦除框 {i+1} 太小，已跳过: x={x}, y={y}, w={w}, h={h}")
                
                if not validated_rects:
                    logger.error(f"[画面擦除] {video_full_name}: 所有擦除框验证失败（太小或越界）")
                    self.update_overall_progress(video_path, 0, "❌ 所有选框都太小")
                    return

                # 构建级联 delogo 滤镜：delogo=...,delogo=...,delogo=...
                delogo_filters = []
                for x, y, w, h in validated_rects:
                    delogo_filters.append(f"delogo=x={x}:y={y}:w={w}:h={h}:show=0")
                
                vf_chain = ",".join(delogo_filters)
                logger.info(f"✨ [画面擦除] {video_full_name}: 执行多区域擦除 [{len(validated_rects)} 个区域]")
                logger.info(f"[画面擦除] FFmpeg 滤镜链: {vf_chain}")

                cmd = [
                    self.ffmpeg.ffmpeg_bin, '-y',
                    '-i', os.path.abspath(video_path),
                    '-vf', vf_chain,
                    '-c:v', 'h264_videotoolbox', '-b:v', bitrate,
                    '-pix_fmt', 'yuv420p', '-c:a', 'copy', os.path.abspath(final_output_path)
                ]
                
                logger.info(f"[画面擦除] 开始执行 FFmpeg 命令...")
                p = subprocess.Popen(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE, text=True)
                self.proc_lock.lock(); self.active_processes.append(p); self.proc_lock.unlock()
                _, stderr = p.communicate()
                time.sleep(0.5)  # 强制同步文件系统缓存
                
                success = (p.returncode == 0 and os.path.exists(final_output_path) and os.path.getsize(final_output_path) > 1000)
                if not success:
                    logger.error(f"[画面擦除] 硬件加速失败 {video_full_name}, 返回码: {p.returncode}")
                    logger.error(f"[画面擦除] FFmpeg 错误信息: {stderr[-500:]}")  # 只记录最后500字符
                    
                    # 清理失败的输出文件
                    if os.path.exists(final_output_path) and os.path.getsize(final_output_path) == 0:
                        os.remove(final_output_path)
                        logger.info(f"[画面擦除] 已清理0B文件")
                    
                    # 尝试降级到 CPU 编码（libx264）再试一次
                    logger.info(f"[画面擦除] 尝试降级到 CPU 编码 (libx264)...")
                    cmd_cpu = [
                        self.ffmpeg.ffmpeg_bin, '-y',
                        '-i', os.path.abspath(video_path),
                        '-vf', vf_chain,
                        '-c:v', 'libx264', '-preset', 'superfast', '-b:v', bitrate,
                        '-pix_fmt', 'yuv420p', '-c:a', 'copy', os.path.abspath(final_output_path)
                    ]
                    p2 = subprocess.Popen(cmd_cpu, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE, text=True)
                    _, stderr2 = p2.communicate()
                    time.sleep(0.5)  # 强制同步文件系统缓存
                    success = (p2.returncode == 0 and os.path.exists(final_output_path) and os.path.getsize(final_output_path) > 1000)
                    if not success:
                        logger.error(f"[画面擦除] CPU 编码也失败，返回码: {p2.returncode}")
                        logger.error(f"[画面擦除] FFmpeg 错误信息: {stderr2[-500:]}")
                        # 清理失败的输出文件
                        if os.path.exists(final_output_path):
                            os.remove(final_output_path)
                    else:
                        logger.info(f"[画面擦除] CPU 编码成功 ✅")
                else:
                    logger.info(f"[画面擦除] 硬件加速成功 ✅")

                status_msg = f"✅ 擦除完成({len(validated_rects)}区域)" if success else "❌ 擦除失败"
                self.update_overall_progress(video_path, 100 if success else 0, status_msg)

            elif self.single_step == 'scale':
                # 分辨率调整逻辑（可选统一帧率）
                target_width = self.config.get('target_width', 1080)
                target_height = self.config.get('target_height', 1920)
                target_fps = self.config.get('target_fps')  # 如 "25" 或 "30"
                
                vf_parts = [f"scale={target_width}:{target_height}"]
                if target_fps:
                    try:
                        fps_int = int(target_fps)
                        vf_parts.append(f"fps={fps_int}")
                        logger.info(f"✨ 调整分辨率+统一帧率: {video_full_name} -> {target_width}x{target_height} @ {fps_int}fps")
                    except (ValueError, TypeError):
                        logger.info(f"✨ 调整分辨率: {video_full_name} -> {target_width}x{target_height}")
                else:
                    logger.info(f"✨ 调整分辨率: {video_full_name} -> {target_width}x{target_height}")
                
                vf_str = ','.join(vf_parts)
                cmd = [
                    self.ffmpeg.ffmpeg_bin, '-y',
                    '-i', os.path.abspath(video_path),
                    '-vf', vf_str,
                    '-c:v', 'h264_videotoolbox', '-b:v', bitrate,
                    '-pix_fmt', 'yuv420p', '-c:a', 'copy', os.path.abspath(final_output_path)
                ]
                
                p = subprocess.Popen(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE, text=True)
                self.proc_lock.lock(); self.active_processes.append(p); self.proc_lock.unlock()
                
                # 监控进度
                while p.poll() is None:
                    if not self.is_running:
                        p.terminate()
                        return
                    time.sleep(0.1)
                
                _, stderr = p.communicate()
                time.sleep(0.5)  # 强制同步文件系统缓存
                success = (p.returncode == 0 and os.path.exists(final_output_path) and os.path.getsize(final_output_path) > 1000)
                
                if not success:
                    logger.error(f"分辨率调整失败 {video_full_name}, 返回码: {p.returncode}")
                    logger.error(f"FFmpeg 错误信息: {stderr[-500:]}")
                    
                    # 尝试降级到 CPU 编码
                    logger.info(f"[调分辨率] 硬件加速失败，尝试 CPU 编码 (libx264)...")
                    cmd_cpu = [
                        self.ffmpeg.ffmpeg_bin, '-y',
                        '-i', os.path.abspath(video_path),
                        '-vf', vf_str,
                        '-c:v', 'libx264', '-preset', 'medium', '-b:v', bitrate,
                        '-pix_fmt', 'yuv420p', '-c:a', 'copy', os.path.abspath(final_output_path)
                    ]
                    p2 = subprocess.Popen(cmd_cpu, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE, text=True)
                    _, stderr2 = p2.communicate()
                    time.sleep(0.5)  # 强制同步文件系统缓存
                    success = (p2.returncode == 0 and os.path.exists(final_output_path) and os.path.getsize(final_output_path) > 1000)
                    if not success:
                        logger.error(f"[调分辨率] CPU 编码也失败，返回码: {p2.returncode}")
                        logger.error(f"[调分辨率] FFmpeg 错误信息: {stderr2[-500:]}")
                        if os.path.exists(final_output_path):
                            os.remove(final_output_path)
                    else:
                        logger.info(f"[调分辨率] CPU 编码成功 ✅")

                self.update_overall_progress(video_path, 100 if success else 0, "✅ 转换完成" if success else "❌ 转换失败")

            elif self.single_step == 'ocr':
                # OCR 逻辑：提取字幕并发送信号
                logger.info(f"[字幕提取] 开始处理: {video_full_name}")
                rect = self.config.get('ocr_rect')
                if not rect: 
                    logger.error(f"[字幕提取] {video_full_name}: 未找到OCR区域配置")
                    self.update_overall_progress(video_path, 0, "❌ 未设置OCR区域")
                    return
                
                logger.info(f"[字幕提取] {video_full_name}: OCR区域 = {rect}")
                self.update_overall_progress(video_path, 5, "🚀 开始提取...")
                
                try:
                    # 调高精度 OCR 提取逻辑
                    srt_text = self.extract_subtitles_high_precision(video_path, video_full_name, rect)
                    
                    if not srt_text or not srt_text.strip():
                        logger.warning(f"[字幕提取] {video_full_name}: OCR结果为空")
                        self.update_overall_progress(video_path, 100, "⚠️ 未识别到字幕")
                        return
                    
                    # 解析 SRT 文本为结构化数据
                    subtitle_data = self.parse_srt_text(srt_text)
                    logger.info(f"[字幕提取] {video_full_name}: 解析到 {len(subtitle_data)} 条字幕")
                    
                    # 发送信号，让主窗口缓存数据
                    self.subtitle_extracted.emit(video_path, subtitle_data)
                    
                    # 仍然写入文件（作为备份）
                    with open(final_output_path, "w", encoding="utf-8") as f: 
                        f.write(srt_text)
                    logger.info(f"[字幕提取] {video_full_name}: SRT文件已保存")
                    
                    self.update_overall_progress(video_path, 100, "✅ 提取完成")
                
                except Exception as e:
                    logger.error(f"[字幕提取] {video_full_name}: OCR异常 - {e}", exc_info=True)
                    self.update_overall_progress(video_path, 0, f"❌ OCR失败: {str(e)[:20]}")
                    raise

            # ... 翻译与压制逻辑保持不变 ...
            elif self.single_step == 'translate':
                with open(video_path, "r", encoding="utf-8") as f: content = f.read()
                translated = self.translate_subtitles(content)
                with open(final_output_path, "w", encoding="utf-8") as f: f.write(translated)
                self.update_overall_progress(video_path, 100, "✅ 翻译完成")
            
            elif self.single_step == 'burn':
                # 这里复用之前的 Pillow 压制逻辑
                self.process_burn_task(video_path, final_output_path, bitrate)

        except Exception as e:
            logger.error(f"[任务处理] {video_full_name} 处理失败: {e}", exc_info=True)
            self.update_overall_progress(video_path, 0, f"❌ 异常: {str(e)[:30]}")

    def update_overall_progress(self, video_path, progress_val, status_text):
        self.file_progress[video_path] = progress_val
        total_p = sum(self.file_progress.values()) / (len(self.video_files) or 1)
        self.progress.emit(int(total_p), status_text)

    def extract_subtitles_high_precision(self, video_path, video_name, rect):
        import difflib; from collections import Counter
        cap = cv2.VideoCapture(video_path)
        fps = cap.get(5) or 25.0
        total_frames = int(cap.get(7))
        duration = total_frames / fps
        raw_samples = []
        
        # 【简化采样策略】每 0.25 秒采样一次
        step = 0.25  # 采样间隔（秒）
        
        logger.info(f"[字幕提取] 视频信息: FPS={fps:.2f}, 总帧数={total_frames}, 时长={duration:.2f}秒")
        
        total_samples = int(duration / step) + 1
        logger.info(f"[字幕提取] 采样策略: 每 {step} 秒采样一次，预计采样 {total_samples} 次")
        logger.info(f"[字幕提取] {video_name}: 开始OCR处理...")
        
        for t_idx in range(total_samples):
            if not self.is_running: 
                logger.warning(f"[字幕提取] {video_name}: 用户中断")
                break
            self.check_pause()
            
            try:
                t = min(t_idx * step, duration)
                cap.set(cv2.CAP_PROP_POS_FRAMES, int(t * fps))
                ret, frame = cap.read()
                if not ret: 
                    logger.warning(f"[字幕提取] {video_name}: 读取帧失败 t={t:.2f}s")
                    break
                
                fh, fw = frame.shape[:2]
                px, py, pw, ph = rect
                pad = 15  # 保留优化：减少 padding 提高单字识别
                roi = frame[max(0, py-pad):min(fh, py+ph+pad), max(0, px-pad):min(fw, px+pw+pad)]
                res = self.ocr.recognize_text(roi, lambda: self.is_running)
                text = "".join(res).strip()
                raw_samples.append({'time': t, 'text': text})
                
                # 每处理10%的进度更新一次
                if t_idx % max(1, total_samples // 10) == 0:
                    progress = int((t_idx / total_samples) * 90)  # OCR占90%进度
                    self.update_overall_progress(video_path, progress, f"🔍 OCR处理中... {progress}%")
                    logger.info(f"[字幕提取] {video_name}: OCR进度 {progress}% ({t_idx}/{total_samples})")
            
            except Exception as e:
                logger.error(f"[字幕提取] {video_name}: 采样 {t_idx} 出错: {e}")
                continue
        
        cap.release()
        logger.info(f"[字幕提取] {video_name}: 实际采样 {len(raw_samples)} 次，开始字幕合并...")
        # 多数表决融合逻辑...
        self.update_overall_progress(video_path, 90, "📝 字幕合并中...")
        logger.info(f"[字幕提取] {video_name}: 开始字幕分组和合并...")
        
        processed = []
        if not raw_samples: 
            logger.warning(f"[字幕提取] {video_name}: 没有采样数据，返回空字幕")
            return ""
        
        curr_g = [raw_samples[0]]
        for i in range(1, len(raw_samples)):
            sim = difflib.SequenceMatcher(None, raw_samples[i]['text'], curr_g[-1]['text']).ratio()
            if sim > 0.6: 
                curr_g.append(raw_samples[i])
            else:
                e = self.finalize_group(curr_g); 
                if e: processed.append(e)
                curr_g = [raw_samples[i]]
        
        e = self.finalize_group(curr_g); 
        if e: processed.append(e)
        
        # 方案2：仅保留含中文的字幕，过滤纯英文/数字误提取（如地面、logo）
        def _has_cjk(t):
            return any('\u4e00' <= c <= '\u9fff' for c in t)
        before_count = len(processed)
        processed = [e for e in processed if _has_cjk(e['text'])]
        if before_count > len(processed):
            logger.info(f"[字幕提取] {video_name}: 过滤纯英文/数字 {before_count - len(processed)} 条")
        
        # 方案4：偏移改为在预览/压制时应用，此处保存原始时间
        self.update_overall_progress(video_path, 95, "✍️ 生成SRT...")
        logger.info(f"[字幕提取] {video_name}: 合并完成，生成 {len(processed)} 条字幕")
        
        srt_output = "\n".join([f"{i+1}\n{self.format_time(e['start'])} --> {self.format_time(e['end'])}\n{e['text']}\n" for i, e in enumerate(processed)])
        logger.info(f"[字幕提取] {video_name}: SRT生成完成")
        
        return srt_output

    def finalize_group(self, group):
        from collections import Counter
        v = [s['text'] for s in group if s['text']]
        if not v: return None
        
        c = Counter(v)
        b = sorted(c.keys(), key=lambda x: (c[x], len(x)), reverse=True)[0]
        
        # 【简化时间计算】开始时间 + 固定结束偏移
        start_time = group[0]['time']
        end_time = group[-1]['time'] + 0.25  # 结束时间：最后采样点 + 0.25秒
        
        logger.debug(f"[字幕组] '{b}' 时间: {start_time:.3f}s -> {end_time:.3f}s (采样点: {len(group)})")
        
        return {'start': start_time, 'end': end_time, 'text': b}

    def format_time(self, s):
        ms = int((s - int(s)) * 1000); s = int(s)
        return f"{s//3600:02d}:{(s%3600)//60:02d}:{s%60:02d}.{ms:03d}"

    def parse_srt_text(self, text):
        """解析SRT文本为列表 [(start, end, text), ...]"""
        import re
        entries = []
        blocks = text.strip().split('\n\n')
        for b in blocks:
            lines = b.split('\n')
            if len(lines) >= 3:
                # 修复：同时支持逗号和点号作为毫秒分隔符（标准SRT使用逗号）
                time_match = re.search(r'(\d+:\d+:\d+[,\.]\d+)\s*-->\s*(\d+:\d+:\d+[,\.]\d+)', lines[1])
                if time_match:
                    start = self.srt_time_to_seconds(time_match.group(1))
                    end = self.srt_time_to_seconds(time_match.group(2))
                    content = "\n".join(lines[2:])
                    entries.append((start, end, content))
        return entries

    def srt_time_to_seconds(self, t_str):
        # 修复：将逗号替换为点号，兼容标准SRT格式
        t_str = t_str.replace(',', '.')
        h, m, s = t_str.split(':')
        s, ms = s.split('.')
        return int(h)*3600 + int(m)*60 + int(s) + int(ms)/1000.0

    def translate_subtitles(self, content):
        """调用 AI 模型翻译字幕"""
        import re
        blocks = content.strip().split('\n\n')
        translated_blocks = []
        
        total = len(blocks)
        for i, b in enumerate(blocks):
            if not self.is_running: break
            self.check_pause()
            
            lines = b.split('\n')
            if len(lines) >= 3:
                # 提取纯文本内容
                text_to_translate = "\n".join(lines[2:])
                
                # 调用翻译引擎
                translated_text = self.translator.translate(text_to_translate)
                
                # 重新构建 SRT 块
                lines[2:] = [translated_text]
                translated_blocks.append("\n".join(lines))
            
            # 更新翻译进度
            progress = int((i / total) * 100)
            self.progress.emit(progress, f"🌐 翻译中... {progress}%")
            
        return "\n\n".join(translated_blocks)

    def _burn_font_colors(self, color_name):
        """与 video_preview  burn 模式「白色/黄色/红色/绿色」一致，返回 (填充 RGB, 描边 RGB)。"""
        key = (color_name or "白色").strip()
        fill_map = {
            "白色": (255, 255, 255),
            "黄色": (255, 255, 0),
            "红色": (255, 0, 0),
            "绿色": (0, 255, 0),
        }
        fill = fill_map.get(key, (255, 255, 255))
        return fill, (0, 0, 0)

    def wrap_text(self, text, font, max_width, draw):
        """自动换行逻辑 - 智能支持中文（按字符）和西方语言（按单词）"""
        if not text: return []
        
        # 如果包含显式的换行符，先按其分割
        paragraphs = text.split('\n')
        lines = []
        
        for p in paragraphs:
            if not p: continue
            
            # 智能判断：如果包含空格，按单词分割（印尼语、英语等）；否则按字符分割（中文）
            if ' ' in p:
                # 西方语言：按空格分割单词，保持单词完整性
                words = p.split(' ')
                current_line = ""
                
                for word in words:
                    # 测试添加这个单词后的宽度
                    test_line = (current_line + " " + word).strip() if current_line else word
                    bbox = draw.textbbox((0, 0), test_line, font=font)
                    w = bbox[2] - bbox[0]
                    
                    if w <= max_width:
                        current_line = test_line
                    else:
                        # 当前行已满，换行
                        if current_line:
                            lines.append(current_line)
                        # 检查单个单词是否超宽（极端情况）
                        bbox_word = draw.textbbox((0, 0), word, font=font)
                        if bbox_word[2] - bbox_word[0] > max_width:
                            # 单词太长，强制按字符拆分
                            current_line = ""
                            for char in word:
                                test_char = current_line + char
                                bbox_char = draw.textbbox((0, 0), test_char, font=font)
                                if bbox_char[2] - bbox_char[0] <= max_width:
                                    current_line = test_char
                                else:
                                    if current_line:
                                        lines.append(current_line)
                                    current_line = char
                        else:
                            current_line = word
                
                if current_line:
                    lines.append(current_line)
            else:
                # 中文等：按字符分割
                current_line = ""
                for char in p:
                    test_line = current_line + char
                    bbox = draw.textbbox((0, 0), test_line, font=font)
                    w = bbox[2] - bbox[0]
                    
                    if w <= max_width:
                        current_line = test_line
                    else:
                        if current_line:
                            lines.append(current_line)
                        current_line = char
                
                if current_line:
                    lines.append(current_line)
                
        return lines

    def process_burn_task(self, video_path, output_path, bitrate):
        """Pillow 逐帧绘制压制逻辑 (回退稳定版)"""
        import numpy as np
        from PIL import Image, ImageDraw, ImageFont
        
        video_name = os.path.basename(video_path)
        video_dir = os.path.dirname(video_path)
        video_base = os.path.splitext(video_name)[0]
        parent_dir = os.path.dirname(video_dir)

        # 1. 优先使用 UI 层传入的 video_srt_map（用户导入或 auto_match 已匹配）
        target_srt = None
        video_srt_map = self.config.get("video_srt_map") or {}
        abs_video = os.path.abspath(video_path)
        if abs_video in video_srt_map:
            cand = video_srt_map[abs_video]
            if isinstance(cand, str) and os.path.exists(cand):
                target_srt = cand

        # 2. 按约定目录顺序查找：同目录、Translated_SRT、父级 OCR_Result / Translated_SRT
        if not target_srt:
            candidates = [
                os.path.join(video_dir, f"{video_base}.srt"),
                os.path.join(video_dir, "Translated_SRT", f"{video_base}.srt"),
                os.path.join(parent_dir, "OCR_Result", f"{video_base}.srt"),
                os.path.join(parent_dir, "Translated_SRT", f"{video_base}.srt"),
            ]
            for p in candidates:
                if os.path.exists(p):
                    target_srt = p
                    break

        if not target_srt or not os.path.exists(target_srt):
            hint = target_srt or f"已查找: 同目录、Translated_SRT、父级 OCR_Result/Translated_SRT"
            logger.error(f"❌ [{video_name}] 未找到字幕文件: {hint}")
            self.update_overall_progress(video_path, 0, "❌ 缺失SRT")
            return

        # 解析字幕
        srt_entries = []
        try:
            with open(target_srt, "r", encoding="utf-8") as f:
                content = f.read()
                srt_entries = self.parse_srt_text(content)
            # 方案4：压制时应用用户可调偏移（负=提前）
            offset = self.config.get('subtitle_time_offset', 0)
            if offset != 0:
                srt_entries = [(max(0, s + offset), max(max(0, s + offset), e + offset), t) for s, e, t in srt_entries]
                logger.info(f"[字幕压制] {video_name}: 已应用时间偏移 {offset:+.2f}s")
            logger.info(f"[字幕压制] {video_name} -> SRT: {os.path.basename(target_srt)}")
            logger.info(f"[字幕压制] 成功解析 {len(srt_entries)} 条字幕")
        except Exception as e:
            logger.error(f"❌ [{video_name}] 解析SRT失败: {e}")
            self.update_overall_progress(video_path, 0, "❌ SRT解析失败")
            return

        cap = cv2.VideoCapture(video_path)
        w = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
        h = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
        fps = cap.get(cv2.CAP_PROP_FPS) or 25.0
        total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
        logger.info(f"[字幕压制] 视频信息: {w}x{h}, {fps:.2f}fps, {total_frames}帧")

        # 字号/颜色与 UI 预览 font_config 一致（Pillow 为像素级字号，与预览 SpinBox 数值对应）
        cfg_font = self.config.get("font_size")
        if cfg_font is not None:
            f_size = max(8, min(200, int(cfg_font)))
        else:
            f_size = max(8, int(h * 0.04))
        color_name = self.config.get("font_color") or "白色"
        fill_rgb, stroke_rgb = self._burn_font_colors(color_name)

        # 加载字体
        font_path = "/System/Library/Fonts/Supplemental/Arial Unicode.ttf"
        try:
            font = ImageFont.truetype(font_path, f_size)
            logger.info(f"[字幕压制] 成功加载字体: {font_path}, 大小: {f_size}px, 颜色: {color_name}")
        except:
            font = ImageFont.load_default()
            logger.warning("[字幕压制] 字体加载失败，使用默认字体")

        # 确定字幕位置（使用配置中的 blue_rect Y坐标）
        burn_rect = self.config.get('blue_rect')  # 修复：键名与 main_window.py 保持一致
        target_y = int(h * 0.85)
        if burn_rect:
            target_y = burn_rect[1] + burn_rect[3] // 2
            logger.info(f"[字幕压制] 使用蓝框中心位置: Y={target_y}")

        # 设置编码器
        parallel_count = self.config.get('burn_parallel_count', 4)
        use_hardware = (parallel_count <= 4)
        encoding_type = "硬件加速" if use_hardware else "CPU"
        
        cmd = [
            self.ffmpeg.ffmpeg_bin, '-y',
            '-f', 'rawvideo', '-pix_fmt', 'bgr24',
            '-s', f'{w}x{h}', '-r', str(fps),
            '-i', '-',
            '-i', os.path.abspath(video_path),
            '-map', '0:v:0', '-map', '1:a:0?',
            '-c:v', 'h264_videotoolbox' if use_hardware else 'libx264',
            '-b:v', bitrate, '-pix_fmt', 'yuv420p',
            '-c:a', 'copy',
            os.path.abspath(output_path)
        ]
        if not use_hardware:
            cmd.insert(cmd.index('-c:v') + 2, '-preset')
            cmd.insert(cmd.index('-preset') + 1, 'superfast')

        logger.info(f"[字幕压制] 使用{encoding_type}编码 ({'h264_videotoolbox' if use_hardware else 'libx264'})")

        try:
            proc = subprocess.Popen(cmd, stdin=subprocess.PIPE, stderr=subprocess.PIPE)
            self.proc_lock.lock()
            self.active_processes.append(proc)
            self.proc_lock.unlock()
            logger.info(f"[字幕压制] FFmpeg进程已启动")
        except Exception as e:
            logger.error(f"❌ [{video_name}] 启动FFmpeg失败: {e}")
            return

        f_idx = 0
        last_progress_update = 0
        
        try:
            while self.is_running:
                self.check_pause()
                ret, frame = cap.read()
                if not ret: break
                
                cur_t = f_idx / fps
                subtitle_text = ""
                for start, end, text in srt_entries:
                    if start <= cur_t <= end:
                        subtitle_text = text
                        break
                
                if subtitle_text:
                    img = Image.fromarray(cv2.cvtColor(frame, cv2.COLOR_BGR2RGB))
                    draw = ImageDraw.Draw(img)
                    
                    # 自动换行
                    max_w = int(w * 0.9)
                    wrapped_lines = self.wrap_text(subtitle_text, font, max_w, draw)
                    
                    line_h = f_size + int(f_size * 0.3)
                    total_text_h = len(wrapped_lines) * line_h
                    start_y = target_y - (total_text_h // 2)

                    for i, line in enumerate(wrapped_lines):
                        bbox = draw.textbbox((0, 0), line, font=font)
                        tw = bbox[2] - bbox[0]
                        tx = (w - tw) // 2
                        ty = start_y + i * line_h

                        sw = max(2, f_size // 20)
                        draw.text((tx + sw, ty + sw), line, font=font, fill=(0, 0, 0, 180)) # 阴影
                        draw.text((tx, ty), line, font=font, fill=fill_rgb,
                                stroke_width=sw, stroke_fill=stroke_rgb)
                    
                    frame = cv2.cvtColor(np.array(img), cv2.COLOR_RGB2BGR)
                
                try:
                    proc.stdin.write(frame.tobytes())
                except:
                    logger.error(f"❌ [{video_name}] FFmpeg管道断开，可能编码失败。")
                    break
                
                f_idx += 1
                if f_idx % 30 == 0:
                    progress = int((f_idx / total_frames) * 100)
                    if progress > last_progress_update + 2:  # 每增加2%更新一次
                        self.update_overall_progress(video_path, progress, f"⚡ {encoding_type}中... {progress}%")
                        last_progress_update = progress
            
            # 步骤10: 完成编码
            try:
                proc.stdin.flush()
                proc.stdin.close()
            except:
                pass
            
            proc.wait()
            time.sleep(0.5)  # 强制等待 0.5 秒，让系统刷新文件句柄和磁盘缓存
            cap.release()
            
            # 清理进程
            self.proc_lock.lock()
            if proc in self.active_processes:
                self.active_processes.remove(proc)
            self.proc_lock.unlock()
            
            # 步骤11: 检查结果
            if os.path.exists(output_path):
                file_size = os.path.getsize(output_path)
                if file_size == 0:
                    logger.error(f"❌ [{video_name}] 输出文件为0B，编码失败")
                    
                    # 如果是硬件加速失败，尝试CPU回退
                    if use_hardware:
                        logger.warning(f"⚠️ [{video_name}] 硬件加速失败，尝试CPU编码重试...")
                        os.remove(output_path)
                        
                        # 重置视频到开头
                        cap = cv2.VideoCapture(video_path)
                        
                        # 使用CPU编码重试
                        cmd_cpu = [
                            self.ffmpeg.ffmpeg_bin, '-y',
                            '-f', 'rawvideo', '-pix_fmt', 'bgr24',
                            '-s', f'{w}x{h}', '-r', str(fps),
                            '-i', '-',
                            '-i', os.path.abspath(video_path),
                            '-map', '0:v:0', '-map', '1:a:0?',
                            '-c:v', 'libx264', '-preset', 'medium',
                            '-b:v', bitrate, '-pix_fmt', 'yuv420p',
                            '-c:a', 'copy',
                            os.path.abspath(output_path)
                        ]
                        
                        proc2 = subprocess.Popen(cmd_cpu, stdin=subprocess.PIPE, stderr=subprocess.PIPE)
                        self.proc_lock.lock()
                        self.active_processes.append(proc2)
                        self.proc_lock.unlock()
                        
                        self.update_overall_progress(video_path, 5, "🔄 CPU编码重试...")
                        
                        f_idx = 0
                        last_progress_update = 0
                        
                        while self.is_running:
                            self.check_pause()
                            ret, frame = cap.read()
                            if not ret: break
                            
                            cur_t = f_idx / fps
                            sub = ""
                            for e in srt_entries:
                                if e[0] <= cur_t <= e[1]:
                                    sub = e[2]
                                    break
                            
                            if sub:
                                img = Image.fromarray(cv2.cvtColor(frame, cv2.COLOR_BGR2RGB))
                                draw = ImageDraw.Draw(img)
                                
                                # 自动换行处理
                                max_width = int(w * 0.9)
                                lines = self.wrap_text(sub, font, max_width, draw)
                                
                                # 计算总高度
                                line_height = f_size + int(f_size * 0.3)
                                total_height = len(lines) * line_height
                                
                                # 起始Y坐标（多行居中）
                                start_y = target_y - (total_height // 2)
                                
                                # 逐行绘制
                                for i, line in enumerate(lines):
                                    bbox = draw.textbbox((0, 0), line, font=font)
                                    tw = bbox[2] - bbox[0]
                                    tx = (w - tw) // 2
                                    ty = start_y + i * line_height
                                    
                                    draw.text((tx + 2, ty + 2), line, font=font, fill=(0, 0, 0, 180))
                                    draw.text((tx, ty), line, font=font, fill=fill_rgb,
                                             stroke_width=max(2, f_size // 20), stroke_fill=stroke_rgb)
                                
                                frame = cv2.cvtColor(np.array(img), cv2.COLOR_RGB2BGR)
                            
                            try:
                                proc2.stdin.write(frame.tobytes())
                            except:
                                break
                            
                            f_idx += 1
                            if f_idx % 30 == 0:
                                progress = int((f_idx / total_frames) * 100)
                                if progress > last_progress_update + 2:
                                    self.update_overall_progress(video_path, progress, f"🔄 CPU编码中... {progress}%")
                                    last_progress_update = progress
                        
                        try:
                            proc2.stdin.flush()
                            proc2.stdin.close()
                        except:
                            pass
                        proc2.wait()
                        time.sleep(0.5)  # 强制同步文件系统缓存
                        cap.release()
                        
                        self.proc_lock.lock()
                        if proc2 in self.active_processes:
                            self.active_processes.remove(proc2)
                        self.proc_lock.unlock()
                        
                        # 检查CPU编码结果
                        if os.path.exists(output_path) and os.path.getsize(output_path) > 1000:
                            logger.info(f"✅ [{video_name}] CPU编码成功（回退方案）")
                            file_size = os.path.getsize(output_path)
                            logger.info(f"✅ [{video_name}] 压制完成，文件大小: {file_size / 1024 / 1024:.2f} MB")
                            self.update_overall_progress(video_path, 100, "✅ 压制完成")
                        else:
                            logger.error(f"❌ [{video_name}] CPU编码也失败")
                            self.update_overall_progress(video_path, 0, "❌ 编码失败")
                            if os.path.exists(output_path):
                                try:
                                    os.remove(output_path)
                                except:
                                    pass
                    else:
                        os.remove(output_path)
                        self.update_overall_progress(video_path, 0, "❌ 文件0B")
                elif file_size < 1000:
                    logger.error(f"❌ [{video_name}] 输出文件过小({file_size}字节)")
                    self.update_overall_progress(video_path, 0, "❌ 文件过小")
                else:
                    logger.info(f"✅ [{video_name}] 压制完成，文件大小: {file_size / 1024 / 1024:.2f} MB")
                    self.update_overall_progress(video_path, 100, "✅ 压制完成")
            else:
                logger.error(f"❌ [{video_name}] 输出文件未生成")
                self.update_overall_progress(video_path, 0, "❌ 文件未生成")
        
        except Exception as e:
            logger.error(f"❌ [{video_name}] 压制过程发生异常: {e}", exc_info=True)
            cap.release()
            
            self.proc_lock.lock()
            if proc in self.active_processes:
                self.active_processes.remove(proc)
            self.proc_lock.unlock()
            
            self.update_overall_progress(video_path, 0, f"❌ 异常: {str(e)[:20]}")
