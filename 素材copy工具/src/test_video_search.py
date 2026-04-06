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

def search_clip_in_dict(clip_features, dict_features, threshold=10, quiet=False):
    """
    在长视频字典中，搜索短片段的位置 (类似滑动窗口匹配)
    针对“妥协拉长方案”，改为基于起止点的弹性匹配，寻找真实的区间。
    """
    if not clip_features or len(clip_features) == 0:
        return None, None, float('inf')
        
    if not quiet:
        print(f"\n开始搜索... 短片段帧数: {len(clip_features)}, 字典帧数: {len(dict_features)}")
    start_time = time.time()
    
    dict_hashes = [hex_to_bin(item['hash']) for item in dict_features]
    clip_hashes = [hex_to_bin(item['hash']) for item in clip_features]
    
    # 定义用于寻找端点的样本长度（帧数），比如3帧（0.6秒）
    sample_len = min(3, len(clip_hashes))
    
    def find_best_match(target_hashes, search_hashes):
        """辅助函数：在搜索序列中找到目标序列的最佳匹配位置和分数"""
        t_len = len(target_hashes)
        s_len = len(search_hashes)
        if t_len == 0 or s_len < t_len:
            return -1, float('inf')
            
        best_idx = -1
        best_score = float('inf')
        
        for i in range(s_len - t_len + 1):
            total_dist = sum(hamming_distance(target_hashes[j], search_hashes[i+j]) for j in range(t_len))
            avg_dist = total_dist / t_len
            if avg_dist < best_score:
                best_score = avg_dist
                best_idx = i
                if best_score == 0: # 完美匹配直接返回
                    break
        return best_idx, best_score

    # 1. 寻找起点
    start_target = clip_hashes[:sample_len]
    start_idx, start_score = find_best_match(start_target, dict_hashes)
    
    # 2. 寻找终点
    end_target = clip_hashes[-sample_len:]
    # 为了避免终点跑到起点前面去，我们限制终点的搜索范围（在起点之后）
    # 稍微给个容错区间，比如起点前 10 帧到现在都可以搜，防止轻微重叠
    search_start_for_end = max(0, start_idx - 10) if start_idx != -1 else 0
    end_search_space = dict_hashes[search_start_for_end:]
    
    rel_end_idx, end_score = find_best_match(end_target, end_search_space)
    
    # 组合平均分作为这次匹配的综合评分
    best_match_score = (start_score + end_score) / 2
    
    if start_idx != -1 and rel_end_idx != -1:
        # 计算真实的在全集字典中的 index
        abs_end_idx = search_start_for_end + rel_end_idx
        # 终点片段包含 sample_len 帧，所以实际结束时间应该取这 sample_len 帧的最后一帧
        actual_end_idx = abs_end_idx + sample_len - 1
        
        # 确保终点在起点之后（至少隔开一定距离或相等）
        if actual_end_idx >= start_idx:
            elapsed = time.time() - start_time
            if best_match_score <= threshold:
                match_start_time = dict_features[start_idx]['time']
                match_end_time = dict_features[actual_end_idx]['time']
                
                # 安全兜底：如果算出来的区间过长（比如超过了广告原长 * 3），说明匹配错乱了，强制抛弃
                ad_duration = clip_features[-1]['time'] - clip_features[0]['time']
                matched_duration = match_end_time - match_start_time
                if matched_duration > ad_duration * 3 and matched_duration > 10:
                    if not quiet:
                        print(f"❌ 匹配失败: 找到的区间过长异常 ({matched_duration:.1f}秒 > 原长 {ad_duration:.1f}秒)")
                    return None, None, best_match_score
                    
                if not quiet:
                    print(f"✅ 弹性匹配成功! (耗时 {elapsed:.4f} 秒)")
                    print(f"📌 原视频位置: {match_start_time:.3f}秒 -> {match_end_time:.3f}秒")
                    print(f"📌 端点平均距离: {best_match_score:.2f} (阈值: {threshold})")
                return match_start_time, match_end_time, best_match_score
    
    # 失败情况
    if not quiet:
        elapsed = time.time() - start_time
        print(f"❌ 匹配失败! (耗时 {elapsed:.4f} 秒)")
        print(f"最佳尝试距离为 {best_match_score:.2f}，高于阈值 {threshold}")
    return None, None, best_match_score

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
