import os
import cv2
import time
import subprocess
import logging
from PyQt6.QtWidgets import (QMainWindow, QWidget, QVBoxLayout, QHBoxLayout, 
                             QPushButton, QListWidget, QLabel, QFileDialog, 
                             QComboBox, QProgressBar, QFrame, QSplitter, QButtonGroup, 
                             QMessageBox, QSpinBox, QDoubleSpinBox, QStackedWidget, QMenu, QGroupBox,
                             QTabWidget, QListWidgetItem, QSlider, QApplication, QDialog, QRadioButton, 
                             QDialogButtonBox, QCheckBox, QLineEdit, QTextEdit, QSizePolicy, QScrollArea)
from PyQt6.QtCore import Qt, QSize, QRect, pyqtSignal, QObject, QTimer, QUrl
from PyQt6.QtGui import QPixmap, QImage, QFont, QColor, QPalette, QKeySequence, QShortcut
from PyQt6.QtMultimedia import QMediaPlayer, QAudioOutput
from ui.components.video_preview import VideoPreview
from ui.components.subtitle_editor import SubtitleEditor
from core.workflow import TranslationWorkflow

logger = logging.getLogger("AI_Drama")

# 升级版：深灰色调 + 饱和布局样式
MODERN_STYLE = """
QMainWindow { background-color: #f2f2f7; }
QTabWidget::pane { border: 1px solid #c6c6c8; background: white; border-radius: 12px; }
QTabBar::tab { background: #e5e5ea; color: #1c1c1e; padding: 12px 35px; margin-right: 4px; border-top-left-radius: 10px; border-top-right-radius: 10px; font-weight: 500; }
QTabBar::tab:selected { background: white; border-bottom: 3px solid #0071e3; font-weight: bold; }
QPushButton { border-radius: 8px; padding: 12px; background-color: #0071e3; color: white; font-weight: bold; font-size: 14px; }
QPushButton:hover { background-color: #007aff; }
QFrame#ControlPanel { background-color: #ffffff; border: 1px solid #d1d1d6; border-radius: 10px; }
QListWidget { border: 1px solid #c6c6c8; border-radius: 10px; background: white; color: #000000; font-size: 14px; }
QLabel { color: #000000; }
QGroupBox { font-weight: bold; color: #1c1c1e; border: 1px solid #d1d1d6; border-radius: 10px; margin-top: 12px; padding-top: 16px; background: #ffffff; }
/* 深色滑块样式 */
QSlider::groove:horizontal { border: 1px solid #999999; height: 6px; background: #3a3a3c; margin: 2px 0; border-radius: 3px; }
QSlider::handle:horizontal { background: #0071e3; border: 1px solid #005bb7; width: 16px; height: 16px; margin: -6px 0; border-radius: 8px; }
/* 并发数和码率输入框：极致清晰 + 精雕三角框版 */
QSpinBox, QDoubleSpinBox { 
    background: white; 
    color: black; 
    border: 2px solid #8e8e93; 
    border-radius: 6px; 
    font-size: 15px; 
    font-weight: bold; 
    padding: 2px; 
    selection-background-color: #0071e3;
}
QSpinBox::up-button, QDoubleSpinBox::up-button { 
    background: #d1d1d6; 
    border-left: 2px solid #8e8e93; 
    width: 28px; 
    border-top-right-radius: 4px; 
}
QSpinBox::down-button, QDoubleSpinBox::down-button { 
    background: #d1d1d6; 
    border-left: 2px solid #8e8e93; 
    width: 28px; 
    border-bottom-right-radius: 4px; 
    border-top: 1px solid #8e8e93; /* 按钮间物理分隔线 */
}
/* 终极稳健版：强制使用 SVG 路径确保三角形绝对清晰 */
QAbstractSpinBox::up-arrow {
    image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath d='M6 2L1 10h10z' fill='black'/%3E%3C/svg%3E");
    width: 14px; height: 14px;
}
QAbstractSpinBox::down-arrow {
    image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath d='M1 2l10 0-5 8z' fill='black'/%3E%3C/svg%3E");
    width: 14px; height: 14px;
}
QSpinBox::up-button, QDoubleSpinBox::up-button, QSpinBox::down-button, QDoubleSpinBox::down-button {
    background: #d1d1d6;
    border-left: 1px solid #8e8e93;
    width: 30px; /* 略微拓宽按钮 */
}
QSpinBox::up-button { border-top-right-radius: 6px; }
QSpinBox::down-button { border-bottom-right-radius: 6px; border-top: 1px solid #8e8e93; }

/* 悬停与点击的颜色反馈 */
QSpinBox::up-button:hover, QDoubleSpinBox::up-button:hover, QSpinBox::down-button:hover, QDoubleSpinBox::down-button:hover {
    background: #aeaeb2;
}
QSpinBox::up-button:pressed, QDoubleSpinBox::up-button:pressed, QSpinBox::down-button:pressed, QDoubleSpinBox::down-button:pressed {
    background: #0071e3;
}
/* 点击时切换为白色三角形，确保对比度 */
QSpinBox::up-button:pressed QAbstractSpinBox::up-arrow {
    image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath d='M6 2L1 10h10z' fill='white'/%3E%3C/svg%3E");
}
QSpinBox::down-button:pressed QAbstractSpinBox::down-arrow {
    image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath d='M1 2l10 0-5 8z' fill='white'/%3E%3C/svg%3E");
}
/* QComboBox 下拉选择器样式优化 */
QComboBox {
    background: white;
    color: #1c1c1e;
    border: 2px solid #0071e3;
    border-radius: 8px;
    padding: 8px 12px;
    font-size: 14px;
    font-weight: 600;
    min-height: 25px;
}
QComboBox:hover {
    border: 2px solid #007aff;
    background: #f0f8ff;
}
QComboBox::drop-down {
    border: none;
    width: 30px;
}
QComboBox::down-arrow {
    image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='14' height='14' viewBox='0 0 14 14'%3E%3Cpath d='M2 5l5 5 5-5z' fill='%230071e3'/%3E%3C/svg%3E");
    width: 16px;
    height: 16px;
}
QComboBox QAbstractItemView {
    background: white;
    border: 2px solid #0071e3;
    border-radius: 8px;
    selection-background-color: #0071e3;
    selection-color: white;
    padding: 5px;
}
/* 复选框：白底上清晰可见 */
QCheckBox { color: #1c1c1e; spacing: 8px; }
QCheckBox::indicator { width: 18px; height: 18px; border: 2px solid #0071e3; border-radius: 4px; background: white; }
QCheckBox::indicator:checked { background: #0071e3; border: 2px solid #005bb7; }
QCheckBox::indicator:hover { border: 2px solid #007aff; }
"""

class TaskItemWidget(QWidget):
    pauseClicked = pyqtSignal()
    stopClicked = pyqtSignal()

    def __init__(self, task_name, task_type, parent=None):
        super().__init__(parent)
        self.setFixedHeight(55) # 压缩高度
        self.start_time = time.time()
        layout = QHBoxLayout(self)
        layout.setContentsMargins(15, 4, 15, 4) # 极窄边距
        layout.setSpacing(10)
        
        info_layout = QVBoxLayout()
        info_layout.setSpacing(2)
        self.name_label = QLabel(f"<b>{task_name}</b>")
        self.name_label.setStyleSheet("font-size: 13px; color: #000000;")
        
        row_layout = QHBoxLayout()
        self.type_label = QLabel(task_type)
        self.type_label.setStyleSheet("color: #005bb7; font-size: 10px; font-weight: bold;")
        self.timer_label = QLabel("⏱️ 00:00")
        self.timer_label.setStyleSheet("color: #1d1d1f; font-family: 'Courier New'; font-size: 10px; font-weight: bold;")
        row_layout.addWidget(self.type_label)
        row_layout.addSpacing(10)
        row_layout.addWidget(self.timer_label)
        row_layout.addStretch()
        
        info_layout.addWidget(self.name_label)
        info_layout.addLayout(row_layout)
        
        progress_container = QVBoxLayout()
        progress_container.setSpacing(2)
        self.pbar = QProgressBar()
        self.pbar.setFixedHeight(6); self.pbar.setTextVisible(False) # 精细进度条
        self.pbar.setStyleSheet("QProgressBar { background: #d1d1d6; border-radius: 3px; border: none; } QProgressBar::chunk { background: #34c759; border-radius: 3px; }")
        self.status_label = QLabel("正在初始化...")
        self.status_label.setStyleSheet("color: #48484a; font-size: 11px; font-weight: 500;")
        progress_container.addStretch(); progress_container.addWidget(self.pbar); progress_container.addWidget(self.status_label); progress_container.addStretch()
        
        self.btn_pause = QPushButton("⏸"); self.btn_pause.setFixedSize(32, 32)
        self.btn_pause.setStyleSheet("background-color: #ffffff; color: #1d1d1f; border: 1px solid #c6c6c8; font-size: 12px;")
        self.btn_pause.clicked.connect(self.on_pause_clicked)
        
        self.btn_stop = QPushButton("⏹"); self.btn_stop.setFixedSize(32, 32)
        self.btn_stop.setStyleSheet("background-color: #ffffff; color: #ff3b30; border: 1px solid #c6c6c8; font-size: 12px;")
        self.btn_stop.clicked.connect(self.stopClicked.emit)
        
        layout.addLayout(info_layout, 4); layout.addLayout(progress_container, 5); layout.addSpacing(10); layout.addWidget(self.btn_pause); layout.addWidget(self.btn_stop)
        
        self.is_paused = False
        self.is_finished = False
        self.clock_timer = QTimer(self)
        self.clock_timer.timeout.connect(self.update_clock)
        self.clock_timer.start(1000)

    def update_clock(self):
        if not self.is_paused and not self.is_finished:
            elapsed = int(time.time() - self.start_time)
            mins = elapsed // 60
            secs = elapsed % 60
            self.timer_label.setText(f"⏱️ {mins:02d}:{secs:02d}")

    def on_task_finished(self):
        self.is_finished = True
        self.clock_timer.stop()
        self.timer_label.setStyleSheet("color: #8e8e93; font-weight: normal;")

    def on_pause_clicked(self):
        self.is_paused = not self.is_paused
        self.btn_pause.setText("▶" if self.is_paused else "⏸")
        self.pauseClicked.emit()

class MainWindow(QMainWindow):
    def __init__(self):
        super().__init__()
        logger.info("初始化主窗口...")
        try:
            self.setWindowTitle("AI 短剧生产中心 - M4 Pro Edition")
            self.setMinimumSize(QSize(1400, 950))
            self.setStyleSheet(MODERN_STYLE)
            self.base_path = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
            self.temp_folder = os.path.join(self.base_path, "temp")
            os.makedirs(self.temp_folder, exist_ok=True)
            self.running_workflows = {}
            self.tab_widgets = {} 
            self.video_srt_map = {} 
            
            self.player = QMediaPlayer()
            self.audio_output = QAudioOutput()
            self.player.setAudioOutput(self.audio_output)
            self.audio_output.setVolume(0.5) 
            
            self.play_timer = QTimer()
            self.play_timer.timeout.connect(self.on_play_timeout)
            self.current_playing_index = None
            self.current_cap = None
            self.current_cap_path = None

            self.init_ui()
            logger.info("主窗口初始化完成")
        except Exception as e:
            logger.error(f"MainWindow 初始化失败: {e}", exc_info=True)
            raise e

    def on_play_timeout(self):
        if self.current_playing_index is not None:
            w = self.tab_widgets[self.current_playing_index]
            pos_ms = self.player.position()
            fps = w["preview"].property("fps") or 25.0
            frame_idx = int((pos_ms / 1000.0) * fps)
            if abs(w["slider"].value() - frame_idx) > 1:
                w["slider"].blockSignals(True)
                w["slider"].setValue(frame_idx)
                w["slider"].blockSignals(False)
                self.update_frame_by_index(self.current_playing_index, frame_idx)

    def stop_playback(self):
        self.play_timer.stop()
        self.player.stop()
        if self.current_playing_index is not None:
            w = self.tab_widgets[self.current_playing_index]
            if "btn_play" in w: w["btn_play"].setText("▶")
        self.current_playing_index = None

    def toggle_playback(self, index):
        w = self.tab_widgets[index]
        if self.current_playing_index == index:
            if self.player.playbackState() == QMediaPlayer.PlaybackState.PlayingState:
                self.player.pause(); self.play_timer.stop(); w["btn_play"].setText("▶")
            else:
                self.player.play(); self.play_timer.start(33); w["btn_play"].setText("⏸")
        else:
            if self.current_playing_index is not None: self.stop_playback()
            self.current_playing_index = index
            path = w["preview"].property("current_path")
            if path:
                self.player.setSource(QUrl.fromLocalFile(path))
                fps = w["preview"].property("fps") or 25.0
                self.player.setPosition(int(w["slider"].value() / fps * 1000))
                self.player.play(); self.play_timer.start(33); w["btn_play"].setText("⏸")
        
    def init_ui(self):
        central_widget = QWidget(); self.setCentralWidget(central_widget)
        # 饱和布局：减小边距
        main_layout = QVBoxLayout(central_widget); main_layout.setSpacing(10); main_layout.setContentsMargins(12, 12, 12, 12)
        
        self.tabs = QTabWidget()
        self.task_stack = QStackedWidget()

        self.add_scale_tab(0)
        self.add_function_tab(1, "提取字幕", "ocr")
        self.add_function_tab(2, "画面擦除", "erase")
        self.add_translation_tab(3)
        self.add_burn_tab(4)
        self.add_merge_tab(5)
        main_layout.addWidget(self.tabs, 7)
        
        # 饱和布局：加高控制面板
        settings_panel = QFrame(); settings_panel.setObjectName("ControlPanel"); settings_panel.setFixedHeight(52)
        settings_layout = QHBoxLayout(settings_panel)
        lbl_engine = QLabel("性能设置"); lbl_engine.setStyleSheet("font-size: 13px; font-weight: bold; color: #1c1c1e;")
        settings_layout.addWidget(lbl_engine); settings_layout.addSpacing(25)
        
        # 字幕压制并发数（硬件加速限制）
        lbl_burn_parallel = QLabel("字幕压制并发:"); lbl_burn_parallel.setStyleSheet("color: #1c1c1e; font-size: 12px;")
        settings_layout.addWidget(lbl_burn_parallel); self.burn_parallel_spin = QSpinBox()
        self.burn_parallel_spin.setRange(1, 4); self.burn_parallel_spin.setValue(4); self.burn_parallel_spin.setFixedSize(80, 35)
        self.burn_parallel_spin.setAlignment(Qt.AlignmentFlag.AlignCenter)
        settings_layout.addWidget(self.burn_parallel_spin)
        burn_hint = QLabel("≤4"); burn_hint.setStyleSheet("color: #8e8e93; font-size: 11px;")
        settings_layout.addWidget(burn_hint)
        settings_layout.addSpacing(20)
        
        # 其他功能并发数
        lbl_other_parallel = QLabel("其他并发:"); lbl_other_parallel.setStyleSheet("color: #1c1c1e; font-size: 12px;")
        settings_layout.addWidget(lbl_other_parallel); self.parallel_spin = QSpinBox()
        self.parallel_spin.setRange(1, 50); self.parallel_spin.setValue(8); self.parallel_spin.setFixedSize(80, 35)
        self.parallel_spin.setAlignment(Qt.AlignmentFlag.AlignCenter)
        settings_layout.addWidget(self.parallel_spin)
        settings_layout.addStretch()
        main_layout.addWidget(settings_panel)
        
        task_group = QGroupBox("实时任务中心")
        task_layout = QVBoxLayout(task_group)
        task_layout.addWidget(self.task_stack)
        main_layout.addWidget(task_group, 3)
        self.tabs.currentChanged.connect(self.task_stack.setCurrentIndex)

        # 全局快捷键
        QShortcut(QKeySequence(Qt.Key.Key_Space), self, self._on_shortcut_play_pause)
        QShortcut(QKeySequence.StandardKey.SelectAll, self, self._on_shortcut_select_all)
        QShortcut(QKeySequence("Ctrl+Shift+Delete"), self, self._on_shortcut_clear)

    def _on_shortcut_play_pause(self):
        """空格：播放/暂停"""
        idx = self.tabs.currentIndex()
        if idx in self.tab_widgets and "btn_play" in self.tab_widgets[idx]:
            self.toggle_playback(idx)

    def _on_shortcut_select_all(self):
        """Ctrl/Cmd+A：全选"""
        idx = self.tabs.currentIndex()
        w = self.tab_widgets.get(idx, {})
        lst = w.get("list") or w.get("batch_list")
        if lst and lst.count() > 0:
            self.set_selection(lst, True)

    def _on_shortcut_clear(self):
        """Ctrl+Shift+Delete：清除列表"""
        idx = self.tabs.currentIndex()
        w = self.tab_widgets.get(idx, {})
        lst = w.get("list") or w.get("batch_list")
        if lst and lst.count() > 0:
            self.clear_all_items(lst, idx)

    def update_preview_font(self, index, key, value):
        w = self.tab_widgets[index]
        if "preview" in w:
            w["preview"].font_config[key] = value
            w["preview"].update()

    def show_list_menu(self, pos, list_widget):
        menu = QMenu()
        del_action = menu.addAction("❌ 删除选中项")
        clear_action = menu.addAction("🗑 清空列表")
        action = menu.exec(list_widget.mapToGlobal(pos))
        if action == del_action:
            for item in list_widget.selectedItems(): list_widget.takeItem(list_widget.row(item))
        elif action == clear_action: list_widget.clear()

    def enter_burn_editor(self, container, v_list, batch_list, index):
        batch_list.clear()
        if v_list.count() == 0:
            QMessageBox.warning(self, "提醒", "请先导入视频素材。"); return
        for i in range(v_list.count()):
            old_item = v_list.item(i); v_path = old_item.data(Qt.ItemDataRole.UserRole)
            new_item = QListWidgetItem(old_item.text()); new_item.setData(Qt.ItemDataRole.UserRole, v_path)
            batch_list.addItem(new_item); self.auto_match_srt(v_path, index, silent=True)
        batch_list.setCurrentRow(0); self.load_preview(batch_list.item(0), index); container.setCurrentIndex(1)

    def add_burn_tab(self, index):
        container = QStackedWidget()
        lv1_page = QWidget(); lv1_layout = QHBoxLayout(lv1_page)
        v_box = QVBoxLayout(); v_box.addWidget(QLabel("📺 <b>1. 导入视频素材</b>"))
        btn_v = QPushButton("➕ 添加视频"); btn_v.setStyleSheet("background-color: #1d1d1f;")
        v_list = QListWidget(); v_list.setSelectionMode(QListWidget.SelectionMode.MultiSelection)
        v_list.setContextMenuPolicy(Qt.ContextMenuPolicy.CustomContextMenu)
        v_list.customContextMenuRequested.connect(lambda pos: self.show_list_menu(pos, v_list))
        v_box.addWidget(btn_v); v_box.addWidget(v_list)
        btn_clear_v = QPushButton("🗑 清空列表"); btn_clear_v.clicked.connect(v_list.clear)
        v_box.addWidget(btn_clear_v); lv1_layout.addLayout(v_box, 1)
        
        s_box = QVBoxLayout(); s_box.addWidget(QLabel("📝 <b>2. 导入对应字幕</b>"))
        btn_s = QPushButton("➕ 添加 SRT"); btn_s.setStyleSheet("background-color: #1d1d1f;")
        s_list = QListWidget(); s_list.setSelectionMode(QListWidget.SelectionMode.MultiSelection)
        s_list.setContextMenuPolicy(Qt.ContextMenuPolicy.CustomContextMenu)
        s_list.customContextMenuRequested.connect(lambda pos: self.show_list_menu(pos, s_list))
        s_box.addWidget(btn_s); s_box.addWidget(s_list)
        btn_clear_s = QPushButton("🗑 清空列表"); btn_clear_s.clicked.connect(s_list.clear)
        s_box.addWidget(btn_clear_s); lv1_layout.addLayout(s_box, 1)
        
        c_box = QVBoxLayout(); c_box.addStretch()
        btn_next = QPushButton("下一步：\n配置样式 ➔"); btn_next.setFixedHeight(120)
        c_box.addWidget(btn_next); lv1_layout.addLayout(c_box, 0)
        
        lv2_page = QWidget(); lv2_layout = QHBoxLayout(lv2_page)
        sidebar = QTabWidget()
        lyric_page = QWidget(); lyric_layout = QVBoxLayout(lyric_page)
        lyric_list = QListWidget(); lyric_layout.addWidget(lyric_list); sidebar.addTab(lyric_page, "台词")
        style_page = QWidget(); style_layout = QVBoxLayout(style_page)
        style_layout.addWidget(QLabel("字体大小:")); font_size = QSpinBox(); font_size.setRange(10, 100); font_size.setValue(36); style_layout.addWidget(font_size)
        style_layout.addWidget(QLabel("文字颜色:")); font_color = QComboBox(); font_color.addItems(["白色", "黄色", "红色", "绿色"]); style_layout.addWidget(font_color)
        offset_row = QHBoxLayout(); offset_row.addWidget(QLabel("字幕时间偏移:"))
        burn_offset_spin = QDoubleSpinBox(); burn_offset_spin.setRange(-0.5, 0.5); burn_offset_spin.setSingleStep(0.05); burn_offset_spin.setValue(0)
        burn_offset_spin.setDecimals(2); burn_offset_spin.setToolTip("负值=提前，正值=延后")
        burn_offset_spin.setAlignment(Qt.AlignmentFlag.AlignCenter); burn_offset_spin.setFixedSize(80, 28)
        offset_row.addWidget(burn_offset_spin); offset_row.addStretch(); style_layout.addLayout(offset_row)
        style_layout.addStretch(); sidebar.addTab(style_page, "样式")
        font_size.valueChanged.connect(lambda v: self.update_preview_font(index, "size", v))
        font_color.currentTextChanged.connect(lambda v: self.update_preview_font(index, "color", v))
        burn_offset_spin.valueChanged.connect(lambda: self.on_subtitle_offset_changed(index))
        lv2_layout.addWidget(sidebar, 1)
        
        mid_panel = QVBoxLayout()
        mid_panel.setContentsMargins(5, 5, 5, 5)  # 小边距
        preview = VideoPreview(color=QColor(0, 113, 227))
        preview.set_mode("burn"); preview.setMinimumSize(300, 500)
        preview.font_config["size"] = 36
        preview.setSizePolicy(QSizePolicy.Policy.Expanding, QSizePolicy.Policy.Expanding)
        mid_panel.addWidget(preview)  # 移除居中对齐，让它自然扩展
        slider_layout = QHBoxLayout(); btn_play = QPushButton("▶"); btn_play.setFixedSize(40, 40)
        time_label = QLabel("00:00 / 00:00"); slider = QSlider(Qt.Orientation.Horizontal)
        volume_layout = QHBoxLayout(); volume_layout.setSpacing(0); volume_layout.setContentsMargins(0, 0, 0, 0)
        volume_slider = QSlider(Qt.Orientation.Horizontal)
        volume_slider.setRange(0, 100); volume_slider.setValue(50); volume_slider.setFixedWidth(80)
        volume_slider.valueChanged.connect(lambda v: self.audio_output.setVolume(v / 100.0))
        lbl_spk = QLabel("🔊"); lbl_spk.setFixedWidth(20)
        volume_layout.addWidget(lbl_spk); volume_layout.addWidget(volume_slider)
        slider_layout.addWidget(btn_play); slider_layout.addWidget(slider); slider_layout.addWidget(time_label); slider_layout.addStretch(); slider_layout.addLayout(volume_layout)
        mid_panel.addLayout(slider_layout); lv2_layout.addLayout(mid_panel, 2)
        
        right_panel = QVBoxLayout(); right_panel.addWidget(QLabel("📋 <b>本批次文件</b>"))
        batch_list = QListWidget(); right_panel.addWidget(batch_list)
        btn_back = QPushButton("⬅ 返回修改素材"); btn_go = QPushButton("🚀 确认并启动压制"); btn_go.setFixedHeight(60); btn_go.setStyleSheet("background-color: #34c759;")
        right_panel.addWidget(btn_back); right_panel.addWidget(btn_go); lv2_layout.addLayout(right_panel, 1)
        
        btn_next.clicked.connect(lambda: self.enter_burn_editor(container, v_list, batch_list, index))
        btn_back.clicked.connect(lambda: container.setCurrentIndex(0)); btn_play.clicked.connect(lambda: self.toggle_playback(index))
        local_task_list = QListWidget(); local_task_list.setStyleSheet("border: none;")
        self.task_stack.addWidget(local_task_list)
        
        self.tab_widgets[index] = {"preview": preview, "slider": slider, "time": time_label, "btn_play": btn_play, "list": v_list, "v_list": v_list, "s_list": s_list, "batch_list": batch_list, "task_list": local_task_list, "lyric_list": lyric_list, "bitrate": None, "subtitle_offset_spin": burn_offset_spin}
        container.addWidget(lv1_page); container.addWidget(lv2_page); self.tabs.addTab(container, "字幕压制")
        btn_v.clicked.connect(lambda: self.import_videos(v_list)); btn_s.clicked.connect(lambda: self.import_srts(s_list))
        v_list.itemClicked.connect(lambda item: self.load_preview(item, index)); batch_list.itemClicked.connect(lambda item: self.load_preview(item, index))
        slider.valueChanged.connect(lambda val: self.seek_preview(val, index)); btn_go.clicked.connect(lambda: self.dispatch_batch_tasks("字幕压制", index))
        lyric_list.itemClicked.connect(lambda item: self.jump_to_lyric(item, index))

    def add_merge_tab(self, index):
        """剧集合并功能标签页 - 智能无感去尾合并器"""
        page = QWidget()
        layout = QHBoxLayout(page)
        layout.setContentsMargins(20, 20, 20, 20)
        layout.setSpacing(24)
        
        # === 左侧：配置区域（可滚动）===
        left_scroll = QScrollArea()
        left_scroll.setWidgetResizable(True)
        left_scroll.setFrameShape(QFrame.Shape.NoFrame)
        left_scroll.setHorizontalScrollBarPolicy(Qt.ScrollBarPolicy.ScrollBarAlwaysOff)
        left_scroll.setMinimumWidth(320)
        left_scroll.setMaximumWidth(400)
        
        left_container = QWidget()
        left_panel = QVBoxLayout(left_container)
        left_panel.setSpacing(16)
        left_panel.setContentsMargins(8, 8, 8, 8)
        
        # 1. 输入输出文件夹
        io_group = QGroupBox("📂 输入输出")
        io_group.setStyleSheet("QGroupBox { color: #1c1c1e; } QGroupBox QLabel { color: #1c1c1e; font-size: 13px; }")
        io_layout = QVBoxLayout(io_group)
        io_layout.setSpacing(10)
        
        input_row = QHBoxLayout()
        input_row.addWidget(QLabel("输入:"))
        self.merge_input_edit = QLineEdit()
        self.merge_input_edit.setPlaceholderText("请选择输入文件夹...")
        self.merge_input_edit.setReadOnly(True)
        input_row.addWidget(self.merge_input_edit)
        btn_select_input = QPushButton("选择")
        btn_select_input.setFixedWidth(70)
        btn_select_input.setStyleSheet("background-color: #007aff; color: white; padding: 6px;")
        btn_select_input.clicked.connect(self.select_merge_input_folder)
        input_row.addWidget(btn_select_input)
        io_layout.addLayout(input_row)
        
        output_row = QHBoxLayout()
        output_row.addWidget(QLabel("输出:"))
        self.merge_output_edit = QLineEdit()
        self.merge_output_edit.setPlaceholderText("请选择输出文件夹...")
        self.merge_output_edit.setReadOnly(True)
        output_row.addWidget(self.merge_output_edit)
        btn_select_output = QPushButton("选择")
        btn_select_output.setFixedWidth(70)
        btn_select_output.setStyleSheet("background-color: #007aff; color: white; padding: 6px;")
        btn_select_output.clicked.connect(self.select_merge_output_folder)
        output_row.addWidget(btn_select_output)
        io_layout.addLayout(output_row)
        
        left_panel.addWidget(io_group)
        left_panel.addSpacing(8)
        
        # 2. 裁剪模式与参数
        trim_group = QGroupBox("✂️ 裁剪设置")
        trim_group.setStyleSheet("QGroupBox { color: #1c1c1e; } QGroupBox QLabel { color: #1c1c1e; font-size: 13px; } QGroupBox QCheckBox { color: #1c1c1e; font-size: 13px; }")
        trim_layout = QVBoxLayout(trim_group)
        trim_layout.setSpacing(12)
        
        mode_row = QHBoxLayout()
        mode_row.addWidget(QLabel("裁剪模式:"))
        self.merge_trim_mode = QComboBox()
        self.merge_trim_mode.addItems(["智能亮度检测", "固定秒数裁剪"])
        self.merge_trim_mode.setCurrentIndex(1)  # 默认：固定秒数裁剪
        self.merge_trim_mode.setMinimumWidth(165)
        self.merge_trim_mode.setFixedHeight(30)
        self.merge_trim_mode.setToolTip("智能亮度检测：自动识别片尾；固定秒数裁剪：每集末尾统一切除指定秒数")
        mode_row.addWidget(self.merge_trim_mode)
        mode_row.addStretch()
        trim_layout.addLayout(mode_row)
        
        fixed_row = QHBoxLayout()
        fixed_row.addWidget(QLabel("固定裁剪(秒):"))
        self.merge_fixed_trim_seconds = QDoubleSpinBox()
        self.merge_fixed_trim_seconds.setRange(0, 60)
        self.merge_fixed_trim_seconds.setValue(2)
        self.merge_fixed_trim_seconds.setSingleStep(0.5)
        self.merge_fixed_trim_seconds.setDecimals(1)
        self.merge_fixed_trim_seconds.setFixedSize(70, 28)
        self.merge_fixed_trim_seconds.setToolTip("每集末尾切除的秒数，0表示不裁剪（仅固定秒数模式生效）")
        fixed_row.addWidget(self.merge_fixed_trim_seconds)
        fixed_row.addStretch()
        trim_layout.addLayout(fixed_row)
        
        self.merge_skip_last_episode = QCheckBox("最后一集保留原片尾不切除")
        self.merge_skip_last_episode.setChecked(True)
        self.merge_skip_last_episode.setToolTip("大结局保留完整片尾（仅固定秒数模式生效）")
        trim_layout.addWidget(self.merge_skip_last_episode)
        
        self.merge_compatible_mode = QCheckBox("高兼容合并（避免交界处卡顿）")
        self.merge_compatible_mode.setChecked(False)
        self.merge_compatible_mode.setToolTip("裁剪时重编码，确保每段以关键帧开头，合并后播放更流畅。速度较慢但可解决片段交界处卡住的问题。")
        trim_layout.addWidget(self.merge_compatible_mode)
        
        left_panel.addWidget(trim_group)
        left_panel.addSpacing(8)
        
        # 3. 智能亮度检测参数
        brightness_group = QGroupBox("🔆 智能亮度检测参数")
        brightness_group.setStyleSheet("QGroupBox { color: #1c1c1e; } QGroupBox QLabel { color: #1c1c1e; font-size: 13px; }")
        brightness_group.setToolTip("仅在选择「智能亮度检测」模式时生效")
        bright_layout = QVBoxLayout(brightness_group)
        bright_layout.setSpacing(10)
        
        def add_param_row(lay, label, spin, tooltip):
            row = QHBoxLayout()
            row.addWidget(QLabel(label))
            spin.setToolTip(tooltip)
            spin.setFixedSize(70, 28)
            row.addWidget(spin)
            row.addStretch()
            lay.addLayout(row)
        
        self.merge_tail_seconds = QSpinBox()
        self.merge_tail_seconds.setRange(1, 60)
        self.merge_tail_seconds.setValue(8)
        add_param_row(bright_layout, "检测时长(秒):", self.merge_tail_seconds, "检测视频末尾N秒，用于识别片尾")
        
        self.merge_brightness = QSpinBox()
        self.merge_brightness.setRange(100, 255)
        self.merge_brightness.setValue(245)
        add_param_row(bright_layout, "亮度阈值:", self.merge_brightness, "当亮度超过此值时判定为片尾")
        
        self.merge_delta = QSpinBox()
        self.merge_delta.setRange(1, 100)
        self.merge_delta.setValue(25)
        add_param_row(bright_layout, "亮度跳变:", self.merge_delta, "亮度变化超过此值时判定为片尾开始")
        
        restore_row = QHBoxLayout()
        restore_row.addStretch()
        btn_restore_brightness = QPushButton("恢复默认值")
        btn_restore_brightness.setFixedHeight(36)
        btn_restore_brightness.setMinimumWidth(100)
        btn_restore_brightness.setStyleSheet("""
            QPushButton {
                font-size: 14px; font-weight: 500; color: white;
                background-color: #5ac8fa; border: none; border-radius: 8px;
                padding: 8px 16px; min-height: 20px;
            }
            QPushButton:hover { background-color: #70d4ff; }
            QPushButton:pressed { background-color: #48b8e6; }
        """)
        btn_restore_brightness.setToolTip("恢复为：检测时长8秒、亮度阈值245、亮度跳变25")
        btn_restore_brightness.clicked.connect(lambda: (
            self.merge_tail_seconds.setValue(8),
            self.merge_brightness.setValue(245),
            self.merge_delta.setValue(25)
        ))
        restore_row.addWidget(btn_restore_brightness)
        bright_layout.addLayout(restore_row)
        
        left_panel.addWidget(brightness_group)
        left_panel.addSpacing(8)
        
        # 4. 并发与启动
        exec_group = QGroupBox("⚡ 执行设置")
        exec_group.setStyleSheet("QGroupBox { color: #1c1c1e; } QGroupBox QLabel { color: #1c1c1e; font-size: 13px; }")
        exec_layout = QVBoxLayout(exec_group)
        exec_layout.setSpacing(10)
        
        workers_row = QHBoxLayout()
        workers_row.addWidget(QLabel("并发核心数:"))
        self.merge_workers = QSpinBox()
        self.merge_workers.setRange(1, os.cpu_count() or 10)
        self.merge_workers.setValue(min(10, os.cpu_count() or 10))
        self.merge_workers.setFixedSize(70, 28)
        self.merge_workers.setToolTip(f"并行处理核心数（当前CPU: {os.cpu_count()}核）")
        workers_row.addWidget(self.merge_workers)
        workers_row.addStretch()
        exec_layout.addLayout(workers_row)
        
        btn_start_merge = QPushButton("🚀 开始智能合并")
        btn_start_merge.setFixedHeight(52)
        btn_start_merge.setStyleSheet("background-color: #34c759; color: white; font-size: 15px; font-weight: bold; border-radius: 8px;")
        btn_start_merge.clicked.connect(self.start_merge_task)
        exec_layout.addWidget(btn_start_merge)
        
        left_panel.addWidget(exec_group)
        left_panel.addStretch()
        
        left_scroll.setWidget(left_container)
        layout.addWidget(left_scroll, 1)
        
        # === 右侧：文件列表和日志区域 ===
        right_panel = QVBoxLayout()
        right_panel.setSpacing(16)
        
        files_group = QGroupBox("📄 待合并文件列表")
        files_layout = QVBoxLayout(files_group)
        
        self.merge_file_list = QListWidget()
        self.merge_file_list.setStyleSheet("""
            QListWidget {
                background: white;
                border: 2px solid #e5e5e7;
                border-radius: 8px;
                padding: 5px;
                font-size: 13px;
            }
            QListWidget::item {
                padding: 8px;
                border-bottom: 1px solid #f0f0f0;
            }
            QListWidget::item:selected {
                background-color: #007aff;
                color: white;
            }
        """)
        files_layout.addWidget(self.merge_file_list)
        right_panel.addWidget(files_group, 3)
        
        log_group = QGroupBox("📋 处理日志")
        log_layout = QVBoxLayout(log_group)
        
        self.merge_log_text = QTextEdit()
        self.merge_log_text.setReadOnly(True)
        self.merge_log_text.setStyleSheet("""
            QTextEdit {
                background: #1e1e1e;
                color: #00ff00;
                border: 2px solid #e5e5e7;
                border-radius: 8px;
                padding: 10px;
                font-family: 'Courier New', monospace;
                font-size: 12px;
            }
        """)
        self.merge_log_text.setPlaceholderText("等待开始处理...")
        log_layout.addWidget(self.merge_log_text)
        right_panel.addWidget(log_group, 2)
        
        layout.addLayout(right_panel, 3)
        
        # 添加到任务列表
        local_task_list = QListWidget()
        local_task_list.setFixedHeight(150)
        self.task_stack.addWidget(local_task_list)
        
        self.tab_widgets[index] = {
            "input_edit": self.merge_input_edit,
            "output_edit": self.merge_output_edit,
            "file_list": self.merge_file_list,
            "log_text": self.merge_log_text,
            "trim_mode": self.merge_trim_mode,
            "fixed_trim_seconds": self.merge_fixed_trim_seconds,
            "skip_last_episode": self.merge_skip_last_episode,
            "compatible_mode": self.merge_compatible_mode,
            "tail_seconds": self.merge_tail_seconds,
            "brightness": self.merge_brightness,
            "delta": self.merge_delta,
            "workers": self.merge_workers,
            "task_list": local_task_list
        }
        
        self.tabs.addTab(page, "剧集合并")

    def add_scale_tab(self, index):
        """调整分辨率功能标签页"""
        page = QWidget(); layout = QHBoxLayout(page); layout.setContentsMargins(15, 15, 15, 15); layout.setSpacing(20)
        left_panel = QVBoxLayout()
        left_panel.addWidget(QLabel("📂 待处理视频"))
        btn_import = QPushButton("➕ 导入视频"); left_panel.addWidget(btn_import)
        select_ctrl = QHBoxLayout(); btn_all = QPushButton("全选"); btn_clear = QPushButton("清除")
        select_ctrl.addWidget(btn_all); select_ctrl.addWidget(btn_clear); left_panel.addLayout(select_ctrl)
        
        # 查询与筛选按钮（统一蓝色主色，筛选为次要样式）
        query_ctrl = QHBoxLayout()
        btn_check_resolution = QPushButton("🔍 查询分辨率与帧率")
        btn_check_resolution.setStyleSheet("background-color: #0071e3; color: white; font-size: 13px; font-weight: bold;")
        btn_check_resolution.setToolTip("一次性扫描所有视频的分辨率和帧率，合并前可发现帧率不一致问题")
        btn_select_by_resolution = QPushButton("按分辨率筛选")
        btn_select_by_resolution.setStyleSheet("background-color: #f2f2f7; color: #1c1c1e; border: 1px solid #d1d1d6; font-size: 12px;")
        btn_select_by_resolution.setToolTip("筛选指定分辨率的视频")
        btn_select_by_fps = QPushButton("按帧率筛选")
        btn_select_by_fps.setStyleSheet("background-color: #f2f2f7; color: #1c1c1e; border: 1px solid #d1d1d6; font-size: 12px;")
        btn_select_by_fps.setToolTip("筛选帧率不一致的视频，便于统一转换")
        query_ctrl.addWidget(btn_check_resolution, 2)
        query_ctrl.addWidget(btn_select_by_resolution, 1)
        query_ctrl.addWidget(btn_select_by_fps, 1)
        left_panel.addLayout(query_ctrl)
        
        # 分辨率与帧率查询结果显示区
        resolution_result_box = QFrame(); resolution_result_box.setObjectName("ControlPanel")
        resolution_result_box.setFixedHeight(130)
        result_layout = QVBoxLayout(resolution_result_box)
        result_layout.setSpacing(4)
        result_title = QLabel("📊 分辨率与帧率统计"); result_title.setStyleSheet("font-size: 12px; font-weight: bold; color: #1c1c1e;")
        resolution_result_label = QLabel("请先导入视频并点击查询")
        resolution_result_label.setWordWrap(True)
        resolution_result_label.setStyleSheet("font-size: 12px; color: #48484a; padding: 4px 0; line-height: 1.4;")
        result_layout.addWidget(result_title)
        result_layout.addWidget(resolution_result_label)
        result_layout.addStretch()
        left_panel.addWidget(resolution_result_box)
        
        file_list = QListWidget(); file_list.setSelectionMode(QListWidget.SelectionMode.MultiSelection); left_panel.addWidget(file_list)
        layout.addLayout(left_panel, 1)
        
        # 核心预览区标题和文件名显示（调分辨率tab）
        preview_header_scale = QHBoxLayout()
        preview_header_scale.addWidget(QLabel("📺 <b>核心预览</b>"))
        preview_header_scale.addStretch()
        filename_label_scale = QLabel("<i style='color: #8e8e93;'>未选择文件</i>")
        filename_label_scale.setStyleSheet("font-size: 12px; padding: 2px 8px;")
        preview_header_scale.addWidget(filename_label_scale)
        mid_panel = QVBoxLayout(); mid_panel.addLayout(preview_header_scale)
        
        # 预览区（只包含VideoPreview）
        preview_container = QFrame(); preview_container.setObjectName("ControlPanel")
        preview_outer_layout = QVBoxLayout(preview_container)
        preview_outer_layout.setContentsMargins(5, 5, 5, 5)  # 小边距
        preview = VideoPreview(); preview.set_mode("scale"); preview.setMinimumSize(300, 500)
        preview.setSizePolicy(QSizePolicy.Policy.Expanding, QSizePolicy.Policy.Expanding)
        preview_outer_layout.addWidget(preview)  # 移除居中对齐，让它自然扩展
        mid_panel.addWidget(preview_container)
        
        # 播放控件（独立于预览区）
        slider_layout = QHBoxLayout(); btn_play = QPushButton("▶"); btn_play.setFixedSize(40, 40)
        time_label = QLabel("00:00 / 00:00"); slider = QSlider(Qt.Orientation.Horizontal)
        volume_layout = QHBoxLayout(); volume_layout.setSpacing(0); volume_layout.setContentsMargins(0, 0, 0, 0)
        volume_slider = QSlider(Qt.Orientation.Horizontal)
        volume_slider.setRange(0, 100); volume_slider.setValue(50); volume_slider.setFixedWidth(80)
        volume_slider.valueChanged.connect(lambda v: self.audio_output.setVolume(v / 100.0))
        lbl_spk = QLabel("🔊"); lbl_spk.setFixedWidth(20)
        volume_layout.addWidget(lbl_spk); volume_layout.addWidget(volume_slider)
        slider_layout.addWidget(btn_play); slider_layout.addWidget(slider); slider_layout.addWidget(time_label); slider_layout.addStretch(); slider_layout.addLayout(volume_layout)
        mid_panel.addLayout(slider_layout)
        
        layout.addLayout(mid_panel, 2)
        
        right_panel = QVBoxLayout()
        group = QGroupBox("输出设置"); lay = QVBoxLayout(group)
        
        # 预设分辨率选择（默认 720x1280 与快速选择一致）
        preset_layout = QHBoxLayout()
        preset_layout.addWidget(QLabel("快速选择:"))
        preset_combo = QComboBox()
        preset_combo.addItems(["720x1280 (竖屏720P)", "1080x1920 (竖屏1080P)", "1280x720 (横屏720P)", "1920x1080 (横屏1080P)", "自定义"])
        preset_combo.setCurrentIndex(0)
        preset_layout.addWidget(preset_combo)
        lay.addLayout(preset_layout)
        
        width_spin = QSpinBox(); width_spin.setRange(128, 7680); width_spin.setValue(720)
        height_spin = QSpinBox(); height_spin.setRange(128, 7680); height_spin.setValue(1280)
        
        def on_preset_changed(text):
            if "720x1280" in text: width_spin.setValue(720); height_spin.setValue(1280)
            elif "1080x1920" in text: width_spin.setValue(1080); height_spin.setValue(1920)
            elif "1280x720" in text: width_spin.setValue(1280); height_spin.setValue(720)
            elif "1920x1080" in text: width_spin.setValue(1920); height_spin.setValue(1080)
        preset_combo.currentTextChanged.connect(on_preset_changed)
        
        custom_layout = QVBoxLayout()
        for lbl, spin in [("宽度", width_spin), ("高度", height_spin)]:
            row = QHBoxLayout()
            row.addWidget(QLabel(f"{lbl}:"))
            spin.setAlignment(Qt.AlignmentFlag.AlignCenter); spin.setFixedSize(90, 32)
            row.addWidget(spin); row.addStretch()
            custom_layout.addLayout(row)
        lay.addLayout(custom_layout)
        
        bitrate_layout = QHBoxLayout()
        bitrate_layout.addWidget(QLabel("码率 (Mbps):"))
        bitrate_spin = QDoubleSpinBox(); bitrate_spin.setRange(0.5, 20.0); bitrate_spin.setValue(1.5)
        bitrate_spin.setAlignment(Qt.AlignmentFlag.AlignCenter); bitrate_spin.setFixedSize(90, 32)
        bitrate_layout.addWidget(bitrate_spin); bitrate_layout.addStretch()
        lay.addLayout(bitrate_layout)
        
        # 帧率统一（合并前推荐）
        fps_row = QHBoxLayout()
        fps_check = QCheckBox("统一帧率")
        fps_check.setToolTip("合并前推荐开启，解决帧率不一致导致的卡顿")
        fps_check.setChecked(False)
        fps_check.setStyleSheet("QCheckBox { font-weight: bold; }")
        fps_row.addWidget(fps_check)
        lbl_fps = QLabel("目标帧率:"); lbl_fps.setStyleSheet("color: #1c1c1e; font-size: 13px;")
        fps_row.addWidget(lbl_fps)
        fps_combo = QComboBox()
        fps_combo.addItems(["25 fps", "30 fps"])
        fps_combo.setFixedWidth(100)
        fps_row.addWidget(fps_combo)
        fps_row.addStretch()
        lay.addLayout(fps_row)
        
        right_panel.addWidget(group)
        
        # 简洁提示（与分组风格统一）
        hint_label = QLabel("调整分辨率会重新编码，建议码率 720P=1.5M、1080P=2.5M")
        hint_label.setStyleSheet("font-size: 11px; color: #8e8e93; padding: 8px 0;")
        hint_label.setWordWrap(True)
        right_panel.addWidget(hint_label)
        right_panel.addStretch()
        
        btn_go = QPushButton("🚀 批量启动 分辨率转换")
        btn_go.setFixedHeight(56)
        btn_go.setStyleSheet("font-size: 16px; font-weight: bold; background-color: #0071e3; color: white; border-radius: 10px;")
        right_panel.addWidget(btn_go); layout.addLayout(right_panel, 1)
        
        local_task_list = QListWidget(); local_task_list.setStyleSheet("border: none;")
        self.task_stack.addWidget(local_task_list)
        self.tab_widgets[index] = {
            "preview": preview, "slider": slider, "time": time_label, "btn_play": btn_play, 
            "list": file_list, "width": width_spin, "height": height_spin, "bitrate": bitrate_spin, 
            "task_list": local_task_list, "resolution_result": resolution_result_label,
            "file_resolution_map": {},  # 存储文件路径到分辨率的映射
            "file_fps_map": {},  # 存储文件路径到帧率的映射
            "filename_label": filename_label_scale,  # 文件名显示标签
            "fps_check": fps_check, "fps_combo": fps_combo
        }
        btn_import.clicked.connect(lambda: self.import_videos(file_list))
        btn_all.clicked.connect(lambda: self.set_selection(file_list, True))
        btn_clear.clicked.connect(lambda: self.clear_all_items(file_list, index))
        btn_check_resolution.clicked.connect(lambda: self.check_resolution_and_fps(index))
        btn_select_by_resolution.clicked.connect(lambda: self.select_by_resolution(index))
        btn_select_by_fps.clicked.connect(lambda: self.select_by_fps(index))
        file_list.itemClicked.connect(lambda item: self.load_preview(item, index))
        slider.valueChanged.connect(lambda val: self.seek_preview(val, index))
        btn_go.clicked.connect(lambda: self.dispatch_batch_tasks("调分辨率", index))
        btn_play.clicked.connect(lambda: self.toggle_playback(index))
        self.tabs.addTab(page, "调分辨率")

    def add_function_tab(self, index, name, mode):
        page = QWidget(); layout = QHBoxLayout(page); layout.setContentsMargins(15, 15, 15, 15); layout.setSpacing(20)
        left_panel = QVBoxLayout(); left_panel.addWidget(QLabel(f"📂 <b>{name} 待选列表</b>"))
        btn_import = QPushButton("➕ 导入素材"); left_panel.addWidget(btn_import)
        
        # 所有模式都显示全选和清除按钮
        select_ctrl = QHBoxLayout()
        btn_all = QPushButton("全选"); btn_clear = QPushButton("清除")
        select_ctrl.addWidget(btn_all); select_ctrl.addWidget(btn_clear)
        left_panel.addLayout(select_ctrl)
        
        # 所有模式都使用多选（批量提取字幕也需要多选）
        file_list = QListWidget()
        file_list.setSelectionMode(QListWidget.SelectionMode.MultiSelection)
        left_panel.addWidget(file_list)
        layout.addLayout(left_panel, 1)
        
        # 核心预览区标题和文件名显示
        preview_header = QHBoxLayout()
        preview_header.addWidget(QLabel("📺 <b>核心预览</b>"))
        preview_header.addStretch()
        filename_label = QLabel("<i style='color: #8e8e93;'>未选择文件</i>")
        filename_label.setStyleSheet("font-size: 12px; padding: 2px 8px;")
        preview_header.addWidget(filename_label)
        mid_panel = QVBoxLayout(); mid_panel.addLayout(preview_header)
        
        # 预览区（只包含VideoPreview）
        preview_container = QFrame(); preview_container.setObjectName("ControlPanel")
        preview_outer_layout = QVBoxLayout(preview_container)
        preview_outer_layout.setContentsMargins(5, 5, 5, 5)  # 小边距
        preview = VideoPreview(); preview.set_mode(mode); preview.setMinimumSize(300, 500)
        preview.setSizePolicy(QSizePolicy.Policy.Expanding, QSizePolicy.Policy.Expanding)
        preview_outer_layout.addWidget(preview)  # 移除居中对齐，让它自然扩展
        mid_panel.addWidget(preview_container)
        
        # 播放控件（独立于预览区）
        slider_layout = QHBoxLayout(); btn_play = QPushButton("▶"); btn_play.setFixedSize(40, 40)
        time_label = QLabel("00:00 / 00:00"); slider = QSlider(Qt.Orientation.Horizontal)
        volume_layout = QHBoxLayout(); volume_layout.setSpacing(0); volume_layout.setContentsMargins(0, 0, 0, 0)
        volume_slider = QSlider(Qt.Orientation.Horizontal)
        volume_slider.setRange(0, 100); volume_slider.setValue(50); volume_slider.setFixedWidth(80)
        volume_slider.valueChanged.connect(lambda v: self.audio_output.setVolume(v / 100.0))
        lbl_spk = QLabel("🔊"); lbl_spk.setFixedWidth(20)
        volume_layout.addWidget(lbl_spk); volume_layout.addWidget(volume_slider)
        slider_layout.addWidget(btn_play); slider_layout.addWidget(slider); slider_layout.addWidget(time_label); slider_layout.addStretch(); slider_layout.addLayout(volume_layout)
        mid_panel.addLayout(slider_layout)
        
        layout.addLayout(mid_panel, 2)
        
        # 提取字幕模式：使用字幕编辑器替代操作控制面板
        subtitle_editor = None
        if mode == "ocr":
            subtitle_editor = SubtitleEditor()
            subtitle_editor.setMinimumWidth(400)  # 设置最小宽度，确保删除按钮可见
            layout.addWidget(subtitle_editor, 2)  # 增加宽度比例从1到2
        
        right_panel = QVBoxLayout(); right_panel.addWidget(QLabel("⚡️ <b>操作控制</b>"))
        bitrate_spin = None
        erase_count_label = None
        btn_clear_rects = None
        
        # 非提取字幕模式才显示右侧操作面板
        if mode != "ocr" and name == "画面擦除":
            group = QGroupBox("🎬 导出设置"); lay = QVBoxLayout(group); lay.addWidget(QLabel("视频码率 (Mbps):"))
            bitrate_spin = QDoubleSpinBox(); bitrate_spin.setRange(1.0, 20.0); bitrate_spin.setValue(1.3)
            bitrate_spin.setAlignment(Qt.AlignmentFlag.AlignCenter); bitrate_spin.setFixedSize(100, 35)
            lay.addWidget(bitrate_spin)
            right_panel.addWidget(group)
        
            # 多区域擦除控制面板
            erase_control = QGroupBox("🎯 多区域擦除"); erase_lay = QVBoxLayout(erase_control)
            erase_count_label = QLabel("当前擦除区域: <b style='color: #0071e3;'>0</b> 个")
            erase_count_label.setStyleSheet("font-size: 13px; color: #1c1c1e; padding: 5px;")
            erase_lay.addWidget(erase_count_label)
            
            btn_clear_rects = QPushButton("🗑️ 清空所有擦除框")
            btn_clear_rects.setStyleSheet("background-color: #ff3b30; font-size: 12px; padding: 8px;")
            btn_clear_rects.setFixedHeight(40)
            erase_lay.addWidget(btn_clear_rects)
            right_panel.addWidget(erase_control)
        
        # 非提取字幕模式才显示操作提示和启动按钮
        if mode != "ocr":
            info_box = QFrame(); info_box.setObjectName("ControlPanel"); info_layout = QVBoxLayout(info_box)
            prompt_map = {
                "ocr": "请划定【提取字幕】的红框区域",
                "erase": "<b>【多区域擦除】操作说明:</b><br>1. 鼠标拖拽绘制红框<br>2. 松开鼠标完成一个区域<br>3. 继续绘制下一个区域<br>4. 右键删除最后一个框",
                "burn": "请划定【字幕压制】的蓝框区域"
            }
            info_layout.addWidget(QLabel(f"<b style='color: #ff4d4f; font-size: 13px;'>{prompt_map.get(mode, '')}</b>")); right_panel.addWidget(info_box)
            right_panel.addStretch(); btn_go = QPushButton(f"🚀 批量启动\n{name}"); btn_go.setFixedHeight(120); btn_go.setStyleSheet("font-size: 18px; font-weight: bold;"); right_panel.addWidget(btn_go)
            layout.addLayout(right_panel, 1)
        else:
            # 提取字幕模式：显示简化的操作按钮
            info_box = QFrame(); info_box.setObjectName("ControlPanel"); info_layout = QVBoxLayout(info_box)
            info_layout.addWidget(QLabel(f"<b style='color: #ff4d4f; font-size: 13px;'>请划定【提取字幕】的红框区域</b>"))
            offset_row = QHBoxLayout()
            offset_row.addWidget(QLabel("字幕时间偏移 (秒):"))
            subtitle_offset_spin = QDoubleSpinBox()
            subtitle_offset_spin.setRange(-0.5, 0.5)
            subtitle_offset_spin.setSingleStep(0.05)
            subtitle_offset_spin.setValue(0)
            subtitle_offset_spin.setDecimals(2)
            subtitle_offset_spin.setToolTip("负值=提前，正值=延后；调整后立即生效，无需重新提取")
            subtitle_offset_spin.setAlignment(Qt.AlignmentFlag.AlignCenter)
            subtitle_offset_spin.setFixedSize(90, 32)
            offset_row.addWidget(subtitle_offset_spin)
            offset_row.addStretch()
            info_layout.addLayout(offset_row)
            right_panel.addWidget(info_box)
            right_panel.addStretch()
            btn_go = QPushButton(f"🚀 批量启动\n{name}"); btn_go.setFixedHeight(120); btn_go.setStyleSheet("font-size: 18px; font-weight: bold;"); right_panel.addWidget(btn_go)
            layout.addLayout(right_panel, 1)
        
        local_task_list = QListWidget(); local_task_list.setStyleSheet("border: none;")
        self.task_stack.addWidget(local_task_list)
        ocr_widgets = {
            "preview": preview, "slider": slider, "time": time_label, "btn_play": btn_play, 
            "list": file_list, "bitrate": bitrate_spin, "task_list": local_task_list,
            "erase_count_label": erase_count_label,  # 保存擦除计数标签
            "subtitle_editor": subtitle_editor,  # 保存字幕编辑器
            "subtitle_cache": {},  # 缓存提取的字幕数据: {video_path: [(start, end, text), ...]}
            "filename_label": filename_label  # 文件名显示标签
        }
        if mode == "ocr":
            ocr_widgets["subtitle_offset_spin"] = subtitle_offset_spin
        self.tab_widgets[index] = ocr_widgets
        btn_import.clicked.connect(lambda: self.import_videos(file_list))
        btn_all.clicked.connect(lambda: self.set_selection(file_list, True))
        btn_clear.clicked.connect(lambda: self.clear_all_items(file_list, index))
        file_list.itemClicked.connect(lambda item: self.load_preview(item, index))
        slider.valueChanged.connect(lambda val: self.seek_preview(val, index)); btn_go.clicked.connect(lambda: self.dispatch_batch_tasks(name, index))
        btn_play.clicked.connect(lambda: self.toggle_playback(index))
        
        # 提取字幕模式：特殊连接
        if mode == "ocr" and subtitle_editor:
            # 点击左侧视频 → 加载字幕到编辑器
            file_list.itemClicked.connect(lambda item: self.on_ocr_video_selected(item, index))
            # 进度条变化 → 高亮当前字幕
            slider.valueChanged.connect(lambda val: self.on_subtitle_time_changed(val, index))
            # 点击字幕 → 视频跳转
            subtitle_editor.subtitleClicked.connect(lambda t: self.on_subtitle_clicked(t, index))
            # 导出字幕
            subtitle_editor.exportRequested.connect(lambda path: self.export_subtitle(path, index))
            # 字幕内容变化 → 同步到视频预览
            subtitle_editor.subtitleChanged.connect(lambda: self.on_subtitle_content_changed(index))
            # 方案4：偏移值变化 → 立即刷新预览
            subtitle_offset_spin.valueChanged.connect(lambda: self.on_subtitle_offset_changed(index))
        
        # 画面擦除：连接清空按钮；擦除框变化时通过信号实时更新计数
        if name == "画面擦除" and btn_clear_rects and erase_count_label:
            btn_clear_rects.clicked.connect(lambda: self.clear_erase_rectangles(index))
            preview.erase_rects_changed.connect(lambda: self.update_erase_count(index))
        
        self.tabs.addTab(page, name)

    def add_translation_tab(self, index):
        page = QWidget(); layout = QHBoxLayout(page)
        left_panel = QVBoxLayout(); left_panel.addWidget(QLabel("📝 <b>SRT 任务列表</b>"))
        btn_import = QPushButton("📁 导入 SRT"); left_panel.addWidget(btn_import)
        srt_list = QListWidget(); srt_list.setSelectionMode(QListWidget.SelectionMode.MultiSelection); left_panel.addWidget(srt_list); layout.addLayout(left_panel, 1)
        mid_panel = QVBoxLayout(); config_box = QFrame(); config_box.setObjectName("ControlPanel"); box_layout = QVBoxLayout(config_box)
        self.combo_lang = QComboBox(); self.combo_lang.addItems(["泰语 (Thai)", "印尼语 (Indonesian)", "英语 (English)"]); box_layout.addWidget(QLabel("目标语言:")); box_layout.addWidget(self.combo_lang)
        self.combo_model = QComboBox(); self.combo_model.addItems(["llama3", "deepseek-v3"]); box_layout.addWidget(QLabel("AI 翻译模型:")); box_layout.addWidget(self.combo_model)
        mid_panel.addWidget(config_box); mid_panel.addStretch(); layout.addLayout(mid_panel, 2)
        right_panel = QVBoxLayout(); btn_go = QPushButton("🚀 启动\n批量翻译"); btn_go.setFixedHeight(120); btn_go.setStyleSheet("background-color: #34c759;"); right_panel.addStretch(); right_panel.addWidget(btn_go); layout.addLayout(right_panel, 1)
        local_task_list = QListWidget(); local_task_list.setStyleSheet("border: none;")
        self.task_stack.addWidget(local_task_list)
        self.tab_widgets[index] = {"list": srt_list, "task_list": local_task_list}
        btn_import.clicked.connect(lambda: self.import_srts(srt_list)); btn_go.clicked.connect(lambda: self.dispatch_batch_tasks("AI 翻译", index)); self.tabs.addTab(page, "AI 翻译")

    def import_videos(self, list_widget):
        files, _ = QFileDialog.getOpenFileNames(self, "选择视频", "", "Videos (*.mp4 *.mkv *.mov *.avi)")
        if not files: return
        
        # 自然数排序：解决 1, 10, 2 的排序问题
        import re
        def natural_sort_key(s):
            return [int(text) if text.isdigit() else text.lower() for text in re.split('([0-9]+)', s)]
        
        sorted_files = sorted(files, key=natural_sort_key)
        for f in sorted_files: 
            item = QListWidgetItem(os.path.basename(f))
            item.setData(Qt.ItemDataRole.UserRole, f)
            list_widget.addItem(item)
        self.set_selection(list_widget, True)

    def import_srts(self, list_widget):
        files, _ = QFileDialog.getOpenFileNames(self, "选择 SRT", "", "Subtitles (*.srt)")
        if not files: return
        
        import re
        def natural_sort_key(s):
            return [int(text) if text.isdigit() else text.lower() for text in re.split('([0-9]+)', s)]
            
        sorted_files = sorted(files, key=natural_sort_key)
        for f in sorted_files: 
            item = QListWidgetItem(os.path.basename(f))
            item.setData(Qt.ItemDataRole.UserRole, f)
            list_widget.addItem(item)
        self.set_selection(list_widget, True)

    def check_resolution_and_fps(self, index):
        """查询列表中所有视频的分辨率和帧率并统计"""
        w = self.tab_widgets[index]
        file_list = w["list"]
        result_label = w["resolution_result"]
        
        if file_list.count() == 0:
            result_label.setText("⚠️ 列表为空，请先导入视频")
            result_label.setStyleSheet("font-size: 11px; color: #ff9500; padding: 5px;")
            return
        
        result_label.setText("🔄 正在扫描分辨率与帧率，请稍候...")
        result_label.setStyleSheet("font-size: 11px; color: #007aff; padding: 5px;")
        QApplication.processEvents()  # 强制刷新界面
        
        # 统计分辨率和帧率
        resolution_map = {}  # {(width, height): [file1, file2, ...]}
        fps_map = {}  # {fps_str: [file1, file2, ...]}
        file_resolution_map = {}
        file_fps_map = {}  # {file_path: fps_int}
        
        for i in range(file_list.count()):
            item = file_list.item(i)
            video_path = item.data(Qt.ItemDataRole.UserRole)
            file_name = os.path.basename(video_path)
            
            try:
                cap = cv2.VideoCapture(video_path)
                if cap.isOpened():
                    width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
                    height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
                    fps_raw = cap.get(cv2.CAP_PROP_FPS)
                    cap.release()
                    fps_int = int(round(fps_raw)) if fps_raw > 0 else 0
                    fps_str = f"{fps_int}fps" if fps_int > 0 else "?"
                    
                    res_key = (width, height)
                    if res_key not in resolution_map:
                        resolution_map[res_key] = []
                    resolution_map[res_key].append(file_name)
                    file_resolution_map[video_path] = res_key
                    
                    if fps_str not in fps_map:
                        fps_map[fps_str] = []
                    fps_map[fps_str].append(file_name)
                    file_fps_map[video_path] = fps_int
                    
                    display_text = f"{file_name}  📐 {width}x{height}  ⏱ {fps_str}"
                    item.setText(display_text)
                else:
                    logger.warning(f"无法打开视频: {video_path}")
                    item.setText(f"{file_name}  ❌ 无法读取")
            except Exception as e:
                logger.error(f"读取视频失败 {video_path}: {e}")
                item.setText(f"{file_name}  ❌ 读取失败")
        
        if not resolution_map:
            result_label.setText("❌ 无法读取任何视频")
            result_label.setStyleSheet("font-size: 11px; color: #ff3b30; padding: 5px;")
            return
        
        total_files = file_list.count()
        unique_resolutions = len(resolution_map)
        unique_fps = len(fps_map)
        
        # 构建统计报告（分辨率 + 帧率）
        result_lines = []
        if unique_resolutions == 1:
            res = list(resolution_map.keys())[0]
            result_lines.append(f"✅ <b>分辨率一致</b> 所有 {total_files} 个视频: <b style='color: #34c759;'>{res[0]}x{res[1]}</b>")
            w["width"].setValue(res[0])
            w["height"].setValue(res[1])
        else:
            result_lines.append(f"⚠️ <b>分辨率</b> 发现 {unique_resolutions} 种:")
            for res, files in sorted(resolution_map.items(), key=lambda x: -len(x[1])):
                result_lines.append(f"  • {res[0]}x{res[1]}: {len(files)}个")
            most_common_res = max(resolution_map.items(), key=lambda x: len(x[1]))[0]
            w["width"].setValue(most_common_res[0])
            w["height"].setValue(most_common_res[1])
        
        if unique_fps == 1:
            fps_val = list(fps_map.keys())[0]
            result_lines.append(f"✅ <b>帧率一致</b> 均为 <b style='color: #34c759;'>{fps_val}</b>")
        else:
            result_lines.append(f"⚠️ <b>帧率</b> 发现 {unique_fps} 种（合并前建议统一）:")
            for fps_val, files in sorted(fps_map.items(), key=lambda x: -len(x[1])):
                result_lines.append(f"  • {fps_val}: {len(files)}个")
        
        result_lines.append("💡 左侧列表已显示每集分辨率与帧率")
        result_text = "<br>".join(result_lines)
        result_label.setText(result_text)
        result_label.setStyleSheet("font-size: 11px; color: #1c1c1e; padding: 5px;")
        if unique_resolutions > 1 or unique_fps > 1:
            result_label.setStyleSheet("font-size: 11px; color: #ff9500; padding: 5px;")
        
        w["file_resolution_map"] = file_resolution_map
        w["file_fps_map"] = file_fps_map
        logger.info(f"分辨率与帧率统计完成: {unique_resolutions} 种分辨率, {unique_fps} 种帧率, 共 {total_files} 个文件")
    
    def select_by_resolution(self, index):
        """按分辨率筛选视频（支持多选）"""
        w = self.tab_widgets[index]
        file_list = w["list"]
        file_resolution_map = w.get("file_resolution_map", {})
        
        if not file_resolution_map:
            QMessageBox.information(self, "提示", "请先点击「🔍 查询分辨率与帧率」按钮获取信息")
            return
        
        # 获取所有不同的分辨率
        unique_resolutions = list(set(file_resolution_map.values()))
        if len(unique_resolutions) == 1:
            QMessageBox.information(self, "提示", f"所有视频分辨率一致 ({unique_resolutions[0][0]}x{unique_resolutions[0][1]})，无需筛选")
            return
        
        # 统计每个分辨率的数量
        resolution_count = {}
        for res in unique_resolutions:
            count = sum(1 for v in file_resolution_map.values() if v == res)
            resolution_count[res] = count
        
        # 构建多选对话框
        dialog = QDialog(self)
        dialog.setWindowTitle("选择要筛选的分辨率")
        dialog.setMinimumWidth(400)
        dialog.setStyleSheet("""
            QDialog {
                background-color: #2b2b2b;
            }
            QLabel {
                color: #ffffff;
                font-size: 14px;
                padding: 10px;
            }
            QCheckBox {
                color: #ffffff;
                font-size: 14px;
                padding: 8px;
                spacing: 10px;
            }
            QCheckBox::indicator {
                width: 20px;
                height: 20px;
                border: 2px solid #0071e3;
                border-radius: 4px;
                background: #1a1a1a;
            }
            QCheckBox::indicator:checked {
                background: #0071e3;
                border: 2px solid #007aff;
            }
            QCheckBox::indicator:hover {
                border: 2px solid #007aff;
            }
            QPushButton {
                background-color: #0071e3;
                color: white;
                border: none;
                border-radius: 6px;
                padding: 10px 20px;
                font-size: 14px;
                font-weight: bold;
                min-width: 80px;
            }
            QPushButton:hover {
                background-color: #007aff;
            }
            QPushButton:pressed {
                background-color: #005bb7;
            }
        """)
        
        layout = QVBoxLayout(dialog)
        layout.setSpacing(5)
        
        title_label = QLabel("📊 <b>请选择要筛选的分辨率（支持多选）：</b>")
        title_label.setStyleSheet("color: #ffffff; font-size: 15px; padding: 10px;")
        layout.addWidget(title_label)
        
        checkboxes = {}
        
        # 按数量从多到少排序
        sorted_resolutions = sorted(resolution_count.items(), key=lambda x: x[1], reverse=True)
        for res, count in sorted_resolutions:
            text = f"{res[0]}x{res[1]} ({count}个文件)"
            checkbox = QCheckBox(text)
            checkboxes[checkbox] = res
            layout.addWidget(checkbox)
        
        # 默认勾选数量最少的（通常是需要统一调整的）
        if len(sorted_resolutions) > 1:
            list(checkboxes.keys())[-1].setChecked(True)  # 选中数量最少的
        
        button_box = QDialogButtonBox(QDialogButtonBox.StandardButton.Ok | QDialogButtonBox.StandardButton.Cancel)
        button_box.accepted.connect(dialog.accept)
        button_box.rejected.connect(dialog.reject)
        layout.addWidget(button_box)
        
        if dialog.exec() == QDialog.DialogCode.Accepted:
            # 找到所有选中的分辨率
            selected_resolutions = []
            for checkbox, res in checkboxes.items():
                if checkbox.isChecked():
                    selected_resolutions.append(res)
            
            if not selected_resolutions:
                QMessageBox.warning(self, "提示", "请至少选择一个分辨率")
                return
            
            # 选中所有匹配选中分辨率的视频
            file_list.clearSelection()
            selected_count = 0
            for i in range(file_list.count()):
                item = file_list.item(i)
                video_path = item.data(Qt.ItemDataRole.UserRole)
                if file_resolution_map.get(video_path) in selected_resolutions:
                    item.setSelected(True)
                    selected_count += 1
            
            # 生成提示文本
            res_text = "、".join([f"{r[0]}x{r[1]}" for r in selected_resolutions])
            
            # 使用深色主题的提示框
            msg = QMessageBox(self)
            msg.setWindowTitle("筛选完成")
            msg.setIcon(QMessageBox.Icon.Information)
            msg.setText(f"<b style='color: #34c759;'>已选中 {selected_count} 个视频</b>")
            msg.setInformativeText(
                f"分辨率范围: <b>{res_text}</b><br><br>"
                f"💡 <b>提示：</b>现在可以直接点击 <b style='color: #0071e3;'>🚀 批量启动分辨率转换</b><br>"
                f"对选中的视频进行统一调整"
            )
            msg.setStyleSheet("""
                QMessageBox {
                    background-color: #2b2b2b;
                }
                QLabel {
                    color: #ffffff;
                    font-size: 13px;
                }
                QPushButton {
                    background-color: #0071e3;
                    color: white;
                    border-radius: 6px;
                    padding: 8px 20px;
                    min-width: 80px;
                }
            """)
            msg.exec()
            
            logger.info(f"按分辨率筛选: {res_text}, 选中 {selected_count} 个文件")

    def select_by_fps(self, index):
        """按帧率筛选视频（支持多选）"""
        w = self.tab_widgets[index]
        file_list = w["list"]
        file_fps_map = w.get("file_fps_map", {})
        
        if not file_fps_map:
            QMessageBox.information(self, "提示", "请先点击「🔍 查询分辨率与帧率」按钮获取信息")
            return
        
        unique_fps = list(set(file_fps_map.values()))
        unique_fps = [f for f in unique_fps if f > 0]
        if len(unique_fps) <= 1:
            QMessageBox.information(self, "提示", f"所有视频帧率一致 ({unique_fps[0] if unique_fps else '?'}fps)，无需筛选")
            return
        
        fps_count = {}
        for fps in unique_fps:
            fps_count[fps] = sum(1 for v in file_fps_map.values() if v == fps)
        
        dialog = QDialog(self)
        dialog.setWindowTitle("选择要筛选的帧率")
        dialog.setMinimumWidth(400)
        dialog.setStyleSheet("""
            QDialog { background-color: #2b2b2b; }
            QLabel { color: #ffffff; font-size: 14px; padding: 10px; }
            QCheckBox { color: #ffffff; font-size: 14px; padding: 8px; spacing: 10px; }
            QCheckBox::indicator { width: 20px; height: 20px; border: 2px solid #0071e3; border-radius: 4px; background: #1a1a1a; }
            QCheckBox::indicator:checked { background: #0071e3; border: 2px solid #007aff; }
            QPushButton { background-color: #0071e3; color: white; border: none; border-radius: 6px; padding: 10px 20px; font-size: 14px; font-weight: bold; min-width: 80px; }
        """)
        layout = QVBoxLayout(dialog)
        layout.addWidget(QLabel("⏱ <b>请选择要筛选的帧率（支持多选）：</b>"))
        checkboxes = {}
        for fps in sorted(fps_count.keys(), key=lambda x: -fps_count[x]):
            cb = QCheckBox(f"{fps} fps ({fps_count[fps]}个文件)")
            checkboxes[cb] = fps
            layout.addWidget(cb)
        if len(fps_count) > 1:
            list(checkboxes.keys())[-1].setChecked(True)  # 默认选中数量最少的
        btn_box = QDialogButtonBox(QDialogButtonBox.StandardButton.Ok | QDialogButtonBox.StandardButton.Cancel)
        btn_box.accepted.connect(dialog.accept)
        btn_box.rejected.connect(dialog.reject)
        layout.addWidget(btn_box)
        
        if dialog.exec() == QDialog.DialogCode.Accepted:
            selected_fps = [checkboxes[cb] for cb in checkboxes if cb.isChecked()]
            if not selected_fps:
                QMessageBox.warning(self, "提示", "请至少选择一个帧率")
                return
            file_list.clearSelection()
            selected_count = 0
            for i in range(file_list.count()):
                item = file_list.item(i)
                video_path = item.data(Qt.ItemDataRole.UserRole)
                if file_fps_map.get(video_path) in selected_fps:
                    item.setSelected(True)
                    selected_count += 1
            fps_text = "、".join([f"{f}fps" for f in selected_fps])
            QMessageBox.information(self, "筛选完成", f"已选中 {selected_count} 个视频\n帧率: {fps_text}\n\n💡 可勾选「统一帧率」后批量转换")
            logger.info(f"按帧率筛选: {fps_text}, 选中 {selected_count} 个文件")

    def clear_erase_rectangles(self, index):
        """清空所有擦除框"""
        w = self.tab_widgets[index]
        if "preview" in w:
            w["preview"].clear_erase_rects()  # 内部会 emit erase_rects_changed，自动更新计数
    
    def update_erase_count(self, index):
        """更新擦除区域计数显示"""
        w = self.tab_widgets[index]
        if "preview" in w and "erase_count_label" in w and w["erase_count_label"]:
            count = len(w["preview"].erase_rects)
            w["erase_count_label"].setText(f"当前擦除区域: <b style='color: #0071e3;'>{count}</b> 个")
    
    def on_ocr_video_selected(self, item, index):
        """提取字幕：点击左侧视频，加载字幕到编辑器"""
        w = self.tab_widgets[index]
        video_path = item.data(Qt.ItemDataRole.UserRole)
        subtitle_editor = w.get("subtitle_editor")
        subtitle_cache = w.get("subtitle_cache", {})
        preview = w.get("preview")
        
        if not subtitle_editor:
            return
        
        # 加载视频预览
        self.load_preview(item, index)
        
        # 检查缓存中是否有字幕数据
        subtitle_data = None
        if video_path in subtitle_cache:
            subtitle_data = subtitle_cache[video_path]
            subtitle_editor.load_subtitles(video_path, subtitle_data)
            logger.info(f"从缓存加载字幕: {os.path.basename(video_path)}, {len(subtitle_data)} 条")
        else:
            # 检查是否已经提取过字幕文件
            video_dir = os.path.dirname(video_path)
            video_basename = os.path.splitext(os.path.basename(video_path))[0]
            srt_path = os.path.join(video_dir, "OCR_Result", f"{video_basename}.srt")
            
            if os.path.exists(srt_path):
                # 解析已有的 SRT 文件
                subtitle_data = self.parse_srt(srt_path)
                subtitle_cache[video_path] = subtitle_data
                subtitle_editor.load_subtitles(video_path, subtitle_data)
                logger.info(f"从文件加载字幕: {srt_path}, {len(subtitle_data)} 条")
            else:
                subtitle_editor.clear()
                logger.info(f"未找到字幕文件: {video_basename}")
        
        # 将字幕数据加载到 VideoPreview 用于叠加显示
        if preview and subtitle_data:
            preview.load_ocr_subtitles(subtitle_data)
            _off = w.get('subtitle_offset_spin')
            if _off is not None:
                preview.set_ocr_subtitle_offset(_off.value())
        elif preview:
            preview.load_ocr_subtitles([])
    
    def on_subtitle_time_changed(self, frame_idx, index):
        """进度条变化：高亮当前时间的字幕"""
        w = self.tab_widgets[index]
        subtitle_editor = w.get("subtitle_editor")
        preview = w.get("preview")
        
        if subtitle_editor and preview:
            fps = preview.property("fps") or 25.0
            if fps <= 0:
                logger.warning(f"[字幕同步] FPS 值异常: {fps}，使用默认值 25.0")
                fps = 25.0
            
            current_time = frame_idx / fps
            offset = w.get('subtitle_offset_spin').value() if w.get('subtitle_offset_spin') else 0
            
            # 高亮字幕编辑器中的当前字幕（传入偏移）
            subtitle_editor.highlight_subtitle_at_time(current_time, offset)
            
            # 在视频预览中显示当前字幕
            if hasattr(preview, 'set_current_subtitle_time'):
                preview.set_current_subtitle_time(current_time)
                preview.update()
                logger.info(f"[字幕同步] 当前时间: {current_time:.3f}s (帧 {frame_idx}), 已更新预览")
    
    def on_subtitle_offset_changed(self, index):
        """方案4：偏移值变化时立即刷新预览"""
        w = self.tab_widgets[index]
        preview = w.get("preview")
        _off = w.get("subtitle_offset_spin")
        if preview and _off is not None:
            preview.set_ocr_subtitle_offset(_off.value())
            preview.update()
    
    def on_subtitle_clicked(self, start_time, index):
        """点击字幕：视频跳转到该时间点（应用偏移后的有效时间）"""
        w = self.tab_widgets[index]
        slider = w.get("slider")
        preview = w.get("preview")
        offset = w.get("subtitle_offset_spin").value() if w.get("subtitle_offset_spin") else 0
        seek_time = start_time + offset
        
        logger.info(f"[字幕点击] 跳转到时间: {seek_time:.2f}s (原始 {start_time:.2f}s + 偏移 {offset:+.2f}s)")
        
        if slider and preview:
            fps = preview.property("fps") or 25.0
            frame_idx = int(max(0, seek_time * fps))
            slider.setValue(frame_idx)
            logger.debug(f"跳转到字幕时间: {seek_time:.2f}s (frame {frame_idx})")
    
    def on_subtitle_content_changed(self, index):
        """字幕内容变化：同步到视频预览区"""
        w = self.tab_widgets[index]
        subtitle_editor = w.get("subtitle_editor")
        preview = w.get("preview")
        
        if not subtitle_editor or not preview:
            return
        
        # 获取最新的字幕数据
        updated_subtitle_data = subtitle_editor.get_subtitle_data()
        logger.info(f"[字幕同步] 字幕内容已变化，同步到视频预览区，共 {len(updated_subtitle_data)} 条")
        
        # 更新 VideoPreview 的字幕数据
        preview.load_ocr_subtitles(updated_subtitle_data)
        _off = w.get('subtitle_offset_spin')
        if _off is not None:
            preview.set_ocr_subtitle_offset(_off.value())
        
        # 强制刷新当前帧，立即显示更新后的字幕
        preview.update()
        
        logger.debug(f"[字幕同步] 视频预览区字幕数据已更新")
    
    def export_subtitle(self, video_path, index):
        """导出编辑后的字幕"""
        w = self.tab_widgets[index]
        subtitle_editor = w.get("subtitle_editor")
        
        if not subtitle_editor:
            logger.error("字幕编辑器不存在")
            return
        
        subtitle_data = subtitle_editor.get_subtitle_data()
        logger.info(f"[导出字幕] 从编辑器获取到 {len(subtitle_data) if subtitle_data else 0} 条字幕数据")
        
        if not subtitle_data:
            QMessageBox.warning(self, "提示", "没有可导出的字幕数据")
            return
        
        # 方案4：导出时应用当前偏移，使导出的 SRT 与预览一致
        offset = w.get("subtitle_offset_spin").value() if w.get("subtitle_offset_spin") else 0
        
        # 生成 SRT 内容（标准格式）
        srt_lines = []
        for i, (start, end, text) in enumerate(subtitle_data, 1):
            s_eff = max(0, start + offset)
            e_eff = max(s_eff, end + offset)
            logger.debug(f"[导出字幕] 处理第 {i} 条: {s_eff:.2f}s -> {e_eff:.2f}s (偏移 {offset:+.2f}s), 文本: {text[:30]}...")
            # 标准 SRT 格式：序号、时间轴、文本、空行
            srt_lines.append(f"{i}")
            srt_lines.append(f"{self.format_time_srt(s_eff)} --> {self.format_time_srt(e_eff)}")
            srt_lines.append(text)
            srt_lines.append("")  # 空行分隔
        
        logger.info(f"[导出字幕] 生成了 {len(srt_lines)} 行 SRT 内容（包含空行）")
        logger.debug(f"[导出字幕] 前3条字幕预览:\n{chr(10).join(srt_lines[:15])}")
        
        # 保存到文件
        video_dir = os.path.dirname(video_path)
        video_basename = os.path.splitext(os.path.basename(video_path))[0]
        output_dir = os.path.join(video_dir, "OCR_Result")
        os.makedirs(output_dir, exist_ok=True)
        output_path = os.path.join(output_dir, f"{video_basename}.srt")
        
        try:
            # 使用标准换行符连接所有行
            full_content = "\n".join(srt_lines)
            logger.info(f"[导出字幕] 准备写入文件，总字符数: {len(full_content)}, 总行数: {len(srt_lines)}")
            logger.debug(f"[导出字幕] 文件内容预览（前500字符）:\n{full_content[:500]}")
            
            with open(output_path, "w", encoding="utf-8") as f:
                f.write(full_content)
            
            # 验证文件写入
            file_size = os.path.getsize(output_path)
            logger.info(f"[导出字幕] 文件写入成功，大小: {file_size} 字节")
            
            # 读取文件验证
            with open(output_path, "r", encoding="utf-8") as f:
                verify_content = f.read()
                verify_lines = verify_content.strip().split("\n")
                logger.info(f"[导出字幕] 验证读取：文件共 {len(verify_lines)} 行")
                # 统计字幕条数（每4行为一条字幕：序号、时间、文本、空行）
                verify_count = verify_content.count("\n\n") + 1  # 粗略统计
                logger.info(f"[导出字幕] 验证字幕条数：约 {verify_count} 条")
            
            QMessageBox.information(self, "导出成功", 
                f"✅ 字幕已保存到:\n{output_path}\n\n"
                f"📊 共 {len(subtitle_data)} 条字幕\n"
                f"📄 文件大小: {file_size} 字节\n"
                f"✔️ 验证通过: {verify_count} 条字幕")
            logger.info(f"字幕导出成功: {output_path}, 共 {len(subtitle_data)} 条")
        except Exception as e:
            QMessageBox.critical(self, "导出失败", f"保存字幕时出错:\n{str(e)}")
            logger.error(f"字幕导出失败: {e}", exc_info=True)
    
    def format_time_srt(self, seconds):
        """格式化时间为 SRT 格式: HH:MM:SS,mmm"""
        hours = int(seconds // 3600)
        mins = int((seconds % 3600) // 60)
        secs = int(seconds % 60)
        millis = int((seconds - int(seconds)) * 1000)
        return f"{hours:02d}:{mins:02d}:{secs:02d},{millis:03d}"

    def set_selection(self, list_widget, select_all):
        for i in range(list_widget.count()): list_widget.item(i).setSelected(select_all)

    def clear_all_items(self, list_widget, index):
        list_widget.clear()
        if index in self.tab_widgets and "preview" in self.tab_widgets[index]:
            self.tab_widgets[index]["preview"].clear(); self.tab_widgets[index]["time"].setText("00:00 / 00:00")

    def load_preview(self, item, index):
        path = item.data(Qt.ItemDataRole.UserRole); w = self.tab_widgets[index]; w["preview"].setProperty("current_path", path)
        
        # 更新文件名显示
        filename = os.path.basename(path)
        if "filename_label" in w:
            w["filename_label"].setText(f"<b style='color: #007aff;'>📁 {filename}</b>")
        
        if self.current_playing_index == index: self.player.setSource(QUrl.fromLocalFile(path)); self.player.setPosition(0)
        cap = cv2.VideoCapture(path)
        if cap.isOpened():
            fps = cap.get(cv2.CAP_PROP_FPS) or 25.0; total = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
            w["preview"].setProperty("fps", fps); w["slider"].setRange(0, total - 1); w["slider"].setValue(0); self.update_frame_by_index(index, 0)
        cap.release()
        if self.tabs.tabText(index) == "字幕压制":
            self.auto_match_srt(path, index)
            _off = w.get('subtitle_offset_spin')
            if _off is not None:
                w["preview"].set_ocr_subtitle_offset(_off.value())

    def auto_match_srt(self, video_path, index, silent=False):
        w = self.tab_widgets[index]; v_name = os.path.basename(video_path)
        # 获取文件名“点”前面的部分，例如 1.mp4 -> 1
        video_base_name = v_name.split('.')[0]
        srt_path = None
        
        # 1. 严格匹配：优先从已导入的 s_list 中寻找点前面名字完全一样的文件
        for i in range(w["s_list"].count()):
            s_item = w["s_list"].item(i)
            s_abs_path = s_item.data(Qt.ItemDataRole.UserRole)
            # 例如 1.srt -> 1
            srt_base_name = os.path.basename(s_abs_path).split('.')[0]
            
            if video_base_name == srt_base_name:
                srt_path = s_abs_path
                break
        
        # 2. 如果列表没找到，再从文件系统寻找完全同名的 .srt
        # 查找顺序：同目录、同目录下 OCR_Result/Translated_SRT、父级 OCR_Result/Translated_SRT
        if not srt_path:
            v_dir = os.path.dirname(video_path)
            parent_dir = os.path.dirname(v_dir)
            search_dirs = [
                v_dir,
                os.path.join(v_dir, "OCR_Result"),
                os.path.join(v_dir, "Translated_SRT"),
                os.path.join(parent_dir, "OCR_Result"),
                os.path.join(parent_dir, "Translated_SRT"),
            ]
            for d in search_dirs:
                if not os.path.exists(d): continue
                p = os.path.join(d, f"{video_base_name}.srt")
                if os.path.exists(p): srt_path = p; break
        
        if srt_path:
            self.video_srt_map[os.path.abspath(video_path)] = os.path.abspath(srt_path)
            if not silent:
                srt_data = self.parse_srt(srt_path); w["lyric_list"].clear()
                if srt_data:
                    w["preview"].set_srt_data(srt_data)
                    for s, e, t in srt_data: w["lyric_list"].addItem(f"[{int(s//60):02d}:{int(s%60):02d}] {t}")
                    
                    if w["preview"].burn_rect.isNull():
                        pix = w["preview"].pixmap()
                        if pix:
                            pw, ph, vw, vh = w["preview"].width(), w["preview"].height(), pix.width(), pix.height()
                            ox = (pw - vw) / 2
                            rw = int(vw * 0.9); rx = int(ox + (vw - rw) / 2)
                            ry = int((ph - vh) / 2 + vh * 0.85)
                            w["preview"].burn_rect = QRect(rx, ry, rw, int(vh * 0.1))
                w["preview"].update()
        else:
            # 如果没找到匹配，清空台词
            if not silent:
                w["lyric_list"].clear()
                w["preview"].set_srt_data([])
                w["preview"].update()

    def parse_srt(self, path):
        import re; data = []
        try:
            with open(path, 'r', encoding='utf-8-sig', errors='ignore') as f: content = f.read()
            pattern = re.compile(r'(\d+)\s*\n(\d{1,2}:\d{2}:\d{2}[.,]\d{3})\s*-->\s*(\d{1,2}:\d{2}:\d{2}[.,]\d{3})\s*\n(.*?)(?=\n\s*\n|\n\s*\d+\s*\n|$)', re.DOTALL)
            matches = pattern.findall(content)
            def to_s(t): t=t.replace(',','.'); p=t.split(':'); return int(p[0])*3600+int(p[1])*60+float(p[2])
            for m in matches: data.append((to_s(m[1]), to_s(m[2]), m[3].strip().replace('\n', ' ')))
            return data
        except: return []

    def jump_to_lyric(self, item, index):
        try:
            time_str = item.text().split(']')[0][1:]
            m, s = time_str.split(':')
            start_time = int(m) * 60 + int(s)
            w = self.tab_widgets[index]
            fps = w["preview"].property("fps") or 25.0
            w["slider"].setValue(int(start_time * fps))
        except: pass

    def seek_preview(self, val, index): 
        self.update_frame_by_index(index, val)
        if self.current_playing_index == index:
            fps = self.tab_widgets[index]["preview"].property("fps") or 25.0
            new_pos = int(val / fps * 1000)
            if abs(self.player.position() - new_pos) > 100: self.player.setPosition(new_pos)

    def update_frame_by_index(self, index, frame_idx):
        w = self.tab_widgets[index]; path = w["preview"].property("current_path")
        if not path: return
        fps = w["preview"].property("fps") or 25.0
        current_time = frame_idx / fps
        
        # 更新两个时间变量：字幕压制模式和OCR模式
        w["preview"].set_current_time(current_time)
        w["preview"].set_current_subtitle_time(current_time)  # OCR字幕时间
        
        # 如果是 OCR 模式且有字幕编辑器，同步高亮当前字幕（传入偏移）
        subtitle_editor = w.get("subtitle_editor")
        if subtitle_editor:
            offset = w.get("subtitle_offset_spin").value() if w.get("subtitle_offset_spin") else 0
            subtitle_editor.highlight_subtitle_at_time(current_time, offset)
        
        if self.current_cap_path != path:
            if self.current_cap: self.current_cap.release()
            self.current_cap = cv2.VideoCapture(path); self.current_cap_path = path
            w["preview"].video_size = QSize(int(self.current_cap.get(3)), int(self.current_cap.get(4)))
        self.current_cap.set(cv2.CAP_PROP_POS_FRAMES, frame_idx)
        ret, frame = self.current_cap.read()
        if ret:
            rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB); h, rw, ch = rgb.shape
            q_img = QImage(rgb.data, rw, h, ch * rw, QImage.Format.Format_RGB888)
            
            # 智能缩放：使用 KeepAspectRatio 自动计算最佳尺寸
            preview_widget = w["preview"]
            available_width = max(preview_widget.width(), 300)  # 至少300px宽度
            available_height = max(preview_widget.height(), 500)  # 至少500px高度
            
            # 直接使用 Qt 的 KeepAspectRatio 进行智能缩放
            pix = QPixmap.fromImage(q_img).scaled(
                available_width, 
                available_height, 
                Qt.AspectRatioMode.KeepAspectRatio, 
                Qt.TransformationMode.SmoothTransformation
            )
            w["preview"].setPixmap(pix)
            cur_s = frame_idx / fps; tot_s = int(self.current_cap.get(7)) / fps
            w["time"].setText(f"{int(cur_s//60):02d}:{int(cur_s%60):02d} / {int(tot_s//60):02d}:{int(tot_s%60):02d}")

    def dispatch_batch_tasks(self, task_type, index):
        w = self.tab_widgets[index]; preview = w.get("preview")
        if task_type == "字幕压制":
            items = [w["batch_list"].item(i) for i in range(w["batch_list"].count())]
        else:
            items = w["list"].selectedItems()
            
        if not items: QMessageBox.warning(self, "提醒", "请先选择或导入文件。"); return
        
        m_ocr = None; m_erase_rects = None; m_burn = None
        if task_type == "提取字幕":
            if not preview or preview.ocr_rect.isNull(): 
                QMessageBox.warning(self, "提醒", "请先划定提取红框。"); 
                return
            
            # 【OCR Tab 保护】检查 video_size 是否有效
            if not hasattr(preview, 'video_size') or preview.video_size.isEmpty():
                logger.warning("[提取字幕] 检测到 video_size 无效，尝试从第一个视频获取尺寸")
                first_video_path = items[0].data(Qt.ItemDataRole.UserRole)
                temp_cap = cv2.VideoCapture(first_video_path)
                if temp_cap.isOpened():
                    preview.video_size = QSize(int(temp_cap.get(cv2.CAP_PROP_FRAME_WIDTH)), 
                                              int(temp_cap.get(cv2.CAP_PROP_FRAME_HEIGHT)))
                    temp_cap.release()
                    logger.info(f"[提取字幕] 成功从第一个视频获取尺寸: {preview.video_size.width()}x{preview.video_size.height()}")
                else:
                    temp_cap.release()
                    QMessageBox.warning(self, "错误", "⚠️ 请先点击左侧列表中的一个视频加载预览，再绘制提取框")
                    return
            
            m_ocr = self.map_rect_to_video(preview.ocr_rect, preview)
            if not m_ocr:
                QMessageBox.warning(self, "坐标转换失败", "提取框坐标转换失败，请重新划定")
                return
        elif task_type == "画面擦除":
            if not preview or len(preview.erase_rects) == 0: 
                QMessageBox.warning(self, "提醒", "请先划定至少一个擦除红框。<br><br>💡 提示：可以绘制多个红框，一次性擦除多个区域"); 
                return
            
            # 【关键修复】检查 video_size 是否有效，如果无效则从第一个视频获取
            if not hasattr(preview, 'video_size') or preview.video_size.isEmpty():
                logger.warning("检测到 video_size 无效，尝试从第一个视频获取尺寸")
                first_video_path = items[0].data(Qt.ItemDataRole.UserRole)
                temp_cap = cv2.VideoCapture(first_video_path)
                if temp_cap.isOpened():
                    preview.video_size = QSize(int(temp_cap.get(cv2.CAP_PROP_FRAME_WIDTH)), 
                                              int(temp_cap.get(cv2.CAP_PROP_FRAME_HEIGHT)))
                    temp_cap.release()
                    logger.info(f"成功从第一个视频获取尺寸: {preview.video_size.width()}x{preview.video_size.height()}")
                else:
                    temp_cap.release()
                    QMessageBox.warning(self, "错误", 
                        "⚠️ <b>无法读取视频尺寸</b><br><br>"
                        "请先执行以下步骤：<br>"
                        "1️⃣ 点击左侧列表中的任意一个视频<br>"
                        "2️⃣ 等待中间预览区加载视频画面<br>"
                        "3️⃣ 在预览画面上绘制擦除框<br>"
                        "4️⃣ 全选所有视频后，再点击批量擦除")
                    return
            
            # 将所有擦除框转换为视频坐标
            m_erase_rects = []
            for i, rect in enumerate(preview.erase_rects):
                mapped = self.map_rect_to_video(rect, preview)
                if mapped:
                    m_erase_rects.append(mapped)
                    logger.debug(f"擦除框 {i+1}: UI坐标 {rect} -> 视频坐标 {mapped}")
                else:
                    logger.error(f"擦除框 {i+1} 转换失败: {rect}")
            
            if not m_erase_rects:
                QMessageBox.warning(self, "坐标转换失败", 
                    "❌ <b>擦除框坐标转换失败</b><br><br>"
                    "可能原因：<br>"
                    "• 视频尚未正确加载预览<br>"
                    "• 擦除框绘制在视频区域外<br><br>"
                    "解决方法：<br>"
                    "1️⃣ 先点击左侧一个视频加载预览<br>"
                    "2️⃣ 确保在视频画面内绘制擦除框<br>"
                    "3️⃣ 重新绘制并再次尝试")
                return
            logger.info(f"✅ 准备擦除 {len(m_erase_rects)} 个区域")
        elif task_type == "字幕压制":
            if not preview or preview.burn_rect.isNull(): 
                QMessageBox.warning(self, "提醒", "请先划定压制蓝框。"); 
                return
            
            # 【字幕压制 Tab 保护】检查 video_size 是否有效
            if not hasattr(preview, 'video_size') or preview.video_size.isEmpty():
                logger.warning("[字幕压制] 检测到 video_size 无效，尝试从第一个视频获取尺寸")
                first_video_path = items[0].data(Qt.ItemDataRole.UserRole)
                temp_cap = cv2.VideoCapture(first_video_path)
                if temp_cap.isOpened():
                    preview.video_size = QSize(int(temp_cap.get(cv2.CAP_PROP_FRAME_WIDTH)), 
                                              int(temp_cap.get(cv2.CAP_PROP_FRAME_HEIGHT)))
                    temp_cap.release()
                    logger.info(f"[字幕压制] 成功从第一个视频获取尺寸: {preview.video_size.width()}x{preview.video_size.height()}")
                else:
                    temp_cap.release()
                    QMessageBox.warning(self, "错误", "⚠️ 请先点击左侧列表中的一个视频加载预览，再绘制压制框")
                    return
            
            m_burn = self.map_rect_to_video(preview.burn_rect, preview)
            if not m_burn:
                QMessageBox.warning(self, "坐标转换失败", "压制框坐标转换失败，请重新划定")
                return

        # 根据任务类型选择并发数
        if task_type == "字幕压制":
            parallel_count = self.burn_parallel_spin.value()
            logger.info(f"[{task_type}] 使用字幕压制并发数: {parallel_count}")
        else:
            parallel_count = self.parallel_spin.value()
            logger.info(f"[{task_type}] 使用其他功能并发数: {parallel_count}")

        config = {
            'target_lang': self.combo_lang.currentText() if hasattr(self, 'combo_lang') else "泰语 (Thai)",
            'model': self.combo_model.currentText() if hasattr(self, 'combo_model') else "llama3",
            'temp_folder': self.temp_folder, 'parallel_count': parallel_count,
            'bitrate': f"{w['bitrate'].value():.1f}M" if w.get('bitrate') else "1.3M",
            'ocr_rect': m_ocr, 'erase_rects': m_erase_rects, 'blue_rect': m_burn,
            'font_size': preview.font_config.get("size", 36) if preview else 36,
            'font_color': preview.font_config.get("color", "白色") if preview else "白色",
            'video_srt_map': self.video_srt_map,
            'target_width': w.get('width').value() if w.get('width') else 1080,
            'target_height': w.get('height').value() if w.get('height') else 1920,
            'target_fps': w.get('fps_combo').currentText().replace(' fps', '') if w.get('fps_check') and w.get('fps_check').isChecked() and w.get('fps_combo') else None
        }
        # 方案4：字幕时间偏移在预览/压制时应用；压制 Tab 和提取 Tab 均有控件，压制时优先用当前 Tab
        if task_type == "字幕压制":
            if w.get('subtitle_offset_spin') is not None:
                config['subtitle_time_offset'] = w['subtitle_offset_spin'].value()
            elif len(self.tab_widgets) > 1 and self.tab_widgets[1].get('subtitle_offset_spin') is not None:
                config['subtitle_time_offset'] = self.tab_widgets[1]['subtitle_offset_spin'].value()
        
        first_path = items[0].data(Qt.ItemDataRole.UserRole)
        batch_name = f"【{os.path.basename(os.path.dirname(first_path))}】批量任务 ({len(items)}个文件)"
        
        target_list = w["task_list"]
        tw = TaskItemWidget(batch_name, task_type)
        li = QListWidgetItem(target_list); li.setSizeHint(tw.sizeHint())
        target_list.insertItem(0, li); target_list.setItemWidget(li, tw)
        
        paths = [os.path.abspath(i.data(Qt.ItemDataRole.UserRole)) for i in items]
        step_map = {"调分辨率": "scale", "提取字幕": "ocr", "画面擦除": "erase", "AI 翻译": "translate", "字幕压制": "burn"}
        
        wf = TranslationWorkflow(paths, config); wf.single_step = step_map.get(task_type)
        tw.pauseClicked.connect(lambda: wf.pause() if not tw.is_paused else wf.resume())
        tw.stopClicked.connect(wf.stop); wf.progress.connect(lambda v, t, twid=tw: self.update_task_status(twid, v, t))
        wf.finished.connect(lambda _, p=wf, twid=tw: self.on_task_finished(p, twid))
        
        # 提取字幕任务：连接字幕数据信号
        if task_type == "提取字幕":
            wf.subtitle_extracted.connect(lambda path, data: self.on_subtitle_extracted(path, data, index))
        
        self.running_workflows[tw] = wf; wf.start()

    def on_subtitle_extracted(self, video_path, subtitle_data, index):
        """字幕提取完成：缓存数据并更新当前显示"""
        w = self.tab_widgets[index]
        subtitle_cache = w.get("subtitle_cache", {})
        subtitle_cache[video_path] = subtitle_data
        logger.info(f"字幕已缓存: {os.path.basename(video_path)}, {len(subtitle_data)} 条")
        
        # 检查当前预览的视频是否就是刚提取完字幕的视频
        preview = w.get("preview")
        subtitle_editor = w.get("subtitle_editor")
        current_path = preview.property("current_path") if preview else None
        
        if current_path == video_path:
            # 自动加载字幕到预览区和编辑器
            logger.info(f"[字幕提取] 自动加载刚提取的字幕到预览区: {os.path.basename(video_path)}")
            if preview:
                preview.load_ocr_subtitles(subtitle_data)
                _off = w.get('subtitle_offset_spin')
                if _off is not None:
                    preview.set_ocr_subtitle_offset(_off.value())
            if subtitle_editor:
                subtitle_editor.load_subtitles(video_path, subtitle_data)
                logger.info(f"[字幕提取] 字幕已加载到编辑器，共 {len(subtitle_data)} 条")

    def update_task_status(self, widget, val, txt): widget.pbar.setValue(val); widget.status_label.setText(txt)
    def on_task_finished(self, wf, widget):
        """任务完成回调：停用按钮、清理工作流、检查并清理0B文件"""
        widget.btn_pause.setEnabled(False)
        widget.btn_stop.setEnabled(False)
        widget.on_task_finished()
        self.running_workflows.pop(widget, None)
        
        # 清理0B文件（如果是字幕压制任务）
        if hasattr(wf, 'single_step') and wf.single_step == 'burn':
            self.cleanup_zero_byte_files(wf)
    
    def cleanup_zero_byte_files(self, workflow):
        """清理字幕压制任务产生的0B文件"""
        try:
            output_folder = workflow.config.get('temp_folder', '')
            if not output_folder or not os.path.exists(output_folder):
                return
            
            # 查找所有0B的mp4文件
            zero_byte_files = []
            for file_name in os.listdir(output_folder):
                if file_name.endswith('.mp4'):
                    file_path = os.path.join(output_folder, file_name)
                    if os.path.isfile(file_path) and os.path.getsize(file_path) == 0:
                        zero_byte_files.append(file_path)
            
            # 删除0B文件
            if zero_byte_files:
                logger.warning(f"🗑️ 发现 {len(zero_byte_files)} 个0B文件，正在清理...")
                deleted_count = 0
                for file_path in zero_byte_files:
                    try:
                        os.remove(file_path)
                        deleted_count += 1
                        logger.info(f"✅ 已删除0B文件: {os.path.basename(file_path)}")
                    except Exception as e:
                        logger.error(f"❌ 删除0B文件失败 {os.path.basename(file_path)}: {e}")
                
                if deleted_count > 0:
                    QMessageBox.information(
                        self,
                        "清理完成",
                        f"已自动清理 <b>{deleted_count}</b> 个0B文件<br><br>"
                        f"<span style='color: #666;'>这些文件通常是由于硬件加速失败且CPU回退也失败导致的。</span>"
                    )
        except Exception as e:
            logger.error(f"清理0B文件过程出错: {e}")
    def closeEvent(self, e): 
        if self.current_cap: self.current_cap.release()
        super().closeEvent(e)

    def select_merge_input_folder(self):
        """选择剧集合并的输入文件夹"""
        folder = QFileDialog.getExistingDirectory(self, "选择输入文件夹（包含多集视频）")
        if folder:
            self.merge_input_edit.setText(folder)
            # 扫描文件夹中的视频文件
            self.scan_merge_files(folder)
    
    def select_merge_output_folder(self):
        """选择剧集合并的输出文件夹"""
        folder = QFileDialog.getExistingDirectory(self, "选择输出文件夹")
        if folder:
            self.merge_output_edit.setText(folder)
    
    def scan_merge_files(self, folder):
        """扫描文件夹中的视频文件"""
        import glob
        import re
        
        self.merge_file_list.clear()
        video_files = []
        
        # 扫描支持的视频格式
        for ext in ['*.mp4', '*.mov', '*.mkv', '*.avi']:
            video_files.extend(glob.glob(os.path.join(folder, ext)))
        
        if not video_files:
            self.merge_log_text.append("❌ 未在文件夹中找到视频文件")
            return
        
        # 自然排序（支持1, 2, 10的正确排序）
        def natural_sort_key(s):
            return [int(text) if text.isdigit() else text.lower()
                    for text in re.split(r'(\d+)', s)]
        
        video_files.sort(key=lambda x: natural_sort_key(os.path.basename(x)))
        
        # 添加到列表
        for video in video_files:
            self.merge_file_list.addItem(f"📹 {os.path.basename(video)}")
        
        self.merge_log_text.append(f"✅ 扫描完成：发现 {len(video_files)} 个视频文件")
    
    def start_merge_task(self):
        """启动剧集合并任务"""
        input_folder = self.merge_input_edit.text()
        output_folder = self.merge_output_edit.text()
        
        if not input_folder:
            QMessageBox.warning(self, "提醒", "请先选择输入文件夹")
            return
        
        if not output_folder:
            QMessageBox.warning(self, "提醒", "请先选择输出文件夹")
            return
        
        if self.merge_file_list.count() == 0:
            QMessageBox.warning(self, "提醒", "输入文件夹中没有视频文件")
            return
        
        trim_mode = self.merge_trim_mode.currentText()
        if trim_mode == "固定秒数裁剪":
            param_str = f"• 裁剪模式: 固定秒数裁剪<br>• 每集切除: {self.merge_fixed_trim_seconds.value()} 秒<br>• 最后一集保留片尾: {'是' if self.merge_skip_last_episode.isChecked() else '否'}<br>"
        else:
            param_str = f"• 裁剪模式: 智能亮度检测<br>• 检测时长: {self.merge_tail_seconds.value()} 秒<br>• 亮度阈值: {self.merge_brightness.value()}<br>• 亮度跳变: {self.merge_delta.value()}<br>"
        param_str += f"• 高兼容合并: {'是' if self.merge_compatible_mode.isChecked() else '否'}<br>"
        
        # 未勾选高兼容合并时，提示可能出现的播放卡顿风险
        warn_html = ""
        if not self.merge_compatible_mode.isChecked():
            warn_html = (
                "<br><span style='color: #ff9500;'>⚠️ 提示：未勾选「高兼容合并」时，合并后的视频可能在片段交界处出现播放卡顿。"
                "若曾遇到此类问题，建议勾选该选项后重新合并。</span><br><br>"
            )
        
        _confirm_style = """
            QMessageBox { background-color: #2b2b2b; }
            QMessageBox QLabel { color: #ffffff; font-size: 13px; }
            QPushButton { background-color: #0071e3; color: white; border-radius: 6px; padding: 8px 20px; min-width: 80px; }
        """
        msg = QMessageBox(self)
        msg.setWindowTitle("确认合并")
        msg.setIcon(QMessageBox.Icon.Question)
        msg.setText("<b style='color: #34c759;'>准备合并 {} 个视频文件</b>".format(self.merge_file_list.count()))
        msg.setInformativeText(
            f"<span style='color: #e5e5ea;'>输入文件夹: </span><span style='color: #ffffff;'>{input_folder}</span><br>"
            f"<span style='color: #e5e5ea;'>输出文件夹: </span><span style='color: #ffffff;'>{output_folder}</span><br><br>"
            f"<span style='color: #e5e5ea;'><b>参数设置：</b></span><br>"
            f"<span style='color: #ffffff;'>{param_str}• 并发核心数: {self.merge_workers.value()}</span>"
            f"{warn_html}"
            f"<span style='color: #e5e5ea;'>是否开始合并？</span>"
        )
        msg.setStyleSheet(_confirm_style)
        msg.setStandardButtons(QMessageBox.StandardButton.Yes | QMessageBox.StandardButton.No)
        msg.setDefaultButton(QMessageBox.StandardButton.Yes)
        msg.button(QMessageBox.StandardButton.Yes).setText("是")
        msg.button(QMessageBox.StandardButton.No).setText("否")
        reply = msg.exec()
        
        if reply == QMessageBox.StandardButton.No:
            return
        
        # 清空日志
        self.merge_log_text.clear()
        self.merge_log_text.append("=" * 50)
        self.merge_log_text.append("🚀 正在启动智能无感去尾合并器...")
        self.merge_log_text.append("=" * 50)
        
        # 创建并启动合并线程
        from core.merge_worker import MergeWorker
        
        config = {
            'input_folder': input_folder,
            'output_folder': output_folder,
            'trim_mode': 'brightness' if trim_mode == '智能亮度检测' else 'fixed_seconds',
            'compatible_merge': self.merge_compatible_mode.isChecked(),
            'check_seconds': self.merge_tail_seconds.value(),
            'brightness_threshold': self.merge_brightness.value(),
            'delta_threshold': self.merge_delta.value(),
            'fixed_trim_seconds': self.merge_fixed_trim_seconds.value(),
            'skip_last_episode_trim': self.merge_skip_last_episode.isChecked(),
            'max_workers': self.merge_workers.value()
        }
        
        self.merge_worker = MergeWorker(config)
        self.merge_worker.progress.connect(self.on_merge_progress)
        self.merge_worker.finished.connect(self.on_merge_finished)
        self.merge_worker.start()
        
        self.merge_log_text.append(f"📦 发现 {self.merge_file_list.count()} 个分段文件")
        self.merge_log_text.append(f"🔥 已启动 {config['max_workers']} 个并发处理核心")
    
    def on_merge_progress(self, message):
        """合并进度更新"""
        self.merge_log_text.append(message)
        # 自动滚动到底部
        self.merge_log_text.verticalScrollBar().setValue(
            self.merge_log_text.verticalScrollBar().maximum()
        )
    
    def on_merge_finished(self, success, output_file):
        """合并完成"""
        _msg_style = """
            QMessageBox { background-color: #2b2b2b; }
            QMessageBox QLabel { color: #ffffff; font-size: 14px; }
            QPushButton { background-color: #0071e3; color: white; border-radius: 6px; padding: 8px 20px; min-width: 80px; }
        """
        if success:
            self.merge_log_text.append("=" * 50)
            self.merge_log_text.append(f"✨ 【全链路处理成功】")
            self.merge_log_text.append(f"📁 最终文件: {output_file}")
            self.merge_log_text.append("=" * 50)
            
            msg = QMessageBox(self)
            msg.setWindowTitle("合并完成")
            msg.setIcon(QMessageBox.Icon.Information)
            msg.setText("<b style='color: #34c759;'>✅ 视频合并成功！</b>")
            msg.setInformativeText(
                f"<span style='color: #e5e5ea;'>输出文件：</span><br>"
                f"<span style='color: #ffffff;'>{output_file}</span><br><br>"
                f"<span style='color: #e5e5ea;'>是否打开输出文件夹？</span>"
            )
            msg.setStyleSheet(_msg_style)
            msg.setStandardButtons(QMessageBox.StandardButton.Yes | QMessageBox.StandardButton.No)
            msg.setDefaultButton(QMessageBox.StandardButton.Yes)
            msg.button(QMessageBox.StandardButton.Yes).setText("是")
            msg.button(QMessageBox.StandardButton.No).setText("否")
            reply = msg.exec()
            
            if reply == QMessageBox.StandardButton.Yes:
                import subprocess
                if os.path.exists(output_file):
                    subprocess.run(['open', '-R', output_file])
        else:
            self.merge_log_text.append("=" * 50)
            self.merge_log_text.append("❌ 合并失败，请查看日志")
            self.merge_log_text.append("=" * 50)
            
            msg = QMessageBox(self)
            msg.setWindowTitle("合并失败")
            msg.setIcon(QMessageBox.Icon.Critical)
            msg.setText("<b style='color: #ff3b30;'>视频合并失败</b>")
            msg.setInformativeText("<span style='color: #e5e5ea;'>请检查日志信息</span>")
            msg.setStyleSheet(_msg_style)
            msg.exec()

    def map_rect_to_video(self, rect, preview):
        if not hasattr(preview, 'video_size') or preview.video_size.isEmpty() or rect.isNull(): return None
        pix = preview.pixmap(); 
        if not pix: return None
        vw, vh, pw, ph = preview.video_size.width(), preview.video_size.height(), pix.width(), pix.height()
        ox, oy = (preview.width() - pw) / 2, (preview.height() - ph) / 2
        rx = max(0, int((rect.x() - ox) * (vw / pw))); ry = max(0, int((rect.y() - oy) * (vh / ph)))
        rw = min(vw - rx, int(rect.width() * (vw / pw))); rh = min(vh - ry, int(rect.height() * (vh / ph)))
        return (rx, ry, rw, rh)

if __name__ == "__main__":
    import sys; from PyQt6.QtWidgets import QApplication
    app = QApplication(sys.argv); window = MainWindow(); window.show(); sys.exit(app.exec())
