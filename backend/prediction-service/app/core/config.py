"""
应用配置
"""
import os
from pathlib import Path
from typing import List

from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """应用配置类"""

    # 应用信息
    APP_NAME: str = "客流预测服务"
    APP_VERSION: str = "1.0.0"
    APP_DESCRIPTION: str = "基于 ARIMA 模型的客流预测微服务"
    APP_ENV: str = os.getenv("APP_ENV", "development")

    # 服务器配置
    HOST: str = os.getenv("HOST", "0.0.0.0")
    PORT: int = int(os.getenv("PORT", "8080"))

    # CORS 配置
    CORS_ORIGINS: List[str] = ["*"]

    # 预测服务配置
    DEFAULT_DAYS: int = 20
    MAX_DAYS: int = 365
    CONFIDENCE_LEVEL: float = 0.95

    # 路径配置
    BASE_DIR: Path = Path(__file__).resolve().parent.parent.parent
    DATA_PATH: Path = BASE_DIR / "data"
    MODEL_PATH: Path = BASE_DIR / "model"

    # 数据文件路径
    DATA_FILE: Path = DATA_PATH / "airline-passengers.csv"

    # ARIMA 默认参数
    DEFAULT_P: int = 5
    DEFAULT_D: int = 1
    DEFAULT_Q: int = 0

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


# 全局配置实例
settings = Settings()
