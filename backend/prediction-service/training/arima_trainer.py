"""
ARIMA 模型训练模块
负责训练 ARIMA 模型并保存参数
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

try:
    import numba
    from numba import jit
    NUMBA_AVAILABLE = True
except ImportError:
    NUMBA_AVAILABLE = False

# 添加 utils 到路径
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from utils.data_processor import DataProcessor

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)


class ARIMATrainer:
    """
    ARIMA 模型训练器
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

    def find_best_params(self, data: pd.Series, max_p: int = 5, max_d: int = 2, max_q: int = 5,
                         use_parallel: bool = True, max_workers: int = None) -> Tuple[int, int, int]:
        """
        自动寻找最优 ARIMA 参数（基于 AIC），支持并行计算加速

        Args:
            data: 时间序列数据
            max_p: 最大 p 值
            max_d: 最大 d 值
            max_q: 最大 q 值
            use_parallel: 是否使用并行计算
            max_workers: 并行工作进程数，默认为 CPU 核心数

        Returns:
            最优 (p, d, q) 参数
        """
        logger.info("开始自动寻找最优 ARIMA 参数...")

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
                max_workers = min(multiprocessing.cpu_count(), 8)

            logger.info(f"使用并行计算，工作进程数: {max_workers}")
            completed = 0
            total = len(param_combinations)

            with ProcessPoolExecutor(max_workers=max_workers) as executor:
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
            logger.error(f"模型训练失败: {str(e)}")
            raise

    def save_model(self, resources_dir: str):
        """
        保存模型参数到 resources 目录

        Args:
            resources_dir: resources 目录路径
        """
        if self.fitted_model is None:
            raise ValueError("模型尚未训练")

        model_dir = os.path.join(resources_dir, 'model')
        os.makedirs(model_dir, exist_ok=True)

        # 保存模型对象
        model_path = os.path.join(model_dir, 'arima_model.pkl')
        with open(model_path, 'wb') as f:
            pickle.dump(self.fitted_model, f)
        logger.info(f"模型已保存到: {model_path}")

        # 保存模型参数（JSON 格式，便于查看）
        params_path = os.path.join(model_dir, 'arima_params.json')
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

        # 保存最后的数据点（用于预测时的延续）
        last_data_path = os.path.join(model_dir, 'last_data.json')
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
    parser.add_argument('--test-size', type=int, default=30, help='验证集大小')

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
        trainer.find_best_params(passenger_series)

    # 训练模型
    history = trainer.train(passenger_series, validate=True, test_size=args.test_size)

    # 保存模型
    trainer.save_model(args.resources)

    logger.info("训练流程完成！")


if __name__ == '__main__':
    main()
