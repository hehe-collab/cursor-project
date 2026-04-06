#!/usr/bin/env python3
"""
整条爆款（可跨多集）在同剧时间轴上的定位：将全集中文音轨按集顺序拼接，
与爆款音轨做 FFT 互相关，得到起始采样点，再映射为若干 (剧集文件, 起止秒)。
前提：爆款为同一时间线上的连续剪辑（无跳接回跳）；音轨与正片一致。
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import List, Tuple

import numpy as np
from scipy import signal

from poc_match import extract_mono_wav, list_episodes, wav_to_float32, _run_ffmpeg


def _correlate_lag_windowed(
    hay: np.ndarray,
    needle: np.ndarray,
    sr: int,
    decim: int,
    pad_sec: float,
) -> Tuple[int, str]:
    """
    先用降采样序列做 FFT 互相关找粗峰（省内存），再在 ±pad_sec 的窄窗内对全采样率做一次互相关。
    与对整段 hay 做单次 correlate 的结果在常见单峰情形下一致；可避免 valid 长度千万级的大数组。
    """
    m = len(needle)
    n_h = len(hay)
    if m < 256 or m > n_h:
        return 0, "invalid"
    d = max(2, int(decim))
    hay_d = hay[::d]
    ndl_d = needle[::d]
    md = len(ndl_d)
    if md < 256 or md > len(hay_d):
        cor = signal.correlate(hay, needle, mode="valid", method="fft")
        lag = int(np.argmax(cor))
        del cor
        return lag, "full"
    print(
        f"  粗互相关（降采样 ×{d}，约省输出长度至 1/{d}）…",
        flush=True,
    )
    cor_d = signal.correlate(hay_d, ndl_d, mode="valid", method="fft")
    lag_d = int(np.argmax(cor_d))
    del cor_d
    center = lag_d * d
    pad = max(int(pad_sec * sr), m // 4 + sr)
    lo = max(0, center - pad)
    hi = min(n_h, center + m + pad)
    hay_win = hay[lo:hi]
    if len(hay_win) < m + 16:
        cor = signal.correlate(hay, needle, mode="valid", method="fft")
        lag = int(np.argmax(cor))
        del cor
        return lag, "full_fallback_small_window"
    print(
        f"  精互相关（窄窗 {lo}–{hi} 样本，约 {(hi - lo) / sr:.1f}s）…",
        flush=True,
    )
    cor_f = signal.correlate(hay_win, needle, mode="valid", method="fft")
    lag_f = int(np.argmax(cor_f))
    del cor_f
    return lo + lag_f, "windowed"


def concat_episode_audio(
    episodes: List[Path],
    scratch: Path,
    sr: int,
) -> Tuple[np.ndarray, List[Tuple[Path, int, int]]]:
    """
    按集顺序拼接 WAV 采样；返回 concat 与 [(path, start_sample, end_sample), ...]（end 为 exclusive）。
    """
    scratch.mkdir(parents=True, exist_ok=True)
    pieces: List[np.ndarray] = []
    meta: List[Tuple[Path, int, int]] = []
    off = 0
    ep_dir = scratch / "timeline_wavs"
    ep_dir.mkdir(parents=True, exist_ok=True)
    total = len(episodes)
    for idx, ep in enumerate(episodes, 1):
        print(f"  音轨 [{idx}/{total}] {ep.name} …", flush=True)
        w = ep_dir / f"{ep.stem}.wav"
        extract_mono_wav(ep, w, sr)
        arr = wav_to_float32(w)
        a = off
        b = off + len(arr)
        meta.append((ep, a, b))
        pieces.append(arr)
        off = b
    return np.concatenate(pieces) if pieces else np.array([], dtype=np.float32), meta


def refine_timeline_lag_full(
    hay: np.ndarray,
    needle: np.ndarray,
    lag_coarse: int,
    sr: int,
    search_sec: float = 6.0,
    step_sec: float = 0.1,
) -> Tuple[int, float]:
    """
    在粗 lag 附近用「整段 needle」的归一化点积 (NCC) 做细网格搜索，
    避免仅用片头在重复 BGM 上误对齐；步长默认 0.1s。
    """
    m = len(needle)
    step = max(1, int(step_sec * sr))
    span = int(search_sec * sr)
    lo, hi = lag_coarse - span, lag_coarse + span
    lo = max(0, lo)
    hi = min(len(hay) - m, hi)
    den_n = float(np.linalg.norm(needle))
    if den_n < 1e-6:
        return lag_coarse, 0.0
    best_lag, best_ncc = lag_coarse, -1.0
    for L in range(lo, hi + 1, step):
        h = hay[L : L + m]
        den_h = float(np.linalg.norm(h))
        if den_h < 1e-6:
            continue
        ncc = float(np.dot(h, needle)) / (den_h * den_n)
        if ncc > best_ncc:
            best_ncc = ncc
            best_lag = L
    return best_lag, best_ncc


def cut_indo_clip(
    ido: Path,
    t0: float,
    t1: float,
    seg_out: Path,
    stream_copy: bool,
    cut_seek_margin_sec: float = 0.0,
) -> None:
    """从印尼单集 ido 裁 [t0,t1) 到 seg_out（与主流程一致的时间语义）。

    默认 margin<=0：重编码采用单次 `-ss` 在 `-i` 前（与已验证天马1_v2 成片一致）。
    margin>0：双 -ss 混合 seek，先入点前回退 N 秒再精切。
    """
    dur = float(t1) - float(t0)
    if dur <= 0:
        raise ValueError("cut duration <= 0")
    if stream_copy:
        cmd = [
            "-i",
            str(ido),
            "-ss",
            f"{t0:.6f}",
            "-to",
            f"{t1:.6f}",
            "-c",
            "copy",
            str(seg_out),
        ]
    elif cut_seek_margin_sec <= 0:
        cmd = [
            "-ss",
            f"{t0:.6f}",
            "-i",
            str(ido),
            "-t",
            f"{dur:.6f}",
            "-c:v",
            "libx264",
            "-preset",
            "fast",
            "-crf",
            "18",
            "-c:a",
            "aac",
            "-b:a",
            "192k",
            "-movflags",
            "+faststart",
            str(seg_out),
        ]
    else:
        if t0 <= 0:
            cmd = [
                "-i",
                str(ido),
                "-t",
                f"{dur:.6f}",
                "-c:v",
                "libx264",
                "-preset",
                "fast",
                "-crf",
                "18",
                "-c:a",
                "aac",
                "-b:a",
                "192k",
                "-movflags",
                "+faststart",
                str(seg_out),
            ]
        else:
            fast = max(0.0, float(t0) - float(cut_seek_margin_sec))
            fine = float(t0) - fast
            cmd = [
                "-ss",
                f"{fast:.6f}",
                "-i",
                str(ido),
                "-ss",
                f"{fine:.6f}",
                "-t",
                f"{dur:.6f}",
                "-c:v",
                "libx264",
                "-preset",
                "fast",
                "-crf",
                "18",
                "-c:a",
                "aac",
                "-b:a",
                "192k",
                "-movflags",
                "+faststart",
                str(seg_out),
            ]
    _run_ffmpeg(cmd)


def concat_demuxer_list(list_txt: Path, out: Path, reencode: bool) -> None:
    """ffmpeg concat demuxer：reencode=True 时整轨 libx264+aac。"""
    if reencode:
        _run_ffmpeg(
            [
                "-f",
                "concat",
                "-safe",
                "0",
                "-i",
                str(list_txt),
                "-c:v",
                "libx264",
                "-preset",
                "fast",
                "-crf",
                "18",
                "-c:a",
                "aac",
                "-b:a",
                "192k",
                "-movflags",
                "+faststart",
                str(out),
            ]
        )
    else:
        _run_ffmpeg(
            [
                "-f",
                "concat",
                "-safe",
                "0",
                "-i",
                str(list_txt),
                "-c",
                "copy",
                str(out),
            ]
        )


def global_to_cuts(
    meta: List[Tuple[Path, int, int]],
    g0: int,
    g1: int,
    sr: int,
) -> List[Tuple[Path, float, float]]:
    """全局采样区间 [g0, g1) 映射为每集内 (path, t0_sec, t1_sec)。"""
    cuts: List[Tuple[Path, float, float]] = []
    for path, a, b in meta:
        if g0 >= b:
            continue
        if g1 <= a:
            break
        o0 = max(g0, a)
        o1 = min(g1, b)
        t0 = (o0 - a) / float(sr)
        t1 = (o1 - a) / float(sr)
        if t1 > t0 + 1e-4:
            cuts.append((path, t0, t1))
    return cuts


def main() -> None:
    ap = argparse.ArgumentParser(description="整条爆款对齐到全剧时间轴（可跨集）")
    ap.add_argument("--episodes-dir", type=Path, required=True, help="用于建时间轴的原剧（建议中文）")
    ap.add_argument("--viral", type=Path, required=True, help="爆款 mp4")
    ap.add_argument("--scratch", type=Path, default=Path("./viral_clone/_scratch_timeline"))
    ap.add_argument("--sr", type=int, default=4000, help="对齐用采样率（越低越快，一般 4k 够用）")
    ap.add_argument("--indo-dir", type=Path, default=None, help="若给则按映射裁印尼同集同秒")
    ap.add_argument("--output", type=Path, default=None, help="印尼成片输出路径（需 --indo-dir）")
    ap.add_argument(
        "--stream-copy",
        action="store_true",
        help="裁切分段用流复制（快，但入点可能卡在关键帧，开头易丢几秒）",
    )
    ap.add_argument(
        "--no-refine-lag",
        action="store_true",
        help="关闭时间轴起点细搜（默认：在粗 lag 附近 ±6s、步长 0.1s 上用整段 NCC 再选峰）",
    )
    ap.add_argument(
        "--refine-search-sec",
        type=float,
        default=6.0,
        help="起点细搜左右范围（秒）",
    )
    ap.add_argument(
        "--refine-step-sec",
        type=float,
        default=0.1,
        help="起点细搜步长（秒），可试 0.05 以更贴原片",
    )
    ap.add_argument(
        "--full-correlate",
        action="store_true",
        help="互相关时对整段 hay 一次性 FFT（耗内存、易像卡死；默认用降采样粗搜+窄窗精搜）",
    )
    ap.add_argument(
        "--coarse-decim",
        type=int,
        default=8,
        help="粗互相关降采样因子（仅窗口模式，默认 8）",
    )
    ap.add_argument(
        "--correlate-pad-sec",
        type=float,
        default=45.0,
        help="粗峰左右保留窗口（秒），用于精互相关；周期性强时可略加大",
    )
    ap.add_argument(
        "--reencode-concat",
        action="store_true",
        help="最终 concat 不再 -c copy，整轨 libx264+aac 重编码（片段编码不一致或拼接报错时用）",
    )
    ap.add_argument(
        "--cut-seek-margin",
        type=float,
        default=0.0,
        help=(
            "重编码分段裁切：默认 0 = 仅单次输入侧 -ss（与您已验证的天马1_印尼_v2 成片路径一致）。"
            ">0 时为先回退 N 秒再解码流上精切（双 -ss），可试 3 减轻极端素材的入点偏差。"
        ),
    )
    ap.add_argument(
        "--min-ncc",
        type=float,
        default=0.30,
        help=(
            "整段 NCC 低于此阈值且指定了 --indo-dir 时，默认不导出印尼成片（避免误匹配仍输出）。"
            "同源短剧通常应明显高于此值；互相关≈随机峰时 NCC 可能只有 0.01～0.1。"
        ),
    )
    ap.add_argument(
        "--force-low-ncc",
        action="store_true",
        help="无视 --min-ncc 仍导出印尼成片（仅调试或自愿承担错片风险）",
    )
    args = ap.parse_args()

    eps_dir = args.episodes_dir.resolve()
    viral = args.viral.resolve()
    scratch = args.scratch.resolve()
    scratch.mkdir(parents=True, exist_ok=True)

    episodes = list_episodes(eps_dir)
    if not episodes:
        print("episodes-dir 无视频", file=sys.stderr)
        sys.exit(1)

    print(f"爆款: {viral.name}", flush=True)
    print(f"集数: {len(episodes)}，拼接时间轴（每集 ffmpeg 抽音轨，请稍候）…", flush=True)
    hay, meta = concat_episode_audio(episodes, scratch, args.sr)
    viral_wav = scratch / "viral.wav"
    print("爆款抽音轨 …", flush=True)
    extract_mono_wav(viral, viral_wav, args.sr)
    needle = wav_to_float32(viral_wav)

    if len(needle) < 256:
        print("爆款过短", file=sys.stderr)
        sys.exit(2)
    if len(needle) > len(hay):
        print(
            f"爆款音轨 ({len(needle)/args.sr:.1f}s) 长于全剧拼接 ({len(hay)/args.sr:.1f}s)，请检查是否同一部剧。",
            file=sys.stderr,
        )
        sys.exit(3)

    print(
        f"互相关定位起点（hay {len(hay)} / needle {len(needle)} 样本 @ {args.sr}Hz）…",
        flush=True,
    )
    if args.full_correlate:
        cor = signal.correlate(hay, needle, mode="valid", method="fft")
        lag_coarse = int(np.argmax(cor))
        del cor
        correlate_mode = "full"
    else:
        lag_coarse, correlate_mode = _correlate_lag_windowed(
            hay,
            needle,
            args.sr,
            decim=args.coarse_decim,
            pad_sec=args.correlate_pad_sec,
        )
    lag = lag_coarse
    if not args.no_refine_lag:
        print(
            f"  整段 NCC 网格细化（±{args.refine_search_sec}s，步长 {args.refine_step_sec}s）…",
            flush=True,
        )
        lag, _ncc_scan = refine_timeline_lag_full(
            hay,
            needle,
            lag_coarse,
            args.sr,
            search_sec=args.refine_search_sec,
            step_sec=args.refine_step_sec,
        )
        if lag != lag_coarse:
            print(
                f"  时间轴起点细化(整段 NCC): {lag_coarse} → {lag} 样本 "
                f"（Δ {(lag - lag_coarse) / float(args.sr):.2f}s）"
            )
    n = float(np.linalg.norm(needle))
    den_tail = float(np.linalg.norm(hay[lag : lag + len(needle)]))
    ncc_full = (
        float(np.dot(hay[lag : lag + len(needle)], needle)) / (n * den_tail)
        if n > 1e-6 and den_tail > 1e-6
        else 0.0
    )
    ncc_coarse_full = (
        float(np.dot(hay[lag_coarse : lag_coarse + len(needle)], needle)) / (n * float(np.linalg.norm(hay[lag_coarse : lag_coarse + len(needle)])))
        if n > 1e-6
        else 0.0
    )
    ncc = ncc_full

    g0, g1 = lag, lag + len(needle)
    cuts_cn = global_to_cuts(meta, g0, g1, args.sr)
    dur_viral = len(needle) / float(args.sr)

    report = {
        "viral": str(viral),
        "episodes_dir": str(eps_dir),
        "sample_rate": args.sr,
        "timeline_start_sample": g0,
        "timeline_end_sample": g1,
        "viral_duration_sec": dur_viral,
        "ncc_full_at_refined_lag": round(ncc_full, 4),
        "ncc_full_at_coarse_lag": round(ncc_coarse_full, 4),
        "lag_coarse_sample": lag_coarse,
        "lag_refined_sample": lag,
        "lag_refined_by_full_ncc_scan": not args.no_refine_lag,
        "refine_search_sec": args.refine_search_sec,
        "refine_step_sec": args.refine_step_sec,
        "correlate_mode": correlate_mode,
        "coarse_decim": args.coarse_decim if not args.full_correlate else None,
        "correlate_pad_sec": args.correlate_pad_sec if not args.full_correlate else None,
        "segment_cut": "stream_copy" if args.stream_copy else "reencode_h264_aac",
        "cut_seek_margin_sec": args.cut_seek_margin,
        "cuts_chinese": [
            {"path": str(p), "start_sec": round(t0, 3), "end_sec": round(t1, 3)}
            for p, t0, t1 in cuts_cn
        ],
        "episode_timeline": [
            {
                "path": str(p.resolve()),
                "start_sample": a,
                "end_sample": b,
            }
            for p, a, b in meta
        ],
        "min_ncc_threshold": args.min_ncc,
        "indo_export_forced_low_ncc": bool(args.force_low_ncc),
    }

    block_export = (
        args.indo_dir is not None
        and ncc_full < args.min_ncc
        and not args.force_low_ncc
    )
    report["indo_export_blocked_low_ncc"] = bool(block_export)

    rpt_path = scratch / "match_report.json"
    with open(rpt_path, "w", encoding="utf-8") as f:
        json.dump(report, f, ensure_ascii=False, indent=2)

    print("—— 时间轴匹配 ——")
    print(
        f"  整段 NCC@细化起点: {ncc_full:.4f}；@粗起点: {ncc_coarse_full:.4f} "
        f"（低则检查 BGM/多峰值；开头画面仍以裁切精度为准）"
    )
    if ncc_full < 0.55:
        print(
            "  提示: NCC 偏低时，建议耳机核对爆款与中文原剧起点；"
            "必要时试 `--no-refine-lag` 或 `--refine-step-sec 0.05` 对照。",
            flush=True,
        )
    if args.cut_seek_margin and args.cut_seek_margin > 0:
        print(
            f"  裁切: 混合 seek（回退 {args.cut_seek_margin:g}s 后精切）",
            flush=True,
        )
    elif not args.stream_copy:
        print(
            "  裁切: 单次输入侧 -ss + 重编码（默认，对齐天马1_v2 验证路径）",
            flush=True,
        )
    print(f"  爆款时长: {dur_viral:.2f}s")
    print(f"  对应中文切片数: {len(cuts_cn)}")
    for i, (p, t0, t1) in enumerate(cuts_cn, 1):
        print(f"    {i}. {p.name}  {t0:.2f}s – {t1:.2f}s  (长 {t1-t0:.2f}s)")
    print(f"  报告: {rpt_path}")

    if block_export:
        print("", flush=True)
        print(
            f"【已中止导出印尼成片】整段 NCC={ncc_full:.4f} < 阈值 {args.min_ncc:.2f}。"
            " 此时互相关**未找到可信对齐**，继续裁切会得到与爆款无关的错片。",
            file=sys.stderr,
        )
        print(
            "常见原因：① 本条爆款音轨**不是**《流星》正片同源（或换过 BGM/调速）；"
            "② 多条爆款共用一个 `--output` **后跑覆盖先跑**，误以为「第一条也错了」。",
            file=sys.stderr,
        )
        print(
            "处理：换用同源爆款 / 换 `--output` 路径；- 若坚持导出可加 `--force-low-ncc`。",
            file=sys.stderr,
        )
        sys.exit(7)

    if args.indo_dir:
        indo_dir = args.indo_dir.resolve()
        out = (
            args.output.resolve()
            if args.output
            else scratch / "indo_output_from_viral.mp4"
        )
        out.parent.mkdir(parents=True, exist_ok=True)
        seg_paths: List[Path] = []
        ncuts = len(cuts_cn)
        for i, (cn_p, t0, t1) in enumerate(cuts_cn):
            print(
                f"  导出印尼片段 [{i + 1}/{ncuts}] {cn_p.name} {t0:.2f}s–{t1:.2f}s …",
                flush=True,
            )
            stem = cn_p.stem
            ido = indo_dir / f"{stem}{cn_p.suffix}"
            if not ido.exists():
                print(f"  缺少印尼文件: {ido}", file=sys.stderr)
                sys.exit(4)
            seg = scratch / f"indo_seg_{i:03d}.mp4"
            cut_indo_clip(
                ido,
                t0,
                t1,
                seg,
                args.stream_copy,
                cut_seek_margin_sec=args.cut_seek_margin,
            )
            seg_paths.append(seg)
        print("  concat 合并成片 …", flush=True)
        list_txt = scratch / "concat_list.txt"
        with open(list_txt, "w", encoding="utf-8") as f:
            for sp in seg_paths:
                ps = str(sp.resolve()).replace("'", "'\\''")
                f.write(f"file '{ps}'\n")
        concat_demuxer_list(list_txt, out, args.reencode_concat)
        print(f"  印尼成片: {out}")


if __name__ == "__main__":
    main()
