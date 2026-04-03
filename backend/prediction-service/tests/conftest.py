"""
测试配置和夹具
"""
import pytest
from fastapi.testclient import TestClient
from app.main import app


@pytest.fixture
def client():
    """提供测试客户端"""
    return TestClient(app)


@pytest.fixture
def base_url():
    """基础 URL"""
    return "/api/mgmt/v1/prediction"
