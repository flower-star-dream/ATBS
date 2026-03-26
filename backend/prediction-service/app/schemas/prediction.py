"""
预测服务的数据模型 (Pydantic)
对应原 Java 的 DTO 类
"""
from datetime import date, datetime
from typing import List, Optional

from pydantic import BaseModel, Field, field_validator


class PredictionRequest(BaseModel):
    """预测请求模型"""
    days: int = Field(default=20, ge=1, le=365, description="预测天数")
    confidence_level: float = Field(default=0.95, ge=0.0, le=1.0, description="置信水平")
    auto_train: bool = Field(default=False, description="是否自动训练")


class PredictionItem(BaseModel):
    """预测结果项"""
    model_config = {"populate_by_name": True}

    prediction_date: date = Field(description="日期", alias="date")
    predicted_passengers: int = Field(description="预测客流量")
    lower_bound: int = Field(description="置信区间下限")
    upper_bound: int = Field(description="置信区间上限")
    day_of_week: str = Field(description="星期")
    is_weekday: bool = Field(description="是否是工作日")


class ModelInfo(BaseModel):
    """模型信息"""
    order: List[int] = Field(description="ARIMA 参数 (p, d, q)")
    aic: Optional[float] = Field(None, description="AIC 值")
    bic: Optional[float] = Field(None, description="BIC 值")
    training_time: Optional[datetime] = Field(None, description="模型训练时间")


class PredictionResponse(BaseModel):
    """预测响应模型"""
    status: str = Field(description="响应状态")
    message: str = Field(description="响应消息")
    prediction_time: datetime = Field(description="预测时间（北京时间）")
    days: int = Field(description="预测天数")
    predictions: List[PredictionItem] = Field(description="预测结果列表")
    model_info: Optional[ModelInfo] = Field(None, description="模型信息")


class TrainingRequest(BaseModel):
    """训练请求模型 - 默认自动寻找最优参数，调用方无需关心具体实现"""
    p: int = Field(default=5, ge=0, le=10, description="自回归阶数 p（仅在 auto_optimize=false 时使用）")
    d: int = Field(default=1, ge=0, le=3, description="差分阶数 d（仅在 auto_optimize=false 时使用）")
    q: int = Field(default=0, ge=0, le=10, description="移动平均阶数 q（仅在 auto_optimize=false 时使用）")
    auto_optimize: bool = Field(default=True, description="是否自动寻找最优参数（默认启用）")
    test_size: int = Field(default=30, ge=7, le=90, description="验证集大小（默认30天，符合一般标准）")


class ValidationMetrics(BaseModel):
    """验证指标"""
    mae: float = Field(description="平均绝对误差")
    rmse: float = Field(description="均方根误差")
    mape: float = Field(description="平均绝对百分比误差")
    test_size: int = Field(description="测试集大小")


class TrainingResponse(BaseModel):
    """训练响应模型"""
    status: str = Field(description="响应状态")
    message: str = Field(description="响应消息")
    order: List[int] = Field(description="使用的 ARIMA 参数")
    aic: Optional[float] = Field(None, description="AIC 值")
    bic: Optional[float] = Field(None, description="BIC 值")
    training_time: datetime = Field(description="训练时间（北京时间）")
    validation: Optional[ValidationMetrics] = Field(None, description="验证指标")
    data_length: int = Field(description="训练数据长度")


class ModelInfoResponse(BaseModel):
    """模型信息响应"""
    model_loaded: bool = Field(description="模型是否已加载")
    prediction_time: datetime = Field(description="查询时间（北京时间）")
    order: Optional[List[int]] = Field(None, description="ARIMA 参数")
    aic: Optional[float] = Field(None, description="AIC 值")
    bic: Optional[float] = Field(None, description="BIC 值")
    sigma2: Optional[float] = Field(None, description="方差")
    training_history: Optional[dict] = Field(None, description="训练历史")
