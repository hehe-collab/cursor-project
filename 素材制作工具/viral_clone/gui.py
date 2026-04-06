#!/usr/bin/env python3
"""
爆款结构复制 · 简易图形界面（tkinter，无需额外 GUI 库）。
选择原剧目录、印尼目录、爆款文件或文件夹内 mp4，一键调用 match_viral_timeline / 镜头检测等。

若启动时报错并 abort：「macOS 26 (…) or later required, have instead 16 (…)」——
这是 Apple 自带 Python 所链接的 Tcl/Tk 与当前系统版本检测不兼容，请改用纯标准库浏览器界面：

  python viral_clone/gui_web.py
"""
from __future__ import annotations

import queue
import re
import subprocess
import sys
import threading
from pathlib import Path
from typing import List, Optional

# 素材制作工具/
TOOL_ROOT = Path(__file__).resolve().parent.parent
VIRAL_CLONE = TOOL_ROOT / "viral_clone"


def _natural_sort_key(p: Path) -> tuple:
    stem = p.stem
    return tuple(int(x) if x.isdigit() else x.lower() for x in re.split(r"(\d+)", stem))


def _list_mp4_in_dir(d: Path) -> List[Path]:
    if not d.is_dir():
        return []
    files = list(d.glob("*.mp4")) + list(d.glob("**/*.mp4"))
    seen = set()
    uniq: List[Path] = []
    for f in sorted(files, key=lambda x: (_natural_sort_key(x), str(x))):
        rp = f.resolve()
        if rp not in seen:
            seen.add(rp)
            uniq.append(rp)
    return uniq


class ViralCloneApp:
    def __init__(self) -> None:
        import tkinter as tk
        from tkinter import filedialog, messagebox, scrolledtext, ttk

        self._tk = tk
        self._filedialog = filedialog
        self._messagebox = messagebox
        self._scrolledtext = scrolledtext
        self._ttk = ttk

        self.root = tk.Tk()
        self.root.title("爆款复制 · 时间轴匹配与印尼导出")
        self.root.minsize(720, 560)
        self.root.geometry("900x640")

        self._log_queue: queue.Queue = queue.Queue()
        self._runner: Optional[threading.Thread] = None

        nb = ttk.Notebook(self.root)
        nb.pack(fill=tk.BOTH, expand=True, padx=8, pady=8)

        self._tab_main = ttk.Frame(nb, padding=8)
        self._tab_scenes = ttk.Frame(nb, padding=8)
        nb.add(self._tab_main, text="主流程：匹配 + 导出")
        nb.add(self._tab_scenes, text="按镜头导出（进阶）")

        self._build_tab_main()
        self._build_tab_scenes()
        self.root.after(120, self._drain_log_queue)

    def _build_tab_main(self) -> None:
        tk = self._tk
        ttk = self._ttk
        f = self._tab_main

        row = 0
        ttk.Label(
            f,
            text="用于对齐时间轴的原剧目录（与爆款同片、同音轨；一般为中文版）",
        ).grid(row=row, column=0, columnspan=3, sticky="w")
        row += 1

        self.var_episodes = tk.StringVar()
        ttk.Entry(f, textvariable=self.var_episodes, width=72).grid(
            row=row, column=0, columnspan=2, sticky="ew", pady=2
        )
        ttk.Button(f, text="浏览…", command=self._browse_episodes).grid(
            row=row, column=2, padx=4, pady=2
        )
        row += 1

        ttk.Label(f, text="印尼版原剧目录（导出成片时从对应集同时间码裁切）").grid(
            row=row, column=0, columnspan=3, sticky="w"
        )
        row += 1
        self.var_indo = tk.StringVar()
        ttk.Entry(f, textvariable=self.var_indo, width=72).grid(
            row=row, column=0, columnspan=2, sticky="ew", pady=2
        )
        ttk.Button(f, text="浏览…", command=self._browse_indo).grid(
            row=row, column=2, padx=4, pady=2
        )
        row += 1

        ttk.Label(f, text="爆款素材（可选文件夹导入，列表中选一个 mp4）").grid(
            row=row, column=0, columnspan=3, sticky="w"
        )
        row += 1
        viral_row = ttk.Frame(f)
        viral_row.grid(row=row, column=0, columnspan=3, sticky="ew")
        self.var_viral = tk.StringVar()
        ttk.Entry(viral_row, textvariable=self.var_viral, width=58).pack(
            side=tk.LEFT, fill=tk.X, expand=True, padx=(0, 4)
        )
        ttk.Button(viral_row, text="选文件", command=self._browse_viral_file).pack(
            side=tk.LEFT, padx=2
        )
        ttk.Button(viral_row, text="导入文件夹", command=self._browse_viral_folder).pack(
            side=tk.LEFT, padx=2
        )
        row += 1

        list_fr = ttk.Frame(f)
        list_fr.grid(row=row, column=0, columnspan=3, sticky="nsew", pady=4)
        f.rowconfigure(row, weight=1)
        f.columnconfigure(0, weight=1)
        sb = ttk.Scrollbar(list_fr)
        sb.pack(side=tk.RIGHT, fill=tk.Y)
        self.list_viral = tk.Listbox(
            list_fr,
            height=6,
            selectmode=tk.SINGLE,
            yscrollcommand=sb.set,
            font=("Menlo", 11) if sys.platform == "darwin" else ("Consolas", 10),
        )
        self.list_viral.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        sb.config(command=self.list_viral.yview)
        self.list_viral.bind("<<ListboxSelect>>", self._on_list_select)
        self.list_viral.bind("<Double-Button-1>", self._on_list_double)
        row += 1

        ttk.Label(f, text="缓存目录 scratch").grid(row=row, column=0, sticky="w")
        row += 1
        self.var_scratch = tk.StringVar(value=str(TOOL_ROOT / "viral_clone" / "_scratch_gui"))
        ttk.Entry(f, textvariable=self.var_scratch, width=72).grid(
            row=row, column=0, columnspan=2, sticky="ew", pady=2
        )
        ttk.Button(f, text="浏览…", command=self._browse_scratch).grid(
            row=row, column=2, padx=4, pady=2
        )
        row += 1

        ttk.Label(f, text="印尼成片输出路径（.mp4）").grid(row=row, column=0, sticky="w")
        row += 1
        self.var_output = tk.StringVar()
        ttk.Entry(f, textvariable=self.var_output, width=72).grid(
            row=row, column=0, columnspan=2, sticky="ew", pady=2
        )
        ttk.Button(f, text="浏览…", command=self._browse_output).grid(
            row=row, column=2, padx=4, pady=2
        )
        row += 1

        opt = ttk.LabelFrame(f, text="选项", padding=6)
        opt.grid(row=row, column=0, columnspan=3, sticky="ew", pady=6)
        self.var_export_indo = tk.BooleanVar(value=True)
        self.var_stream_copy = tk.BooleanVar(value=False)
        self.var_reencode_concat = tk.BooleanVar(value=False)
        self.var_no_refine = tk.BooleanVar(value=False)
        self.var_force_low_ncc = tk.BooleanVar(value=False)
        ttk.Checkbutton(
            opt,
            text="导出印尼成片（取消则只生成 match_report 与缓存，不写输出 mp4）",
            variable=self.var_export_indo,
        ).grid(row=0, column=0, sticky="w")
        ttk.Checkbutton(
            opt,
            text="分段裁切使用流复制（快，入点可能卡在关键帧）",
            variable=self.var_stream_copy,
        ).grid(row=1, column=0, sticky="w")
        ttk.Checkbutton(
            opt,
            text="最终合并整轨重编码（片段参数不一致或 concat 报错时勾选）",
            variable=self.var_reencode_concat,
        ).grid(row=2, column=0, sticky="w")
        ttk.Checkbutton(
            opt,
            text="关闭起点细搜（略快，对齐可能略粗）",
            variable=self.var_no_refine,
        ).grid(row=3, column=0, sticky="w")
        ttk.Checkbutton(
            opt,
            text="NCC 过低仍导出印尼成片（不推荐）",
            variable=self.var_force_low_ncc,
        ).grid(row=4, column=0, sticky="w")
        row += 1

        btn_fr = ttk.Frame(f)
        btn_fr.grid(row=row, column=0, columnspan=3, pady=8)
        self.btn_run = ttk.Button(btn_fr, text="开始运行", command=self._run_match)
        self.btn_run.pack(side=tk.LEFT, padx=4)
        ttk.Button(btn_fr, text="打开缓存目录", command=self._open_scratch).pack(
            side=tk.LEFT, padx=4
        )
        ttk.Button(btn_fr, text="清空日志", command=self._clear_log).pack(side=tk.LEFT, padx=4)
        row += 1

        self.log = self._scrolledtext.ScrolledText(
            f, height=14, state=tk.DISABLED, font=("Menlo", 10) if sys.platform == "darwin" else ("Consolas", 9)
        )
        self.log.grid(row=row, column=0, columnspan=3, sticky="nsew", pady=4)
        f.rowconfigure(row, weight=2)

    def _build_tab_scenes(self) -> None:
        tk = self._tk
        ttk = self._ttk
        f = self._tab_scenes

        ttk.Label(
            f,
            text=(
                "需先完成主流程生成 match_report.json（含 episode_timeline）。"
                "「仅镜头检测」会把 JSON 写到「镜头检测输出目录」，文件名为「视频名_scenes.json」；"
                "「按镜头导出」时再浏览选择该 JSON。"
            ),
            wraplength=820,
        ).pack(anchor="w", pady=(0, 8))

        self.var_report = tk.StringVar(
            value=str(TOOL_ROOT / "viral_clone" / "_scratch_gui" / "match_report.json")
        )
        self.var_scene_detect_dir = tk.StringVar(
            value=str(TOOL_ROOT / "viral_clone" / "_scenes")
        )
        self.var_scenes_json = tk.StringVar(value="")
        self.var_indo2 = tk.StringVar()
        self.var_out2 = tk.StringVar()
        self.var_scratch2 = tk.StringVar(value=str(TOOL_ROOT / "viral_clone" / "_scratch_indo_scenes"))

        for label, var, cmd in [
            ("match_report.json", self.var_report, self._browse_report),
            ("镜头检测输出目录", self.var_scene_detect_dir, self._browse_scene_detect_dir),
            ("导引用 · 镜头表 JSON", self.var_scenes_json, self._browse_scenes),
            ("印尼原剧目录", self.var_indo2, self._browse_indo2),
            ("输出 mp4", self.var_out2, self._browse_out2),
            ("片段缓存 scratch", self.var_scratch2, self._browse_scratch2),
        ]:
            fr = ttk.Frame(f)
            fr.pack(fill=tk.X, pady=3)
            ttk.Label(fr, text=label, width=22).pack(side=tk.LEFT)
            ttk.Entry(fr, textvariable=var).pack(side=tk.LEFT, fill=tk.X, expand=True, padx=4)
            ttk.Button(fr, text="浏览…", command=cmd).pack(side=tk.LEFT)

        self.var_sc_stream = tk.BooleanVar(value=False)
        self.var_sc_reenc = tk.BooleanVar(value=False)
        of = ttk.Frame(f)
        of.pack(anchor="w", pady=6)
        ttk.Checkbutton(
            of, text="分段流复制", variable=self.var_sc_stream
        ).pack(side=tk.LEFT, padx=4)
        ttk.Checkbutton(
            of, text="合并时整轨重编码", variable=self.var_sc_reenc
        ).pack(side=tk.LEFT, padx=4)

        bf = ttk.Frame(f)
        bf.pack(pady=8)
        ttk.Button(bf, text="仅运行镜头检测（需下面先设爆款路径）", command=self._run_scene_only).pack(
            side=tk.LEFT, padx=4
        )
        ttk.Button(bf, text="按镜头导出印尼成片", command=self._run_indo_scenes).pack(
            side=tk.LEFT, padx=4
        )

        self.var_scene_viral = tk.StringVar()
        sf = ttk.Frame(f)
        sf.pack(fill=tk.X, pady=4)
        ttk.Label(sf, text="镜头检测用爆款 mp4").pack(side=tk.LEFT)
        ttk.Entry(sf, textvariable=self.var_scene_viral, width=50).pack(
            side=tk.LEFT, fill=tk.X, expand=True, padx=4
        )
        ttk.Button(sf, text="选文件", command=self._browse_scene_viral).pack(side=tk.LEFT)

    def _browse_episodes(self) -> None:
        d = self._filedialog.askdirectory(title="选择原剧目录（建议中文）")
        if d:
            self.var_episodes.set(d)

    def _browse_indo(self) -> None:
        d = self._filedialog.askdirectory(title="选择印尼原剧目录")
        if d:
            self.var_indo.set(d)

    def _browse_scratch(self) -> None:
        d = self._filedialog.askdirectory(title="选择 scratch 目录")
        if d:
            self.var_scratch.set(d)

    def _browse_output(self) -> None:
        p = self._filedialog.asksaveasfilename(
            title="印尼成片保存为",
            defaultextension=".mp4",
            filetypes=[("MP4", "*.mp4"), ("全部", "*.*")],
        )
        if p:
            self.var_output.set(p)

    def _browse_viral_file(self) -> None:
        p = self._filedialog.askopenfilename(
            title="选择爆款 mp4",
            filetypes=[("视频", "*.mp4 *.mov *.mkv"), ("全部", "*.*")],
        )
        if p:
            self.var_viral.set(p)
            self.var_scene_viral.set(p)
            self._sync_output_default(Path(p))

    def _browse_viral_folder(self) -> None:
        d = self._filedialog.askdirectory(title="选择爆款素材文件夹")
        if not d:
            return
        paths = _list_mp4_in_dir(Path(d))
        self.list_viral.delete(0, self._tk.END)
        for p in paths:
            self.list_viral.insert(self._tk.END, str(p))
        if paths:
            self.list_viral.selection_set(0)
            self.var_viral.set(str(paths[0]))
            self._sync_output_default(paths[0])

    def _sync_output_default(self, viral: Path) -> None:
        if self.var_output.get().strip():
            return
        scratch = Path(self.var_scratch.get().strip())
        try:
            scratch.mkdir(parents=True, exist_ok=True)
        except OSError:
            scratch = TOOL_ROOT / "viral_clone" / "_scratch_gui"
        self.var_output.set(str(scratch / f"{viral.stem}_indo.mp4"))

    def _on_list_select(self, _evt=None) -> None:
        sel = self.list_viral.curselection()
        if not sel:
            return
        p = self.list_viral.get(sel[0])
        self.var_viral.set(p)
        self.var_scene_viral.set(p)
        self._sync_output_default(Path(p))

    def _on_list_double(self, _evt=None) -> None:
        self._on_list_select()

    def _browse_report(self) -> None:
        p = self._filedialog.askopenfilename(title="match_report.json", filetypes=[("JSON", "*.json")])
        if p:
            self.var_report.set(p)

    def _browse_scene_detect_dir(self) -> None:
        d = self._filedialog.askdirectory(title="镜头检测 JSON 输出目录")
        if d:
            self.var_scene_detect_dir.set(d)

    def _browse_scenes(self) -> None:
        p = self._filedialog.askopenfilename(
            title="镜头检测生成的 _scenes.json",
            filetypes=[("JSON", "*.json"), ("全部", "*.*")],
        )
        if p:
            self.var_scenes_json.set(p)

    def _browse_indo2(self) -> None:
        d = self._filedialog.askdirectory(title="印尼原剧目录")
        if d:
            self.var_indo2.set(d)

    def _browse_out2(self) -> None:
        p = self._filedialog.asksaveasfilename(
            title="按镜头导出保存为",
            defaultextension=".mp4",
            filetypes=[("MP4", "*.mp4")],
        )
        if p:
            self.var_out2.set(p)

    def _browse_scratch2(self) -> None:
        d = self._filedialog.askdirectory(title="片段缓存目录")
        if d:
            self.var_scratch2.set(d)

    def _browse_scene_viral(self) -> None:
        p = self._filedialog.askopenfilename(
            title="镜头检测用爆款 mp4",
            filetypes=[("视频", "*.mp4 *.mov *.mkv"), ("全部", "*.*")],
        )
        if p:
            self.var_scene_viral.set(p)

    def _append_log(self, s: str) -> None:
        self.log.config(state=self._tk.NORMAL)
        self.log.insert(self._tk.END, s)
        self.log.see(self._tk.END)
        self.log.config(state=self._tk.DISABLED)

    def _clear_log(self) -> None:
        self.log.config(state=self._tk.NORMAL)
        self.log.delete("1.0", self._tk.END)
        self.log.config(state=self._tk.DISABLED)

    def _drain_log_queue(self) -> None:
        try:
            while True:
                line = self._log_queue.get_nowait()
                if line is None:
                    self.btn_run.config(state=self._tk.NORMAL)
                    break
                self._append_log(line)
        except queue.Empty:
            pass
        self.root.after(120, self._drain_log_queue)

    def _open_scratch(self) -> None:
        p = Path(self.var_scratch.get().strip())
        p.mkdir(parents=True, exist_ok=True)
        import os

        if sys.platform == "darwin":
            subprocess.Popen(["open", str(p)])
        elif os.name == "nt":
            os.startfile(str(p))  # type: ignore[attr-defined]
        else:
            subprocess.Popen(["xdg-open", str(p)])

    def _validate_main(self) -> bool:
        ep = self.var_episodes.get().strip()
        vir = self.var_viral.get().strip()
        if not ep or not Path(ep).is_dir():
            self._messagebox.showerror("校验", "请选择有效的原剧目录。")
            return False
        if not vir or not Path(vir).is_file():
            self._messagebox.showerror("校验", "请选择有效的爆款视频文件。")
            return False
        if self.var_export_indo.get():
            indo = self.var_indo.get().strip()
            if not indo or not Path(indo).is_dir():
                self._messagebox.showerror("校验", "导出印尼成片时，请填写印尼原剧目录。")
                return False
            out = self.var_output.get().strip()
            if not out:
                self._messagebox.showerror("校验", "请填写输出 mp4 路径。")
                return False
        return True

    def _run_match(self) -> None:
        if not self._validate_main():
            return
        self.btn_run.config(state=self._tk.DISABLED)
        scratch = self.var_scratch.get().strip()
        Path(scratch).mkdir(parents=True, exist_ok=True)

        cmd: List[str] = [
            sys.executable,
            str(VIRAL_CLONE / "match_viral_timeline.py"),
            "--episodes-dir",
            self.var_episodes.get().strip(),
            "--viral",
            self.var_viral.get().strip(),
            "--scratch",
            scratch,
        ]
        if self.var_export_indo.get():
            cmd.extend(
                [
                    "--indo-dir",
                    self.var_indo.get().strip(),
                    "--output",
                    self.var_output.get().strip(),
                ]
            )
            if self.var_force_low_ncc.get():
                cmd.append("--force-low-ncc")
        if self.var_stream_copy.get():
            cmd.append("--stream-copy")
        if self.var_reencode_concat.get():
            cmd.append("--reencode-concat")
        if self.var_no_refine.get():
            cmd.append("--no-refine-lag")

        self._append_log("\n$ " + " ".join(cmd) + "\n")

        def worker() -> None:
            try:
                env = dict(**__import__("os").environ)
                env.setdefault("PYTHONUTF8", "1")
                p = subprocess.Popen(
                    cmd,
                    cwd=str(TOOL_ROOT),
                    stdout=subprocess.PIPE,
                    stderr=subprocess.STDOUT,
                    text=True,
                    bufsize=1,
                    env=env,
                )
                assert p.stdout is not None
                for line in p.stdout:
                    self._log_queue.put(line)
                p.wait()
                self._log_queue.put(f"\n[退出码 {p.returncode}]\n")
            except Exception as e:
                self._log_queue.put(f"\n[异常] {e}\n")
            finally:
                self._log_queue.put(None)

        threading.Thread(target=worker, daemon=True).start()

    def _run_scene_only(self) -> None:
        v = self.var_scene_viral.get().strip() or self.var_viral.get().strip()
        if not v or not Path(v).is_file():
            self._messagebox.showerror("校验", "请先选择镜头检测用的爆款 mp4。")
            return
        sd = self.var_scene_detect_dir.get().strip()
        if not sd:
            self._messagebox.showerror("校验", "请填写「镜头检测输出目录」。")
            return
        scratch_dir = Path(sd)
        scratch_dir.mkdir(parents=True, exist_ok=True)
        cmd = [
            sys.executable,
            str(VIRAL_CLONE / "scene_detect_viral.py"),
            "--video",
            v,
            "--scratch",
            str(scratch_dir),
        ]
        self._append_log("\n$ " + " ".join(cmd) + "\n")

        def worker() -> None:
            try:
                p = subprocess.Popen(
                    cmd,
                    cwd=str(TOOL_ROOT),
                    stdout=subprocess.PIPE,
                    stderr=subprocess.STDOUT,
                    text=True,
                    bufsize=1,
                )
                assert p.stdout is not None
                for line in p.stdout:
                    self._log_queue.put(line)
                p.wait()
                self._log_queue.put(f"\n[退出码 {p.returncode}]\n")
            except Exception as e:
                self._log_queue.put(f"\n[异常] {e}\n")
            finally:
                self._log_queue.put(None)

        self.btn_run.config(state=self._tk.DISABLED)
        threading.Thread(target=worker, daemon=True).start()

    def _run_indo_scenes(self) -> None:
        rpt = self.var_report.get().strip()
        scn = self.var_scenes_json.get().strip()
        indo = self.var_indo2.get().strip()
        out = self.var_out2.get().strip()
        if not Path(rpt).is_file() or not Path(scn).is_file():
            self._messagebox.showerror("校验", "请选择有效的 match_report.json 与 scenes.json。")
            return
        if not indo or not Path(indo).is_dir():
            self._messagebox.showerror("校验", "请选择印尼原剧目录。")
            return
        if not out:
            self._messagebox.showerror("校验", "请填写输出路径。")
            return
        cmd = [
            sys.executable,
            str(VIRAL_CLONE / "indo_from_scenes.py"),
            "--match-report",
            rpt,
            "--scenes",
            scn,
            "--indo-dir",
            indo,
            "--output",
            out,
            "--scratch",
            self.var_scratch2.get().strip(),
        ]
        if self.var_sc_stream.get():
            cmd.append("--stream-copy")
        if self.var_sc_reenc.get():
            cmd.append("--reencode-concat")
        self._append_log("\n$ " + " ".join(cmd) + "\n")
        self.btn_run.config(state=self._tk.DISABLED)

        def worker() -> None:
            try:
                p = subprocess.Popen(
                    cmd,
                    cwd=str(TOOL_ROOT),
                    stdout=subprocess.PIPE,
                    stderr=subprocess.STDOUT,
                    text=True,
                    bufsize=1,
                )
                assert p.stdout is not None
                for line in p.stdout:
                    self._log_queue.put(line)
                p.wait()
                self._log_queue.put(f"\n[退出码 {p.returncode}]\n")
            except Exception as e:
                self._log_queue.put(f"\n[异常] {e}\n")
            finally:
                self._log_queue.put(None)

        threading.Thread(target=worker, daemon=True).start()

    def run(self) -> None:
        self.root.mainloop()


def main() -> None:
    if str(TOOL_ROOT) not in sys.path:
        sys.path.insert(0, str(TOOL_ROOT))
    app = ViralCloneApp()
    app.run()


if __name__ == "__main__":
    main()
