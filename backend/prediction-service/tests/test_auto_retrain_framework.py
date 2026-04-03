"""
自动重训功能测试框架
提供测试基类、数据生成工具、测试执行控制和报告生成
"""
import os
import sys
import json
import asyncio
import logging
import tempfile
import shutil
from datetime import datetime, timedelta
from pathlib import Path
from typing import Dict, List, Optional, Any, Callable, Tuple
from dataclasses import dataclass, field, asdict
from enum import Enum
import time
import random
import csv
import pandas as pd
import numpy as np

# 添加项目路径
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from app.core.config import settings
from app.services.auto_retrain_service import DataRollingStorage, AutoRetrainService

logger = logging.getLogger(__name__)


class TestStatus(Enum):
    """测试状态枚举"""
    PENDING = "pending"
    RUNNING = "running"
    PASSED = "passed"
    FAILED = "failed"
    SKIPPED = "skipped"
    ERROR = "error"


@dataclass
class TestResult:
    """测试结果数据类"""
    test_name: str
    test_description: str
    status: TestStatus
    start_time: Optional[datetime] = None
    end_time: Optional[datetime] = None
    duration_seconds: float = 0.0
    error_message: Optional[str] = None
    error_traceback: Optional[str] = None
    details: Dict[str, Any] = field(default_factory=dict)
    
    def to_dict(self) -> Dict[str, Any]:
        """转换为字典"""
        return {
            'test_name': self.test_name,
            'test_description': self.test_description,
            'status': self.status.value,
            'start_time': self.start_time.isoformat() if self.start_time else None,
            'end_time': self.end_time.isoformat() if self.end_time else None,
            'duration_seconds': self.duration_seconds,
            'error_message': self.error_message,
            'error_traceback': self.error_traceback,
            'details': self.details
        }


@dataclass
class TestSuiteResult:
    """测试套件结果"""
    suite_name: str
    start_time: datetime
    end_time: Optional[datetime] = None
    results: List[TestResult] = field(default_factory=list)
    
    @property
    def total_tests(self) -> int:
        return len(self.results)
    
    @property
    def passed_count(self) -> int:
        return sum(1 for r in self.results if r.status == TestStatus.PASSED)
    
    @property
    def failed_count(self) -> int:
        return sum(1 for r in self.results if r.status == TestStatus.FAILED)
    
    @property
    def error_count(self) -> int:
        return sum(1 for r in self.results if r.status == TestStatus.ERROR)
    
    @property
    def skipped_count(self) -> int:
        return sum(1 for r in self.results if r.status == TestStatus.SKIPPED)
    
    @property
    def pass_rate(self) -> float:
        if self.total_tests == 0:
            return 0.0
        return (self.passed_count / self.total_tests) * 100
    
    @property
    def duration_seconds(self) -> float:
        if self.end_time and self.start_time:
            return (self.end_time - self.start_time).total_seconds()
        return 0.0
    
    def to_dict(self) -> Dict[str, Any]:
        """转换为字典"""
        return {
            'suite_name': self.suite_name,
            'start_time': self.start_time.isoformat(),
            'end_time': self.end_time.isoformat() if self.end_time else None,
            'duration_seconds': self.duration_seconds,
            'summary': {
                'total_tests': self.total_tests,
                'passed': self.passed_count,
                'failed': self.failed_count,
                'errors': self.error_count,
                'skipped': self.skipped_count,
                'pass_rate': f"{self.pass_rate:.2f}%"
            },
            'results': [r.to_dict() for r in self.results]
        }


class MockDataGenerator:
    """
    模拟训练数据生成器
    支持生成多种日期异常场景的模拟数据
    """
    
    def __init__(self, seed: int = 42):
        """
        初始化数据生成器
        
        Args:
            seed: 随机种子，确保可复现
        """
        self.seed = seed
        random.seed(seed)
        np.random.seed(seed)
    
    def generate_continuous_dates(
        self,
        start_date: str,
        end_date: str,
        freq: str = 'D'
    ) -> pd.DatetimeIndex:
        """
        生成连续日期序列
        
        Args:
            start_date: 开始日期 (YYYY-MM-DD)
            end_date: 结束日期 (YYYY-MM-DD)
            freq: 频率 ('D'=日, 'M'=月)
            
        Returns:
            日期索引
        """
        return pd.date_range(start=start_date, end=end_date, freq=freq)
    
    def generate_passenger_data(
        self,
        dates: pd.DatetimeIndex,
        base_value: int = 300,
        trend_factor: float = 0.02,
        seasonal_amplitude: float = 50,
        noise_level: float = 20
    ) -> np.ndarray:
        """
        生成客流量数据
        
        Args:
            dates: 日期索引
            base_value: 基础客流量
            trend_factor: 趋势因子
            seasonal_amplitude: 季节性振幅
            noise_level: 噪声水平
            
        Returns:
            客流量数组
        """
        n = len(dates)
        
        # 趋势成分
        trend = np.arange(n) * trend_factor
        
        # 季节性成分（月度周期）
        if hasattr(dates, 'month'):
            month = dates.month
        else:
            month = pd.DatetimeIndex(dates).month
        seasonal = seasonal_amplitude * np.sin(2 * np.pi * month / 12)
        
        # 噪声
        noise = np.random.normal(0, noise_level, n)
        
        # 组合
        passengers = base_value + trend + seasonal + noise
        passengers = np.maximum(passengers, 0).astype(int)
        
        return passengers
    
    def generate_continuous_data(
        self,
        start_date: str = "2023-01-01",
        days: int = 365,
        **kwargs
    ) -> pd.DataFrame:
        """
        生成完全连续的日期数据（基准测试）
        
        Args:
            start_date: 开始日期
            days: 天数
            **kwargs: 传递给generate_passenger_data的参数
            
        Returns:
            DataFrame包含Date和Passengers列
        """
        start = pd.to_datetime(start_date)
        end = start + timedelta(days=days-1)
        dates = self.generate_continuous_dates(start_date, end.strftime('%Y-%m-%d'))
        passengers = self.generate_passenger_data(dates, **kwargs)
        
        return pd.DataFrame({
            'Date': dates.strftime('%Y-%m-%d'),
            'Passengers': passengers
        })
    
    def generate_single_missing_data(
        self,
        start_date: str = "2023-01-01",
        days: int = 365,
        missing_date: Optional[str] = None,
        **kwargs
    ) -> pd.DataFrame:
        """
        生成单日期缺失数据
        
        Args:
            start_date: 开始日期
            days: 天数
            missing_date: 缺失日期，默认中间日期
            **kwargs: 传递给generate_passenger_data的参数
            
        Returns:
            DataFrame包含Date和Passengers列（缺少指定日期）
        """
        df = self.generate_continuous_data(start_date, days, **kwargs)
        
        if missing_date is None:
            # 默认删除中间日期
            mid_idx = len(df) // 2
            missing_date = df.iloc[mid_idx]['Date']
        
        # 删除指定日期
        df = df[df['Date'] != missing_date].reset_index(drop=True)
        
        return df
    
    def generate_multiple_missing_data(
        self,
        start_date: str = "2023-01-01",
        days: int = 365,
        missing_count: int = 5,
        missing_consecutive: bool = False,
        **kwargs
    ) -> pd.DataFrame:
        """
        生成多日期缺失数据
        
        Args:
            start_date: 开始日期
            days: 天数
            missing_count: 缺失日期数量
            missing_consecutive: 是否连续缺失
            **kwargs: 传递给generate_passenger_data的参数
            
        Returns:
            DataFrame包含Date和Passengers列
        """
        df = self.generate_continuous_data(start_date, days, **kwargs)
        
        if missing_consecutive:
            # 连续缺失
            start_idx = random.randint(10, len(df) - missing_count - 10)
            missing_indices = range(start_idx, start_idx + missing_count)
        else:
            # 随机缺失
            missing_indices = random.sample(range(5, len(df)-5), missing_count)
        
        df = df.drop(df.index[list(missing_indices)]).reset_index(drop=True)
        
        return df
    
    def generate_shuffled_data(
        self,
        start_date: str = "2023-01-01",
        days: int = 365,
        shuffle_ratio: float = 0.3,
        **kwargs
    ) -> pd.DataFrame:
        """
        生成日期乱序数据
        
        Args:
            start_date: 开始日期
            days: 天数
            shuffle_ratio: 乱序比例
            **kwargs: 传递给generate_passenger_data的参数
            
        Returns:
            DataFrame包含Date和Passengers列（日期乱序）
        """
        df = self.generate_continuous_data(start_date, days, **kwargs)
        
        # 随机打乱部分数据
        n_shuffle = int(len(df) * shuffle_ratio)
        shuffle_indices = random.sample(range(len(df)), n_shuffle)
        
        # 提取并打乱
        subset = df.iloc[shuffle_indices].copy()
        subset = subset.sample(frac=1).reset_index(drop=True)
        
        # 放回原位置
        for i, idx in enumerate(shuffle_indices):
            df.iloc[idx] = subset.iloc[i]
        
        return df.reset_index(drop=True)
    
    def generate_extreme_date_range(
        self,
        scenario: str = "leap_year",
        **kwargs
    ) -> pd.DataFrame:
        """
        生成极端日期范围数据
        
        Args:
            scenario: 场景类型
                - "leap_year": 闰年2月29日
                - "century_start": 世纪开始
                - "very_long": 超长日期范围
                - "very_short": 超短日期范围
            **kwargs: 传递给generate_passenger_data的参数
            
        Returns:
            DataFrame包含Date和Passengers列
        """
        if scenario == "leap_year":
            # 闰年2月29日场景
            dates = pd.date_range("2020-02-28", "2020-03-02", freq='D')
            passengers = self.generate_passenger_data(dates, **kwargs)
            
        elif scenario == "century_start":
            # 世纪开始
            dates = pd.date_range("2000-01-01", "2000-01-10", freq='D')
            passengers = self.generate_passenger_data(dates, **kwargs)
            
        elif scenario == "very_long":
            # 超长日期范围（10年）
            dates = pd.date_range("2010-01-01", "2019-12-31", freq='D')
            passengers = self.generate_passenger_data(dates, **kwargs)
            
        elif scenario == "very_short":
            # 超短日期范围（3天）
            dates = pd.date_range("2023-01-01", "2023-01-03", freq='D')
            passengers = self.generate_passenger_data(dates, **kwargs)
            
        else:
            raise ValueError(f"未知场景: {scenario}")
        
        return pd.DataFrame({
            'Date': dates.strftime('%Y-%m-%d'),
            'Passengers': passengers
        })
    
    def generate_monthly_format_data(
        self,
        start_date: str = "2023-01",
        months: int = 12,
        **kwargs
    ) -> pd.DataFrame:
        """
        生成月度格式数据（与原始数据格式一致）
        
        Args:
            start_date: 开始年月 (YYYY-MM)
            months: 月数
            **kwargs: 数据生成参数
            
        Returns:
            DataFrame包含Month和Passengers列
        """
        start = pd.to_datetime(start_date + "-01")
        dates = pd.date_range(start=start, periods=months, freq='MS')
        
        # 月度数据值更大
        passengers = self.generate_passenger_data(
            dates,
            base_value=kwargs.get('base_value', 300) * 30,  # 月度累计
            trend_factor=kwargs.get('trend_factor', 0.02) * 30,
            seasonal_amplitude=kwargs.get('seasonal_amplitude', 50) * 30,
            noise_level=kwargs.get('noise_level', 20) * 30
        )
        
        return pd.DataFrame({
            'Month': dates.strftime('%Y-%m'),
            'Passengers': passengers
        })


class AutoRetrainTestBase:
    """
    自动重训测试基类
    提供测试前置准备和后置清理机制
    """
    
    def __init__(self):
        self.test_data_dir: Optional[Path] = None
        self.original_data_file: Optional[Path] = None
        self.mock_data_generator = MockDataGenerator()
        self.test_results: List[TestResult] = []
        
    def setup(self):
        """
        测试前置准备
        创建临时测试目录和数据文件
        """
        # 创建临时目录
        self.test_data_dir = Path(tempfile.mkdtemp(prefix="auto_retrain_test_"))
        
        # 创建原始数据文件备份
        self.original_data_file = settings.original_data_file
        
        logger.info(f"测试环境准备完成: {self.test_data_dir}")
        
    def teardown(self):
        """
        测试后置清理
        删除临时文件和目录
        """
        if self.test_data_dir and self.test_data_dir.exists():
            shutil.rmtree(self.test_data_dir)
            logger.info(f"测试环境清理完成: {self.test_data_dir}")
        
    def create_test_data_file(self, df: pd.DataFrame, filename: str = "test_data.csv") -> Path:
        """
        创建测试数据文件
        
        Args:
            df: 数据DataFrame
            filename: 文件名
            
        Returns:
            文件路径
        """
        filepath = self.test_data_dir / filename
        df.to_csv(filepath, index=False)
        return filepath
    
    def run_test_case(
        self,
        test_name: str,
        test_description: str,
        test_func: Callable[[], Tuple[bool, Dict[str, Any], Optional[str]]]
    ) -> TestResult:
        """
        运行单个测试用例
        
        Args:
            test_name: 测试名称
            test_description: 测试描述
            test_func: 测试函数，返回(是否通过, 详细信息, 错误信息)
            
        Returns:
            测试结果
        """
        result = TestResult(
            test_name=test_name,
            test_description=test_description,
            status=TestStatus.RUNNING
        )
        result.start_time = datetime.now()
        
        try:
            passed, details, error = test_func()
            result.end_time = datetime.now()
            result.duration_seconds = (result.end_time - result.start_time).total_seconds()
            result.details = details
            
            if passed:
                result.status = TestStatus.PASSED
            else:
                result.status = TestStatus.FAILED
                result.error_message = error
                
        except Exception as e:
            result.end_time = datetime.now()
            result.duration_seconds = (result.end_time - result.start_time).total_seconds()
            result.status = TestStatus.ERROR
            result.error_message = str(e)
            result.error_traceback = traceback.format_exc()
            
        self.test_results.append(result)
        return result
    
    def validate_date_continuity(self, df: pd.DataFrame, date_col: str = 'Date') -> Dict[str, Any]:
        """
        验证日期连续性
        
        Args:
            df: 数据DataFrame
            date_col: 日期列名
            
        Returns:
            验证结果
        """
        dates = pd.to_datetime(df[date_col])
        dates = dates.sort_values().reset_index(drop=True)
        
        # 计算日期间隔
        gaps = []
        for i in range(1, len(dates)):
            diff = (dates[i] - dates[i-1]).days
            if diff > 1:
                gaps.append({
                    'from': dates[i-1].strftime('%Y-%m-%d'),
                    'to': dates[i].strftime('%Y-%m-%d'),
                    'gap_days': diff - 1
                })
        
        return {
            'is_continuous': len(gaps) == 0,
            'total_dates': len(dates),
            'date_range': {
                'start': dates.min().strftime('%Y-%m-%d'),
                'end': dates.max().strftime('%Y-%m-%d')
            },
            'gaps': gaps,
            'gap_count': len(gaps)
        }


class TestReportGenerator:
    """
    测试报告生成器
    生成HTML和JSON格式的测试报告
    """
    
    def __init__(self, output_dir: str = "test_reports"):
        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(exist_ok=True)
        
    def generate_json_report(self, suite_result: TestSuiteResult) -> Path:
        """
        生成JSON格式报告
        
        Args:
            suite_result: 测试套件结果
            
        Returns:
            报告文件路径
        """
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        report_file = self.output_dir / f"test_report_{timestamp}.json"
        
        with open(report_file, 'w', encoding='utf-8') as f:
            json.dump(suite_result.to_dict(), f, indent=2, ensure_ascii=False)
        
        logger.info(f"JSON报告已生成: {report_file}")
        return report_file
    
    def generate_html_report(self, suite_result: TestSuiteResult) -> Path:
        """
        生成HTML格式报告
        
        Args:
            suite_result: 测试套件结果
            
        Returns:
            报告文件路径
        """
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        report_file = self.output_dir / f"test_report_{timestamp}.html"
        
        html_content = self._build_html_content(suite_result)
        
        with open(report_file, 'w', encoding='utf-8') as f:
            f.write(html_content)
        
        logger.info(f"HTML报告已生成: {report_file}")
        return report_file
    
    def _build_html_content(self, suite_result: TestSuiteResult) -> str:
        """构建HTML报告内容"""
        
        # 状态颜色映射
        status_colors = {
            TestStatus.PASSED: "#28a745",
            TestStatus.FAILED: "#dc3545",
            TestStatus.ERROR: "#fd7e14",
            TestStatus.SKIPPED: "#6c757d",
            TestStatus.RUNNING: "#007bff",
            TestStatus.PENDING: "#6c757d"
        }
        
        # 构建测试详情表格
        test_rows = ""
        for result in suite_result.results:
            status_color = status_colors.get(result.status, "#6c757d")
            error_info = f"<pre>{result.error_message}</pre>" if result.error_message else "-"
            details = json.dumps(result.details, indent=2, ensure_ascii=False) if result.details else "{}"
            
            test_rows += f"""
            <tr>
                <td>{result.test_name}</td>
                <td>{result.test_description}</td>
                <td style="color: {status_color}; font-weight: bold;">{result.status.value.upper()}</td>
                <td>{result.duration_seconds:.2f}s</td>
                <td>{error_info}</td>
                <td><pre>{details}</pre></td>
            </tr>
            """
        
        html = f"""
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>自动重训功能测试报告</title>
    <style>
        body {{
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            margin: 0;
            padding: 20px;
            background-color: #f5f5f5;
        }}
        .container {{
            max-width: 1400px;
            margin: 0 auto;
            background: white;
            padding: 30px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }}
        h1 {{
            color: #333;
            border-bottom: 3px solid #007bff;
            padding-bottom: 10px;
        }}
        .summary {{
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 20px;
            margin: 30px 0;
        }}
        .summary-card {{
            background: #f8f9fa;
            padding: 20px;
            border-radius: 8px;
            text-align: center;
        }}
        .summary-card h3 {{
            margin: 0 0 10px 0;
            color: #666;
            font-size: 14px;
        }}
        .summary-card .value {{
            font-size: 32px;
            font-weight: bold;
            color: #333;
        }}
        .summary-card.passed .value {{ color: #28a745; }}
        .summary-card.failed .value {{ color: #dc3545; }}
        .summary-card.errors .value {{ color: #fd7e14; }}
        .summary-card.pass-rate .value {{ color: #007bff; }}
        
        table {{
            width: 100%;
            border-collapse: collapse;
            margin-top: 30px;
        }}
        th, td {{
            padding: 12px;
            text-align: left;
            border-bottom: 1px solid #ddd;
        }}
        th {{
            background-color: #f8f9fa;
            font-weight: 600;
            color: #333;
        }}
        tr:hover {{
            background-color: #f8f9fa;
        }}
        pre {{
            background: #f4f4f4;
            padding: 10px;
            border-radius: 4px;
            overflow-x: auto;
            max-height: 200px;
            font-size: 12px;
        }}
        .timestamp {{
            color: #666;
            font-size: 14px;
            margin-bottom: 20px;
        }}
    </style>
</head>
<body>
    <div class="container">
        <h1>自动重训功能测试报告</h1>
        <div class="timestamp">
            测试时间: {suite_result.start_time.strftime('%Y-%m-%d %H:%M:%S')} | 
            持续时间: {suite_result.duration_seconds:.2f}秒
        </div>
        
        <div class="summary">
            <div class="summary-card">
                <h3>总测试数</h3>
                <div class="value">{suite_result.total_tests}</div>
            </div>
            <div class="summary-card passed">
                <h3>通过</h3>
                <div class="value">{suite_result.passed_count}</div>
            </div>
            <div class="summary-card failed">
                <h3>失败</h3>
                <div class="value">{suite_result.failed_count}</div>
            </div>
            <div class="summary-card errors">
                <h3>错误</h3>
                <div class="value">{suite_result.error_count}</div>
            </div>
            <div class="summary-card pass-rate">
                <h3>通过率</h3>
                <div class="value">{suite_result.pass_rate:.1f}%</div>
            </div>
        </div>
        
        <h2>测试详情</h2>
        <table>
            <thead>
                <tr>
                    <th>测试名称</th>
                    <th>测试描述</th>
                    <th>状态</th>
                    <th>耗时</th>
                    <th>错误信息</th>
                    <th>详细信息</th>
                </tr>
            </thead>
            <tbody>
                {test_rows}
            </tbody>
        </table>
    </div>
</body>
</html>
"""
        return html


# 导出模块
__all__ = [
    'TestStatus',
    'TestResult',
    'TestSuiteResult',
    'MockDataGenerator',
    'AutoRetrainTestBase',
    'TestReportGenerator'
]


if __name__ == '__main__':
    # 测试数据生成器
    generator = MockDataGenerator()
    
    # 生成各种测试数据
    print("生成测试数据...")
    
    # 1. 连续数据
    df_continuous = generator.generate_continuous_data(days=30)
    print(f"连续数据: {len(df_continuous)} 条")
    
    # 2. 单日期缺失
    df_single_missing = generator.generate_single_missing_data(days=30)
    print(f"单日期缺失: {len(df_single_missing)} 条")
    
    # 3. 多日期连续缺失
    df_multi_missing = generator.generate_multiple_missing_data(days=30, missing_count=3, missing_consecutive=True)
    print(f"多日期连续缺失: {len(df_multi_missing)} 条")
    
    # 4. 日期乱序
    df_shuffled = generator.generate_shuffled_data(days=30)
    print(f"日期乱序: {len(df_shuffled)} 条")
    
    # 5. 极端日期
    df_leap = generator.generate_extreme_date_range("leap_year")
    print(f"闰年数据: {len(df_leap)} 条")
    
    print("测试数据生成完成！")
