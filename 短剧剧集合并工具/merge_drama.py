import os
import re
import sys
import subprocess
import shutil
import time

def check_dependencies():
    """检查是否安装了 ffmpeg 和 ffprobe"""
    if shutil.which("ffmpeg") is None or shutil.which("ffprobe") is None:
        print("错误: 未找到 ffmpeg 或 ffprobe！请确保已经安装并配置了环境变量。")
        print("macOS 可以使用命令安装: brew install ffmpeg")
        sys.exit(1)

def natural_sort_key(s):
    """自然排序算法，用于正确排序 1.mp4, 2.mp4, 10.mp4"""
    return [int(text) if text.isdigit() else text.lower() for text in re.split(r'(\d+)', s)]

def get_video_files(directory):
    """获取 input 目录下所有的 mp4 文件并自然排序"""
    if not os.path.exists(directory):
        os.makedirs(directory)
        return []
    files = [f for f in os.listdir(directory) if f.lower().endswith('.mp4')]
    return sorted(files, key=natural_sort_key)

def get_video_duration(filepath):
    """使用 ffprobe 获取视频总时长（秒）"""
    cmd = [
        "ffprobe", 
        "-v", "error", 
        "-show_entries", "format=duration", 
        "-of", "default=noprint_wrappers=1:nokey=1", 
        filepath
    ]
    try:
        output = subprocess.check_output(cmd).decode("utf-8").strip()
        return float(output)
    except Exception as e:
        print(f"获取视频时长失败: {filepath} ({e})")
        return 0.0

def trim_video(input_file, output_file, end_time):
    """使用 ffmpeg 截取视频从 0 到 end_time 的部分 (流拷贝，极速无损)"""
    cmd = [
        "ffmpeg",
        "-y",               # 覆盖已存在文件
        "-i", input_file,
        "-t", str(end_time),
        "-c", "copy",       # 核心：流拷贝，不重新编码
        output_file
    ]
    # 不输出长篇日志，只保留错误
    subprocess.run(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

def concat_videos(file_list, output_file):
    """使用 ffmpeg 的 concat demuxer 无损拼接视频"""
    # 1. 生成 concat.txt
    concat_txt_path = "concat_list.txt"
    with open(concat_txt_path, "w", encoding="utf-8") as f:
        for file in file_list:
            # 兼容路径中有空格等情况，写入格式：file 'filename.mp4'
            f.write(f"file '{file}'\n")
    
    # 2. 执行合并
    cmd = [
        "ffmpeg",
        "-y",
        "-f", "concat",
        "-safe", "0",
        "-i", concat_txt_path,
        "-c", "copy",
        output_file
    ]
    subprocess.run(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    
    # 3. 清理 txt
    if os.path.exists(concat_txt_path):
        os.remove(concat_txt_path)

def main():
    print("="*50)
    print("🎬 短剧剧集合并工具 (极速无损版)")
    print("="*50)
    
    check_dependencies()
    
    # 获取用户输入的目录路径
    input_dir_str = input("👉 请输入（或直接拖入）存放短剧的文件夹路径: ").strip()
    
    # 清理路径中可能自带的引号（终端拖拽时经常会带上）
    if (input_dir_str.startswith("'") and input_dir_str.endswith("'")) or \
       (input_dir_str.startswith('"') and input_dir_str.endswith('"')):
        input_dir_str = input_dir_str[1:-1]
    
    # 处理转义空格 (macOS terminal 特性)
    input_dir_str = input_dir_str.replace("\\ ", " ")
    
    input_dir = os.path.abspath(input_dir_str)
    
    if not os.path.isdir(input_dir):
        print(f"❌ 错误: 找不到目录 '{input_dir}'，请检查路径是否正确。")
        return

    # 提取文件夹名称，作为最终导出的文件名
    folder_name = os.path.basename(input_dir)
    if not folder_name:  # 防止拖入的是根目录等极端情况
        folder_name = "最终合并完整版"
    output_filename = f"{folder_name}.mp4"

    # 统一输出到本工具所在目录的 output 文件夹下
    current_script_dir = os.path.dirname(os.path.abspath(__file__))
    output_dir = os.path.join(current_script_dir, "output")
    
    # 确保 output 文件夹存在
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)
        
    video_files = get_video_files(input_dir)
    
    if not video_files:
        print(f"❌ '{folder_name}' 文件夹中没有找到任何 .mp4 视频文件！")
        return
        
    print(f"✅ 成功在 [{folder_name}] 中找到 {len(video_files)} 集视频。")
    print(f"首集: {video_files[0]}")
    print(f"尾集: {video_files[-1]}\n")
    
    # 获取用户输入
    trim_seconds_str = input("👉 请输入每集【片尾】需要切除的秒数 (例如输入 8 表示切除最后8秒，0表示不切): ")
    try:
        trim_seconds = float(trim_seconds_str)
    except ValueError:
        print("❌ 输入的秒数无效，必须是数字！程序退出。")
        return
        
    skip_last_str = "y"
    if trim_seconds > 0:
        skip_last_str = input("👉 最后一集(大结局)是否需要【保留原片尾不切除】？(Y/n 默认Y): ").strip().lower()
        if not skip_last_str:
            skip_last_str = "y"
    
    # 移除了手动输入合并文件名的步骤，直接显示系统决定生成的文件名
    print(f"\n✨ 智能命名: 合并后的文件将保存为 -> {output_filename}")
        
    print("\n" + "="*50)
    print("🚀 开始处理，请稍候...")
    
    start_time = time.time()
    
    # 创建临时文件夹存放裁剪后的中间文件
    temp_dir = "temp_trimmed_videos"
    if not os.path.exists(temp_dir):
        os.makedirs(temp_dir)
        
    trimmed_files_paths = []
    
    # 逐集处理
    for i, video_name in enumerate(video_files):
        video_path = os.path.join(input_dir, video_name)
        is_last_episode = (i == len(video_files) - 1)
        
        duration = get_video_duration(video_path)
        if duration <= 0:
            print(f"⚠️ 警告: 无法获取 {video_name} 的时长，跳过此文件。")
            continue
            
        print(f"正在处理 [{i+1}/{len(video_files)}]: {video_name} (原长: {duration:.2f}s) ... ", end="", flush=True)
        
        # 决定该集是否需要裁剪
        current_trim_seconds = trim_seconds
        if is_last_episode and skip_last_str == 'y':
            current_trim_seconds = 0
            
        temp_out_path = os.path.join(temp_dir, f"trimmed_{i:04d}.mp4")
        
        if current_trim_seconds > 0:
            cut_point = duration - current_trim_seconds
            if cut_point <= 0:
                print(f"❌ 错误: 视频太短，不足以切除 {current_trim_seconds} 秒！跳过。")
                continue
            trim_video(video_path, temp_out_path, cut_point)
            print(f"已切除尾部 {current_trim_seconds} 秒 (新长: {cut_point:.2f}s)")
        else:
            # 如果不需要裁剪（比如输入为0，或者是选择不切的最后一集），直接拷贝过去
            shutil.copy2(video_path, temp_out_path)
            print("保留原长度")
            
        # 记录真实路径用于最后的合并
        trimmed_files_paths.append(temp_out_path)
        
    print("\n⏳ 所有分集处理完毕，正在进行无损合并拼接...")
    final_output_path = os.path.join(output_dir, output_filename)
    concat_videos(trimmed_files_paths, final_output_path)
    
    print("🧹 清理临时缓存文件...")
    shutil.rmtree(temp_dir, ignore_errors=True)
    
    end_time = time.time()
    print("="*50)
    print(f"🎉 恭喜！处理完成。")
    print(f"📂 最终输出文件: {final_output_path}")
    print(f"⏱️ 总耗时: {end_time - start_time:.2f} 秒")
    print("="*50)

if __name__ == "__main__":
    # 为了避免把最终输出文件当成下一次的原片，如果存在先不管，但在 get_video_files 时它可能会被包含进去
    # 如果用户在该目录二次运行，需要注意。我们可以在 get_video_files 里做一个小过滤，或者提醒用户。
    try:
        main()
    except KeyboardInterrupt:
        print("\n\n⏹️ 用户手动终止了程序。")
        shutil.rmtree("temp_trimmed_videos", ignore_errors=True)
        sys.exit(0)