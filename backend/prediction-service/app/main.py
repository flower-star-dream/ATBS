"""
客流预测服务 - FastAPI 主应用
基于 ARIMA 模型的客流预测微服务
"""
import os
import sys
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

# 添加项目路径
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from app.api.routes import prediction
from app.core.config import settings
from app.core.logging import setup_logging

# 设置日志
setup_logging()


@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    应用生命周期管理
    """
    # 启动时执行
    print(f"=" * 60)
    print(f"客流预测服务启动")
    print(f"版本: {settings.APP_VERSION}")
    print(f"环境: {settings.APP_ENV}")
    print(f"=" * 60)

    # 确保模型目录存在
    os.makedirs(settings.MODEL_PATH, exist_ok=True)
    os.makedirs(settings.DATA_PATH, exist_ok=True)

    yield

    # 关闭时执行
    print("\n客流预测服务关闭")


# 创建 FastAPI 应用
app = FastAPI(
    title=settings.APP_NAME,
    description=settings.APP_DESCRIPTION,
    version=settings.APP_VERSION,
    docs_url="/docs" if settings.APP_ENV == "development" else None,
    redoc_url="/redoc" if settings.APP_ENV == "development" else None,
    lifespan=lifespan
)

# 配置 CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.CORS_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 注册路由
app.include_router(prediction.router, prefix="/api/v1/prediction", tags=["预测服务"])


@app.get("/")
async def root():
    """根路径 - 服务信息"""
    return {
        "name": settings.APP_NAME,
        "version": settings.APP_VERSION,
        "description": settings.APP_DESCRIPTION,
        "docs": "/docs"
    }


@app.get("/health")
async def health():
    """健康检查"""
    return {"status": "ok", "service": settings.APP_NAME}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "app.main:app",
        host=settings.HOST,
        port=settings.PORT,
        reload=settings.APP_ENV == "development",
        workers=1
    )
