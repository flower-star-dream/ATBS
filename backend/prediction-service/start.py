#!/usr/bin/env python3
"""
启动脚本 - 开发环境使用
"""
import uvicorn
from app.core.config import settings

if __name__ == "__main__":
    print(f"启动 {settings.APP_NAME}...")
    print(f"访问文档: http://{settings.HOST}:{settings.PORT}/docs")

    uvicorn.run(
        "app.main:app",
        host=settings.HOST,
        port=settings.PORT,
        reload=True,
        log_level="info"
    )
