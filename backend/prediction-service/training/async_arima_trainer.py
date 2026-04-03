"""
异步 ARIMA 模型训练模块
支持检查点保存和恢复，完全异步化实现，带资源限制
"""
import os
import sys
import json
import pickle
import logging
import asyncio
from datetime import datetime
from typing import Dict, Tuple, Optional, List, Callable, Any
import warnings
warnings.filterwarnings('ignore')

import numpy as np
import pandas as pd
from statsmodels.tsa.arima.model import ARIMA
from statsmodels.tsa.stattools import adfuller
from sklearn.metrics import mean_absolute_error, mean_squared_error

# 导入网格搜索模块
try:
    from training.async_grid_search import (
        AsyncTimeSeriesGridSearch, ParameterSpace, CrossValidationConfig,
        GridSearchResult, GridSearchReport, async_grid_search_arima
    )
    ASYNC_GRID_SEARCH_AVAILABLE = True
except ImportError:
    ASYNC_GRID_SEARCH_AVAILABLE = False
    logger = logging.getLogger(__name__)
    logger.warning("异步网格搜索模块未找到")

# 添加 utils 到路径
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from utils.data_processor import DataProcessor
from app.core.task_persistence import TrainingCheckpoint
from app.core.resource_manager import resource_governor, training_executor

logger = logging.getLogger(__name__)


class AsyncARIMATrainer:
    """
    异步 ARIMA 模型训练器
    支持检查点保存和恢复，完全异步化实现
    """

    def __init__(self, p: int = 5, d: int = 1, q: int = 0):
        """
        初始化异步 ARIMA 训练器

        Args:
            p: 自回归阶数
            d: 差分阶数
            q: 移动平均阶数
        """
        self.p = p
        self.d = d
        self.q = q
        self.model: Optional[ARIMA] = None
        self.fitted_model = None
        self.training_history: Dict = {}
        self.grid_search_report: Optional[GridSearchReport] = None
        
        # 检查点回调
        self._checkpoint_callback: Optional[Callable] = None
        self._task_id: Optional[str] = None
        
        # 恢复状态
        self._resumed_from_checkpoint: bool = False
        self._checkpoint_data: Optional[TrainingCheckpoint] = None

    def set_checkpoint_callback(self, callback: Callable, task_id: str):
        """
        设置检查点保存回调

        Args:
            callback: 检查点保存回调函数
            task_id: 任务ID
        """
        self._checkpoint_callback = callback
        self._task_id = task_id

    async def _save_checkpoint(
        self,
        stage: str,
        percent: int,
        current_step: str,
        message: str,
        **kwargs
    ):
        """保存训练检查点"""
        if not self._checkpoint_callback or not self._task_id:
            return

        try:
            checkpoint = TrainingCheckpoint(
                task_id=self._task_id,
                stage=stage,
                percent=percent,
                current_step=current_step,
                message=message,
                model_params={'p': self.p, 'd': self.d, 'q': self.q},
                training_history=self.training_history.copy() if self.training_history else None,
                **kwargs
            )
            
            # 序列化模型状态（如果存在）
            if self.fitted_model is not None:
                checkpoint.fitted_model_state = pickle.dumps(self.fitted_model)
            
            await self._checkpoint_callback(self._task_id, checkpoint)
            logger.debug(f"检查点已保存: {self._task_id}, stage={stage}, percent={percent}%")
            
        except Exception as e:
            logger.error(f"保存检查点失败: {e}")

    def load_from_checkpoint(self, checkpoint: TrainingCheckpoint) -> bool:
        """
        从检查点恢复训练状态

        Args:
            checkpoint: 检查点数据

        Returns:
            是否成功恢复
        """
        try:
            if checkpoint.model_params:
                self.p = checkpoint.model_params.get('p', 5)
                self.d = checkpoint.model_params.get('d', 1)
                self.q = checkpoint.model_params.get('q', 0)
            
            if checkpoint.training_history:
                self.training_history = checkpoint.training_history.copy()
            
            # 恢复模型状态
            if checkpoint.fitted_model_state:
                self.fitted_model = pickle.loads(checkpoint.fitted_model_state)
            
            self._resumed_from_checkpoint = True
            self._checkpoint_data = checkpoint
            
            logger.info(f"已从检查点恢复: {checkpoint.task_id}, stage={checkpoint.stage}")
            return True
            
        except Exception as e:
            logger.error(f"从检查点恢复失败: {e}")
            return False

    async def find_best_params(
        self,
        data: pd.Series,
        max_p: int = 5,
        max_d: int = 2,
        max_q: int = 5,
        method: str = 'grid_search',
        scoring: str = 'mape',
        n_splits: int = 5,
        random_state: int = 42
    ) -> Tuple[int, int, int]:
        """
        异步寻找最优 ARIMA 参数

        Args:
            data: 时间序列数据
            max_p: 最大 p 值
            max_d: 最大 d 值
            max_q: 最大 q 值
            method: 搜索方法 ('grid_search' 或 'aic')
            scoring: 评分指标
            n_splits: 交叉验证折数
            random_state: 随机种子

        Returns:
            最优 (p, d, q) 参数
        """
        logger.info("=" * 60)
        logger.info(f"开始异步寻找最优 ARIMA 参数 (方法: {method})")
        logger.info("=" * 60)

        # 保存检查点
        await self._save_checkpoint(
            stage='parameter_optimization',
            percent=35,
            current_step='参数优化',
            message='开始寻找最优参数',
            grid_search_progress={'method': method, 'scoring': scoring}
        )

        if method == 'grid_search' and ASYNC_GRID_SEARCH_AVAILABLE:
            return await self._async_find_best_params_grid_search(
                data, max_p, max_d, max_q, scoring, n_splits, random_state
            )
        else:
            if method == 'grid_search' and not ASYNC_GRID_SEARCH_AVAILABLE:
                logger.warning("异步网格搜索模块不可用，回退到AIC搜索")
            return await self._async_find_best_params_aic(data, max_p, max_d, max_q)

    async def _async_find_best_params_grid_search(
        self,
        data: pd.Series,
        max_p: int = 5,
        max_d: int = 2,
        max_q: int = 5,
        scoring: str = 'mape',
        n_splits: int = 5,
        random_state: int = 42
    ) -> Tuple[int, int, int]:
        """
        使用异步网格搜索寻找最优参数
        """
        p_values = list(range(max_p + 1))
        d_values = list(range(max_d + 1))
        q_values = list(range(max_q + 1))

        logger.info(f"参数空间: p={p_values}, d={d_values}, q={q_values}")
        logger.info(f"交叉验证: {n_splits}-fold, 评分指标: {scoring}")

        try:
            # 执行异步网格搜索
            best_params, report = await async_grid_search_arima(
                data=data,
                p_values=p_values,
                d_values=d_values,
                q_values=q_values,
                n_splits=n_splits,
                scoring=scoring,
                verbose=1,
                random_state=random_state,
                checkpoint_callback=self._save_checkpoint,
                task_id=self._task_id
            )

            self.grid_search_report = report
            self.p, self.d, self.q = best_params

            # 保存检查点
            await self._save_checkpoint(
                stage='parameter_optimization',
                percent=59,
                current_step='参数优化完成',
                message=f'找到最优参数: p={self.p}, d={self.d}, q={self.q}',
                grid_search_progress={
                    'best_params': best_params,
                    'best_score': report.best_score,
                    'method': 'grid_search'
                }
            )

            logger.info("=" * 60)
            logger.info("网格搜索完成")
            logger.info(f"最佳参数: p={self.p}, d={self.d}, q={self.q}")
            logger.info(f"最佳 {scoring.upper()}: {report.best_score:.4f}")
            logger.info("=" * 60)

            return best_params

        except Exception as e:
            logger.error(f"异步网格搜索失败: {e}", exc_info=True)
            logger.warning("回退到AIC搜索方法")
            return await self._async_find_best_params_aic(data, max_p, max_d, max_q)

    async def _async_find_best_params_aic(
        self,
        data: pd.Series,
        max_p: int = 5,
        max_d: int = 2,
        max_q: int = 5
    ) -> Tuple[int, int, int]:
        """
        使用异步AIC方法寻找最优参数（带资源治理）
        """
        logger.info("使用异步AIC搜索方法...")

        # 生成所有参数组合
        param_combinations = []
        for p in range(max_p + 1):
            for d in range(max_d + 1):
                for q in range(max_q + 1):
                    if not (p == 0 and q == 0):
                        param_combinations.append((p, d, q))

        best_aic = float('inf')
        best_params = (0, 0, 0)
        total = len(param_combinations)
        batch_size = resource_governor.batch_size

        logger.info(f"总共 {total} 个参数组合需要评估，批处理大小: {batch_size}")

        # 分批处理，避免阻塞事件循环
        for batch_start in range(0, total, batch_size):
            batch_end = min(batch_start + batch_size, total)
            batch = param_combinations[batch_start:batch_end]

            # 在资源治理下执行批次
            await resource_governor.acquire()
            try:
                for i, (p, d, q) in enumerate(batch):
                    try:
                        # 使用进程池执行CPU密集型任务
                        def fit_model():
                            model = ARIMA(data, order=(p, d, q))
                            return model.fit()

                        fitted = await training_executor.submit(fit_model)
                        aic = fitted.aic

                        if aic < best_aic:
                            best_aic = aic
                            best_params = (p, d, q)
                            logger.info(f"发现更优参数: p={p}, d={d}, q={q}, AIC={aic:.2f}")

                    except Exception as e:
                        logger.warning(f"参数 ({p},{d},{q}) 评估失败: {e}")
                        continue

                # 批次完成后保存检查点
                evaluated = batch_end
                progress = 35 + evaluated / total * 24  # 35% - 59%
                await self._save_checkpoint(
                    stage='parameter_optimization',
                    percent=int(progress),
                    current_step='参数优化中',
                    message=f'已评估 {evaluated}/{total} 个参数组合',
                    grid_search_progress={
                        'evaluated_count': evaluated,
                        'total_count': total,
                        'best_params_so_far': best_params,
                        'best_score_so_far': best_aic
                    }
                )

                # 让出控制权
                await resource_governor.yield_control()

            finally:
                resource_governor.release()

        self.p, self.d, self.q = best_params
        logger.info(f"最优参数: p={self.p}, d={self.d}, q={self.q}, AIC={best_aic:.2f}")

        return best_params

    async def train(
        self,
        data: pd.Series,
        validate: bool = True,
        test_size: int = 30
    ) -> Dict:
        """
        异步训练 ARIMA 模型

        Args:
            data: 训练数据
            validate: 是否进行验证
            test_size: 验证集大小

        Returns:
            训练历史记录
        """
        logger.info(f"开始异步训练 ARIMA({self.p},{self.d},{self.q}) 模型")
        logger.info(f"数据长度: {len(data)}")

        # 记录训练开始时间
        beijing_time = DataProcessor.get_beijing_time()
        self.training_history['start_time'] = beijing_time.isoformat()

        # 检查是否从检查点恢复
        if self._resumed_from_checkpoint and self._checkpoint_data:
            logger.info(f"从检查点恢复训练，当前阶段: {self._checkpoint_data.stage}")
            
            # 如果已经训练完成，直接返回
            if self._checkpoint_data.stage == 'model_training' and self._checkpoint_data.percent >= 85:
                logger.info("模型训练已完成，跳过训练阶段")
                return self.training_history

        if validate:
            train_data = data[:-test_size]
            val_data = data[-test_size:]
        else:
            train_data = data
            val_data = None

        try:
            # 保存检查点
            await self._save_checkpoint(
                stage='model_training',
                percent=60,
                current_step='模型训练中',
                message=f'开始训练 ARIMA({self.p},{self.d},{self.q}) 模型'
            )

            # 异步训练模型（使用进程池隔离）
            def train_model():
                self.model = ARIMA(train_data, order=(self.p, self.d, self.q))
                return self.model.fit()

            self.fitted_model = await training_executor.submit(train_model)

            logger.info("模型训练完成")
            logger.info(f"AIC: {self.fitted_model.aic:.2f}")
            logger.info(f"BIC: {self.fitted_model.bic:.2f}")

            # 保存模型信息
            self.training_history['aic'] = self.fitted_model.aic
            self.training_history['bic'] = self.fitted_model.bic
            self.training_history['params'] = {
                'p': self.p,
                'd': self.d,
                'q': self.q
            }

            # 保存检查点
            await self._save_checkpoint(
                stage='model_training',
                percent=75,
                current_step='模型训练完成',
                message='模型训练完成，准备验证',
                training_history=self.training_history.copy()
            )

            if validate and val_data is not None:
                # 异步验证
                await self._async_validate(val_data)

            self.training_history['end_time'] = DataProcessor.get_beijing_time().isoformat()
            self.training_history['data_length'] = len(data)

            return self.training_history

        except Exception as e:
            logger.error(f"模型训练失败: {str(e)}", exc_info=True)
            raise RuntimeError(f"模型训练失败: {str(e)}") from e

    async def _async_validate(self, val_data: pd.Series):
        """异步验证模型"""
        logger.info("开始异步验证...")

        # 保存检查点
        await self._save_checkpoint(
            stage='model_validation',
            percent=85,
            current_step='模型验证中',
            message='正在验证模型性能'
        )

        # 异步预测（使用进程池隔离）
        def forecast_model():
            return self.fitted_model.forecast(steps=len(val_data))

        predictions = await training_executor.submit(forecast_model)

        # 计算评估指标
        mae = mean_absolute_error(val_data, predictions)
        rmse = np.sqrt(mean_squared_error(val_data, predictions))
        mape = np.mean(np.abs((val_data - predictions) / val_data)) * 100

        logger.info(f"验证集 MAE: {mae:.2f}")
        logger.info(f"验证集 RMSE: {rmse:.2f}")
        logger.info(f"验证集 MAPE: {mape:.2f}%")

        self.training_history['validation'] = {
            'mae': float(mae),
            'rmse': float(rmse),
            'mape': float(mape),
            'test_size': len(val_data)
        }

        # 保存检查点
        await self._save_checkpoint(
            stage='model_validation',
            percent=94,
            current_step='模型验证完成',
            message=f'验证完成 - MAE:{mae:.2f}, RMSE:{rmse:.2f}, MAPE:{mape:.2f}%',
            training_history=self.training_history.copy()
        )

    async def save_model(self, resources_dir: str):
        """
        异步保存模型参数

        Args:
            resources_dir: resources 目录路径
        """
        if self.fitted_model is None:
            raise ValueError("模型尚未训练")

        # 保存检查点
        await self._save_checkpoint(
            stage='model_saving',
            percent=95,
            current_step='模型保存中',
            message='正在保存模型文件'
        )

        # 统一路径处理
        os.makedirs(resources_dir, exist_ok=True)

        # 保存模型对象
        model_path = os.path.join(resources_dir, 'arima_model.pkl')
        with open(model_path, 'wb') as f:
            pickle.dump(self.fitted_model, f)
        logger.info(f"模型已保存到: {model_path}")

        # 保存模型参数（JSON 格式）
        params_path = os.path.join(resources_dir, 'arima_params.json')
        params = {
            'order': [self.p, self.d, self.q],
            'params': self.fitted_model.params.tolist(),
            'aic': self.fitted_model.aic,
            'bic': self.fitted_model.bic,
            'sigma2': float(self.fitted_model.scale),
            'training_history': self.training_history
        }
        with open(params_path, 'w', encoding='utf-8') as f:
            json.dump(params, f, indent=2, ensure_ascii=False)
        logger.info(f"模型参数已保存到: {params_path}")

        # 保存网格搜索报告
        if self.grid_search_report:
            report_path = os.path.join(resources_dir, 'grid_search_report.json')
            self.grid_search_report.save_json(report_path)

        # 保存检查点
        await self._save_checkpoint(
            stage='model_saving',
            percent=99,
            current_step='模型保存完成',
            message='模型文件保存完成',
            training_history=self.training_history.copy()
        )

        # 保存最后的数据点
        last_data_path = os.path.join(resources_dir, 'last_data.json')
        if hasattr(self.fitted_model, 'data'):
            last_data = {
                'last_values': self.fitted_model.data.endog[-30:].tolist(),
                'last_date': self.training_history.get('end_time')
            }
            with open(last_data_path, 'w', encoding='utf-8') as f:
                json.dump(last_data, f, indent=2)
            logger.info(f"最后数据点已保存到: {last_data_path}")


# 保持向后兼容的别名
ARIMATrainer = AsyncARIMATrainer
