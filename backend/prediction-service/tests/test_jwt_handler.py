"""
JWT 处理器测试
"""
import pytest
import base64
import json
import hmac
import hashlib
from datetime import datetime, timedelta, timezone
from fastapi import HTTPException
from app.core.jwt_handler import JWTHandler, JWTContext


class TestJWTHandler:
    """JWT 处理器测试类"""

    @pytest.fixture
    def handler(self):
        return JWTHandler()

    @pytest.fixture
    def valid_token(self):
        """创建一个有效的测试 Token"""
        # 创建简单的测试 payload
        payload = {
            "operatorId": 123,
            "operatorName": "test_user",
            "clientType": "web",
            "roles": ["admin"],
            "exp": int((datetime.now(timezone.utc) + timedelta(hours=1)).timestamp()),
            "iat": int(datetime.now(timezone.utc).timestamp())
        }

        # 简单的 JWT 格式（不含有效签名，用于测试解析）
        header = base64.urlsafe_b64encode(json.dumps({"alg": "none"}).encode()).decode().rstrip("=")
        payload_b64 = base64.urlsafe_b64encode(json.dumps(payload).encode()).decode().rstrip("=")
        signature = "dummy_signature"

        return f"{header}.{payload_b64}.{signature}"

    def test_extract_token_from_header(self, handler):
        """测试从 Authorization 头提取 Token"""
        from fastapi import Request
        from starlette.datastructures import Headers

        # 模拟请求
        scope = {
            "type": "http",
            "method": "GET",
            "path": "/",
            "headers": [(b"authorization", b"Bearer test_token_123")],
        }
        request = Request(scope)

        token = handler.extract_token(request)
        assert token == "test_token_123"

    def test_extract_token_without_bearer(self, handler):
        """测试不带 Bearer 前缀的 Token"""
        from fastapi import Request

        scope = {
            "type": "http",
            "method": "GET",
            "path": "/",
            "headers": [(b"authorization", b"raw_token_123")],
        }
        request = Request(scope)

        token = handler.extract_token(request)
        assert token == "raw_token_123"

    def test_extract_token_no_header(self, handler):
        """测试没有 Authorization 头时返回 None"""
        from fastapi import Request

        scope = {
            "type": "http",
            "method": "GET",
            "path": "/",
            "headers": [],
        }
        request = Request(scope)

        token = handler.extract_token(request)
        assert token is None

    def test_decode_base64(self, handler):
        """测试 Base64 解码"""
        original = "test_data"
        encoded = base64.urlsafe_b64encode(original.encode()).decode().rstrip("=")
        decoded = handler.decode_base64(encoded)
        assert decoded == original

    def test_parse_token_invalid_format(self, handler):
        """测试无效格式的 Token"""
        with pytest.raises(HTTPException) as exc_info:
            handler.parse_token("invalid.token", verify_signature=False)
        assert exc_info.value.status_code == 401

    def test_jwt_context_has_role(self):
        """测试 JWTContext 角色检查"""
        ctx = JWTContext()
        ctx.roles = ["admin", "user"]

        assert ctx.has_role("admin") is True
        assert ctx.has_role("superadmin") is False

    def test_jwt_context_has_any_role(self):
        """测试 JWTContext 任意角色检查"""
        ctx = JWTContext()
        ctx.roles = ["admin", "user"]

        assert ctx.has_any_role(["admin", "superadmin"]) is True
        assert ctx.has_any_role(["superadmin", "operator"]) is False

    def test_jwt_context_is_authenticated(self):
        """测试 JWTContext 认证状态"""
        ctx = JWTContext()
        assert ctx.is_authenticated() is False

        ctx.operator_id = 123
        assert ctx.is_authenticated() is True

    def test_is_token_expired(self, handler):
        """测试 Token 过期检查"""
        # 已过期
        expired_claims = {
            "exp": int((datetime.now(timezone.utc) - timedelta(hours=1)).timestamp())
        }
        assert handler.is_token_expired(expired_claims) is True

        # 未过期
        valid_claims = {
            "exp": int((datetime.now(timezone.utc) + timedelta(hours=1)).timestamp())
        }
        assert handler.is_token_expired(valid_claims) is False

        # 无过期时间
        no_exp_claims = {}
        assert handler.is_token_expired(no_exp_claims) is False
