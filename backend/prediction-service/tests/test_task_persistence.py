"""
任务持久化模块测试
测试任务队列和检查点的保存、加载、恢复功能
"""
import pytest
import asyncio
import json
import tempfile
import shutil
from pathlib import Path
from datetime import datetime, timezone, timedelta

from app.core.task_persistence import (
    TaskPersistenceManager,
    PersistentTask,
    TrainingCheckpoint,
    task_persistence
)


class TestPersistentTask:
    """PersistentTask 数据类测试"""

    def test_persistent_task_creation(self):
        """测试 PersistentTask 创建"""
        task = PersistentTask(
            task_id="TRAIN-20240101120000-TEST001",
            status="pending",
            user_id="test_user",
            priority=1,
            created_at=datetime.now(timezone(timedelta(hours=8))).isoformat()
        )
        
        assert task.task_id == "TRAIN-20240101120000-TEST001"
        assert task.status == "pending"
        assert task.priority == 1
        assert task.user_id == "test_user"

    def test_persistent_task_to_dict(self):
        """测试 PersistentTask 转换为字典"""
        task = PersistentTask(
            task_id="TRAIN-20240101120000-TEST001",
            status="processing",
            user_id="test_user",
            priority=0,
            created_at="2024-01-01T12:00:00+08:00",
            progress_percent=50,
            progress_stage="model_training"
        )
        
        data = task.to_dict()
        assert data["task_id"] == "TRAIN-20240101120000-TEST001"
        assert data["status"] == "processing"
        assert data["progress_percent"] == 50
        assert data["progress_stage"] == "model_training"

    def test_persistent_task_from_dict(self):
        """测试从字典创建 PersistentTask"""
        data = {
            "task_id": "TRAIN-20240101120000-TEST001",
            "status": "completed",
            "user_id": "test_user",
            "priority": 2,
            "created_at": "2024-01-01T12:00:00+08:00",
            "progress_percent": 100,
            "has_checkpoint": True
        }
        
        task = PersistentTask.from_dict(data)
        assert task.task_id == "TRAIN-20240101120000-TEST001"
        assert task.status == "completed"
        assert task.priority == 2
        assert task.has_checkpoint is True


class TestTrainingCheckpoint:
    """TrainingCheckpoint 数据类测试"""

    def test_checkpoint_creation(self):
        """测试 TrainingCheckpoint 创建"""
        checkpoint = TrainingCheckpoint(
            task_id="TRAIN-20240101120000-TEST001",
            stage="model_training",
            percent=60,
            current_step="模型训练中",
            message="正在训练模型",
            model_params={"p": 5, "d": 1, "q": 0}
        )
        
        assert checkpoint.task_id == "TRAIN-20240101120000-TEST001"
        assert checkpoint.stage == "model_training"
        assert checkpoint.percent == 60
        assert checkpoint.model_params == {"p": 5, "d": 1, "q": 0}

    def test_checkpoint_to_dict(self):
        """测试 TrainingCheckpoint 转换为字典"""
        checkpoint = TrainingCheckpoint(
            task_id="TRAIN-20240101120000-TEST001",
            stage="parameter_optimization",
            percent=35,
            current_step="参数优化",
            message="寻找最优参数",
            model_params={"p": 3, "d": 1, "q": 2},
            training_history={"aic": 1000.0},
            grid_search_progress={"evaluated_count": 10, "total_count": 100}
        )
        
        data = checkpoint.to_dict()
        assert data["task_id"] == "TRAIN-20240101120000-TEST001"
        assert data["stage"] == "parameter_optimization"
        assert data["grid_search_progress"]["evaluated_count"] == 10

    def test_checkpoint_from_dict(self):
        """测试从字典创建 TrainingCheckpoint"""
        data = {
            "task_id": "TRAIN-20240101120000-TEST001",
            "stage": "model_validation",
            "percent": 85,
            "current_step": "模型验证",
            "message": "验证中",
            "model_params": {"p": 5, "d": 1, "q": 0},
            "training_history": {"aic": 1234.5, "bic": 1245.6},
            "evaluated_params": [[1, 0, 0], [2, 1, 0]],
            "best_params_so_far": [5, 1, 0],
            "best_score_so_far": 100.0,
            "loss_history": [{"mae": 10.5}, {"mae": 9.8}]
        }
        
        checkpoint = TrainingCheckpoint.from_dict(data)
        assert checkpoint.task_id == "TRAIN-20240101120000-TEST001"
        assert checkpoint.percent == 85
        assert checkpoint.best_params_so_far == (5, 1, 0)
        assert len(checkpoint.loss_history) == 2


@pytest.mark.asyncio
class TestTaskPersistenceManager:
    """TaskPersistenceManager 测试"""

    @pytest.fixture
    async def temp_persistence(self, temp_dir):
        """创建临时的持久化管理器"""
        persistence = TaskPersistenceManager(persistence_dir=str(temp_dir))
        yield persistence
        # 清理
        shutil.rmtree(temp_dir, ignore_errors=True)

    async def test_save_and_load_task_queue(self, temp_persistence):
        """测试保存和加载任务队列"""
        # 创建测试任务
        tasks = [
            PersistentTask(
                task_id=f"TRAIN-20240101120000-TEST{i:03d}",
                status="pending",
                user_id="test_user",
                priority=i,
                created_at=datetime.now(timezone(timedelta(hours=8))).isoformat()
            )
            for i in range(3)
        ]
        
        # 保存任务队列
        result = await temp_persistence.save_task_queue(tasks)
        assert result is True
        
        # 加载任务队列
        loaded_tasks = await temp_persistence.load_task_queue()
        assert len(loaded_tasks) == 3
        assert loaded_tasks[0].task_id == "TRAIN-20240101120000-TEST000"

    async def test_save_and_load_single_task(self, temp_persistence):
        """测试保存和加载单个任务"""
        task = PersistentTask(
            task_id="TRAIN-20240101120000-TEST001",
            status="processing",
            user_id="test_user",
            priority=0,
            created_at=datetime.now(timezone(timedelta(hours=8))).isoformat(),
            progress_percent=50
        )
        
        # 保存任务
        result = await temp_persistence.save_task(task)
        assert result is True
        
        # 加载任务
        loaded_task = await temp_persistence.load_task("TRAIN-20240101120000-TEST001")
        assert loaded_task is not None
        assert loaded_task.task_id == "TRAIN-20240101120000-TEST001"
        assert loaded_task.progress_percent == 50

    async def test_load_nonexistent_task(self, temp_persistence):
        """测试加载不存在的任务"""
        loaded_task = await temp_persistence.load_task("NONEXISTENT-TASK")
        assert loaded_task is None

    async def test_save_and_load_checkpoint(self, temp_persistence):
        """测试保存和加载检查点"""
        checkpoint = TrainingCheckpoint(
            task_id="TRAIN-20240101120000-TEST001",
            stage="model_training",
            percent=60,
            current_step="模型训练中",
            message="训练进行中",
            model_params={"p": 5, "d": 1, "q": 0},
            training_history={"aic": 1234.5}
        )
        
        # 保存检查点
        result = await temp_persistence.save_checkpoint(
            "TRAIN-20240101120000-TEST001",
            checkpoint
        )
        assert result is True
        
        # 加载检查点
        loaded_checkpoint = await temp_persistence.load_checkpoint(
            "TRAIN-20240101120000-TEST001"
        )
        assert loaded_checkpoint is not None
        assert loaded_checkpoint.task_id == "TRAIN-20240101120000-TEST001"
        assert loaded_checkpoint.percent == 60
        assert loaded_checkpoint.model_params == {"p": 5, "d": 1, "q": 0}

    async def test_delete_task(self, temp_persistence):
        """测试删除任务"""
        task = PersistentTask(
            task_id="TRAIN-20240101120000-TEST001",
            status="completed",
            user_id="test_user",
            priority=0,
            created_at=datetime.now(timezone(timedelta(hours=8))).isoformat()
        )
        
        # 保存任务
        await temp_persistence.save_task(task)
        
        # 删除任务
        result = await temp_persistence.delete_task("TRAIN-20240101120000-TEST001")
        assert result is True
        
        # 验证任务已删除
        loaded_task = await temp_persistence.load_task("TRAIN-20240101120000-TEST001")
        assert loaded_task is None

    async def test_get_recoverable_tasks(self, temp_persistence):
        """测试获取可恢复任务"""
        # 创建不同状态的任务
        tasks = [
            PersistentTask(
                task_id="TRAIN-20240101120000-PENDING",
                status="pending",
                user_id="test_user",
                priority=0,
                created_at=datetime.now(timezone(timedelta(hours=8))).isoformat()
            ),
            PersistentTask(
                task_id="TRAIN-20240101120000-PROCESSING",
                status="processing",
                user_id="test_user",
                priority=1,
                created_at=datetime.now(timezone(timedelta(hours=8))).isoformat()
            ),
            PersistentTask(
                task_id="TRAIN-20240101120000-COMPLETED",
                status="completed",
                user_id="test_user",
                priority=2,
                created_at=datetime.now(timezone(timedelta(hours=8))).isoformat()
            )
        ]
        
        # 保存任务队列
        await temp_persistence.save_task_queue(tasks)
        
        # 获取可恢复任务
        recoverable = await temp_persistence.get_recoverable_tasks()
        assert len(recoverable) == 2  # pending 和 processing
        
        task_ids = [t.task_id for t in recoverable]
        assert "TRAIN-20240101120000-PENDING" in task_ids
        assert "TRAIN-20240101120000-PROCESSING" in task_ids
        assert "TRAIN-20240101120000-COMPLETED" not in task_ids

    async def test_create_recovery_report(self, temp_persistence):
        """测试创建恢复报告"""
        # 创建测试任务
        tasks = [
            PersistentTask(
                task_id=f"TRAIN-20240101120000-TEST{i:03d}",
                status=status,
                user_id="test_user",
                priority=0,
                created_at=datetime.now(timezone(timedelta(hours=8))).isoformat()
            )
            for i, status in enumerate(["pending", "processing", "completed", "failed"])
        ]
        
        await temp_persistence.save_task_queue(tasks)
        
        # 创建恢复报告
        report = await temp_persistence.create_recovery_report()
        
        assert report["total_tasks"] == 4
        assert report["recoverable_tasks"] == 2  # pending 和 processing
        assert len(report["recoverable_task_ids"]) == 2
        assert "status_distribution" in report

    async def test_concurrent_access(self, temp_persistence):
        """测试并发访问"""
        task = PersistentTask(
            task_id="TRAIN-20240101120000-TEST001",
            status="pending",
            user_id="test_user",
            priority=0,
            created_at=datetime.now(timezone(timedelta(hours=8))).isoformat()
        )
        
        # 并发保存同一个任务
        async def save_task():
            return await temp_persistence.save_task(task)
        
        # 同时执行多个保存操作
        results = await asyncio.gather(*[save_task() for _ in range(5)])
        assert all(results)  # 所有操作都应该成功


@pytest.mark.asyncio
class TestTaskPersistenceIntegration:
    """任务持久化集成测试"""

    async def test_full_task_lifecycle(self, temp_dir):
        """测试完整的任务生命周期"""
        persistence = TaskPersistenceManager(persistence_dir=str(temp_dir))
        
        # 1. 创建任务
        task = PersistentTask(
            task_id="TRAIN-20240101120000-LIFECYCLE",
            status="pending",
            user_id="test_user",
            priority=0,
            created_at=datetime.now(timezone(timedelta(hours=8))).isoformat()
        )
        
        # 2. 保存任务
        await persistence.save_task(task)
        
        # 3. 更新任务状态
        task.status = "processing"
        task.progress_percent = 50
        task.progress_stage = "model_training"
        await persistence.save_task(task)
        
        # 4. 保存检查点
        checkpoint = TrainingCheckpoint(
            task_id="TRAIN-20240101120000-LIFECYCLE",
            stage="model_training",
            percent=50,
            current_step="模型训练中",
            message="训练进行中",
            model_params={"p": 5, "d": 1, "q": 0}
        )
        await persistence.save_checkpoint("TRAIN-20240101120000-LIFECYCLE", checkpoint)
        
        # 5. 完成任务
        task.status = "completed"
        task.progress_percent = 100
        await persistence.save_task(task)
        
        # 6. 验证最终状态
        final_task = await persistence.load_task("TRAIN-20240101120000-LIFECYCLE")
        assert final_task.status == "completed"
        assert final_task.progress_percent == 100
        
        final_checkpoint = await persistence.load_checkpoint("TRAIN-20240101120000-LIFECYCLE")
        assert final_checkpoint is not None
        assert final_checkpoint.percent == 50

    async def test_crash_recovery_simulation(self, temp_dir):
        """模拟崩溃恢复场景"""
        persistence = TaskPersistenceManager(persistence_dir=str(temp_dir))
        
        # 创建多个任务，模拟不同状态
        tasks = [
            PersistentTask(
                task_id="TRAIN-20240101120000-CRASH1",
                status="processing",  # 处理中，需要恢复
                user_id="test_user",
                priority=0,
                created_at=datetime.now(timezone(timedelta(hours=8))).isoformat(),
                progress_percent=60,
                progress_stage="model_training"
            ),
            PersistentTask(
                task_id="TRAIN-20240101120000-CRASH2",
                status="pending",  # 待处理，需要恢复
                user_id="test_user",
                priority=1,
                created_at=datetime.now(timezone(timedelta(hours=8))).isoformat()
            ),
            PersistentTask(
                task_id="TRAIN-20240101120000-CRASH3",
                status="completed",  # 已完成，不需要恢复
                user_id="test_user",
                priority=2,
                created_at=datetime.now(timezone(timedelta(hours=8))).isoformat()
            )
        ]
        
        # 为处理中的任务保存检查点
        checkpoint = TrainingCheckpoint(
            task_id="TRAIN-20240101120000-CRASH1",
            stage="model_training",
            percent=60,
            current_step="模型训练中",
            message="训练进行中",
            model_params={"p": 5, "d": 1, "q": 0}
        )
        await persistence.save_checkpoint("TRAIN-20240101120000-CRASH1", checkpoint)
        
        # 保存任务队列
        await persistence.save_task_queue(tasks)
        
        # 模拟崩溃后重新加载
        new_persistence = TaskPersistenceManager(persistence_dir=str(temp_dir))
        
        # 获取可恢复任务
        recoverable = await new_persistence.get_recoverable_tasks()
        
        # 验证只有 pending 和 processing 任务需要恢复
        assert len(recoverable) == 2
        task_ids = {t.task_id for t in recoverable}
        assert "TRAIN-20240101120000-CRASH1" in task_ids
        assert "TRAIN-20240101120000-CRASH2" in task_ids
        assert "TRAIN-20240101120000-CRASH3" not in task_ids
        
        # 验证可以加载检查点
        loaded_checkpoint = await new_persistence.load_checkpoint("TRAIN-20240101120000-CRASH1")
        assert loaded_checkpoint is not None
        assert loaded_checkpoint.percent == 60
