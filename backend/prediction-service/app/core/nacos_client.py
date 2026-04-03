"""
Nacos 服务注册与发现客户端
使用 nacos-sdk-python 官方 SDK
"""
import json
import os
import socket
import logging
import asyncio
import time
from typing import Optional, Dict, Any, List

import nacos
from nacos import NacosClient

logger = logging.getLogger(__name__)


class NacosConfigClient:
    """Nacos 配置客户端 - 用于获取共享配置"""

    def __init__(self, server_addr: str, namespace: str = "",
                 username: str = None, password: str = None):
        self.server_addr = server_addr
        self.namespace = namespace
        self.username = username
        self.password = password
        self._client: Optional[NacosClient] = None

    def _get_server_url(self) -> str:
        """获取 Nacos 服务器 URL"""
        if not self.server_addr.startswith(("http://", "https://")):
            return f"http://{self.server_addr}"
        return self.server_addr

    def _init_client(self) -> bool:
        """初始化 Nacos 客户端"""
        if self._client:
            return True

        try:
            server_url = self._get_server_url()

            if self.username and self.password:
                self._client = NacosClient(
                    server_addresses=server_url,
                    namespace=self.namespace,
                    username=self.username,
                    password=self.password
                )
            else:
                self._client = NacosClient(
                    server_addresses=server_url,
                    namespace=self.namespace
                )

            logger.debug(f"Nacos 配置客户端初始化成功: {server_url}")
            return True

        except Exception as e:
            logger.error(f"Nacos 配置客户端初始化失败: {e}")
            return False

    def get_config(self, data_id: str, group: str = "DEFAULT_GROUP", timeout: int = 10,
                   debug_mode: bool = False) -> Optional[str]:
        """
        从 Nacos 获取配置

        Args:
            data_id: 配置 ID (如 "atbs-common.yaml")
            group: 配置分组
            timeout: 超时时间（秒）
            debug_mode: 是否为调试模式（打印配置内容）

        Returns:
            配置内容，获取失败返回 None
        """
        if not self._init_client():
            return None

        try:
            # 使用 no_snapshot=True 强制从服务器获取，避免本地缓存问题
            config = self._client.get_config(data_id, group, timeout=timeout, no_snapshot=True)
            if config:
                logger.info(f"从 Nacos 获取配置成功: {data_id} (group: {group})")
                # debug 级别时打印配置内容
                if debug_mode or logger.isEnabledFor(logging.DEBUG):
                    # 截取前2000字符，避免日志过长
                    config_preview = config[:2000] if len(config) > 2000 else config
                    print(f"[Config] 配置内容 [{data_id}]:\n{config_preview}")
                    if len(config) > 2000:
                        print(f"[Config] 配置内容已截断，总长度: {len(config)} 字符")
                return config
            else:
                logger.warning(f"Nacos 配置不存在或为空: {data_id} (group: {group}, namespace: {self.namespace})")
                return None

        except Exception as e:
            logger.error(f"从 Nacos 获取配置失败: {data_id} (group: {group}) - {e}")
            import traceback
            logger.error(traceback.format_exc())
            return None

    def load_configs(self, imports: List[str], app_name: str = "") -> Dict[str, Any]:
        """
        加载多个 Nacos 配置

        Args:
            imports: 配置导入列表，如 ["nacos:atbs-common.yaml", "nacos:${spring.application.name}.yaml"]
            app_name: 应用名称，用于替换 ${spring.application.name}

        Returns:
            合并后的配置字典
        """
        merged_config = {}

        for import_item in imports:
            if not import_item.startswith("nacos:"):
                continue

            # 解析配置 ID
            config_id = import_item.replace("nacos:", "")

            # 替换变量
            config_id = config_id.replace("${spring.application.name}", app_name)

            # 获取配置
            config_content = self.get_config(config_id)
            if config_content:
                try:
                    import yaml
                    config_data = yaml.safe_load(config_content)
                    if isinstance(config_data, dict):
                        # 递归合并配置
                        self._deep_merge(merged_config, config_data)
                        logger.info(f"合并 Nacos 配置: {config_id}")
                except Exception as e:
                    logger.error(f"解析 Nacos 配置失败: {config_id} - {e}")

        return merged_config

    @staticmethod
    def _deep_merge(base: Dict, override: Dict) -> Dict:
        """深度合并字典"""
        for key, value in override.items():
            if key in base and isinstance(base[key], dict) and isinstance(value, dict):
                NacosConfigClient._deep_merge(base[key], value)
            else:
                base[key] = value
        return base


class NacosServiceRegistry:
    """Nacos 服务注册器 - 使用官方 SDK，增强稳定性"""

    def __init__(self, settings=None):
        self.client: Optional[NacosClient] = None
        self.service_name: Optional[str] = None
        self.ip: Optional[str] = None
        self.port: Optional[int] = None
        self._heartbeat_task: Optional[asyncio.Task] = None
        self._running = False
        self._registered = False
        self._settings = settings
        self._heartbeat_interval = 5  # 心跳间隔（秒）
        self._retry_count = 0
        self._max_retries = 3

    def _get_server_url(self) -> str:
        """获取 Nacos 服务器 URL"""
        if not self._settings:
            return "http://localhost:8848"
        server_addr = getattr(self._settings, 'nacos_server_addr', 'localhost:8848')
        if not server_addr.startswith(("http://", "https://")):
            server_addr = f"http://{server_addr}"
        return server_addr

    def _get_local_ip(self) -> str:
        """获取本机 IP 地址"""
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            s.connect(("8.8.8.8", 80))
            ip = s.getsockname()[0]
            s.close()
            return ip
        except Exception:
            return "127.0.0.1"

    async def init_client(self) -> bool:
        """初始化 Nacos 客户端（带重试）"""
        for attempt in range(self._max_retries):
            try:
                server_url = self._get_server_url()
                namespace = getattr(self._settings, 'nacos_namespace', "") if self._settings else ""
                username = getattr(self._settings, 'nacos_username', None) if self._settings else None
                password = getattr(self._settings, 'nacos_password', None) if self._settings else None

                # 创建 Nacos 客户端
                if username and password:
                    self.client = NacosClient(
                        server_addresses=server_url,
                        namespace=namespace,
                        username=username,
                        password=password
                    )
                else:
                    self.client = NacosClient(
                        server_addresses=server_url,
                        namespace=namespace
                    )

                logger.info(f"Nacos 客户端初始化成功: {server_url}")
                self._retry_count = 0
                return True

            except Exception as e:
                self._retry_count += 1
                logger.error(f"Nacos 客户端初始化失败 (尝试 {attempt + 1}/{self._max_retries}): {e}")
                if attempt < self._max_retries - 1:
                    await asyncio.sleep(2 ** attempt)  # 指数退避
                else:
                    return False

    async def register_service(
        self,
        service_name: str = None,
        ip: str = None,
        port: int = None,
        metadata: Dict[str, str] = None
    ) -> bool:
        """
        注册服务实例（带重试机制）
        """
        if not self.client:
            success = await self.init_client()
            if not success:
                return False

        self.service_name = service_name or (getattr(self._settings, 'nacos_service_name', 'prediction-service') if self._settings else 'prediction-service')
        self.ip = ip or self._get_local_ip()
        self.port = port or (getattr(self._settings, 'port', 8080) if self._settings else 8080)

        # 构建元数据
        service_metadata = {
            "version": getattr(self._settings, 'app_version', '1.0.0') if self._settings else "1.0.0",
            "preserved.register.source": "FASTAPI",
            "protocol": "http",
            "management.context-path": "/health",
            "preserved.heart.beat.interval": "5000",  # 心跳间隔 5 秒
            "preserved.heart.beat.timeout": "15000",  # 心跳超时 15 秒
            "preserved.ip.delete.timeout": "30000",   # IP 删除超时 30 秒
            **(metadata or {})
        }

        nacos_group = getattr(self._settings, 'nacos_group', 'DEFAULT_GROUP') if self._settings else 'DEFAULT_GROUP'

        # 重试注册
        for attempt in range(self._max_retries):
            try:
                # 注册实例
                self.client.add_naming_instance(
                    service_name=self.service_name,
                    ip=self.ip,
                    port=self.port,
                    group_name=nacos_group,
                    weight=1.0,
                    enable=True,
                    healthy=True,
                    ephemeral=True,
                    metadata=service_metadata
                )

                logger.info(f"服务注册成功: {self.service_name} @ {self.ip}:{self.port}")
                self._registered = True
                self._retry_count = 0

                # 启动心跳任务
                self._running = True
                self._start_heartbeat()

                return True

            except Exception as e:
                self._retry_count += 1
                logger.error(f"服务注册失败 (尝试 {attempt + 1}/{self._max_retries}): {e}")
                if attempt < self._max_retries - 1:
                    await asyncio.sleep(2 ** attempt)
                else:
                    return False

    async def deregister_service(self) -> bool:
        """注销服务实例"""
        self._running = False
        self._registered = False

        if not self.client or not self.service_name:
            logger.warning("服务信息不完整，无法注销")
            return False

        # 停止心跳
        if self._heartbeat_task and not self._heartbeat_task.done():
            self._heartbeat_task.cancel()
            try:
                # 等待任务取消，设置超时防止无限等待
                await asyncio.wait_for(self._heartbeat_task, timeout=5.0)
            except asyncio.TimeoutError:
                logger.warning("心跳任务取消超时")
            except asyncio.CancelledError:
                pass
            finally:
                self._heartbeat_task = None

        try:
            nacos_group = getattr(self._settings, 'nacos_group', 'DEFAULT_GROUP') if self._settings else 'DEFAULT_GROUP'
            self.client.remove_naming_instance(
                service_name=self.service_name,
                ip=self.ip,
                port=self.port,
                group_name=nacos_group,
                ephemeral=True
            )

            logger.info(f"服务注销成功: {self.service_name}")
            return True

        except Exception as e:
            logger.error(f"服务注销失败: {e}")
            return False

    def send_heartbeat(self) -> bool:
        """发送心跳"""
        if not self.client or not self.service_name:
            return False

        try:
            nacos_group = getattr(self._settings, 'nacos_group', 'DEFAULT_GROUP') if self._settings else 'DEFAULT_GROUP'
            self.client.send_heartbeat(
                service_name=self.service_name,
                ip=self.ip,
                port=self.port,
                group_name=nacos_group,
                ephemeral=True
            )
            return True

        except Exception as e:
            logger.error(f"心跳发送失败: {e}")
            return False

    def _start_heartbeat(self):
        """启动心跳任务"""
        if self._heartbeat_task is None or self._heartbeat_task.done():
            self._heartbeat_task = asyncio.create_task(self._heartbeat_loop())

    async def _heartbeat_loop(self):
        """
        心跳循环 - 更频繁的心跳确保服务保持注册状态
        只有第一次心跳成功才打印日志，之后只有失败才打印
        """
        consecutive_failures = 0
        max_failures = 3
        first_success_logged = False  # 标记是否已经记录过第一次成功

        while self._running:
            try:
                # 发送心跳
                success = self.send_heartbeat()

                if success:
                    consecutive_failures = 0
                    # 只有第一次成功才打印日志
                    if not first_success_logged:
                        logger.info(f"心跳发送成功: {self.service_name} (后续成功日志将静默)")
                        first_success_logged = True
                else:
                    consecutive_failures += 1
                    logger.warning(f"心跳发送失败 ({consecutive_failures}/{max_failures}): {self.service_name}")

                    # 如果连续失败次数达到阈值，尝试重新注册
                    if consecutive_failures >= max_failures and self._registered:
                        logger.error(f"心跳连续失败 {max_failures} 次，尝试重新注册服务")
                        await self._reregister()
                        consecutive_failures = 0
                        first_success_logged = False  # 重置标记，重新注册成功后需要再次记录

                # 等待下一次心跳
                await asyncio.sleep(self._heartbeat_interval)

            except asyncio.CancelledError:
                logger.info("心跳循环已取消")
                break
            except Exception as e:
                logger.error(f"心跳循环异常: {e}")
                await asyncio.sleep(self._heartbeat_interval)

    async def _reregister(self):
        """重新注册服务"""
        try:
            logger.info(f"正在重新注册服务: {self.service_name}")
            # 先注销再注册
            try:
                nacos_group = getattr(self._settings, 'nacos_group', 'DEFAULT_GROUP') if self._settings else 'DEFAULT_GROUP'
                self.client.remove_naming_instance(
                    service_name=self.service_name,
                    ip=self.ip,
                    port=self.port,
                    group_name=nacos_group,
                    ephemeral=True
                )
            except:
                pass  # 注销失败不影响重新注册

            # 等待一下确保注销完成
            await asyncio.sleep(1)

            # 重新注册
            success = await self.register_service(
                service_name=self.service_name,
                ip=self.ip,
                port=self.port
            )

            if success:
                logger.info(f"服务重新注册成功: {self.service_name}")
            else:
                logger.error(f"服务重新注册失败: {self.service_name}")

        except Exception as e:
            logger.error(f"重新注册服务时出错: {e}")


# 全局 Nacos 注册器实例
nacos_registry: Optional[NacosServiceRegistry] = None


async def init_nacos(settings=None) -> bool:
    """初始化 Nacos 并注册服务（带重试）"""
    global nacos_registry

    if settings and not getattr(settings, 'nacos_enabled', True):
        logger.info("Nacos 服务注册已禁用")
        return True

    nacos_registry = NacosServiceRegistry(settings=settings)

    # 尝试注册，失败后重试
    for attempt in range(3):
        success = await nacos_registry.register_service()
        if success:
            return True
        else:
            logger.warning(f"Nacos 注册失败，将在 5 秒后重试 (尝试 {attempt + 1}/3)")
            await asyncio.sleep(5)

    logger.error("Nacos 服务注册失败，服务将继续运行但可能无法被发现")
    return False


async def close_nacos():
    """关闭 Nacos 并注销服务"""
    global nacos_registry

    if nacos_registry:
        await nacos_registry.deregister_service()
        nacos_registry = None
