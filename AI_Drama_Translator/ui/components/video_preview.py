from PyQt6.QtWidgets import QLabel
from PyQt6.QtCore import Qt, QRect, QPoint, pyqtSignal, QSize
from PyQt6.QtGui import QPainter, QPen, QColor, QFont, QFontMetrics
import logging

logger = logging.getLogger("AI_Drama")

class VideoPreview(QLabel):
    def __init__(self, color=QColor(255, 0, 0), parent=None):
        super().__init__(parent)
        self.setAlignment(Qt.AlignmentFlag.AlignCenter)
        self.setStyleSheet("background-color: #1c1c1e;")  # 深灰色背景，更柔和
        self.setMouseTracking(True)
        self.setScaledContents(False)  # 不自动缩放，我们手动控制
        
        # 独立存储三套选框，互不干扰
        self.ocr_rect = QRect()
        self.erase_rects = []  # 改为列表，支持多区域擦除
        self.current_erase_rect = QRect()  # 当前正在绘制的擦除框
        self.burn_rect = QRect()
        
        self.start_point = QPoint()
        self.is_drawing = False
        self.draw_color = color
        self.mode = "ocr" # ocr, erase, burn
        self.video_size = QSize(0, 0)
        self.current_time = 0.0
        self.srt_data = []
        self.font_config = {"size": 18, "color": "白色"}
        self.ocr_subtitle_data = []  # OCR模式的字幕数据（用于叠加显示）
        self.ocr_current_time = 0.0

    @property
    def target_rect(self):
        """根据当前模式返回对应的选框"""
        if self.mode == "ocr": return self.ocr_rect
        if self.mode == "erase": return self.current_erase_rect  # 返回正在绘制的框
        return self.burn_rect

    @target_rect.setter
    def target_rect(self, value):
        if self.mode == "ocr": self.ocr_rect = value
        elif self.mode == "erase": self.current_erase_rect = value
        else: self.burn_rect = value

    def set_mode(self, mode):
        self.mode = mode
        self.draw_color = QColor(0, 113, 227) if mode == "burn" else QColor(255, 0, 0)
        self.update()

    def set_srt_data(self, data):
        self.srt_data = data
        self.update()

    def set_current_time(self, time):
        self.current_time = time
        self.update()
    
    def set_current_subtitle_time(self, time):
        """设置OCR模式的字幕当前时间"""
        self.ocr_current_time = time
        self.update()  # 触发重绘，显示当前时间的字幕
    
    def load_ocr_subtitles(self, subtitle_data):
        """加载OCR模式的字幕数据用于叠加显示"""
        self.ocr_subtitle_data = subtitle_data
        logger.info(f"[VideoPreview] load_ocr_subtitles 被调用，加载 {len(subtitle_data)} 条字幕")
        if subtitle_data:
            logger.info(f"[VideoPreview] 第一条字幕: {subtitle_data[0][0]:.2f}s -> {subtitle_data[0][1]:.2f}s: {subtitle_data[0][2][:30]}")

    def mousePressEvent(self, event):
        if event.button() == Qt.MouseButton.LeftButton:
            self.start_point = event.position().toPoint()
            if self.mode == "burn":
                # 压制模式：点击时立即在当前高度创建一个“宽度占满、居中”的初始线
                self.enforce_burn_alignment(self.start_point.y(), 10)
            else:
                self.target_rect = QRect(self.start_point, QSize())
            self.is_drawing = True
            self.update()

    def mouseMoveEvent(self, event):
        if self.is_drawing:
            pos = event.position().toPoint()
            if self.mode == "burn":
                # 压制模式：只取鼠标的 y 轴，高度根据位移计算，x 轴永远锁死
                new_y = min(pos.y(), self.start_point.y())
                new_h = max(10, abs(pos.y() - self.start_point.y()))
                self.enforce_burn_alignment(new_y, new_h)
            else:
                self.target_rect = QRect(self.start_point, pos).normalized()
            self.update()

    def enforce_burn_alignment(self, y, h):
        """强制蓝框水平居中，且宽度为画幅的 90%"""
        pix = self.pixmap()
        if not pix or pix.isNull(): return
        
        pw = pix.width()
        ph = pix.height()
        # 计算视频在黑框中的起始偏移
        offset_x = (self.width() - pw) / 2
        offset_y = (self.height() - ph) / 2
        
        # 强制宽度和 X 坐标
        rw = int(pw * 0.9)
        rx = int(offset_x + (pw - rw) / 2)
        
        # 边界检查，防止 y 超出视频范围
        y = max(int(offset_y), y)
        if y + h > offset_y + ph:
            h = int(offset_y + ph - y)
            
        self.burn_rect = QRect(rx, y, rw, max(10, h))

    def mouseReleaseEvent(self, event):
        if event.button() == Qt.MouseButton.LeftButton:
            self.is_drawing = False
            
            # 擦除模式：松开鼠标后，将当前框添加到列表中
            if self.mode == "erase" and not self.current_erase_rect.isNull():
                # 检查框是否足够大（避免误触）
                if self.current_erase_rect.width() > 10 and self.current_erase_rect.height() > 10:
                    self.erase_rects.append(self.current_erase_rect)
                    self.current_erase_rect = QRect()  # 重置当前框，准备绘制下一个
                    logger.info(f"已添加擦除框，当前共 {len(self.erase_rects)} 个擦除区域")
            
            self.update()
        elif event.button() == Qt.MouseButton.RightButton:
            # 右键：清除最后一个擦除框
            if self.mode == "erase" and self.erase_rects:
                self.erase_rects.pop()
                logger.info(f"已删除最后一个擦除框，剩余 {len(self.erase_rects)} 个")
                self.update()

    def paintEvent(self, event):
        super().paintEvent(event)
        painter = QPainter(self)
        painter.setRenderHint(QPainter.RenderHint.Antialiasing)
        
        pix = self.pixmap()
        if not pix or pix.isNull(): 
            return

        # 擦除模式：绘制所有已完成的擦除框 + 当前正在绘制的框
        if self.mode == "erase":
            # 绘制所有已完成的擦除框
            for i, rect in enumerate(self.erase_rects):
                if not rect.isNull():
                    # 绘制实线边框
                    pen = QPen(self.draw_color, 2, Qt.PenStyle.SolidLine)
                    painter.setPen(pen)
                    painter.drawRect(rect)
                    
                    # 绘制虚线边框
                    dash_pen = QPen(self.draw_color, 1, Qt.PenStyle.DashLine)
                    painter.setPen(dash_pen)
                    painter.drawRect(rect)
                    
                    # 半透明填充
                    overlay_color = QColor(self.draw_color)
                    overlay_color.setAlpha(40)
                    painter.fillRect(rect, overlay_color)
                    
                    # 绘制序号标签
                    label_color = QColor(255, 255, 255)
                    label_bg = QColor(self.draw_color)
                    label_bg.setAlpha(200)
                    
                    label_rect = QRect(rect.x() + 5, rect.y() + 5, 30, 25)
                    painter.fillRect(label_rect, label_bg)
                    painter.setPen(label_color)
                    painter.setFont(QFont("Arial", 12, QFont.Weight.Bold))
                    painter.drawText(label_rect, Qt.AlignmentFlag.AlignCenter, str(i + 1))
            
            # 绘制当前正在绘制的框（用不同颜色区分）
            if not self.current_erase_rect.isNull():
                pen = QPen(QColor(255, 165, 0), 3, Qt.PenStyle.SolidLine)  # 橙色，更粗
                painter.setPen(pen)
                painter.drawRect(self.current_erase_rect)
                
                overlay_color = QColor(255, 165, 0)
                overlay_color.setAlpha(60)
                painter.fillRect(self.current_erase_rect, overlay_color)
        
        # 非擦除模式：绘制单个选框
        else:
            rect = self.target_rect
            if not rect.isNull():
                # 最终绘图保险：如果在压制模式，绘图前最后一次纠偏 x 坐标
                if self.mode == "burn":
                    pw = pix.width()
                    ox = (self.width() - pw) / 2
                    rw = int(pw * 0.9)
                    rx = int(ox + (pw - rw) / 2)
                    rect.setX(rx)
                    rect.setWidth(rw)

                pen = QPen(self.draw_color, 2, Qt.PenStyle.SolidLine)
                painter.setPen(pen)
                painter.drawRect(rect)
                
                dash_pen = QPen(self.draw_color, 1, Qt.PenStyle.DashLine)
                painter.setPen(dash_pen)
                painter.drawRect(rect)
                
                overlay_color = QColor(self.draw_color)
                overlay_color.setAlpha(40)
                painter.fillRect(rect, overlay_color)

        # OCR 模式字幕叠加显示
        if self.mode == "ocr" and self.ocr_subtitle_data:
            current_text = ""
            # 使用半开区间 [start, end) 与字幕编辑器保持一致
            for start, end, text in self.ocr_subtitle_data:
                if start <= self.ocr_current_time < end:
                    current_text = text
                    logger.debug(f"[VideoPreview] 匹配到字幕: {self.ocr_current_time:.2f}s -> {text[:30]}")
                    break
            
            # 只在找到匹配字幕时显示
            if current_text:
                # 在视频底部居中显示字幕
                pw = pix.width()
                ph = pix.height()
                offset_x = (self.width() - pw) / 2
                offset_y = (self.height() - ph) / 2
                
                # 字幕文字样式
                font_size = max(20, int(ph * 0.04))  # 字体大小根据视频高度自适应，至少20px
                font = QFont("Arial Unicode MS", font_size, QFont.Weight.Bold)
                painter.setFont(font)
                
                # 计算文本尺寸
                fm = painter.fontMetrics()
                text_rect = fm.boundingRect(0, 0, int(pw * 0.9), int(ph * 0.3), 
                                           Qt.AlignmentFlag.AlignCenter | Qt.TextFlag.TextWordWrap, 
                                           current_text)
                
                # 字幕位置：底部偏上一点（避免被OCR框遮挡）
                text_height = text_rect.height() + 20  # 额外留出20px边距
                subtitle_x = int(offset_x + (pw - text_rect.width()) / 2 - 10)
                subtitle_y = int(offset_y + ph * 0.75)  # 从底部85%改为75%，避开OCR框
                subtitle_rect = QRect(subtitle_x, subtitle_y, text_rect.width() + 20, text_height)
                
                # 绘制半透明黑色背景
                bg_color = QColor(0, 0, 0, 200)  # 增加不透明度
                painter.fillRect(subtitle_rect, bg_color)
                
                # 绘制黑色描边
                painter.setPen(QPen(Qt.GlobalColor.black, 4))  # 更粗的描边
                for dx in [-2, 0, 2]:
                    for dy in [-2, 0, 2]:
                        if dx != 0 or dy != 0:
                            painter.drawText(subtitle_rect, 
                                           Qt.AlignmentFlag.AlignCenter | Qt.TextFlag.TextWordWrap, 
                                           current_text)
                
                # 黄色文字（传统字幕颜色）
                painter.setPen(QColor(255, 255, 0))  # 亮黄色
                painter.drawText(subtitle_rect, 
                               Qt.AlignmentFlag.AlignCenter | Qt.TextFlag.TextWordWrap, 
                               current_text)
                
                logger.info(f"[VideoPreview] 已绘制字幕: pos=({subtitle_x},{subtitle_y}), size=({subtitle_rect.width()},{subtitle_rect.height()}), text='{current_text[:20]}...'")
        
        # 字幕压制预览逻辑
        elif self.mode == "burn" and self.srt_data and not self.burn_rect.isNull():
            current_text = ""
            # 使用半开区间 [start, end) 与字幕编辑器保持一致
            for start, end, text in self.srt_data:
                if start <= self.current_time < end:
                    current_text = text
                    break
            
            # 只在找到匹配字幕时显示
            if current_text:
                font_size = self.font_config.get("size", 18)
                font = QFont("Arial Unicode MS", font_size)
                painter.setFont(font)
                
                c_map = {"白色": Qt.GlobalColor.white, "黄色": Qt.GlobalColor.yellow, "红色": Qt.GlobalColor.red, "绿色": Qt.GlobalColor.green}
                text_color = c_map.get(self.font_config.get("color", "白色"), Qt.GlobalColor.white)
                
                stroke_width = max(1, font_size // 15)
                painter.setPen(QPen(Qt.GlobalColor.black, stroke_width * 2))
                for dx, dy in [(-1, -1), (1, -1), (-1, 1), (1, 1)]:
                    painter.drawText(self.burn_rect.translated(dx, dy), Qt.AlignmentFlag.AlignCenter | Qt.TextFlag.TextWordWrap, current_text)
                
                painter.setPen(text_color)
                painter.drawText(self.burn_rect, Qt.AlignmentFlag.AlignCenter | Qt.TextFlag.TextWordWrap, current_text)

    def clear(self):
        self.ocr_rect = QRect()
        self.erase_rects = []
        self.current_erase_rect = QRect()
        self.burn_rect = QRect()
        self.srt_data = []
        self.current_time = 0.0
        self.update()
    
    def clear_erase_rects(self):
        """清空所有擦除框"""
        self.erase_rects = []
        self.current_erase_rect = QRect()
        self.update()
        logger.info("已清空所有擦除框")
    
    def resizeEvent(self, event):
        """窗口大小变化时的处理"""
        super().resizeEvent(event)
        # 触发重绘，确保选框位置正确
        self.update()
