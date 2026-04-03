"""
客流预测服务 - FastAPI 主应用
基于 ARIMA 模型的客流预测微服务
与 Java 微服务生态系统完全兼容
"""
import os
import sys
import logging
import time
import traceback
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

# 添加项目路径
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from app.api.routes import prediction
from app.core.config import settings
from app.core.logging import setup_logging
from app.core.nacos_client import init_nacos, close_nacos
from app.core.middleware import TraceIdMiddleware
from app.core.result import Result
from app.core.exceptions import BusinessException, ErrorCode

# 设置日志（传入配置）
setup_logging(settings)
logger = logging.getLogger(__name__)


# 全局异常处理器
def register_exception_handlers(app: FastAPI):
    """注册全局异常处理器"""

    @app.exception_handler(BusinessException)
    async def business_exception_handler(request: Request, exc: BusinessException):
        """业务异常处理"""
        return JSONResponse(
            status_code=exc.error_code.code,
            content=Result(
                code=exc.error_code.code,
                message=exc.message
            ).model_dump()
        )

    @app.exception_handler(401)
    async def unauthorized_exception_handler(request: Request, exc):
        """未授权异常处理"""
        return JSONResponse(
            status_code=401,
            content=Result.unauthorized().model_dump()
        )

    @app.exception_handler(403)
    async def forbidden_exception_handler(request: Request, exc):
        """无权限异常处理"""
        return JSONResponse(
            status_code=403,
            content=Result.forbidden().model_dump()
        )

    @app.exception_handler(Exception)
    async def global_exception_handler(request: Request, exc: Exception):
        """全局异常处理"""
        path = request.url.path
        error_msg = str(exc)
        error_trace = traceback.format_exc()

        logger.error(f"[GlobalException] Path: {path}")
        logger.error(f"[GlobalException] Error: {error_msg}")
        logger.error(f"[GlobalException] Traceback:\n{error_trace}")

        # 对于 openapi 请求，返回更多调试信息
        if "/v3/api-docs" in path or "/openapi" in path:
            return JSONResponse(
                status_code=500,
                content={
                    "error": "Internal Server Error",
                    "message": error_msg,
                    "path": path
                }
            )

        return JSONResponse(
            status_code=500,
            content=Result.error(
                message="服务器内部错误",
                code=ErrorCode.INTERNAL_SERVER_ERROR.code
            ).model_dump()
        )


@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    应用生命周期管理
    """
    # 启动时执行
    logger.info("=" * 60)
    logger.info(f"客流预测服务启动")
    logger.info(f"版本: {settings.app_version}")
    logger.info(f"环境: {settings.app_env}")
    logger.info(f"Nacos 服务注册: {'启用' if settings.nacos_enabled else '禁用'}")
    logger.info("=" * 60)

    # 确保目录存在
    os.makedirs(settings.model_path, exist_ok=True)
    os.makedirs(settings.data_path, exist_ok=True)

    # 初始化 Nacos 服务注册
    if settings.nacos_enabled:
        nacos_success = await init_nacos(settings)
        if nacos_success:
            logger.info("Nacos 服务注册成功")
        else:
            logger.warning("Nacos 服务注册失败，服务将继续运行")

    yield

    # 关闭时执行
    logger.info("\n客流预测服务关闭")

    # 注销 Nacos 服务
    if settings.nacos_enabled:
        await close_nacos()


# FastAPI 应用配置常量
# 请求体大小限制：10MB
MAX_REQUEST_BODY_SIZE = 10 * 1024 * 1024


# 创建 FastAPI 应用
# 开发环境启用文档，生产环境禁用
# 配置与 Java 微服务保持一致：/v3/api-docs 和 /doc.html
if settings.app_env == "development":
    app = FastAPI(
        title=settings.app_name,
        description=settings.app_description,
        version=settings.app_version,
        docs_url="/doc.html",  # 与 Java 服务保持一致
        redoc_url=None,  # 禁用 ReDoc
        openapi_url="/v3/api-docs",  # 与 Java SpringDoc 保持一致
        lifespan=lifespan
    )
else:
    # 生产环境禁用文档
    app = FastAPI(
        title=settings.app_name,
        description=settings.app_description,
        version=settings.app_version,
        docs_url=None,
        redoc_url=None,
        openapi_url=None,
        lifespan=lifespan
    )

# 注册异常处理器
register_exception_handlers(app)

# 添加请求体大小限制中间件
@app.middleware("http")
async def request_size_limit_middleware(request: Request, call_next):
    """限制请求体大小，防止内存耗尽攻击"""
    # 检查 Content-Length 头
    content_length = request.headers.get('content-length')
    if content_length:
        try:
            size = int(content_length)
            if size > MAX_REQUEST_BODY_SIZE:
                logger.warning(f"请求体过大: {size} bytes，限制: {MAX_REQUEST_BODY_SIZE} bytes")
                return JSONResponse(
                    status_code=413,
                    content={"code": 413, "message": "请求体过大，最大允许 10MB", "data": None}
                )
        except ValueError:
            pass

    return await call_next(request)


# 添加请求日志中间件
@app.middleware("http")
async def request_logging_middleware(request: Request, call_next):
    """记录所有请求和响应"""
    start_time = time.time()
    path = request.url.path
    method = request.method
    client = request.client.host if request.client else "unknown"

    # 详细记录请求信息（用于诊断网关问题）
    headers = dict(request.headers)
    # 移除敏感信息
    headers.pop('authorization', None)
    headers.pop('cookie', None)

    logger.info(f"[Request] {method} {path} - Client: {client}")
    logger.debug(f"[Request Headers] {headers}")
    logger.debug(f"[Request Query] {request.query_params}")

    try:
        response = await call_next(request)
        duration = time.time() - start_time
        logger.info(f"[Response] {method} {path} - Status: {response.status_code} - Duration: {duration:.3f}s")
        return response
    except Exception as exc:
        duration = time.time() - start_time
        logger.error(f"[Error] {method} {path} - Duration: {duration:.3f}s - Error: {str(exc)}")
        logger.error(f"[Error] Traceback: {traceback.format_exc()}")
        raise

# 添加 TraceId 中间件
app.add_middleware(TraceIdMiddleware)

# 配置 CORS - 从配置类读取跨域配置
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_allow_origins,
    allow_methods=settings.cors_allow_methods,
    allow_headers=settings.cors_allow_headers,
    expose_headers=settings.cors_expose_headers,
    allow_credentials=settings.cors_allow_credentials,
    max_age=settings.cors_max_age,
)

# 注册路由
app.include_router(prediction.router, prefix="/api/mgmt/v1/prediction", tags=["预测服务"])


@app.get("/")
async def root():
    """根路径 - 服务信息"""
    return Result.success(data={
        "name": settings.app_name,
        "version": settings.app_version,
        "description": settings.app_description,
        "docs": "/doc.html"
    })


@app.get("/health")
async def health():
    """健康检查 - 供 Nacos 和网关调用"""
    return Result.success(data={
        "status": "UP",
        "service": settings.nacos_service_name,
        "version": settings.app_version
    })


@app.get("/actuator/health")
async def actuator_health():
    """Spring Boot Actuator 风格健康检查"""
    return Result.success(data={
        "status": "UP"
    })


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "app.main:app",
        host=settings.host,
        port=settings.port,
        reload=settings.app_env == "development",
        workers=1
    )
