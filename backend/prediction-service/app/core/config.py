"""
应用配置
使用 Pydantic BaseSettings + YAML 配置文件，与 Spring Boot 保持一致
提供运行时配置校验和 IDE 自动补全支持

配置结构参考 Java 的 Properties 模式，每个一级配置独立成类
"""
import os
from pathlib import Path
from typing import List, Optional, Dict, Any

import yaml
from pydantic import Field, field_validator, BaseModel
from pydantic_settings import BaseSettings, SettingsConfigDict, PydanticBaseSettingsSource


# ========== 独立配置类（对应 Java 的 Properties 类）==========

class SpringApplicationProperties(BaseModel):
    """Spring 应用配置 - 对应 spring.application.*"""
    name: str = Field(default="prediction-service", description="应用名称")


class SpringProfilesProperties(BaseModel):
    """Spring 环境配置 - 对应 spring.profiles.*"""
    active: str = Field(default="dev", description="当前激活的环境")


class SpringConfigImport(BaseModel):
    """Spring 配置导入 - 对应 spring.config.import.*"""
    imports: List[str] = Field(default_factory=list, alias="import", description="配置导入列表")


class NacosConfigProperties(BaseModel):
    """Nacos 配置中心属性 - 对应 spring.cloud.nacos.config.*"""
    namespace: str = Field(default="", description="配置中心命名空间")
    group: str = Field(default="DEFAULT_GROUP", description="配置分组")
    file_extension: str = Field(default="yaml", alias="file-extension", description="配置文件扩展名")


class NacosDiscoveryProperties(BaseModel):
    """Nacos 服务发现属性 - 对应 spring.cloud.nacos.discovery.*"""
    namespace: str = Field(default="", description="服务发现命名空间")
    group: str = Field(default="DEFAULT_GROUP", description="服务分组")
    enabled: bool = Field(default=True, description="是否启用服务发现")


class NacosProperties(BaseModel):
    """Nacos 配置属性 - 对应 spring.cloud.nacos.*"""
    server_addr: str = Field(default="localhost:8848", alias="server-addr", description="Nacos 服务器地址")
    username: Optional[str] = Field(default=None, description="Nacos 用户名")
    password: Optional[str] = Field(default=None, description="Nacos 密码")
    config: NacosConfigProperties = Field(default_factory=NacosConfigProperties, description="配置中心属性")
    discovery: NacosDiscoveryProperties = Field(default_factory=NacosDiscoveryProperties, description="服务发现属性")

    @property
    def is_enabled(self) -> bool:
        """服务发现是否启用"""
        return self.discovery.enabled


class SpringCloudProperties(BaseModel):
    """Spring Cloud 配置 - 对应 spring.cloud.*"""
    nacos: NacosProperties = Field(default_factory=NacosProperties, description="Nacos 配置")


class SpringProperties(BaseModel):
    """Spring 配置 - 对应 spring.*"""
    application: SpringApplicationProperties = Field(default_factory=SpringApplicationProperties, description="应用配置")
    profiles: SpringProfilesProperties = Field(default_factory=SpringProfilesProperties, description="环境配置")
    config: SpringConfigImport = Field(default_factory=SpringConfigImport, description="配置导入")
    cloud: SpringCloudProperties = Field(default_factory=SpringCloudProperties, description="Cloud 配置")


class ServerProperties(BaseModel):
    """服务器配置 - 对应 server.*"""
    host: str = Field(default="0.0.0.0", description="服务器监听地址")
    port: int = Field(default=8080, ge=1, le=65535, description="服务器监听端口")


class PredictionProperties(BaseModel):
    """预测服务配置 - 对应 prediction.*"""
    default_days: int = Field(default=20, ge=1, le=365, alias="default-days", description="默认预测天数")
    max_days: int = Field(default=365, ge=1, le=3650, alias="max-days", description="最大预测天数")
    confidence_level: float = Field(default=0.95, ge=0.0, le=1.0, alias="confidence-level", description="默认置信水平")


class AutoRetrainProperties(BaseModel):
    """自动重训配置 - 对应 auto-retrain.*"""
    enabled: bool = Field(default=True, description="是否启用自动重训")
    schedule_time: str = Field(default="02:00", alias="schedule-time", description="每日执行时间 (HH:MM)")
    retrain_cycle_days: int = Field(default=7, ge=1, le=30, alias="retrain-cycle-days", description="重训周期（天）")
    order_service_name: str = Field(default="order-service", alias="order-service-name", description="Order服务名")
    data_file: str = Field(default="bus_data.csv", alias="data-file", description="数据文件名")
    max_data_rows: int = Field(default=145, ge=50, le=500, alias="max-data-rows", description="最大数据行数（滚动窗口）")
    use_grid_search: bool = Field(default=True, alias="use-grid-search", description="是否使用网格搜索")
    scoring: str = Field(default="mape", description="评分指标")

    @field_validator("schedule_time")
    @classmethod
    def validate_schedule_time(cls, v):
        """验证时间格式"""
        try:
            from datetime import datetime
            datetime.strptime(v, "%H:%M")
            return v
        except ValueError:
            raise ValueError("schedule-time 格式错误，应为 HH:MM")


class JwtTokenConfig(BaseModel):
    """JWT Token 配置 - 对应 jwt.tokens.{type}.*"""
    secret_key: str = Field(default="", alias="secret-key", description="JWT 签名密钥")
    ttl: int = Field(default=604800, description="Token 有效期（秒）")
    token_name: str = Field(default="token", alias="token-name", description="Token 名称")
    refresh_time: int = Field(default=86400, alias="refresh-time", description="刷新时间（秒）")


class JwtTokensProperties(BaseModel):
    """JWT Tokens 配置 - 对应 jwt.tokens.*"""
    app: JwtTokenConfig = Field(default_factory=JwtTokenConfig, description="App Token 配置")
    internal: JwtTokenConfig = Field(default_factory=JwtTokenConfig, description="Internal Token 配置")
    mgmt: JwtTokenConfig = Field(default_factory=lambda: JwtTokenConfig(ttl=7200, refresh_time=300), description="Mgmt Token 配置")


class JwtEndpointConfig(BaseModel):
    """JWT Endpoint 配置 - 对应 jwt.endpoints.{type}.*"""
    default_token_type: str = Field(default="mgmt", alias="defaultTokenType", description="默认 Token 类型")


class JwtEndpointsProperties(BaseModel):
    """JWT Endpoints 配置 - 对应 jwt.endpoints.*"""
    app: JwtEndpointConfig = Field(default_factory=lambda: JwtEndpointConfig(default_token_type="app"), description="App Endpoint 配置")
    internal: JwtEndpointConfig = Field(default_factory=lambda: JwtEndpointConfig(default_token_type="internal"), description="Internal Endpoint 配置")
    mgmt: JwtEndpointConfig = Field(default_factory=lambda: JwtEndpointConfig(default_token_type="mgmt"), description="Mgmt Endpoint 配置")


class JwtProperties(BaseModel):
    """JWT 配置 - 对应 jwt.*"""
    tokens: JwtTokensProperties = Field(default_factory=JwtTokensProperties, description="Token 配置")
    endpoints: JwtEndpointsProperties = Field(default_factory=JwtEndpointsProperties, description="Endpoint 配置")


class CorsProperties(BaseModel):
    """CORS 跨域配置 - 对应 cors.* (参考 Java CorsProperties)"""
    allow_origins: List[str] = Field(default_factory=lambda: ["*"], alias="allow-origins", description="允许的跨域来源")
    allow_methods: List[str] = Field(default_factory=lambda: ["*"], alias="allow-methods", description="允许的HTTP方法")
    allow_headers: List[str] = Field(default_factory=lambda: ["*"], alias="allow-headers", description="允许的请求头")
    expose_headers: List[str] = Field(default_factory=list, alias="expose-headers", description="暴露的响应头")
    allow_credentials: bool = Field(default=True, alias="allow-credentials", description="是否允许携带凭证")
    max_age: int = Field(default=3600, alias="max-age", description="预检请求缓存时间(秒)")

    @field_validator("allow_origins", mode="before")
    @classmethod
    def parse_origins(cls, v):
        """解析来源列表（支持字符串或列表）"""
        if isinstance(v, str):
            try:
                import json
                return json.loads(v)
            except:
                return [v]
        return v

    @field_validator("allow_methods", "allow_headers", "expose_headers", mode="before")
    @classmethod
    def parse_string_list(cls, v):
        """解析字符串列表（支持字符串或列表）"""
        if isinstance(v, str):
            try:
                import json
                return json.loads(v)
            except:
                return [item.strip() for item in v.split(",") if item.strip()]
        return v


class LoggingLevelProperties(BaseModel):
    """日志级别详细配置 - 对应 logging.level.* (Java 风格)"""
    root: str = Field(default="INFO", description="根日志级别")
    top_flowerstardream_base: str = Field(default="INFO", alias="top.flowerstardream.base", description="base 包日志级别")
    top_flowerstardream_atbs: str = Field(default="INFO", alias="top.flowerstardream.atbs", description="atbs 包日志级别")
    org_springframework_web: str = Field(default="WARN", alias="org.springframework.web", description="Spring Web 日志级别")
    com_alibaba_cloud_nacos: str = Field(default="WARN", alias="com.alibaba.cloud.nacos", description="Nacos 日志级别")


class LoggingFileProperties(BaseModel):
    """日志文件配置 - 对应 logging.file.*"""
    enabled: bool = Field(default=True, description="是否启用文件日志")
    path: str = Field(default="backend/logs/${spring.application.name}", description="日志文件路径")
    name: str = Field(default="${spring.application.name}.log", description="日志文件名")
    history_pattern: str = Field(default="${spring.application.name}-%d{yyyy-MM-dd}.log", alias="history-pattern", description="历史日志文件名模式")
    max_history: int = Field(default=30, ge=1, le=365, alias="max-history", description="历史日志保存天数")
    max_size: str = Field(default="100MB", description="单个日志文件最大大小")


class LoggingProperties(BaseModel):
    """日志配置 - 对应 logging.*"""
    level: LoggingLevelProperties = Field(default_factory=LoggingLevelProperties, description="日志级别配置")
    file: LoggingFileProperties = Field(default_factory=LoggingFileProperties, description="日志文件配置")

    @property
    def root_level(self) -> str:
        """获取根日志级别"""
        return self.level.root


# ========== YAML 配置源 ==========

class YamlConfigSettingsSource(PydanticBaseSettingsSource):
    """
    自定义 YAML 配置源
    从 application.yml 加载配置，并支持从 Nacos 导入共享配置
    """

    def __init__(self, settings_cls: type[BaseSettings], yaml_file: str = "application.yml"):
        super().__init__(settings_cls)
        self.yaml_file = yaml_file
        self._yaml_data: Dict[str, Any] = self._load_yaml()
        # 从 Nacos 导入配置
        self._import_nacos_configs()

    def _load_yaml(self) -> Dict[str, Any]:
        """加载 YAML 配置文件"""
        config_paths = [
            Path(self.yaml_file),
            Path("config") / self.yaml_file,
            Path(__file__).resolve().parent.parent.parent / self.yaml_file,
        ]

        for config_path in config_paths:
            if config_path.exists():
                try:
                    with open(config_path, "r", encoding="utf-8") as f:
                        return yaml.safe_load(f) or {}
                except Exception:
                    return {}
        return {}

    def _import_nacos_configs(self):
        """从 Nacos 导入共享配置，类似 Spring Cloud Nacos Config"""
        try:
            # 获取配置导入列表
            spring_config = self._yaml_data.get("spring", {})
            config_import = spring_config.get("config", {})
            imports = config_import.get("import", [])

            if not imports:
                return

            # 过滤出 nacos: 开头的配置
            nacos_imports = [imp for imp in imports if isinstance(imp, str) and imp.startswith("nacos:")]

            if not nacos_imports:
                return

            # 获取 Nacos 连接信息
            nacos_config = spring_config.get("cloud", {}).get("nacos", {})
            server_addr = nacos_config.get("server-addr", "localhost:8848")
            username = nacos_config.get("username")
            password = nacos_config.get("password")
            namespace = nacos_config.get("config", {}).get("namespace", "")
            group = nacos_config.get("config", {}).get("group", "DEFAULT_GROUP")

            # 获取应用名称
            app_name = spring_config.get("application", {}).get("name", "prediction-service")

            # 延迟导入 NacosConfigClient，避免循环导入
            from app.core.nacos_client import NacosConfigClient

            # 创建 Nacos 配置客户端
            config_client = NacosConfigClient(
                server_addr=server_addr,
                namespace=namespace,
                username=username,
                password=password
            )

            # 检查是否为 DEBUG 日志级别
            debug_mode = self._yaml_data.get("logging", {}).get("level", {}).get("root", "INFO") == "DEBUG"

            # 加载并合并 Nacos 配置
            for import_item in nacos_imports:
                # 解析配置 ID，如 "nacos:atbs-common.yaml"
                config_id = import_item.replace("nacos:", "")

                # 替换变量
                config_id = config_id.replace("${spring.application.name}", app_name)

                print(f"[Config] 正在从 Nacos 获取配置: {config_id} (group: {group}, namespace: {namespace})")

                # 获取配置（传入 debug_mode）
                config_content = config_client.get_config(config_id, group, debug_mode=debug_mode)
                if config_content:
                    try:
                        nacos_data = yaml.safe_load(config_content)
                        if isinstance(nacos_data, dict):
                            # 递归合并到当前配置
                            self._deep_merge(self._yaml_data, nacos_data)
                            print(f"[Config] 已从 Nacos 导入配置: {config_id}")
                        else:
                            print(f"[Config] Nacos 配置格式错误（应为字典）: {config_id}")
                    except Exception as e:
                        print(f"[Config] 解析 Nacos 配置失败: {config_id} - {e}")
                else:
                    print(f"[Config] 无法获取 Nacos 配置: {config_id}")

        except Exception as e:
            # 配置导入失败不影响本地配置加载
            print(f"[Config] Nacos 配置导入失败: {e}")

    @staticmethod
    def _deep_merge(base: Dict[str, Any], override: Dict[str, Any]) -> Dict[str, Any]:
        """深度合并字典，override 优先级更高"""
        for key, value in override.items():
            if key in base and isinstance(base[key], dict) and isinstance(value, dict):
                YamlConfigSettingsSource._deep_merge(base[key], value)
            else:
                base[key] = value
        return base

    def get_field_value(self, field: Field, field_name: str) -> Any:
        """获取字段值（Pydantic v2 新方法）"""
        yaml_path = self._get_yaml_path(field_name)
        value = self._get_value_by_path(yaml_path)

        if value is None:
            env_name = field_name.upper()
            value = os.getenv(env_name)

        return value, field_name, self.field_is_complex(field)

    def __call__(self) -> Dict[str, Any]:
        """返回所有配置数据"""
        result = {}
        for field_name, field in self.settings_cls.model_fields.items():
            value, _, _ = self.get_field_value(field, field_name)
            if value is not None:
                result[field_name] = value
        return result

    def _get_yaml_path(self, field_name: str) -> str:
        """将字段名映射到 YAML 路径"""
        mapping = {
            # Spring 配置
            "spring": "spring",
            # Server 配置
            "server": "server",
            # Prediction 配置
            "prediction": "prediction",
            # JWT 配置
            "jwt": "jwt",
            # CORS 配置
            "cors": "cors",
            # Logging 配置
            "logging": "logging",
        }
        return mapping.get(field_name, field_name.replace("_", "."))

    def _get_value_by_path(self, path: str) -> Any:
        """根据点分隔路径获取 YAML 值"""
        keys = path.split(".")
        value = self._yaml_data

        for key in keys:
            if isinstance(value, dict) and key in value:
                value = value[key]
            else:
                return None
        return value


# ========== 主配置类 ==========

class AppConfig(BaseSettings):
    """
    应用主配置类
    组合所有独立的 Properties 类，与 Spring Boot 配置风格保持一致
    """

    model_config = SettingsConfigDict(
        env_file_encoding="utf-8",
        case_sensitive=False,
        extra="ignore",
        populate_by_name=True,
    )

    # ========== 嵌套配置对象（对应 Java 的 @ConfigurationProperties）==========
    spring: SpringProperties = Field(default_factory=SpringProperties, description="Spring 配置")
    server: ServerProperties = Field(default_factory=ServerProperties, description="服务器配置")
    prediction: PredictionProperties = Field(default_factory=PredictionProperties, description="预测服务配置")
    auto_retrain: AutoRetrainProperties = Field(default_factory=AutoRetrainProperties, alias="auto-retrain", description="自动重训配置")
    jwt: JwtProperties = Field(default_factory=JwtProperties, description="JWT 配置")
    cors: CorsProperties = Field(default_factory=CorsProperties, description="CORS 配置")
    logging: LoggingProperties = Field(default_factory=LoggingProperties, description="日志配置")

    # ========== 派生属性（保持向后兼容）==========
    @property
    def app_name(self) -> str:
        """应用名称"""
        return self.spring.application.name

    @property
    def app_version(self) -> str:
        """应用版本（从环境变量或默认值获取）"""
        return os.getenv("APP_VERSION", "Release-1.0.0")

    @property
    def app_description(self) -> str:
        """应用描述"""
        return "基于 ARIMA 模型的客流预测微服务"

    @property
    def app_env(self) -> str:
        """应用环境 - 将 Spring 的 profile 映射为 Python 风格"""
        profile = self.spring.profiles.active
        mapping = {
            "dev": "development",
            "prod": "production",
            "test": "testing"
        }
        return mapping.get(profile, profile)

    @property
    def host(self) -> str:
        """服务器监听地址"""
        return self.server.host

    @property
    def port(self) -> int:
        """服务器监听端口"""
        return self.server.port

    @property
    def nacos_enabled(self) -> bool:
        """Nacos 服务注册是否启用"""
        return self.spring.cloud.nacos.is_enabled

    @property
    def nacos_server_addr(self) -> str:
        """Nacos 服务器地址"""
        return self.spring.cloud.nacos.server_addr

    @property
    def nacos_namespace(self) -> str:
        """Nacos 命名空间"""
        return self.spring.cloud.nacos.discovery.namespace

    @property
    def nacos_group(self) -> str:
        """Nacos 服务分组"""
        return self.spring.cloud.nacos.discovery.group

    @property
    def nacos_username(self) -> Optional[str]:
        """Nacos 用户名"""
        return self.spring.cloud.nacos.username

    @property
    def nacos_password(self) -> Optional[str]:
        """Nacos 密码"""
        return self.spring.cloud.nacos.password

    @property
    def nacos_service_name(self) -> str:
        """Nacos 服务名称"""
        return self.app_name

    @property
    def default_days(self) -> int:
        """默认预测天数"""
        return self.prediction.default_days

    @property
    def max_days(self) -> int:
        """最大预测天数"""
        return self.prediction.max_days

    @property
    def confidence_level(self) -> float:
        """默认置信水平"""
        return self.prediction.confidence_level

    @property
    def jwt_secret_key(self) -> str:
        """JWT 签名密钥 - 默认使用 mgmt token 的密钥"""
        return self.jwt.tokens.mgmt.secret_key or self.jwt.tokens.app.secret_key

    @property
    def log_level(self) -> str:
        """日志级别"""
        return self.logging.level.root

    @property
    def log_file_enabled(self) -> bool:
        """是否启用文件日志"""
        return self.logging.file.enabled

    @property
    def log_file_path(self) -> Path:
        """日志文件目录"""
        path_str = self.logging.file.path.replace("${spring.application.name}", self.app_name)
        return Path(path_str)

    @property
    def log_file_name(self) -> str:
        """日志文件名"""
        return self.logging.file.name.replace("${spring.application.name}", self.app_name)

    @property
    def log_history_pattern(self) -> str:
        """历史日志文件名模式"""
        return self.logging.file.history_pattern.replace("${spring.application.name}", self.app_name)

    @property
    def log_max_history(self) -> int:
        """历史日志保存天数"""
        return self.logging.file.max_history

    @property
    def jwt_token_header(self) -> str:
        """JWT Token 前缀 - 固定为 Bearer"""
        return "Bearer "

    @property
    def cors_allow_origins(self) -> List[str]:
        """CORS 允许的来源"""
        return self.cors.allow_origins

    @property
    def cors_allow_methods(self) -> List[str]:
        """CORS 允许的HTTP方法"""
        return self.cors.allow_methods

    @property
    def cors_allow_headers(self) -> List[str]:
        """CORS 允许的请求头"""
        return self.cors.allow_headers

    @property
    def cors_expose_headers(self) -> List[str]:
        """CORS 暴露的响应头"""
        return self.cors.expose_headers

    @property
    def cors_allow_credentials(self) -> bool:
        """CORS 是否允许携带凭证"""
        return self.cors.allow_credentials

    @property
    def cors_max_age(self) -> int:
        """CORS 预检请求缓存时间(秒)"""
        return self.cors.max_age

    @property
    def base_dir(self) -> Path:
        """项目根目录"""
        return Path(__file__).resolve().parent.parent.parent

    @property
    def data_path(self) -> Path:
        """数据文件目录"""
        return self.base_dir / "data"

    @property
    def model_path(self) -> Path:
        """模型文件目录"""
        return self.base_dir / "model"

    @property
    def data_file(self) -> Path:
        """数据文件路径 - 优先使用bus_data.csv，不存在则使用airline-passengers.csv"""
        bus_data_file = self.data_path / self.auto_retrain.data_file
        if bus_data_file.exists():
            return bus_data_file
        return self.data_path / "airline-passengers.csv"

    @property
    def bus_data_file(self) -> Path:
        """滚动数据文件路径"""
        return self.data_path / self.auto_retrain.data_file

    @property
    def original_data_file(self) -> Path:
        """原始数据文件路径"""
        return self.data_path / "airline-passengers.csv"

    # ========== 配置源优先级 ==========
    @classmethod
    def settings_customise_sources(
        cls,
        settings_cls: type[BaseSettings],
        init_settings: PydanticBaseSettingsSource,
        env_settings: PydanticBaseSettingsSource,
        dotenv_settings: PydanticBaseSettingsSource,
        file_secret_settings: PydanticBaseSettingsSource,
    ) -> tuple[PydanticBaseSettingsSource, ...]:
        """
        自定义配置源优先级：
        1. 初始化参数
        2. 环境变量
        3. YAML 配置文件
        4. 默认值
        """
        yaml_source = YamlConfigSettingsSource(settings_cls)
        return (
            init_settings,
            env_settings,
            yaml_source,
            dotenv_settings,
        )


# ========== 全局配置实例 ==========
settings = AppConfig()

# 导出所有配置类
__all__ = [
    "AppConfig",
    "settings",
    "SpringProperties",
    "SpringApplicationProperties",
    "SpringProfilesProperties",
    "SpringConfigImport",
    "SpringCloudProperties",
    "NacosProperties",
    "NacosConfigProperties",
    "NacosDiscoveryProperties",
    "ServerProperties",
    "PredictionProperties",
    "JwtProperties",
    "JwtTokensProperties",
    "JwtTokenConfig",
    "JwtEndpointsProperties",
    "JwtEndpointConfig",
    "CorsProperties",
    "LoggingProperties",
    "LoggingLevelProperties",
    "LoggingFileProperties",
]
