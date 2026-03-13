import cv2
import imagehash
from PIL import Image
import json
import time

def crop_subtitle_area(image_cv, crop_ratio=0.2):
    """裁剪图片底部区域以消除字幕干扰"""
    height, width = image_cv.shape[:2]
    crop_height = int(height * (1 - crop_ratio))
    return image_cv[:crop_height, :]

def calculate_phash(image_cv):
    """计算感知哈希"""
    image_rgb = cv2.cvtColor(image_cv, cv2.COLOR_BGR2RGB)
    pil_image = Image.fromarray(image_rgb)
    return str(imagehash.phash(pil_image))

def extract_video_features(video_path, target_fps=5, crop_ratio=0.2):
    """
    按指定帧率提取视频特征
    """
    print(f"\n开始分析视频: {video_path}")
    start_time = time.time()
    
    cap = cv2.VideoCapture(video_path)
    if not cap.isOpened():
        print(f"❌ 错误: 无法打开视频文件 {video_path}")
        return None

    # 获取视频原始信息
    original_fps = cap.get(cv2.CAP_PROP_FPS)
    total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
    video_duration = total_frames / original_fps
    
    print(f"视频原始帧率: {original_fps:.2f} fps")
    print(f"视频总时长: {video_duration:.2f} 秒")
    print(f"目标抽取帧率: {target_fps} fps (每秒抽 {target_fps} 帧)")
    
    # 计算需要抽取的帧间隔步长
    # 比如原视频 30fps，目标 5fps，那么步长就是 6（每 6 帧抽 1 帧）
    frame_step = int(max(1, round(original_fps / target_fps)))
    
    features = []
    current_frame = 0
    extracted_count = 0
    
    while True:
        ret, frame = cap.read()
        if not ret:
            break
            
        # 只处理位于步长节点上的帧
        if current_frame % frame_step == 0:
            # 1. 计算当前帧的时间戳（秒）
            timestamp = current_frame / original_fps
            
            # 2. 裁剪字幕区
            cropped_frame = crop_subtitle_area(frame, crop_ratio)
            
            # 3. 提取特征哈希
            frame_hash = calculate_phash(cropped_frame)
            
            features.append({
                "time": round(timestamp, 3),
                "frame_idx": current_frame,
                "hash": frame_hash
            })
            extracted_count += 1
            
        current_frame += 1
        
    cap.release()
    
    elapsed = time.time() - start_time
    print(f"✅ 抽取完成! 共抽出 {extracted_count} 帧特征字典。耗时: {elapsed:.2f} 秒")
    
    return features

if __name__ == "__main__":
    import sys
    
    video_file = "dummy_video.mp4"
    if len(sys.argv) > 1:
        video_file = sys.argv[1]
        
    features = extract_video_features(video_file, target_fps=5)
    
    if features:
        # 保存结果到 JSON 文件以便查看
        out_json = f"{video_file}_features.json"
        with open(out_json, "w", encoding="utf-8") as f:
            json.dump(features, f, indent=2)
            
        print(f"\n特征字典已保存到: {out_json}")
        print("\n特征字典前 5 帧预览:")
        for item in features[:5]:
            print(f"时间: {item['time']:.3f}s | 原始帧号: {item['frame_idx']} | 哈希指纹: {item['hash']}")
