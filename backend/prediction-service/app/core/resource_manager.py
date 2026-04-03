"""
资源管理器
实现训练任务的资源隔离和限制，确保不影响接口服务性能
"""
import os
import sys
import time
import psutil
import asyncio
import logging
from typing import Optional, Callable, Any
from concurrent.futures import ProcessPoolExecutor, ThreadPoolExecutor
from functools import wraps

from app.core.config import settings

logger = logging.getLogger(__name__)


class ResourceLimiter:
    """资源限制器"""

    def __init__(self):
        self.cpu_limit_percent = settings.training.cpu_limit_percent
        self.process = psutil.Process()

    def limit_cpu_usage(self):
        """限制当前进程的CPU使用率"""
        try:
            # 设置CPU亲和性，限制使用的CPU核心
            cpu_count = psutil.cpu_count()
            max_cpus = max(1, int(cpu_count * self.cpu_limit_percent / 100))
            cpus_to_use = list(range(max_cpus))
            self.process.cpu_affinity(cpus_to_use)
            logger.info(f"限制CPU使用: 使用核心 {cpus_to_use}")
        except Exception as e:
            logger.warning(f"设置CPU限制失败: {e}")

    def set_nice_priority(self, priority: int = 10):
        """设置进程优先级（Windows/Linux兼容）"""
        try:
            if sys.platform == 'win32':
                # Windows: 使用优先级类
                import ctypes
                handle = ctypes.windll.kernel32.GetCurrentProcess()
                # BELOW_NORMAL_PRIORITY_CLASS = 0x00004000
                ctypes.windll.kernel32.SetPriorityClass(handle, 0x00004000)
            else:
                # Linux/Mac: 使用nice值
                os.nice(priority)
            logger.info(f"设置进程优先级: {priority}")
        except Exception as e:
            logger.warning(f"设置进程优先级失败: {e}")


class TrainingExecutor:
    """训练任务执行器（带资源隔离）"""

    def __init__(self):
        self.max_workers = settings.training.max_workers
        self.use_process_pool = settings.training.use_process_pool
        self._executor: Optional[Any] = None
        self._resource_limiter = ResourceLimiter()

    def _init_executor(self):
        """初始化执行器"""
        if self._executor is None:
            if self.use_process_pool:
                # 使用进程池实现真正的并行和隔离
                self._executor = ProcessPoolExecutor(
                    max_workers=self.max_workers,
                    initializer=self._worker_init
                )
                logger.info(f"初始化进程池，工作进程数: {self.max_workers}")
            else:
                # 使用线程池
                self._executor = ThreadPoolExecutor(max_workers=self.max_workers)
                logger.info(f"初始化线程池，工作线程数: {self.max_workers}")

    @staticmethod
    def _worker_init():
        """工作进程初始化函数"""
        # 在工作进程中设置资源限制
        limiter = ResourceLimiter()
        limiter.limit_cpu_usage()
        limiter.set_nice_priority()

    async def submit(self, func: Callable, *args, **kwargs) -> Any:
        """提交任务到执行器"""
        self._init_executor()
        loop = asyncio.get_event_loop()
        return await loop.run_in_executor(
            self._executor,
            func,
            *args,
            **kwargs
        )

    async def shutdown(self):
        """关闭执行器"""
        if self._executor:
            self._executor.shutdown(wait=True)
            self._executor = None
            logger.info("训练执行器已关闭")


class CPUMonitor:
    """CPU监控器"""

    def __init__(self, interval: float = 1.0):
        self.interval = interval
        self._running = False
        self._callbacks = []

    def add_callback(self, callback: Callable[[float], None]):
        """添加CPU使用率回调"""
        self._callbacks.append(callback)

    async def start_monitoring(self):
        """开始监控CPU使用率"""
        self._running = True
        while self._running:
            try:
                cpu_percent = psutil.cpu_percent(interval=self.interval)
                for callback in self._callbacks:
                    try:
                        callback(cpu_percent)
                    except Exception as e:
                        logger.error(f"CPU回调执行失败: {e}")
                await asyncio.sleep(self.interval)
            except Exception as e:
                logger.error(f"CPU监控异常: {e}")
                await asyncio.sleep(self.interval)

    def stop_monitoring(self):
        """停止监控"""
        self._running = False


def with_resource_limit(func: Callable) -> Callable:
    """装饰器：为函数添加资源限制"""
    @wraps(func)
    def wrapper(*args, **kwargs):
        limiter = ResourceLimiter()
        limiter.limit_cpu_usage()
        limiter.set_nice_priority()
        return func(*args, **kwargs)
    return wrapper


class AsyncResourceGovernor:
    """异步资源治理器"""

    def __init__(self):
        self.yield_interval = settings.training.yield_interval
        self.batch_size = settings.training.batch_size
        self._semaphore = asyncio.Semaphore(settings.training.max_workers)

    async def acquire(self):
        """获取资源许可"""
        await self._semaphore.acquire()

    def release(self):
        """释放资源许可"""
        self._semaphore.release()

    async def yield_control(self):
        """让出控制权，允许其他协程执行"""
        await asyncio.sleep(self.yield_interval)

    async def run_with_governance(self, func: Callable, *args, **kwargs) -> Any:
        """在资源治理下运行函数"""
        await self.acquire()
        try:
            return await func(*args, **kwargs)
        finally:
            self.release()


# 全局实例
training_executor = TrainingExecutor()
resource_governor = AsyncResourceGovernor()
cpu_monitor = CPUMonitor()
