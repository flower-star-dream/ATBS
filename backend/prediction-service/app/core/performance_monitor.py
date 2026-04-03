"""
性能监控中间件
监控API响应时间和系统资源使用情况
"""
import time
import logging
import asyncio
from typing import Callable, Dict, List, Optional
from dataclasses import dataclass, field
from datetime import datetime, timedelta
from collections import deque
import statistics

import psutil
from fastapi import Request, Response
from starlette.middleware.base import BaseHTTPMiddleware

logger = logging.getLogger(__name__)


@dataclass
class RequestMetrics:
    """请求指标"""
    path: str
    method: str
    status_code: int
    duration_ms: float
    timestamp: datetime = field(default_factory=lambda: datetime.now())


class PerformanceMonitor:
    """性能监控器"""

    def __init__(self, window_size: int = 1000):
        self.window_size = window_size
        self._metrics: deque = deque(maxlen=window_size)
        self._alerts: List[Dict] = []
        self._alert_thresholds = {
            'avg_response_time': 500,  # ms
            'p95_response_time': 1000,  # ms
            'error_rate': 0.05  # 5%
        }

    def record_request(self, metrics: RequestMetrics):
        """记录请求指标"""
        self._metrics.append(metrics)
        self._check_alerts()

    def get_statistics(self) -> Dict:
        """获取性能统计"""
        if not self._metrics:
            return {
                'total_requests': 0,
                'avg_response_time': 0,
                'p95_response_time': 0,
                'p99_response_time': 0,
                'error_rate': 0,
                'requests_per_second': 0
            }

        durations = [m.duration_ms for m in self._metrics]
        error_count = sum(1 for m in self._metrics if m.status_code >= 500)

        # 计算时间窗口
        if len(self._metrics) >= 2:
            time_span = (self._metrics[-1].timestamp - self._metrics[0].timestamp).total_seconds()
            rps = len(self._metrics) / max(time_span, 1)
        else:
            rps = 0

        return {
            'total_requests': len(self._metrics),
            'avg_response_time': statistics.mean(durations),
            'p95_response_time': self._percentile(durations, 95),
            'p99_response_time': self._percentile(durations, 99),
            'error_rate': error_count / len(self._metrics),
            'requests_per_second': rps
        }

    def _percentile(self, data: List[float], percentile: int) -> float:
        """计算百分位数"""
        if not data:
            return 0
        sorted_data = sorted(data)
        index = int(len(sorted_data) * percentile / 100)
        return sorted_data[min(index, len(sorted_data) - 1)]

    def _check_alerts(self):
        """检查是否需要告警"""
        stats = self.get_statistics()

        if stats['avg_response_time'] > self._alert_thresholds['avg_response_time']:
            self._add_alert(
                'high_avg_response_time',
                f"平均响应时间过高: {stats['avg_response_time']:.2f}ms"
            )

        if stats['p95_response_time'] > self._alert_thresholds['p95_response_time']:
            self._add_alert(
                'high_p95_response_time',
                f"P95响应时间过高: {stats['p95_response_time']:.2f}ms"
            )

        if stats['error_rate'] > self._alert_thresholds['error_rate']:
            self._add_alert(
                'high_error_rate',
                f"错误率过高: {stats['error_rate']:.2%}"
            )

    def _add_alert(self, alert_type: str, message: str):
        """添加告警"""
        alert = {
            'type': alert_type,
            'message': message,
            'timestamp': datetime.now()
        }
        self._alerts.append(alert)
        logger.warning(f"性能告警: {message}")

    def get_alerts(self, limit: int = 10) -> List[Dict]:
        """获取最近的告警"""
        return sorted(
            self._alerts,
            key=lambda x: x['timestamp'],
            reverse=True
        )[:limit]

    def clear_alerts(self):
        """清除告警"""
        self._alerts.clear()


class PerformanceMiddleware(BaseHTTPMiddleware):
    """性能监控中间件"""

    def __init__(self, app, monitor: PerformanceMonitor):
        super().__init__(app)
        self.monitor = monitor

    async def dispatch(self, request: Request, call_next: Callable) -> Response:
        """处理请求并记录性能指标"""
        start_time = time.time()

        try:
            response = await call_next(request)
            status_code = response.status_code
        except Exception as e:
            status_code = 500
            raise e
        finally:
            duration_ms = (time.time() - start_time) * 1000

            metrics = RequestMetrics(
                path=request.url.path,
                method=request.method,
                status_code=status_code,
                duration_ms=duration_ms
            )
            self.monitor.record_request(metrics)

            # 记录慢请求
            if duration_ms > 1000:
                logger.warning(f"慢请求: {request.method} {request.url.path} - {duration_ms:.2f}ms")

        return response


class SystemResourceMonitor:
    """系统资源监控器"""

    def __init__(self, interval: float = 5.0):
        self.interval = interval
        self._running = False
        self._callbacks: List[Callable] = []

    def add_callback(self, callback: Callable[[Dict], None]):
        """添加资源状态回调"""
        self._callbacks.append(callback)

    async def start_monitoring(self):
        """开始监控系统资源"""
        self._running = True
        while self._running:
            try:
                stats = self._get_resource_stats()
                for callback in self._callbacks:
                    try:
                        callback(stats)
                    except Exception as e:
                        logger.error(f"资源回调执行失败: {e}")

                # 检查资源告警
                self._check_resource_alerts(stats)

                await asyncio.sleep(self.interval)
            except Exception as e:
                logger.error(f"资源监控异常: {e}")
                await asyncio.sleep(self.interval)

    def stop_monitoring(self):
        """停止监控"""
        self._running = False

    def _get_resource_stats(self) -> Dict:
        """获取系统资源统计"""
        return {
            'cpu_percent': psutil.cpu_percent(interval=1),
            'memory_percent': psutil.virtual_memory().percent,
            'disk_usage': psutil.disk_usage('/').percent,
            'timestamp': datetime.now().isoformat()
        }

    def _check_resource_alerts(self, stats: Dict):
        """检查资源告警"""
        if stats['cpu_percent'] > 80:
            logger.warning(f"CPU使用率过高: {stats['cpu_percent']}%")

        if stats['memory_percent'] > 85:
            logger.warning(f"内存使用率过高: {stats['memory_percent']}%")

        if stats['disk_usage'] > 90:
            logger.warning(f"磁盘使用率过高: {stats['disk_usage']}%")


# 全局实例
performance_monitor = PerformanceMonitor()
system_monitor = SystemResourceMonitor()
