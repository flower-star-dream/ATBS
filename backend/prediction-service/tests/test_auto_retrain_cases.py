"""
自动重训功能测试用例
实现基准测试和各种日期异常场景的测试
"""
import os
import sys
import asyncio
import logging
import traceback
from datetime import datetime, timedelta
from typing import Dict, Any, Optional, Tuple
from pathlib import Path

import pandas as pd
import numpy as np

# 添加项目路径
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from tests.test_auto_retrain_framework import (
    AutoRetrainTestBase, MockDataGenerator, TestStatus,
    TestResult, TestSuiteResult, TestReportGenerator
)
from app.services.auto_retrain_service import DataRollingStorage
from app.core.config import settings

logger = logging.getLogger(__name__)


class AutoRetrainTestCases(AutoRetrainTestBase):
    """
    自动重训测试用例集
    """
    
    def __init__(self):
        super().__init__()
        self.suite_result = TestSuiteResult(
            suite_name="自动重训功能测试套件",
            start_time=datetime.now()
        )
    
    # ==================== 测试用例实现 ====================
    
    def test_continuous_data_baseline(self) -> TestResult:
        """
        测试用例1: 基准测试 - 完全连续日期数据
        验证系统在正常连续数据下的基本功能
        """
        def test_func() -> Tuple[bool, Dict[str, Any], Optional[str]]:
            # 生成连续数据
            df = self.mock_data_generator.generate_continuous_data(
                start_date="2023-01-01",
                days=60
            )
            
            # 验证日期连续性
            continuity = self.validate_date_continuity(df, 'Date')
            
            # 创建测试数据文件
            test_file = self.create_test_data_file(df, "continuous_data.csv")
            
            # 验证数据完整性
            details = {
                'data_shape': df.shape,
                'date_range': continuity['date_range'],
                'is_continuous': continuity['is_continuous'],
                'gap_count': continuity['gap_count'],
                'test_file': str(test_file)
            }
            
            # 断言：数据应该是连续的
            if not continuity['is_continuous']:
                return False, details, f"基准数据不应该有缺失，但发现 {continuity['gap_count']} 个间隔"
            
            if len(df) != 60:
                return False, details, f"数据行数应为60，实际为{len(df)}"
            
            return True, details, None
        
        return self.run_test_case(
            test_name="test_continuous_data_baseline",
            test_description="基准测试：验证完全连续日期数据场景下自动重训功能的正常工作流程",
            test_func=test_func
        )
    
    def test_single_date_missing(self) -> TestResult:
        """
        测试用例2: 单日期缺失测试
        模拟单个关键日期数据缺失场景，验证系统检测与处理能力
        """
        def test_func() -> Tuple[bool, Dict[str, Any], Optional[str]]:
            # 生成单日期缺失数据
            missing_date = "2023-02-15"
            df = self.mock_data_generator.generate_single_missing_data(
                start_date="2023-01-01",
                days=60,
                missing_date=missing_date
            )
            
            # 验证日期连续性
            continuity = self.validate_date_continuity(df, 'Date')
            
            # 创建测试数据文件
            test_file = self.create_test_data_file(df, "single_missing_data.csv")
            
            details = {
                'data_shape': df.shape,
                'expected_missing_date': missing_date,
                'date_range': continuity['date_range'],
                'is_continuous': continuity['is_continuous'],
                'gap_count': continuity['gap_count'],
                'gaps': continuity['gaps'],
                'test_file': str(test_file)
            }
            
            # 断言：应该检测到1个缺失
            if continuity['gap_count'] != 1:
                return False, details, f"应该检测到1个日期间隔，实际检测到{continuity['gap_count']}个"
            
            # 验证缺失的日期
            if continuity['gaps']:
                gap = continuity['gaps'][0]
                if gap['from'] != "2023-02-14" or gap['to'] != "2023-02-16":
                    return False, details, f"缺失日期检测错误: {gap}"
            
            # 验证数据行数
            if len(df) != 59:  # 60天减去1天缺失
                return False, details, f"数据行数应为59，实际为{len(df)}"
            
            return True, details, None
        
        return self.run_test_case(
            test_name="test_single_date_missing",
            test_description="单日期缺失测试：模拟单个关键日期数据缺失场景，验证系统检测与处理能力",
            test_func=test_func
        )
    
    def test_multiple_consecutive_missing(self) -> TestResult:
        """
        测试用例3: 多日期连续缺失测试
        模拟多个连续日期数据缺失场景，验证系统边界处理逻辑
        """
        def test_func() -> Tuple[bool, Dict[str, Any], Optional[str]]:
            # 生成多日期连续缺失数据
            missing_count = 5
            df = self.mock_data_generator.generate_multiple_missing_data(
                start_date="2023-01-01",
                days=60,
                missing_count=missing_count,
                missing_consecutive=True
            )
            
            # 验证日期连续性
            continuity = self.validate_date_continuity(df, 'Date')
            
            # 创建测试数据文件
            test_file = self.create_test_data_file(df, "multiple_missing_data.csv")
            
            details = {
                'data_shape': df.shape,
                'expected_missing_count': missing_count,
                'date_range': continuity['date_range'],
                'is_continuous': continuity['is_continuous'],
                'gap_count': continuity['gap_count'],
                'gaps': continuity['gaps'],
                'test_file': str(test_file)
            }
            
            # 断言：应该检测到1个连续缺失区间
            if continuity['gap_count'] != 1:
                return False, details, f"应该检测到1个连续间隔，实际检测到{continuity['gap_count']}个"
            
            # 验证缺失天数
            if continuity['gaps']:
                gap = continuity['gaps'][0]
                if gap['gap_days'] != missing_count:
                    return False, details, f"缺失天数应为{missing_count}，实际为{gap['gap_days']}"
            
            # 验证数据行数
            expected_rows = 60 - missing_count
            if len(df) != expected_rows:
                return False, details, f"数据行数应为{expected_rows}，实际为{len(df)}"
            
            return True, details, None
        
        return self.run_test_case(
            test_name="test_multiple_consecutive_missing",
            test_description="多日期连续缺失测试：模拟多个连续日期数据缺失场景，验证系统边界处理逻辑",
            test_func=test_func
        )
    
    def test_multiple_random_missing(self) -> TestResult:
        """
        测试用例4: 多日期随机缺失测试
        模拟多个不连续日期数据缺失场景
        """
        def test_func() -> Tuple[bool, Dict[str, Any], Optional[str]]:
            # 生成多日期随机缺失数据
            missing_count = 3
            df = self.mock_data_generator.generate_multiple_missing_data(
                start_date="2023-01-01",
                days=60,
                missing_count=missing_count,
                missing_consecutive=False
            )
            
            # 验证日期连续性
            continuity = self.validate_date_continuity(df, 'Date')
            
            # 创建测试数据文件
            test_file = self.create_test_data_file(df, "random_missing_data.csv")
            
            details = {
                'data_shape': df.shape,
                'expected_missing_count': missing_count,
                'date_range': continuity['date_range'],
                'is_continuous': continuity['is_continuous'],
                'gap_count': continuity['gap_count'],
                'gaps': continuity['gaps'],
                'test_file': str(test_file)
            }
            
            # 断言：应该检测到多个缺失区间
            if continuity['gap_count'] != missing_count:
                return False, details, f"应该检测到{missing_count}个间隔，实际检测到{continuity['gap_count']}个"
            
            # 验证数据行数
            expected_rows = 60 - missing_count
            if len(df) != expected_rows:
                return False, details, f"数据行数应为{expected_rows}，实际为{len(df)}"
            
            return True, details, None
        
        return self.run_test_case(
            test_name="test_multiple_random_missing",
            test_description="多日期随机缺失测试：模拟多个不连续日期数据缺失场景",
            test_func=test_func
        )
    
    def test_shuffled_dates(self) -> TestResult:
        """
        测试用例5: 日期乱序测试
        模拟日期序列无序排列场景，验证系统排序与连续性判断能力
        """
        def test_func() -> Tuple[bool, Dict[str, Any], Optional[str]]:
            # 生成日期乱序数据
            df = self.mock_data_generator.generate_shuffled_data(
                start_date="2023-01-01",
                days=30,
                shuffle_ratio=0.5
            )
            
            # 记录原始顺序
            original_order = df['Date'].tolist()
            
            # 验证日期连续性（会自动排序）
            continuity = self.validate_date_continuity(df, 'Date')
            
            # 创建测试数据文件
            test_file = self.create_test_data_file(df, "shuffled_data.csv")
            
            # 对数据进行排序
            df_sorted = df.sort_values('Date').reset_index(drop=True)
            
            details = {
                'data_shape': df.shape,
                'original_order_sample': original_order[:5],
                'sorted_order_sample': df_sorted['Date'].tolist()[:5],
                'date_range': continuity['date_range'],
                'is_continuous': continuity['is_continuous'],
                'test_file': str(test_file)
            }
            
            # 断言：排序后应该是连续的
            if not continuity['is_continuous']:
                return False, details, f"排序后数据应该是连续的，但发现{continuity['gap_count']}个间隔"
            
            # 验证数据完整性
            if len(df) != 30:
                return False, details, f"数据行数应为30，实际为{len(df)}"
            
            return True, details, None
        
        return self.run_test_case(
            test_name="test_shuffled_dates",
            test_description="日期乱序测试：模拟日期序列无序排列场景，验证系统排序与连续性判断能力",
            test_func=test_func
        )
    
    def test_leap_year_date(self) -> TestResult:
        """
        测试用例6: 闰年2月29日测试
        验证系统对闰年特殊日期的处理能力
        """
        def test_func() -> Tuple[bool, Dict[str, Any], Optional[str]]:
            # 生成闰年数据
            df = self.mock_data_generator.generate_extreme_date_range(
                scenario="leap_year"
            )
            
            # 验证日期连续性
            continuity = self.validate_date_continuity(df, 'Date')
            
            # 创建测试数据文件
            test_file = self.create_test_data_file(df, "leap_year_data.csv")
            
            # 检查是否包含2月29日
            has_leap_day = "2020-02-29" in df['Date'].values
            
            details = {
                'data_shape': df.shape,
                'date_range': continuity['date_range'],
                'is_continuous': continuity['is_continuous'],
                'has_leap_day': has_leap_day,
                'test_file': str(test_file)
            }
            
            # 断言：应该包含闰日且连续
            if not has_leap_day:
                return False, details, "数据应该包含2020-02-29闰日"
            
            if not continuity['is_continuous']:
                return False, details, "闰年数据应该是连续的"
            
            return True, details, None
        
        return self.run_test_case(
            test_name="test_leap_year_date",
            test_description="闰年2月29日测试：验证系统对闰年特殊日期的处理能力",
            test_func=test_func
        )
    
    def test_century_start_date(self) -> TestResult:
        """
        测试用例7: 世纪开始日期测试
        验证系统对世纪开始日期的处理能力
        """
        def test_func() -> Tuple[bool, Dict[str, Any], Optional[str]]:
            # 生成世纪开始数据
            df = self.mock_data_generator.generate_extreme_date_range(
                scenario="century_start"
            )
            
            # 验证日期连续性
            continuity = self.validate_date_continuity(df, 'Date')
            
            # 创建测试数据文件
            test_file = self.create_test_data_file(df, "century_start_data.csv")
            
            # 检查是否包含世纪开始日
            has_century_start = "2000-01-01" in df['Date'].values
            
            details = {
                'data_shape': df.shape,
                'date_range': continuity['date_range'],
                'is_continuous': continuity['is_continuous'],
                'has_century_start': has_century_start,
                'test_file': str(test_file)
            }
            
            # 断言
            if not has_century_start:
                return False, details, "数据应该包含2000-01-01世纪开始日"
            
            if not continuity['is_continuous']:
                return False, details, "世纪开始数据应该是连续的"
            
            return True, details, None
        
        return self.run_test_case(
            test_name="test_century_start_date",
            test_description="世纪开始日期测试：验证系统对世纪开始日期的处理能力",
            test_func=test_func
        )
    
    def test_very_short_date_range(self) -> TestResult:
        """
        测试用例8: 超短日期范围测试
        验证系统对极短日期范围的处理能力
        """
        def test_func() -> Tuple[bool, Dict[str, Any], Optional[str]]:
            # 生成超短日期范围数据
            df = self.mock_data_generator.generate_extreme_date_range(
                scenario="very_short"
            )
            
            # 验证日期连续性
            continuity = self.validate_date_continuity(df, 'Date')
            
            # 创建测试数据文件
            test_file = self.create_test_data_file(df, "very_short_data.csv")
            
            details = {
                'data_shape': df.shape,
                'date_range': continuity['date_range'],
                'is_continuous': continuity['is_continuous'],
                'test_file': str(test_file)
            }
            
            # 断言：应该包含3天数据且连续
            if len(df) != 3:
                return False, details, f"超短范围数据应该包含3天，实际为{len(df)}天"
            
            if not continuity['is_continuous']:
                return False, details, "超短范围数据应该是连续的"
            
            return True, details, None
        
        return self.run_test_case(
            test_name="test_very_short_date_range",
            test_description="超短日期范围测试：验证系统对极短日期范围的处理能力",
            test_func=test_func
        )
    
    def test_data_rolling_storage(self) -> TestResult:
        """
        测试用例9: 数据滚动存储测试
        验证滚动存储机制的正确性
        """
        def test_func() -> Tuple[bool, Dict[str, Any], Optional[str]]:
            # 创建临时滚动存储
            max_rows = 10
            storage = DataRollingStorage(
                data_file=self.test_data_dir / "rolling_test.csv",
                max_rows=max_rows
            )
            
            # 初始化数据
            df_initial = self.mock_data_generator.generate_monthly_format_data(
                start_date="2023-01",
                months=5
            )
            df_initial.to_csv(storage.data_file, index=False)
            
            # 添加新数据，触发滚动
            for i in range(10):
                date = f"2023-{i+6:02d}-15"
                passengers = 1000 + i * 100
                storage.append_daily_data(date, passengers)
            
            # 读取最终数据
            df_final = storage.get_current_data()
            
            details = {
                'initial_rows': len(df_initial),
                'final_rows': len(df_final) if df_final is not None else 0,
                'max_rows': max_rows,
                'data_file': str(storage.data_file)
            }
            
            # 断言：数据行数不应超过最大值
            if df_final is not None and len(df_final) > max_rows:
                return False, details, f"滚动存储数据行数不应超过{max_rows}，实际为{len(df_final)}"
            
            return True, details, None
        
        return self.run_test_case(
            test_name="test_data_rolling_storage",
            test_description="数据滚动存储测试：验证滚动存储机制的正确性",
            test_func=test_func
        )
    
    def test_monthly_format_data(self) -> TestResult:
        """
        测试用例10: 月度格式数据测试
        验证系统对月度格式数据的处理能力
        """
        def test_func() -> Tuple[bool, Dict[str, Any], Optional[str]]:
            # 生成月度格式数据
            df = self.mock_data_generator.generate_monthly_format_data(
                start_date="2023-01",
                months=12
            )
            
            # 创建测试数据文件
            test_file = self.create_test_data_file(df, "monthly_data.csv")
            
            # 验证数据格式
            has_month_col = 'Month' in df.columns
            has_passengers_col = 'Passengers' in df.columns
            
            # 验证日期格式
            try:
                pd.to_datetime(df['Month'])
                valid_date_format = True
            except:
                valid_date_format = False
            
            details = {
                'data_shape': df.shape,
                'columns': list(df.columns),
                'has_month_col': has_month_col,
                'has_passengers_col': has_passengers_col,
                'valid_date_format': valid_date_format,
                'date_range': {
                    'start': df['Month'].iloc[0],
                    'end': df['Month'].iloc[-1]
                },
                'test_file': str(test_file)
            }
            
            # 断言
            if not has_month_col or not has_passengers_col:
                return False, details, "月度数据应该包含Month和Passengers列"
            
            if not valid_date_format:
                return False, details, "Month列应该是有效的日期格式"
            
            if len(df) != 12:
                return False, details, f"月度数据应该包含12个月，实际为{len(df)}"
            
            return True, details, None
        
        return self.run_test_case(
            test_name="test_monthly_format_data",
            test_description="月度格式数据测试：验证系统对月度格式数据的处理能力",
            test_func=test_func
        )
    
    # ==================== 测试执行入口 ====================
    
    def run_all_tests(self) -> TestSuiteResult:
        """
        运行所有测试用例
        
        Returns:
            测试套件结果
        """
        logger.info("=" * 60)
        logger.info("开始执行自动重训功能测试")
        logger.info("=" * 60)
        
        # 准备测试环境
        self.setup()
        
        try:
            # 执行测试用例
            self.test_continuous_data_baseline()
            self.test_single_date_missing()
            self.test_multiple_consecutive_missing()
            self.test_multiple_random_missing()
            self.test_shuffled_dates()
            self.test_leap_year_date()
            self.test_century_start_date()
            self.test_very_short_date_range()
            self.test_data_rolling_storage()
            self.test_monthly_format_data()
            
        finally:
            # 清理测试环境
            self.teardown()
        
        # 完成测试套件
        self.suite_result.end_time = datetime.now()
        self.suite_result.results = self.test_results
        
        logger.info("=" * 60)
        logger.info("测试执行完成")
        logger.info(f"总测试数: {self.suite_result.total_tests}")
        logger.info(f"通过: {self.suite_result.passed_count}")
        logger.info(f"失败: {self.suite_result.failed_count}")
        logger.info(f"错误: {self.suite_result.error_count}")
        logger.info(f"通过率: {self.suite_result.pass_rate:.2f}%")
        logger.info("=" * 60)
        
        return self.suite_result


def main():
    """
    测试执行入口
    """
    # 配置日志
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    )
    
    # 创建测试实例
    test_cases = AutoRetrainTestCases()
    
    # 运行所有测试
    suite_result = test_cases.run_all_tests()
    
    # 生成测试报告
    report_generator = TestReportGenerator(output_dir="test_reports")
    
    json_report = report_generator.generate_json_report(suite_result)
    html_report = report_generator.generate_html_report(suite_result)
    
    print(f"\n测试报告已生成:")
    print(f"  JSON: {json_report}")
    print(f"  HTML: {html_report}")
    
    # 返回退出码
    return 0 if suite_result.failed_count == 0 and suite_result.error_count == 0 else 1


if __name__ == '__main__':
    exit_code = main()
    sys.exit(exit_code)
