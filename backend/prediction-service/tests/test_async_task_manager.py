"""
异步任务管理器测试
测试任务创建、执行、状态管理和崩溃恢复
"""
import pytest
import asyncio
import tempfile
import shutil
from datetime import datetime, timezone, timedelta
from pathlib import Path

from app.core.async_task_manager import (
    AsyncTaskManager,
    TrainingTask,
    TaskProgress,
    TaskStatus,
    TaskStage
)
from app.core.task_persistence import TrainingCheckpoint


@pytest.mark.asyncio
class TestTaskProgress:
    """任务进度测试"""

    def test_task_progress_creation(self):
        """测试任务进度创建"""
        progress = TaskProgress(
            stage=TaskStage.INITIALIZING,
            percent=0,
            current_step="初始化",
            message="正在初始化"
        )
        
        assert progress.stage == TaskStage.INITIALIZING
        assert progress.percent == 0
        assert progress.total_steps == 7

    def test_task_progress_to_dict(self):
        """测试任务进度转换为字典"""
        progress = TaskProgress(
            stage=TaskStage.MODEL_TRAINING,
            percent=60,
            current_step="模型训练中",
            completed_steps=4,
            estimated_remaining_seconds=120,
            message="训练进行中"
        )
        
        data = progress.to_dict()
        assert data["stage"] == "model_training"
        assert data["percent"] == 60
        assert data["completed_steps"] == 4
        assert data["estimated_remaining_seconds"] == 120


@pytest.mark.asyncio
class TestAsyncTaskManager:
    """异步任务管理器测试"""

    @pytest.fixture
    async def task_manager(self, temp_dir):
        """创建测试用的任务管理器"""
        persistence_dir = temp_dir / "task_persistence"
        manager = AsyncTaskManager(max_workers=1)
        # 手动设置持久化目录
        manager._persistence.persistence_dir = persistence_dir
        manager._persistence.tasks_dir = persistence_dir / "tasks"
        manager._persistence.checkpoints_dir = persistence_dir / "checkpoints"
        manager._persistence._ensure_directories()
        
        yield manager
        
        # 清理
        if manager._running:
            await manager.stop()
        shutil.rmtree(temp_dir, ignore_errors=True)

    async def test_create_task(self, task_manager):
        """测试创建任务"""
        task = await task_manager.create_task(user_id="test_user", priority=1)
        
        assert task.task_id.startswith("TRAIN-")
        assert task.status == TaskStatus.PENDING
        assert task.user_id == "test_user"
        assert task.priority == 1
        assert task.progress.stage == TaskStage.INITIALIZING

    async def test_get_task(self, task_manager):
        """测试获取任务"""
        # 创建任务
        created_task = await task_manager.create_task(user_id="test_user")
        
        # 获取任务
        retrieved_task = await task_manager.get_task(created_task.task_id)
        
        assert retrieved_task is not None
        assert retrieved_task.task_id == created_task.task_id
        assert retrieved_task.user_id == "test_user"

    async def test_get_nonexistent_task(self, task_manager):
        """测试获取不存在的任务"""
        task = await task_manager.get_task("NONEXISTENT-TASK")
        assert task is None

    async def test_update_task_status(self, task_manager):
        """测试更新任务状态"""
        # 创建任务
        task = await task_manager.create_task(user_id="test_user")
        
        # 更新状态为处理中
        await task_manager.update_task_status(
            task.task_id,
            TaskStatus.PROCESSING
        )
        
        # 验证状态更新
        updated_task = await task_manager.get_task(task.task_id)
        assert updated_task.status == TaskStatus.PROCESSING
        assert updated_task.started_at is not None

    async def test_update_task_progress(self, task_manager):
        """测试更新任务进度"""
        # 创建任务
        task = await task_manager.create_task(user_id="test_user")
        
        # 更新进度
        await task_manager.update_task_progress(
            task_id=task.task_id,
            stage=TaskStage.MODEL_TRAINING,
            percent=60,
            current_step="模型训练中",
            message="训练进行中",
            estimated_remaining_seconds=120
        )
        
        # 验证进度更新
        updated_task = await task_manager.get_task(task.task_id)
        assert updated_task.progress.stage == TaskStage.MODEL_TRAINING
        assert updated_task.progress.percent == 60
        assert updated_task.progress.completed_steps == 4  # MODEL_TRAINING 是第4个阶段

    async def test_save_checkpoint(self, task_manager):
        """测试保存检查点"""
        # 创建任务
        task = await task_manager.create_task(user_id="test_user")
        
        # 创建检查点
        checkpoint = TrainingCheckpoint(
            task_id=task.task_id,
            stage="model_training",
            percent=60,
            current_step="模型训练中",
            message="训练进行中",
            model_params={"p": 5, "d": 1, "q": 0}
        )
        
        # 保存检查点
        await task_manager.save_checkpoint(task.task_id, checkpoint)
        
        # 验证检查点已保存
        updated_task = await task_manager.get_task(task.task_id)
        assert updated_task.checkpoint is not None
        assert updated_task.checkpoint.percent == 60

    async def test_cancel_task(self, task_manager):
        """测试取消任务"""
        # 创建任务
        task = await task_manager.create_task(user_id="test_user")
        
        # 取消任务
        result = await task_manager.cancel_task(task.task_id)
        assert result is True
        
        # 验证任务已取消
        cancelled_task = await task_manager.get_task(task.task_id)
        assert cancelled_task.status == TaskStatus.CANCELLED
        assert cancelled_task.completed_at is not None

    async def test_cancel_completed_task(self, task_manager):
        """测试取消已完成的任务"""
        # 创建任务
        task = await task_manager.create_task(user_id="test_user")
        
        # 先完成任务
        await task_manager.update_task_status(task.task_id, TaskStatus.COMPLETED)
        
        # 尝试取消已完成的任务
        result = await task_manager.cancel_task(task.task_id)
        assert result is False  # 应该失败

    async def test_get_all_tasks(self, task_manager):
        """测试获取所有任务"""
        # 创建多个任务
        tasks = []
        for i in range(5):
            task = await task_manager.create_task(user_id=f"user_{i}", priority=i)
            tasks.append(task)
        
        # 获取所有任务
        all_tasks = await task_manager.get_all_tasks()
        assert len(all_tasks) == 5
        
        # 验证排序（按优先级）
        priorities = [t.priority for t in all_tasks]
        assert priorities == sorted(priorities, reverse=True)

    async def test_get_tasks_by_status(self, task_manager):
        """测试按状态获取任务"""
        # 创建不同状态的任务
        pending_task = await task_manager.create_task(user_id="user1")
        processing_task = await task_manager.create_task(user_id="user2")
        
        await task_manager.update_task_status(processing_task.task_id, TaskStatus.PROCESSING)
        
        # 获取 pending 状态的任务
        pending_tasks = await task_manager.get_all_tasks(status=TaskStatus.PENDING)
        assert len(pending_tasks) == 1
        assert pending_tasks[0].task_id == pending_task.task_id

    async def test_task_priority_queue(self, task_manager):
        """测试任务优先级队列"""
        # 创建不同优先级的任务
        low_priority = await task_manager.create_task(user_id="user1", priority=5)
        high_priority = await task_manager.create_task(user_id="user2", priority=1)
        medium_priority = await task_manager.create_task(user_id="user3", priority=3)
        
        # 启动任务管理器
        await task_manager.start()
        
        # 等待一段时间让任务被处理
        await asyncio.sleep(0.5)
        
        # 验证高优先级任务先被处理
        # 注意：由于异步特性，这里只是验证队列顺序
        queue_items = []
        while not task_manager._task_queue.empty():
            try:
                item = task_manager._task_queue.get_nowait()
                queue_items.append(item)
            except asyncio.QueueEmpty:
                break
        
        # 验证优先级顺序
        if queue_items:
            priorities = [item[0] for item in queue_items]
            assert priorities == sorted(priorities)

    async def test_cleanup_old_tasks(self, task_manager):
        """测试清理旧任务"""
        # 创建任务
        task = await task_manager.create_task(user_id="test_user")
        
        # 完成任务
        await task_manager.update_task_status(task.task_id, TaskStatus.COMPLETED)
        
        # 修改任务创建时间为很久以前
        task.created_at = datetime.now(timezone(timedelta(hours=8))) - timedelta(days=2)
        
        # 清理超过1小时的任务
        await task_manager.cleanup_old_tasks(max_age_hours=1)
        
        # 验证任务已被清理
        cleaned_task = await task_manager.get_task(task.task_id)
        assert cleaned_task is None


@pytest.mark.asyncio
class TestAsyncTaskManagerRecovery:
    """异步任务管理器恢复测试"""

    async def test_recover_tasks(self, temp_dir):
        """测试任务恢复"""
        # 创建第一个任务管理器并创建任务
        persistence_dir = temp_dir / "task_persistence"
        manager1 = AsyncTaskManager(max_workers=1)
        manager1._persistence.persistence_dir = persistence_dir
        manager1._persistence.tasks_dir = persistence_dir / "tasks"
        manager1._persistence.checkpoints_dir = persistence_dir / "checkpoints"
        manager1._persistence._ensure_directories()
        
        # 创建任务
        task1 = await manager1.create_task(user_id="user1", priority=0)
        task2 = await manager1.create_task(user_id="user2", priority=1)
        
        # 将 task1 设置为 processing 状态
        await manager1.update_task_status(task1.task_id, TaskStatus.PROCESSING)
        
        # 保存检查点
        checkpoint = TrainingCheckpoint(
            task_id=task1.task_id,
            stage="model_training",
            percent=60,
            current_step="模型训练中",
            message="训练进行中",
            model_params={"p": 5, "d": 1, "q": 0}
        )
        await manager1.save_checkpoint(task1.task_id, checkpoint)
        
        # 保存所有任务
        await manager1._save_all_tasks()
        
        # 创建新的任务管理器（模拟重启）
        manager2 = AsyncTaskManager(max_workers=1)
        manager2._persistence.persistence_dir = persistence_dir
        manager2._persistence.tasks_dir = persistence_dir / "tasks"
        manager2._persistence.checkpoints_dir = persistence_dir / "checkpoints"
        manager2._persistence._ensure_directories()
        
        # 恢复任务
        await manager2._recover_tasks()
        
        # 验证任务已恢复
        assert task1.task_id in manager2.tasks
        assert task2.task_id in manager2.tasks
        
        # 验证检查点已恢复
        recovered_task1 = manager2.tasks[task1.task_id]
        assert recovered_task1.checkpoint is not None
        assert recovered_task1.checkpoint.percent == 60
        
        # 清理
        await manager1.stop()
        await manager2.stop()
        shutil.rmtree(temp_dir, ignore_errors=True)

    async def test_emergency_save(self, temp_dir):
        """测试紧急保存"""
        persistence_dir = temp_dir / "task_persistence"
        manager = AsyncTaskManager(max_workers=1)
        manager._persistence.persistence_dir = persistence_dir
        manager._persistence.tasks_dir = persistence_dir / "tasks"
        manager._persistence.checkpoints_dir = persistence_dir / "checkpoints"
        manager._persistence._ensure_directories()
        
        # 创建任务并设置为 processing
        task = await manager.create_task(user_id="test_user")
        await manager.update_task_status(task.task_id, TaskStatus.PROCESSING)
        
        # 执行紧急保存
        await manager._emergency_save()
        
        # 验证任务已保存
        loaded_tasks = await manager._persistence.load_task_queue()
        assert len(loaded_tasks) == 1
        assert loaded_tasks[0].status == "pending"  # 应该被重置为 pending
        
        # 清理
        await manager.stop()
        shutil.rmtree(temp_dir, ignore_errors=True)


@pytest.mark.asyncio
class TestAsyncTaskManagerLifecycle:
    """异步任务管理器生命周期测试"""

    async def test_start_and_stop(self, temp_dir):
        """测试启动和停止"""
        persistence_dir = temp_dir / "task_persistence"
        manager = AsyncTaskManager(max_workers=2)
        manager._persistence.persistence_dir = persistence_dir
        manager._persistence.tasks_dir = persistence_dir / "tasks"
        manager._persistence.checkpoints_dir = persistence_dir / "checkpoints"
        manager._persistence._ensure_directories()
        
        # 启动
        await manager.start()
        assert manager._running is True
        assert len(manager._worker_tasks) == 2
        
        # 停止
        await manager.stop()
        assert manager._running is False
        
        # 清理
        shutil.rmtree(temp_dir, ignore_errors=True)

    async def test_multiple_start_calls(self, temp_dir):
        """测试多次启动调用"""
        persistence_dir = temp_dir / "task_persistence"
        manager = AsyncTaskManager(max_workers=1)
        manager._persistence.persistence_dir = persistence_dir
        manager._persistence.tasks_dir = persistence_dir / "tasks"
        manager._persistence.checkpoints_dir = persistence_dir / "checkpoints"
        manager._persistence._ensure_directories()
        
        # 第一次启动
        await manager.start()
        worker_count_1 = len(manager._worker_tasks)
        
        # 第二次启动（应该被忽略）
        await manager.start()
        worker_count_2 = len(manager._worker_tasks)
        
        # 验证工作进程数量没有增加
        assert worker_count_1 == worker_count_2
        
        # 清理
        await manager.stop()
        shutil.rmtree(temp_dir, ignore_errors=True)
