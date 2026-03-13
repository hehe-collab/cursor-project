import cv2
import numpy as np

def generate_video(filename, width=1280, height=720, fps=30, duration=3):
    """
    生成一段模拟视频：
    背景颜色随时间渐变（从蓝色渐变到紫色，再到红色）
    中间有一个随着时间移动和缩放的白色方块
    """
    fourcc = cv2.VideoWriter_fourcc(*'mp4v')
    out = cv2.VideoWriter(filename, fourcc, fps, (width, height))
    
    total_frames = fps * duration
    
    for i in range(total_frames):
        # 计算进度 (0.0 ~ 1.0)
        progress = i / total_frames
        
        # 背景颜色渐变 (BGR)
        # 蓝色(255,0,0) -> 红色(0,0,255)
        b = int(255 * (1 - progress))
        g = 0
        r = int(255 * progress)
        
        frame = np.full((height, width, 3), (b, g, r), dtype=np.uint8)
        
        # 移动的白色方块
        # x 从左向右移动
        x = int(100 + (width - 200) * progress)
        y = height // 2
        # 方块大小随时间变化
        size = int(100 + 50 * np.sin(progress * np.pi * 2))
        
        # 绘制方块
        cv2.rectangle(frame, (x - size, y - size), (x + size, y + size), (255, 255, 255), -1)
        
        # 模拟字幕区 (底部 20% 加个深色遮罩和文字)
        subtitle_y = int(height * 0.8)
        cv2.rectangle(frame, (0, subtitle_y), (width, height), (30, 30, 30), -1)
        cv2.putText(frame, f"Frame: {i} / {total_frames} - Simulated Subtitle", 
                    (50, int(height * 0.9)), cv2.FONT_HERSHEY_SIMPLEX, 1.5, (200, 200, 200), 3)
        
        out.write(frame)
        
    out.release()
    print(f"✅ 成功生成模拟测试视频: {filename} (时长: {duration}秒, FPS: {fps})")

if __name__ == "__main__":
    generate_video("dummy_video.mp4")
