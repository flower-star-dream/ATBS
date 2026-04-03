"""
任务状态持久化模块
实现任务队列和训练进度的持久化存储与恢复
支持原子性保存和崩溃恢复
"""
import os
import json
import pickle
import logging
import asyncio
import shutil
import tempfile
import threading
from datetime import datetime, timezone, timedelta
from typing import Dict, Optional, List, Any, Tuple
from dataclasses import dataclass, field, asdict
from pathlib import Path
import aiofiles

# 跨平台文件锁支持
try:
    import fcntl
    HAS_FCNTL = True
except ImportError:
    HAS_FCNTL = False
    # Windows 上使用 threading.Lock 作为替代
    _file_locks: Dict[str, threading.Lock] = {}

logger = logging.getLogger(__name__)


@dataclass
class TrainingCheckpoint:
    """训练检查点数据"""
    task_id: str
    stage: str
    percent: int
    current_step: str
    message: str
    
    # 模型训练状态
    model_params: Optional[Dict[str, Any]] = None  # 当前模型参数 (p, d, q)
    fitted_model_state: Optional[bytes] = None     # 序列化的模型状态
    training_history: Optional[Dict[str, Any]] = None  # 训练历史
    
    # 数据状态
    data_processor_state: Optional[Dict[str, Any]] = None  # 数据处理器状态
    current_data_index: int = 0  # 当前处理到的数据索引
    
    # 网格搜索状态
    grid_search_progress: Optional[Dict[str, Any]] = None  # 网格搜索进度
    evaluated_params: List[Tuple[int, int, int]] = field(default_factory=list)  # 已评估的参数
    best_params_so_far: Optional[Tuple[int, int, int]] = None  # 当前最优参数
    best_score_so_far: float = float('inf')
    
    # 损失函数值历史
    loss_history: List[Dict[str, float]] = field(default_factory=list)
    
    # 时间戳
    created_at: str = field(default_factory=lambda: datetime.now(timezone(timedelta(hours=8))).isoformat())
    updated_at: str = field(default_factory=lambda: datetime.now(timezone(timedelta(hours=8))).isoformat())
    
    def to_dict(self) -> Dict[str, Any]:
        """转换为字典"""
        return {
            'task_id': self.task_id,
            'stage': self.stage,
            'percent': self.percent,
            'current_step': self.current_step,
            'message': self.message,
            'model_params': self.model_params,
            'training_history': self.training_history,
            'data_processor_state': self.data_processor_state,
            'current_data_index': self.current_data_index,
            'grid_search_progress': self.grid_search_progress,
            'evaluated_params': self.evaluated_params,
            'best_params_so_far': self.best_params_so_far,
            'best_score_so_far': self.best_score_so_far,
            'loss_history': self.loss_history,
            'created_at': self.created_at,
            'updated_at': self.updated_at
        }
    
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> 'TrainingCheckpoint':
        """从字典创建"""
        return cls(
            task_id=data['task_id'],
            stage=data['stage'],
            percent=data['percent'],
            current_step=data['current_step'],
            message=data['message'],
            model_params=data.get('model_params'),
            training_history=data.get('training_history'),
            data_processor_state=data.get('data_processor_state'),
            current_data_index=data.get('current_data_index', 0),
            grid_search_progress=data.get('grid_search_progress'),
            evaluated_params=data.get('evaluated_params', []),
            best_params_so_far=tuple(data['best_params_so_far']) if data.get('best_params_so_far') else None,
            best_score_so_far=data.get('best_score_so_far', float('inf')),
            loss_history=data.get('loss_history', []),
            created_at=data.get('created_at', datetime.now(timezone(timedelta(hours=8))).isoformat()),
            updated_at=data.get('updated_at', datetime.now(timezone(timedelta(hours=8))).isoformat())
        )


@dataclass
class PersistentTask:
    """可持久化的任务"""
    task_id: str
    status: str
    user_id: Optional[str]
    created_at: str
    priority: int = 0  # 任务优先级，数字越小优先级越高
    started_at: Optional[str] = None
    completed_at: Optional[str] = None
    result: Optional[Dict[str, Any]] = None
    error_info: Optional[Dict[str, Any]] = None

    # 进度信息
    progress_stage: str = "initializing"
    progress_percent: int = 0
    progress_message: str = ""
    current_step: str = ""
    completed_steps: int = 0
    estimated_remaining_seconds: Optional[int] = None

    # 检查点信息
    has_checkpoint: bool = False
    checkpoint_path: Optional[str] = None
    last_checkpoint_time: Optional[str] = None
    
    def to_dict(self) -> Dict[str, Any]:
        """转换为字典"""
        return {
            'task_id': self.task_id,
            'status': self.status,
            'user_id': self.user_id,
            'priority': self.priority,
            'created_at': self.created_at,
            'started_at': self.started_at,
            'completed_at': self.completed_at,
            'result': self.result,
            'error_info': self.error_info,
            'progress_stage': self.progress_stage,
            'progress_percent': self.progress_percent,
            'progress_message': self.progress_message,
            'current_step': self.current_step,
            'completed_steps': self.completed_steps,
            'estimated_remaining_seconds': self.estimated_remaining_seconds,
            'has_checkpoint': self.has_checkpoint,
            'checkpoint_path': self.checkpoint_path,
            'last_checkpoint_time': self.last_checkpoint_time
        }
    
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> 'PersistentTask':
        """从字典创建"""
        return cls(
            task_id=data['task_id'],
            status=data['status'],
            user_id=data.get('user_id'),
            priority=data.get('priority', 0),
            created_at=data['created_at'],
            started_at=data.get('started_at'),
            completed_at=data.get('completed_at'),
            result=data.get('result'),
            error_info=data.get('error_info'),
            progress_stage=data.get('progress_stage', 'initializing'),
            progress_percent=data.get('progress_percent', 0),
            progress_message=data.get('progress_message', ''),
            current_step=data.get('current_step', ''),
            completed_steps=data.get('completed_steps', 0),
            estimated_remaining_seconds=data.get('estimated_remaining_seconds'),
            has_checkpoint=data.get('has_checkpoint', False),
            checkpoint_path=data.get('checkpoint_path'),
            last_checkpoint_time=data.get('last_checkpoint_time')
        )


class TaskPersistenceManager:
    """
    任务持久化管理器
    负责任务队列和检查点的持久化存储
    """
    
    def __init__(self, persistence_dir: Optional[str] = None):
        """
        初始化持久化管理器
        
        Args:
            persistence_dir: 持久化数据存储目录
        """
        if persistence_dir is None:
            persistence_dir = os.path.join(
                os.path.dirname(os.path.dirname(os.path.dirname(__file__))),
                'data', 'task_persistence'
            )
        
        self.persistence_dir = Path(persistence_dir)
        self.tasks_dir = self.persistence_dir / 'tasks'
        self.checkpoints_dir = self.persistence_dir / 'checkpoints'
        self.queue_file = self.persistence_dir / 'task_queue.json'
        self.lock_file = self.persistence_dir / '.lock'
        
        # 确保目录存在
        self._ensure_directories()
        
        # 异步锁
        self._async_lock = asyncio.Lock()
        
        logger.info(f"任务持久化管理器初始化完成，存储目录: {self.persistence_dir}")
    
    def _ensure_directories(self):
        """确保必要的目录存在"""
        self.persistence_dir.mkdir(parents=True, exist_ok=True)
        self.tasks_dir.mkdir(exist_ok=True)
        self.checkpoints_dir.mkdir(exist_ok=True)
    
    def _acquire_file_lock(self):
        """获取文件锁（用于进程间同步）"""
        if HAS_FCNTL:
            # Unix/Linux 系统使用 fcntl
            lock_path = self.lock_file
            lock_fd = open(lock_path, 'w')
            fcntl.flock(lock_fd.fileno(), fcntl.LOCK_EX)
            return lock_fd
        else:
            # Windows 系统使用 threading.Lock
            lock_path = str(self.lock_file)
            if lock_path not in _file_locks:
                _file_locks[lock_path] = threading.Lock()
            _file_locks[lock_path].acquire()
            return lock_path
    
    def _release_file_lock(self, lock_handle):
        """释放文件锁"""
        if HAS_FCNTL:
            # Unix/Linux 系统
            fcntl.flock(lock_handle.fileno(), fcntl.LOCK_UN)
            lock_handle.close()
        else:
            # Windows 系统
            if lock_handle in _file_locks:
                _file_locks[lock_handle].release()
    
    async def save_task_queue(self, tasks: List[PersistentTask]) -> bool:
        """
        原子性保存任务队列
        
        Args:
            tasks: 任务列表
            
        Returns:
            是否保存成功
        """
        async with self._async_lock:
            try:
                # 获取文件锁
                lock_fd = self._acquire_file_lock()
                
                try:
                    # 准备数据
                    data = {
                        'version': '1.0',
                        'saved_at': datetime.now(timezone(timedelta(hours=8))).isoformat(),
                        'tasks': [task.to_dict() for task in tasks]
                    }
                    
                    # 写入临时文件
                    temp_file = self.queue_file.with_suffix('.tmp')
                    async with aiofiles.open(temp_file, 'w', encoding='utf-8') as f:
                        await f.write(json.dumps(data, indent=2, ensure_ascii=False))
                    
                    # 原子性重命名
                    temp_file.replace(self.queue_file)
                    
                    logger.info(f"任务队列已保存，共 {len(tasks)} 个任务")
                    return True
                    
                finally:
                    self._release_file_lock(lock_fd)
                    
            except Exception as e:
                logger.error(f"保存任务队列失败: {e}", exc_info=True)
                return False
    
    async def load_task_queue(self) -> List[PersistentTask]:
        """
        加载任务队列
        
        Returns:
            任务列表
        """
        async with self._async_lock:
            try:
                if not self.queue_file.exists():
                    logger.info("没有找到任务队列文件，返回空列表")
                    return []
                
                # 获取文件锁
                lock_fd = self._acquire_file_lock()
                
                try:
                    async with aiofiles.open(self.queue_file, 'r', encoding='utf-8') as f:
                        content = await f.read()
                    
                    data = json.loads(content)
                    tasks = [PersistentTask.from_dict(t) for t in data.get('tasks', [])]
                    
                    logger.info(f"任务队列已加载，共 {len(tasks)} 个任务")
                    return tasks
                    
                finally:
                    self._release_file_lock(lock_fd)
                    
            except Exception as e:
                logger.error(f"加载任务队列失败: {e}", exc_info=True)
                return []
    
    async def save_task(self, task: PersistentTask) -> bool:
        """
        保存单个任务
        
        Args:
            task: 任务对象
            
        Returns:
            是否保存成功
        """
        async with self._async_lock:
            try:
                task_file = self.tasks_dir / f"{task.task_id}.json"
                
                async with aiofiles.open(task_file, 'w', encoding='utf-8') as f:
                    await f.write(json.dumps(task.to_dict(), indent=2, ensure_ascii=False))
                
                return True
                
            except Exception as e:
                logger.error(f"保存任务 {task.task_id} 失败: {e}")
                return False
    
    async def load_task(self, task_id: str) -> Optional[PersistentTask]:
        """
        加载单个任务
        
        Args:
            task_id: 任务ID
            
        Returns:
            任务对象，不存在返回None
        """
        async with self._async_lock:
            try:
                task_file = self.tasks_dir / f"{task_id}.json"
                
                if not task_file.exists():
                    return None
                
                async with aiofiles.open(task_file, 'r', encoding='utf-8') as f:
                    content = await f.read()
                
                data = json.loads(content)
                return PersistentTask.from_dict(data)
                
            except Exception as e:
                logger.error(f"加载任务 {task_id} 失败: {e}")
                return None
    
    async def delete_task(self, task_id: str) -> bool:
        """
        删除任务文件
        
        Args:
            task_id: 任务ID
            
        Returns:
            是否删除成功
        """
        async with self._async_lock:
            try:
                task_file = self.tasks_dir / f"{task_id}.json"
                checkpoint_file = self.checkpoints_dir / f"{task_id}.pkl"
                
                if task_file.exists():
                    task_file.unlink()
                
                if checkpoint_file.exists():
                    checkpoint_file.unlink()
                
                return True
                
            except Exception as e:
                logger.error(f"删除任务 {task_id} 失败: {e}")
                return False
    
    async def save_checkpoint(self, task_id: str, checkpoint: TrainingCheckpoint) -> bool:
        """
        保存训练检查点（使用pickle以支持复杂对象）
        
        Args:
            task_id: 任务ID
            checkpoint: 检查点数据
            
        Returns:
            是否保存成功
        """
        async with self._async_lock:
            try:
                checkpoint_file = self.checkpoints_dir / f"{task_id}.pkl"
                temp_file = checkpoint_file.with_suffix('.tmp')
                
                # 更新检查点时间
                checkpoint.updated_at = datetime.now(timezone(timedelta(hours=8))).isoformat()
                
                # 使用pickle保存
                with open(temp_file, 'wb') as f:
                    pickle.dump(checkpoint, f)
                
                # 原子性重命名
                temp_file.replace(checkpoint_file)
                
                logger.debug(f"任务 {task_id} 的检查点已保存")
                return True
                
            except Exception as e:
                logger.error(f"保存检查点 {task_id} 失败: {e}", exc_info=True)
                return False
    
    async def load_checkpoint(self, task_id: str) -> Optional[TrainingCheckpoint]:
        """
        加载训练检查点
        
        Args:
            task_id: 任务ID
            
        Returns:
            检查点数据，不存在返回None
        """
        async with self._async_lock:
            try:
                checkpoint_file = self.checkpoints_dir / f"{task_id}.pkl"
                
                if not checkpoint_file.exists():
                    return None
                
                with open(checkpoint_file, 'rb') as f:
                    checkpoint = pickle.load(f)
                
                return checkpoint
                
            except Exception as e:
                logger.error(f"加载检查点 {task_id} 失败: {e}")
                return None
    
    async def cleanup_old_checkpoints(self, max_age_hours: int = 24) -> int:
        """
        清理旧的检查点文件
        
        Args:
            max_age_hours: 最大保留时间（小时）
            
        Returns:
            清理的文件数量
        """
        async with self._async_lock:
            try:
                now = datetime.now(timezone(timedelta(hours=8)))
                cutoff_time = now - timedelta(hours=max_age_hours)
                
                cleaned_count = 0
                
                for checkpoint_file in self.checkpoints_dir.glob('*.pkl'):
                    try:
                        # 获取文件修改时间
                        mtime = datetime.fromtimestamp(
                            checkpoint_file.stat().st_mtime,
                            timezone(timedelta(hours=8))
                        )
                        
                        if mtime < cutoff_time:
                            checkpoint_file.unlink()
                            cleaned_count += 1
                            
                    except Exception as e:
                        logger.warning(f"清理检查点文件失败 {checkpoint_file}: {e}")
                
                if cleaned_count > 0:
                    logger.info(f"清理了 {cleaned_count} 个过期检查点")
                
                return cleaned_count
                
            except Exception as e:
                logger.error(f"清理检查点失败: {e}")
                return 0
    
    async def get_recoverable_tasks(self) -> List[PersistentTask]:
        """
        获取可恢复的任务列表
        包括：pending 和 processing 状态的任务
        
        Returns:
            可恢复的任务列表
        """
        tasks = await self.load_task_queue()
        
        # 筛选可恢复的任务
        recoverable = [
            task for task in tasks
            if task.status in ['pending', 'processing']
        ]
        
        # 按优先级和创建时间排序
        recoverable.sort(key=lambda t: (t.priority, t.created_at))
        
        logger.info(f"发现 {len(recoverable)} 个可恢复任务")
        return recoverable
    
    async def create_recovery_report(self) -> Dict[str, Any]:
        """
        创建恢复报告
        
        Returns:
            恢复报告
        """
        tasks = await self.load_task_queue()
        
        status_count = {}
        for task in tasks:
            status_count[task.status] = status_count.get(task.status, 0) + 1
        
        recoverable = await self.get_recoverable_tasks()
        
        return {
            'total_tasks': len(tasks),
            'status_distribution': status_count,
            'recoverable_tasks': len(recoverable),
            'recoverable_task_ids': [t.task_id for t in recoverable],
            'persistence_dir': str(self.persistence_dir),
            'report_time': datetime.now(timezone(timedelta(hours=8))).isoformat()
        }


# 全局持久化管理器实例
task_persistence = TaskPersistenceManager()
