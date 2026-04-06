#!/usr/bin/env python3
"""
爆款结构复制 · PyQt6 桌面界面（与 AI_Drama_Translator 同技术栈，不依赖 tkinter/Tcl）。

  cd 素材制作工具 && source venv/bin/activate && pip install -r viral_clone/requirements.txt
  python viral_clone/gui_qt.py
"""
from __future__ import annotations

import re
import sys
from pathlib import Path
from typing import List, Optional

from PyQt6.QtCore import QProcess, QProcessEnvironment, Qt, QUrl
from PyQt6.QtGui import QColor, QDesktopServices, QPalette
from PyQt6.QtWidgets import (
    QApplication,
    QCheckBox,
    QFileDialog,
    QDoubleSpinBox,
    QGroupBox,
    QHBoxLayout,
    QLabel,
    QLineEdit,
    QListWidget,
    QListWidgetItem,
    QMainWindow,
    QMessageBox,
    QPushButton,
    QPlainTextEdit,
    QTabWidget,
    QVBoxLayout,
    QWidget,
)

TOOL_ROOT = Path(__file__).resolve().parent.parent
VIRAL_CLONE = TOOL_ROOT / "viral_clone"

APP_STYLE = """
QMainWindow { background-color: #f2f2f7; }
QTabWidget::pane { border: 1px solid #c6c6c8; background: white; border-radius: 10px; }
QTabBar::tab { background: #e5e5ea; color: #1c1c1e; padding: 10px 24px; margin-right: 4px;
  border-top-left-radius: 8px; border-top-right-radius: 8px; font-weight: 500; }
QTabBar::tab:selected { background: white; border-bottom: 3px solid #0071e3; font-weight: bold; }
QPushButton { border-radius: 8px; padding: 8px 14px; background-color: #0071e3; color: white;
  font-weight: 600; font-size: 13px; }
QPushButton:hover { background-color: #007aff; }
QPushButton:disabled { background-color: #8e8e93; }
QLineEdit { border: 1px solid #c6c6c8; border-radius: 6px; padding: 6px 8px;
  background-color: #ffffff; color: #111111; selection-background-color: #0071e3; selection-color: #ffffff; }
QPlainTextEdit { border: 1px solid #c6c6c8; border-radius: 8px; background-color: #1e1e1e; color: #e8e8e8;
  font-family: Menlo, Consolas, monospace; font-size: 12px;
  selection-background-color: #0071e3; selection-color: #ffffff; }
QListWidget { border: 1px solid #c6c6c8; border-radius: 8px;
  background-color: #ffffff; color: #111111; }
QListWidget::item { padding: 4px; color: #111111; background-color: #ffffff; }
QListWidget::item:selected { background-color: #0071e3; color: #ffffff; }
QListWidget::item:hover { background-color: #e5f0ff; color: #111111; }
QDoubleSpinBox { border: 1px solid #c6c6c8; border-radius: 6px; padding: 4px 8px;
  background-color: #ffffff; color: #111111; }
QGroupBox { font-weight: bold; border: 1px solid #d1d1d6; border-radius: 8px; margin-top: 10px;
  padding-top: 12px; background-color: #ffffff; color: #1c1c1e; }
QGroupBox::title {
  subcontrol-origin: margin;
  subcontrol-position: top left;
  padding: 2px 8px 0 8px;
  color: #1c1c1e;
}
QLabel { color: #1c1c1e; }
QCheckBox {
  color: #111111;
  spacing: 8px;
  background-color: transparent;
}
QCheckBox::indicator {
  width: 18px;
  height: 18px;
  border: 2px solid #636366;
  border-radius: 4px;
  background-color: #ffffff;
}
QCheckBox::indicator:hover { border-color: #0071e3; }
QCheckBox::indicator:checked {
  background-color: #0071e3;
  border-color: #005bb7;
  image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='14' height='14' viewBox='0 0 14 14'%3E%3Cpath d='M3 7l3 3 5-6' stroke='white' stroke-width='2' fill='none' stroke-linecap='round' stroke-linejoin='round'/%3E%3C/svg%3E");
}
"""


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


def _path_row(label: str, line: QLineEdit, browse: QPushButton) -> QWidget:
    w = QWidget()
    h = QHBoxLayout(w)
    h.setContentsMargins(0, 0, 0, 0)
    lb = QLabel(label)
    lb.setMinimumWidth(160)
    h.addWidget(lb)
    h.addWidget(line, 1)
    h.addWidget(browse)
    return w


class ViralCloneQtWindow(QMainWindow):
    def __init__(self) -> None:
        super().__init__()
        self.setWindowTitle("爆款复制 · PyQt6")
        self.resize(960, 720)
        self._proc: Optional[QProcess] = None

        tabs = QTabWidget()
        self.setCentralWidget(tabs)

        self._tab_main = QWidget()
        self._tab_scenes = QWidget()
        tabs.addTab(self._tab_main, "主流程：匹配 + 导出")
        tabs.addTab(self._tab_scenes, "按镜头导出（进阶）")

        self._build_main()
        self._build_scenes()
        self._apply_forced_palettes()

    def _apply_forced_palettes(self) -> None:
        """macOS 深色系统下 Fusion+样式表仍可能把控件文字打成浅色，强制对比度。"""
        light = QPalette()
        light.setColor(QPalette.ColorRole.Window, QColor("#ffffff"))
        light.setColor(QPalette.ColorRole.Base, QColor("#ffffff"))
        light.setColor(QPalette.ColorRole.Text, QColor("#111111"))
        light.setColor(QPalette.ColorRole.WindowText, QColor("#111111"))
        light.setColor(QPalette.ColorRole.Button, QColor("#f2f2f7"))
        light.setColor(QPalette.ColorRole.ButtonText, QColor("#111111"))
        for w in (
            self.list_viral,
            self.le_episodes,
            self.le_indo,
            self.le_viral,
            self.le_scratch,
            self.le_output,
            self.le_report,
            self.le_scene_dir,
            self.le_scenes_json,
            self.le_indo2,
            self.le_out2,
            self.le_scratch2,
            self.le_scene_viral,
            self.sp_cut_margin,
        ):
            w.setPalette(light)
            w.setAutoFillBackground(True)
        for c in (
            self.cb_export_indo,
            self.cb_stream,
            self.cb_reencode_concat,
            self.cb_no_refine,
            self.cb_sc_stream,
            self.cb_sc_reenc,
        ):
            c.setPalette(light)
            c.setAutoFillBackground(False)
        log_p = QPalette()
        log_p.setColor(QPalette.ColorRole.Window, QColor("#1e1e1e"))
        log_p.setColor(QPalette.ColorRole.Base, QColor("#1e1e1e"))
        log_p.setColor(QPalette.ColorRole.Text, QColor("#e8e8e8"))
        self.te_log.setPalette(log_p)
        self.te_log.setAutoFillBackground(True)

    def _build_main(self) -> None:
        root = QVBoxLayout(self._tab_main)

        root.addWidget(
            QLabel("用于对齐时间轴的原剧目录（与爆款同片、同音轨；一般为中文版）")
        )
        self.le_episodes = QLineEdit()
        b_ep = QPushButton("浏览…")
        b_ep.clicked.connect(self._browse_episodes)
        root.addWidget(_path_row("原剧目录", self.le_episodes, b_ep))

        root.addWidget(QLabel("印尼版原剧目录（导出成片时从对应集同时间码裁切）"))
        self.le_indo = QLineEdit()
        b_in = QPushButton("浏览…")
        b_in.clicked.connect(self._browse_indo)
        root.addWidget(_path_row("印尼目录", self.le_indo, b_in))

        root.addWidget(QLabel("爆款素材（可选文件夹导入，列表中选一个 mp4）"))
        hv = QHBoxLayout()
        self.le_viral = QLineEdit()
        b_vf = QPushButton("选文件")
        b_vf.clicked.connect(self._browse_viral_file)
        b_vd = QPushButton("导入文件夹")
        b_vd.clicked.connect(self._browse_viral_folder)
        hv.addWidget(self.le_viral, 1)
        hv.addWidget(b_vf)
        hv.addWidget(b_vd)
        root.addLayout(hv)

        self.list_viral = QListWidget()
        self.list_viral.setMaximumHeight(140)
        self.list_viral.currentItemChanged.connect(self._on_viral_list_change)
        root.addWidget(self.list_viral)

        self.le_scratch = QLineEdit(str(TOOL_ROOT / "viral_clone" / "_scratch_gui"))
        b_sc = QPushButton("浏览…")
        b_sc.clicked.connect(self._browse_scratch)
        root.addWidget(_path_row("缓存 scratch", self.le_scratch, b_sc))

        self.le_output = QLineEdit()
        b_out = QPushButton("浏览…")
        b_out.clicked.connect(self._browse_output)
        root.addWidget(_path_row("印尼输出 .mp4", self.le_output, b_out))

        opt = QGroupBox("选项")
        fl = QVBoxLayout(opt)
        self.cb_export_indo = QCheckBox("导出印尼成片（取消则只生成 match_report 与缓存）")
        self.cb_export_indo.setChecked(True)
        self.cb_stream = QCheckBox("分段裁切使用流复制（快，入点可能卡在关键帧）")
        self.cb_reencode_concat = QCheckBox("最终合并整轨重编码（concat 异常时勾选）")
        self.cb_no_refine = QCheckBox("关闭起点细搜")
        self.cb_force_low_ncc = QCheckBox(
            "NCC 低于默认阈值仍导出印尼成片（不推荐；仅调试或自愿承担错片）"
        )
        for c in (
            self.cb_export_indo,
            self.cb_stream,
            self.cb_reencode_concat,
            self.cb_no_refine,
            self.cb_force_low_ncc,
        ):
            fl.addWidget(c)
        row_margin = QHBoxLayout()
        _lbl_margin = QLabel(
            "裁切 seek 回退(秒)：0 = 天马1_v2 默认（单次 -ss）；3 = 混合 seek"
        )
        _lbl_margin.setWordWrap(True)
        row_margin.addWidget(_lbl_margin, 1)
        self.sp_cut_margin = QDoubleSpinBox()
        self.sp_cut_margin.setRange(0.0, 20.0)
        self.sp_cut_margin.setSingleStep(0.5)
        self.sp_cut_margin.setValue(0.0)
        self.sp_cut_margin.setDecimals(1)
        self.sp_cut_margin.setToolTip(
            "0：仅 ffmpeg -ss 在 -i 前（与已验证成片路径一致）；"
            "3：先入点前几秒再精切，部分素材更贴关键帧。"
        )
        row_margin.addWidget(self.sp_cut_margin)
        row_margin.addStretch()
        fl.addLayout(row_margin)
        root.addWidget(opt)

        bh = QHBoxLayout()
        self.btn_run = QPushButton("开始运行")
        self.btn_run.clicked.connect(self._run_match)
        btn_open = QPushButton("打开缓存目录")
        btn_open.clicked.connect(self._open_scratch)
        btn_clear = QPushButton("清空日志")
        self.te_log = QPlainTextEdit()
        self.te_log.setReadOnly(True)
        btn_clear.clicked.connect(self.te_log.clear)
        bh.addWidget(self.btn_run)
        bh.addWidget(btn_open)
        bh.addWidget(btn_clear)
        bh.addStretch()
        root.addLayout(bh)

        root.addWidget(self.te_log, 1)

    def _build_scenes(self) -> None:
        root = QVBoxLayout(self._tab_scenes)
        root.addWidget(
            QLabel(
                "需先完成主流程生成 match_report.json（含 episode_timeline）。"
                "镜头检测会把 JSON 写到「镜头检测输出目录」；按镜头导出时再选择该 JSON。"
            )
        )
        self.le_report = QLineEdit(
            str(TOOL_ROOT / "viral_clone" / "_scratch_gui" / "match_report.json")
        )
        b_r = QPushButton("浏览…")
        b_r.clicked.connect(self._browse_report)
        root.addWidget(_path_row("match_report.json", self.le_report, b_r))

        self.le_scene_dir = QLineEdit(str(TOOL_ROOT / "viral_clone" / "_scenes"))
        b_sd = QPushButton("浏览…")
        b_sd.clicked.connect(self._browse_scene_dir)
        root.addWidget(_path_row("镜头检测输出目录", self.le_scene_dir, b_sd))

        self.le_scenes_json = QLineEdit()
        b_sj = QPushButton("浏览…")
        b_sj.clicked.connect(self._browse_scenes_json)
        root.addWidget(_path_row("导引用 · 镜头表 JSON", self.le_scenes_json, b_sj))

        self.le_indo2 = QLineEdit()
        b_i2 = QPushButton("浏览…")
        b_i2.clicked.connect(self._browse_indo2)
        root.addWidget(_path_row("印尼原剧目录", self.le_indo2, b_i2))

        self.le_out2 = QLineEdit()
        b_o2 = QPushButton("浏览…")
        b_o2.clicked.connect(self._browse_out2)
        root.addWidget(_path_row("按镜头 · 输出 mp4", self.le_out2, b_o2))

        self.le_scratch2 = QLineEdit(str(TOOL_ROOT / "viral_clone" / "_scratch_indo_scenes"))
        b_s2 = QPushButton("浏览…")
        b_s2.clicked.connect(self._browse_scratch2)
        root.addWidget(_path_row("片段缓存 scratch", self.le_scratch2, b_s2))

        h_opt = QHBoxLayout()
        self.cb_sc_stream = QCheckBox("分段流复制")
        self.cb_sc_reenc = QCheckBox("合并整轨重编码")
        h_opt.addWidget(self.cb_sc_stream)
        h_opt.addWidget(self.cb_sc_reenc)
        h_opt.addStretch()
        root.addLayout(h_opt)

        h_btn = QHBoxLayout()
        self.btn_scene_only = QPushButton("仅运行镜头检测")
        self.btn_scene_only.clicked.connect(self._run_scene_only)
        self.btn_indo_scenes = QPushButton("按镜头导出印尼成片")
        self.btn_indo_scenes.clicked.connect(self._run_indo_scenes)
        h_btn.addWidget(self.btn_scene_only)
        h_btn.addWidget(self.btn_indo_scenes)
        root.addLayout(h_btn)

        hv = QHBoxLayout()
        hv.addWidget(QLabel("镜头检测用爆款 mp4"))
        self.le_scene_viral = QLineEdit()
        b_sv = QPushButton("选文件")
        b_sv.clicked.connect(self._browse_scene_viral)
        hv.addWidget(self.le_scene_viral, 1)
        hv.addWidget(b_sv)
        root.addLayout(hv)

    def _log(self, s: str) -> None:
        self.te_log.appendPlainText(s) if hasattr(self.te_log, "appendPlainText") else None
        self.te_log.append(s.rstrip("\n"))

    def _browse_episodes(self) -> None:
        d = QFileDialog.getExistingDirectory(self, "选择原剧目录（建议中文）")
        if d:
            self.le_episodes.setText(d)

    def _browse_indo(self) -> None:
        d = QFileDialog.getExistingDirectory(self, "选择印尼原剧目录")
        if d:
            self.le_indo.setText(d)

    def _browse_scratch(self) -> None:
        d = QFileDialog.getExistingDirectory(self, "选择 scratch 目录")
        if d:
            self.le_scratch.setText(d)

    def _browse_output(self) -> None:
        p, _ = QFileDialog.getSaveFileName(
            self, "印尼成片保存为", "", "MP4 (*.mp4);;All (*.*)"
        )
        if p:
            self.le_output.setText(p)

    def _browse_viral_file(self) -> None:
        p, _ = QFileDialog.getOpenFileName(
            self, "选择爆款", "", "Video (*.mp4 *.mov *.mkv);;All (*.*)"
        )
        if p:
            self.le_viral.setText(p)
            self.le_scene_viral.setText(p)
            self._sync_output_default(Path(p))

    def _browse_viral_folder(self) -> None:
        d = QFileDialog.getExistingDirectory(self, "选择爆款素材文件夹")
        if not d:
            return
        paths = _list_mp4_in_dir(Path(d))
        self.list_viral.clear()
        for x in paths:
            self.list_viral.addItem(QListWidgetItem(str(x)))
        if paths:
            self.list_viral.setCurrentRow(0)
            self.le_viral.setText(str(paths[0]))
            self.le_scene_viral.setText(str(paths[0]))
            self._sync_output_default(paths[0])

    def _on_viral_list_change(
        self, cur: Optional[QListWidgetItem], _prev: Optional[QListWidgetItem]
    ) -> None:
        if not cur:
            return
        p = cur.text()
        self.le_viral.setText(p)
        self.le_scene_viral.setText(p)
        self._sync_output_default(Path(p))

    def _sync_output_default(self, viral: Path) -> None:
        if self.le_output.text().strip():
            return
        scratch = Path(self.le_scratch.text().strip())
        try:
            scratch.mkdir(parents=True, exist_ok=True)
        except OSError:
            scratch = TOOL_ROOT / "viral_clone" / "_scratch_gui"
        self.le_output.setText(str(scratch / f"{viral.stem}_indo.mp4"))

    def _browse_report(self) -> None:
        p, _ = QFileDialog.getOpenFileName(self, "match_report.json", "", "JSON (*.json)")
        if p:
            self.le_report.setText(p)

    def _browse_scene_dir(self) -> None:
        d = QFileDialog.getExistingDirectory(self, "镜头检测 JSON 输出目录")
        if d:
            self.le_scene_dir.setText(d)

    def _browse_scenes_json(self) -> None:
        p, _ = QFileDialog.getOpenFileName(
            self, "镜头表 JSON", "", "JSON (*.json);;All (*.*)"
        )
        if p:
            self.le_scenes_json.setText(p)

    def _browse_indo2(self) -> None:
        d = QFileDialog.getExistingDirectory(self, "印尼原剧目录")
        if d:
            self.le_indo2.setText(d)

    def _browse_out2(self) -> None:
        p, _ = QFileDialog.getSaveFileName(
            self, "按镜头导出保存为", "", "MP4 (*.mp4);;All (*.*)"
        )
        if p:
            self.le_out2.setText(p)

    def _browse_scratch2(self) -> None:
        d = QFileDialog.getExistingDirectory(self, "片段缓存目录")
        if d:
            self.le_scratch2.setText(d)

    def _browse_scene_viral(self) -> None:
        p, _ = QFileDialog.getOpenFileName(
            self, "镜头检测用爆款", "", "Video (*.mp4 *.mov *.mkv);;All (*.*)"
        )
        if p:
            self.le_scene_viral.setText(p)

    def _open_scratch(self) -> None:
        p = Path(self.le_scratch.text().strip())
        p.mkdir(parents=True, exist_ok=True)
        QDesktopServices.openUrl(QUrl.fromLocalFile(str(p.resolve())))

    def _set_busy(self, busy: bool) -> None:
        self.btn_run.setEnabled(not busy)
        self.btn_scene_only.setEnabled(not busy)
        self.btn_indo_scenes.setEnabled(not busy)

    def _start_process(self, args: List[str]) -> None:
        if self._proc is not None and self._proc.state() != QProcess.ProcessState.NotRunning:
            QMessageBox.warning(self, "忙", "已有任务在运行，请等待结束。")
            return
        self._set_busy(True)
        self._proc = QProcess(self)
        self._proc.setProcessChannelMode(QProcess.ProcessChannelMode.MergedChannels)
        self._proc.setWorkingDirectory(str(TOOL_ROOT))
        env = QProcessEnvironment.systemEnvironment()
        env.insert("PYTHONUTF8", "1")
        self._proc.setProcessEnvironment(env)
        self._proc.readyReadStandardOutput.connect(self._on_proc_read)
        self._proc.finished.connect(self._on_proc_finished)
        self._proc.errorOccurred.connect(self._on_proc_error)

        self.te_log.appendPlainText("\n$ " + " ".join([sys.executable] + args))
        self.te_log.verticalScrollBar().setValue(self.te_log.verticalScrollBar().maximum())

        self._proc.start(sys.executable, args)

    def _on_proc_read(self) -> None:
        if not self._proc:
            return
        data = bytes(self._proc.readAllStandardOutput()).decode("utf-8", errors="replace")
        if data:
            self.te_log.insertPlainText(data)
            self.te_log.verticalScrollBar().setValue(self.te_log.verticalScrollBar().maximum())

    def _on_proc_error(self, _err: QProcess.ProcessError) -> None:
        if self._proc:
            self.te_log.appendPlainText(
                f"\n[进程错误] {self._proc.errorString()}\n"
            )
            self.te_log.verticalScrollBar().setValue(
                self.te_log.verticalScrollBar().maximum()
            )

    def _on_proc_finished(self, _code: int, _status: QProcess.ExitStatus) -> None:
        if self._proc:
            self.te_log.appendPlainText(f"\n[退出码 {self._proc.exitCode()}]\n")
            self.te_log.verticalScrollBar().setValue(
                self.te_log.verticalScrollBar().maximum()
            )
        self._set_busy(False)
        self._proc = None

    def _validate_main(self) -> bool:
        ep = self.le_episodes.text().strip()
        vir = self.le_viral.text().strip()
        if not ep or not Path(ep).is_dir():
            QMessageBox.critical(self, "校验", "请选择有效的原剧目录。")
            return False
        if not vir or not Path(vir).is_file():
            QMessageBox.critical(self, "校验", "请选择有效的爆款视频文件。")
            return False
        if self.cb_export_indo.isChecked():
            indo = self.le_indo.text().strip()
            if not indo or not Path(indo).is_dir():
                QMessageBox.critical(self, "校验", "导出印尼成片时，请填写印尼原剧目录。")
                return False
            if not self.le_output.text().strip():
                QMessageBox.critical(self, "校验", "请填写输出 mp4 路径。")
                return False
        return True

    def _run_match(self) -> None:
        if not self._validate_main():
            return
        scratch = self.le_scratch.text().strip()
        Path(scratch).mkdir(parents=True, exist_ok=True)
        args: List[str] = [
            str(VIRAL_CLONE / "match_viral_timeline.py"),
            "--episodes-dir",
            self.le_episodes.text().strip(),
            "--viral",
            self.le_viral.text().strip(),
            "--scratch",
            scratch,
        ]
        if self.cb_export_indo.isChecked():
            args.extend(
                [
                    "--indo-dir",
                    self.le_indo.text().strip(),
                    "--output",
                    self.le_output.text().strip(),
                ]
            )
            if self.cb_force_low_ncc.isChecked():
                args.append("--force-low-ncc")
        if self.cb_stream.isChecked():
            args.append("--stream-copy")
        if self.cb_reencode_concat.isChecked():
            args.append("--reencode-concat")
        if self.cb_no_refine.isChecked():
            args.append("--no-refine-lag")
        args.extend(
            [
                "--cut-seek-margin",
                str(float(self.sp_cut_margin.value())),
            ]
        )
        self._start_process(args)

    def _run_scene_only(self) -> None:
        v = self.le_scene_viral.text().strip() or self.le_viral.text().strip()
        if not v or not Path(v).is_file():
            QMessageBox.critical(self, "校验", "请先选择镜头检测用的爆款 mp4。")
            return
        sd = self.le_scene_dir.text().strip()
        if not sd:
            QMessageBox.critical(self, "校验", "请填写「镜头检测输出目录」。")
            return
        Path(sd).mkdir(parents=True, exist_ok=True)
        args = [
            str(VIRAL_CLONE / "scene_detect_viral.py"),
            "--video",
            v,
            "--scratch",
            sd,
        ]
        self._start_process(args)

    def _run_indo_scenes(self) -> None:
        rpt = self.le_report.text().strip()
        scn = self.le_scenes_json.text().strip()
        indo = self.le_indo2.text().strip()
        out = self.le_out2.text().strip()
        sc2 = self.le_scratch2.text().strip()
        if not Path(rpt).is_file() or not Path(scn).is_file():
            QMessageBox.critical(self, "校验", "请选择有效的 match_report.json 与 scenes.json。")
            return
        if not indo or not Path(indo).is_dir():
            QMessageBox.critical(self, "校验", "请选择印尼原剧目录。")
            return
        if not out:
            QMessageBox.critical(self, "校验", "请填写输出路径。")
            return
        if not sc2.strip():
            QMessageBox.critical(self, "校验", "请填写片段缓存 scratch。")
            return
        args = [
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
            sc2,
        ]
        if self.cb_sc_stream.isChecked():
            args.append("--stream-copy")
        if self.cb_sc_reenc.isChecked():
            args.append("--reencode-concat")
        args.extend(
            [
                "--cut-seek-margin",
                str(float(self.sp_cut_margin.value())),
            ]
        )
        self._start_process(args)


def main() -> None:
    app = QApplication(sys.argv)
    app.setStyle("Fusion")
    app.setStyleSheet(APP_STYLE)
    w = ViralCloneQtWindow()
    w.show()
    sys.exit(app.exec())


if __name__ == "__main__":
    main()
