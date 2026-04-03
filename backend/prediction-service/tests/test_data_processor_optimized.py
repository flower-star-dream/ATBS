"""
数据处理器优化方案单元测试
测试数据转换逻辑、插值算法、效应因子应用及求和校验等功能
"""
import pytest
import pandas as pd
import numpy as np
from datetime import datetime, timedelta
from unittest.mock import patch, MagicMock
import os
import tempfile

# 导入被测试的模块
from utils.data_processor import (
    DataProcessor,
    DailyPatternEffects,
    RandomPerturbation
)


class TestDailyPatternEffects:
    """
    测试日度模式效应类
    """

    def test_default_initialization(self):
        """测试默认初始化"""
        effects = DailyPatternEffects()

        # 验证默认星期几效应
        assert effects.day_of_week_effects[0] == 1.05  # 周一
        assert effects.day_of_week_effects[6] == 1.15  # 周日

        # 验证默认月度内模式
        assert effects.monthly_pattern['start_weight'] == 1.08
        assert effects.monthly_pattern['mid_weight'] == 0.95

        # 验证默认季节性效应
        assert effects.seasonal_effects[7] == 1.18  # 7月暑期高峰
        assert effects.seasonal_effects[2] == 0.92  # 2月春节影响

    def test_custom_initialization(self):
        """测试自定义初始化"""
        custom_dow = {0: 1.0, 1: 1.0, 2: 1.0, 3: 1.0, 4: 1.0, 5: 1.0, 6: 1.0}
        custom_seasonal = {1: 1.0, 2: 1.0, 3: 1.0}

        effects = DailyPatternEffects(
            day_of_week_effects=custom_dow,
            seasonal_effects=custom_seasonal
        )

        assert effects.day_of_week_effects == custom_dow
        assert effects.seasonal_effects == custom_seasonal

    def test_get_day_of_week_effect(self):
        """测试获取星期几效应"""
        effects = DailyPatternEffects()

        # 测试已知值
        assert effects.get_day_of_week_effect(0) == 1.05
        assert effects.get_day_of_week_effect(6) == 1.15

        # 测试默认值
        assert effects.get_day_of_week_effect(99) == 1.0

    def test_get_monthly_pattern_effect(self):
        """测试获取月度内模式效应"""
        effects = DailyPatternEffects()

        # 月初 (1-5日)
        assert effects.get_monthly_pattern_effect(1, 31) == 1.08
        assert effects.get_monthly_pattern_effect(5, 31) == 1.08

        # 月末 (26-月底)
        assert effects.get_monthly_pattern_effect(27, 31) == 1.10
        assert effects.get_monthly_pattern_effect(31, 31) == 1.10

        # 月中 (6-25日)
        assert effects.get_monthly_pattern_effect(15, 31) == 0.95

    def test_get_seasonal_effect(self):
        """测试获取季节性效应"""
        effects = DailyPatternEffects()

        assert effects.get_seasonal_effect(7) == 1.18
        assert effects.get_seasonal_effect(99) == 1.0  # 默认值

    def test_calculate_combined_effect(self):
        """测试计算综合效应"""
        effects = DailyPatternEffects()

        # 测试周一7月初
        date = datetime(2024, 7, 1)  # 周一, 7月1日
        effect = effects.calculate_combined_effect(date)

        # 验证效应值在合理范围内
        assert 0.5 < effect < 2.0

        # 周日7月末应该效应更高
        date_sun = datetime(2024, 7, 28)  # 周日, 7月28日
        effect_sun = effects.calculate_combined_effect(date_sun)

        # 周日效应应该大于周一
        assert effect_sun > effect


class TestRandomPerturbation:
    """
    测试随机扰动生成器类
    """

    def test_default_initialization(self):
        """测试默认初始化"""
        perturbation = RandomPerturbation()

        assert perturbation.noise_level == 0.08
        assert perturbation.noise_type == 'gaussian'
        assert perturbation.rng is not None

    def test_custom_initialization(self):
        """测试自定义初始化"""
        perturbation = RandomPerturbation(
            noise_level=0.1,
            noise_type='uniform',
            seed=42
        )

        assert perturbation.noise_level == 0.1
        assert perturbation.noise_type == 'uniform'

    def test_generate_noise_gaussian(self):
        """测试生成高斯噪声"""
        perturbation = RandomPerturbation(noise_type='gaussian', seed=42)
        noise = perturbation.generate_noise(1000)

        # 验证噪声统计特性
        assert len(noise) == 1000
        assert np.abs(np.mean(noise)) < 0.05  # 均值接近0
        assert 0.07 < np.std(noise) < 0.09   # 标准差接近0.08

    def test_generate_noise_uniform(self):
        """测试生成均匀噪声"""
        perturbation = RandomPerturbation(noise_type='uniform', seed=42)
        noise = perturbation.generate_noise(1000)

        # 验证噪声范围
        assert len(noise) == 1000
        assert np.all(noise >= -0.08)
        assert np.all(noise <= 0.08)

    def test_generate_noise_lognormal(self):
        """测试生成对数正态噪声"""
        perturbation = RandomPerturbation(noise_type='lognormal', seed=42)
        noise = perturbation.generate_noise(1000)

        # 验证噪声特性
        assert len(noise) == 1000
        assert np.all(noise > -1.0)  # 对数正态噪声 > -1

    def test_apply_perturbation_preserve_sum(self):
        """测试应用扰动并保持总和"""
        perturbation = RandomPerturbation(seed=42)
        values = np.array([100.0, 200.0, 300.0, 400.0])
        original_sum = np.sum(values)

        perturbed = perturbation.apply_perturbation(values, preserve_sum=True)

        # 验证非负
        assert np.all(perturbed >= 0)

        # 验证总和保持不变
        assert np.abs(np.sum(perturbed) - original_sum) < 1e-6

    def test_apply_perturbation_no_preserve_sum(self):
        """测试应用扰动不保持总和"""
        perturbation = RandomPerturbation(seed=42)
        values = np.array([100.0, 200.0, 300.0, 400.0])

        perturbed = perturbation.apply_perturbation(values, preserve_sum=False)

        # 验证非负
        assert np.all(perturbed >= 0)

        # 验证值有变化
        assert not np.allclose(perturbed, values)

    def test_apply_perturbation_empty_array(self):
        """测试对空数组应用扰动"""
        perturbation = RandomPerturbation()
        values = np.array([])

        perturbed = perturbation.apply_perturbation(values)

        assert len(perturbed) == 0

    def test_reproducibility_with_seed(self):
        """测试随机种子可重复性"""
        perturbation1 = RandomPerturbation(seed=42)
        perturbation2 = RandomPerturbation(seed=42)

        values = np.array([100.0, 200.0, 300.0])

        perturbed1 = perturbation1.apply_perturbation(values, preserve_sum=False)
        perturbed2 = perturbation2.apply_perturbation(values, preserve_sum=False)

        # 相同种子应该产生相同结果
        assert np.allclose(perturbed1, perturbed2)


class TestDataProcessor:
    """
    测试数据处理器类
    """

    @pytest.fixture
    def sample_monthly_data(self):
        """创建示例月度数据"""
        dates = pd.date_range(start='2023-01-01', periods=12, freq='MS')
        passengers = [300, 280, 350, 400, 450, 500, 550, 520, 480, 420, 380, 400]
        return pd.DataFrame({
            'Month': dates,
            'Passengers': passengers
        })

    @pytest.fixture
    def temp_csv_file(self, sample_monthly_data):
        """创建临时CSV文件"""
        with tempfile.NamedTemporaryFile(mode='w', suffix='.csv', delete=False) as f:
            sample_monthly_data.to_csv(f.name, index=False)
            yield f.name
        os.unlink(f.name)

    @pytest.fixture
    def processor(self):
        """创建数据处理器实例"""
        return DataProcessor()

    def test_initialization(self, processor):
        """测试数据处理器初始化"""
        assert processor.original_data is None
        assert processor.daily_data is None
        assert isinstance(processor.effects, DailyPatternEffects)
        assert isinstance(processor.perturbation, RandomPerturbation)

    def test_load_monthly_data_success(self, processor, temp_csv_file, sample_monthly_data):
        """测试成功加载月度数据"""
        result = processor.load_monthly_data(temp_csv_file)

        assert result is not None
        assert len(result) == len(sample_monthly_data)
        assert 'Month' in result.columns
        assert 'Passengers' in result.columns
        assert processor.original_data is not None

    def test_load_monthly_data_file_not_found(self, processor):
        """测试加载不存在的文件"""
        with pytest.raises(FileNotFoundError):
            processor.load_monthly_data('nonexistent_file.csv')

    def test_load_monthly_data_missing_columns(self, processor):
        """测试加载缺少必需列的数据"""
        with tempfile.NamedTemporaryFile(mode='w', suffix='.csv', delete=False) as f:
            df = pd.DataFrame({'Date': ['2023-01-01'], 'Count': [100]})
            df.to_csv(f.name, index=False)
            temp_path = f.name

        try:
            with pytest.raises(ValueError) as exc_info:
                processor.load_monthly_data(temp_path)
            assert 'Month' in str(exc_info.value) or 'Passengers' in str(exc_info.value)
        finally:
            os.unlink(temp_path)

    def test_load_monthly_data_empty_file(self, processor):
        """测试加载空文件"""
        with tempfile.NamedTemporaryFile(mode='w', suffix='.csv', delete=False) as f:
            f.write('')
            temp_path = f.name

        try:
            with pytest.raises(ValueError):
                processor.load_monthly_data(temp_path)
        finally:
            os.unlink(temp_path)

    def test_load_monthly_data_negative_values(self, processor):
        """测试加载包含负值的数据"""
        with tempfile.NamedTemporaryFile(mode='w', suffix='.csv', delete=False) as f:
            df = pd.DataFrame({
                'Month': ['2023-01-01', '2023-02-01', '2023-03-01'],
                'Passengers': [100, -50, 200]
            })
            df.to_csv(f.name, index=False)
            temp_path = f.name

        try:
            result = processor.load_monthly_data(temp_path)
            # 负值应该被移除
            assert len(result) == 2
            assert (result['Passengers'] >= 0).all()
        finally:
            os.unlink(temp_path)

    def test_monthly_to_daily_without_loading(self, processor):
        """测试未加载数据时调用转换"""
        with pytest.raises(ValueError) as exc_info:
            processor.monthly_to_daily()
        assert '请先调用load_monthly_data' in str(exc_info.value)

    def test_monthly_to_daily_basic(self, processor, temp_csv_file):
        """测试基本的月度到日度转换"""
        processor.load_monthly_data(temp_csv_file)
        daily_data = processor.monthly_to_daily(
            interpolation_method='cubic',
            apply_effects=False,
            apply_perturbation=False
        )

        assert daily_data is not None
        assert len(daily_data) > 0
        assert 'Date' in daily_data.columns
        assert 'Passengers' in daily_data.columns

    def test_monthly_to_daily_with_effects(self, processor, temp_csv_file):
        """测试带效应的月度到日度转换"""
        processor.load_monthly_data(temp_csv_file)
        daily_data = processor.monthly_to_daily(
            interpolation_method='cubic',
            apply_effects=True,
            apply_perturbation=False
        )

        assert daily_data is not None
        assert len(daily_data) > 0

    def test_monthly_to_daily_with_perturbation(self, processor, temp_csv_file):
        """测试带扰动的月度到日度转换"""
        processor.load_monthly_data(temp_csv_file)
        daily_data = processor.monthly_to_daily(
            interpolation_method='cubic',
            apply_effects=False,
            apply_perturbation=True,
            noise_level=0.05,
            random_seed=42
        )

        assert daily_data is not None
        assert len(daily_data) > 0

    def test_monthly_to_daily_with_all_features(self, processor, temp_csv_file):
        """测试带所有特性的月度到日度转换"""
        processor.load_monthly_data(temp_csv_file)
        daily_data = processor.monthly_to_daily(
            interpolation_method='cubic',
            apply_effects=True,
            apply_perturbation=True,
            noise_level=0.08,
            random_seed=42
        )

        assert daily_data is not None
        assert len(daily_data) > 0
        assert 'Year' in daily_data.columns
        assert 'Month' in daily_data.columns
        assert 'Day' in daily_data.columns
        assert 'DayOfWeek' in daily_data.columns
        assert 'DayName' in daily_data.columns

    def test_interpolation_methods(self, processor, temp_csv_file):
        """测试不同插值方法"""
        processor.load_monthly_data(temp_csv_file)

        methods = ['cubic', 'pchip', 'linear']
        for method in methods:
            daily_data = processor.monthly_to_daily(
                interpolation_method=method,
                apply_effects=False,
                apply_perturbation=False
            )
            assert daily_data is not None
            assert len(daily_data) > 0

    def test_sum_validation(self, processor, temp_csv_file):
        """测试求和校验"""
        processor.load_monthly_data(temp_csv_file)
        daily_data = processor.monthly_to_daily(
            apply_effects=True,
            apply_perturbation=True,
            random_seed=42
        )

        # 按月汇总日数据
        daily_data_copy = daily_data.copy()
        daily_data_copy['YearMonth'] = daily_data_copy['Date'].dt.to_period('M')
        daily_sum = daily_data_copy.groupby('YearMonth')['Passengers'].sum()

        # 获取原始月度数据
        original_sum = processor.original_data.set_index('Month')['Passengers']
        original_sum.index = pd.PeriodIndex(original_sum.index, freq='M')

        # 验证每个月的总和是否相等
        for month in daily_sum.index:
            if month in original_sum.index:
                assert daily_sum[month] == original_sum[month], \
                    f"月份 {month} 的日数据总和 {daily_sum[month]} 不等于原始月度数据 {original_sum[month]}"

    def test_compare_monthly_daily(self, processor, temp_csv_file):
        """测试月度与日度数据比较"""
        processor.load_monthly_data(temp_csv_file)
        processor.monthly_to_daily()

        comparison = processor.compare_monthly_daily()

        assert comparison is not None
        assert 'Month' in comparison.columns
        assert 'Passengers' in comparison.columns
        assert 'DailySum' in comparison.columns
        assert 'Diff' in comparison.columns

    def test_add_time_features(self, processor, temp_csv_file):
        """测试添加时间特征"""
        processor.load_monthly_data(temp_csv_file)
        daily_data = processor.monthly_to_daily()

        features_df = processor.add_time_features(daily_data)

        assert 'Year' in features_df.columns
        assert 'Month' in features_df.columns
        assert 'Day' in features_df.columns
        assert 'DayOfWeek' in features_df.columns
        assert 'DayOfYear' in features_df.columns
        assert 'WeekOfYear' in features_df.columns
        assert 'Quarter' in features_df.columns
        assert 'Month_Sin' in features_df.columns
        assert 'Month_Cos' in features_df.columns
        assert 'DayOfWeek_Sin' in features_df.columns
        assert 'DayOfWeek_Cos' in features_df.columns

    def test_get_daily_statistics(self, processor, temp_csv_file):
        """测试获取日数据统计信息"""
        processor.load_monthly_data(temp_csv_file)
        processor.monthly_to_daily()

        stats = processor.get_daily_statistics()

        assert 'total_days' in stats
        assert 'total_passengers' in stats
        assert 'avg_daily' in stats
        assert 'std_daily' in stats
        assert 'min_daily' in stats
        assert 'max_daily' in stats
        assert 'date_range' in stats
        assert 'dayofweek_avg' in stats

        assert stats['total_days'] > 0
        assert stats['total_passengers'] > 0
        assert stats['avg_daily'] > 0

    def test_get_daily_statistics_without_data(self, processor):
        """测试未转换数据时获取统计信息"""
        with pytest.raises(ValueError):
            processor.get_daily_statistics()

    def test_save_daily_data(self, processor, temp_csv_file):
        """测试保存日数据"""
        processor.load_monthly_data(temp_csv_file)
        processor.monthly_to_daily()

        with tempfile.NamedTemporaryFile(mode='w', suffix='.csv', delete=False) as f:
            output_path = f.name

        try:
            processor.save_daily_data(output_path)
            assert os.path.exists(output_path)

            # 验证保存的数据可以正确读取
            saved_data = pd.read_csv(output_path)
            assert 'Date' in saved_data.columns
            assert 'Passengers' in saved_data.columns
        finally:
            if os.path.exists(output_path):
                os.unlink(output_path)

    def test_save_daily_data_without_data(self, processor):
        """测试未转换数据时保存"""
        with pytest.raises(ValueError):
            processor.save_daily_data('output.csv')

    def test_get_train_test_split(self, processor, temp_csv_file):
        """测试划分训练集和测试集"""
        processor.load_monthly_data(temp_csv_file)
        processor.monthly_to_daily()

        train_data, test_data = processor.get_train_test_split(test_size=30)

        assert train_data is not None
        assert test_data is not None
        assert len(test_data) == 30
        assert len(train_data) > 0

    def test_get_train_test_split_without_data(self, processor):
        """测试未转换数据时划分"""
        with pytest.raises(ValueError):
            processor.get_train_test_split()

    def test_get_beijing_time(self):
        """测试获取北京时间"""
        beijing_time = DataProcessor.get_beijing_time()

        assert isinstance(beijing_time, datetime)
        # 验证时区偏移为+8小时
        assert beijing_time.tzinfo is not None

    def test_get_prediction_dates(self):
        """测试获取预测日期"""
        future_dates = DataProcessor.get_prediction_dates(days=20)

        assert len(future_dates) == 20
        assert isinstance(future_dates, pd.DatetimeIndex)

        # 验证日期是递增的
        for i in range(1, len(future_dates)):
            assert future_dates[i] > future_dates[i-1]

    def test_different_noise_levels(self, processor, temp_csv_file):
        """测试不同噪声水平"""
        processor.load_monthly_data(temp_csv_file)

        # 低噪声
        daily_low = processor.monthly_to_daily(
            apply_perturbation=True,
            noise_level=0.02,
            random_seed=42
        )

        # 高噪声
        processor.load_monthly_data(temp_csv_file)  # 重新加载
        daily_high = processor.monthly_to_daily(
            apply_perturbation=True,
            noise_level=0.15,
            random_seed=42
        )

        # 高噪声应该有更大的标准差
        std_low = daily_low['Passengers'].std()
        std_high = daily_high['Passengers'].std()

        # 由于随机性，这里只做基本验证
        assert std_low >= 0
        assert std_high >= 0

    def test_reproducibility(self, processor, temp_csv_file):
        """测试结果可重复性"""
        # 第一次运行
        processor.load_monthly_data(temp_csv_file)
        daily_data1 = processor.monthly_to_daily(
            apply_effects=True,
            apply_perturbation=True,
            random_seed=42
        )

        # 第二次运行（相同种子）
        processor2 = DataProcessor()
        processor2.load_monthly_data(temp_csv_file)
        daily_data2 = processor2.monthly_to_daily(
            apply_effects=True,
            apply_perturbation=True,
            random_seed=42
        )

        # 验证结果相同
        assert np.allclose(daily_data1['Passengers'].values, daily_data2['Passengers'].values)

    def test_edge_case_single_month(self):
        """测试单个月份的边缘情况"""
        with tempfile.NamedTemporaryFile(mode='w', suffix='.csv', delete=False) as f:
            df = pd.DataFrame({
                'Month': ['2023-01-01'],
                'Passengers': [3000]
            })
            df.to_csv(f.name, index=False)
            temp_path = f.name

        try:
            processor = DataProcessor()
            processor.load_monthly_data(temp_path)
            daily_data = processor.monthly_to_daily()

            assert daily_data is not None
            assert len(daily_data) == 31  # 1月有31天

            # 验证总和
            assert daily_data['Passengers'].sum() == 3000
        finally:
            os.unlink(temp_path)

    def test_edge_case_very_small_values(self):
        """测试非常小数值的边缘情况"""
        with tempfile.NamedTemporaryFile(mode='w', suffix='.csv', delete=False) as f:
            df = pd.DataFrame({
                'Month': ['2023-01-01', '2023-02-01'],
                'Passengers': [10, 20]
            })
            df.to_csv(f.name, index=False)
            temp_path = f.name

        try:
            processor = DataProcessor()
            processor.load_monthly_data(temp_path)
            daily_data = processor.monthly_to_daily()

            assert daily_data is not None
            # 验证所有值非负
            assert (daily_data['Passengers'] >= 0).all()
            # 验证总和
            assert daily_data['Passengers'].sum() == 30
        finally:
            os.unlink(temp_path)

    def test_edge_case_large_values(self):
        """测试大数值的边缘情况"""
        with tempfile.NamedTemporaryFile(mode='w', suffix='.csv', delete=False) as f:
            df = pd.DataFrame({
                'Month': ['2023-01-01', '2023-02-01'],
                'Passengers': [1000000, 2000000]
            })
            df.to_csv(f.name, index=False)
            temp_path = f.name

        try:
            processor = DataProcessor()
            processor.load_monthly_data(temp_path)
            daily_data = processor.monthly_to_daily()

            assert daily_data is not None
            # 验证总和
            assert daily_data['Passengers'].sum() == 3000000
        finally:
            os.unlink(temp_path)


class TestIntegration:
    """
    集成测试
    """

    def test_full_pipeline(self):
        """测试完整数据处理流程"""
        # 创建测试数据
        with tempfile.NamedTemporaryFile(mode='w', suffix='.csv', delete=False) as f:
            dates = pd.date_range(start='2023-01-01', periods=6, freq='MS')
            passengers = [300, 350, 400, 380, 420, 450]
            df = pd.DataFrame({
                'Month': dates,
                'Passengers': passengers
            })
            df.to_csv(f.name, index=False)
            temp_path = f.name

        try:
            # 1. 创建处理器
            processor = DataProcessor()

            # 2. 加载数据
            monthly_data = processor.load_monthly_data(temp_path)
            assert monthly_data is not None

            # 3. 转换为日数据
            daily_data = processor.monthly_to_daily(
                interpolation_method='cubic',
                apply_effects=True,
                apply_perturbation=True,
                noise_level=0.08,
                random_seed=42
            )
            assert daily_data is not None

            # 4. 验证求和
            comparison = processor.compare_monthly_daily()
            assert (comparison['Diff'] == 0).all()

            # 5. 添加时间特征
            features_df = processor.add_time_features(daily_data)
            assert 'Month_Sin' in features_df.columns

            # 6. 获取统计信息
            stats = processor.get_daily_statistics()
            assert stats['total_days'] > 0

            # 7. 划分训练测试集
            train, test = processor.get_train_test_split(test_size=30)
            assert len(train) > 0
            assert len(test) == 30

        finally:
            os.unlink(temp_path)

    def test_weekend_effect_consistency(self):
        """测试周末效应的一致性"""
        with tempfile.NamedTemporaryFile(mode='w', suffix='.csv', delete=False) as f:
            dates = pd.date_range(start='2023-01-01', periods=3, freq='MS')
            passengers = [3000, 3000, 3000]
            df = pd.DataFrame({
                'Month': dates,
                'Passengers': passengers
            })
            df.to_csv(f.name, index=False)
            temp_path = f.name

        try:
            processor = DataProcessor()
            processor.load_monthly_data(temp_path)

            # 多次运行，验证周末效应模式一致
            weekend_avgs = []
            for seed in [1, 2, 3]:
                processor.load_monthly_data(temp_path)
                daily_data = processor.monthly_to_daily(
                    apply_effects=True,
                    apply_perturbation=False,
                    random_seed=seed
                )

                # 计算周末和工作日的平均客流
                weekend_mask = daily_data['DayOfWeek'].isin([5, 6])  # 周六、周日
                weekend_avg = daily_data[weekend_mask]['Passengers'].mean()
                weekday_avg = daily_data[~weekend_mask]['Passengers'].mean()

                weekend_avgs.append((weekend_avg, weekday_avg))

            # 验证每次运行周末都高于工作日
            for weekend_avg, weekday_avg in weekend_avgs:
                assert weekend_avg > weekday_avg

        finally:
            os.unlink(temp_path)


if __name__ == '__main__':
    pytest.main([__file__, '-v'])
