"""
JWT 处理模块
与 Java JWT 解析机制保持一致
"""
import base64
import json
import hmac
import hashlib
from typing import Optional, Dict, List, Any
from datetime import datetime
from fastapi import Request, HTTPException
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials

from app.core.config import settings

# JWT Claims 常量 (与 JwtClaimsConstant 保持一致)
JWT_CLAIMS_OPERATOR_ID = "operatorId"
JWT_CLAIMS_OPERATOR_NAME = "operatorName"
JWT_CLAIMS_CLIENT_TYPE = "clientType"
JWT_CLAIMS_ROLES = "roles"
JWT_CLAIMS_SUB = "sub"
JWT_CLAIMS_CLIENT_ID = "client_id"
JWT_CLAIMS_SCOPE = "scope"
JWT_CLAIMS_EXP = "exp"
JWT_CLAIMS_IAT = "iat"

# HTTP Header 常量
HEADER_AUTHORIZATION = "Authorization"
HEADER_OPERATOR_ID = "operatorId"
HEADER_OPERATOR_NAME = "operatorName"
HEADER_CLIENT_TYPE = "clientType"


class JWTContext:
    """
    JWT 上下文信息
    存储从 JWT 中解析的用户信息
    """

    def __init__(self):
        self.operator_id: Optional[int] = None
        self.operator_name: Optional[str] = None
        self.client_type: Optional[str] = None
        self.roles: List[str] = []
        self.token: Optional[str] = None
        self.claims: Dict[str, Any] = {}

    def has_role(self, role: str) -> bool:
        """检查是否有指定角色"""
        return role in self.roles

    def has_any_role(self, roles: List[str]) -> bool:
        """检查是否有任意一个角色"""
        return any(r in self.roles for r in roles)

    def is_authenticated(self) -> bool:
        """是否已认证"""
        return self.operator_id is not None


class JWTHandler:
    """JWT 处理器"""

    def __init__(self):
        self.security = HTTPBearer(auto_error=False)
        self.token_prefix = "Bearer "

    def extract_token(self, request: Request) -> Optional[str]:
        """
        从请求中提取 JWT Token
        优先从 Header 中提取，支持多种格式
        """
        # 1. 从 Authorization Header 提取
        auth_header = request.headers.get(HEADER_AUTHORIZATION)
        if auth_header and auth_header.startswith(self.token_prefix):
            return auth_header[len(self.token_prefix):]
        if auth_header:
            return auth_header

        return None

    def decode_base64(self, data: str) -> str:
        """Base64 解码（兼容 JWT 的 Base64Url）"""
        # 补齐 Base64Url 的填充
        padding = 4 - len(data) % 4
        if padding != 4:
            data += '=' * padding
        return base64.urlsafe_b64decode(data).decode('utf-8')

    def parse_token(self, token: str) -> Dict[str, Any]:
        """
        解析 JWT Token（不验证签名，仅解析内容）
        生产环境建议验证签名
        """
        try:
            parts = token.split('.')
            if len(parts) != 3:
                raise ValueError("Invalid JWT format")

            # 解码 payload
            payload_json = self.decode_base64(parts[1])
            payload = json.loads(payload_json)

            return payload

        except Exception as e:
            raise HTTPException(status_code=401, detail=f"无效的Token: {str(e)}")

    def get_context(self, request: Request) -> JWTContext:
        """
        获取 JWT 上下文
        从请求中解析 JWT 并提取用户信息
        """
        ctx = JWTContext()

        # 提取 Token
        token = self.extract_token(request)
        if not token:
            return ctx

        ctx.token = token

        try:
            # 解析 claims
            claims = self.parse_token(token)
            ctx.claims = claims

            # 提取用户信息
            ctx.operator_id = claims.get(JWT_CLAIMS_OPERATOR_ID)
            if ctx.operator_id:
                ctx.operator_id = int(ctx.operator_id)

            ctx.operator_name = claims.get(JWT_CLAIMS_OPERATOR_NAME)
            ctx.client_type = claims.get(JWT_CLAIMS_CLIENT_TYPE)

            # 提取角色
            roles = claims.get(JWT_CLAIMS_ROLES)
            if roles and isinstance(roles, list):
                ctx.roles = roles

        except Exception:
            # 解析失败，返回空的上下文
            pass

        return ctx

    def verify_token(self, token: str, secret_key: str) -> bool:
        """
        验证 JWT 签名
        简单的 HMAC SHA256 验证
        """
        try:
            parts = token.split('.')
            if len(parts) != 3:
                return False

            message = f"{parts[0]}.{parts[1]}"
            signature = base64.urlsafe_b64decode(
                parts[2] + '=' * (4 - len(parts[2]) % 4)
            )

            expected_signature = hmac.new(
                secret_key.encode('utf-8'),
                message.encode('utf-8'),
                hashlib.sha256
            ).digest()

            return hmac.compare_digest(signature, expected_signature)

        except Exception:
            return False

    def is_token_expired(self, claims: Dict[str, Any]) -> bool:
        """检查 Token 是否过期"""
        exp = claims.get(JWT_CLAIMS_EXP)
        if not exp:
            return False

        try:
            exp_time = datetime.fromtimestamp(exp)
            return datetime.now() > exp_time
        except (TypeError, ValueError):
            return False


# 全局 JWT 处理器实例
jwt_handler = JWTHandler()


# 依赖注入函数
async def get_jwt_context(request: Request) -> JWTContext:
    """FastAPI 依赖：获取 JWT 上下文"""
    return jwt_handler.get_context(request)


async def require_auth(request: Request) -> JWTContext:
    """FastAPI 依赖：要求必须登录"""
    ctx = jwt_handler.get_context(request)
    if not ctx.is_authenticated():
        raise HTTPException(status_code=401, detail="登录状态已失效，请重新登录")
    return ctx
