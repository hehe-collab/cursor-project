#!/usr/bin/env python3
"""
检查剧集 3、4 及相邻集是否有异常
用法: python check_episodes.py <视频文件夹路径>
示例: python check_episodes.py /path/to/your/videos
"""
import os
import sys
import subprocess
import glob
import re

def natural_sort_key(s):
    return [int(text) if text.isdigit() else text.lower() for text in re.split(r'(\d+)', s)]

def get_video_files(folder):
    files = []
    for ext in ['*.mp4', '*.mov', '*.mkv', '*.avi']:
        files.extend(glob.glob(os.path.join(folder, ext)))
    files.sort(key=lambda x: natural_sort_key(os.path.basename(x)))
    return files

def run_ffprobe(path):
    """返回 ffprobe 的完整输出"""
    ffprobe = "ffprobe" if os.name != 'nt' else "ffprobe.exe"
    try:
        result = subprocess.run([
            ffprobe, '-v', 'error', '-show_format', '-show_streams',
            '-print_format', 'default', path
        ], capture_output=True, text=True, timeout=30)
        return result.stdout + result.stderr, result.returncode
    except Exception as e:
        return str(e), -1

def run_ffprobe_check(path):
    """检查是否有异常帧、时间戳问题"""
    ffprobe = "ffprobe" if os.name != 'nt' else "ffprobe.exe"
    try:
        result = subprocess.run([
            ffprobe, '-v', 'warning', '-show_frames', '-select_streams', 'v:0',
            '-show_entries', 'frame=key_frame,pkt_pts_time,pkt_dts_time,pkt_duration_time',
            '-of', 'csv=p=0', path
        ], capture_output=True, text=True, timeout=60)
        return result.stdout, result.stderr, result.returncode
    except Exception as e:
        return "", str(e), -1

def parse_stream_info(output):
    """从 ffprobe 输出解析关键信息"""
    info = {}
    current = None
    for line in output.split('\n'):
        if line.startswith('[STREAM]'):
            current = {}
        elif line.startswith('[/STREAM]') and current:
            if current.get('codec_type') == 'video':
                info.setdefault('video', {}).update(current)
            elif current.get('codec_type') == 'audio':
                info.setdefault('audio', {}).update(current)
            current = None
        elif '=' in line and current is not None:
            k, v = line.split('=', 1)
            current[k] = v
        elif line.startswith('[') and 'FORMAT' in line:
            pass
        elif '=' in line:
            k, v = line.split('=', 1)
            info[k] = v
    return info

def analyze_frames(stderr):
    """分析帧数据中的异常"""
    issues = []
    if 'Non-monotonous DTS' in stderr:
        issues.append("⚠️ 存在 Non-monotonous DTS（时间戳不单调）")
    if 'error' in stderr.lower():
        issues.append(f"⚠️ FFprobe 报错: {stderr[:200]}")
    return issues

def main():
    if len(sys.argv) < 2:
        print("用法: python check_episodes.py <视频文件夹路径>")
        print("示例: python check_episodes.py /Users/xxx/Desktop/剧集文件夹")
        sys.exit(1)
    
    folder = sys.argv[1]
    if not os.path.isdir(folder):
        print(f"错误: 文件夹不存在: {folder}")
        sys.exit(1)
    
    files = get_video_files(folder)
    if not files:
        print(f"错误: 在 {folder} 中未找到视频文件")
        sys.exit(1)
    
    # 检查第 1、2、3、4、5 集（索引 0-4）
    indices = [0, 1, 2, 3, 4]  # 第 1-5 集
    indices = [i for i in indices if i < len(files)]
    
    print("=" * 70)
    print("剧集媒体信息检查（重点关注第 3、4 集）")
    print("=" * 70)
    
    all_infos = []
    for i in indices:
        path = files[i]
        name = os.path.basename(path)
        print(f"\n【第 {i+1} 集】{name}")
        print("-" * 50)
        
        output, ret = run_ffprobe(path)
        if ret != 0:
            print(f"  ❌ ffprobe 失败 (returncode={ret})")
            print(f"  {output[:500]}")
            continue
        
        info = parse_stream_info(output)
        v = info.get('video', {})
        a = info.get('audio', {})
        fmt = {k: v for k, v in info.items() if k not in ('video', 'audio') and isinstance(v, str)}
        
        v_codec = v.get('codec_name', '?')
        v_width = v.get('width', '?')
        v_height = v.get('height', '?')
        v_fps = v.get('r_frame_rate', '?')
        v_duration = v.get('duration') or fmt.get('duration', '?')
        v_nb_frames = v.get('nb_frames', '?')
        v_bitrate = v.get('bit_rate', '?')
        
        a_codec = a.get('codec_name', '?')
        a_sample_rate = a.get('sample_rate', '?')
        a_channels = a.get('channels', '?')
        a_duration = a.get('duration') or fmt.get('duration', '?')
        
        print(f"  视频: {v_codec} | {v_width}x{v_height} | {v_fps} fps | 时长 {v_duration}s | 帧数 {v_nb_frames}")
        print(f"  音频: {a_codec} | {a_sample_rate} Hz | {a_channels} 声道 | 时长 {a_duration}s")
        
        # 检查帧级时间戳
        frames_out, frames_err, _ = run_ffprobe_check(path)
        frame_issues = analyze_frames(frames_err)
        if frame_issues:
            for iss in frame_issues:
                print(f"  {iss}")
        
        # 取前几帧的 PTS 看是否从 0 开始
        if frames_out:
            lines = [l for l in frames_out.strip().split('\n') if l][:5]
            if lines:
                print(f"  前5帧 (key_frame,pts,dts,duration): {lines[0][:80]}...")
        
        all_infos.append({
            'name': name,
            'idx': i + 1,
            'v_codec': v_codec,
            'v_fps': v_fps,
            'v_duration': v_duration,
            'a_codec': a_codec,
            'a_sample_rate': a_sample_rate,
        })
    
    # 对比分析
    print("\n" + "=" * 70)
    print("对比分析（检查 3、4 集是否与 1、2、5 集不一致）")
    print("=" * 70)
    
    if len(all_infos) >= 3:
        ref = all_infos[0]
        for inf in all_infos[1:]:
            diffs = []
            if inf['v_codec'] != ref['v_codec']:
                diffs.append(f"视频编码不同: {ref['v_codec']} vs {inf['v_codec']}")
            if inf['v_fps'] != ref['v_fps']:
                diffs.append(f"帧率不同: {ref['v_fps']} vs {inf['v_fps']}")
            if inf['a_codec'] != ref['a_codec']:
                diffs.append(f"音频编码不同: {ref['a_codec']} vs {inf['a_codec']}")
            if inf['a_sample_rate'] != ref['a_sample_rate']:
                diffs.append(f"采样率不同: {ref['a_sample_rate']} vs {inf['a_sample_rate']}")
            if diffs:
                print(f"\n  ⚠️ 第 {inf['idx']} 集 ({inf['name']}) 与第 1 集不一致:")
                for d in diffs:
                    print(f"     - {d}")
            else:
                print(f"\n  ✓ 第 {inf['idx']} 集 与第 1 集参数一致")
    
    print("\n" + "=" * 70)
    print("检查完成。若第 3、4 集有参数不一致或时间戳异常，可能是合并问题的根源。")
    print("=" * 70)

if __name__ == '__main__':
    main()
