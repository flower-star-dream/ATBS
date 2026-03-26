"""
日志配置 - 类似 Java Spring Boot 的日志配置
支持控制台和文件输出，自动按日期轮转
"""
import logging
import sys
import os
import re
from datetime import datetime, timedelta, timezone
from pathlib import Path
from logging.handlers import TimedRotatingFileHandler
from typing import Optional


class BeijingFormatter(logging.Formatter):
    """北京时间格式化器"""

    def formatTime(self, record, datefmt=None):
        # 转换为北京时间 (UTC+8)
        beijing_time = datetime.now(timezone(timedelta(hours=8)))
        if datefmt:
            return beijing_time.strftime(datefmt)
        return beijing_time.strftime("%Y-%m-%d %H:%M:%S")


class CleanExpiredFilesFilter:
    """
    清理过期日志文件的过滤器
    在每次轮转时检查并删除超过保留天数的日志文件
    """

    def __init__(self, log_dir: Path, max_days: int, filename_pattern: str):
        self.log_dir = log_dir
        self.max_days = max_days
        # 将历史日志模式转换为正则表达式
        # ${spring.application.name}-%d{yyyy-MM-dd}.log -> prediction-service-\d{4}-\d{2}-\d{2}.log
        pattern = filename_pattern.replace("${spring.application.name}", ".*?")
        pattern = pattern.replace("%d{yyyy-MM-dd}", r"\d{4}-\d{2}-\d{2}")
        pattern = pattern.replace(".", r"\.")
        self.regex = re.compile(pattern)

    def __call__(self, record):
        # 每次记录日志时检查是否需要清理（简化处理，每天清理一次）
        current_date = datetime.now(timezone(timedelta(hours=8))).date()
        if not hasattr(self, '_last_clean_date') or self._last_clean_date != current_date:
            self._clean_expired_files()
            self._last_clean_date = current_date
        return True

    def _clean_expired_files(self):
        """清理过期的日志文件"""
        if not self.log_dir.exists():
            return

        cutoff_date = datetime.now(timezone(timedelta(hours=8))) - timedelta(days=self.max_days)

        for file_path in self.log_dir.iterdir():
            if file_path.is_file() and self.regex.match(file_path.name):
                try:
                    # 获取文件修改时间
                    mtime = datetime.fromtimestamp(file_path.stat().st_mtime, timezone(timedelta(hours=8)))
                    if mtime < cutoff_date:
                        file_path.unlink()
                        print(f"[Log] 删除过期日志文件: {file_path.name}")
                except Exception as e:
                    print(f"[Log] 删除日志文件失败: {file_path} - {e}")


def get_log_level(level_str: str) -> int:
    """将日志级别字符串转换为 logging 级别"""
    level_map = {
        "DEBUG": logging.DEBUG,
        "INFO": logging.INFO,
        "WARNING": logging.WARNING,
        "WARN": logging.WARNING,
        "ERROR": logging.ERROR,
        "CRITICAL": logging.CRITICAL,
    }
    return level_map.get(level_str.upper(), logging.INFO)


def setup_logging(settings=None):
    """
    设置日志配置 - 类似 Java 的 %d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n

    Args:
        settings: 应用配置对象，包含日志配置
    """
    # 默认配置
    log_level = logging.INFO
    file_enabled = False
    log_dir = Path("backend/logs/prediction-service")
    log_filename = "prediction-service.log"
    max_history = 30

    # 如果提供了配置，使用配置值
    if settings:
        log_level = get_log_level(getattr(settings, 'log_level', 'INFO'))
        file_enabled = getattr(settings, 'log_file_enabled', False)
        log_dir = getattr(settings, 'log_file_path', log_dir)
        log_filename = getattr(settings, 'log_file_name', log_filename)
        max_history = getattr(settings, 'log_max_history', 30)

    # 创建根日志记录器
    logger = logging.getLogger()
    logger.setLevel(log_level)

    # 清除已有处理器
    logger.handlers.clear()

    # 设置格式化器 - Java 风格
    formatter = BeijingFormatter(
        fmt="%(asctime)s [%(threadName)s] %(levelname)-5s %(name)-36.36s - %(message)s",
        datefmt="%Y-%m-%d %H:%M:%S"
    )

    # 创建控制台处理器
    console_handler = logging.StreamHandler(sys.stdout)
    console_handler.setLevel(log_level)
    console_handler.setFormatter(formatter)
    logger.addHandler(console_handler)

    # 创建文件处理器（如果启用）
    if file_enabled:
        try:
            # 确保日志目录存在
            log_dir = Path(log_dir)
            log_dir.mkdir(parents=True, exist_ok=True)

            # 当前日志文件路径
            current_log_file = log_dir / log_filename

            # 创建按日期轮转的处理器
            # when='midnight' 表示每天午夜轮转
            # interval=1 表示每天
            # backupCount 设为 max_history，但实际清理由自定义逻辑处理
            file_handler = TimedRotatingFileHandler(
                filename=str(current_log_file),
                when='midnight',
                interval=1,
                backupCount=max_history,
                encoding='utf-8'
            )
            file_handler.setLevel(log_level)
            file_handler.setFormatter(formatter)

            # 设置轮转文件名格式
            # 默认格式是 filename.YYYY-MM-DD，我们需要改成 filename-YYYY-MM-DD
            # 通过 suffix 和 extMatch 实现
            file_handler.suffix = "%Y-%m-%d"
            file_handler.extMatch = re.compile(r"^\d{4}-\d{2}-\d{2}$")

            # 添加过期文件清理过滤器
            clean_filter = CleanExpiredFilesFilter(log_dir, max_history, "${spring.application.name}-%d{yyyy-MM-dd}.log")
            clean_filter._last_clean_date = None  # 初始化

            # 包装 emit 方法以便在轮转后清理
            original_emit = file_handler.emit
            def wrapped_emit(record):
                clean_filter(record)  # 触发清理检查
                original_emit(record)
            file_handler.emit = wrapped_emit

            logger.addHandler(file_handler)

            # 记录日志配置信息
            print(f"[Log] 文件日志已启用: {current_log_file}")
            print(f"[Log] 日志级别: {logging.getLevelName(log_level)}")
            print(f"[Log] 历史日志保存天数: {max_history}")

        except Exception as e:
            print(f"[Log] 文件日志初始化失败: {e}")

    # 设置第三方库的日志级别（降低噪音）
    logging.getLogger("uvicorn").setLevel(logging.WARNING)
    logging.getLogger("fastapi").setLevel(logging.WARNING)
    logging.getLogger("nacos").setLevel(logging.WARNING)

    return logger


# 向后兼容
if __name__ == "__main__":
    setup_logging()
    logger = logging.getLogger("test")
    logger.info("日志测试")
