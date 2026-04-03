"""
异步 ARIMA 模型网格搜索优化模块
实现异步网格搜索算法、k-fold交叉验证、残差检验
支持检查点保存和恢复
"""
import os
import json
import logging
import asyncio
import warnings
from datetime import datetime
from typing import Dict, List, Tuple, Optional, Any, Callable
from dataclasses import dataclass, field
import itertools

import numpy as np
import pandas as pd
from statsmodels.tsa.arima.model import ARIMA
from statsmodels.stats.diagnostic import acorr_ljungbox
from sklearn.metrics import mean_absolute_error, mean_squared_error, mean_absolute_percentage_error
from sklearn.model_selection import TimeSeriesSplit

warnings.filterwarnings('ignore')
logger = logging.getLogger(__name__)


@dataclass
class ParameterSpace:
    """参数搜索空间定义"""
    p_values: List[int] = field(default_factory=lambda: [1, 2, 3, 4])
    d_values: List[int] = field(default_factory=lambda: [0, 1, 2])
    q_values: List[int] = field(default_factory=lambda: [0, 1, 2, 3])

    def get_all_combinations(self) -> List[Tuple[int, int, int]]:
        """获取所有参数组合"""
        combinations = []
        for p, d, q in itertools.product(self.p_values, self.d_values, self.q_values):
            if not (p == 0 and q == 0):
                combinations.append((p, d, q))
        return combinations

    def to_dict(self) -> Dict[str, List[int]]:
        """转换为字典"""
        return {
            'p_values': self.p_values,
            'd_values': self.d_values,
            'q_values': self.q_values
        }


@dataclass
class CrossValidationConfig:
    """交叉验证配置"""
    n_splits: int = 5
    test_size: int = 30
    gap: int = 0

    def to_dict(self) -> Dict[str, int]:
        """转换为字典"""
        return {
            'n_splits': self.n_splits,
            'test_size': self.test_size,
            'gap': self.gap
        }


@dataclass
class ResidualDiagnostics:
    """残差诊断结果"""
    ljung_box_stat: float
    ljung_box_pvalue: float
    is_white_noise: bool
    residual_mean: float
    residual_std: float
    residual_skewness: float
    residual_kurtosis: float
    durbin_watson: float

    def to_dict(self) -> Dict[str, Any]:
        """转换为字典"""
        return {
            'ljung_box_stat': float(self.ljung_box_stat),
            'ljung_box_pvalue': float(self.ljung_box_pvalue),
            'is_white_noise': bool(self.is_white_noise),
            'residual_mean': float(self.residual_mean),
            'residual_std': float(self.residual_std),
            'residual_skewness': float(self.residual_skewness),
            'residual_kurtosis': float(self.residual_kurtosis),
            'durbin_watson': float(self.durbin_watson)
        }


@dataclass
class GridSearchResult:
    """网格搜索结果"""
    params: Tuple[int, int, int]
    mean_mae: float
    mean_rmse: float
    mean_mape: float
    mean_aic: float
    mean_bic: float
    std_mae: float
    std_rmse: float
    std_mape: float
    fold_scores: List[Dict[str, float]] = field(default_factory=list)
    is_valid: bool = True
    error_message: Optional[str] = None
    residual_diagnostics: Optional[ResidualDiagnostics] = None
    composite_score: float = float('inf')

    def to_dict(self) -> Dict[str, Any]:
        """转换为字典"""
        return {
            'params': self.params,
            'mean_mae': float(self.mean_mae),
            'mean_rmse': float(self.mean_rmse),
            'mean_mape': float(self.mean_mape),
            'mean_aic': float(self.mean_aic),
            'mean_bic': float(self.mean_bic),
            'std_mae': float(self.std_mae),
            'std_rmse': float(self.std_rmse),
            'std_mape': float(self.std_mape),
            'fold_scores': self.fold_scores,
            'is_valid': bool(self.is_valid),
            'error_message': self.error_message,
            'residual_diagnostics': self.residual_diagnostics.to_dict() if self.residual_diagnostics else None,
            'composite_score': float(self.composite_score)
        }


@dataclass
class GridSearchReport:
    """网格搜索完整报告"""
    best_params: Tuple[int, int, int]
    best_score: float
    scoring_metric: str
    all_results: List[GridSearchResult]
    parameter_space: ParameterSpace
    cv_config: CrossValidationConfig
    execution_time: float
    timestamp: str
    data_info: Dict[str, Any]
    selection_criteria: str

    def to_dict(self) -> Dict[str, Any]:
        """转换为字典"""
        return {
            'best_params': self.best_params,
            'best_score': self.best_score,
            'scoring_metric': self.scoring_metric,
            'all_results': [r.to_dict() for r in self.all_results],
            'parameter_space': self.parameter_space.to_dict(),
            'cv_config': self.cv_config.to_dict(),
            'execution_time': self.execution_time,
            'timestamp': self.timestamp,
            'data_info': self.data_info,
            'selection_criteria': self.selection_criteria
        }

    def save_json(self, filepath: str):
        """保存为JSON文件"""
        with open(filepath, 'w', encoding='utf-8') as f:
            json.dump(self.to_dict(), f, indent=2, ensure_ascii=False)
        logger.info(f"网格搜索报告已保存到: {filepath}")


class AsyncResidualAnalyzer:
    """异步残差分析器"""

    @staticmethod
    async def analyze_residuals(residuals: np.ndarray, lags: int = 10) -> ResidualDiagnostics:
        """异步分析残差序列"""
        residuals = residuals[~np.isnan(residuals)]

        if len(residuals) < lags + 1:
            return ResidualDiagnostics(
                ljung_box_stat=float('nan'),
                ljung_box_pvalue=float('nan'),
                is_white_noise=False,
                residual_mean=float('nan'),
                residual_std=float('nan'),
                residual_skewness=float('nan'),
                residual_kurtosis=float('nan'),
                durbin_watson=float('nan')
            )

        # Ljung-Box检验（异步执行）
        try:
            lb_result = await asyncio.to_thread(
                acorr_ljungbox,
                residuals,
                lags=lags,
                return_df=True
            )
            lb_stat = lb_result['lb_stat'].iloc[-1] if len(lb_result) > 0 else float('nan')
            lb_pvalue = lb_result['lb_pvalue'].iloc[-1] if len(lb_result) > 0 else float('nan')
            is_white_noise = lb_pvalue > 0.05
        except Exception as e:
            logger.warning(f"Ljung-Box检验失败: {e}")
            lb_stat = float('nan')
            lb_pvalue = float('nan')
            is_white_noise = False

        # 计算基本统计量
        residual_mean = np.mean(residuals)
        residual_std = np.std(residuals)

        # 计算偏度和峰度
        if len(residuals) > 3:
            residual_skewness = pd.Series(residuals).skew()
            residual_kurtosis = pd.Series(residuals).kurtosis()
        else:
            residual_skewness = float('nan')
            residual_kurtosis = float('nan')

        # Durbin-Watson统计量
        durbin_watson = await asyncio.to_thread(
            AsyncResidualAnalyzer._calculate_dw_statistic,
            residuals
        )

        return ResidualDiagnostics(
            ljung_box_stat=lb_stat,
            ljung_box_pvalue=lb_pvalue,
            is_white_noise=is_white_noise,
            residual_mean=residual_mean,
            residual_std=residual_std,
            residual_skewness=residual_skewness,
            residual_kurtosis=residual_kurtosis,
            durbin_watson=durbin_watson
        )

    @staticmethod
    def _calculate_dw_statistic(residuals: np.ndarray) -> float:
        """计算Durbin-Watson统计量"""
        if len(residuals) < 2:
            return float('nan')

        diff_residuals = np.diff(residuals)
        numerator = np.sum(diff_residuals ** 2)
        denominator = np.sum(residuals ** 2)

        if denominator == 0:
            return float('nan')

        return numerator / denominator


class AsyncTimeSeriesGridSearch:
    """
    异步时间序列网格搜索器
    实现异步网格搜索 + k-fold交叉验证 + 残差诊断
    """

    def __init__(
        self,
        parameter_space: Optional[ParameterSpace] = None,
        cv_config: Optional[CrossValidationConfig] = None,
        scoring: str = 'mape',
        verbose: int = 1,
        random_state: Optional[int] = None,
        use_composite_score: bool = True,
        residual_weight: float = 0.3
    ):
        """
        初始化异步网格搜索器

        Args:
            parameter_space: 参数搜索空间
            cv_config: 交叉验证配置
            scoring: 评分指标
            verbose: 详细程度
            random_state: 随机种子
            use_composite_score: 是否使用综合评分
            residual_weight: 残差诊断权重
        """
        self.parameter_space = parameter_space or ParameterSpace()
        self.cv_config = cv_config or CrossValidationConfig()
        self.scoring = scoring
        self.verbose = verbose
        self.random_state = random_state
        self.use_composite_score = use_composite_score
        self.residual_weight = residual_weight

        if random_state is not None:
            np.random.seed(random_state)

        self.results: List[GridSearchResult] = []
        self.best_result: Optional[GridSearchResult] = None
        self.residual_analyzer = AsyncResidualAnalyzer()
        self.report: Optional[GridSearchReport] = None

        # 检查点相关
        self._checkpoint_callback: Optional[Callable] = None
        self._task_id: Optional[str] = None

    def set_checkpoint_callback(self, callback: Callable, task_id: str):
        """设置检查点回调"""
        self._checkpoint_callback = callback
        self._task_id = task_id

    async def _save_checkpoint(self, evaluated_count: int, total_count: int, best_params: Tuple, best_score: float):
        """保存网格搜索检查点"""
        if not self._checkpoint_callback or not self._task_id:
            return

        try:
            progress = 35 + evaluated_count / total_count * 24
            await self._checkpoint_callback(
                stage='parameter_optimization',
                percent=int(progress),
                current_step='参数优化中',
                message=f'已评估 {evaluated_count}/{total_count} 个参数组合',
                grid_search_progress={
                    'evaluated_count': evaluated_count,
                    'total_count': total_count,
                    'best_params_so_far': best_params,
                    'best_score_so_far': best_score
                }
            )
        except Exception as e:
            logger.error(f"保存网格搜索检查点失败: {e}")

    async def _evaluate_single_fold(
        self,
        train_data: pd.Series,
        test_data: pd.Series,
        order: Tuple[int, int, int]
    ) -> Dict[str, float]:
        """异步评估单个fold"""
        try:
            # 异步训练模型
            model = ARIMA(train_data, order=order)
            fitted = await asyncio.to_thread(model.fit)

            # 异步预测
            predictions = await asyncio.to_thread(fitted.forecast, steps=len(test_data))

            # 计算指标
            mae = mean_absolute_error(test_data, predictions)
            rmse = np.sqrt(mean_squared_error(test_data, predictions))
            mape = mean_absolute_percentage_error(test_data, predictions) * 100

            return {
                'mae': mae,
                'rmse': rmse,
                'mape': mape,
                'aic': fitted.aic,
                'bic': fitted.bic,
                'valid': True
            }
        except Exception as e:
            logger.warning(f"Fold评估失败 (order={order}): {e}")
            return {
                'mae': np.inf,
                'rmse': np.inf,
                'mape': np.inf,
                'aic': np.inf,
                'bic': np.inf,
                'valid': False,
                'error': str(e)
            }

    async def _cross_validate(
        self,
        data: pd.Series,
        order: Tuple[int, int, int]
    ) -> GridSearchResult:
        """异步执行k-fold交叉验证"""
        p, d, q = order

        if self.verbose > 1:
            logger.info(f"评估参数: p={p}, d={d}, q={q}")

        n_samples = len(data)
        test_size = self.cv_config.test_size
        n_splits = self.cv_config.n_splits

        # 计算每个fold的起始位置
        total_required = n_splits * test_size
        if total_required >= n_samples:
            test_size = max(10, (n_samples - 1) // (n_splits + 1))

        fold_scores = []
        fold_starts = []

        for i in range(n_splits):
            test_end = n_samples - i * test_size
            test_start = test_end - test_size
            if test_start < 10:
                continue
            fold_starts.append((0, test_start, test_end))

        # 异步执行所有fold的评估
        tasks = []
        for train_start, test_start, test_end in fold_starts:
            train_data = data.iloc[train_start:test_start]
            test_data = data.iloc[test_start:test_end]

            if len(train_data) >= 10 and len(test_data) >= 1:
                task = self._evaluate_single_fold(train_data, test_data, order)
                tasks.append(task)

        # 等待所有fold完成
        fold_results = await asyncio.gather(*tasks, return_exceptions=True)

        # 处理结果
        valid_scores = []
        for i, result in enumerate(fold_results):
            if isinstance(result, Exception):
                logger.warning(f"Fold {i} 评估异常: {result}")
                continue
            if result.get('valid', False):
                result['fold'] = i
                fold_scores.append(result)
                valid_scores.append(result)

        if not valid_scores:
            return GridSearchResult(
                params=order,
                mean_mae=np.inf,
                mean_rmse=np.inf,
                mean_mape=np.inf,
                mean_aic=np.inf,
                mean_bic=np.inf,
                std_mae=0,
                std_rmse=0,
                std_mape=0,
                fold_scores=fold_scores,
                is_valid=False,
                error_message="所有fold都失败"
            )

        # 计算平均值和标准差
        metrics = ['mae', 'rmse', 'mape', 'aic', 'bic']
        means = {m: np.mean([s[m] for s in valid_scores]) for m in metrics}
        stds = {m: np.std([s[m] for s in valid_scores]) for m in metrics}

        # 异步残差诊断
        residual_diagnostics = None
        try:
            full_model = ARIMA(data, order=order)
            full_fitted = await asyncio.to_thread(full_model.fit)
            residuals = full_fitted.resid
            residual_diagnostics = await self.residual_analyzer.analyze_residuals(residuals)
        except Exception as e:
            logger.warning(f"残差诊断失败 (order={order}): {e}")

        # 计算综合评分
        composite_score = self._calculate_composite_score(means, residual_diagnostics)

        return GridSearchResult(
            params=order,
            mean_mae=means['mae'],
            mean_rmse=means['rmse'],
            mean_mape=means['mape'],
            mean_aic=means['aic'],
            mean_bic=means['bic'],
            std_mae=stds['mae'],
            std_rmse=stds['rmse'],
            std_mape=stds['mape'],
            fold_scores=fold_scores,
            is_valid=True,
            residual_diagnostics=residual_diagnostics,
            composite_score=composite_score
        )

    def _calculate_composite_score(
        self,
        metrics: Dict[str, float],
        residual_diagnostics: Optional[ResidualDiagnostics]
    ) -> float:
        """计算综合评分"""
        if self.scoring == 'mae':
            prediction_score = metrics['mae']
        elif self.scoring == 'rmse':
            prediction_score = metrics['rmse']
        elif self.scoring == 'mape':
            prediction_score = metrics['mape']
        elif self.scoring == 'aic':
            prediction_score = metrics['aic']
        elif self.scoring == 'bic':
            prediction_score = metrics['bic']
        else:
            prediction_score = metrics['mape']

        if not self.use_composite_score or residual_diagnostics is None:
            return prediction_score

        # 残差诊断评分
        residual_score = 0
        if not residual_diagnostics.is_white_noise:
            residual_score = 100 * (0.05 - min(residual_diagnostics.ljung_box_pvalue, 0.05))

        # 综合评分
        w = self.residual_weight
        composite_score = (1 - w) * prediction_score + w * residual_score

        return composite_score

    async def fit(self, data: pd.Series) -> 'AsyncTimeSeriesGridSearch':
        """
        执行异步网格搜索

        Args:
            data: 时间序列数据

        Returns:
            self
        """
        import time
        start_time = time.time()

        logger.info("=" * 60)
        logger.info("开始异步网格搜索")
        logger.info("=" * 60)

        # 获取所有参数组合
        param_combinations = self.parameter_space.get_all_combinations()
        total_combinations = len(param_combinations)

        logger.info(f"总共需要评估 {total_combinations} 个参数组合")

        # 准备数据信息
        data_info = {
            'length': len(data),
            'mean': float(data.mean()),
            'std': float(data.std()),
            'min': float(data.min()),
            'max': float(data.max())
        }

        # 执行网格搜索（限制并发数）
        self.results = []
        best_score = float('inf')
        best_params = (0, 0, 0)

        # 使用信号量限制并发
        semaphore = asyncio.Semaphore(4)  # 最多4个并发

        async def evaluate_with_limit(order: Tuple[int, int, int]) -> GridSearchResult:
            async with semaphore:
                return await self._cross_validate(data, order)

        # 分批执行，避免内存占用过大
        batch_size = 10
        for batch_start in range(0, total_combinations, batch_size):
            batch_end = min(batch_start + batch_size, total_combinations)
            batch_orders = param_combinations[batch_start:batch_end]

            # 异步执行当前批次
            tasks = [evaluate_with_limit(order) for order in batch_orders]
            batch_results = await asyncio.gather(*tasks, return_exceptions=True)

            # 处理结果
            for result in batch_results:
                if isinstance(result, Exception):
                    logger.error(f"参数评估异常: {result}")
                    continue
                self.results.append(result)

                # 更新最优参数
                if result.is_valid:
                    score = result.composite_score if self.use_composite_score else getattr(
                        result, f'mean_{self.scoring}'
                    )
                    if score < best_score:
                        best_score = score
                        best_params = result.params

            # 保存检查点
            evaluated_count = len(self.results)
            await self._save_checkpoint(evaluated_count, total_combinations, best_params, best_score)

            if self.verbose > 0:
                logger.info(f"进度: {evaluated_count}/{total_combinations} ({evaluated_count/total_combinations*100:.1f}%)")

            # 让出控制权
            await asyncio.sleep(0)

        # 找出最佳结果
        valid_results = [r for r in self.results if r.is_valid]

        if not valid_results:
            raise RuntimeError("没有有效的参数组合")

        if self.use_composite_score:
            self.best_result = min(valid_results, key=lambda x: x.composite_score)
            selection_criteria = f"综合评分 (预测精度权重={1-self.residual_weight:.0%}, 残差诊断权重={self.residual_weight:.0%})"
        else:
            self.best_result = min(valid_results, key=lambda x: getattr(x, f'mean_{self.scoring}'))
            selection_criteria = f"单一指标: {self.scoring.upper()}"

        execution_time = time.time() - start_time

        logger.info("=" * 60)
        logger.info("网格搜索完成")
        logger.info(f"最佳参数: p={self.best_result.params[0]}, d={self.best_result.params[1]}, q={self.best_result.params[2]}")
        logger.info(f"选择标准: {selection_criteria}")
        logger.info(f"执行时间: {execution_time:.2f} 秒")
        logger.info("=" * 60)

        # 生成报告
        self.report = GridSearchReport(
            best_params=self.best_result.params,
            best_score=self.best_result.composite_score if self.use_composite_score else getattr(
                self.best_result, f'mean_{self.scoring}'
            ),
            scoring_metric=self.scoring,
            all_results=self.results,
            parameter_space=self.parameter_space,
            cv_config=self.cv_config,
            execution_time=execution_time,
            timestamp=datetime.now().isoformat(),
            data_info=data_info,
            selection_criteria=selection_criteria
        )

        return self

    def get_best_params(self) -> Tuple[int, int, int]:
        """获取最佳参数"""
        if self.best_result is None:
            raise RuntimeError("请先执行fit()")
        return self.best_result.params

    def get_best_score(self) -> float:
        """获取最佳分数"""
        if self.best_result is None:
            raise RuntimeError("请先执行fit()")
        if self.use_composite_score:
            return self.best_result.composite_score
        return getattr(self.best_result, f'mean_{self.scoring}')


async def async_grid_search_arima(
    data: pd.Series,
    p_values: List[int] = None,
    d_values: List[int] = None,
    q_values: List[int] = None,
    n_splits: int = 5,
    scoring: str = 'mape',
    verbose: int = 1,
    random_state: int = 42,
    checkpoint_callback: Optional[Callable] = None,
    task_id: Optional[str] = None
) -> Tuple[Tuple[int, int, int], GridSearchReport]:
    """
    便捷的异步网格搜索函数

    Args:
        data: 时间序列数据
        p_values: p参数取值列表
        d_values: d参数取值列表
        q_values: q参数取值列表
        n_splits: 交叉验证折数
        scoring: 评分指标
        verbose: 详细程度
        random_state: 随机种子
        checkpoint_callback: 检查点回调函数
        task_id: 任务ID

    Returns:
        (最佳参数, 完整报告)
    """
    # 设置默认参数空间
    p_values = p_values or [1, 2, 3, 4]
    d_values = d_values or [0, 1, 2]
    q_values = q_values or [0, 1, 2, 3]

    # 创建参数空间
    param_space = ParameterSpace(
        p_values=p_values,
        d_values=d_values,
        q_values=q_values
    )

    # 创建交叉验证配置
    cv_config = CrossValidationConfig(n_splits=n_splits)

    # 创建网格搜索器
    searcher = AsyncTimeSeriesGridSearch(
        parameter_space=param_space,
        cv_config=cv_config,
        scoring=scoring,
        verbose=verbose,
        random_state=random_state
    )

    # 设置检查点回调
    if checkpoint_callback and task_id:
        searcher.set_checkpoint_callback(checkpoint_callback, task_id)

    # 执行搜索
    await searcher.fit(data)

    return searcher.get_best_params(), searcher.report


# 保持向后兼容的别名
TimeSeriesGridSearch = AsyncTimeSeriesGridSearch
grid_search_arima = async_grid_search_arima
