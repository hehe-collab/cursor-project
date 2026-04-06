import os
import sys
import json
import time
import subprocess
from tqdm import tqdm

from test_scene_detect import detect_scenes_in_video
from test_video_extract import extract_video_features
from test_video_search import load_features, search_clip_in_dict

def get_input_path(prompt_text):
    """交互式获取路径，并自动处理拖拽时可能带有的引号和空格"""
    path = input(prompt_text).strip()
    if path.startswith("'") and path.endswith("'"):
        path = path[1:-1]
    if path.startswith('"') and path.endswith('"'):
        path = path[1:-1]
    return path.strip()

def build_ffmpeg_concat_script(plan, source_video, project_dir, output_txt):
    """根据坐标切分原视频，并生成 concat 清单"""
    with open(output_txt, "w", encoding="utf-8") as f:
        # 使用 tqdm 显示切片进度
        for i, segment in enumerate(tqdm(plan, desc="切片进度", unit="段", leave=False)):
            temp_clip = os.path.join(project_dir, f"temp_final_{i}.mp4")
            start = segment['start']
            duration = segment['end'] - segment['start']
            
            cmd = [
                "ffmpeg", "-y", "-hide_banner", "-loglevel", "error",
                "-ss", str(start),
                "-i", source_video,
                "-t", str(duration),
                "-c:v", "libx264", "-c:a", "aac",
                "-copyts", # 保持原始时间戳
                temp_clip
            ]
            subprocess.run(cmd, check=True)
            # concat 脚本中使用的路径，建议使用正斜杠或相对路径
            # 这里为避免路径中有特殊字符，仅使用文件名
            f.write(f"file 'temp_final_{i}.mp4'\n")

def merge_videos(concat_txt, output_video, project_dir):
    """无损合并"""
    cmd = [
        "ffmpeg", "-y", "-hide_banner", "-loglevel", "error",
        "-f", "concat", "-safe", "0",
        "-i", os.path.basename(concat_txt),
        "-c", "copy",
        output_video
    ]
    subprocess.run(cmd, check=True, cwd=project_dir)

def cleanup_temps(plan_len, project_dir, concat_txt):
    for i in range(plan_len):
        temp_clip = os.path.join(project_dir, f"temp_final_{i}.mp4")
        if os.path.exists(temp_clip):
            os.remove(temp_clip)
    if os.path.exists(concat_txt):
        os.remove(concat_txt)

def main():
    print("\n" + "="*50)
    print("       🎬 素材像素级复刻工具 (Auto Video Copier)  ")
    print("="*50 + "\n")
    
    # 1. 交互式获取路径
    full_movie_path = get_input_path("👉 请输入（或直接拖拽）[印尼语全集源视频] 路径: ")
    if not os.path.exists(full_movie_path):
        print(f"❌ 找不到文件: {full_movie_path}")
        return
        
    ad_video_path = get_input_path("👉 请输入（或直接拖拽）[中文混剪素材] 路径: ")
    if not os.path.exists(ad_video_path):
        print(f"❌ 找不到文件: {ad_video_path}")
        return

    # 路径规范化
    full_movie_path = os.path.abspath(full_movie_path)
    ad_video_path = os.path.abspath(ad_video_path)
    
    # 获取各个目录的绝对路径
    src_dir = os.path.dirname(os.path.abspath(__file__))
    project_dir = os.path.dirname(src_dir)
    data_dir = os.path.join(project_dir, "data")
    output_dir = os.path.join(project_dir, "output")
    
    os.makedirs(data_dir, exist_ok=True)
    os.makedirs(output_dir, exist_ok=True)

    # 2. 全集建库缓存逻辑
    movie_filename = os.path.basename(full_movie_path)
    dict_json = os.path.join(data_dir, f"{movie_filename}_features.json")
    
    if os.path.exists(dict_json):
        print(f"\n[1/4] 📦 检测到全集特征字典已存在，直接加载...")
        dict_features = load_features(dict_json)
    else:
        print(f"\n[1/4] ⏳ 全集特征字典不存在，开始首次建库 (这可能需要几分钟)...")
        dict_features = extract_video_features(full_movie_path, target_fps=5, crop_ratio=0.2)
        if not dict_features:
            print("❌ 建库失败")
            return
        with open(dict_json, "w", encoding="utf-8") as f:
            json.dump(dict_features, f, indent=2)
        print(f"✅ 建库完成！已缓存至 data 目录。")

    # 3. 场景打碎
    print(f"\n[2/4] 🔍 正在分析并打碎中文混剪素材...")
    shots = detect_scenes_in_video(ad_video_path)
    if not shots:
        print("❌ 未能分析出任何有效镜头")
        return
    
    # 4. 逐个 Shot 检索坐标
    print(f"\n[3/4] 🎯 正在逐个 Shot 检索印尼语原始坐标...")
    rebuild_plan = []
    dropped_shots = []
    
    # 进度条包裹
    for shot in tqdm(shots, desc="匹配进度", unit="shot"):
        shot_id = shot['shot_id']
        temp_shot_file = os.path.join(project_dir, f"temp_shot_{shot_id}.mp4")
        
        # 截取 Shot
        cmd = [
            "ffmpeg", "-y", "-hide_banner", "-loglevel", "error",
            "-ss", str(shot['start']),
            "-i", ad_video_path,
            "-t", str(shot['duration']),
            "-c:v", "libx264", "-c:a", "aac",
            temp_shot_file
        ]
        subprocess.run(cmd, check=True)
        
        # 提取 Shot 特征 (安静模式处理，避免刷屏)
        shot_features = extract_video_features(temp_shot_file, target_fps=5, crop_ratio=0.2, quiet=True)
        
        match_start = None
        if shot_features and len(shot_features) > 0:
            # 策略 1: 正常提取 + 正常阈值
            match_start, match_end, best_score = search_clip_in_dict(shot_features, dict_features, threshold=10, quiet=True)
            
            # 策略 2: 如果失败，去掉头尾0.4秒 (约2帧)，并用正常阈值 (对抗转场特效)
            if match_start is None and len(shot_features) > 4:
                trimmed_features = shot_features[2:-2]
                match_start, match_end, best_score = search_clip_in_dict(trimmed_features, dict_features, threshold=10, quiet=True)
                
            # 策略 3: 如果还是失败，放宽阈值到 15 (对抗滤镜和花字)
            if match_start is None:
                match_start, match_end, best_score = search_clip_in_dict(shot_features, dict_features, threshold=15, quiet=True)
                
            # 策略 4: 如果还是失败，去掉头尾 + 放宽阈值 (大招)
            if match_start is None and len(shot_features) > 4:
                trimmed_features = shot_features[2:-2]
                match_start, match_end, best_score = search_clip_in_dict(trimmed_features, dict_features, threshold=15, quiet=True)
                
            if match_start is not None:
                rebuild_plan.append({
                    "shot_id": shot_id,
                    "start": match_start,
                    "end": match_end
                })
            else:
                dropped_shots.append({
                    "shot_id": shot_id,
                    "file": temp_shot_file,
                    "score": best_score
                })
        else:
            dropped_shots.append({
                "shot_id": shot_id,
                "file": temp_shot_file,
                "score": "N/A (No features)"
            })
            
        if match_start is not None and os.path.exists(temp_shot_file):
            # 只有匹配成功的才立即删除临时文件，未匹配的保留用于生成 dropped 视频
            os.remove(temp_shot_file)

    if dropped_shots:
        print(f"\n⚠️ 发现 {len(dropped_shots)} 个镜头被丢弃！正在生成丢弃镜头合集排错视频...")
        drop_concat_txt = os.path.join(project_dir, "drop_concat.txt")
        with open(drop_concat_txt, "w", encoding="utf-8") as f:
            for drop in dropped_shots:
                f.write(f"file '{os.path.basename(drop['file'])}'\n")
                print(f"   - Shot {drop['shot_id']:02d} 被丢弃，最佳匹配距离为: {drop['score']}")
                
        drop_output = os.path.join(output_dir, "drop_shots.mp4")
        cmd = [
            "ffmpeg", "-y", "-hide_banner", "-loglevel", "error",
            "-f", "concat", "-safe", "0",
            "-i", os.path.basename(drop_concat_txt),
            "-c", "copy",
            drop_output
        ]
        subprocess.run(cmd, check=True, cwd=project_dir)
        
        # 清理丢弃的临时文件
        for drop in dropped_shots:
            if os.path.exists(drop['file']):
                os.remove(drop['file'])
        if os.path.exists(drop_concat_txt):
            os.remove(drop_concat_txt)
        print(f"👉 被丢弃的镜头已合成为: {drop_output}，请查看此视频排查原因。")

    if not rebuild_plan:
        print("\n❌ 匹配失败！所有镜头均未能找到对应的原片画面 (可能是非本剧素材或画质改动过大)。")
        return

    print("\n📋 成功匹配的剪辑坐标清单:")
    for p in rebuild_plan:
        print(f"   ✓ Shot {p['shot_id']:02d} -> 原剧 {p['start']:.3f}秒 至 {p['end']:.3f}秒")

    # 5. 生成最终视频
    print("\n[4/4] 🎬 正在根据坐标生成最终的印尼语版视频...")
    concat_txt = os.path.join(project_dir, "rebuild_concat.txt")
    
    ad_filename = os.path.basename(ad_video_path)
    output_name = os.path.splitext(ad_filename)[0] + "_indo_version.mp4"
    final_output = os.path.join(output_dir, output_name)
    
    build_ffmpeg_concat_script(rebuild_plan, full_movie_path, project_dir, concat_txt)
    merge_videos(concat_txt, final_output, project_dir)
    cleanup_temps(len(rebuild_plan), project_dir, concat_txt)
    
    print("\n" + "="*50)
    print(f"🎉 恭喜！端到端复刻完毕！")
    print(f"📂 最终视频已保存至: {final_output}")
    print("="*50 + "\n")

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n\n⚠️ 用户强制中断了程序。")
        sys.exit(0)
