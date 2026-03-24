"""
预测服务路由
对应原 Java 的 PredictionController
"""
from fastapi import APIRouter, Query
from typing import Optional

from app.schemas.prediction import (
    PredictionRequest, PredictionResponse,
    TrainingRequest, TrainingResponse,
    ModelInfoResponse
)
from app.services.prediction_service import prediction_service

router = APIRouter()


@router.post("/forecast", response_model=PredictionResponse)
async def forecast(request: PredictionRequest):
    """
    预测未来客流

    - **days**: 预测天数 (1-365)
    - **confidence_level**: 置信水平 (0-1)
    - **auto_train**: 是否自动训练（如果模型不存在）
    """
    return prediction_service.predict(request)


@router.get("/forecast", response_model=PredictionResponse)
async def quick_forecast(
    days: int = Query(default=20, ge=1, le=365, description="预测天数"),
    confidence_level: float = Query(default=0.95, ge=0.0, le=1.0, description="置信水平"),
    auto_train: bool = Query(default=False, description="是否自动训练")
):
    """
    快速预测（默认20天）

    - **days**: 预测天数，默认20天
    - **confidence_level**: 置信水平，默认0.95
    - **auto_train**: 是否自动训练，默认False
    """
    request = PredictionRequest(
        days=days,
        confidence_level=confidence_level,
        auto_train=auto_train
    )
    return prediction_service.predict(request)


@router.post("/train", response_model=TrainingResponse)
async def train(request: TrainingRequest):
    """
    训练模型

    - **p**: 自回归阶数 (0-10)
    - **d**: 差分阶数 (0-3)
    - **q**: 移动平均阶数 (0-10)
    - **auto_optimize**: 是否自动寻找最优参数
    - **test_size**: 验证集大小 (7-90)
    """
    return prediction_service.train(request)


@router.get("/train", response_model=TrainingResponse)
async def quick_train(
    p: int = Query(default=5, ge=0, le=10, description="自回归阶数 p"),
    d: int = Query(default=1, ge=0, le=3, description="差分阶数 d"),
    q: int = Query(default=0, ge=0, le=10, description="移动平均阶数 q"),
    auto_optimize: bool = Query(default=False, description="是否自动寻找最优参数"),
    test_size: int = Query(default=30, ge=7, le=90, description="验证集大小")
):
    """
    快速训练（使用默认参数）

    - **p**: 自回归阶数，默认5
    - **d**: 差分阶数，默认1
    - **q**: 移动平均阶数，默认0
    - **auto_optimize**: 是否自动寻找最优参数，默认False
    - **test_size**: 验证集大小，默认30
    """
    request = TrainingRequest(
        p=p,
        d=d,
        q=q,
        auto_optimize=auto_optimize,
        test_size=test_size
    )
    return prediction_service.train(request)


@router.get("/model-info", response_model=ModelInfoResponse)
async def get_model_info():
    """
    获取模型信息

    返回当前加载的 ARIMA 模型信息，包括参数、AIC/BIC、训练时间等
    """
    return prediction_service.get_model_info()


@router.get("/health")
async def health():
    """
    健康检查

    返回服务健康状态
    """
    return {"status": "ok"}
