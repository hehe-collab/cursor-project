import os
from scenedetect import detect, ContentDetector

def detect_scenes_in_video(video_path):
    """
    使用 PySceneDetect 检测视频中的场景（硬切点）
    """
    print(f"\n开始对测试素材进行场景切分分析: {video_path}")
    
    # 使用 ContentDetector 来寻找硬切转场 (默认 threshold 27 一般够用)
    scene_list = detect(video_path, ContentDetector(threshold=27))
    
    print(f"✅ 分析完成！共发现 {len(scene_list)} 个独立镜头(Shot)。")
    
    results = []
    for i, scene in enumerate(scene_list):
        start_time = scene[0].get_seconds()
        end_time = scene[1].get_seconds()
        duration = end_time - start_time
        print(f"  - Shot {i+1}: {start_time:.3f}秒 -> {end_time:.3f}秒 (时长: {duration:.3f}秒)")
        
        results.append({
            "shot_id": i + 1,
            "start": start_time,
            "end": end_time,
            "duration": duration
        })
        
    return results

if __name__ == "__main__":
    import sys
    video_file = "test_ad.mp4"
    if len(sys.argv) > 1:
        video_file = sys.argv[1]
        
    if not os.path.exists(video_file):
        print(f"找不到文件: {video_file}")
    else:
        detect_scenes_in_video(video_file)
