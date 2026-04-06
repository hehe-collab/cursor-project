import os
import json
import time
import subprocess
from test_scene_detect import detect_scenes_in_video
from test_video_extract import extract_video_features
from test_video_search import load_features, search_clip_in_dict

def extract_shot_video(video_path, start_time, duration, output_path):
    """用 FFmpeg 将视频的某个区间截取出来，保存为临时文件"""
    # -y: 覆盖, -ss: 开始时间, -t: 时长, -c copy: 无损流拷贝（非常快，但可能不精确到帧），这里用重新编码确保帧精确
    cmd = [
        "ffmpeg", "-y", "-hide_banner", "-loglevel", "error",
        "-ss", str(start_time),
        "-i", video_path,
        "-t", str(duration),
        "-c:v", "libx264", "-c:a", "aac",
        output_path
    ]
    subprocess.run(cmd, check=True)

def build_ffmpeg_concat_script(plan, output_txt="concat.txt"):
    """
    plan: list of dict, [{'start': 10.5, 'end': 12.0}, ...]
    根据计划，生成 ffmpeg concat 所需的临时小片段，并写入 concat.txt
    """
    # 真实应用中，我们会直接使用 ffmpeg filter_complex，但为了 MVP 简单直观，
    # 我们先从原剧 1.mp4 中把这几个小片段切出来，再用 concat demuxer 拼起来。
    
    with open(output_txt, "w", encoding="utf-8") as f:
        for i, segment in enumerate(plan):
            temp_clip = f"temp_final_{i}.mp4"
            start = segment['start']
            duration = segment['end'] - segment['start']
            print(f"  > 正在从原剧中切割片段 {i}: {start:.3f}秒, 长度 {duration:.3f}秒")
            
            cmd = [
                "ffmpeg", "-y", "-hide_banner", "-loglevel", "error",
                "-ss", str(start),
                "-i", "1.mp4",  # 假设我们从 1.mp4 这个原剧中切
                "-t", str(duration),
                "-c:v", "libx264", "-c:a", "aac",
                temp_clip
            ]
            subprocess.run(cmd)
            f.write(f"file '{temp_clip}'\n")

def merge_videos(concat_txt, output_video):
    """使用 concat demuxer 将小片段无损合并为最终视频"""
    cmd = [
        "ffmpeg", "-y", "-hide_banner", "-loglevel", "error",
        "-f", "concat", "-safe", "0",
        "-i", concat_txt,
        "-c", "copy",
        output_video
    ]
    subprocess.run(cmd)
    print(f"✅ 最终视频已生成: {output_video}")

def cleanup_temps(plan, concat_txt):
    for i in range(len(plan)):
        temp_clip = f"temp_final_{i}.mp4"
        if os.path.exists(temp_clip):
            os.remove(temp_clip)
    if os.path.exists(concat_txt):
        os.remove(concat_txt)

def end_to_end_mvp(ad_video, dict_json):
    print("=========================================")
    print(" 🚀 开始执行 MVP 端到端复刻流程")
    print("=========================================")
    
    # 1. 场景打碎
    print(f"\n[1/4] 正在分析并打碎混剪素材: {ad_video}")
    shots = detect_scenes_in_video(ad_video)
    
    # 2. 加载字典
    print(f"\n[2/4] 正在加载印尼语原剧特征字典: {dict_json}")
    dict_features = load_features(dict_json)
    
    # 3. 逐个 Shot 提取特征并去字典里找坐标
    print("\n[3/4] 正在逐个 Shot 检索原始坐标...")
    rebuild_plan = []
    
    for shot in shots:
        shot_id = shot['shot_id']
        temp_shot_file = f"temp_shot_{shot_id}.mp4"
        
        # 把这个 Shot 从广告素材里切出来
        extract_shot_video(ad_video, shot['start'], shot['duration'], temp_shot_file)
        
        # 给这个 Shot 提特征
        shot_features = extract_video_features(temp_shot_file, target_fps=5, crop_ratio=0.2)
        
        # 去全集字典里搜
        if shot_features and len(shot_features) > 0:
            match_result = search_clip_in_dict(shot_features, dict_features, threshold=10)
            if match_result:
                # match_result 包含了 (match_start_time, match_end_time)
                rebuild_plan.append({
                    "shot_id": shot_id,
                    "start": match_result[0],
                    "end": match_result[1]
                })
            else:
                print(f"⚠️ Shot {shot_id} 未找到匹配画面，可能为 CTA 或外来素材，已丢弃(Drop)。")
        
        # 删掉临时的 Shot 文件
        if os.path.exists(temp_shot_file):
            os.remove(temp_shot_file)
            
    print("\n=========================================")
    print(" 📋 最终生成的剪辑坐标清单 (Rebuild Plan):")
    for p in rebuild_plan:
        print(f"   Shot {p['shot_id']} -> 原剧 {p['start']:.3f}秒 至 {p['end']:.3f}秒")
    print("=========================================")
    
    # 4. 根据坐标从原剧中切片并重组
    print("\n[4/4] 正在根据坐标生成最终的印尼语视频...")
    concat_txt = "rebuild_concat.txt"
    final_output = "final_indo_version.mp4"
    
    build_ffmpeg_concat_script(rebuild_plan, concat_txt)
    merge_videos(concat_txt, final_output)
    
    cleanup_temps(rebuild_plan, concat_txt)
    print("\n🎉 端到端流程执行完毕！")

if __name__ == "__main__":
    end_to_end_mvp("test_ad.mp4", "1.mp4_features.json")
