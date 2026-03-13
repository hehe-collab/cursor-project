"""
修复 analysis.json 中因 JSON 解析失败而缺失的 scenes/top_highlights/top_hooks
从 raw 字段重新解析
"""

import json
from pathlib import Path
from typing import Union


def parse_raw(raw: str) -> dict:
    """从 raw 文本中解析 JSON"""
    text = raw.strip()
    if text.lower().startswith("json"):
        text = text[4:].lstrip()
    # 提取 {...} 部分
    start = text.find("{")
    if start >= 0:
        depth = 0
        for i, c in enumerate(text[start:], start):
            if c == "{":
                depth += 1
            elif c == "}":
                depth -= 1
                if depth == 0:
                    text = text[start:i+1]
                    break
    return json.loads(text)


def repair(analysis_path: Union[str, Path]) -> None:
    analysis_path = Path(analysis_path)
    with open(analysis_path, encoding="utf-8") as f:
        data = json.load(f)

    all_highlights = []
    all_hooks = []

    for ep in data.get("episodes", []):
        ana = ep.get("analysis", {})
        if ana.get("error") == "JSON 解析失败" and "raw" in ana:
            try:
                parsed = parse_raw(ana["raw"])
                ana["scenes"] = parsed.get("scenes", [])
                if "episode_end_highlight" in parsed:
                    ana["episode_end_highlight"] = parsed["episode_end_highlight"]
                ana.pop("error", None)
                ana.pop("raw", None)

                ep_path = ep.get("path", "")
                ep_num = ep.get("episode_num", 0)

                for s in ana.get("scenes", []):
                    seg = {"episode": ep_num, "path": ep_path, **s}
                    if s.get("type") == "highlight":
                        all_highlights.append(seg)
                    elif s.get("type") == "hook":
                        all_hooks.append(seg)

                if "episode_end_highlight" in parsed:
                    h = parsed["episode_end_highlight"]
                    all_highlights.append({
                        "episode": ep_num, "path": ep_path,
                        "start_sec": h.get("start_sec"), "end_sec": h.get("end_sec"),
                        "score": h.get("score", 8), "type": "highlight", "reason": "集尾高亮",
                    })
            except Exception as e:
                print(f"修复失败 episode {ep.get('episode_num')}: {e}")

    all_highlights.sort(key=lambda x: x.get("score", 0), reverse=True)
    all_hooks.sort(key=lambda x: x.get("score", 0), reverse=True)
    data["top_highlights"] = all_highlights[:10]
    data["top_hooks"] = all_hooks[:10]

    with open(analysis_path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

    print(f"已修复: top_highlights={len(data['top_highlights'])}, top_hooks={len(data['top_hooks'])}")


if __name__ == "__main__":
    import sys
    path = sys.argv[1] if len(sys.argv) > 1 else "analysis.json"
    repair(path)
