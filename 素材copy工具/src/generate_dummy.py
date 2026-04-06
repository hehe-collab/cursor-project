import cv2
import numpy as np

def create_dummy():
    # 场景1：蓝色背景，中间白色大圆，代表画面主体
    img1 = np.full((720, 1280, 3), (255, 0, 0), dtype=np.uint8)
    cv2.circle(img1, (640, 300), 150, (255, 255, 255), -1)
    
    # 场景2：模拟经过了轻微调色和画质压缩（颜色稍微变深）
    img2 = np.full((720, 1280, 3), (240, 10, 10), dtype=np.uint8)
    cv2.circle(img2, (640, 300), 150, (240, 240, 240), -1)
    
    # 在图片底部加上不同的字幕干扰（模拟中文素材 vs 印尼语原剧）
    # img1 底部加上绿色中文示意字幕
    cv2.putText(img1, "Chinese Subtitles (Interference)", (200, 650), cv2.FONT_HERSHEY_SIMPLEX, 1.5, (0, 255, 0), 4)
    
    # img2 底部加上红色印尼语示意字幕
    cv2.putText(img2, "Indonesian Subtitles (Interference)", (150, 650), cv2.FONT_HERSHEY_SIMPLEX, 1.5, (0, 0, 255), 4)
    
    # 写入文件
    cv2.imwrite("dummy_chinese.jpg", img1)
    cv2.imwrite("dummy_indo.jpg", img2)
    print("生成了测试图片: dummy_chinese.jpg 和 dummy_indo.jpg")

if __name__ == "__main__":
    create_dummy()
