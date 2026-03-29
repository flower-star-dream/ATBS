#!/usr/bin/env python3
"""
启动脚本 - 开发环境使用
"""
import uvicorn
from app.core.config import settings

if __name__ == "__main__":
    print(f"启动 {settings.app_name}...")
    print(f"访问文档: http://{settings.host}:{settings.port}/doc.html")

    uvicorn.run(
        "app.main:app",
        host=settings.host,
        port=settings.port,
        reload=True,
        log_level="info"
    )
