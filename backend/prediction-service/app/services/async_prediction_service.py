"""
异步预测服务
集成 ARIMA 模型训练和预测功能，完全异步化实现
支持任务持久化、检查点保存和崩溃恢复
"""
import os
import sys
import json
import logging
import asyncio
from datetime import datetime, timezone, timedelta
from typing import Optional, Dict, Callable
from pathlib import Path

import pandas as pd
import numpy as np

# 添加 prediction 模块路径
sys.path.insert(0, str(Path(__file__).resolve().parent.parent.parent / "prediction"))

from prediction.arima_predictor import ARIMAPredictor
from training.async_arima_trainer import AsyncARIMATrainer
from utils.data_processor import DataProcessor

from app.core.config import settings
from app.core.async_task_manager import async_task_manager, TaskStage, TaskStatus
from app.core.task_persistence import TrainingCheckpoint, task_persistence
from app.schemas.prediction import (
    PredictionRequest, PredictionResponse, PredictionItem, ModelInfo,
    TrainingResponse, ValidationMetrics, ModelInfoResponse,
    TrainingTaskCreateResponse, TrainingTaskResponse, TaskProgressInfo,
    TaskErrorInfo, TrainingTaskListItem, TrainingTaskListResponse
)

logger = logging.getLogger(__name__)


class AsyncPredictionService:
    """完全异步化的预测服务类"""

    def __init__(self):
        self.predictor: Optional[ARIMAPredictor] = None
        self._model_lock = asyncio.Lock()
        self._load_model()

    async def initialize(self):
        """初始化服务，设置任务执行器"""
        # 设置任务执行器，用于执行恢复的任务
        # 使用lambda包装以确保self正确绑定
        async_task_manager.set_task_executor(
            lambda task_id: self._execute_training_task(task_id)
        )
        logger.info("异步预测服务初始化完成，任务执行器已设置")

    def _load_model(self):
        """加载已训练的模型"""
        model_file = settings.model_path / "arima_model.pkl"
        if model_file.exists():
            try:
                self.predictor = ARIMAPredictor(str(settings.model_path))
                logger.info("模型加载成功")
            except Exception as e:
                logger.warning(f"模型加载失败: {e}")
                self.predictor = None
        else:
            logger.info(f"没有找到已训练的模型: {model_file}")
            self.predictor = None

    def _get_beijing_time(self) -> datetime:
        """获取北京时间"""
        beijing_tz = timezone(timedelta(hours=8))
        return datetime.now(beijing_tz)

    def _get_model_info(self) -> Optional[ModelInfo]:
        """获取模型信息"""
        if not self.predictor or not self.predictor.params:
            return None

        params = self.predictor.params
        training_history = params.get('training_history', {})

        training_time = None
        if 'end_time' in training_history:
            try:
                training_time = datetime.fromisoformat(training_history['end_time'])
            except:
                pass

        default_order = [5, 1, 0]
        return ModelInfo(
            order=params.get('order', default_order),
            aic=params.get('aic'),
            bic=params.get('bic'),
            training_time=training_time
        )

    async def predict(self, request: PredictionRequest) -> PredictionResponse:
        """
        纯异步执行预测

        Args:
            request: 预测请求

        Returns:
            预测响应
        """
        # 如果没有模型且请求了自动训练，则先训练
        if self.predictor is None and request.auto_train:
            logger.info("模型不存在，执行自动训练...")
            await self.train()

        if self.predictor is None:
            return PredictionResponse(
                status="error",
                message="模型未加载，请先训练模型或设置 auto_train=true",
                prediction_time=self._get_beijing_time(),
                days=request.days,
                predictions=[],
                model_info=None
            )

        try:
            # 纯异步执行预测
            alpha = 1 - request.confidence_level
            result_df = await asyncio.to_thread(
                self.predictor.predict,
                steps=request.days,
                alpha=alpha
            )

            # 转换结果
            predictions = []
            for _, row in result_df.iterrows():
                weekday = row['Weekday']
                predictions.append(PredictionItem(
                    prediction_date=row['Date'].date() if isinstance(row['Date'], pd.Timestamp) else row['Date'],
                    predicted_passengers=float(row['Predicted_Passengers']),
                    lower_bound=float(row['Lower_Bound']),
                    upper_bound=float(row['Upper_Bound']),
                    day_of_week=row['DayOfWeek'],
                    is_weekday=weekday < 5
                ))

            return PredictionResponse(
                status="success",
                message=f"成功预测未来 {request.days} 天的客流",
                prediction_time=self._get_beijing_time(),
                days=request.days,
                predictions=predictions,
                model_info=self._get_model_info()
            )

        except Exception as e:
            logger.error(f"预测失败: {e}")
            return PredictionResponse(
                status="error",
                message=f"预测失败: {str(e)}",
                prediction_time=self._get_beijing_time(),
                days=request.days,
                predictions=[],
                model_info=self._get_model_info()
            )

    async def create_training_task(self, user_id: Optional[str] = None, priority: int = 0) -> TrainingTaskCreateResponse:
        """
        创建异步训练任务

        Args:
            user_id: 用户ID
            priority: 任务优先级

        Returns:
            任务创建响应
        """
        # 创建任务（任务会自动添加到队列，由工作进程执行）
        task = await async_task_manager.create_task(user_id=user_id, priority=priority)

        return TrainingTaskCreateResponse(
            task_id=task.task_id,
            status=task.status.value,
            message="训练任务已创建并加入队列",
            created_at=task.created_at.isoformat()
        )

    async def _execute_training_task(self, task_id: str):
        """
        执行训练任务（使用独立进程）

        Args:
            task_id: 任务ID
        """
        logger.info(f"=" * 60)
        logger.info(f"开始执行任务 {task_id}")
        logger.info(f"=" * 60)

        task = await async_task_manager.get_task(task_id)
        if not task:
            logger.error(f"任务 {task_id} 不存在")
            return

        logger.info(f"任务 {task_id}: 当前状态 {task.status.value}")

        # 更新状态为处理中
        await async_task_manager.update_task_status(task_id, TaskStatus.PROCESSING)
        logger.info(f"任务 {task_id}: 状态已更新为 PROCESSING")

        try:
            # 阶段1: 初始化
            logger.info(f"任务 {task_id}: 阶段1 - 初始化")
            await async_task_manager.update_task_progress(
                task_id=task_id,
                stage=TaskStage.INITIALIZING,
                percent=5,
                current_step="初始化",
                message="正在初始化训练环境..."
            )

            # 检查数据文件
            data_file = settings.data_file
            logger.info(f"任务 {task_id}: 检查数据文件 {data_file}")
            if not data_file.exists():
                raise FileNotFoundError(f"数据文件不存在: {data_file}")
            logger.info(f"任务 {task_id}: 数据文件存在")

            # 阶段2: 启动独立训练进程
            logger.info(f"任务 {task_id}: 阶段2 - 启动独立训练进程")
            await async_task_manager.update_task_progress(
                task_id=task_id,
                stage=TaskStage.MODEL_TRAINING,
                percent=10,
                current_step="模型训练",
                message="启动独立训练进程...",
                estimated_remaining_seconds=180
            )

            # 使用独立进程进行训练
            from app.core.training_process import independent_trainer

            logger.info(f"任务 {task_id}: 调用 independent_trainer.start_training()")
            logger.info(f"任务 {task_id}: 数据文件={data_file}, 输出目录={settings.model_path}")

            result = await independent_trainer.start_training(
                data_file=str(data_file),
                model_output_dir=str(settings.model_path),
                progress_callback=lambda p: logger.info(f"[训练进度] {p}")
            )

            logger.info(f"任务 {task_id}: 独立训练进程返回结果: success={result.success}")

            if result.success:
                logger.info(f"任务 {task_id}: 训练成功")

                # 更新进度到完成
                await async_task_manager.update_task_progress(
                    task_id=task_id,
                    stage=TaskStage.COMPLETED,
                    percent=100,
                    current_step="完成",
                    message="训练完成！"
                )

                # 构建训练结果
                training_result = {
                    'status': 'success',
                    'message': '模型训练成功',
                    'order': result.model_info.get('order', [5, 1, 0]),
                    'aic': result.model_info.get('aic'),
                    'bic': result.model_info.get('bic'),
                    'training_time': self._get_beijing_time().isoformat(),
                    'validation': result.model_info.get('validation'),
                    'data_length': 0  # 独立进程中无法获取
                }

                # 更新任务状态为完成
                await async_task_manager.update_task_status(
                    task_id=task_id,
                    status=TaskStatus.COMPLETED,
                    result=training_result
                )

                # 重新加载模型
                async with self._model_lock:
                    self._load_model()

            else:
                logger.error(f"任务 {task_id}: 训练失败 - {result.error}")
                raise RuntimeError(result.error or "训练失败")

        except Exception as e:
            logger.error(f"训练任务 {task_id} 失败: {e}", exc_info=True)
            error_info = {
                "error_type": type(e).__name__,
                "error_message": str(e),
                "timestamp": self._get_beijing_time().isoformat()
            }
            await async_task_manager.update_task_status(
                task_id=task_id,
                status=TaskStatus.FAILED,
                error_info=error_info
            )

    async def _execute_training_task_old(self, task_id: str):
        """
        执行训练任务（旧版本，保留用于参考）

        Args:
            task_id: 任务ID
        """
        task = await async_task_manager.get_task(task_id)
        if not task:
            logger.error(f"任务 {task_id} 不存在")
            return

        # 更新状态为处理中
        await async_task_manager.update_task_status(task_id, TaskStatus.PROCESSING)

        # 创建检查点保存回调
        async def checkpoint_callback(task_id: str, checkpoint: TrainingCheckpoint):
            await async_task_manager.save_checkpoint(task_id, checkpoint)

        try:
            # 检查数据文件
            data_file = settings.data_file
            if not data_file.exists():
                raise FileNotFoundError(f"数据文件不存在: {data_file}")

            # 阶段1: 初始化
            await async_task_manager.update_task_progress(
                task_id=task_id,
                stage=TaskStage.INITIALIZING,
                percent=5,
                current_step="初始化",
                message="正在初始化训练环境..."
            )

            # 阶段2: 数据加载
            await async_task_manager.update_task_progress(
                task_id=task_id,
                stage=TaskStage.DATA_LOADING,
                percent=10,
                current_step="数据加载",
                message="正在加载日度数据..."
            )

            # 异步读取数据
            daily_df = await asyncio.to_thread(pd.read_csv, data_file)

            # 验证数据格式
            if 'Date' in daily_df.columns and 'Passengers' in daily_df.columns:
                daily_df['Date'] = pd.to_datetime(daily_df['Date'])
                daily_df = daily_df.sort_values('Date').reset_index(drop=True)
                passenger_series = daily_df['Passengers']
                logger.info(f"已加载日度数据: {len(passenger_series)} 条记录")
            elif 'Month' in daily_df.columns and 'Passengers' in daily_df.columns:
                # 月度格式，需要转换
                await async_task_manager.update_task_progress(
                    task_id=task_id,
                    stage=TaskStage.DATA_PROCESSING,
                    percent=20,
                    current_step="数据处理",
                    message="正在将月度数据转换为日度数据..."
                )

                processor = DataProcessor()
                monthly_data = processor.load_monthly_data(str(data_file))
                daily_data = processor.monthly_to_daily(
                    interpolation_method='cubic',
                    apply_effects=True,
                    apply_perturbation=True,
                    noise_level=0.15,
                    noise_type='adaptive',
                    random_seed=42
                )
                passenger_series = daily_data['Passengers']
            else:
                raise ValueError(f"数据文件格式错误: {data_file}")

            # 阶段3: 参数优化
            await async_task_manager.update_task_progress(
                task_id=task_id,
                stage=TaskStage.PARAMETER_OPTIMIZATION,
                percent=35,
                current_step="参数优化",
                message="正在自动寻找最优 ARIMA 参数...",
                estimated_remaining_seconds=120
            )

            # 检查是否有检查点需要恢复
            checkpoint = None
            if task.checkpoint:
                checkpoint = task.checkpoint
                logger.info(f"从检查点恢复训练: {task_id}, 当前阶段: {checkpoint.stage}")

            # 创建异步训练器
            trainer = AsyncARIMATrainer(p=5, d=1, q=0)
            trainer.set_checkpoint_callback(checkpoint_callback, task_id)

            # 从检查点恢复（如果有）
            if checkpoint:
                trainer.load_from_checkpoint(checkpoint)

            # 异步寻找最优参数
            await trainer.find_best_params(
                passenger_series,
                max_p=5,
                max_d=2,
                max_q=5
            )

            # 阶段4: 模型训练
            await async_task_manager.update_task_progress(
                task_id=task_id,
                stage=TaskStage.MODEL_TRAINING,
                percent=60,
                current_step="模型训练",
                message=f"正在训练 ARIMA({trainer.p},{trainer.d},{trainer.q}) 模型...",
                estimated_remaining_seconds=60
            )

            # 异步训练模型
            history = await trainer.train(
                passenger_series,
                validate=True,
                test_size=30
            )

            # 阶段5: 模型验证
            await async_task_manager.update_task_progress(
                task_id=task_id,
                stage=TaskStage.MODEL_VALIDATION,
                percent=85,
                current_step="模型验证",
                message="正在验证模型性能...",
                estimated_remaining_seconds=20
            )

            # 阶段6: 模型保存
            await async_task_manager.update_task_progress(
                task_id=task_id,
                stage=TaskStage.MODEL_SAVING,
                percent=95,
                current_step="模型保存",
                message="正在保存模型...",
                estimated_remaining_seconds=10
            )

            # 异步保存模型
            await trainer.save_model(str(settings.model_path))

            # 完成
            await async_task_manager.update_task_progress(
                task_id=task_id,
                stage=TaskStage.COMPLETED,
                percent=100,
                current_step="完成",
                message="训练完成！"
            )

            # 构建验证指标
            validation = None
            if 'validation' in history:
                val = history['validation']
                validation = {
                    'mae': val['mae'],
                    'rmse': val['rmse'],
                    'mape': val['mape'],
                    'test_size': val['test_size']
                }

            # 构建结果
            result = {
                'status': 'success',
                'message': '模型训练成功',
                'order': [trainer.p, trainer.d, trainer.q],
                'aic': history.get('aic'),
                'bic': history.get('bic'),
                'training_time': self._get_beijing_time().isoformat(),
                'validation': validation,
                'data_length': history.get('data_length', len(passenger_series))
            }

            # 更新任务状态为完成
            await async_task_manager.update_task_status(
                task_id=task_id,
                status=TaskStatus.COMPLETED,
                result=result
            )

            # 重新加载模型
            async with self._model_lock:
                self._load_model()

            logger.info(f"训练任务 {task_id} 完成")

        except Exception as e:
            logger.error(f"训练任务 {task_id} 失败: {e}", exc_info=True)
            error_info = {
                "error_type": type(e).__name__,
                "error_message": str(e),
                "timestamp": self._get_beijing_time().isoformat()
            }
            await async_task_manager.update_task_status(
                task_id=task_id,
                status=TaskStatus.FAILED,
                error_info=error_info
            )

    async def get_task_status(self, task_id: str) -> Optional[TrainingTaskResponse]:
        """获取任务状态"""
        task = await async_task_manager.get_task(task_id)
        if not task:
            return None

        # 构建进度信息
        progress = TaskProgressInfo(
            stage=task.progress.stage.value,
            percent=task.progress.percent,
            current_step=task.progress.current_step,
            total_steps=task.progress.total_steps,
            completed_steps=task.progress.completed_steps,
            estimated_remaining_seconds=task.progress.estimated_remaining_seconds,
            message=task.progress.message,
            updated_at=task.progress.updated_at.isoformat()
        )

        # 构建结果或错误信息
        result = None
        error_info = None

        if task.status == TaskStatus.COMPLETED and task.result:
            validation = None
            if task.result.get('validation'):
                val = task.result['validation']
                validation = ValidationMetrics(
                    mae=val['mae'],
                    rmse=val['rmse'],
                    mape=val['mape'],
                    test_size=val['test_size']
                )

            result = TrainingResponse(
                status=task.result.get('status', 'success'),
                message=task.result.get('message', '训练成功'),
                order=task.result.get('order', [5, 1, 0]),
                aic=task.result.get('aic'),
                bic=task.result.get('bic'),
                training_time=datetime.fromisoformat(task.result.get('training_time')) if task.result.get('training_time') else self._get_beijing_time(),
                validation=validation,
                data_length=task.result.get('data_length', 0)
            )

        elif task.status == TaskStatus.FAILED and task.error_info:
            error_info = TaskErrorInfo(
                error_type=task.error_info.get('error_type', 'UnknownError'),
                error_message=task.error_info.get('error_message', '未知错误'),
                timestamp=task.error_info.get('timestamp', datetime.now(timezone(timedelta(hours=8))).isoformat())
            )

        return TrainingTaskResponse(
            task_id=task.task_id,
            status=task.status.value,
            progress=progress,
            created_at=task.created_at.isoformat(),
            started_at=task.started_at.isoformat() if task.started_at else None,
            completed_at=task.completed_at.isoformat() if task.completed_at else None,
            result=result,
            error_info=error_info
        )

    async def get_task_list(
        self,
        status: Optional[str] = None,
        limit: int = 100,
        offset: int = 0
    ) -> TrainingTaskListResponse:
        """获取任务列表"""
        task_status = TaskStatus(status) if status else None
        tasks = await async_task_manager.get_all_tasks(task_status, limit, offset)

        task_items = []
        for task in tasks:
            task_items.append(TrainingTaskListItem(
                task_id=task.task_id,
                status=task.status.value,
                progress_percent=task.progress.percent,
                current_stage=task.progress.stage.value,
                created_at=task.created_at.isoformat(),
                completed_at=task.completed_at.isoformat() if task.completed_at else None
            ))

        return TrainingTaskListResponse(
            total=len(tasks),
            tasks=task_items
        )

    async def cancel_task(self, task_id: str) -> bool:
        """取消任务"""
        return await async_task_manager.cancel_task(task_id)

    async def train(self) -> TrainingResponse:
        """
        同步训练（向后兼容，内部调用异步）

        Returns:
            训练响应
        """
        try:
            # 检查数据文件
            data_file = settings.data_file
            if not data_file.exists():
                return TrainingResponse(
                    status="error",
                    message=f"数据文件不存在: {data_file}",
                    order=[5, 1, 0],
                    training_time=self._get_beijing_time(),
                    data_length=0
                )

            # 创建高优先级任务并等待完成
            task = await self.create_training_task(priority=0)
            task_id = task.task_id

            # 等待任务完成
            while True:
                task_status = await self.get_task_status(task_id)
                if not task_status:
                    break

                if task_status.status in ['completed', 'failed']:
                    if task_status.result:
                        return task_status.result
                    elif task_status.error_info:
                        return TrainingResponse(
                            status="error",
                            message=task_status.error_info.error_message,
                            order=[5, 1, 0],
                            training_time=self._get_beijing_time(),
                            data_length=0
                        )
                    break

                await asyncio.sleep(1)

            return TrainingResponse(
                status="error",
                message="训练任务异常结束",
                order=[5, 1, 0],
                training_time=self._get_beijing_time(),
                data_length=0
            )

        except Exception as e:
            logger.error(f"训练失败: {e}")
            return TrainingResponse(
                status="error",
                message=f"训练失败: {str(e)}",
                order=[5, 1, 0],
                training_time=self._get_beijing_time(),
                data_length=0
            )

    async def get_model_info(self) -> ModelInfoResponse:
        """获取模型信息"""
        if not self.predictor:
            return ModelInfoResponse(
                model_loaded=False,
                prediction_time=self._get_beijing_time()
            )

        if not self.predictor.params:
            return ModelInfoResponse(
                model_loaded=True,
                prediction_time=self._get_beijing_time(),
                order=None,
                aic=None,
                bic=None,
                sigma2=None,
                training_history=None
            )

        info = self.predictor.get_model_info()

        training_time = None
        if 'training_history' in info and 'end_time' in info['training_history']:
            try:
                training_time = datetime.fromisoformat(info['training_history']['end_time'])
            except:
                pass

        return ModelInfoResponse(
            model_loaded=info.get('model_loaded', False),
            prediction_time=self._get_beijing_time(),
            order=info.get('order'),
            aic=info.get('aic'),
            bic=info.get('bic'),
            sigma2=info.get('sigma2'),
            training_history=info.get('training_history')
        )


# 全局异步服务实例
async_prediction_service = AsyncPredictionService()
