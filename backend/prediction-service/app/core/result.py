"""
统一响应结果类 - 与 Java Result 类保持一致
"""
from typing import TypeVar, Generic, Optional, Any
from pydantic import BaseModel, ConfigDict

T = TypeVar('T')


class Result(BaseModel, Generic[T]):
    """
    统一响应结果
    与 Java 的 top.flowerstardream.base.result.Result 保持一致
    """
    model_config = ConfigDict(arbitrary_types_allowed=True)

    code: int = 200
    message: str = "操作成功"
    data: Optional[T] = None

    @staticmethod
    def success(data: T = None, message: str = "操作成功") -> "Result[T]":
        """成功响应"""
        return Result(code=200, message=message, data=data)

    @staticmethod
    def error(message: str = "操作失败", code: int = 500) -> "Result[Any]":
        """错误响应"""
        return Result(code=code, message=message, data=None)

    @staticmethod
    def unauthorized(message: str = "登录状态已失效，请重新登录") -> "Result[Any]":
        """未授权响应"""
        return Result(code=401, message=message, data=None)

    @staticmethod
    def forbidden(message: str = "当前用户无权限") -> "Result[Any]":
        """无权限响应"""
        return Result(code=403, message=message, data=None)

    @staticmethod
    def param_error(message: str = "参数错误") -> "Result[Any]":
        """参数错误响应"""
        return Result(code=400, message=message, data=None)

    @staticmethod
    def not_found(message: str = "资源不存在") -> "Result[Any]":
        """资源不存在响应"""
        return Result(code=404, message=message, data=None)

    def is_success(self) -> bool:
        """判断是否成功"""
        return self.code == 200


def success_result(data: T = None, message: str = "操作成功") -> Result[T]:
    """快捷方法：成功响应"""
    return Result.success(data=data, message=message)


def error_result(message: str = "操作失败", code: int = 500) -> Result:
    """快捷方法：错误响应"""
    return Result.error(message=message, code=code)
