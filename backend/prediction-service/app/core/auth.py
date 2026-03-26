"""
权限验证模块
提供装饰器和依赖函数用于权限控制
"""
from functools import wraps
from typing import List, Optional, Callable
from fastapi import Request, HTTPException, Depends

from app.core.jwt_handler import JWTContext, jwt_handler, require_auth


class PermissionChecker:
    """权限检查器"""

    def __init__(self, roles: Optional[List[str]] = None):
        self.roles = roles or []

    def __call__(self, ctx: JWTContext = Depends(require_auth)):
        """执行权限检查"""
        if not self.roles:
            return ctx

        # 检查是否有任意一个角色
        if not ctx.has_any_role(self.roles):
            raise HTTPException(status_code=403, detail="当前用户无权限")

        return ctx


def require_role(*roles: str):
    """
    要求具有指定角色的依赖函数

    使用示例:
        @router.get("/admin-only")
        async def admin_endpoint(ctx: JWTContext = Depends(require_role("admin"))):
            return {"message": "Admin access granted"}
    """
    def checker(ctx: JWTContext = Depends(require_auth)) -> JWTContext:
        if not ctx.has_any_role(list(roles)):
            raise HTTPException(status_code=403, detail="当前用户无权限")
        return ctx
    return checker


def require_any_role(roles: List[str]):
    """
    要求具有任意一个角色的依赖函数

    使用示例:
        @router.get("/multi-role")
        async def multi_role_endpoint(ctx: JWTContext = Depends(require_any_role(["admin", "operator"]))):
            return {"message": "Access granted"}
    """
    def checker(ctx: JWTContext = Depends(require_auth)) -> JWTContext:
        if not ctx.has_any_role(roles):
            raise HTTPException(status_code=403, detail="当前用户无权限")
        return ctx
    return checker


def check_permission(request: Request, required_roles: Optional[List[str]] = None) -> JWTContext:
    """
    检查权限（非装饰器方式）

    Args:
        request: FastAPI 请求对象
        required_roles: 需要的角色列表

    Returns:
        JWTContext: JWT 上下文

    Raises:
        HTTPException: 未授权或无权限
    """
    ctx = jwt_handler.get_context(request)

    # 检查是否登录
    if not ctx.is_authenticated():
        raise HTTPException(status_code=401, detail="登录状态已失效，请重新登录")

    # 检查角色权限
    if required_roles and not ctx.has_any_role(required_roles):
        raise HTTPException(status_code=403, detail="当前用户无权限")

    return ctx


# 白名单路径检查
WHITE_LIST_PATHS = [
    "/",
    "/health",
    "/docs",
    "/redoc",
    "/openapi.json",
    "/api/v1/prediction/health",
]


def is_white_list_path(path: str) -> bool:
    """检查路径是否在白名单中"""
    for white_path in WHITE_LIST_PATHS:
        if path == white_path or path.startswith(white_path):
            return True
    return False
