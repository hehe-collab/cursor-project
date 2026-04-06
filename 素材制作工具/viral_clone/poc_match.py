#!/usr/bin/env python3
"""
PoC：同音轨前提下，用音频互相关把「爆款/片段」定位到原剧集与时间偏移。
不依赖 fpcalc/dejavu；适合短剧单集数分钟量级的批量扫集。
"""
from __future__ import annotations

import argparse
import re
import subprocess
import sys
from pathlib import Path
from typing import List, Optional, Tuple

import numpy as np
from scipy import signal


def _run_ffmpeg(args: List[str]) -> None:
    r = subprocess.run(
        ["ffmpeg", "-y", "-hide_banner", "-loglevel", "error"] + args,
        capture_output=True,
        text=True,
    )
    if r.returncode != 0:
        raise RuntimeError(r.stderr or "ffmpeg failed")


def extract_mono_wav(video: Path, wav_out: Path, sample_rate: int = 8000) -> None:
    wav_out.parent.mkdir(parents=True, exist_ok=True)
    _run_ffmpeg(
        [
            "-i",
            str(video),
            "-vn",
            "-ac",
            "1",
            "-ar",
            str(sample_rate),
            "-f",
            "wav",
            str(wav_out),
        ]
    )


def wav_to_float32(path: Path) -> np.ndarray:
    """16-bit PCM mono WAV -> float32 [-1,1]."""
    from wave import open as wave_open

    with wave_open(str(path), "rb") as w:
        nch = w.getnchannels()
        sw = w.getsampwidth()
        nframes = w.getnframes()
        raw = w.readframes(nframes)
    if nch != 1 or sw != 2:
        raise ValueError(f"expected mono int16 wav, got ch={nch} width={sw}")
    x = np.frombuffer(raw, dtype="<i2").astype(np.float32) / 32768.0
    return x


def best_match_offset(
    haystack: np.ndarray, needle: np.ndarray
) -> Tuple[int, float]:
    """needle 在 haystack 中的起始样本下标与归一化峰值相关 (rough NCC peak)."""
    if len(needle) < 256 or len(needle) > len(haystack):
        return -1, 0.0
    # valid 互相关：cor[i] = dot(hay[i:i+M], needle)
    cor = signal.correlate(haystack, needle, mode="valid", method="fft")
    i = int(np.argmax(cor))
    peak = float(cor[i])
    n = float(np.linalg.norm(needle))
    denom = float(np.linalg.norm(haystack[i : i + len(needle)]))
    if n < 1e-6 or denom < 1e-6:
        return i, 0.0
    ncc = peak / (n * denom)
    return i, ncc


def list_episodes(d: Path) -> List[Path]:
    exts = {".mp4", ".mkv", ".mov", ".webm"}

    def sort_key(p: Path) -> Tuple[int, str]:
        stem = p.stem
        nums = re.findall(r"\d+", stem)
        return (int(nums[0]) if nums else 0, stem)

    files = sorted(
        [p for p in d.iterdir() if p.suffix.lower() in exts and p.is_file()],
        key=sort_key,
    )
    return files


def main() -> None:
    ap = argparse.ArgumentParser(description="PoC: 音轨匹配定位（同片正片）")
    ap.add_argument(
        "--episodes-dir",
        type=Path,
        required=True,
        help="原剧目录（如印尼或中文，音轨一致即可）",
    )
    ap.add_argument(
        "--viral",
        type=Path,
        default=None,
        help="爆款/片段 mp4；不配则用本目录第一集切 60~90s 伪爆款",
    )
    ap.add_argument(
        "--scratch",
        type=Path,
        default=Path("./viral_clone/_scratch"),
        help="临时 wav 等",
    )
    ap.add_argument("--sr", type=int, default=8000, help="提取音频采样率")
    ap.add_argument(
        "--min-ncc",
        type=float,
        default=0.85,
        help="归一化相关阈值（同一条母带通常 >0.9）",
    )
    args = ap.parse_args()

    eps_dir = args.episodes_dir.resolve()
    scratch = args.scratch.resolve()
    scratch.mkdir(parents=True, exist_ok=True)

    episodes = list_episodes(eps_dir)
    if not episodes:
        print("目录内无 mp4/mkv", file=sys.stderr)
        sys.exit(1)

    if args.viral:
        viral = args.viral.resolve()
    else:
        # 从第一集切 60~90s 作为伪爆款
        src = episodes[0]
        viral = scratch / "_synthetic_viral.mp4"
        _run_ffmpeg(
            [
                "-i",
                str(src),
                "-ss",
                "60",
                "-t",
                "30",
                "-c",
                "copy",
                str(viral),
            ]
        )
        print(f"[PoC] 未指定 --viral，已用 {src.name} 60~90s 生成伪爆款: {viral}")

    viral_wav = scratch / "viral.wav"
    extract_mono_wav(viral, viral_wav, args.sr)
    needle = wav_to_float32(viral_wav)

    best_ep: Optional[Path] = None
    best_pos = -1
    best_ncc = -1.0
    best_hay_len = 0

    for ep in episodes:
        w = scratch / "ep_audio" / f"{ep.stem}.wav"
        try:
            extract_mono_wav(ep, w, args.sr)
            hay = wav_to_float32(w)
        except Exception as e:
            print(f"  skip {ep.name}: {e}", file=sys.stderr)
            continue
        pos, ncc = best_match_offset(hay, needle)
        if ncc > best_ncc:
            best_ncc = ncc
            best_pos = pos
            best_ep = ep
            best_hay_len = len(hay)

    if best_ep is None or best_pos < 0:
        print("匹配失败", file=sys.stderr)
        sys.exit(2)

    t0 = best_pos / float(args.sr)
    dur = len(needle) / float(args.sr)
    t1 = t0 + dur

    print("—— 匹配结果 ——")
    print(f"  剧集文件: {best_ep.name}")
    print(f"  起止(秒): {t0:.3f} ~ {t1:.3f}  (时长 {dur:.3f}s)")
    print(f"  峰值 NCC: {best_ncc:.4f}  (建议 >= {args.min_ncc})")
    if best_ncc < args.min_ncc:
        print(
            "  警告: 相关偏低，可能非同一母带、重编码或爆款含大量 BGM 混音。",
            file=sys.stderr,
        )

    out_v = scratch / "poc_output_segment.mp4"
    _run_ffmpeg(
        [
            "-i",
            str(best_ep),
            "-ss",
            str(t0),
            "-t",
            str(dur),
            "-c",
            "copy",
            str(out_v),
        ]
    )
    print(f"  已裁切验证片: {out_v}")
    print("  可与原爆款并排放映对比画面（字幕可能不同）与听感。")


if __name__ == "__main__":
    main()
