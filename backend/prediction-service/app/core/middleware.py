"""
FastAPI 中间件
包含 JWT 验证、跨域、请求追踪等
"""
import time
import uuid
import logging
from typing import Optional

from fastapi import Request, Response
from fastapi.responses import JSONResponse
from starlette.middleware.base import BaseHTTPMiddleware

from app.core.result import Result
from app.core.jwt_handler import jwt_handler
from app.core.auth import is_white_list_path
from app.core.exceptions import BusinessException, ErrorCode

logger = logging.getLogger(__name__)


class TraceIdMiddleware(BaseHTTPMiddleware):
    """TraceId 中间件 - 为每个请求添加追踪ID"""

    async def dispatch(self, request: Request, call_next):
        # 从请求头获取 traceId，如果没有则生成
        trace_id = request.headers.get("operatorId") or str(uuid.uuid4())
        request.state.trace_id = trace_id

        # 记录请求开始时间
        start_time = time.time()

        # 继续处理请求
        response = await call_next(request)

        # 添加 traceId 到响应头
        response.headers["X-Trace-Id"] = trace_id

        # 记录请求耗时
        process_time = time.time() - start_time
        logger.info(f"[{trace_id}] {request.method} {request.url.path} - {process_time:.3f}s")

        return response


class JWTAuthMiddleware(BaseHTTPMiddleware):
    """
    JWT 认证中间件
    对非白名单路径进行 JWT 验证
    """

    async def dispatch(self, request: Request, call_next):
        # 检查是否是白名单路径
        if is_white_list_path(request.url.path):
            return await call_next(request)

        # 获取 JWT Token
        token = jwt_handler.extract_token(request)

        # 如果没有 Token，返回未授权
        if not token:
            return JSONResponse(
                status_code=401,
                content=Result.unauthorized().model_dump()
            )

        try:
            # 解析 Token
            claims = jwt_handler.parse_token(token)

            # 检查 Token 是否过期
            if jwt_handler.is_token_expired(claims):
                return JSONResponse(
                    status_code=401,
                    content=Result(
                        code=401,
                        message="JWT令牌过期，请重新登录"
                    ).model_dump()
                )

            # 将 claims 存储到请求状态
            request.state.jwt_claims = claims

            # 继续处理请求
            response = await call_next(request)
            return response

        except Exception as e:
            logger.error(f"JWT 验证失败: {e}")
            return JSONResponse(
                status_code=401,
                content=Result.unauthorized().model_dump()
            )


class ResponseFormatMiddleware(BaseHTTPMiddleware):
    """
    响应格式化中间件
    确保所有响应都符合统一格式
    """

    async def dispatch(self, request: Request, call_next):
        try:
            response = await call_next(request)

            # 如果是 JSONResponse 且不是文档相关路径，进行格式化处理
            if (isinstance(response, JSONResponse) and
                not request.url.path.startswith(("/docs", "/redoc", "/openapi"))):
                return response

            return response

        except BusinessException as e:
            return JSONResponse(
                status_code=200,
                content=Result(
                    code=e.error_code.code,
                    message=e.message
                ).model_dump()
            )
        except Exception as e:
            logger.exception("请求处理异常")
            return JSONResponse(
                status_code=500,
                content=Result.error(
                    message="服务器内部错误",
                    code=ErrorCode.INTERNAL_SERVER_ERROR.code
                ).model_dump()
            )
