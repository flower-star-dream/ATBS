"""
预测服务路由
对应原 Java 的 PredictionController
使用统一的 Result 响应格式
"""
from fastapi import APIRouter, Depends, Query

from app.schemas.prediction import (
    PredictionRequest, PredictionResponse,
    TrainingResponse,
    ModelInfoResponse
)
from app.services.prediction_service import prediction_service
from app.core.result import Result, success_result
from app.core.jwt_handler import JWTContext, get_jwt_context
from app.core.auth import require_auth

router = APIRouter()


@router.post("/forecast", response_model=Result[PredictionResponse])
async def forecast(
    request: PredictionRequest,
    ctx: JWTContext = Depends(get_jwt_context)
):
    """
    预测未来客流

    - **days**: 预测天数 (1-365)
    - **confidence_level**: 置信水平 (0-1)
    - **auto_train**: 是否自动训练（如果模型不存在）
    """
    result = prediction_service.predict(request)
    if result.status == "error":
        return Result.error(message=result.message)
    return success_result(data=result)


@router.get("/forecast", response_model=Result[PredictionResponse])
async def quick_forecast(
    days: int = Query(default=20, ge=1, le=365, description="预测天数"),
    confidence_level: float = Query(default=0.95, ge=0.0, le=1.0, description="置信水平"),
    auto_train: bool = Query(default=False, description="是否自动训练"),
    ctx: JWTContext = Depends(get_jwt_context)
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
    result = prediction_service.predict(request)
    if result.status == "error":
        return Result.error(message=result.message)
    return success_result(data=result)


@router.post("/train", response_model=Result[TrainingResponse])
async def train(ctx: JWTContext = Depends(require_auth)):
    """
    训练模型（需要登录权限）

    模型自动寻找最优参数，调用方无需关心具体实现
    """
    result = prediction_service.train()
    if result.status == "error":
        return Result.error(message=result.message)
    return success_result(data=result)

@router.get("/model-info", response_model=Result[ModelInfoResponse])
async def get_model_info(ctx: JWTContext = Depends(get_jwt_context)):
    """
    获取模型信息

    返回当前加载的 ARIMA 模型信息，包括参数、AIC/BIC、训练时间等
    """
    result = prediction_service.get_model_info()
    return success_result(data=result)


@router.get("/health")
async def health():
    """
    健康检查

    返回服务健康状态
    """
    return {"status": "ok"}
