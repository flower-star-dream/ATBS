"""
ARIMA 预测模块
负责加载已训练的模型并进行未来预测
"""
import os
import sys
import json
import pickle
import logging
from datetime import datetime, timedelta
from typing import List, Dict, Optional, Tuple
import warnings
warnings.filterwarnings('ignore')

import numpy as np
import pandas as pd
from statsmodels.tsa.arima.model import ARIMA, ARIMAResults

# 添加 utils 到路径
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from utils.data_processor import DataProcessor

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)


class ARIMAPredictor:
    """
    ARIMA 预测器
    使用已训练的模型进行未来客流预测
    """

    def __init__(self, resources_dir: str):
        """
        初始化预测器

        Args:
            resources_dir: resources 目录路径
        """
        self.resources_dir = resources_dir
        self.model_dir = os.path.join(resources_dir, 'model')
        self.model: Optional[ARIMAResults] = None
        self.params: Optional[Dict] = None
        self.last_data: Optional[Dict] = None

        # 加载模型
        self._load_model()

    def _load_model(self):
        """
        从 resources 目录加载已训练的模型
        """
        model_path = os.path.join(self.model_dir, 'arima_model.pkl')
        params_path = os.path.join(self.model_dir, 'arima_params.json')
        last_data_path = os.path.join(self.model_dir, 'last_data.json')

        if not os.path.exists(model_path):
            raise FileNotFoundError(f"模型文件不存在: {model_path}")

        # 加载模型对象
        with open(model_path, 'rb') as f:
            self.model = pickle.load(f)
        logger.info(f"已加载模型: {model_path}")

        # 加载参数
        if os.path.exists(params_path):
            with open(params_path, 'r', encoding='utf-8') as f:
                self.params = json.load(f)
            logger.info(f"已加载模型参数: {params_path}")

        # 加载最后数据点
        if os.path.exists(last_data_path):
            with open(last_data_path, 'r', encoding='utf-8') as f:
                self.last_data = json.load(f)
            logger.info(f"已加载最后数据点: {last_data_path}")

    def predict(self, steps: int = 20, alpha: float = 0.05) -> pd.DataFrame:
        """
        预测未来指定天数的客流

        Args:
            steps: 预测天数，默认20天
            alpha: 置信区间水平，默认95%置信区间

        Returns:
            包含预测结果的 DataFrame
        """
        if self.model is None:
            raise ValueError("模型未加载")

        logger.info(f"开始预测未来 {steps} 天的客流")

        # 获取预测日期（北京时间）
        prediction_dates = DataProcessor.get_prediction_dates(steps)

        # 进行预测
        forecast_result = self.model.get_forecast(steps=steps)

        # 获取预测值
        forecast_mean = forecast_result.predicted_mean

        # 获取置信区间
        conf_int = forecast_result.conf_int(alpha=alpha)

        # 确保预测值为非负
        forecast_mean = np.maximum(forecast_mean, 0)
        conf_int_lower = np.maximum(conf_int.iloc[:, 0], 0)
        conf_int_upper = np.maximum(conf_int.iloc[:, 1], 0)

        # 构建结果 DataFrame
        result = pd.DataFrame({
            'Date': prediction_dates,
            'Predicted_Passengers': np.round(forecast_mean.values).astype(int),
            'Lower_Bound': np.round(conf_int_lower.values).astype(int),
            'Upper_Bound': np.round(conf_int_upper.values).astype(int),
            'Confidence_Level': f"{(1 - alpha) * 100:.0f}%"
        })

        # 添加星期信息
        result['DayOfWeek'] = result['Date'].dt.day_name()
        result['Weekday'] = result['Date'].dt.weekday

        # 格式化日期
        result['Date_Str'] = result['Date'].dt.strftime('%Y-%m-%d')

        logger.info("预测完成")
        logger.info(f"预测区间: {result['Date_Str'].iloc[0]} 至 {result['Date_Str'].iloc[-1]} (北京时间)")

        return result

    def predict_with_intervals(self, steps: int = 20, intervals: List[int] = None) -> Dict:
        """
        预测并返回不同置信区间的结果

        Args:
            steps: 预测天数
            intervals: 置信区间列表，如 [0.8, 0.9, 0.95]

        Returns:
            包含多组置信区间的预测结果
        """
        if intervals is None:
            intervals = [0.8, 0.95]

        result = {
            'prediction_time': DataProcessor.get_beijing_time().isoformat(),
            'model_info': self.params.get('training_history', {}),
            'forecasts': []
        }

        for conf_level in intervals:
            alpha = 1 - conf_level
            forecast_df = self.predict(steps=steps, alpha=alpha)

            forecast_dict = {
                'confidence_level': f"{conf_level * 100:.0f}%",
                'predictions': forecast_df.to_dict('records')
            }
            result['forecasts'].append(forecast_dict)

        return result

    def get_model_summary(self) -> str:
        """
        获取模型摘要信息

        Returns:
            模型摘要字符串
        """
        if self.model is None:
            return "模型未加载"

        return str(self.model.summary())

    def get_model_info(self) -> Dict:
        """
        获取模型信息

        Returns:
            模型信息字典
        """
        info = {
            'model_loaded': self.model is not None,
            'prediction_time': DataProcessor.get_beijing_time().isoformat()
        }

        if self.params:
            info.update({
                'order': self.params.get('order'),
                'aic': self.params.get('aic'),
                'bic': self.params.get('bic'),
                'sigma2': self.params.get('sigma2'),
                'training_history': self.params.get('training_history')
            })

        return info


def predict_command():
    """
    命令行预测接口
    """
    import argparse

    parser = argparse.ArgumentParser(description='ARIMA 客流预测')
    parser.add_argument('--resources', type=str, required=True, help='resources 目录路径')
    parser.add_argument('--days', type=int, default=20, help='预测天数')
    parser.add_argument('--output', type=str, help='预测结果输出文件路径')

    args = parser.parse_args()

    # 创建预测器
    predictor = ARIMAPredictor(args.resources)

    # 进行预测
    result = predictor.predict(steps=args.days)

    # 输出结果
    print("\n" + "=" * 60)
    print("客流预测结果 (北京时间)")
    print("=" * 60)
    print(result[['Date_Str', 'DayOfWeek', 'Predicted_Passengers', 'Lower_Bound', 'Upper_Bound']].to_string(index=False))
    print("=" * 60)

    # 统计信息
    print(f"\n预测统计:")
    print(f"  - 预测天数: {len(result)} 天")
    print(f"  - 平均客流: {result['Predicted_Passengers'].mean():.0f} 人/天")
    print(f"  - 最高客流: {result['Predicted_Passengers'].max()} 人 (日期: {result.loc[result['Predicted_Passengers'].idxmax(), 'Date_Str']})")
    print(f"  - 最低客流: {result['Predicted_Passengers'].min()} 人 (日期: {result.loc[result['Predicted_Passengers'].idxmin(), 'Date_Str']})")

    # 保存结果
    if args.output:
        result.to_csv(args.output, index=False, encoding='utf-8-sig')
        print(f"\n预测结果已保存到: {args.output}")

    return result


if __name__ == '__main__':
    predict_command()
