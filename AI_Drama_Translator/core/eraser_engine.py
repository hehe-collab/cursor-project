import cv2
import numpy as np

class EraserEngine:
    def __init__(self):
        pass

    def erase_area(self, frame, rect):
        """
        擦除视频帧中的指定区域
        :param frame: OpenCV 帧
        :param rect: QRect 转换后的坐标 (x, y, w, h)
        :return: 擦除后的帧
        """
        x, y, w, h = rect
        
        # 提取区域
        roi = frame[y:y+h, x:x+w]
        
        # 简单的 OCR 辅助蒙版生成（这里简化为将该区域变模糊或使用 inpaint）
        # 实际更高级的做法是检测文字轮廓再擦除
        mask = np.zeros(roi.shape[:2], dtype=np.uint8)
        # 假设字幕在区域中心，我们先做一个简单的全覆盖蒙版
        mask.fill(255)
        
        # 使用 Telea 算法进行修复
        dst = cv2.inpaint(roi, mask, 3, cv2.INPAINT_TELEA)
        
        # 将修复后的区域放回原图
        frame[y:y+h, x:x+w] = dst
        return frame

    def process_video_batch(self, video_path, rect, output_path, progress_callback=None):
        """
        批量处理整个视频的擦除
        """
        cap = cv2.VideoCapture(video_path)
        fps = cap.get(cv2.CAP_PROP_FPS)
        width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
        height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
        total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
        
        # 使用 Mac 的 VideoToolbox 加速编码
        fourcc = cv2.VideoWriter_fourcc(*'avc1') 
        out = cv2.VideoWriter(output_path, fourcc, fps, (width, height))
        
        count = 0
        while cap.isOpened():
            ret, frame = cap.read()
            if not ret:
                break
            
            # 每一帧都执行擦除
            frame = self.erase_area(frame, rect)
            out.write(frame)
            
            count += 1
            if progress_callback and count % 10 == 0:
                progress_callback(int(count / total_frames * 100))
                
        cap.release()
        out.release()
        return True

