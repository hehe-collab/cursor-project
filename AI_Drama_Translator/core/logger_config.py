import logging
import os
import sys
import traceback

def setup_logging(base_path):
    log_file = os.path.join(base_path, "app.log")
    
    # 配置日志格式
    logging.basicConfig(
        level=logging.DEBUG,
        format='%(asctime)s [%(levelname)s] %(name)s: %(message)s',
        handlers=[
            logging.FileHandler(log_file, encoding='utf-8'),
            logging.StreamHandler(sys.stdout)
        ]
    )
    return logging.getLogger("AI_Drama")

def handle_exception(exc_type, exc_value, exc_traceback):
    """全局未捕获异常的处理句柄"""
    if issubclass(exc_type, KeyboardInterrupt):
        sys.__excepthook__(exc_type, exc_value, exc_traceback)
        return
    
    logger = logging.getLogger("AI_Drama")
    logger.critical("未捕获的严重崩溃!", exc_info=(exc_type, exc_value, exc_traceback))

