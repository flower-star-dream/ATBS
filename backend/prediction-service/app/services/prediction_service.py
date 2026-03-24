"""
预测服务
集成 ARIMA 模型训练和预测功能
"""
import os
import sys
import json
import logging
from datetime import datetime, timezone, timedelta
from typing import Optional, Dict, List
from pathlib import Path

import pandas as pd
import numpy as np

# 添加 prediction 模块路径
sys.path.insert(0, str(Path(__file__).resolve().parent.parent.parent / "prediction"))

from prediction.arima_predictor import ARIMAPredictor
from training.arima_trainer import ARIMATrainer
from utils.data_processor import DataProcessor

from app.core.config import settings
from app.schemas.prediction import (
    PredictionRequest, PredictionResponse, PredictionItem, ModelInfo,
    TrainingRequest, TrainingResponse, ValidationMetrics, ModelInfoResponse
)

logger = logging.getLogger(__name__)


class PredictionService:
    """预测服务类"""

    def __init__(self):
        self.predictor: Optional[ARIMAPredictor] = None
        self._load_model()

    def _load_model(self):
        """加载已训练的模型"""
        model_file = settings.MODEL_PATH / "arima_model.pkl"
        if model_file.exists():
            try:
                self.predictor = ARIMAPredictor(str(settings.MODEL_PATH))
                logger.info("模型加载成功")
            except Exception as e:
                logger.warning(f"模型加载失败: {e}")
                self.predictor = None
        else:
            logger.info("没有找到已训练的模型")
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

        return ModelInfo(
            order=params.get('order', [settings.DEFAULT_P, settings.DEFAULT_D, settings.DEFAULT_Q]),
            aic=params.get('aic'),
            bic=params.get('bic'),
            training_time=training_time
        )

    def predict(self, request: PredictionRequest) -> PredictionResponse:
        """
        执行预测

        Args:
            request: 预测请求

        Returns:
            预测响应
        """
        # 如果没有模型且请求了自动训练，则先训练
        if self.predictor is None and request.auto_train:
            logger.info("模型不存在，执行自动训练...")
            train_request = TrainingRequest()
            self.train(train_request)

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
            # 执行预测
            alpha = 1 - request.confidence_level
            result_df = self.predictor.predict(steps=request.days, alpha=alpha)

            # 转换结果为 Pydantic 模型
            predictions = []
            for _, row in result_df.iterrows():
                weekday = row['Weekday']
                predictions.append(PredictionItem(
                    date=row['Date'].date() if isinstance(row['Date'], pd.Timestamp) else row['Date'],
                    predicted_passengers=int(row['Predicted_Passengers']),
                    lower_bound=int(row['Lower_Bound']),
                    upper_bound=int(row['Upper_Bound']),
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

    def train(self, request: TrainingRequest) -> TrainingResponse:
        """
        训练模型

        Args:
            request: 训练请求

        Returns:
            训练响应
        """
        try:
            # 检查数据文件是否存在
            data_file = settings.DATA_FILE
            if not data_file.exists():
                # 尝试使用示例数据
                alt_path = Path(__file__).resolve().parent.parent.parent.parent / "src" / "main" / "resources" / "data" / "airline-passengers.csv"
                if alt_path.exists():
                    data_file = alt_path
                else:
                    return TrainingResponse(
                        status="error",
                        message=f"数据文件不存在: {data_file}",
                        order=[request.p, request.d, request.q],
                        training_time=self._get_beijing_time(),
                        data_length=0
                    )

            # 加载和处理数据
            processor = DataProcessor()
            monthly_data = processor.load_monthly_data(str(data_file))
            daily_data = processor.monthly_to_daily(interpolation_method='cubic')
            passenger_series = daily_data['Passengers']

            # 创建训练器
            trainer = ARIMATrainer(p=request.p, d=request.d, q=request.q)

            # 如果需要，自动寻找最优参数
            if request.auto_optimize:
                logger.info("自动寻找最优 ARIMA 参数...")
                trainer.find_best_params(passenger_series)

            # 训练模型
            history = trainer.train(
                passenger_series,
                validate=True,
                test_size=request.test_size
            )

            # 保存模型
            trainer.save_model(str(settings.MODEL_PATH))

            # 重新加载模型
            self._load_model()

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

        except Exception as e:
            logger.error(f"训练失败: {e}")
            return TrainingResponse(
                status="error",
                message=f"训练失败: {str(e)}",
                order=[request.p, request.d, request.q],
                training_time=self._get_beijing_time(),
                data_length=0
            )

    def get_model_info(self) -> ModelInfoResponse:
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
