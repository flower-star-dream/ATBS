"""
数据处理器 - 将月数据转换为日数据
"""
import pandas as pd
import numpy as np
from datetime import datetime, timedelta
from typing import Tuple, Optional
import logging

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)


class DataProcessor:
    """
    数据处理器类
    负责将月度客流数据转换为日数据
    """

    def __init__(self):
        self.original_data: Optional[pd.DataFrame] = None
        self.daily_data: Optional[pd.DataFrame] = None

    def load_monthly_data(self, file_path: str) -> pd.DataFrame:
        """
        加载月度数据

        Args:
            file_path: CSV文件路径

        Returns:
            DataFrame包含Month和Passengers列
        """
        logger.info(f"正在加载月度数据: {file_path}")

        df = pd.read_csv(file_path)

        # 处理列名（去除空格）
        df.columns = df.columns.str.strip()

        # 转换日期格式
        df['Month'] = pd.to_datetime(df['Month'])
        df = df.sort_values('Month')

        self.original_data = df.copy()
        logger.info(f"成功加载 {len(df)} 条月度数据")

        return df

    def monthly_to_daily(self, interpolation_method: str = 'cubic') -> pd.DataFrame:
        """
        将月度数据转换为日数据，确保每月日数据之和等于该月原始数据

        使用比例分配法：
        1. 首先通过插值获得每日的相对趋势（形状）
        2. 然后对每个月，按该月内的日趋势比例分配月总量
        3. 保证：每月日数据之和 = 该月原始数据

        Args:
            interpolation_method: 插值方法，默认使用三次样条插值

        Returns:
            包含日数据的DataFrame
        """
        if self.original_data is None:
            raise ValueError("请先调用load_monthly_data加载数据")

        logger.info(f"开始将月度数据转换为日数据，使用{interpolation_method}插值方法")
        logger.info("使用比例分配法：确保每月日数据之和等于该月原始数据")

        # 创建完整的日期范围（从第一个月的1号到最后一个月的月底）
        start_date = self.original_data['Month'].min()
        end_date = self.original_data['Month'].max() + pd.offsets.MonthEnd(1)

        # 生成每日日期序列
        daily_dates = pd.date_range(start=start_date, end=end_date, freq='D')

        # 创建月度的数值索引（用于插值）
        monthly_dates = self.original_data['Month'].values
        monthly_values = self.original_data['Passengers'].values

        # 将日期转换为数值（时间戳）用于插值
        monthly_timestamps = pd.to_datetime(monthly_dates).astype(np.int64) // 10 ** 9
        daily_timestamps = daily_dates.astype(np.int64) // 10 ** 9

        # 步骤1: 使用插值方法获得每日相对趋势
        if interpolation_method == 'cubic':
            from scipy.interpolate import CubicSpline
            cs = CubicSpline(monthly_timestamps, monthly_values)
            daily_trend = cs(daily_timestamps)
        else:
            daily_trend = np.interp(daily_timestamps, monthly_timestamps, monthly_values)

        # 确保趋势值为正
        daily_trend = np.maximum(daily_trend, 0.001)  # 避免除零

        # 步骤2: 创建日数据DataFrame（初始为趋势值）
        daily_df = pd.DataFrame({
            'Date': daily_dates,
            'Trend': daily_trend
        })

        # 添加年月列用于分组
        daily_df['YearMonth'] = daily_df['Date'].dt.to_period('M')

        # 步骤3: 按月进行比例分配
        daily_passengers = []

        for _, month_row in self.original_data.iterrows():
            month_period = pd.Period(month_row['Month'], freq='M')
            month_total = month_row['Passengers']

            # 获取该月的所有日数据
            month_days = daily_df[daily_df['YearMonth'] == month_period]

            if len(month_days) == 0:
                continue

            # 计算该月的趋势总和
            month_trend_sum = month_days['Trend'].sum()

            # 按比例分配：日客流 = 月总量 * (日趋势 / 该月趋势总和)
            month_daily_values = (month_total * month_days['Trend'] / month_trend_sum).values

            # 四舍五入为整数
            month_daily_values = np.round(month_daily_values).astype(int)

            # 调整误差：确保总和等于月总量
            current_sum = month_daily_values.sum()
            diff = month_total - current_sum

            # 将误差分配到天数最多的那几天（避免某一天变化太大）
            if diff != 0:
                # 找到趋势最大的那些天进行补偿
                sorted_indices = np.argsort(month_days['Trend'].values)[::-1]
                for i in range(abs(diff)):
                    idx = sorted_indices[i % len(sorted_indices)]
                    month_daily_values[idx] += 1 if diff > 0 else -1
                    # 确保不为负
                    if month_daily_values[idx] < 0:
                        month_daily_values[idx] = 0

            daily_passengers.extend(month_daily_values)

        # 创建最终的日数据DataFrame
        self.daily_data = pd.DataFrame({
            'Date': daily_dates[:len(daily_passengers)],
            'Passengers': daily_passengers
        })

        # 验证转换结果
        self._validate_conversion()

        logger.info(f"成功生成 {len(self.daily_data)} 条日数据")

        return self.daily_data

    def _validate_conversion(self):
        """
        验证日数据转换结果
        确保每月日数据之和等于原始月数据
        """
        logger.info("验证数据转换结果...")

        # 为日数据添加年月列
        daily_copy = self.daily_data.copy()
        daily_copy['YearMonth'] = daily_copy['Date'].dt.to_period('M')

        # 按月汇总日数据
        daily_monthly_sum = daily_copy.groupby('YearMonth')['Passengers'].sum().reset_index()
        daily_monthly_sum['YearMonth'] = daily_monthly_sum['YearMonth'].dt.to_timestamp()

        # 合并比较
        comparison = self.original_data.merge(
            daily_monthly_sum,
            left_on='Month',
            right_on='YearMonth',
            suffixes=('_Original', '_DailySum')
        )

        # 检查差异
        comparison['Diff'] = comparison['Passengers_Original'] - comparison['Passengers_DailySum']
        max_diff = comparison['Diff'].abs().max()

        if max_diff == 0:
            logger.info("✓ 验证通过：所有月份的日数据之和等于原始月数据")
        else:
            logger.warning(f"⚠ 验证发现差异：最大差异为 {max_diff}")
            logger.debug(f"差异详情:\n{comparison[comparison['Diff'] != 0]}")

        return max_diff == 0

    def compare_monthly_daily(self) -> pd.DataFrame:
        """
        比较月度原始数据和日数据汇总

        Returns:
            比较结果DataFrame
        """
        if self.daily_data is None or self.original_data is None:
            raise ValueError("请先加载并转换数据")

        # 为日数据添加年月列
        daily_copy = self.daily_data.copy()
        daily_copy['YearMonth'] = daily_copy['Date'].dt.to_period('M')

        # 按月汇总日数据
        daily_monthly_sum = daily_copy.groupby('YearMonth')['Passengers'].sum().reset_index()
        daily_monthly_sum['YearMonth'] = daily_monthly_sum['YearMonth'].dt.to_timestamp()
        daily_monthly_sum.columns = ['Month', 'DailySum']

        # 合并比较
        comparison = self.original_data.merge(
            daily_monthly_sum,
            on='Month',
            how='outer'
        )
        comparison['Diff'] = comparison['Passengers'] - comparison['DailySum']
        comparison['DiffPct'] = (comparison['Diff'] / comparison['Passengers'] * 100).round(2)

        return comparison

    def add_time_features(self, df: pd.DataFrame) -> pd.DataFrame:
        """
        添加时间特征

        Args:
            df: 输入DataFrame

        Returns:
            添加时间特征后的DataFrame
        """
        df = df.copy()
        df['Year'] = df['Date'].dt.year
        df['Month'] = df['Date'].dt.month
        df['Day'] = df['Date'].dt.day
        df['DayOfWeek'] = df['Date'].dt.dayofweek
        df['DayOfYear'] = df['Date'].dt.dayofyear
        df['WeekOfYear'] = df['Date'].dt.isocalendar().week.values
        df['Quarter'] = df['Date'].dt.quarter

        # 添加周期性特征（正弦余弦变换）
        df['Month_Sin'] = np.sin(2 * np.pi * df['Month'] / 12)
        df['Month_Cos'] = np.cos(2 * np.pi * df['Month'] / 12)
        df['DayOfWeek_Sin'] = np.sin(2 * np.pi * df['DayOfWeek'] / 7)
        df['DayOfWeek_Cos'] = np.cos(2 * np.pi * df['DayOfWeek'] / 7)
        df['DayOfYear_Sin'] = np.sin(2 * np.pi * df['DayOfYear'] / 365)
        df['DayOfYear_Cos'] = np.cos(2 * np.pi * df['DayOfYear'] / 365)

        logger.info("已添加时间特征")
        return df

    def save_daily_data(self, output_path: str):
        """
        保存日数据到CSV

        Args:
            output_path: 输出文件路径
        """
        if self.daily_data is None:
            raise ValueError("没有可保存的日数据")

        self.daily_data.to_csv(output_path, index=False)
        logger.info(f"日数据已保存到: {output_path}")

    def get_train_test_split(self, test_size: int = 30) -> Tuple[pd.DataFrame, pd.DataFrame]:
        """
        划分训练集和测试集

        Args:
            test_size: 测试集天数

        Returns:
            (训练集, 测试集)
        """
        if self.daily_data is None:
            raise ValueError("请先转换数据")

        train_data = self.daily_data[:-test_size].copy()
        test_data = self.daily_data[-test_size:].copy()

        logger.info(f"训练集大小: {len(train_data)}, 测试集大小: {len(test_data)}")

        return train_data, test_data

    @staticmethod
    def get_beijing_time() -> datetime:
        """
        获取北京时间

        Returns:
            北京时间
        """
        from datetime import timezone, timedelta

        # 北京时间 = UTC+8
        beijing_tz = timezone(timedelta(hours=8))
        return datetime.now(beijing_tz)

    @staticmethod
    def get_prediction_dates(days: int = 20) -> pd.DatetimeIndex:
        """
        获取未来预测日期（北京时间）

        Args:
            days: 预测天数

        Returns:
            未来日期索引
        """
        from datetime import timezone, timedelta

        beijing_tz = timezone(timedelta(hours=8))
        today = datetime.now(beijing_tz)

        # 生成未来n天的日期
        future_dates = pd.date_range(
            start=today + timedelta(days=1),
            periods=days,
            freq='D',
            tz=beijing_tz
        )

        return future_dates


if __name__ == '__main__':
    # 测试数据处理器
    processor = DataProcessor()

    # 加载数据（示例路径）
    # monthly_data = processor.load_monthly_data('../../../resources/data/airline-passengers.csv')

    # 转换为日数据
    # daily_data = processor.monthly_to_daily()

    # 添加时间特征
    # daily_data_with_features = processor.add_time_features(daily_data)

    # 保存数据
    # processor.save_daily_data('../../../resources/data/daily-passengers.csv')

    # 获取预测日期
    future_dates = DataProcessor.get_prediction_dates(20)
    logger.info(f"未来20天预测日期:\n{future_dates}")
