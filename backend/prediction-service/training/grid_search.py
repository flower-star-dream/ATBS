"""
ARIMA 模型网格搜索(Grid Search)优化模块
实现网格搜索算法、k-fold交叉验证、残差检验和综合评估体系
优化版本：缩小搜索范围、增加Ljung-Box检验、建立综合评估体系
"""
import os
import json
import logging
import warnings
from datetime import datetime
from typing import Dict, List, Tuple, Optional, Any, Callable
from dataclasses import dataclass, field, asdict
from concurrent.futures import ProcessPoolExecutor, ThreadPoolExecutor, as_completed
import multiprocessing
import itertools
import pickle

import numpy as np
import pandas as pd
from statsmodels.tsa.arima.model import ARIMA
from statsmodels.tsa.stattools import adfuller
from statsmodels.stats.diagnostic import acorr_ljungbox
from sklearn.metrics import mean_absolute_error, mean_squared_error, mean_absolute_percentage_error
from sklearn.model_selection import TimeSeriesSplit
import matplotlib
matplotlib.use('Agg')  # 使用非交互式后端
import matplotlib.pyplot as plt
import seaborn as sns

warnings.filterwarnings('ignore')
plt.rcParams['font.sans-serif'] = ['SimHei']
plt.rcParams['axes.unicode_minus'] = False
logger = logging.getLogger(__name__)


@dataclass
class ParameterSpace:
    """参数搜索空间定义（优化版本 - 基于领域知识缩小范围）"""
    # 基于航空客流数据的领域知识设定合理范围
    # p: 自回归阶数，通常2-4足够捕捉短期依赖
    # d: 差分阶数，通常0-2（ADF检验后确定）
    # q: 移动平均阶数，通常1-3足够
    p_values: List[int] = field(default_factory=lambda: [1, 2, 3, 4])
    d_values: List[int] = field(default_factory=lambda: [0, 1, 2])
    q_values: List[int] = field(default_factory=lambda: [0, 1, 2, 3])

    def __post_init__(self):
        """验证参数空间"""
        self._validate()

    def _validate(self):
        """验证参数空间有效性"""
        # 移除 p=0 且 q=0 的组合（无效模型）
        valid_combinations = []
        for p, d, q in itertools.product(self.p_values, self.d_values, self.q_values):
            if not (p == 0 and q == 0):
                valid_combinations.append((p, d, q))
        return valid_combinations

    def get_all_combinations(self) -> List[Tuple[int, int, int]]:
        """获取所有参数组合"""
        return self._validate()

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
    n_splits: int = 5  # k-fold的k值（≥5）
    test_size: int = 30  # 每个fold的测试集大小
    gap: int = 0  # 训练集和测试集之间的间隔

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
    ljung_box_stat: float  # Ljung-Box统计量
    ljung_box_pvalue: float  # Ljung-Box p值
    is_white_noise: bool  # 是否通过白噪声检验
    residual_mean: float  # 残差均值
    residual_std: float  # 残差标准差
    residual_skewness: float  # 残差偏度
    residual_kurtosis: float  # 残差峰度
    durbin_watson: float  # Durbin-Watson统计量

    def to_dict(self) -> Dict[str, Any]:
        """转换为字典"""
        return {
            'ljung_box_stat': self.ljung_box_stat,
            'ljung_box_pvalue': self.ljung_box_pvalue,
            'is_white_noise': self.is_white_noise,
            'residual_mean': self.residual_mean,
            'residual_std': self.residual_std,
            'residual_skewness': self.residual_skewness,
            'residual_kurtosis': self.residual_kurtosis,
            'durbin_watson': self.durbin_watson
        }


@dataclass
class GridSearchResult:
    """网格搜索结果（增强版本 - 包含残差诊断）"""
    params: Tuple[int, int, int]  # (p, d, q)
    mean_mae: float  # 平均MAE
    mean_rmse: float  # 平均RMSE
    mean_mape: float  # 平均MAPE
    mean_aic: float  # 平均AIC
    mean_bic: float  # 平均BIC
    std_mae: float  # MAE标准差
    std_rmse: float  # RMSE标准差
    std_mape: float  # MAPE标准差
    fold_scores: List[Dict[str, float]] = field(default_factory=list)  # 每个fold的得分
    is_valid: bool = True  # 是否有效
    error_message: Optional[str] = None  # 错误信息
    residual_diagnostics: Optional[ResidualDiagnostics] = None  # 残差诊断结果
    composite_score: float = float('inf')  # 综合评分

    def to_dict(self) -> Dict[str, Any]:
        """转换为字典"""
        return {
            'params': self.params,
            'mean_mae': self.mean_mae,
            'mean_rmse': self.mean_rmse,
            'mean_mape': self.mean_mape,
            'mean_aic': self.mean_aic,
            'mean_bic': self.mean_bic,
            'std_mae': self.std_mae,
            'std_rmse': self.std_rmse,
            'std_mape': self.std_mape,
            'fold_scores': self.fold_scores,
            'is_valid': self.is_valid,
            'error_message': self.error_message,
            'residual_diagnostics': self.residual_diagnostics.to_dict() if self.residual_diagnostics else None,
            'composite_score': self.composite_score
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
    selection_criteria: str  # 选择标准说明

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


class ResidualAnalyzer:
    """残差分析器 - 执行Ljung-Box检验等诊断"""

    @staticmethod
    def analyze_residuals(residuals: np.ndarray, lags: int = 10) -> ResidualDiagnostics:
        """
        分析残差序列

        Args:
            residuals: 残差序列
            lags: Ljung-Box检验的滞后阶数

        Returns:
            残差诊断结果
        """
        # 移除NaN值
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

        # Ljung-Box检验
        try:
            lb_result = acorr_ljungbox(residuals, lags=lags, return_df=True)
            lb_stat = lb_result['lb_stat'].iloc[-1] if len(lb_result) > 0 else float('nan')
            lb_pvalue = lb_result['lb_pvalue'].iloc[-1] if len(lb_result) > 0 else float('nan')
            is_white_noise = lb_pvalue > 0.05  # p值>0.05表示残差是白噪声
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
        durbin_watson = ResidualAnalyzer._calculate_dw_statistic(residuals)

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


class TimeSeriesGridSearch:
    """
    时间序列网格搜索器（优化版本）
    实现网格搜索 + k-fold交叉验证 + 残差诊断 + 综合评估
    """

    def __init__(
        self,
        parameter_space: Optional[ParameterSpace] = None,
        cv_config: Optional[CrossValidationConfig] = None,
        scoring: str = 'mape',
        n_jobs: int = -1,
        verbose: int = 1,
        random_state: Optional[int] = None,
        use_composite_score: bool = True,  # 使用综合评分
        residual_weight: float = 0.3  # 残差诊断在综合评分中的权重
    ):
        """
        初始化网格搜索器

        Args:
            parameter_space: 参数搜索空间
            cv_config: 交叉验证配置
            scoring: 评分指标 ('mae', 'rmse', 'mape', 'aic', 'bic')
            n_jobs: 并行作业数，-1表示使用所有CPU
            verbose: 详细程度
            random_state: 随机种子，确保可复现
            use_composite_score: 是否使用综合评分（结合预测精度和残差诊断）
            residual_weight: 残差诊断在综合评分中的权重
        """
        self.parameter_space = parameter_space or ParameterSpace()
        self.cv_config = cv_config or CrossValidationConfig()
        self.scoring = scoring
        self.n_jobs = n_jobs if n_jobs > 0 else multiprocessing.cpu_count()
        self.verbose = verbose
        self.random_state = random_state
        self.use_composite_score = use_composite_score
        self.residual_weight = residual_weight

        # 设置随机种子
        if random_state is not None:
            np.random.seed(random_state)

        self.results: List[GridSearchResult] = []
        self.best_result: Optional[GridSearchResult] = None
        self.residual_analyzer = ResidualAnalyzer()

        logger.info(f"初始化网格搜索器: scoring={scoring}, n_jobs={n_jobs}")
        logger.info(f"参数空间: p={self.parameter_space.p_values}, "
                   f"d={self.parameter_space.d_values}, q={self.parameter_space.q_values}")
        logger.info(f"交叉验证: {self.cv_config.n_splits}-fold")
        logger.info(f"综合评估: use_composite_score={use_composite_score}, residual_weight={residual_weight}")

    def _evaluate_single_fold(
        self,
        train_data: pd.Series,
        test_data: pd.Series,
        order: Tuple[int, int, int]
    ) -> Dict[str, float]:
        """
        评估单个fold

        Args:
            train_data: 训练数据
            test_data: 测试数据
            order: ARIMA参数 (p, d, q)

        Returns:
            评估指标字典
        """
        try:
            # 训练模型
            model = ARIMA(train_data, order=order)
            fitted = model.fit()

            # 预测
            predictions = fitted.forecast(steps=len(test_data))

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

    def _cross_validate(
        self,
        data: pd.Series,
        order: Tuple[int, int, int]
    ) -> GridSearchResult:
        """
        对一组参数执行k-fold交叉验证

        Args:
            data: 时间序列数据
            order: ARIMA参数 (p, d, q)

        Returns:
            网格搜索结果
        """
        p, d, q = order

        if self.verbose > 1:
            logger.info(f"评估参数: p={p}, d={d}, q={q}")

        # 创建时间序列分割器
        n_samples = len(data)
        test_size = self.cv_config.test_size
        n_splits = self.cv_config.n_splits
        gap = self.cv_config.gap

        # 手动实现时间序列交叉验证
        fold_scores = []

        # 计算每个fold的起始位置
        total_required = n_splits * test_size + gap * (n_splits - 1)
        if total_required >= n_samples:
            # 数据不足，调整test_size
            test_size = max(10, (n_samples - gap * (n_splits - 1)) // (n_splits + 1))

        fold_starts = []
        for i in range(n_splits):
            test_end = n_samples - i * (test_size + gap)
            test_start = test_end - test_size
            if test_start < 10:  # 确保训练集至少有10个样本
                continue
            fold_starts.append((0, test_start, test_end))

        # 执行交叉验证
        for fold_idx, (train_start, test_start, test_end) in enumerate(fold_starts):
            train_data = data.iloc[train_start:test_start]
            test_data = data.iloc[test_start:test_end]

            if len(train_data) < 10 or len(test_data) < 1:
                continue

            scores = self._evaluate_single_fold(train_data, test_data, order)
            scores['fold'] = fold_idx
            fold_scores.append(scores)

        # 检查是否有有效的fold
        valid_scores = [s for s in fold_scores if s.get('valid', False)]

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

        # 在完整数据上训练模型进行残差诊断
        residual_diagnostics = None
        try:
            full_model = ARIMA(data, order=order)
            full_fitted = full_model.fit()
            residuals = full_fitted.resid
            residual_diagnostics = self.residual_analyzer.analyze_residuals(residuals)
        except Exception as e:
            logger.warning(f"残差诊断失败 (order={order}): {e}")

        # 计算综合评分
        composite_score = self._calculate_composite_score(
            means, residual_diagnostics
        )

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
        """
        计算综合评分
        结合预测精度和残差诊断结果

        Args:
            metrics: 预测指标字典
            residual_diagnostics: 残差诊断结果

        Returns:
            综合评分（越小越好）
        """
        # 预测精度评分（归一化）
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
        # Ljung-Box p值越接近1越好（残差越接近白噪声）
        # 如果p值<0.05，说明残差不是白噪声，模型欠拟合，增加惩罚
        residual_score = 0
        if not residual_diagnostics.is_white_noise:
            # 残差不是白噪声，增加惩罚
            residual_score = 100 * (0.05 - min(residual_diagnostics.ljung_box_pvalue, 0.05))

        # 综合评分 = (1-w) * 预测精度 + w * 残差评分
        w = self.residual_weight
        composite_score = (1 - w) * prediction_score + w * residual_score

        return composite_score

    @staticmethod
    def _evaluate_params_static(args: Tuple) -> GridSearchResult:
        """
        静态方法用于并行评估参数

        Args:
            args: (data, order, cv_config, scoring, verbose)
        """
        data, order, cv_config_tuple, scoring, verbose, use_composite_score, residual_weight = args

        # 重建CrossValidationConfig
        cv_config = CrossValidationConfig(
            n_splits=cv_config_tuple[0],
            test_size=cv_config_tuple[1],
            gap=cv_config_tuple[2]
        )

        # 创建临时搜索器实例
        searcher = TimeSeriesGridSearch(
            cv_config=cv_config,
            scoring=scoring,
            verbose=verbose,
            use_composite_score=use_composite_score,
            residual_weight=residual_weight
        )

        return searcher._cross_validate(data, order)

    def fit(self, data: pd.Series) -> 'TimeSeriesGridSearch':
        """
        执行网格搜索

        Args:
            data: 时间序列数据

        Returns:
            self
        """
        import time
        start_time = time.time()

        logger.info("=" * 60)
        logger.info("开始网格搜索")
        logger.info("=" * 60)

        # 获取所有参数组合
        param_combinations = self.parameter_space.get_all_combinations()
        total_combinations = len(param_combinations)

        logger.info(f"总共需要评估 {total_combinations} 个参数组合")
        logger.info(f"交叉验证: {self.cv_config.n_splits}-fold")

        # 准备数据信息
        data_info = {
            'length': len(data),
            'mean': float(data.mean()),
            'std': float(data.std()),
            'min': float(data.min()),
            'max': float(data.max())
        }

        # 执行网格搜索
        self.results = []

        if self.n_jobs > 1 and total_combinations > 4:
            # 并行执行
            logger.info(f"使用并行计算，作业数: {self.n_jobs}")

            cv_config_tuple = (
                self.cv_config.n_splits,
                self.cv_config.test_size,
                self.cv_config.gap
            )

            args_list = [
                (data, order, cv_config_tuple, self.scoring, 0,
                 self.use_composite_score, self.residual_weight)
                for order in param_combinations
            ]

            completed = 0
            with ProcessPoolExecutor(max_workers=self.n_jobs) as executor:
                futures = {executor.submit(self._evaluate_params_static, args): args
                          for args in args_list}

                for future in as_completed(futures):
                    try:
                        result = future.result()
                        self.results.append(result)
                        completed += 1

                        if self.verbose > 0 and completed % 5 == 0:
                            logger.info(f"进度: {completed}/{total_combinations} "
                                       f"({completed/total_combinations*100:.1f}%)")
                    except Exception as e:
                        logger.error(f"参数评估失败: {e}")
        else:
            # 串行执行
            logger.info("使用串行计算")
            for i, order in enumerate(param_combinations):
                result = self._cross_validate(data, order)
                self.results.append(result)

                if self.verbose > 0 and (i + 1) % 5 == 0:
                    logger.info(f"进度: {i+1}/{total_combinations} "
                               f"({(i+1)/total_combinations*100:.1f}%)")

        # 找出最佳参数
        valid_results = [r for r in self.results if r.is_valid]

        if not valid_results:
            raise RuntimeError("没有有效的参数组合，请检查数据或参数空间")

        # 根据综合评分或单一指标排序
        if self.use_composite_score:
            self.best_result = min(valid_results, key=lambda x: x.composite_score)
            selection_criteria = f"综合评分 (预测精度权重={1-self.residual_weight:.0%}, 残差诊断权重={self.residual_weight:.0%})"
        else:
            if self.scoring == 'mae':
                self.best_result = min(valid_results, key=lambda x: x.mean_mae)
            elif self.scoring == 'rmse':
                self.best_result = min(valid_results, key=lambda x: x.mean_rmse)
            elif self.scoring == 'mape':
                self.best_result = min(valid_results, key=lambda x: x.mean_mape)
            elif self.scoring == 'aic':
                self.best_result = min(valid_results, key=lambda x: x.mean_aic)
            elif self.scoring == 'bic':
                self.best_result = min(valid_results, key=lambda x: x.mean_bic)
            else:
                self.best_result = min(valid_results, key=lambda x: x.mean_mape)
            selection_criteria = f"单一指标: {self.scoring.upper()}"

        execution_time = time.time() - start_time

        logger.info("=" * 60)
        logger.info("网格搜索完成")
        logger.info(f"最佳参数: p={self.best_result.params[0]}, "
                   f"d={self.best_result.params[1]}, q={self.best_result.params[2]}")
        logger.info(f"选择标准: {selection_criteria}")

        if self.use_composite_score:
            logger.info(f"综合评分: {self.best_result.composite_score:.4f}")

        logger.info(f"预测精度 - {self.scoring.upper()}: {getattr(self.best_result, f'mean_{self.scoring}'):.4f}")

        if self.best_result.residual_diagnostics:
            rd = self.best_result.residual_diagnostics
            logger.info(f"残差诊断 - Ljung-Box p值: {rd.ljung_box_pvalue:.4f}, "
                       f"白噪声: {'是' if rd.is_white_noise else '否'}")

        logger.info(f"执行时间: {execution_time:.2f} 秒")
        logger.info("=" * 60)

        # 生成报告
        self.report = GridSearchReport(
            best_params=self.best_result.params,
            best_score=self.best_result.composite_score if self.use_composite_score else getattr(self.best_result, f'mean_{self.scoring}'),
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

    def plot_results(self, output_dir: str = "./grid_search_results"):
        """
        生成可视化对比报告

        Args:
            output_dir: 输出目录
        """
        if not self.results:
            raise RuntimeError("请先执行fit()")

        os.makedirs(output_dir, exist_ok=True)

        # 准备数据
        valid_results = [r for r in self.results if r.is_valid]

        if not valid_results:
            logger.warning("没有有效结果可供可视化")
            return

        # 1. 热力图：p vs q，颜色表示评分
        self._plot_heatmap(valid_results, output_dir)

        # 2. 柱状图：Top 10 参数组合
        self._plot_top10(valid_results, output_dir)

        # 3. 折线图：不同d值的影响
        self._plot_d_effect(valid_results, output_dir)

        # 4. 综合对比图
        self._plot_comparison(valid_results, output_dir)

        # 5. 残差诊断图（新增）
        if self.use_composite_score:
            self._plot_residual_diagnostics(valid_results, output_dir)

        logger.info(f"可视化图表已保存到: {output_dir}")

    def _plot_heatmap(self, results: List[GridSearchResult], output_dir: str):
        """绘制p-q热力图"""
        try:
            # 为每个d值创建一个热力图
            for d in self.parameter_space.d_values:
                fig, ax = plt.subplots(figsize=(10, 8))

                # 准备数据
                d_results = [r for r in results if r.params[1] == d]
                if not d_results:
                    continue

                # 创建矩阵
                p_vals = sorted(set(r.params[0] for r in d_results))
                q_vals = sorted(set(r.params[2] for r in d_results))

                matrix = np.full((len(p_vals), len(q_vals)), np.nan)
                for r in d_results:
                    p_idx = p_vals.index(r.params[0])
                    q_idx = q_vals.index(r.params[2])
                    score = r.composite_score if self.use_composite_score else getattr(r, f'mean_{self.scoring}')
                    matrix[p_idx, q_idx] = score

                # 绘制热力图
                sns.heatmap(matrix, annot=True, fmt='.2f', cmap='YlOrRd_r',
                           xticklabels=q_vals, yticklabels=p_vals,
                           ax=ax, cbar_kws={'label': '综合评分' if self.use_composite_score else self.scoring.upper()})
                ax.set_xlabel('q (移动平均阶数)')
                ax.set_ylabel('p (自回归阶数)')
                ax.set_title(f'Grid Search Results (d={d}) - {"综合评分" if self.use_composite_score else self.scoring.upper()}')

                plt.tight_layout()
                plt.savefig(os.path.join(output_dir, f'heatmap_d{d}.png'), dpi=150)
                plt.close()
        except Exception as e:
            logger.error(f"热力图绘制失败: {e}")

    def _plot_top10(self, results: List[GridSearchResult], output_dir: str):
        """绘制Top 10参数组合"""
        try:
            # 排序
            if self.use_composite_score:
                sorted_results = sorted(results, key=lambda x: x.composite_score)[:10]
            else:
                sorted_results = sorted(results, key=lambda x: getattr(x, f'mean_{self.scoring}'))[:10]

            fig, ax = plt.subplots(figsize=(12, 6))

            labels = [f"({r.params[0]},{r.params[1]},{r.params[2]})" for r in sorted_results]
            scores = [r.composite_score if self.use_composite_score else getattr(r, f'mean_{self.scoring}') for r in sorted_results]

            bars = ax.barh(range(len(labels)), scores, color='steelblue')
            ax.set_yticks(range(len(labels)))
            ax.set_yticklabels(labels)
            ax.set_xlabel('综合评分' if self.use_composite_score else f'{self.scoring.upper()} Score')
            ax.set_title(f'Top 10 Parameter Combinations by {"综合评分" if self.use_composite_score else self.scoring.upper()}')
            ax.invert_yaxis()

            # 添加数值标签
            for i, (bar, score) in enumerate(zip(bars, scores)):
                ax.text(score, i, f' {score:.2f}', va='center')

            plt.tight_layout()
            plt.savefig(os.path.join(output_dir, 'top10_params.png'), dpi=150)
            plt.close()
        except Exception as e:
            logger.error(f"Top10图绘制失败: {e}")

    def _plot_d_effect(self, results: List[GridSearchResult], output_dir: str):
        """绘制d值影响图"""
        try:
            fig, ax = plt.subplots(figsize=(10, 6))

            for d in self.parameter_space.d_values:
                d_results = [r for r in results if r.params[1] == d]
                if not d_results:
                    continue

                scores = [r.composite_score if self.use_composite_score else getattr(r, f'mean_{self.scoring}') for r in d_results]
                ax.scatter([d] * len(scores), scores, alpha=0.6, s=100, label=f'd={d}')

            ax.set_xlabel('d (差分阶数)')
            ax.set_ylabel('综合评分' if self.use_composite_score else f'{self.scoring.upper()} Score')
            ax.set_title(f'Effect of Differencing Order (d) on {"综合评分" if self.use_composite_score else self.scoring.upper()}')
            ax.legend()
            ax.grid(True, alpha=0.3)

            plt.tight_layout()
            plt.savefig(os.path.join(output_dir, 'd_effect.png'), dpi=150)
            plt.close()
        except Exception as e:
            logger.error(f"d值影响图绘制失败: {e}")

    def _plot_comparison(self, results: List[GridSearchResult], output_dir: str):
        """绘制综合对比图"""
        try:
            fig, axes = plt.subplots(2, 2, figsize=(14, 10))

            metrics = ['mae', 'rmse', 'mape', 'composite_score' if self.use_composite_score else 'aic']

            for idx, metric in enumerate(metrics):
                ax = axes[idx // 2, idx % 2]

                # 获取前20个结果
                if metric == 'composite_score':
                    sorted_results = sorted(results, key=lambda x: x.composite_score)[:20]
                else:
                    sorted_results = sorted(results, key=lambda x: getattr(x, f'mean_{metric}'))[:20]

                labels = [f"({r.params[0]},{r.params[1]},{r.params[2]})"
                         for r in sorted_results]
                scores = [r.composite_score if metric == 'composite_score' else getattr(r, f'mean_{metric}') for r in sorted_results]

                ax.barh(range(len(labels)), scores, color='steelblue')
                ax.set_yticks(range(len(labels)))
                ax.set_yticklabels(labels, fontsize=8)
                ax.set_xlabel(f'{metric.upper()} Score')
                ax.set_title(f'Top 20 by {metric.upper()}')
                ax.invert_yaxis()

            plt.tight_layout()
            plt.savefig(os.path.join(output_dir, 'comparison.png'), dpi=150)
            plt.close()
        except Exception as e:
            logger.error(f"综合对比图绘制失败: {e}")

    def _plot_residual_diagnostics(self, results: List[GridSearchResult], output_dir: str):
        """绘制残差诊断图"""
        try:
            fig, axes = plt.subplots(1, 2, figsize=(14, 5))

            # 筛选有残差诊断结果的数据
            results_with_rd = [r for r in results if r.residual_diagnostics is not None]

            if not results_with_rd:
                return

            # Ljung-Box p值分布
            ax1 = axes[0]
            p_values = [r.residual_diagnostics.ljung_box_pvalue for r in results_with_rd]
            colors = ['green' if r.residual_diagnostics.is_white_noise else 'red' for r in results_with_rd]
            ax1.scatter(range(len(p_values)), p_values, c=colors, alpha=0.6)
            ax1.axhline(y=0.05, color='r', linestyle='--', label='显著性水平(0.05)')
            ax1.set_xlabel('参数组合索引')
            ax1.set_ylabel('Ljung-Box p值')
            ax1.set_title('残差白噪声检验 (p值>0.05为白噪声)')
            ax1.legend()
            ax1.grid(True, alpha=0.3)

            # 残差标准差分布
            ax2 = axes[1]
            std_values = [r.residual_diagnostics.residual_std for r in results_with_rd]
            ax2.hist(std_values, bins=20, color='steelblue', alpha=0.7)
            ax2.set_xlabel('残差标准差')
            ax2.set_ylabel('频数')
            ax2.set_title('残差标准差分布')
            ax2.grid(True, alpha=0.3)

            plt.tight_layout()
            plt.savefig(os.path.join(output_dir, 'residual_diagnostics.png'), dpi=150)
            plt.close()
        except Exception as e:
            logger.error(f"残差诊断图绘制失败: {e}")


def grid_search_arima(
    data: pd.Series,
    p_values: List[int] = None,
    d_values: List[int] = None,
    q_values: List[int] = None,
    n_splits: int = 5,
    scoring: str = 'mape',
    n_jobs: int = -1,
    verbose: int = 1,
    random_state: int = 42,
    use_composite_score: bool = True,
    residual_weight: float = 0.3
) -> Tuple[Tuple[int, int, int], GridSearchReport]:
    """
    便捷的网格搜索函数

    Args:
        data: 时间序列数据
        p_values: p参数取值列表
        d_values: d参数取值列表
        q_values: q参数取值列表
        n_splits: 交叉验证折数
        scoring: 评分指标
        n_jobs: 并行作业数
        verbose: 详细程度
        random_state: 随机种子
        use_composite_score: 是否使用综合评分
        residual_weight: 残差诊断权重

    Returns:
        (最佳参数, 完整报告)
    """
    # 设置默认参数空间（优化后的范围）
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
    searcher = TimeSeriesGridSearch(
        parameter_space=param_space,
        cv_config=cv_config,
        scoring=scoring,
        n_jobs=n_jobs,
        verbose=verbose,
        random_state=random_state,
        use_composite_score=use_composite_score,
        residual_weight=residual_weight
    )

    # 执行搜索
    searcher.fit(data)

    return searcher.get_best_params(), searcher.report
