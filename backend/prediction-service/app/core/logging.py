"""
日志配置
"""
import logging
import sys
from datetime import datetime, timedelta, timezone


class BeijingFormatter(logging.Formatter):
    """北京时间格式化器"""

    def formatTime(self, record, datefmt=None):
        # 转换为北京时间 (UTC+8)
        beijing_time = datetime.now(timezone(timedelta(hours=8)))
        if datefmt:
            return beijing_time.strftime(datefmt)
        return beijing_time.strftime("%Y-%m-%d %H:%M:%S")


def setup_logging():
    """设置日志配置"""
    # 创建根日志记录器
    logger = logging.getLogger()
    logger.setLevel(logging.INFO)

    # 清除已有处理器
    logger.handlers.clear()

    # 创建控制台处理器
    console_handler = logging.StreamHandler(sys.stdout)
    console_handler.setLevel(logging.INFO)

    # 设置格式化器
    formatter = BeijingFormatter(
        fmt="%(asctime)s [%(levelname)s] %(name)s - %(message)s",
        datefmt="%Y-%m-%d %H:%M:%S"
    )
    console_handler.setFormatter(formatter)

    # 添加处理器
    logger.addHandler(console_handler)

    # 设置第三方库的日志级别
    logging.getLogger("uvicorn").setLevel(logging.WARNING)
    logging.getLogger("fastapi").setLevel(logging.WARNING)
