"""
全剧分析模块
输入：剧集目录
输出：起量片段、次高能片段、候选钩子
"""

import json
import os
from pathlib import Path
from typing import Any, List, Optional, Tuple, Union

from scene_detect import detect_scenes, get_scene_midpoint
from frame_extract import extract_frame, frame_to_base64


# 尝试导入 openai，未配置时给出提示
try:
    from openai import OpenAI
except ImportError:
    OpenAI = None


def load_config() -> dict:
    """加载配置"""
    config_path = Path(__file__).parent.parent / "config.yaml"
    if not config_path.exists():
        config_path = Path(__file__).parent.parent / "config.example.yaml"
    if config_path.exists():
        import yaml
        with open(config_path, encoding="utf-8") as f:
            return yaml.safe_load(f) or {}
    return {}


def list_episodes(drama_dir: Union[str, Path]) -> list:
    """列出剧集文件，按集数排序"""
    drama_dir = Path(drama_dir)
    if not drama_dir.is_dir():
        raise NotADirectoryError(f"目录不存在: {drama_dir}")

    exts = {".mp4", ".mkv", ".avi", ".mov", ".webm"}
    files = []
    for f in drama_dir.iterdir():
        if f.suffix.lower() in exts and f.is_file():
            files.append(f)

    # 简单按文件名排序（假设包含集数如 01、02 或 1、2）
    def sort_key(p: Path) -> tuple:
        name = p.stem
        # 尝试提取数字
        import re
        nums = re.findall(r"\d+", name)
        return (int(nums[0]) if nums else 0, name)

    files.sort(key=sort_key)
    return files


def analyze_episode_with_ai(
    episode_path: Path,
    scenes: List[Tuple[float, float]],
    frame_size: int = 384,
    model: str = "gpt-4o",
    max_scenes_to_analyze: int = 24,
    api_key: Optional[str] = None,
    base_url: Optional[str] = None,
) -> dict:
    """
    对单集进行 AI 分析
    每场景抽 1 帧，发送给 GPT 打分（模型由 config 指定）
    """
    if OpenAI is None:
        raise ImportError("请安装 openai: pip install openai")

    key = api_key or os.environ.get("OPENAI_API_KEY", "")
    client_kw = {"api_key": key}
    if base_url:
        client_kw["base_url"] = base_url.rstrip("/")
    client = OpenAI(**client_kw)

    # 限制分析场景数，避免成本过高
    scenes_to_use = scenes[:max_scenes_to_analyze]
    if len(scenes) > max_scenes_to_analyze:
        # 均匀采样
        step = len(scenes) / max_scenes_to_analyze
        indices = [int(i * step) for i in range(max_scenes_to_analyze)]
        scenes_to_use = [scenes[i] for i in indices]

    content = []
    for i, (start, end) in enumerate(scenes_to_use):
        mid = (start + end) / 2
        try:
            img_bytes = extract_frame(episode_path, mid, size=frame_size)
            b64 = frame_to_base64(img_bytes)
            content.append({
                "type": "image_url",
                "image_url": {"url": f"data:image/jpeg;base64,{b64}"}
            })
        except Exception as e:
            content.append({
                "type": "text",
                "text": f"[场景{i+1} 抽帧失败: {e}]"
            })

    prompt = """你是一个短剧广告素材分析师。下面是一集短剧的若干场景截图（按时间顺序）。

请分析每个场景，评估其作为「广告钩子」或「高能片段」的潜力。考虑：
- 是否有冲突、悬念、反转
- 情绪强度（愤怒、震惊、委屈、紧张等）
- 是否适合作为吸引用户点击的开头

请以 JSON 格式输出，格式如下：
{
  "scenes": [
    {
      "index": 1,
      "start_sec": 0.0,
      "end_sec": 5.2,
      "score": 8,
      "reason": "冲突明显，女主质问",
      "type": "hook|highlight|normal"
    }
  ],
  "episode_end_highlight": {"start_sec": 55.0, "end_sec": 60.0, "score": 9}
}

type 说明：hook=适合做钩子, highlight=高能片段, normal=普通
只输出 JSON，不要其他文字。"""

    # 把场景时间信息加入
    scene_info = "\n".join([
        f"场景{i+1}: {s:.1f}s - {e:.1f}s"
        for i, (s, e) in enumerate(scenes_to_use)
    ])
    content.insert(0, {"type": "text", "text": f"{prompt}\n\n场景时间轴：\n{scene_info}\n\n截图如下："})

    response = client.chat.completions.create(
        model=model,
        messages=[{"role": "user", "content": content}],
        max_tokens=2000,
    )
    text = response.choices[0].message.content

    # 解析 JSON
    try:
        raw = text.strip()
        # 尝试提取 JSON 块（markdown 代码块）
        if "```" in raw:
            start = raw.find("```") + 3
            if raw[start:start+4].lower() == "json":
                start += 4
            start = raw.find("{", start)  # 定位到 { 开始
            end = raw.rfind("}") + 1
            raw = raw[start:end]
        # 处理 "json\n{...}" 格式
        elif raw.lower().startswith("json"):
            raw = raw[4:].lstrip()
        return json.loads(raw)
    except json.JSONDecodeError:
        return {"raw": text, "scenes": [], "error": "JSON 解析失败"}


def run_analysis(
    drama_dir: Union[str, Path],
    output_path: Optional[Union[str, Path]] = None,
    config: Optional[dict] = None,
    max_episodes: Optional[int] = None,
) -> dict:
    """
    对整部剧进行分析

    Returns:
        {
          "episodes": [
            {
              "path": "...",
              "episode_num": 1,
              "scenes": [(s,e), ...],
              "analysis": {...}
            }
          ],
          "top_highlights": [...],
          "top_hooks": [...]
        }
    """
    config = config or load_config()
    drama_dir = Path(drama_dir)
    episodes = list_episodes(drama_dir)

    if not episodes:
        return {"error": "未找到剧集文件", "episodes": []}

    openai_cfg = config.get("openai", {})
    analyze_cfg = config.get("analyze", {})

    # 优先用参数，其次用配置；0 或不填表示分析全部
    limit = max_episodes
    if limit is None:
        limit = analyze_cfg.get("max_episodes") or 0
    if limit and limit > 0:
        episodes = episodes[:limit]
        print(f"限制分析前 {limit} 集")
    model = openai_cfg.get("model", "gpt-4o")
    api_key = openai_cfg.get("api_key") or os.environ.get("OPENAI_API_KEY")
    base_url = openai_cfg.get("base_url") or ""
    frame_size = analyze_cfg.get("frame_size", 384)
    max_scenes = analyze_cfg.get("scenes_per_episode", 24)

    result = {"episodes": [], "top_highlights": [], "top_hooks": []}
    all_highlights = []
    all_hooks = []

    for i, ep_path in enumerate(episodes):
        print(f"处理: {ep_path.name} ({i+1}/{len(episodes)})")
        scenes = detect_scenes(ep_path)
        if not scenes:
            result["episodes"].append({
                "path": str(ep_path),
                "episode_num": i + 1,
                "scenes": [],
                "analysis": {"error": "未检测到场景"},
            })
            continue

        analysis = analyze_episode_with_ai(
            ep_path, scenes,
            frame_size=frame_size,
            model=model,
            max_scenes_to_analyze=max_scenes,
            api_key=api_key,
            base_url=base_url if base_url else None,
        )

        ep_data = {
            "path": str(ep_path),
            "episode_num": i + 1,
            "scenes": scenes,
            "analysis": analysis,
        }
        result["episodes"].append(ep_data)

        # 收集高光和钩子
        if "scenes" in analysis:
            for s in analysis.get("scenes", []):
                seg = {
                    "episode": i + 1,
                    "path": str(ep_path),
                    **s,
                }
                if s.get("type") == "highlight":
                    all_highlights.append(seg)
                elif s.get("type") == "hook":
                    all_hooks.append(seg)

        if "episode_end_highlight" in analysis:
            h = analysis["episode_end_highlight"]
            all_highlights.append({
                "episode": i + 1,
                "path": str(ep_path),
                "start_sec": h.get("start_sec"),
                "end_sec": h.get("end_sec"),
                "score": h.get("score", 8),
                "type": "highlight",
                "reason": "集尾高亮",
            })

    # 按分数排序，取 top
    all_highlights.sort(key=lambda x: x.get("score", 0), reverse=True)
    all_hooks.sort(key=lambda x: x.get("score", 0), reverse=True)
    result["top_highlights"] = all_highlights[:10]
    result["top_hooks"] = all_hooks[:10]

    if output_path:
        output_path = Path(output_path)
        output_path.parent.mkdir(parents=True, exist_ok=True)
        with open(output_path, "w", encoding="utf-8") as f:
            json.dump(result, f, ensure_ascii=False, indent=2)

    return result


if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(description="分析短剧，输出候选片段")
    parser.add_argument("--input", "-i", required=True, help="剧集目录路径")
    parser.add_argument("--output", "-o", help="输出 JSON 路径")
    parser.add_argument("--max-episodes", "-n", type=int, help="最多分析集数（测试用）")
    args = parser.parse_args()

    run_analysis(args.input, args.output, max_episodes=args.max_episodes)
