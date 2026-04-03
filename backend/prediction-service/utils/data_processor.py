"""
数据处理器 - 将月度数据转换为日数据
采用样条插值、随机扰动和多因素效应模型，确保生成的日度数据既真实又保持月度总和一致性
优化版本：增强随机扰动强度，提高模型鲁棒性
"""
import os
import pandas as pd
import numpy as np
from datetime import datetime, timedelta
from typing import Tuple, Optional, Dict, List, Callable
from scipy.interpolate import CubicSpline, PchipInterpolator
import logging

# 使用应用统一的日志配置，不重复调用 basicConfig
logger = logging.getLogger(__name__)


class DailyPatternEffects:
    """
    日度模式效应类
    包含星期几效应、月度内模式和行业特定规律的配置与计算
    """

    # 星期几效应权重 (周一到周日)
    # 航空客流通常：周五、周日较高，周二、周三较低
    DEFAULT_DAY_OF_WEEK_EFFECTS = {
        0: 1.05,  # 周一
        1: 0.95,  # 周二
        2: 0.93,  # 周三
        3: 0.97,  # 周四
        4: 1.08,  # 周五
        5: 1.12,  # 周六
        6: 1.15,  # 周日
    }

    # 月度内模式权重
    # 月初和月末通常客流较高，月中相对平稳
    DEFAULT_MONTHLY_PATTERN = {
        'start_weight': 1.08,    # 月初(1-5日)
        'mid_weight': 0.95,      # 月中(6-25日)
        'end_weight': 1.10,      # 月末(26-月底)
        'start_days': 5,
        'end_days': 5,
    }

    # 行业特定规律 (航空业)
    # 节假日、旺季等影响
    DEFAULT_SEASONAL_EFFECTS = {
        # 月份: 季节性因子
        1: 0.90,   # 1月 - 节后淡季
        2: 0.92,   # 2月 - 春节影响
        3: 1.02,   # 3月 - 春季回升
        4: 1.05,   # 4月 - 春季旺季
        5: 1.08,   # 5月 - 五一假期
        6: 1.12,   # 6月 - 暑期开始
        7: 1.18,   # 7月 - 暑期高峰
        8: 1.15,   # 8月 - 暑期高峰
        9: 1.05,   # 9月 - 开学后回落
        10: 1.10,  # 10月 - 国庆假期
        11: 0.95,  # 11月 - 淡季
        12: 1.05,  # 12月 - 年末出行
    }

    def __init__(
        self,
        day_of_week_effects: Optional[Dict[int, float]] = None,
        monthly_pattern: Optional[Dict] = None,
        seasonal_effects: Optional[Dict[int, float]] = None
    ):
        """
        初始化日度模式效应

        Args:
            day_of_week_effects: 星期几效应权重字典 {0-6: weight}
            monthly_pattern: 月度内模式配置
            seasonal_effects: 季节性效应权重字典 {1-12: weight}
        """
        self.day_of_week_effects = day_of_week_effects or self.DEFAULT_DAY_OF_WEEK_EFFECTS.copy()
        self.monthly_pattern = monthly_pattern or self.DEFAULT_MONTHLY_PATTERN.copy()
        self.seasonal_effects = seasonal_effects or self.DEFAULT_SEASONAL_EFFECTS.copy()

    def get_day_of_week_effect(self, weekday: int) -> float:
        """
        获取星期几效应权重

        Args:
            weekday: 星期几 (0=周一, 6=周日)

        Returns:
            效应权重
        """
        return self.day_of_week_effects.get(weekday, 1.0)

    def get_monthly_pattern_effect(self, day_of_month: int, days_in_month: int) -> float:
        """
        获取月度内模式效应权重

        Args:
            day_of_month: 日期 (1-31)
            days_in_month: 该月总天数

        Returns:
            效应权重
        """
        start_days = self.monthly_pattern['start_days']
        end_days = self.monthly_pattern['end_days']

        if day_of_month <= start_days:
            return self.monthly_pattern['start_weight']
        elif day_of_month > days_in_month - end_days:
            return self.monthly_pattern['end_weight']
        else:
            return self.monthly_pattern['mid_weight']

    def get_seasonal_effect(self, month: int) -> float:
        """
        获取季节性效应权重

        Args:
            month: 月份 (1-12)

        Returns:
            效应权重
        """
        return self.seasonal_effects.get(month, 1.0)

    def calculate_combined_effect(
        self,
        date: datetime,
        base_value: float = 1.0
    ) -> float:
        """
        计算综合效应权重

        Args:
            date: 日期
            base_value: 基础值

        Returns:
            综合效应权重
        """
        weekday_effect = self.get_day_of_week_effect(date.weekday())
        days_in_month = (date.replace(day=28) + timedelta(days=4)).replace(day=1) - timedelta(days=1)
        days_in_month = days_in_month.day
        monthly_effect = self.get_monthly_pattern_effect(date.day, days_in_month)
        seasonal_effect = self.get_seasonal_effect(date.month)

        # 综合效应 = 各效应相乘后归一化
        combined = weekday_effect * monthly_effect * seasonal_effect

        # 归一化因子 (确保平均效应接近1.0)
        normalization_factor = 1.0 / (
            np.mean(list(self.day_of_week_effects.values())) *
            (self.monthly_pattern['start_weight'] * self.monthly_pattern['start_days'] +
             self.monthly_pattern['mid_weight'] * 20 +
             self.monthly_pattern['end_weight'] * self.monthly_pattern['end_days']) / 30 *
            np.mean(list(self.seasonal_effects.values()))
        )

        return base_value * combined * normalization_factor


class RandomPerturbation:
    """
    随机扰动生成器（优化版本）
    为日度数据添加增强的随机性以提高模型鲁棒性
    """

    # 噪声水平配置（基于实验确定的合理范围）
    NOISE_LEVELS = {
        'low': 0.08,      # 低噪声 - 保守估计
        'medium': 0.15,   # 中等噪声 - 平衡选择（默认）
        'high': 0.25,     # 高噪声 - 增强鲁棒性
        'adaptive': None  # 自适应 - 根据数据特性动态调整
    }

    def __init__(
        self,
        noise_level: float = 0.15,  # 默认提升至中等水平
        noise_type: str = 'adaptive',  # 默认使用自适应噪声
        seed: Optional[int] = None,
        preserve_trend: bool = True  # 保留趋势特征
    ):
        """
        初始化随机扰动生成器

        Args:
            noise_level: 噪声水平 (标准差占均值的比例)
            noise_type: 噪声类型 ('gaussian', 'uniform', 'lognormal', 'adaptive')
            seed: 随机种子
            preserve_trend: 是否保留趋势特征
        """
        self.noise_level = noise_level
        self.noise_type = noise_type
        self.seed = seed
        self.preserve_trend = preserve_trend
        self.rng = np.random.RandomState(seed)

    def _calculate_adaptive_noise_level(self, values: np.ndarray) -> float:
        """
        计算自适应噪声水平
        基于数据的变异系数(CV)动态调整噪声强度

        Args:
            values: 数据值数组

        Returns:
            自适应噪声水平
        """
        if len(values) == 0 or np.mean(values) == 0:
            return self.noise_level

        # 计算变异系数(CV)
        cv = np.std(values) / np.mean(values)

        # 基于CV调整噪声水平
        # CV高说明数据本身波动大，可以增加噪声
        # CV低说明数据稳定，应减小噪声
        if cv < 0.1:
            return self.noise_level * 0.5  # 数据稳定，减小噪声
        elif cv < 0.2:
            return self.noise_level * 0.8  # 数据较稳定，适度噪声
        elif cv < 0.3:
            return self.noise_level        # 使用默认噪声水平
        else:
            return min(self.noise_level * 1.2, 0.30)  # 数据波动大，增加噪声但不超过30%

    def generate_noise(self, size: int, base_values: Optional[np.ndarray] = None) -> np.ndarray:
        """
        生成随机噪声（增强版本）

        Args:
            size: 噪声数组大小
            base_values: 基础值数组 (用于自适应噪声)

        Returns:
            噪声数组
        """
        # 确定噪声水平
        effective_noise_level = self.noise_level
        if self.noise_type == 'adaptive' and base_values is not None:
            effective_noise_level = self._calculate_adaptive_noise_level(base_values)

        if self.noise_type == 'gaussian' or (self.noise_type == 'adaptive' and base_values is None):
            # 高斯噪声 - 适合一般情况
            noise = self.rng.normal(0, effective_noise_level, size)
        elif self.noise_type == 'uniform':
            # 均匀分布噪声
            noise = self.rng.uniform(-effective_noise_level, effective_noise_level, size)
        elif self.noise_type == 'lognormal':
            # 对数正态噪声 (更适合客流数据)
            sigma = effective_noise_level
            noise = self.rng.lognormal(0, sigma, size) - 1.0
        elif self.noise_type == 'adaptive':
            # 自适应噪声 - 混合策略
            # 70% 高斯噪声 + 30% 对数正态噪声
            gaussian_noise = self.rng.normal(0, effective_noise_level * 0.7, size)
            lognormal_noise = self.rng.lognormal(0, effective_noise_level * 0.5, size) - 1.0
            noise = 0.7 * gaussian_noise + 0.3 * lognormal_noise
        else:
            noise = self.rng.normal(0, effective_noise_level, size)

        return noise

    def apply_perturbation(
        self,
        values: np.ndarray,
        preserve_sum: bool = True,
        trend_weights: Optional[np.ndarray] = None
    ) -> np.ndarray:
        """
        应用随机扰动（增强版本）

        Args:
            values: 原始值数组
            preserve_sum: 是否保持总和不变
            trend_weights: 趋势权重（用于保留趋势特征）

        Returns:
            扰动后的值数组
        """
        if len(values) == 0:
            return values

        # 生成噪声
        noise = self.generate_noise(len(values), values)

        # 如果保留趋势，根据趋势权重调整噪声
        if self.preserve_trend and trend_weights is not None:
            # 趋势强的位置减少噪声，趋势弱的位置增加噪声
            adjusted_noise = noise * (1.0 - 0.5 * trend_weights)
            noise = adjusted_noise

        # 应用扰动
        perturbed = values * (1 + noise)

        # 确保非负
        perturbed = np.maximum(perturbed, 0.001)  # 最小值避免除零

        if preserve_sum:
            # 调整以保持总和一致（使用更平滑的调整策略）
            original_sum = np.sum(values)
            perturbed_sum = np.sum(perturbed)
            if perturbed_sum > 0:
                # 使用对数调整，避免极端值
                adjustment_ratio = original_sum / perturbed_sum
                # 限制调整幅度，避免过度扭曲
                adjustment_ratio = np.clip(adjustment_ratio, 0.8, 1.2)
                perturbed = perturbed * adjustment_ratio

        return perturbed

    def get_noise_stats(self) -> Dict[str, float]:
        """
        获取噪声统计信息

        Returns:
            噪声统计字典
        """
        return {
            'noise_level': self.noise_level,
            'noise_type': self.noise_type,
            'preserve_trend': self.preserve_trend
        }


class DataProcessor:
    """
    数据处理器类
    负责将月度客流数据转换为日数据，采用样条插值、随机扰动和多因素效应模型
    """

    def __init__(self):
        self.original_data: Optional[pd.DataFrame] = None
        self.daily_data: Optional[pd.DataFrame] = None
        self.effects: DailyPatternEffects = DailyPatternEffects()
        self.perturbation: RandomPerturbation = RandomPerturbation()

    def load_monthly_data(self, file_path: str) -> pd.DataFrame:
        """
        加载月度数据

        Args:
            file_path: CSV文件路径

        Returns:
            DataFrame包含Month和Passengers列

        Raises:
            FileNotFoundError: 文件不存在时抛出
            ValueError: 数据格式错误或缺少必需列时抛出
        """
        logger.info(f"正在加载月度数据: {file_path}")

        # 验证文件是否存在
        if not os.path.exists(file_path):
            raise FileNotFoundError(f"数据文件不存在: {file_path}")

        # 尝试读取 CSV 文件
        try:
            df = pd.read_csv(file_path)
        except pd.errors.EmptyDataError:
            raise ValueError("数据文件为空")
        except pd.errors.ParserError as e:
            raise ValueError(f"CSV 解析错误: {e}")

        # 验证必需列是否存在
        required_columns = {'Month', 'Passengers'}
        df.columns = df.columns.str.strip()
        missing_columns = required_columns - set(df.columns)
        if missing_columns:
            raise ValueError(f"数据文件缺少必需列: {missing_columns}")

        # 数据清洗：移除空值行
        initial_count = len(df)
        df = df.dropna(subset=['Month', 'Passengers'])
        dropped_count = initial_count - len(df)
        if dropped_count > 0:
            logger.warning(f"移除了 {dropped_count} 行包含空值的数据")

        # 转换 Passengers 为数值类型
        df['Passengers'] = pd.to_numeric(df['Passengers'], errors='coerce')

        # 再次移除转换后产生的空值
        df = df.dropna(subset=['Passengers'])

        # 验证是否有有效数据
        if df.empty:
            raise ValueError("数据文件中没有有效数据")

        # 验证 Passengers 是否为正数
        if (df['Passengers'] < 0).any():
            negative_count = (df['Passengers'] < 0).sum()
            logger.warning(f"发现 {negative_count} 条负数的乘客数据，将被移除")
            df = df[df['Passengers'] >= 0]

        if df.empty:
            raise ValueError("数据清洗后没有有效数据")

        # 转换日期格式
        try:
            df['Month'] = pd.to_datetime(df['Month'])
        except (ValueError, TypeError) as e:
            raise ValueError(f"日期格式转换失败: {e}")

        df = df.sort_values('Month')

        self.original_data = df.copy()
        logger.info(f"成功加载 {len(df)} 条月度数据，日期范围: {df['Month'].min()} 至 {df['Month'].max()}")

        return df

    def monthly_to_daily(
        self,
        interpolation_method: str = 'cubic',
        apply_effects: bool = True,
        apply_perturbation: bool = True,
        noise_level: float = 0.15,  # 默认提升至中等水平
        noise_type: str = 'adaptive',
        random_seed: Optional[int] = None
    ) -> pd.DataFrame:
        """
        将月度数据转换为日数据，确保每月日数据之和等于该月原始数据

        优化方案：
        1. 使用样条插值算法生成基础日度序列
        2. 加入增强的随机扰动以提高模型鲁棒性（默认noise_level=0.15，adaptive类型）
        3. 整合星期几效应、月度内模式及行业特定规律等影响因素
        4. 求和校验确保月度数据一致性

        Args:
            interpolation_method: 插值方法 ('cubic'-三次样条, 'pchip'-保形插值, 'linear'-线性)
            apply_effects: 是否应用多因素效应
            apply_perturbation: 是否应用随机扰动
            noise_level: 噪声水平（默认0.15，增强鲁棒性）
            noise_type: 噪声类型（默认'adaptive'自适应）
            random_seed: 随机种子

        Returns:
            包含日数据的DataFrame
        """
        if self.original_data is None:
            raise ValueError("请先调用load_monthly_data加载数据")

        logger.info(f"开始将月度数据转换为日数据")
        logger.info(f"插值方法: {interpolation_method}, 应用效应: {apply_effects}, 应用扰动: {apply_perturbation}")

        # 初始化随机扰动生成器（使用增强配置）
        if apply_perturbation:
            self.perturbation = RandomPerturbation(
                noise_level=noise_level,
                noise_type=noise_type,
                seed=random_seed,
                preserve_trend=True
            )
            logger.info(f"随机扰动配置: {self.perturbation.get_noise_stats()}")

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

        # 步骤1: 使用样条插值算法获得每日基础趋势
        daily_trend = self._interpolate_daily_trend(
            monthly_timestamps,
            monthly_values,
            daily_timestamps,
            interpolation_method
        )

        # 确保趋势值为正
        daily_trend = np.maximum(daily_trend, 0.001)

        # 步骤2: 创建日数据DataFrame（初始为趋势值）
        daily_df = pd.DataFrame({
            'Date': daily_dates,
            'Trend': daily_trend
        })

        # 添加年月列用于分组
        daily_df['YearMonth'] = daily_df['Date'].dt.to_period('M')

        # 步骤3: 按月进行精细化处理
        daily_passengers = []

        for _, month_row in self.original_data.iterrows():
            month_period = pd.Period(month_row['Month'], freq='M')
            month_total = month_row['Passengers']

            # 获取该月的所有日数据
            month_mask = daily_df['YearMonth'] == month_period
            month_days = daily_df[month_mask].copy()

            if len(month_days) == 0:
                continue

            # 获取该月的基础趋势值
            month_trend = month_days['Trend'].values

            # 计算趋势权重（用于保留趋势特征）
            trend_weights = np.abs(np.diff(month_trend, prepend=month_trend[0]))
            if np.max(trend_weights) > 0:
                trend_weights = trend_weights / np.max(trend_weights)
            else:
                trend_weights = np.zeros_like(trend_weights)

            # 步骤3a: 应用多因素效应
            if apply_effects:
                month_trend = self._apply_effects_to_month(
                    month_days['Date'].values,
                    month_trend
                )

            # 步骤3b: 应用随机扰动
            if apply_perturbation:
                month_trend = self.perturbation.apply_perturbation(
                    month_trend,
                    preserve_sum=False,
                    trend_weights=trend_weights
                )

            # 步骤3c: 按比例分配确保总和等于月总量
            month_daily_values = self._distribute_monthly_total(
                month_total,
                month_trend,
                month_days['Trend'].values  # 原始趋势用于误差补偿
            )

            daily_passengers.extend(month_daily_values)

        # 创建最终的日数据DataFrame
        self.daily_data = pd.DataFrame({
            'Date': daily_dates[:len(daily_passengers)],
            'Passengers': daily_passengers
        })

        # 添加辅助列
        self.daily_data['Year'] = self.daily_data['Date'].dt.year
        self.daily_data['Month'] = self.daily_data['Date'].dt.month
        self.daily_data['Day'] = self.daily_data['Date'].dt.day
        self.daily_data['DayOfWeek'] = self.daily_data['Date'].dt.dayofweek
        self.daily_data['DayName'] = self.daily_data['Date'].dt.day_name()

        # 验证转换结果
        validation_result = self._validate_conversion()

        if validation_result['is_valid']:
            logger.info(f"✓ 验证通过：所有月份的日数据之和等于原始月数据")
        else:
            logger.warning(f"⚠ 验证发现差异：最大差异为 {validation_result['max_diff']}")

        logger.info(f"成功生成 {len(self.daily_data)} 条日数据")

        return self.daily_data

    def _interpolate_daily_trend(
        self,
        monthly_timestamps: np.ndarray,
        monthly_values: np.ndarray,
        daily_timestamps: np.ndarray,
        method: str = 'cubic'
    ) -> np.ndarray:
        """
        使用样条插值生成日度趋势

        Args:
            monthly_timestamps: 月度时间戳数组
            monthly_values: 月度数值数组
            daily_timestamps: 日度时间戳数组
            method: 插值方法

        Returns:
            日度趋势值数组
        """
        logger.info(f"使用 {method} 插值方法生成基础日度序列")

        if method == 'cubic':
            # 三次样条插值 - 平滑且连续
            cs = CubicSpline(monthly_timestamps, monthly_values)
            daily_trend = cs(daily_timestamps)
        elif method == 'pchip':
            # PCHIP插值 - 保形插值，避免过冲
            pchip = PchipInterpolator(monthly_timestamps, monthly_values)
            daily_trend = pchip(daily_timestamps)
        elif method == 'linear':
            # 线性插值
            daily_trend = np.interp(daily_timestamps, monthly_timestamps, monthly_values)
        else:
            # 默认使用三次样条
            cs = CubicSpline(monthly_timestamps, monthly_values)
            daily_trend = cs(daily_timestamps)

        return daily_trend

    def _apply_effects_to_month(
        self,
        dates: np.ndarray,
        trend_values: np.ndarray
    ) -> np.ndarray:
        """
        为月度数据应用多因素效应

        Args:
            dates: 日期数组
            trend_values: 趋势值数组

        Returns:
            应用效应后的值数组
        """
        adjusted_values = trend_values.copy()

        for i, date in enumerate(dates):
            if isinstance(date, np.datetime64):
                date = pd.Timestamp(date).to_pydatetime()

            # 计算综合效应
            effect = self.effects.calculate_combined_effect(date, base_value=1.0)
            adjusted_values[i] *= effect

        return adjusted_values

    def _distribute_monthly_total(
        self,
        month_total: float,
        adjusted_trend: np.ndarray,
        original_trend: np.ndarray
    ) -> np.ndarray:
        """
        按比例分配月度总量到日度

        Args:
            month_total: 月度总量
            adjusted_trend: 调整后的趋势值 (已应用效应和扰动)
            original_trend: 原始趋势值 (用于误差补偿)

        Returns:
            分配后的日度值数组 (整数)
        """
        # 计算调整后的趋势总和
        trend_sum = np.sum(adjusted_trend)

        if trend_sum < 1e-10:
            # 如果趋势总和接近0，使用均匀分配
            logger.warning(f"月度趋势总和接近0，使用均匀分配")
            daily_values = np.full(len(adjusted_trend), month_total / len(adjusted_trend))
        else:
            # 按比例分配
            daily_values = month_total * adjusted_trend / trend_sum

        # 四舍五入为整数
        daily_values = np.round(daily_values).astype(int)

        # 求和校验与误差调整
        current_sum = daily_values.sum()
        diff = int(month_total - current_sum)

        if diff != 0:
            # 将误差分配到趋势最大的那些天
            sorted_indices = np.argsort(original_trend)[::-1]

            for i in range(abs(diff)):
                idx = sorted_indices[i % len(sorted_indices)]
                adjustment = 1 if diff > 0 else -1

                # 确保调整后不为负
                if daily_values[idx] + adjustment >= 0:
                    daily_values[idx] += adjustment
                else:
                    # 找下一个可以调整的索引
                    for j in range(len(sorted_indices)):
                        alt_idx = sorted_indices[j]
                        if daily_values[alt_idx] + adjustment >= 0:
                            daily_values[alt_idx] += adjustment
                            break

        return daily_values

    def _validate_conversion(self) -> Dict:
        """
        验证日数据转换结果
        确保每月日数据之和等于原始月数据

        Returns:
            验证结果字典
        """
        logger.info("执行求和校验...")

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
        comparison['DiffAbs'] = comparison['Diff'].abs()

        max_diff = comparison['DiffAbs'].max()
        total_diff = comparison['DiffAbs'].sum()

        is_valid = max_diff == 0

        result = {
            'is_valid': is_valid,
            'max_diff': max_diff,
            'total_diff': total_diff,
            'comparison': comparison,
            'month_count': len(comparison)
        }

        if is_valid:
            logger.info(f"✓ 求和校验通过：{len(comparison)} 个月份全部匹配")
        else:
            logger.warning(f"⚠ 求和校验发现差异：最大差异 {max_diff}, 总差异 {total_diff}")
            problematic = comparison[comparison['Diff'] != 0]
            if not problematic.empty:
                logger.debug(f"差异详情:\n{problematic[['YearMonth', 'Passengers_Original', 'Passengers_DailySum', 'Diff']]}")

        return result

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

    def get_daily_statistics(self) -> Dict:
        """
        获取日数据统计信息

        Returns:
            统计信息字典
        """
        if self.daily_data is None:
            raise ValueError("请先转换数据")

        stats = {
            'total_days': len(self.daily_data),
            'total_passengers': int(self.daily_data['Passengers'].sum()),
            'avg_daily': float(self.daily_data['Passengers'].mean()),
            'std_daily': float(self.daily_data['Passengers'].std()),
            'min_daily': int(self.daily_data['Passengers'].min()),
            'max_daily': int(self.daily_data['Passengers'].max()),
            'date_range': {
                'start': self.daily_data['Date'].min().strftime('%Y-%m-%d'),
                'end': self.daily_data['Date'].max().strftime('%Y-%m-%d')
            }
        }

        # 星期几统计
        dayofweek_stats = self.daily_data.groupby('DayOfWeek')['Passengers'].agg(['mean', 'std']).round(2)
        stats['dayofweek_avg'] = dayofweek_stats['mean'].to_dict()

        return stats

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

    # 获取预测日期
    future_dates = DataProcessor.get_prediction_dates(20)
    logger.info(f"未来20天预测日期:\n{future_dates}")
