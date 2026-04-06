import cv2
import numpy as np
import logging

try:
    from Foundation import NSURL, NSArray
    from Vision import VNImageRequestHandler, VNRecognizeTextRequest
    HAS_VISION = True
except ImportError:
    HAS_VISION = False

logger = logging.getLogger("AI_Drama")

class OCREngine:
    def __init__(self):
        self.enabled = HAS_VISION
        if not HAS_VISION:
            logger.warning("⚠️ 警告: 未检测到 macOS Vision 框架")

    def recognize_text(self, image, should_continue=None):
        """
        标准识别模式 (回退稳定版)
        should_continue: 可选，返回 False 时跳过本次 Vision（用于停止键尽快生效）
        """
        if should_continue is not None and not should_continue():
            return []
        if not self.enabled or image.size == 0:
            return []

        # 1. 基础预处理
        gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
        
        # 2. 3倍放大提高识别率
        processed = cv2.resize(gray, None, fx=3.0, fy=3.0, interpolation=cv2.INTER_CUBIC)
        
        # 3. 简单的拉普拉斯锐化
        kernel = np.array([[0, -1, 0], [-1, 5, -1], [0, -1, 0]])
        processed = cv2.filter2D(processed, -1, kernel)

        results = []

        def completion_handler(request, error):
            if error:
                return
            observations = request.results()
            for observation in observations:
                top_candidate = observation.topCandidates_(1)[0]
                text = top_candidate.string()
                # 剔除常见的噪点字符
                clean_text = "".join([c for c in text if c not in "¾⑨/\\_—|~{}[]{}"])
                clean_text = clean_text.strip(" .,-")
                # 方案A：多字保留；单字仅保留 CJK 汉字（好、啊、嗯等），过滤单字噪点（|、l、。等）
                if len(clean_text) > 1:
                    results.append(clean_text)
                elif len(clean_text) == 1 and '\u4e00' <= clean_text <= '\u9fff':
                    results.append(clean_text)

        try:
            import tempfile
            with tempfile.NamedTemporaryFile(suffix=".jpg") as tmp:
                cv2.imwrite(tmp.name, processed)
                url = NSURL.fileURLWithPath_(tmp.name)
                
                handler = VNImageRequestHandler.alloc().initWithURL_options_(url, None)
                request = VNRecognizeTextRequest.alloc().initWithCompletionHandler_(completion_handler)
                
                # 恢复默认：开启语言纠错
                request.setRecognitionLevel_(0) # 0 是准确模式
                langs = NSArray.arrayWithArray_(["zh-Hans", "en-US"])
                request.setRecognitionLanguages_(langs)
                request.setUsesLanguageCorrection_(True) # 恢复语言纠错
                
                handler.performRequests_error_([request], None)
        except Exception as e:
            logger.error(f"Vision OCR 调用失败: {e}")
            
        return results
