from PyQt6.QtWidgets import QWidget, QVBoxLayout, QListWidget, QListWidgetItem, QPushButton, QLabel, QLineEdit, QHBoxLayout, QTextEdit, QMessageBox
from PyQt6.QtCore import Qt, pyqtSignal, QSize
from PyQt6.QtGui import QColor, QFont
import logging

logger = logging.getLogger("AI_Drama")

class SubtitleItemWidget(QWidget):
    """单条字幕的编辑组件"""
    textChanged = pyqtSignal(int, str)  # (index, new_text)
    clicked = pyqtSignal(float)  # (start_time)
    deleteRequested = pyqtSignal(int)  # (index) - 新增删除信号
    
    def __init__(self, index, start_time, end_time, text, parent=None):
        super().__init__(parent)
        self.index = index
        self.start_time = start_time
        self.end_time = end_time
        self.is_highlighted = False
        
        layout = QVBoxLayout(self)
        layout.setContentsMargins(8, 6, 8, 6)
        layout.setSpacing(4)
        
        # 顶部：时间码 + 删除按钮
        top_layout = QHBoxLayout()
        top_layout.setSpacing(5)
        
        time_str = f"{self.format_time(start_time)} → {self.format_time(end_time)}"
        self.time_label = QLabel(time_str)
        self.time_label.setStyleSheet("color: #007aff; font-size: 11px; font-weight: bold;")
        top_layout.addWidget(self.time_label)
        
        top_layout.addStretch()
        
        # 删除按钮
        self.btn_delete = QPushButton("🗑")
        self.btn_delete.setFixedSize(24, 24)
        self.btn_delete.setStyleSheet("""
            QPushButton {
                background-color: #ff3b30;
                color: white;
                border-radius: 4px;
                font-size: 12px;
                padding: 2px;
            }
            QPushButton:hover {
                background-color: #ff453a;
            }
        """)
        self.btn_delete.clicked.connect(lambda: self.deleteRequested.emit(self.index))
        top_layout.addWidget(self.btn_delete)
        
        layout.addLayout(top_layout)
        
        # 文本编辑框
        self.text_edit = QTextEdit()
        self.text_edit.setPlainText(text)
        self.text_edit.setMaximumHeight(55)  # 稍微降低高度，让布局更紧凑
        self.text_edit.setMinimumHeight(40)  # 设置最小高度
        self.text_edit.setStyleSheet("""
            QTextEdit {
                border: 1px solid #c6c6c8;
                border-radius: 4px;
                padding: 4px;
                font-size: 13px;
                background: white;
                color: #1c1c1e;  /* 深灰色文字，确保在白色背景上清晰可见 */
            }
            QTextEdit:focus {
                border: 2px solid #007aff;
            }
        """)
        self.text_edit.textChanged.connect(self.on_text_changed)
        layout.addWidget(self.text_edit)
        
        # 设置整个组件的固定高度
        self.setFixedHeight(90)
        
        self.update_style()
    
    def format_time(self, seconds):
        """格式化时间：MM:SS"""
        mins = int(seconds // 60)
        secs = int(seconds % 60)
        return f"{mins:02d}:{secs:02d}"
    
    def on_text_changed(self):
        new_text = self.text_edit.toPlainText()
        self.textChanged.emit(self.index, new_text)
    
    def set_highlight(self, highlighted):
        """高亮当前播放的字幕"""
        self.is_highlighted = highlighted
        self.update_style()
    
    def update_style(self):
        if self.is_highlighted:
            self.setStyleSheet("""
                SubtitleItemWidget {
                    background: #e3f2fd;
                    border-left: 4px solid #007aff;
                    border-radius: 4px;
                }
            """)
        else:
            self.setStyleSheet("""
                SubtitleItemWidget {
                    background: #f9f9f9;
                    border-left: 4px solid transparent;
                    border-radius: 4px;
                }
                SubtitleItemWidget:hover {
                    background: #f0f0f0;
                }
            """)
    
    def mousePressEvent(self, event):
        """点击跳转到该字幕时间"""
        if event.button() == Qt.MouseButton.LeftButton:
            self.clicked.emit(self.start_time)
        super().mousePressEvent(event)


class SubtitleEditor(QWidget):
    """字幕编辑器主组件"""
    subtitleClicked = pyqtSignal(float)  # 点击字幕，发送开始时间
    subtitleChanged = pyqtSignal()  # 字幕内容变化
    exportRequested = pyqtSignal(str)  # 请求导出，发送视频路径
    
    def __init__(self, parent=None):
        super().__init__(parent)
        self.subtitle_data = []  # [(start, end, text), ...]
        self.subtitle_widgets = []
        self.current_video_path = None
        
        layout = QVBoxLayout(self)
        layout.setContentsMargins(0, 0, 0, 0)
        layout.setSpacing(10)
        
        # 标题栏
        header = QWidget()
        header_layout = QHBoxLayout(header)
        header_layout.setContentsMargins(10, 10, 10, 10)
        self.title_label = QLabel("📝 <b>字幕预览区</b>")
        self.title_label.setStyleSheet("font-size: 14px; color: #1c1c1e;")
        self.count_label = QLabel("共 0 条字幕")
        self.count_label.setStyleSheet("font-size: 12px; color: #8e8e93;")
        header_layout.addWidget(self.title_label)
        header_layout.addStretch()
        header_layout.addWidget(self.count_label)
        layout.addWidget(header)
        
        # 字幕列表
        self.list_widget = QListWidget()
        self.list_widget.setStyleSheet("""
            QListWidget {
                border: 1px solid #c6c6c8;
                border-radius: 8px;
                background: white;
            }
            QListWidget::item {
                border: none;
                padding: 3px;
                margin: 2px 0px;
            }
            QListWidget::item:selected {
                background: transparent;
            }
        """)
        layout.addWidget(self.list_widget)
        
        # 底部按钮
        btn_layout = QHBoxLayout()
        self.btn_export = QPushButton("💾 导出字幕")
        self.btn_export.setStyleSheet("""
            QPushButton {
                background-color: #34c759;
                color: white;
                border-radius: 6px;
                padding: 10px 20px;
                font-size: 14px;
                font-weight: bold;
            }
            QPushButton:hover {
                background-color: #30b350;
            }
            QPushButton:disabled {
                background-color: #c6c6c8;
            }
        """)
        self.btn_export.setEnabled(False)
        self.btn_export.clicked.connect(self.on_export_clicked)
        btn_layout.addStretch()
        btn_layout.addWidget(self.btn_export)
        layout.addLayout(btn_layout)
    
    def load_subtitles(self, video_path, subtitle_data):
        """加载字幕数据"""
        self.current_video_path = video_path
        # 深拷贝列表中的每个元组，避免引用问题
        self.subtitle_data = [(start, end, text) for start, end, text in subtitle_data] if subtitle_data else []
        self.subtitle_widgets = []
        self.list_widget.clear()
        
        logger.info(f"[SubtitleEditor] load_subtitles 被调用，接收到 {len(self.subtitle_data)} 条字幕")
        
        if not subtitle_data:
            self.count_label.setText("暂无字幕")
            self.btn_export.setEnabled(False)
            return
        
        self.count_label.setText(f"共 {len(subtitle_data)} 条字幕")
        self.btn_export.setEnabled(True)
        
        for i, (start, end, text) in enumerate(subtitle_data):
            logger.debug(f"[SubtitleEditor] 创建第 {i+1} 条字幕组件: {start:.2f}s -> {end:.2f}s")
            item_widget = SubtitleItemWidget(i, start, end, text)
            item_widget.textChanged.connect(self.on_subtitle_text_changed)
            item_widget.clicked.connect(self.subtitleClicked.emit)
            item_widget.deleteRequested.connect(self.on_subtitle_delete_requested)  # 连接删除信号
            
            item = QListWidgetItem(self.list_widget)
            item.setSizeHint(QSize(350, 90))  # 设置固定宽度和高度
            self.list_widget.addItem(item)
            self.list_widget.setItemWidget(item, item_widget)
            
            self.subtitle_widgets.append(item_widget)
        
        logger.info(f"[SubtitleEditor] 字幕编辑器加载完成: {len(self.subtitle_data)} 条字幕，创建了 {len(self.subtitle_widgets)} 个组件")
    
    def on_subtitle_text_changed(self, index, new_text):
        """字幕文本改变"""
        if 0 <= index < len(self.subtitle_data):
            start, end, _ = self.subtitle_data[index]
            self.subtitle_data[index] = (start, end, new_text)
            self.subtitleChanged.emit()
            logger.debug(f"字幕 {index+1} 已修改: {new_text[:20]}...")
    
    def on_subtitle_delete_requested(self, index):
        """删除字幕请求"""
        if not (0 <= index < len(self.subtitle_data)):
            logger.warning(f"[删除字幕] 索引超出范围: {index}")
            return
        
        # 确认删除
        start, end, text = self.subtitle_data[index]
        reply = QMessageBox.question(
            self, 
            "确认删除", 
            f"确定要删除这条字幕吗？\n\n"
            f"⏱ {self.format_time(start)} → {self.format_time(end)}\n"
            f"📝 {text[:50]}{'...' if len(text) > 50 else ''}",
            QMessageBox.StandardButton.Yes | QMessageBox.StandardButton.No,
            QMessageBox.StandardButton.No
        )
        
        if reply == QMessageBox.StandardButton.Yes:
            # 删除数据
            deleted_text = self.subtitle_data.pop(index)
            logger.info(f"[删除字幕] 已删除第 {index+1} 条字幕: {deleted_text[2][:30]}...")
            
            # 重新加载（重建所有组件）
            self.reload_subtitles()
            
            # 通知变化
            self.subtitleChanged.emit()
            
            logger.info(f"[删除字幕] 剩余 {len(self.subtitle_data)} 条字幕")
    
    def reload_subtitles(self):
        """重新加载字幕列表（用于删除后刷新）"""
        if not self.current_video_path:
            return
        
        # 保存当前数据
        current_data = self.subtitle_data.copy() if isinstance(self.subtitle_data, list) else list(self.subtitle_data)
        current_path = self.current_video_path
        
        # 重新加载
        self.load_subtitles(current_path, current_data)
    
    def format_time(self, seconds):
        """格式化时间为 MM:SS"""
        mins = int(seconds // 60)
        secs = int(seconds % 60)
        return f"{mins:02d}:{secs:02d}"
    
    def highlight_subtitle_at_time(self, current_time):
        """高亮当前时间点的字幕"""
        found = False
        for i, widget in enumerate(self.subtitle_widgets):
            # 修复时间匹配逻辑：使用更精确的范围判断
            if widget.start_time <= current_time < widget.end_time:
                widget.set_highlight(True)
                # 自动滚动到当前字幕
                self.list_widget.scrollToItem(self.list_widget.item(i))
                found = True
                logger.debug(f"[字幕同步] 当前时间 {current_time:.2f}s 匹配到字幕 {i+1}: {widget.start_time:.2f}s - {widget.end_time:.2f}s")
            else:
                widget.set_highlight(False)
        
        if not found:
            logger.debug(f"[字幕同步] 当前时间 {current_time:.2f}s 未匹配到任何字幕")
    
    def clear(self):
        """清空编辑器"""
        self.current_video_path = None
        self.subtitle_data = []
        self.subtitle_widgets = []
        self.list_widget.clear()
        self.count_label.setText("共 0 条字幕")
        self.btn_export.setEnabled(False)
    
    def on_export_clicked(self):
        """导出按钮点击"""
        if self.current_video_path:
            self.exportRequested.emit(self.current_video_path)
    
    def get_subtitle_data(self):
        """获取当前编辑后的字幕数据"""
        logger.info(f"[SubtitleEditor] get_subtitle_data 被调用，当前有 {len(self.subtitle_data)} 条字幕")
        for i, (start, end, text) in enumerate(self.subtitle_data):
            logger.debug(f"  [{i+1}] {start:.2f}s -> {end:.2f}s: {text[:30]}...")
        return self.subtitle_data

