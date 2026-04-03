"""
任务管理模块
负责训练任务的创建、状态管理、进度跟踪和结果存储
"""
import uuid
import asyncio
import logging
from datetime import datetime, timezone, timedelta
from typing import Dict, Optional, Any, Callable
from enum import Enum
from dataclasses import dataclass, field
from concurrent.futures import ThreadPoolExecutor
import threading

logger = logging.getLogger(__name__)


class TaskStatus(str, Enum):
    """任务状态枚举"""
    PENDING = "pending"           # 待处理
    PROCESSING = "processing"     # 处理中
    COMPLETED = "completed"       # 已完成
    FAILED = "failed"             # 失败
    CANCELLED = "cancelled"       # 已取消


class TaskStage(str, Enum):
    """训练阶段枚举"""
    INITIALIZING = "initializing"           # 初始化
    DATA_LOADING = "data_loading"           # 数据加载
    DATA_PROCESSING = "data_processing"     # 数据处理
    PARAMETER_OPTIMIZATION = "parameter_optimization"  # 参数优化
    MODEL_TRAINING = "model_training"       # 模型训练
    MODEL_VALIDATION = "model_validation"   # 模型验证
    MODEL_SAVING = "model_saving"           # 模型保存
    COMPLETED = "completed"                 # 完成


@dataclass
class TaskProgress:
    """任务进度信息"""
    stage: TaskStage = TaskStage.INITIALIZING
    percent: int = 0                          # 完成百分比 (0-100)
    current_step: str = ""                    # 当前步骤描述
    total_steps: int = 7                      # 总步骤数
    completed_steps: int = 0                  # 已完成步骤数
    estimated_remaining_seconds: Optional[int] = None  # 预计剩余时间(秒)
    message: str = ""                         # 进度消息
    updated_at: datetime = field(default_factory=lambda: datetime.now(timezone(timedelta(hours=8))))

    def to_dict(self) -> Dict[str, Any]:
        """转换为字典"""
        return {
            "stage": self.stage.value,
            "percent": self.percent,
            "current_step": self.current_step,
            "total_steps": self.total_steps,
            "completed_steps": self.completed_steps,
            "estimated_remaining_seconds": self.estimated_remaining_seconds,
            "message": self.message,
            "updated_at": self.updated_at.isoformat()
        }


@dataclass
class TrainingTask:
    """训练任务数据类"""
    task_id: str
    status: TaskStatus
    progress: TaskProgress
    created_at: datetime
    started_at: Optional[datetime] = None
    completed_at: Optional[datetime] = None
    result: Optional[Dict[str, Any]] = None
    error_info: Optional[Dict[str, Any]] = None
    user_id: Optional[str] = None

    def to_dict(self) -> Dict[str, Any]:
        """转换为字典"""
        return {
            "task_id": self.task_id,
            "status": self.status.value,
            "progress": self.progress.to_dict(),
            "created_at": self.created_at.isoformat(),
            "started_at": self.started_at.isoformat() if self.started_at else None,
            "completed_at": self.completed_at.isoformat() if self.completed_at else None,
            "result": self.result,
            "error_info": self.error_info
        }


class TaskManager:
    """
    任务管理器
    负责任务的创建、状态管理、进度更新和结果存储
    """

    def __init__(self, max_workers: int = 2):
        self.tasks: Dict[str, TrainingTask] = {}
        self._task_queue: asyncio.Queue = asyncio.Queue()
        self._executor = ThreadPoolExecutor(max_workers=max_workers)
        self._lock = asyncio.Lock()
        self._progress_callbacks: Dict[str, Callable] = {}
        self._beijing_tz = timezone(timedelta(hours=8))
        
        # 启动后台任务处理器
        self._processor_task: Optional[asyncio.Task] = None

    async def start(self):
        """启动任务处理器"""
        if self._processor_task is None or self._processor_task.done():
            self._processor_task = asyncio.create_task(self._process_tasks())
            logger.info("任务处理器已启动")

    async def stop(self):
        """停止任务处理器"""
        if self._processor_task and not self._processor_task.done():
            self._processor_task.cancel()
            try:
                await self._processor_task
            except asyncio.CancelledError:
                pass
        self._executor.shutdown(wait=True)
        logger.info("任务处理器已停止")

    def _generate_task_id(self) -> str:
        """
        生成唯一的任务ID
        格式: TRAIN-{timestamp}-{uuid前8位}
        """
        timestamp = datetime.now(self._beijing_tz).strftime("%Y%m%d%H%M%S")
        short_uuid = uuid.uuid4().hex[:8].upper()
        return f"TRAIN-{timestamp}-{short_uuid}"

    async def create_task(self, user_id: Optional[str] = None) -> TrainingTask:
        """
        创建新的训练任务

        Args:
            user_id: 用户ID

        Returns:
            新创建的任务对象
        """
        task_id = self._generate_task_id()
        now = datetime.now(self._beijing_tz)
        
        task = TrainingTask(
            task_id=task_id,
            status=TaskStatus.PENDING,
            progress=TaskProgress(),
            created_at=now,
            user_id=user_id
        )

        async with self._lock:
            self.tasks[task_id] = task

        logger.info(f"创建训练任务: {task_id}")
        return task

    async def get_task(self, task_id: str) -> Optional[TrainingTask]:
        """
        获取任务信息

        Args:
            task_id: 任务ID

        Returns:
            任务对象，不存在则返回None
        """
        async with self._lock:
            return self.tasks.get(task_id)

    async def update_task_status(
        self,
        task_id: str,
        status: TaskStatus,
        result: Optional[Dict] = None,
        error_info: Optional[Dict] = None
    ):
        """
        更新任务状态

        Args:
            task_id: 任务ID
            status: 新状态
            result: 任务结果（完成时）
            error_info: 错误信息（失败时）
        """
        async with self._lock:
            task = self.tasks.get(task_id)
            if not task:
                logger.warning(f"更新状态失败: 任务 {task_id} 不存在")
                return

            task.status = status
            now = datetime.now(self._beijing_tz)

            if status == TaskStatus.PROCESSING and not task.started_at:
                task.started_at = now
            elif status in [TaskStatus.COMPLETED, TaskStatus.FAILED, TaskStatus.CANCELLED]:
                task.completed_at = now

            if result:
                task.result = result
            if error_info:
                task.error_info = error_info

        logger.info(f"任务 {task_id} 状态更新为: {status.value}")

    async def update_task_progress(
        self,
        task_id: str,
        stage: Optional[TaskStage] = None,
        percent: Optional[int] = None,
        current_step: Optional[str] = None,
        message: Optional[str] = None,
        estimated_remaining_seconds: Optional[int] = None
    ):
        """
        更新任务进度

        Args:
            task_id: 任务ID
            stage: 当前阶段
            percent: 完成百分比
            current_step: 当前步骤描述
            message: 进度消息
            estimated_remaining_seconds: 预计剩余时间(秒)
        """
        async with self._lock:
            task = self.tasks.get(task_id)
            if not task:
                return

            progress = task.progress

            if stage:
                progress.stage = stage
                # 根据阶段自动更新步骤计数
                stage_order = [
                    TaskStage.INITIALIZING,
                    TaskStage.DATA_LOADING,
                    TaskStage.DATA_PROCESSING,
                    TaskStage.PARAMETER_OPTIMIZATION,
                    TaskStage.MODEL_TRAINING,
                    TaskStage.MODEL_VALIDATION,
                    TaskStage.MODEL_SAVING,
                    TaskStage.COMPLETED
                ]
                if stage in stage_order:
                    progress.completed_steps = stage_order.index(stage)

            if percent is not None:
                progress.percent = max(0, min(100, percent))

            if current_step:
                progress.current_step = current_step

            if message:
                progress.message = message

            if estimated_remaining_seconds is not None:
                progress.estimated_remaining_seconds = estimated_remaining_seconds

            progress.updated_at = datetime.now(self._beijing_tz)

        # 触发进度回调
        if task_id in self._progress_callbacks:
            try:
                callback = self._progress_callbacks[task_id]
                await callback(task_id, task.progress)
            except Exception as e:
                logger.error(f"进度回调执行失败: {e}")

    async def submit_task(self, task_id: str, training_func: Callable, *args, **kwargs):
        """
        提交任务到执行队列

        Args:
            task_id: 任务ID
            training_func: 训练函数
            *args, **kwargs: 训练函数参数
        """
        await self._task_queue.put((task_id, training_func, args, kwargs))
        logger.info(f"任务 {task_id} 已提交到队列")

    async def _process_tasks(self):
        """后台任务处理器"""
        while True:
            try:
                task_id, training_func, args, kwargs = await self._task_queue.get()
                
                # 更新任务状态为处理中
                await self.update_task_status(task_id, TaskStatus.PROCESSING)
                
                # 在线程池中执行训练任务
                loop = asyncio.get_event_loop()
                try:
                    result = await loop.run_in_executor(
                        self._executor,
                        self._run_training_with_progress,
                        task_id,
                        training_func,
                        *args,
                        **kwargs
                    )
                    
                    await self.update_task_status(
                        task_id,
                        TaskStatus.COMPLETED,
                        result=result
                    )
                    
                except Exception as e:
                    logger.error(f"任务 {task_id} 执行失败: {e}", exc_info=True)
                    error_info = {
                        "error_type": type(e).__name__,
                        "error_message": str(e),
                        "timestamp": datetime.now(self._beijing_tz).isoformat()
                    }
                    await self.update_task_status(
                        task_id,
                        TaskStatus.FAILED,
                        error_info=error_info
                    )

            except asyncio.CancelledError:
                break
            except Exception as e:
                logger.error(f"任务处理器异常: {e}", exc_info=True)

    def _run_training_with_progress(
        self,
        task_id: str,
        training_func: Callable,
        *args,
        **kwargs
    ) -> Dict:
        """
        运行训练任务并更新进度
        
        注意：此方法在单独的线程中执行
        """
        # 创建新的事件循环用于异步进度更新
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)

        # 注入进度回调函数
        progress_callback = self._create_progress_callback(task_id)
        kwargs['progress_callback'] = progress_callback

        try:
            result = training_func(*args, **kwargs)
            return result
        finally:
            loop.close()

    def _create_progress_callback(self, task_id: str) -> Callable:
        """
        创建进度回调函数
        
        注意：由于训练在单独的线程中执行，我们需要使用线程安全的方式更新进度
        """
        def callback(stage: TaskStage, percent: int, message: str, **kwargs):
            # 使用线程安全的方式更新进度
            # 这里我们直接更新内存中的任务对象
            task = self.tasks.get(task_id)
            if task:
                progress = task.progress
                progress.stage = stage
                progress.percent = percent
                progress.message = message
                progress.updated_at = datetime.now(self._beijing_tz)
                
                if 'current_step' in kwargs:
                    progress.current_step = kwargs['current_step']
                if 'estimated_remaining_seconds' in kwargs:
                    progress.estimated_remaining_seconds = kwargs['estimated_remaining_seconds']
                if 'completed_steps' in kwargs:
                    progress.completed_steps = kwargs['completed_steps']

        return callback

    async def cancel_task(self, task_id: str) -> bool:
        """
        取消任务

        Args:
            task_id: 任务ID

        Returns:
            是否成功取消
        """
        async with self._lock:
            task = self.tasks.get(task_id)
            if not task:
                return False

            if task.status in [TaskStatus.COMPLETED, TaskStatus.FAILED]:
                return False

            task.status = TaskStatus.CANCELLED
            task.completed_at = datetime.now(self._beijing_tz)

        logger.info(f"任务 {task_id} 已取消")
        return True

    async def get_all_tasks(
        self,
        status: Optional[TaskStatus] = None,
        limit: int = 100,
        offset: int = 0
    ) -> list:
        """
        获取任务列表

        Args:
            status: 按状态筛选
            limit: 返回数量限制
            offset: 偏移量

        Returns:
            任务列表
        """
        async with self._lock:
            tasks = list(self.tasks.values())
            
            if status:
                tasks = [t for t in tasks if t.status == status]
            
            # 按创建时间倒序
            tasks.sort(key=lambda x: x.created_at, reverse=True)
            
            return tasks[offset:offset + limit]

    async def cleanup_old_tasks(self, max_age_hours: int = 24):
        """
        清理旧任务

        Args:
            max_age_hours: 最大保留时间（小时）
        """
        now = datetime.now(self._beijing_tz)
        cutoff_time = now - timedelta(hours=max_age_hours)

        async with self._lock:
            expired_tasks = [
                task_id for task_id, task in self.tasks.items()
                if task.created_at < cutoff_time
                and task.status in [TaskStatus.COMPLETED, TaskStatus.FAILED, TaskStatus.CANCELLED]
            ]
            
            for task_id in expired_tasks:
                del self.tasks[task_id]

        if expired_tasks:
            logger.info(f"清理了 {len(expired_tasks)} 个过期任务")


# 全局任务管理器实例
task_manager = TaskManager()
