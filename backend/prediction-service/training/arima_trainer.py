"""
ARIMA 模型训练模块
负责训练 ARIMA 模型并保存参数
集成网格搜索(Grid Search)优化方法
"""
import os
import sys
import json
import pickle
import logging
from datetime import datetime
from typing import Dict, Tuple, Optional, List
import warnings
warnings.filterwarnings('ignore')
from concurrent.futures import ProcessPoolExecutor, as_completed
import multiprocessing

import numpy as np
import pandas as pd
from statsmodels.tsa.arima.model import ARIMA
from statsmodels.tsa.stattools import adfuller, acf, pacf
from sklearn.metrics import mean_absolute_error, mean_squared_error

# 导入网格搜索模块
try:
    from training.grid_search import (
        TimeSeriesGridSearch, ParameterSpace, CrossValidationConfig,
        GridSearchResult, GridSearchReport, grid_search_arima
    )
    GRID_SEARCH_AVAILABLE = True
except ImportError:
    GRID_SEARCH_AVAILABLE = False
    logger.warning("网格搜索模块未找到，将使用传统AIC搜索方法")

# 添加 utils 到路径
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from utils.data_processor import DataProcessor

# 使用应用统一的日志配置，不重复调用 basicConfig
logger = logging.getLogger(__name__)


class ARIMATrainer:
    """
    ARIMA 模型训练器
    支持传统AIC搜索和网格搜索(Grid Search)两种参数优化方法
    """

    def __init__(self, p: int = 5, d: int = 1, q: int = 0):
        """
        初始化 ARIMA 训练器

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

    @staticmethod
    def _evaluate_params(args):
        """静态方法：评估一组 ARIMA 参数（用于并行计算）"""
        data, p, d, q = args
        try:
            model = ARIMA(data, order=(p, d, q))
            fitted = model.fit()
            return (p, d, q, fitted.aic, None)
        except Exception as e:
            return (p, d, q, float('inf'), str(e))

    def find_best_params(
        self,
        data: pd.Series,
        max_p: int = 5,
        max_d: int = 2,
        max_q: int = 5,
        use_parallel: bool = True,
        max_workers: int = None,
        method: str = 'grid_search',
        scoring: str = 'mape',
        n_splits: int = 5,
        random_state: int = 42
    ) -> Tuple[int, int, int]:
        """
        自动寻找最优 ARIMA 参数
        
        支持两种方法：
        1. 'grid_search': 网格搜索 + k-fold交叉验证（推荐）
        2. 'aic': 传统AIC搜索（向后兼容）

        Args:
            data: 时间序列数据
            max_p: 最大 p 值
            max_d: 最大 d 值
            max_q: 最大 q 值
            use_parallel: 是否使用并行计算
            max_workers: 并行工作进程数，默认为 CPU 核心数
            method: 搜索方法 ('grid_search' 或 'aic')
            scoring: 网格搜索评分指标 ('mae', 'rmse', 'mape', 'aic', 'bic')
            n_splits: 交叉验证折数
            random_state: 随机种子，确保可复现

        Returns:
            最优 (p, d, q) 参数
        """
        logger.info("=" * 60)
        logger.info(f"开始寻找最优 ARIMA 参数 (方法: {method})")
        logger.info("=" * 60)

        if method == 'grid_search' and GRID_SEARCH_AVAILABLE:
            return self._find_best_params_grid_search(
                data, max_p, max_d, max_q, use_parallel, max_workers,
                scoring, n_splits, random_state
            )
        else:
            if method == 'grid_search' and not GRID_SEARCH_AVAILABLE:
                logger.warning("网格搜索模块不可用，回退到AIC搜索")
            return self._find_best_params_aic(
                data, max_p, max_d, max_q, use_parallel, max_workers
            )

    def _find_best_params_grid_search(
        self,
        data: pd.Series,
        max_p: int = 5,
        max_d: int = 2,
        max_q: int = 5,
        use_parallel: bool = True,
        max_workers: int = None,
        scoring: str = 'mape',
        n_splits: int = 5,
        random_state: int = 42
    ) -> Tuple[int, int, int]:
        """
        使用网格搜索寻找最优参数
        
        Args:
            data: 时间序列数据
            max_p: 最大 p 值
            max_d: 最大 d 值
            max_q: 最大 q 值
            use_parallel: 是否使用并行计算
            max_workers: 并行工作进程数
            scoring: 评分指标
            n_splits: 交叉验证折数
            random_state: 随机种子
            
        Returns:
            最优 (p, d, q) 参数
        """
        # 设置并行作业数
        n_jobs = max_workers if max_workers else multiprocessing.cpu_count()
        if not use_parallel:
            n_jobs = 1

        # 定义参数空间
        p_values = list(range(max_p + 1))
        d_values = list(range(max_d + 1))
        q_values = list(range(max_q + 1))

        logger.info(f"参数空间: p={p_values}, d={d_values}, q={q_values}")
        logger.info(f"交叉验证: {n_splits}-fold, 评分指标: {scoring}")
        logger.info(f"并行作业数: {n_jobs}")

        try:
            # 执行网格搜索
            best_params, report = grid_search_arima(
                data=data,
                p_values=p_values,
                d_values=d_values,
                q_values=q_values,
                n_splits=n_splits,
                scoring=scoring,
                n_jobs=n_jobs,
                verbose=1,
                random_state=random_state
            )

            # 保存报告
            self.grid_search_report = report

            # 更新参数
            self.p, self.d, self.q = best_params

            logger.info("=" * 60)
            logger.info("网格搜索完成")
            logger.info(f"最佳参数: p={self.p}, d={self.d}, q={self.q}")
            logger.info(f"最佳 {scoring.upper()}: {report.best_score:.4f}")
            logger.info(f"执行时间: {report.execution_time:.2f} 秒")
            logger.info("=" * 60)

            return best_params

        except Exception as e:
            logger.error(f"网格搜索失败: {e}", exc_info=True)
            logger.warning("回退到AIC搜索方法")
            return self._find_best_params_aic(data, max_p, max_d, max_q, use_parallel, max_workers)

    def _find_best_params_aic(
        self,
        data: pd.Series,
        max_p: int = 5,
        max_d: int = 2,
        max_q: int = 5,
        use_parallel: bool = True,
        max_workers: int = None
    ) -> Tuple[int, int, int]:
        """
        使用传统AIC方法寻找最优参数（向后兼容）

        Args:
            data: 时间序列数据
            max_p: 最大 p 值
            max_d: 最大 d 值
            max_q: 最大 q 值
            use_parallel: 是否使用并行计算
            max_workers: 并行工作进程数

        Returns:
            最优 (p, d, q) 参数
        """
        logger.info("使用AIC搜索方法...")

        # 生成所有参数组合
        param_combinations = []
        for p in range(max_p + 1):
            for d in range(max_d + 1):
                for q in range(max_q + 1):
                    if p == 0 and q == 0:
                        continue
                    param_combinations.append((data, p, d, q))

        best_aic = float('inf')
        best_params = (0, 0, 0)

        if use_parallel and len(param_combinations) > 4:
            # 使用并行计算
            if max_workers is None:
                max_workers = min(multiprocessing.cpu_count(), 4)

            logger.info(f"使用并行计算，工作线程数: {max_workers}")
            completed = 0
            total = len(param_combinations)

            from concurrent.futures import ThreadPoolExecutor as ThreadExecutor
            with ThreadExecutor(max_workers=max_workers) as executor:
                futures = {executor.submit(self._evaluate_params, args): args for args in param_combinations}
                for future in as_completed(futures):
                    completed += 1
                    if completed % 10 == 0:
                        logger.info(f"参数评估进度: {completed}/{total}")

                    p, d, q, aic, error = future.result()
                    if error is None and aic < best_aic:
                        best_aic = aic
                        best_params = (p, d, q)
                        logger.info(f"发现更优参数: p={p}, d={d}, q={q}, AIC={aic:.2f}")
        else:
            # 串行计算
            logger.info("使用串行计算...")
            for args in param_combinations:
                p, d, q, aic, error = self._evaluate_params(args)
                if error is None and aic < best_aic:
                    best_aic = aic
                    best_params = (p, d, q)
                    logger.info(f"发现更优参数: p={p}, d={d}, q={q}, AIC={aic:.2f}")

        self.p, self.d, self.q = best_params
        logger.info(f"最优参数: p={self.p}, d={self.d}, q={self.q}, AIC={best_aic:.2f}")

        return best_params

    def check_stationarity(self, data: pd.Series) -> Tuple[bool, float]:
        """
        使用 ADF 检验检查数据平稳性

        Args:
            data: 时间序列数据

        Returns:
            (是否平稳, p 值)
        """
        result = adfuller(data.dropna())
        p_value = result[1]
        is_stationary = p_value < 0.05

        logger.info(f"ADF 检验结果: p-value={p_value:.4f}, 平稳性={is_stationary}")

        return is_stationary, p_value

    def train(self, data: pd.Series, validate: bool = True, test_size: int = 30) -> Dict:
        """
        训练 ARIMA 模型

        Args:
            data: 训练数据（日客流数据）
            validate: 是否进行验证
            test_size: 验证集大小

        Returns:
            训练历史记录
        """
        logger.info(f"开始训练 ARIMA({self.p},{self.d},{self.q}) 模型")
        logger.info(f"数据长度: {len(data)}")

        # 记录训练开始时间（北京时间）
        beijing_time = DataProcessor.get_beijing_time()
        self.training_history['start_time'] = beijing_time.isoformat()

        if validate:
            # 划分训练集和验证集
            train_data = data[:-test_size]
            val_data = data[-test_size:]
        else:
            train_data = data
            val_data = None

        try:
            # 训练模型
            self.model = ARIMA(train_data, order=(self.p, self.d, self.q))
            self.fitted_model = self.model.fit()

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

            # 如果有网格搜索报告，保存相关信息
            if self.grid_search_report:
                self.training_history['grid_search'] = {
                    'best_score': self.grid_search_report.best_score,
                    'scoring_metric': self.grid_search_report.scoring_metric,
                    'cv_config': self.grid_search_report.cv_config.to_dict(),
                    'execution_time': self.grid_search_report.execution_time
                }

            if validate and val_data is not None:
                # 在验证集上进行预测
                predictions = self.fitted_model.forecast(steps=len(val_data))

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
                    'test_size': test_size
                }

            self.training_history['end_time'] = DataProcessor.get_beijing_time().isoformat()
            self.training_history['data_length'] = len(data)

            return self.training_history

        except Exception as e:
            logger.error(f"模型训练失败: {str(e)}", exc_info=True)
            raise RuntimeError(f"模型训练失败: {str(e)}") from e

    def save_model(self, resources_dir: str):
        """
        保存模型参数到 resources 目录

        Args:
            resources_dir: resources 目录路径
        """
        if self.fitted_model is None:
            raise ValueError("模型尚未训练")

        # 统一路径处理：直接使用传入的 resources_dir 作为模型目录
        os.makedirs(resources_dir, exist_ok=True)

        # 保存模型对象
        model_path = os.path.join(resources_dir, 'arima_model.pkl')
        with open(model_path, 'wb') as f:
            pickle.dump(self.fitted_model, f)
        logger.info(f"模型已保存到: {model_path}")

        # 保存模型参数（JSON 格式，便于查看）
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

        # 保存网格搜索报告（如果有）
        if self.grid_search_report:
            report_path = os.path.join(resources_dir, 'grid_search_report.json')
            self.grid_search_report.save_json(report_path)

            # 生成可视化图表
            try:
                viz_dir = os.path.join(resources_dir, 'grid_search_viz')
                from training.grid_search import TimeSeriesGridSearch
                searcher = TimeSeriesGridSearch(
                    parameter_space=self.grid_search_report.parameter_space,
                    cv_config=self.grid_search_report.cv_config,
                    scoring=self.grid_search_report.scoring_metric
                )
                searcher.results = self.grid_search_report.all_results
                searcher.best_result = next(
                    (r for r in self.grid_search_report.all_results 
                     if r.params == self.grid_search_report.best_params), None
                )
                searcher.report = self.grid_search_report
                searcher.plot_results(viz_dir)
            except Exception as e:
                logger.warning(f"网格搜索可视化生成失败: {e}")

        # 保存最后的数据点（用于预测时的延续）
        last_data_path = os.path.join(resources_dir, 'last_data.json')
        if hasattr(self.fitted_model, 'data'):
            last_data = {
                'last_values': self.fitted_model.data.endog[-30:].tolist(),
                'last_date': self.training_history.get('end_time')
            }
            with open(last_data_path, 'w', encoding='utf-8') as f:
                json.dump(last_data, f, indent=2)
            logger.info(f"最后数据点已保存到: {last_data_path}")


def main():
    """
    主函数 - 用于命令行训练
    """
    import argparse

    parser = argparse.ArgumentParser(description='ARIMA 模型训练')
    parser.add_argument('--data', type=str, required=True, help='训练数据路径')
    parser.add_argument('--resources', type=str, required=True, help='resources 目录路径')
    parser.add_argument('--p', type=int, default=5, help='自回归阶数')
    parser.add_argument('--d', type=int, default=1, help='差分阶数')
    parser.add_argument('--q', type=int, default=0, help='移动平均阶数')
    parser.add_argument('--auto', action='store_true', help='自动寻找最优参数')
    parser.add_argument('--method', type=str, default='grid_search',
                       choices=['grid_search', 'aic'],
                       help='参数搜索方法 (grid_search: 网格搜索+k-fold, aic: 传统AIC)')
    parser.add_argument('--scoring', type=str, default='mape',
                       choices=['mae', 'rmse', 'mape', 'aic', 'bic'],
                       help='网格搜索评分指标')
    parser.add_argument('--n-splits', type=int, default=5,
                       help='交叉验证折数 (k-fold中的k)')
    parser.add_argument('--test-size', type=int, default=30, help='验证集大小')
    parser.add_argument('--random-state', type=int, default=42,
                       help='随机种子，确保结果可复现')

    args = parser.parse_args()

    # 加载数据
    processor = DataProcessor()
    monthly_data = processor.load_monthly_data(args.data)
    daily_data = processor.monthly_to_daily()

    # 获取客流数据序列
    passenger_series = daily_data['Passengers']

    # 创建训练器
    trainer = ARIMATrainer(p=args.p, d=args.d, q=args.q)

    # 检查平稳性
    is_stationary, p_value = trainer.check_stationarity(passenger_series)

    # 如果需要，自动寻找最优参数
    if args.auto:
        trainer.find_best_params(
            passenger_series,
            method=args.method,
            scoring=args.scoring,
            n_splits=args.n_splits,
            random_state=args.random_state
        )

    # 训练模型
    history = trainer.train(passenger_series, validate=True, test_size=args.test_size)

    # 保存模型
    trainer.save_model(args.resources)

    logger.info("训练流程完成！")


if __name__ == '__main__':
    main()
