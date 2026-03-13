import json
import time

def load_features(json_path):
    """加载之前生成的特征字典"""
    with open(json_path, 'r', encoding='utf-8') as f:
        return json.load(f)

def hex_to_bin(hex_str):
    """将十六进制哈希字符串转为二进制整数"""
    return int(hex_str, 16)

def hamming_distance(hash1, hash2):
    """计算两个64位哈希的汉明距离"""
    # 异或后统计二进制中 1 的个数
    x = hash1 ^ hash2
    return bin(x).count('1')

def search_clip_in_dict(clip_features, dict_features, threshold=10):
    """
    在长视频字典中，搜索短片段的位置 (类似滑动窗口匹配)
    """
    print(f"\n开始搜索... 短片段帧数: {len(clip_features)}, 字典帧数: {len(dict_features)}")
    start_time = time.time()
    
    # 提前把字典的哈希全部转为二进制整数，加速计算
    dict_hashes = [hex_to_bin(item['hash']) for item in dict_features]
    clip_hashes = [hex_to_bin(item['hash']) for item in clip_features]
    
    clip_len = len(clip_hashes)
    dict_len = len(dict_hashes)
    
    best_match_start_idx = -1
    best_match_score = float('inf')  # 越小越好 (平均汉明距离)
    
    # 滑动窗口遍历字典
    for i in range(dict_len - clip_len + 1):
        total_distance = 0
        
        # 逐帧对比窗口内的哈希
        for j in range(clip_len):
            dist = hamming_distance(clip_hashes[j], dict_hashes[i + j])
            total_distance += dist
            
            # 早期熔断 (Early Exiting)：如果前面几帧距离已经很大，直接跳过当前窗口，极大提速
            if total_distance > threshold * clip_len:
                break
                
        avg_distance = total_distance / clip_len
        
        # 调试信息：打印每个窗口的距离
        # print(f"窗口 {i}, 时间 {dict_features[i]['time']:.2f}, 平均距离: {avg_distance:.2f}")
        
        if avg_distance < best_match_score:
            best_match_score = avg_distance
            best_match_start_idx = i
            
    elapsed = time.time() - start_time
    
    if best_match_score <= threshold:
        match_start_time = dict_features[best_match_start_idx]['time']
        match_end_time = dict_features[best_match_start_idx + clip_len - 1]['time']
        print(f"✅ 匹配成功! (耗时 {elapsed:.4f} 秒)")
        print(f"📌 原视频位置: {match_start_time:.3f}秒 -> {match_end_time:.3f}秒")
        print(f"📌 平均汉明距离: {best_match_score:.2f} (阈值: {threshold})")
        return match_start_time, match_end_time
    else:
        print(f"❌ 匹配失败! (耗时 {elapsed:.4f} 秒)")
        print(f"最佳尝试距离为 {best_match_score:.2f}，高于阈值 {threshold}")
        return None

if __name__ == "__main__":
    import sys
    from test_video_extract import extract_video_features
    
    if len(sys.argv) < 2:
        print("请提供要测试的短片段视频路径")
        sys.exit(1)
        
    clip_path = sys.argv[1]
    dict_path = "1.mp4_features.json"
    
    threshold = 10
    if len(sys.argv) > 2:
        threshold = int(sys.argv[2])
    
    print(f"\n>>> 阶段 1: 提取待搜索片段 '{clip_path}' 的特征...")
    # 对测试片段同样按 5fps 提取特征，并切除字幕区
    clip_features = extract_video_features(clip_path, target_fps=5, crop_ratio=0.2)
    
    print(f"\n>>> 阶段 2: 加载原剧全集字典 '{dict_path}'...")
    dict_features = load_features(dict_path)
    
    print(f"\n>>> 阶段 3: 执行滑动窗口搜索匹配... (使用阈值: {threshold})")
    search_clip_in_dict(clip_features, dict_features, threshold=threshold)
