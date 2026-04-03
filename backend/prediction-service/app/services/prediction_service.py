"""
预测服务
集成 ARIMA 模型训练和预测功能，支持异步训练任务
"""
import os
import sys
import json
import logging
import asyncio
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone, timedelta
from typing import Optional, Dict, List, Callable
from pathlib import Path

import pandas as pd
import numpy as np

# 添加 prediction 模块路径
sys.path.insert(0, str(Path(__file__).resolve().parent.parent.parent / "prediction"))

from prediction.arima_predictor import ARIMAPredictor
from training.arima_trainer import ARIMATrainer
from utils.data_processor import DataProcessor

from app.core.config import settings
from app.core.task_manager import task_manager, TaskStage, TaskStatus
from app.schemas.prediction import (
    PredictionRequest, PredictionResponse, PredictionItem, ModelInfo,
    TrainingResponse, ValidationMetrics, ModelInfoResponse,
    TrainingTaskCreateResponse, TrainingTaskResponse, TaskProgressInfo,
    TaskErrorInfo, TrainingTaskListItem, TrainingTaskListResponse
)

logger = logging.getLogger(__name__)


class PredictionService:
    """预测服务类"""

    def __init__(self):
        self.predictor: Optional[ARIMAPredictor] = None
        self._model_lock = asyncio.Lock()  # 用于保护模型加载/更新的锁
        self._executor = ThreadPoolExecutor(max_workers=4)  # 线程池用于执行阻塞操作
        self._load_model()

    def _load_model(self):
        """加载已训练的模型"""
        # 统一模型路径：直接使用 settings.model_path 作为模型根目录
        model_file = settings.model_path / "arima_model.pkl"
        if model_file.exists():
            try:
                # 传入 model_path 作为模型根目录
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

        # 解析训练时间
        training_time = None
        if 'end_time' in training_history:
            try:
                training_time = datetime.fromisoformat(training_history['end_time'])
            except:
                pass

        # 使用模型参数或默认值
        default_order = [5, 1, 0]
        return ModelInfo(
            order=params.get('order', default_order),
            aic=params.get('aic'),
            bic=params.get('bic'),
            training_time=training_time
        )

    async def predict(self, request: PredictionRequest) -> PredictionResponse:
        """
        执行预测（异步版本）

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
            # 在线程池中执行阻塞的预测操作
            loop = asyncio.get_event_loop()
            result_df = await loop.run_in_executor(
                self._executor,
                self._do_predict_sync,
                request.days,
                request.confidence_level
            )

            # 转换结果为 Pydantic 模型
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

    def _do_predict_sync(self, days: int, confidence_level: float) -> pd.DataFrame:
        """同步执行预测（在线程池中运行）"""
        alpha = 1 - confidence_level
        return self.predictor.predict(steps=days, alpha=alpha)

    async def create_training_task(self, user_id: Optional[str] = None) -> TrainingTaskCreateResponse:
        """
        创建异步训练任务

        Args:
            user_id: 用户ID

        Returns:
            任务创建响应
        """
        # 创建任务
        task = await task_manager.create_task(user_id)

        # 提交训练任务到队列
        await task_manager.submit_task(
            task.task_id,
            self._run_training_with_progress,
            task.task_id
        )

        return TrainingTaskCreateResponse(
            task_id=task.task_id,
            status=task.status.value,
            message="训练任务已创建并加入队列",
            created_at=task.created_at.isoformat()
        )

    def _run_training_with_progress(self, task_id: str, progress_callback: Optional[Callable] = None) -> Dict:
        """
        运行训练任务并更新进度

        Args:
            task_id: 任务ID
            progress_callback: 进度回调函数

        Returns:
            训练结果字典
        """
        beijing_tz = timezone(timedelta(hours=8))

        def update_progress(stage: TaskStage, percent: int, message: str, **kwargs):
            """更新进度辅助函数"""
            if progress_callback:
                progress_callback(stage, percent, message, **kwargs)
            logger.info(f"[{task_id}] {stage.value}: {percent}% - {message}")

        try:
            # 阶段1: 初始化
            update_progress(
                TaskStage.INITIALIZING,
                5,
                "正在初始化训练环境...",
                current_step="初始化"
            )

            # 检查数据文件
            data_file = settings.data_file
            if not data_file.exists():
                raise FileNotFoundError(f"数据文件不存在: {data_file}")

            # 阶段2: 数据加载
            update_progress(
                TaskStage.DATA_LOADING,
                10,
                "正在加载日度数据...",
                current_step="数据加载"
            )

            # 直接读取日度数据文件（支持bus_data.csv的日度格式）
            import pandas as pd
            daily_df = pd.read_csv(data_file)

            # 验证数据格式
            if 'Date' in daily_df.columns and 'Passengers' in daily_df.columns:
                # 日度格式（bus_data.csv）
                daily_df['Date'] = pd.to_datetime(daily_df['Date'])
                daily_df = daily_df.sort_values('Date').reset_index(drop=True)
                passenger_series = daily_df['Passengers']
                logger.info(f"已加载日度数据: {len(passenger_series)} 条记录")
            elif 'Month' in daily_df.columns and 'Passengers' in daily_df.columns:
                # 月度格式（airline-passengers.csv）
                processor = DataProcessor()
                monthly_data = processor.load_monthly_data(str(data_file))

                # 阶段3: 数据处理（月度转日度）
                update_progress(
                    TaskStage.DATA_PROCESSING,
                    20,
                    "正在将月度数据转换为日度数据...",
                    current_step="数据处理"
                )

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
                raise ValueError(f"数据文件格式错误，需要包含(Date/Month)和Passengers列: {data_file}")

            # 阶段4: 参数优化
            update_progress(
                TaskStage.PARAMETER_OPTIMIZATION,
                35,
                "正在自动寻找最优 ARIMA 参数...",
                current_step="参数优化",
                estimated_remaining_seconds=120
            )

            trainer = ARIMATrainer(p=5, d=1, q=0)
            trainer.find_best_params(passenger_series, max_p=5, max_d=2, max_q=5)

            # 阶段5: 模型训练
            update_progress(
                TaskStage.MODEL_TRAINING,
                60,
                f"正在训练 ARIMA({trainer.p},{trainer.d},{trainer.q}) 模型...",
                current_step="模型训练",
                estimated_remaining_seconds=60
            )

            history = trainer.train(
                passenger_series,
                validate=True,
                test_size=30
            )

            # 阶段6: 模型验证
            update_progress(
                TaskStage.MODEL_VALIDATION,
                85,
                "正在验证模型性能...",
                current_step="模型验证",
                estimated_remaining_seconds=20
            )

            # 阶段7: 模型保存
            update_progress(
                TaskStage.MODEL_SAVING,
                95,
                "正在保存模型...",
                current_step="模型保存",
                estimated_remaining_seconds=10
            )

            trainer.save_model(str(settings.model_path))

            # 完成
            update_progress(
                TaskStage.COMPLETED,
                100,
                "训练完成！",
                current_step="完成"
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

            # 返回训练结果
            return {
                'status': 'success',
                'message': '模型训练成功',
                'order': [trainer.p, trainer.d, trainer.q],
                'aic': history.get('aic'),
                'bic': history.get('bic'),
                'training_time': datetime.now(beijing_tz).isoformat(),
                'validation': validation,
                'data_length': history.get('data_length', len(passenger_series))
            }

        except Exception as e:
            logger.error(f"训练任务 {task_id} 失败: {e}", exc_info=True)
            raise

    async def get_task_status(self, task_id: str) -> Optional[TrainingTaskResponse]:
        """
        获取任务状态

        Args:
            task_id: 任务ID

        Returns:
            任务状态响应
        """
        task = await task_manager.get_task(task_id)
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
        """
        获取任务列表

        Args:
            status: 状态筛选
            limit: 数量限制
            offset: 偏移量

        Returns:
            任务列表响应
        """
        task_status = TaskStatus(status) if status else None
        tasks = await task_manager.get_all_tasks(task_status, limit, offset)

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
        """
        取消任务

        Args:
            task_id: 任务ID

        Returns:
            是否成功取消
        """
        return await task_manager.cancel_task(task_id)

    async def train(self) -> TrainingResponse:
        """
        训练模型（同步版本，向后兼容）

        Returns:
            训练响应
        """
        try:
            # 检查数据文件是否存在
            data_file = settings.data_file
            if not data_file.exists():
                return TrainingResponse(
                    status="error",
                    message=f"数据文件不存在: {data_file}",
                    order=[5, 1, 0],
                    training_time=self._get_beijing_time(),
                    data_length=0
                )

            # 在线程池中执行阻塞的训练操作
            loop = asyncio.get_event_loop()
            result = await loop.run_in_executor(
                self._executor,
                self._do_train_sync
            )

            # 使用锁保护模型加载
            async with self._model_lock:
                self._load_model()

            return result

        except Exception as e:
            logger.error(f"训练失败: {e}")
            return TrainingResponse(
                status="error",
                message=f"训练失败: {str(e)}",
                order=[5, 1, 0],
                training_time=self._get_beijing_time(),
                data_length=0
            )

    def _do_train_sync(self) -> TrainingResponse:
        """同步执行训练（在线程池中运行）"""
        data_file = settings.data_file

        # 加载和处理数据（支持日度或月度格式）
        import pandas as pd
        df = pd.read_csv(data_file)

        if 'Date' in df.columns and 'Passengers' in df.columns:
            # 日度格式（bus_data.csv）
            df['Date'] = pd.to_datetime(df['Date'])
            df = df.sort_values('Date').reset_index(drop=True)
            passenger_series = df['Passengers']
            logger.info(f"已加载日度数据: {len(passenger_series)} 条记录")
        elif 'Month' in df.columns and 'Passengers' in df.columns:
            # 月度格式（airline-passengers.csv）
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
            raise ValueError(f"数据文件格式错误，需要包含(Date/Month)和Passengers列: {data_file}")

        # 创建训练器并自动寻找最优参数
        logger.info("自动寻找最优 ARIMA 参数...")
        trainer = ARIMATrainer(p=5, d=1, q=0)
        trainer.find_best_params(passenger_series)

        # 训练模型（使用默认验证集大小30天）
        history = trainer.train(
            passenger_series,
            validate=True,
            test_size=30
        )

        # 保存模型
        trainer.save_model(str(settings.model_path))

        # 构建验证指标
        validation = None
        if 'validation' in history:
            val = history['validation']
            validation = ValidationMetrics(
                mae=val['mae'],
                rmse=val['rmse'],
                mape=val['mape'],
                test_size=val['test_size']
            )

        return TrainingResponse(
            status="success",
            message="模型训练成功",
            order=[trainer.p, trainer.d, trainer.q],
            aic=history.get('aic'),
            bic=history.get('bic'),
            training_time=self._get_beijing_time(),
            validation=validation,
            data_length=history.get('data_length', len(passenger_series))
        )

    async def get_model_info(self) -> ModelInfoResponse:
        """
        获取模型信息

        Returns:
            模型信息响应
        """
        if not self.predictor:
            return ModelInfoResponse(
                model_loaded=False,
                prediction_time=self._get_beijing_time()
            )

        # 检查模型参数是否存在
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

        # 解析训练时间
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


# 全局服务实例
prediction_service = PredictionService()
