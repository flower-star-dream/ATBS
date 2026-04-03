"""
异步任务管理器（增强版）
支持任务持久化、检查点、崩溃恢复
"""
import uuid
import asyncio
import logging
from datetime import datetime, timezone, timedelta
from typing import Dict, Optional, Any, Callable, List
from enum import Enum
from dataclasses import dataclass, field
import signal
import sys

from app.core.task_persistence import (
    TaskPersistenceManager, PersistentTask, TrainingCheckpoint,
    task_persistence
)

logger = logging.getLogger(__name__)


class TaskStatus(str, Enum):
    """任务状态枚举"""
    PENDING = "pending"           # 待处理
    PROCESSING = "processing"     # 处理中
    COMPLETED = "completed"       # 已完成
    FAILED = "failed"             # 失败
    CANCELLED = "cancelled"       # 已取消
    RECOVERING = "recovering"     # 恢复中


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
    priority: int = 0  # 任务优先级
    checkpoint: Optional[TrainingCheckpoint] = None  # 检查点数据

    def to_persistent_task(self) -> PersistentTask:
        """转换为可持久化任务"""
        return PersistentTask(
            task_id=self.task_id,
            status=self.status.value,
            user_id=self.user_id,
            priority=self.priority,
            created_at=self.created_at.isoformat(),
            started_at=self.started_at.isoformat() if self.started_at else None,
            completed_at=self.completed_at.isoformat() if self.completed_at else None,
            result=self.result,
            error_info=self.error_info,
            progress_stage=self.progress.stage.value,
            progress_percent=self.progress.percent,
            progress_message=self.progress.message,
            current_step=self.progress.current_step,
            completed_steps=self.progress.completed_steps,
            estimated_remaining_seconds=self.progress.estimated_remaining_seconds,
            has_checkpoint=self.checkpoint is not None,
            last_checkpoint_time=self.checkpoint.updated_at if self.checkpoint else None
        )


class AsyncTaskManager:
    """
    异步任务管理器（增强版）
    支持任务持久化、检查点保存、崩溃恢复
    """

    def __init__(self, max_workers: int = 2):
        self.tasks: Dict[str, TrainingTask] = {}
        self._task_queue: asyncio.PriorityQueue = asyncio.PriorityQueue()
        self._lock = asyncio.Lock()
        self._beijing_tz = timezone(timedelta(hours=8))
        self._max_workers = max_workers
        self._running = False
        self._worker_tasks: List[asyncio.Task] = []
        self._persistence = task_persistence
        self._progress_callbacks: Dict[str, Callable] = {}
        self._task_executor: Optional[Callable] = None  # 任务执行器回调

        # 设置信号处理
        self._setup_signal_handlers()

    def set_task_executor(self, executor: Callable):
        """
        设置任务执行器

        Args:
            executor: 任务执行函数，接收 task_id 作为参数
        """
        self._task_executor = executor
        logger.info("任务执行器已设置")

    def _setup_signal_handlers(self):
        """设置信号处理器，用于优雅关闭和崩溃恢复"""
        def signal_handler(signum, frame):
            logger.warning(f"接收到信号 {signum}，准备保存任务状态...")
            try:
                # 直接使用同步方法保存，避免事件循环问题
                self._emergency_save_sync()
            except Exception as e:
                logger.error(f"紧急保存失败: {e}")
            finally:
                sys.exit(1)

        # 注册信号处理器
        signal.signal(signal.SIGTERM, signal_handler)
        signal.signal(signal.SIGINT, signal_handler)



    async def _emergency_save(self):
        """紧急保存任务状态（异步版本）"""
        logger.info("执行紧急保存...")
        try:
            # 将所有任务标记为需要恢复
            for task in self.tasks.values():
                if task.status == TaskStatus.PROCESSING:
                    task.status = TaskStatus.PENDING  # 重置为待处理状态
                    # 保存检查点
                    if task.checkpoint:
                        await self._persistence.save_checkpoint(task.task_id, task.checkpoint)

            # 保存任务队列
            persistent_tasks = [t.to_persistent_task() for t in self.tasks.values()]
            await self._persistence.save_task_queue(persistent_tasks)

            logger.info("紧急保存完成")
        except Exception as e:
            logger.error(f"紧急保存失败: {e}")

    def _emergency_save_sync(self):
        """紧急保存任务状态（同步版本，用于信号处理）"""
        logger.info("执行同步紧急保存...")
        try:
            # 将所有任务标记为需要恢复
            for task in self.tasks.values():
                if task.status == TaskStatus.PROCESSING:
                    task.status = TaskStatus.PENDING

            # 同步保存任务队列
            persistent_tasks = [t.to_persistent_task() for t in self.tasks.values()]
            self._persistence.save_task_queue_sync(persistent_tasks)

            logger.info("同步紧急保存完成")
        except Exception as e:
            logger.error(f"同步紧急保存失败: {e}")

    async def start(self):
        """启动任务管理器"""
        if self._running:
            return

        logger.info("=" * 60)
        logger.info("启动异步任务管理器")
        logger.info("=" * 60)

        self._running = True

        # 尝试恢复任务
        await self._recover_tasks()

        # 检查任务执行器是否设置
        if self._task_executor:
            logger.info("任务执行器已设置")
        else:
            logger.warning("任务执行器未设置！恢复的任务将无法执行")

        # 启动工作进程
        for i in range(self._max_workers):
            worker = asyncio.create_task(self._worker_loop(i))
            self._worker_tasks.append(worker)

        logger.info(f"任务管理器已启动，工作进程数: {self._max_workers}")

    async def stop(self):
        """停止任务管理器"""
        logger.info("停止任务管理器...")
        self._running = False
        
        # 保存所有任务状态
        await self._save_all_tasks()
        
        # 取消所有工作进程
        for worker in self._worker_tasks:
            worker.cancel()
        
        # 等待工作进程结束
        if self._worker_tasks:
            await asyncio.gather(*self._worker_tasks, return_exceptions=True)
        
        logger.info("任务管理器已停止")

    async def _recover_tasks(self):
        """恢复之前的任务"""
        try:
            recoverable = await self._persistence.get_recoverable_tasks()
            
            if not recoverable:
                logger.info("没有需要恢复的任务")
                return
            
            logger.info(f"开始恢复 {len(recoverable)} 个任务...")
            
            for persistent_task in recoverable:
                try:
                    # 加载检查点
                    checkpoint = await self._persistence.load_checkpoint(persistent_task.task_id)
                    
                    # 创建任务对象
                    task = TrainingTask(
                        task_id=persistent_task.task_id,
                        status=TaskStatus.RECOVERING,
                        progress=TaskProgress(
                            stage=TaskStage(persistent_task.progress_stage),
                            percent=persistent_task.progress_percent,
                            current_step=persistent_task.current_step,
                            completed_steps=persistent_task.completed_steps,
                            estimated_remaining_seconds=persistent_task.estimated_remaining_seconds,
                            message=f"恢复中: {persistent_task.progress_message}"
                        ),
                        created_at=datetime.fromisoformat(persistent_task.created_at),
                        started_at=datetime.fromisoformat(persistent_task.started_at) if persistent_task.started_at else None,
                        user_id=persistent_task.user_id,
                        priority=persistent_task.priority,
                        checkpoint=checkpoint
                    )
                    
                    # 添加到任务列表
                    self.tasks[task.task_id] = task
                    
                    # 添加到队列（重置为pending状态）
                    task.status = TaskStatus.PENDING
                    await self._task_queue.put((task.priority, task.created_at.timestamp(), task.task_id))
                    
                    logger.info(f"任务 {task.task_id} 已恢复，进度: {task.progress.percent}%")
                    
                except Exception as e:
                    logger.error(f"恢复任务 {persistent_task.task_id} 失败: {e}")
            
            logger.info("任务恢复完成")
            
        except Exception as e:
            logger.error(f"任务恢复过程失败: {e}")

    async def _save_all_tasks(self):
        """保存所有任务状态"""
        try:
            persistent_tasks = [t.to_persistent_task() for t in self.tasks.values()]
            await self._persistence.save_task_queue(persistent_tasks)
            logger.info(f"已保存 {len(persistent_tasks)} 个任务状态")
        except Exception as e:
            logger.error(f"保存任务状态失败: {e}")

    def _generate_task_id(self) -> str:
        """生成唯一的任务ID"""
        timestamp = datetime.now(self._beijing_tz).strftime("%Y%m%d%H%M%S")
        short_uuid = uuid.uuid4().hex[:8].upper()
        return f"TRAIN-{timestamp}-{short_uuid}"

    async def create_task(self, user_id: Optional[str] = None, priority: int = 0) -> TrainingTask:
        """
        创建新的训练任务

        Args:
            user_id: 用户ID
            priority: 任务优先级（数字越小优先级越高）

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
            user_id=user_id,
            priority=priority
        )

        async with self._lock:
            self.tasks[task_id] = task
            # 保存任务
            await self._persistence.save_task(task.to_persistent_task())
            # 添加到队列
            await self._task_queue.put((priority, now.timestamp(), task_id))

        logger.info(f"创建训练任务: {task_id}, 优先级: {priority}")
        return task

    async def get_task(self, task_id: str) -> Optional[TrainingTask]:
        """获取任务信息（使用细粒度锁，快速返回）"""
        # 使用非阻塞方式快速获取任务
        try:
            async with asyncio.timeout(0.1):  # 100ms超时
                async with self._lock:
                    return self.tasks.get(task_id)
        except asyncio.TimeoutError:
            # 如果获取锁超时，尝试无锁读取（可能读到稍旧的数据）
            logger.warning(f"获取任务 {task_id} 锁超时，尝试直接读取")
            return self.tasks.get(task_id)

    async def update_task_status(
        self,
        task_id: str,
        status: TaskStatus,
        result: Optional[Dict] = None,
        error_info: Optional[Dict] = None
    ):
        """更新任务状态"""
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

            # 持久化保存
            await self._persistence.save_task(task.to_persistent_task())

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
        """更新任务进度"""
        task = None
        need_persist = False

        # 快速获取任务引用并更新内存状态
        async with self._lock:
            task = self.tasks.get(task_id)
            if not task:
                return

            progress = task.progress

            if stage:
                progress.stage = stage
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

            # 定期持久化（每10%或每阶段）
            if percent and (percent % 10 == 0 or stage):
                need_persist = True

        # 在锁外执行持久化，避免阻塞其他操作
        if need_persist and task:
            try:
                await self._persistence.save_task(task.to_persistent_task())
            except Exception as e:
                logger.error(f"持久化任务 {task_id} 失败: {e}")

        # 触发进度回调
        if task_id in self._progress_callbacks:
            try:
                callback = self._progress_callbacks[task_id]
                await callback(task_id, task.progress)
            except Exception as e:
                logger.error(f"进度回调执行失败: {e}")

    async def save_checkpoint(self, task_id: str, checkpoint: TrainingCheckpoint):
        """保存训练检查点"""
        async with self._lock:
            task = self.tasks.get(task_id)
            if not task:
                return
            
            task.checkpoint = checkpoint
            
            # 保存检查点
            await self._persistence.save_checkpoint(task_id, checkpoint)
            
            # 更新任务信息
            await self._persistence.save_task(task.to_persistent_task())
        
        logger.debug(f"任务 {task_id} 检查点已保存")

    async def _worker_loop(self, worker_id: int):
        """工作进程主循环"""
        logger.info(f"工作进程 {worker_id} 已启动")

        while self._running:
            try:
                logger.debug(f"工作进程 {worker_id}: 等待任务...")
                # 获取任务
                priority, created_at, task_id = await self._task_queue.get()
                logger.info(f"工作进程 {worker_id}: 获取到任务 {task_id}")

                task = await self.get_task(task_id)
                if not task:
                    logger.warning(f"工作进程 {worker_id}: 任务 {task_id} 不存在")
                    continue

                if task.status not in [TaskStatus.PENDING, TaskStatus.RECOVERING]:
                    logger.info(f"工作进程 {worker_id}: 任务 {task_id} 状态为 {task.status.value}，跳过执行")
                    continue

                logger.info(f"工作进程 {worker_id}: 开始执行任务 {task_id}，当前状态: {task.status.value}")

                # 更新状态为处理中
                await self.update_task_status(task_id, TaskStatus.PROCESSING)

                # 执行训练任务
                if self._task_executor:
                    try:
                        logger.info(f"工作进程 {worker_id}: 调用任务执行器执行任务 {task_id}")
                        await self._task_executor(task_id)
                        logger.info(f"工作进程 {worker_id}: 任务 {task_id} 执行完成")
                    except Exception as e:
                        logger.error(f"工作进程 {worker_id}: 执行任务 {task_id} 失败: {e}", exc_info=True)
                        # 更新任务状态为失败
                        await self.update_task_status(
                            task_id,
                            TaskStatus.FAILED,
                            error_info={
                                "error_type": type(e).__name__,
                                "error_message": str(e),
                                "timestamp": datetime.now(self._beijing_tz).isoformat()
                            }
                        )
                else:
                    logger.error(f"工作进程 {worker_id}: 未设置任务执行器，无法执行任务 {task_id}")

            except asyncio.CancelledError:
                logger.info(f"工作进程 {worker_id}: 收到取消信号")
                break
            except Exception as e:
                logger.error(f"工作进程 {worker_id} 异常: {e}", exc_info=True)

        logger.info(f"工作进程 {worker_id} 已停止")

    async def cancel_task(self, task_id: str) -> bool:
        """取消任务"""
        async with self._lock:
            task = self.tasks.get(task_id)
            if not task:
                return False

            if task.status in [TaskStatus.COMPLETED, TaskStatus.FAILED]:
                return False

            task.status = TaskStatus.CANCELLED
            task.completed_at = datetime.now(self._beijing_tz)
            
            # 持久化保存
            await self._persistence.save_task(task.to_persistent_task())

        logger.info(f"任务 {task_id} 已取消")
        return True

    async def get_all_tasks(
        self,
        status: Optional[TaskStatus] = None,
        limit: int = 100,
        offset: int = 0
    ) -> List[TrainingTask]:
        """获取任务列表"""
        async with self._lock:
            tasks = list(self.tasks.values())
            
            if status:
                tasks = [t for t in tasks if t.status == status]
            
            # 按优先级和创建时间排序
            tasks.sort(key=lambda x: (x.priority, x.created_at), reverse=True)
            
            return tasks[offset:offset + limit]

    async def cleanup_old_tasks(self, max_age_hours: int = 24):
        """清理旧任务"""
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
                await self._persistence.delete_task(task_id)

        if expired_tasks:
            logger.info(f"清理了 {len(expired_tasks)} 个过期任务")


# 全局异步任务管理器实例
async_task_manager = AsyncTaskManager()
