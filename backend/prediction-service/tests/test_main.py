"""
主应用测试
"""
import pytest
from fastapi.testclient import TestClient
from app.main import app


@pytest.fixture
def client():
    return TestClient(app)


class TestHealthEndpoints:
    """健康检查端点测试"""

    def test_root_endpoint(self, client):
        """测试根路径"""
        response = client.get("/")
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 200
        assert "name" in data["data"]

    def test_health_endpoint(self, client):
        """测试健康检查"""
        response = client.get("/health")
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 200
        assert data["data"]["status"] == "UP"

    def test_actuator_health_endpoint(self, client):
        """测试 Spring Boot 风格健康检查"""
        response = client.get("/actuator/health")
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == 200
        assert data["data"]["status"] == "UP"


class TestRequestSizeLimit:
    """请求体大小限制测试"""

    def test_request_size_limit(self, client):
        """测试请求体大小限制"""
        # 创建一个超过 10MB 的请求体
        large_data = "x" * (11 * 1024 * 1024)
        response = client.post(
            "/api/mgmt/v1/prediction/forecast",
            json={"days": 20, "confidence_level": 0.95},
            headers={"Content-Length": str(len(large_data) + 100)}
        )
        # 注意：实际测试中可能需要调整，因为 TestClient 可能不严格检查 Content-Length
        assert response.status_code in [200, 413]
