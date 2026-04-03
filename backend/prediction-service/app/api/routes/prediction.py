"""
预测服务路由（异步化版本）
对应原 Java 的 PredictionController
使用统一的 Result 响应格式
"""
import logging
from fastapi import APIRouter, Depends, Query, Body, HTTPException
from typing import Optional

from app.schemas.prediction import (
    PredictionRequest, PredictionResponse,
    TrainingResponse,
    ModelInfoResponse,
    TrainingTaskCreateResponse,
    TrainingTaskResponse,
    TrainingTaskListResponse
)
# 使用新的异步预测服务
from app.services.async_prediction_service import async_prediction_service
from app.services.auto_retrain_service import auto_retrain_service, init_auto_retrain, close_auto_retrain
from app.core.result import Result, success_result
from app.core.jwt_handler import JWTContext, get_jwt_context
from app.core.auth import require_auth
# 使用新的异步任务管理器
from app.core.async_task_manager import async_task_manager

router = APIRouter()
logger = logging.getLogger(__name__)


@router.post("/forecast", response_model=Result[PredictionResponse])
async def forecast(
    request: PredictionRequest,
    ctx: JWTContext = Depends(get_jwt_context)
):
    """
    预测未来客流（需要请求体参数）

    - **days**: 预测天数 (1-365)，默认20天
    - **confidence_level**: 置信水平 (0-1)，默认0.95
    - **auto_train**: 是否自动训练（如果模型不存在），默认False
    """
    result = await async_prediction_service.predict(request)
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
    快速预测（Query 参数，无需请求体）

    - **days**: 预测天数，默认20天
    - **confidence_level**: 置信水平，默认0.95
    - **auto_train**: 是否自动训练，默认False
    """
    request = PredictionRequest(
        days=days,
        confidence_level=confidence_level,
        auto_train=auto_train
    )
    result = await async_prediction_service.predict(request)
    if result.status == "error":
        return Result.error(message=result.message)
    return success_result(data=result)


# ==================== 异步训练任务接口 ====================

@router.post("/train/async", response_model=Result[TrainingTaskCreateResponse])
async def create_training_task(
    priority: int = Query(default=0, ge=0, le=10, description="任务优先级，数字越小优先级越高"),
    ctx: JWTContext = Depends(require_auth)
):
    """
    创建异步训练任务（需要登录权限）

    立即返回任务ID，训练任务在后台异步执行
    支持任务优先级设置
    """
    result = await async_prediction_service.create_training_task(
        user_id=str(ctx.operator_id),
        priority=priority
    )
    return success_result(data=result)


@router.get("/train/task/{task_id}", response_model=Result[TrainingTaskResponse])
async def get_training_task_status(
    task_id: str,
    ctx: JWTContext = Depends(get_jwt_context)
):
    """
    获取训练任务状态和进度

    - **task_id**: 任务ID

    返回任务当前状态、进度百分比、当前阶段、预计剩余时间等信息
    支持从检查点恢复的任务会显示恢复状态
    """
    result = await async_prediction_service.get_task_status(task_id)
    if result is None:
        return Result.not_found(message=f"任务不存在: {task_id}")
    return success_result(data=result)


@router.get("/train/tasks", response_model=Result[TrainingTaskListResponse])
async def list_training_tasks(
    status: Optional[str] = Query(default=None, description="状态筛选 (pending/processing/completed/failed/cancelled/recovering)"),
    limit: int = Query(default=20, ge=1, le=100, description="返回数量限制"),
    offset: int = Query(default=0, ge=0, description="偏移量"),
    ctx: JWTContext = Depends(get_jwt_context)
):
    """
    获取训练任务列表

    - **status**: 按状态筛选
    - **limit**: 返回数量限制
    - **offset**: 偏移量
    """
    result = await async_prediction_service.get_task_list(status, limit, offset)
    return success_result(data=result)


@router.post("/train/task/{task_id}/cancel", response_model=Result[dict])
async def cancel_training_task(
    task_id: str,
    ctx: JWTContext = Depends(require_auth)
):
    """
    取消训练任务（需要登录权限）

    - **task_id**: 任务ID
    """
    success = await async_prediction_service.cancel_task(task_id)
    if success:
        return success_result(data={"task_id": task_id, "cancelled": True})
    return Result.error(message=f"取消任务失败，任务可能不存在或已完成: {task_id}")


@router.get("/train/recovery-status", response_model=Result[dict])
async def get_recovery_status(ctx: JWTContext = Depends(get_jwt_context)):
    """
    获取任务恢复状态

    返回可恢复的任务列表和恢复报告
    """
    from app.core.task_persistence import task_persistence
    report = await task_persistence.create_recovery_report()
    return success_result(data=report)


@router.get("/model-info", response_model=Result[ModelInfoResponse])
async def get_model_info(ctx: JWTContext = Depends(get_jwt_context)):
    """
    获取模型信息

    返回当前加载的 ARIMA 模型信息，包括参数、AIC/BIC、训练时间等
    """
    result = await async_prediction_service.get_model_info()
    return success_result(data=result)


@router.get("/health")
async def health():
    """
    健康检查

    返回服务健康状态
    """
    return {"status": "ok"}


# ==================== 自动重训相关接口 ====================

@router.get("/auto-retrain/status", response_model=Result[dict])
async def get_auto_retrain_status(ctx: JWTContext = Depends(get_jwt_context)):
    """
    获取自动重训服务状态

    返回自动重训服务的运行状态、配置信息、上次重训时间等
    即使服务未启动也会返回统一格式的状态数据
    """
    # 从模块重新获取全局变量，确保获取到最新的实例
    from app.services.auto_retrain_service import auto_retrain_service as current_service

    if current_service is None:
        # 服务未启动时返回统一格式的状态数据
        status = {
            'enabled': False,
            'running': False,
            'schedule_time': None,
            'retrain_cycle_days': None,
            'last_retrain_date': None,
            'retrain_count': 0,
            'data_file': None,
            'data_rows': 0
        }
        return success_result(data=status)

    status = current_service.get_status()
    return success_result(data=status)


@router.get("/performance", response_model=Result[dict])
async def get_performance_metrics(ctx: JWTContext = Depends(get_jwt_context)):
    """
    获取系统性能指标

    返回API响应时间统计、系统资源使用情况等
    """
    from app.core.performance_monitor import performance_monitor, system_monitor

    # 获取性能统计
    perf_stats = performance_monitor.get_statistics()
    alerts = performance_monitor.get_alerts(limit=5)

    # 获取系统资源
    import psutil
    system_stats = {
        'cpu_percent': psutil.cpu_percent(interval=0.1),
        'memory_percent': psutil.virtual_memory().percent,
        'disk_usage': psutil.disk_usage('/').percent
    }

    data = {
        'performance': perf_stats,
        'system': system_stats,
        'alerts': alerts,
        'timestamp': datetime.now(timezone(timedelta(hours=8))).isoformat()
    }

    return success_result(data=data)


# 启动时初始化异步任务管理器
@router.on_event("startup")
async def startup_event():
    """服务启动时初始化"""
    global auto_retrain_service

    logger.info("=" * 60)
    logger.info("启动异步预测服务")
    logger.info("=" * 60)

    # 初始化异步预测服务（设置任务执行器）
    await async_prediction_service.initialize()

    # 启动新的异步任务管理器（会自动恢复之前的任务）
    await async_task_manager.start()

    # 启动自动重训服务
    await init_auto_retrain()

    logger.info("异步预测服务启动完成")


@router.on_event("shutdown")
async def shutdown_event():
    """服务关闭时清理"""
    logger.info("=" * 60)
    logger.info("关闭异步预测服务")
    logger.info("=" * 60)

    # 关闭自动重训服务
    await close_auto_retrain()

    # 停止异步任务管理器（会自动保存所有任务状态）
    await async_task_manager.stop()

    logger.info("异步预测服务已关闭")
