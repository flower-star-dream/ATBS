"""
模型自动重训服务
实现定时重训、数据获取、滚动存储等功能
"""
import os
import json
import asyncio
import logging
from datetime import datetime, timedelta
from pathlib import Path
from typing import Optional, Dict, Any, List
import httpx
import pandas as pd
import numpy as np

from app.core.config import settings
from app.core.nacos_client import NacosServiceRegistry
from app.services.prediction_service import prediction_service
from app.core.task_manager import task_manager, TaskStage

logger = logging.getLogger(__name__)


class DataRollingStorage:
    """
    数据滚动存储管理器
    实现数据的滚动窗口存储机制
    """

    def __init__(self, data_file: Path, max_rows: int):
        """
        初始化滚动存储管理器

        Args:
            data_file: 数据文件路径
            max_rows: 最大保留行数
        """
        self.data_file = data_file
        self.max_rows = max_rows

    def initialize_from_original(self, original_file: Path) -> bool:
        """
        从原始数据文件初始化滚动数据文件

        Args:
            original_file: 原始数据文件路径

        Returns:
            是否成功初始化
        """
        try:
            if not original_file.exists():
                logger.error(f"原始数据文件不存在: {original_file}")
                return False

            # 读取原始数据
            df = pd.read_csv(original_file)

            # 预处理：将月度数据转换为日度数据
            from utils.data_processor import DataProcessor
            processor = DataProcessor()
            processor.original_data = df.copy()

            # 转换列为正确的类型
            df['Month'] = pd.to_datetime(df['Month'])

            # 生成日度数据（不使用随机扰动，保持数据一致性）
            daily_data = processor.monthly_to_daily(
                interpolation_method='cubic',
                apply_effects=True,
                apply_perturbation=False,  # 初始化时不添加扰动
                noise_level=0.0,
                random_seed=42
            )

            # 按月份聚合回月度数据格式（用于存储）
            daily_data['YearMonth'] = daily_data['Date'].dt.to_period('M')
            monthly_aggregated = daily_data.groupby('YearMonth')['Passengers'].sum().reset_index()
            monthly_aggregated['YearMonth'] = monthly_aggregated['YearMonth'].dt.strftime('%Y-%m')
            monthly_aggregated.columns = ['Month', 'Passengers']

            # 确保不超过最大行数
            if len(monthly_aggregated) > self.max_rows:
                monthly_aggregated = monthly_aggregated.tail(self.max_rows)

            # 保存到滚动数据文件
            monthly_aggregated.to_csv(self.data_file, index=False)
            logger.info(f"已初始化滚动数据文件: {self.data_file}, 共 {len(monthly_aggregated)} 条记录")
            return True

        except Exception as e:
            logger.error(f"初始化滚动数据文件失败: {e}", exc_info=True)
            return False

    def append_daily_data(self, date: str, passengers: int) -> bool:
        """
        添加日度数据到滚动存储

        Args:
            date: 日期字符串 (YYYY-MM-DD)
            passengers: 客流量

        Returns:
            是否成功添加
        """
        try:
            # 读取现有数据
            if self.data_file.exists():
                df = pd.read_csv(self.data_file)
            else:
                df = pd.DataFrame(columns=['Month', 'Passengers'])

            # 将日度数据转换为月度格式（按月份聚合）
            date_obj = pd.to_datetime(date)
            month_key = date_obj.strftime('%Y-%m')

            # 检查是否已存在该月份
            if month_key in df['Month'].values:
                # 更新该月份的累计值
                df.loc[df['Month'] == month_key, 'Passengers'] += passengers
            else:
                # 添加新月份
                new_row = pd.DataFrame({'Month': [month_key], 'Passengers': [passengers]})
                df = pd.concat([df, new_row], ignore_index=True)

            # 滚动窗口：如果超过最大行数，删除最前面的数据
            while len(df) > self.max_rows:
                removed = df.iloc[0]
                df = df.iloc[1:].reset_index(drop=True)
                logger.debug(f"滚动删除旧数据: {removed['Month']}")

            # 保存数据
            df.to_csv(self.data_file, index=False)
            logger.info(f"已添加日度数据: {date}, 当前共 {len(df)} 条记录")
            return True

        except Exception as e:
            logger.error(f"添加日度数据失败: {e}", exc_info=True)
            return False

    def get_current_data(self) -> Optional[pd.DataFrame]:
        """
        获取当前数据

        Returns:
            数据DataFrame，文件不存在返回None
        """
        if not self.data_file.exists():
            return None
        try:
            return pd.read_csv(self.data_file)
        except Exception as e:
            logger.error(f"读取数据文件失败: {e}")
            return None


class OrderServiceClient:
    """
    Order服务客户端
    通过Nacos服务发现调用Order服务接口
    """

    def __init__(self, service_name: str, nacos_registry: Optional[NacosServiceRegistry] = None):
        """
        初始化Order服务客户端

        Args:
            service_name: 服务名
            nacos_registry: Nacos注册器实例
        """
        self.service_name = service_name
        self.nacos_registry = nacos_registry
        self.client = httpx.AsyncClient(timeout=30.0)

    async def get_daily_passengers(self, date: str) -> Optional[int]:
        """
        获取指定日期的日度客流量

        Args:
            date: 日期字符串 (YYYY-MM-DD)

        Returns:
            客流量，获取失败返回None
        """
        try:
            # 通过Nacos获取服务实例
            service_url = await self._get_service_url()
            if not service_url:
                logger.error(f"无法获取服务实例: {self.service_name}")
                return None

            # 调用Order服务接口
            url = f"{service_url}/api/order/v1/statistics/daily-passengers"
            params = {"date": date}

            response = await self.client.get(url, params=params)
            response.raise_for_status()

            data = response.json()
            if data.get("code") == 200:
                return data.get("data", {}).get("passengers")
            else:
                logger.warning(f"获取日度数据失败: {data.get('message')}")
                return None

        except Exception as e:
            logger.error(f"调用Order服务失败: {e}")
            return None

    async def _get_service_url(self) -> Optional[str]:
        """
        通过Nacos获取服务URL

        Returns:
            服务URL，获取失败返回None
        """
        try:
            # 如果有Nacos注册器，使用其客户端
            if self.nacos_registry and self.nacos_registry.client:
                from nacos import NacosClient
                client: NacosClient = self.nacos_registry.client

                # 获取服务实例列表
                instances = client.list_naming_instance(self.service_name)
                if instances and 'hosts' in instances and instances['hosts']:
                    # 选择健康的实例
                    healthy_hosts = [h for h in instances['hosts'] if h.get('healthy', True)]
                    if healthy_hosts:
                        # 简单轮询选择第一个
                        host = healthy_hosts[0]
                        ip = host.get('ip')
                        port = host.get('port')
                        return f"http://{ip}:{port}"

            # 如果没有Nacos，尝试直接连接（用于测试）
            return os.getenv("ORDER_SERVICE_URL", "http://localhost:8081")

        except Exception as e:
            logger.error(f"获取服务URL失败: {e}")
            return None

    async def close(self):
        """关闭HTTP客户端"""
        await self.client.aclose()


class AutoRetrainService:
    """
    自动重训服务
    实现定时任务调度、数据获取、模型重训等功能
    """

    def __init__(self):
        self.config = settings.auto_retrain
        self.data_storage = DataRollingStorage(
            data_file=settings.bus_data_file,
            max_rows=self.config.max_data_rows
        )
        self.order_client: Optional[OrderServiceClient] = None
        self._scheduler_task: Optional[asyncio.Task] = None
        self._running = False
        self._last_retrain_date: Optional[datetime] = None
        self._retrain_count = 0

    async def start(self):
        """启动自动重训服务"""
        if not self.config.enabled:
            logger.info("自动重训服务已禁用")
            return

        logger.info("=" * 60)
        logger.info("启动自动重训服务")
        logger.info("=" * 60)
        logger.info(f"执行时间: 每日 {self.config.schedule_time}")
        logger.info(f"重训周期: 每 {self.config.retrain_cycle_days} 天")
        logger.info(f"Order服务: {self.config.order_service_name}")
        logger.info(f"数据文件: {self.config.data_file}")
        logger.info(f"最大数据行数: {self.config.max_data_rows}")

        # 初始化Order服务客户端
        self.order_client = OrderServiceClient(
            service_name=self.config.order_service_name
        )

        # 检查数据文件是否存在，不存在则初始化
        if not settings.bus_data_file.exists():
            logger.info("滚动数据文件不存在，正在初始化...")
            success = self.data_storage.initialize_from_original(settings.original_data_file)
            if not success:
                logger.error("初始化滚动数据文件失败")
                return

        self._running = True

        # 启动调度任务
        self._scheduler_task = asyncio.create_task(self._schedule_loop())
        logger.info("自动重训服务已启动")

    async def stop(self):
        """停止自动重训服务"""
        self._running = False

        if self._scheduler_task and not self._scheduler_task.done():
            self._scheduler_task.cancel()
            try:
                await self._scheduler_task
            except asyncio.CancelledError:
                pass

        if self.order_client:
            await self.order_client.close()

        logger.info("自动重训服务已停止")

    async def _schedule_loop(self):
        """
        调度循环
        每日在指定时间执行数据获取和重训检查
        """
        while self._running:
            try:
                now = datetime.now()
                schedule_time = datetime.strptime(self.config.schedule_time, "%H:%M").time()

                # 计算下一次执行时间
                next_run = datetime.combine(now.date(), schedule_time)
                if next_run <= now:
                    # 如果今天的时间已过，安排到明天
                    next_run += timedelta(days=1)

                wait_seconds = (next_run - now).total_seconds()
                logger.info(f"下次执行时间: {next_run}, 等待 {wait_seconds/3600:.1f} 小时")

                # 等待到执行时间
                await asyncio.sleep(wait_seconds)

                if not self._running:
                    break

                # 执行每日任务
                await self._daily_task()

            except asyncio.CancelledError:
                break
            except Exception as e:
                logger.error(f"调度循环异常: {e}", exc_info=True)
                await asyncio.sleep(60)  # 异常后等待1分钟再重试

    async def _daily_task(self):
        """
        每日任务
        1. 获取昨日数据
        2. 更新滚动存储
        3. 检查是否需要重训
        """
        logger.info("=" * 60)
        logger.info("执行每日任务")
        logger.info("=" * 60)

        # 获取昨日日期
        yesterday = (datetime.now() - timedelta(days=1)).strftime("%Y-%m-%d")

        # 获取昨日数据
        passengers = await self.order_client.get_daily_passengers(yesterday)

        if passengers is not None:
            # 更新滚动存储
            success = self.data_storage.append_daily_data(yesterday, passengers)
            if success:
                logger.info(f"已更新日度数据: {yesterday}, 客流量: {passengers}")
            else:
                logger.error(f"更新日度数据失败: {yesterday}")
        else:
            logger.warning(f"无法获取日度数据: {yesterday}")

        # 检查是否需要重训
        await self._check_and_retrain()

    async def _check_and_retrain(self):
        """
        检查是否需要重训并执行
        根据重训周期判断
        """
        now = datetime.now()

        # 判断是否需要重训
        need_retrain = False
        if self._last_retrain_date is None:
            need_retrain = True
            logger.info("首次运行，需要重训")
        else:
            days_since_last = (now - self._last_retrain_date).days
            if days_since_last >= self.config.retrain_cycle_days:
                need_retrain = True
                logger.info(f"距离上次重训已 {days_since_last} 天，需要重训")

        if not need_retrain:
            days_since = (now - self._last_retrain_date).days if self._last_retrain_date else 0
            remaining = self.config.retrain_cycle_days - days_since
            logger.info(f"暂不需要重训，距离下次重训还有 {remaining} 天")
            return

        # 执行重训
        await self._perform_retrain()

    async def _perform_retrain(self):
        """
        执行模型重训
        """
        logger.info("=" * 60)
        logger.info("开始执行模型重训")
        logger.info("=" * 60)

        try:
            # 使用任务管理器创建异步训练任务
            from app.core.task_manager import task_manager, TaskStatus

            # 创建任务
            task = await task_manager.create_task(user_id="auto-retrain")

            # 提交训练任务
            await task_manager.submit_task(
                task.task_id,
                self._run_training_with_progress,
                task.task_id
            )

            logger.info(f"已提交重训任务: {task.task_id}")

            # 轮询等待任务完成（最多等待30分钟）
            max_wait = 30 * 60  # 30分钟
            waited = 0
            while waited < max_wait:
                await asyncio.sleep(5)
                waited += 5

                task_info = await task_manager.get_task(task.task_id)
                if not task_info:
                    break

                if task_info.status in [TaskStatus.COMPLETED, TaskStatus.FAILED]:
                    break

            # 更新重训记录
            if task_info and task_info.status == TaskStatus.COMPLETED:
                self._last_retrain_date = datetime.now()
                self._retrain_count += 1
                logger.info(f"重训完成，当前重训次数: {self._retrain_count}")

                # 重新加载模型
                prediction_service._load_model()
                logger.info("模型已重新加载")
            else:
                logger.error("重训失败或超时")

        except Exception as e:
            logger.error(f"重训过程异常: {e}", exc_info=True)

    def _run_training_with_progress(self, task_id: str, progress_callback: Optional[Callable] = None) -> Dict:
        """
        执行训练并更新进度

        Args:
            task_id: 任务ID
            progress_callback: 进度回调函数

        Returns:
            训练结果
        """
        import time
        start_time = time.time()

        def update_progress(stage, percent, message, **kwargs):
            if progress_callback:
                progress_callback(stage, percent, message, **kwargs)
            logger.info(f"[{task_id}] {stage.value}: {percent}% - {message}")

        try:
            # 阶段1: 初始化
            update_progress(TaskStage.INITIALIZING, 5, "正在初始化训练环境...")

            # 检查数据文件
            if not settings.bus_data_file.exists():
                raise FileNotFoundError(f"数据文件不存在: {settings.bus_data_file}")

            # 阶段2: 数据加载
            update_progress(TaskStage.DATA_LOADING, 10, "正在加载数据...")

            from utils.data_processor import DataProcessor
            processor = DataProcessor()
            monthly_data = processor.load_monthly_data(str(settings.bus_data_file))

            # 阶段3: 数据处理
            update_progress(TaskStage.DATA_PROCESSING, 20, "正在处理数据...")
            daily_data = processor.monthly_to_daily(
                interpolation_method='cubic',
                apply_effects=True,
                apply_perturbation=True,
                noise_level=0.15,
                noise_type='adaptive',
                random_seed=42
            )
            passenger_series = daily_data['Passengers']

            # 阶段4: 参数优化
            update_progress(TaskStage.PARAMETER_OPTIMIZATION, 35,
                          f"正在执行网格搜索 (scoring={self.config.scoring})...",
                          estimated_remaining_seconds=120)

            from training.arima_trainer import ARIMATrainer
            trainer = ARIMATrainer()

            if self.config.use_grid_search:
                trainer.find_best_params(
                    passenger_series,
                    method='grid_search',
                    scoring=self.config.scoring,
                    n_splits=5,
                    random_state=42
                )
            else:
                trainer.find_best_params(
                    passenger_series,
                    method='aic',
                    random_state=42
                )

            # 阶段5: 模型训练
            update_progress(TaskStage.MODEL_TRAINING, 60,
                          f"正在训练 ARIMA({trainer.p},{trainer.d},{trainer.q}) 模型...",
                          estimated_remaining_seconds=60)

            history = trainer.train(
                passenger_series,
                validate=True,
                test_size=30
            )

            # 阶段6: 模型验证
            update_progress(TaskStage.MODEL_VALIDATION, 85, "正在验证模型性能...")

            # 阶段7: 模型保存
            update_progress(TaskStage.MODEL_SAVING, 95, "正在保存模型...")
            trainer.save_model(str(settings.model_path))

            # 完成
            update_progress(TaskStage.COMPLETED, 100, "训练完成！")

            execution_time = time.time() - start_time

            # 构建验证指标
            validation = None
            if 'validation' in history:
                val = history['validation']
                validation = {
                    'mae': val['mae'],
                    'rmse': val['rmse'],
                    'mape': val['mape'],
                    'test_size': val['test_size']
                }

            return {
                'status': 'success',
                'message': '模型重训成功',
                'order': [trainer.p, trainer.d, trainer.q],
                'aic': history.get('aic'),
                'bic': history.get('bic'),
                'training_time': datetime.now().isoformat(),
                'execution_time': execution_time,
                'validation': validation,
                'data_length': history.get('data_length', len(passenger_series))
            }

        except Exception as e:
            logger.error(f"重训任务 {task_id} 失败: {e}", exc_info=True)
            raise

    async def force_retrain(self) -> bool:
        """
        强制立即执行重训

        Returns:
            是否成功启动重训
        """
        logger.info("强制启动重训...")
        await self._perform_retrain()
        return True

    def get_status(self) -> Dict[str, Any]:
        """
        获取服务状态

        Returns:
            状态字典
        """
        return {
            'enabled': self.config.enabled,
            'running': self._running,
            'schedule_time': self.config.schedule_time,
            'retrain_cycle_days': self.config.retrain_cycle_days,
            'last_retrain_date': self._last_retrain_date.isoformat() if self._last_retrain_date else None,
            'retrain_count': self._retrain_count,
            'data_file': str(self.config.data_file),
            'data_rows': len(self.data_storage.get_current_data()) if self.data_storage.get_current_data() is not None else 0
        }


# 全局自动重训服务实例
auto_retrain_service: Optional[AutoRetrainService] = None


async def init_auto_retrain() -> bool:
    """初始化自动重训服务"""
    global auto_retrain_service

    auto_retrain_service = AutoRetrainService()
    await auto_retrain_service.start()
    return True


async def close_auto_retrain():
    """关闭自动重训服务"""
    global auto_retrain_service

    if auto_retrain_service:
        await auto_retrain_service.stop()
        auto_retrain_service = None
