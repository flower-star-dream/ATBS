"""
数据处理器优化方案独立测试脚本
不依赖 pytest，使用标准库进行测试
"""
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import pandas as pd
import numpy as np
from datetime import datetime, timedelta
import tempfile
import traceback

from utils.data_processor import (
    DataProcessor,
    DailyPatternEffects,
    RandomPerturbation
)


class TestRunner:
    """简单测试运行器"""

    def __init__(self):
        self.passed = 0
        self.failed = 0
        self.errors = []

    def assert_true(self, condition, message=""):
        """断言为真"""
        if not condition:
            raise AssertionError(message)

    def assert_equal(self, a, b, message=""):
        """断言相等"""
        if a != b:
            raise AssertionError(f"{message} Expected {b}, got {a}")

    def assert_almost_equal(self, a, b, tol=1e-6, message=""):
        """断言近似相等"""
        if abs(a - b) > tol:
            raise AssertionError(f"{message} Expected {b} ± {tol}, got {a}")

    def assert_in(self, item, container, message=""):
        """断言包含"""
        if item not in container:
            raise AssertionError(f"{message} Expected {item} in {container}")

    def assert_raises(self, exc_type, func, *args, **kwargs):
        """断言抛出异常"""
        try:
            func(*args, **kwargs)
            raise AssertionError(f"Expected {exc_type.__name__} to be raised")
        except exc_type:
            pass

    def run_test(self, test_name, test_func):
        """运行单个测试"""
        try:
            test_func()
            self.passed += 1
            print(f"  ✓ {test_name}")
        except Exception as e:
            self.failed += 1
            error_msg = f"  ✗ {test_name}: {str(e)}"
            self.errors.append(error_msg)
            print(error_msg)
            # traceback.print_exc()

    def report(self):
        """输出测试报告"""
        print("\n" + "=" * 60)
        print(f"测试完成: 通过 {self.passed}, 失败 {self.failed}")
        print("=" * 60)
        if self.errors:
            print("\n错误详情:")
            for error in self.errors:
                print(error)
        return self.failed == 0


def test_daily_pattern_effects():
    """测试日度模式效应类"""
    runner = TestRunner()
    print("\n测试 DailyPatternEffects 类:")

    def test_default_initialization():
        effects = DailyPatternEffects()
        runner.assert_equal(effects.day_of_week_effects[0], 1.05, "周一效应")
        runner.assert_equal(effects.day_of_week_effects[6], 1.15, "周日效应")
        runner.assert_equal(effects.monthly_pattern['start_weight'], 1.08, "月初权重")
        runner.assert_equal(effects.seasonal_effects[7], 1.18, "7月效应")

    def test_get_day_of_week_effect():
        effects = DailyPatternEffects()
        runner.assert_equal(effects.get_day_of_week_effect(0), 1.05)
        runner.assert_equal(effects.get_day_of_week_effect(6), 1.15)
        runner.assert_equal(effects.get_day_of_week_effect(99), 1.0)

    def test_get_monthly_pattern_effect():
        effects = DailyPatternEffects()
        runner.assert_equal(effects.get_monthly_pattern_effect(1, 31), 1.08)
        runner.assert_equal(effects.get_monthly_pattern_effect(15, 31), 0.95)
        runner.assert_equal(effects.get_monthly_pattern_effect(31, 31), 1.10)

    def test_get_seasonal_effect():
        effects = DailyPatternEffects()
        runner.assert_equal(effects.get_seasonal_effect(7), 1.18)
        runner.assert_equal(effects.get_seasonal_effect(99), 1.0)

    def test_calculate_combined_effect():
        effects = DailyPatternEffects()
        date = datetime(2024, 7, 1)
        effect = effects.calculate_combined_effect(date)
        runner.assert_true(0.5 < effect < 2.0, f"效应值应在合理范围内: {effect}")

        date_sun = datetime(2024, 7, 28)
        effect_sun = effects.calculate_combined_effect(date_sun)
        runner.assert_true(effect_sun > effect, "周日效应应大于周一")

    runner.run_test("默认初始化", test_default_initialization)
    runner.run_test("获取星期几效应", test_get_day_of_week_effect)
    runner.run_test("获取月度内模式效应", test_get_monthly_pattern_effect)
    runner.run_test("获取季节性效应", test_get_seasonal_effect)
    runner.run_test("计算综合效应", test_calculate_combined_effect)

    return runner


def test_random_perturbation():
    """测试随机扰动生成器类"""
    runner = TestRunner()
    print("\n测试 RandomPerturbation 类:")

    def test_default_initialization():
        perturbation = RandomPerturbation()
        runner.assert_equal(perturbation.noise_level, 0.08)
        runner.assert_equal(perturbation.noise_type, 'gaussian')
        runner.assert_true(perturbation.rng is not None)

    def test_generate_noise_gaussian():
        perturbation = RandomPerturbation(noise_type='gaussian', seed=42)
        noise = perturbation.generate_noise(1000)
        runner.assert_equal(len(noise), 1000)
        runner.assert_true(abs(np.mean(noise)) < 0.05, "均值应接近0")
        runner.assert_true(0.07 < np.std(noise) < 0.09, "标准差应接近0.08")

    def test_generate_noise_uniform():
        perturbation = RandomPerturbation(noise_type='uniform', seed=42)
        noise = perturbation.generate_noise(1000)
        runner.assert_true(np.all(noise >= -0.08), "噪声应 >= -0.08")
        runner.assert_true(np.all(noise <= 0.08), "噪声应 <= 0.08")

    def test_apply_perturbation_preserve_sum():
        perturbation = RandomPerturbation(seed=42)
        values = np.array([100.0, 200.0, 300.0, 400.0])
        original_sum = np.sum(values)
        perturbed = perturbation.apply_perturbation(values, preserve_sum=True)
        runner.assert_true(np.all(perturbed >= 0), "值应非负")
        runner.assert_almost_equal(np.sum(perturbed), original_sum, tol=1e-6, message="总和应保持不变")

    def test_apply_perturbation_no_preserve_sum():
        perturbation = RandomPerturbation(seed=42)
        values = np.array([100.0, 200.0, 300.0, 400.0])
        perturbed = perturbation.apply_perturbation(values, preserve_sum=False)
        runner.assert_true(np.all(perturbed >= 0), "值应非负")
        runner.assert_true(not np.allclose(perturbed, values), "值应有变化")

    def test_reproducibility_with_seed():
        perturbation1 = RandomPerturbation(seed=42)
        perturbation2 = RandomPerturbation(seed=42)
        values = np.array([100.0, 200.0, 300.0])
        perturbed1 = perturbation1.apply_perturbation(values, preserve_sum=False)
        perturbed2 = perturbation2.apply_perturbation(values, preserve_sum=False)
        runner.assert_true(np.allclose(perturbed1, perturbed2), "相同种子应产生相同结果")

    runner.run_test("默认初始化", test_default_initialization)
    runner.run_test("生成高斯噪声", test_generate_noise_gaussian)
    runner.run_test("生成均匀噪声", test_generate_noise_uniform)
    runner.run_test("应用扰动保持总和", test_apply_perturbation_preserve_sum)
    runner.run_test("应用扰动不保持总和", test_apply_perturbation_no_preserve_sum)
    runner.run_test("随机种子可重复性", test_reproducibility_with_seed)

    return runner


def test_data_processor():
    """测试数据处理器类"""
    runner = TestRunner()
    print("\n测试 DataProcessor 类:")

    # 创建临时测试数据
    def create_temp_csv():
        dates = pd.date_range(start='2023-01-01', periods=12, freq='MS')
        passengers = [300, 280, 350, 400, 450, 500, 550, 520, 480, 420, 380, 400]
        df = pd.DataFrame({'Month': dates, 'Passengers': passengers})
        temp_file = tempfile.NamedTemporaryFile(mode='w', suffix='.csv', delete=False)
        df.to_csv(temp_file.name, index=False)
        return temp_file.name

    temp_path = create_temp_csv()

    def test_initialization():
        processor = DataProcessor()
        runner.assert_true(processor.original_data is None)
        runner.assert_true(processor.daily_data is None)
        runner.assert_true(isinstance(processor.effects, DailyPatternEffects))

    def test_load_monthly_data_success():
        processor = DataProcessor()
        result = processor.load_monthly_data(temp_path)
        runner.assert_true(result is not None)
        runner.assert_equal(len(result), 12)
        runner.assert_in('Month', result.columns)
        runner.assert_in('Passengers', result.columns)

    def test_load_monthly_data_file_not_found():
        processor = DataProcessor()
        try:
            processor.load_monthly_data('nonexistent_file.csv')
            raise AssertionError("应抛出 FileNotFoundError")
        except FileNotFoundError:
            pass

    def test_monthly_to_daily_without_loading():
        processor = DataProcessor()
        try:
            processor.monthly_to_daily()
            raise AssertionError("应抛出 ValueError")
        except ValueError as e:
            runner.assert_in('请先调用load_monthly_data', str(e))

    def test_monthly_to_daily_basic():
        processor = DataProcessor()
        processor.load_monthly_data(temp_path)
        daily_data = processor.monthly_to_daily(
            interpolation_method='cubic',
            apply_effects=False,
            apply_perturbation=False
        )
        runner.assert_true(daily_data is not None)
        runner.assert_true(len(daily_data) > 0)
        runner.assert_in('Date', daily_data.columns)
        runner.assert_in('Passengers', daily_data.columns)

    def test_monthly_to_daily_with_all_features():
        processor = DataProcessor()
        processor.load_monthly_data(temp_path)
        daily_data = processor.monthly_to_daily(
            interpolation_method='cubic',
            apply_effects=True,
            apply_perturbation=True,
            noise_level=0.08,
            random_seed=42
        )
        runner.assert_true(daily_data is not None)
        runner.assert_in('Year', daily_data.columns)
        runner.assert_in('Month', daily_data.columns)
        runner.assert_in('Day', daily_data.columns)
        runner.assert_in('DayOfWeek', daily_data.columns)
        runner.assert_in('DayName', daily_data.columns)

    def test_interpolation_methods():
        processor = DataProcessor()
        processor.load_monthly_data(temp_path)
        for method in ['cubic', 'pchip', 'linear']:
            processor2 = DataProcessor()
            processor2.load_monthly_data(temp_path)
            daily_data = processor2.monthly_to_daily(
                interpolation_method=method,
                apply_effects=False,
                apply_perturbation=False
            )
            runner.assert_true(daily_data is not None, f"{method} 插值应成功")

    def test_sum_validation():
        processor = DataProcessor()
        processor.load_monthly_data(temp_path)
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
                runner.assert_equal(
                    daily_sum[month],
                    original_sum[month],
                    f"月份 {month} 的日数据总和应等于原始月度数据"
                )

    def test_compare_monthly_daily():
        processor = DataProcessor()
        processor.load_monthly_data(temp_path)
        processor.monthly_to_daily()
        comparison = processor.compare_monthly_daily()
        runner.assert_true(comparison is not None)
        runner.assert_in('Month', comparison.columns)
        runner.assert_in('Passengers', comparison.columns)
        runner.assert_in('DailySum', comparison.columns)

    def test_add_time_features():
        processor = DataProcessor()
        processor.load_monthly_data(temp_path)
        daily_data = processor.monthly_to_daily()
        features_df = processor.add_time_features(daily_data)
        runner.assert_in('Year', features_df.columns)
        runner.assert_in('Month_Sin', features_df.columns)
        runner.assert_in('Month_Cos', features_df.columns)

    def test_get_daily_statistics():
        processor = DataProcessor()
        processor.load_monthly_data(temp_path)
        processor.monthly_to_daily()
        stats = processor.get_daily_statistics()
        runner.assert_in('total_days', stats)
        runner.assert_in('total_passengers', stats)
        runner.assert_in('avg_daily', stats)
        runner.assert_true(stats['total_days'] > 0)
        runner.assert_true(stats['total_passengers'] > 0)

    def test_get_train_test_split():
        processor = DataProcessor()
        processor.load_monthly_data(temp_path)
        processor.monthly_to_daily()
        train_data, test_data = processor.get_train_test_split(test_size=30)
        runner.assert_true(train_data is not None)
        runner.assert_true(test_data is not None)
        runner.assert_equal(len(test_data), 30)

    def test_reproducibility():
        processor1 = DataProcessor()
        processor1.load_monthly_data(temp_path)
        daily_data1 = processor1.monthly_to_daily(
            apply_effects=True,
            apply_perturbation=True,
            random_seed=42
        )

        processor2 = DataProcessor()
        processor2.load_monthly_data(temp_path)
        daily_data2 = processor2.monthly_to_daily(
            apply_effects=True,
            apply_perturbation=True,
            random_seed=42
        )

        runner.assert_true(
            np.allclose(daily_data1['Passengers'].values, daily_data2['Passengers'].values),
            "相同种子应产生相同结果"
        )

    runner.run_test("初始化", test_initialization)
    runner.run_test("成功加载月度数据", test_load_monthly_data_success)
    runner.run_test("加载不存在的文件", test_load_monthly_data_file_not_found)
    runner.run_test("未加载数据时调用转换", test_monthly_to_daily_without_loading)
    runner.run_test("基本月度到日度转换", test_monthly_to_daily_basic)
    runner.run_test("带所有特性的转换", test_monthly_to_daily_with_all_features)
    runner.run_test("不同插值方法", test_interpolation_methods)
    runner.run_test("求和校验", test_sum_validation)
    runner.run_test("月度与日度数据比较", test_compare_monthly_daily)
    runner.run_test("添加时间特征", test_add_time_features)
    runner.run_test("获取日数据统计信息", test_get_daily_statistics)
    runner.run_test("划分训练集和测试集", test_get_train_test_split)
    runner.run_test("结果可重复性", test_reproducibility)

    # 清理临时文件
    os.unlink(temp_path)

    return runner


def test_edge_cases():
    """测试边缘情况"""
    runner = TestRunner()
    print("\n测试边缘情况:")

    def test_single_month():
        temp_file = tempfile.NamedTemporaryFile(mode='w', suffix='.csv', delete=False)
        df = pd.DataFrame({'Month': ['2023-01-01'], 'Passengers': [3000]})
        df.to_csv(temp_file.name, index=False)

        try:
            processor = DataProcessor()
            processor.load_monthly_data(temp_file.name)
            daily_data = processor.monthly_to_daily()
            runner.assert_equal(len(daily_data), 31)
            runner.assert_equal(daily_data['Passengers'].sum(), 3000)
        finally:
            os.unlink(temp_file.name)

    def test_very_small_values():
        temp_file = tempfile.NamedTemporaryFile(mode='w', suffix='.csv', delete=False)
        df = pd.DataFrame({
            'Month': ['2023-01-01', '2023-02-01'],
            'Passengers': [10, 20]
        })
        df.to_csv(temp_file.name, index=False)

        try:
            processor = DataProcessor()
            processor.load_monthly_data(temp_file.name)
            daily_data = processor.monthly_to_daily()
            runner.assert_true(np.all(daily_data['Passengers'] >= 0))
            runner.assert_equal(daily_data['Passengers'].sum(), 30)
        finally:
            os.unlink(temp_file.name)

    def test_large_values():
        temp_file = tempfile.NamedTemporaryFile(mode='w', suffix='.csv', delete=False)
        df = pd.DataFrame({
            'Month': ['2023-01-01', '2023-02-01'],
            'Passengers': [1000000, 2000000]
        })
        df.to_csv(temp_file.name, index=False)

        try:
            processor = DataProcessor()
            processor.load_monthly_data(temp_file.name)
            daily_data = processor.monthly_to_daily()
            runner.assert_equal(daily_data['Passengers'].sum(), 3000000)
        finally:
            os.unlink(temp_file.name)

    def test_weekend_effect():
        temp_file = tempfile.NamedTemporaryFile(mode='w', suffix='.csv', delete=False)
        df = pd.DataFrame({
            'Month': ['2023-01-01', '2023-02-01', '2023-03-01'],
            'Passengers': [3000, 3000, 3000]
        })
        df.to_csv(temp_file.name, index=False)

        try:
            processor = DataProcessor()
            processor.load_monthly_data(temp_file.name)
            daily_data = processor.monthly_to_daily(
                apply_effects=True,
                apply_perturbation=False
            )

            weekend_mask = daily_data['DayOfWeek'].isin([5, 6])
            weekend_avg = daily_data[weekend_mask]['Passengers'].mean()
            weekday_avg = daily_data[~weekend_mask]['Passengers'].mean()

            runner.assert_true(weekend_avg > weekday_avg, "周末平均应高于工作日平均")
        finally:
            os.unlink(temp_file.name)

    runner.run_test("单个月份", test_single_month)
    runner.run_test("非常小数值", test_very_small_values)
    runner.run_test("大数值", test_large_values)
    runner.run_test("周末效应", test_weekend_effect)

    return runner


def main():
    """主函数"""
    print("=" * 60)
    print("数据处理器优化方案测试")
    print("=" * 60)

    all_runners = []

    # 运行所有测试
    all_runners.append(test_daily_pattern_effects())
    all_runners.append(test_random_perturbation())
    all_runners.append(test_data_processor())
    all_runners.append(test_edge_cases())

    # 汇总结果
    total_passed = sum(r.passed for r in all_runners)
    total_failed = sum(r.failed for r in all_runners)

    print("\n" + "=" * 60)
    print(f"总体测试结果: 通过 {total_passed}, 失败 {total_failed}")
    print("=" * 60)

    if total_failed > 0:
        print("\n失败的测试:")
        for runner in all_runners:
            for error in runner.errors:
                print(error)
        return 1
    else:
        print("\n✓ 所有测试通过!")
        return 0


if __name__ == '__main__':
    exit_code = main()
    sys.exit(exit_code)
