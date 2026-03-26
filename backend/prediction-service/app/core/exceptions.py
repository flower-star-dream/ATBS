"""
异常定义和错误码枚举
与 Java BaseExceptionEnum 保持一致
"""
from enum import Enum
from fastapi import HTTPException


class ErrorCode(Enum):
    """
    错误码枚举
    与 Java 的 BaseExceptionEnum 保持一致
    """
    SUCCESS = (200, "操作成功")
    ERROR = (500, "操作失败")
    PARAM_ERROR = (400, "参数错误")
    THE_QUERY_PARAMETER_CANNOT_BE_EMPTY = (400, "查询参数不能为空")
    UNAUTHORIZED = (401, "登录状态已失效，请重新登录")
    THE_JWT_TOKEN_HAS_EXPIRED = (401, "JWT令牌过期，请重新登录")
    FORBIDDEN = (403, "当前用户无权限")
    NOT_FOUND = (404, "资源不存在")
    METHOD_NOT_ALLOWED = (405, "方法不允许")
    CONFLICT = (409, "资源冲突")
    TOO_MANY_REQUESTS = (429, "请求过于频繁")
    INTERNAL_SERVER_ERROR = (500, "服务器内部错误")
    SERVICE_UNAVAILABLE = (503, "服务不可用")

    def __init__(self, code: int, message: str):
        self.code = code
        self.message = message

    @classmethod
    def get_by_code(cls, code: int) -> "ErrorCode":
        """根据错误码获取枚举"""
        for item in cls:
            if item.code == code:
                return item
        return cls.ERROR


class BusinessException(Exception):
    """业务异常"""

    def __init__(self, error_code: ErrorCode = None, message: str = None):
        self.error_code = error_code or ErrorCode.ERROR
        self.message = message or self.error_code.message
        super().__init__(self.message)


class UnauthorizedException(Exception):
    """未授权异常"""

    def __init__(self, message: str = "登录状态已失效，请重新登录"):
        self.message = message
        super().__init__(self.message)


class ForbiddenException(Exception):
    """无权限异常"""

    def __init__(self, message: str = "当前用户无权限"):
        self.message = message
        super().__init__(self.message)
