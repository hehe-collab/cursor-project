import sys
import os
from PyQt6.QtWidgets import QApplication
from ui.main_window import MainWindow
from core.logger_config import setup_logging, handle_exception

def main():
    base_path = os.path.dirname(os.path.abspath(__file__))
    logger = setup_logging(base_path)
    logger.info("=== 软件启动 ===")
    
    # 设置全局异常钩子
    sys.excepthook = handle_exception
    
    app = QApplication(sys.argv)
    try:
        window = MainWindow()
        window.show()
        sys.exit(app.exec())
    except Exception as e:
        logger.critical(f"启动过程中发生致命错误: {e}")
        logger.critical(os.sys.exc_info())

if __name__ == "__main__":
    main()
