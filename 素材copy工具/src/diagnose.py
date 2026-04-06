import os
import sys
import time
import subprocess
from test_scene_detect import detect_scenes_in_video
from test_video_extract import extract_video_features
from test_video_search import load_features, search_clip_in_dict

def run_diagnosis(ad_video, dict_json):
    print(f"🎬 [诊断模式] 开始分析...")
    print(f"📁 字典文件: {dict_json}")
    print(f"📁 广告素材: {ad_video}\n")
    
    dict_features = load_features(dict_json)
    
    print("🔍 正在进行场景切分...")
    shots = detect_scenes_in_video(ad_video)
    if not shots:
        print("❌ 场景切分失败！")
        return
        
    print(f"\n✅ 切分完成，共 {len(shots)} 个镜头。开始排查被丢弃的镜头 (最多分析10个)...\n")
    
    fail_count = 0
    
    for shot in shots:
        shot_id = shot['shot_id']
        temp_shot_file = f"temp_diag_shot_{shot_id}.mp4"
        
        # 截取 Shot
        cmd = [
            "ffmpeg", "-y", "-hide_banner", "-loglevel", "error",
            "-ss", str(shot['start']),
            "-i", ad_video,
            "-t", str(shot['duration']),
            "-c:v", "libx264", "-c:a", "aac",
            temp_shot_file
        ]
        subprocess.run(cmd, check=True)
        
        # 默认标准抽取 (裁剪底部 20%, 阈值10)
        features_default = extract_video_features(temp_shot_file, target_fps=5, crop_ratio=0.2, quiet=True)
        if not features_default or len(features_default) == 0:
            if os.path.exists(temp_shot_file): os.remove(temp_shot_file)
            continue
            
        start, end, score = search_clip_in_dict(features_default, dict_features, threshold=10, quiet=True)
        
        # 如果默认匹配失败，开始多维度诊断
        if start is None:
            fail_count += 1
            print(f"==================================================")
            print(f"🚨 发现抛弃镜头: Shot {shot_id} (时长: {shot['duration']:.2f}秒, 默认最佳距离: {score:.2f})")
            
            match_found_in_diag = False
            
            # --- 诊断 A: 放宽阈值 (滤镜/轻度形变) ---
            start_A, _, score_A = search_clip_in_dict(features_default, dict_features, threshold=15, quiet=True)
            if start_A is not None:
                print(f"  👉 [测试 A - 放宽阈值到15]: ✅ 成功匹配！(距离 {score_A:.2f})")
                print("     💡 结论: 该镜头可能加了【轻度滤镜、调色】或发生了【轻微画面缩放】。")
                match_found_in_diag = True
            else:
                print(f"  👉 [测试 A - 放宽阈值到15]: ❌ 依然失败 (最佳距离 {score_A:.2f})")

            # --- 诊断 B: 扩大裁剪区域到 40% (花字遮挡) ---
            features_B = extract_video_features(temp_shot_file, target_fps=5, crop_ratio=0.4, quiet=True)
            if features_B:
                start_B, _, score_B = search_clip_in_dict(features_B, dict_features, threshold=10, quiet=True)
                if start_B is not None:
                    print(f"  👉 [测试 B - 裁剪底部40%防花字]: ✅ 成功匹配！(距离 {score_B:.2f})")
                    print("     💡 结论: 该镜头中下部极大概率有【大字号花字遮挡】。")
                    match_found_in_diag = True
                else:
                    print(f"  👉 [测试 B - 裁剪底部40%防花字]: ❌ 依然失败 (最佳距离 {score_B:.2f})")

            # --- 诊断 C: 去头去尾 (转场特效污染) ---
            if len(features_default) > 4:
                features_C = features_default[2:-2] # 去掉头尾各2帧 (约0.4秒)
                start_C, _, score_C = search_clip_in_dict(features_C, dict_features, threshold=10, quiet=True)
                if start_C is not None:
                    print(f"  👉 [测试 C - 去除头尾0.4秒]: ✅ 成功匹配！(距离 {score_C:.2f})")
                    print("     💡 结论: 该镜头中间是纯净的，但头尾受到了【黑场/叠化等转场特效污染】。")
                    match_found_in_diag = True
                else:
                    print(f"  👉 [测试 C - 去除头尾0.4秒]: ❌ 依然失败 (最佳距离 {score_C:.2f})")
            else:
                print(f"  👉 [测试 C - 去除头尾0.4秒]: ⚠️ 镜头太短，无法进行去头尾测试。")
                
            if not match_found_in_diag:
                print("  💀 综合诊断: 以上三种常见原因均被排除。")
                print("     可能原因: 【严重加速/减速(需DTW)】 或 【完全非本剧素材(如片尾CTA)】 或 【画面被镜像翻转】。")
                
            print(f"==================================================\n")
            
        if os.path.exists(temp_shot_file):
            os.remove(temp_shot_file)
            
        if fail_count >= 10:
            print("🛑 已分析 10 个被抛弃的镜头，为了节省时间，诊断提前结束。规律应该已足够明显。")
            break

if __name__ == "__main__":
    ad_video = "/Volumes/存钱罐/素材汇总/0313托孤/ADXray 国内行业版.mp4"
    dict_json = "data/托孤-IDN.mp4_features.json"
    
    if os.path.exists(ad_video) and os.path.exists(dict_json):
        run_diagnosis(ad_video, dict_json)
    else:
        print("❌ 文件路径不正确，请检查！")
