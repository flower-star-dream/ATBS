"""
数据处理器测试
"""
import pytest
import pandas as pd
import numpy as np
from datetime import datetime
from utils.data_processor import DataProcessor


class TestDataProcessor:
    """数据处理器测试类"""

    @pytest.fixture
    def processor(self):
        return DataProcessor()

    @pytest.fixture
    def sample_data(self, tmp_path):
        """创建示例 CSV 数据文件"""
        csv_content = """Month,Passengers
1949-01,112
1949-02,118
1949-03,132
1949-04,129
1949-05,121
"""
        file_path = tmp_path / "test_data.csv"
        file_path.write_text(csv_content)
        return str(file_path)

    def test_load_monthly_data_success(self, processor, sample_data):
        """测试成功加载月度数据"""
        df = processor.load_monthly_data(sample_data)
        assert len(df) == 5
        assert "Month" in df.columns
        assert "Passengers" in df.columns
        assert df["Passengers"].dtype in [np.int64, np.float64]

    def test_load_monthly_data_file_not_found(self, processor):
        """测试文件不存在时抛出异常"""
        with pytest.raises(FileNotFoundError):
            processor.load_monthly_data("/nonexistent/file.csv")

    def test_load_monthly_data_missing_columns(self, processor, tmp_path):
        """测试缺少必需列时抛出异常"""
        csv_content = """Date,Count
1949-01,112
"""
        file_path = tmp_path / "bad_data.csv"
        file_path.write_text(csv_content)

        with pytest.raises(ValueError, match="缺少必需列"):
            processor.load_monthly_data(str(file_path))

    def test_load_monthly_data_empty_file(self, processor, tmp_path):
        """测试空文件时抛出异常"""
        file_path = tmp_path / "empty.csv"
        file_path.write_text("Month,Passengers\n")

        with pytest.raises(ValueError):
            processor.load_monthly_data(str(file_path))

    def test_load_monthly_data_negative_values(self, processor, tmp_path):
        """测试负数数据被过滤"""
        csv_content = """Month,Passengers
1949-01,112
1949-02,-10
1949-03,132
"""
        file_path = tmp_path / "negative_data.csv"
        file_path.write_text(csv_content)

        df = processor.load_monthly_data(str(file_path))
        assert len(df) == 2  # 负数行被过滤

    def test_monthly_to_daily(self, processor, sample_data):
        """测试月度转日度数据"""
        processor.load_monthly_data(sample_data)
        daily_df = processor.monthly_to_daily()

        assert len(daily_df) > 0
        assert "Date" in daily_df.columns
        assert "Passengers" in daily_df.columns
        assert daily_df["Passengers"].sum() > 0

    def test_monthly_to_daily_without_load(self, processor):
        """测试未加载数据时抛出异常"""
        with pytest.raises(ValueError, match="请先调用load_monthly_data"):
            processor.monthly_to_daily()

    def test_get_prediction_dates(self):
        """测试获取预测日期"""
        dates = DataProcessor.get_prediction_dates(10)
        assert len(dates) == 10
        assert isinstance(dates[0], pd.Timestamp)
