# 客流预测服务 (Prediction Service)

基于 FastAPI + ARIMA 模型的客流预测微服务（纯 Python 实现）。

## 功能特性

- **客流预测**：基于 ARIMA 模型预测未来客流
- **模型训练**：支持手动参数设置和自动参数优化
- **RESTful API**：完整的 HTTP API 接口
- **Swagger 文档**：自动生成 API 文档（/docs）
- **Docker 支持**：一键部署
- **微服务标准化**：与 Java 微服务生态系统完全兼容

## 微服务标准化特性

- **统一响应格式**：与 Java `Result<T>` 完全一致（code, message, data）
- **JWT 认证**：解析 JWT Token，获取 operatorId、operatorName、roles
- **权限验证**：支持基于角色的访问控制
- **Nacos 服务注册**：自动注册到 Nacos 服务发现中心
- **网关集成**：通过统一网关路由，支持健康检查端点

## 技术栈

- Python 3.11
- FastAPI
- Uvicorn
- Pandas / NumPy
- Statsmodels (ARIMA)
- Scikit-learn
- httpx (Nacos 通信)

## 目录结构

```
prediction-service/
├── app/                    # FastAPI 应用主目录
│   ├── api/               # API 路由
│   │   └── routes/
│   │       └── prediction.py
│   ├── core/              # 核心配置
│   │   ├── config.py      # 配置类
│   │   ├── logging.py     # 日志配置
│   │   ├── result.py      # 统一响应格式
│   │   ├── exceptions.py  # 异常定义
│   │   ├── jwt_handler.py # JWT 处理
│   │   ├── auth.py        # 权限验证
│   │   ├── nacos_client.py# Nacos 服务注册
│   │   └── middleware.py  # 中间件
│   ├── schemas/           # Pydantic 模型（DTO）
│   │   └── prediction.py
│   ├── services/          # 业务服务
│   │   └── prediction_service.py
│   ├── __init__.py
│   └── main.py            # 应用入口
├── prediction/            # ARIMA 预测模块
│   ├── __init__.py
│   └── arima_predictor.py
├── training/              # ARIMA 训练模块
│   ├── __init__.py
│   └── arima_trainer.py
├── utils/                 # 工具模块
│   ├── __init__.py
│   └── data_processor.py
├── data/                  # 数据文件
│   └── airline-passengers.csv
├── model/                 # 模型文件（运行时生成）
├── requirements.txt       # Python 依赖
├── Dockerfile             # Docker 构建文件
├── .dockerignore          # Docker 忽略文件
├── application.yml        # 配置文件（与 Spring Boot 一致）
├── start.py               # 启动脚本
└── README.md              # 本文档
```

## 快速开始

### 1. 安装依赖

```bash
pip install -r requirements.txt
```

### 2. 配置服务

配置文件统一使用 YAML 格式 (`application.yml`)，与 Spring Boot 保持一致：

```yaml
spring:
  application:
    name: prediction-service
  profiles:
    active: dev
  cloud:
    nacos:
      server-addr: 103.115.43.55:8848
      username: nacos
      password: nacos
      discovery:
        namespace: your-namespace-id
        group: DEFAULT_GROUP
        enabled: true

server:
  host: 0.0.0.0
  port: 8080

prediction:
  default-days: 20
  max-days: 365
  confidence-level: 0.95
```

**注意**: 只保留 `application.yml` 作为配置文件，与 Java 微服务保持一致。

### 3. 启动服务

开发模式（热重载）：
```bash
python start.py
```

或使用 uvicorn：
```bash
uvicorn app.main:app --reload --host 0.0.0.0 --port 8080
```

### 4. 访问 API 文档

启动后访问：http://localhost:8080/docs

## 微服务标准化

### 统一响应格式

与 Java `Result<T>` 完全一致：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": { }
}
```

错误码与 Java `BaseExceptionEnum` 保持一致：
- `200` - 操作成功
- `400` - 参数错误
- `401` - 登录状态已失效，请重新登录
- `403` - 当前用户无权限
- `404` - 资源不存在
- `500` - 服务器内部错误

### JWT 认证

服务自动从请求头解析 JWT Token：

```python
# 在路由中获取 JWT 上下文
from app.core.jwt_handler import get_jwt_context, require_auth

# 可选认证（不强制登录）
@app.get("/api/forecast")
async def forecast(ctx: JWTContext = Depends(get_jwt_context)):
    if ctx.is_authenticated():
        print(f"当前用户: {ctx.operator_name}")
    # ...

# 强制认证（必须登录）
@app.post("/api/train")
async def train(ctx: JWTContext = Depends(require_auth)):
    print(f"已登录用户: {ctx.operator_id}")
    # ...
```

JWT Claims 与 Java 保持一致：
- `operatorId` - 用户ID
- `operatorName` - 用户名
- `roles` - 角色列表
- `clientType` - 客户端类型

### Nacos 服务注册

服务启动时自动注册到 Nacos（配置在 `application.yml` 中）：

```yaml
spring:
  cloud:
    nacos:
      server-addr: 103.115.43.55:8848
      username: nacos
      password: nacos
      discovery:
        namespace: 80338282-5c6a-4b82-89d4-5c90adfd6997
        group: DEFAULT_GROUP
        enabled: true
```

支持功能：
- 服务自动注册
- 心跳维持（每5秒）
- 健康检查端点 `/health` 和 `/actuator/health`
- 服务优雅注销

### 网关集成

通过网关访问服务（推荐）：

```
http://gateway:8080/prediction-service/api/v1/prediction/forecast
```

JWT Token 通过网关转发，服务自动解析。

## API 接口

### 预测接口

#### POST /api/v1/prediction/forecast
预测未来客流

请求体：
```json
{
  "days": 20,
  "confidence_level": 0.95,
  "auto_train": false
}
```

响应示例（统一格式）：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "status": "success",
    "message": "成功预测未来 20 天的客流",
    "prediction_time": "2026-03-24T10:30:00+08:00",
    "days": 20,
    "predictions": [
      {
        "date": "2026-03-25",
        "predicted_passengers": 1450,
        "lower_bound": 1300,
        "upper_bound": 1600,
        "day_of_week": "Wednesday",
        "is_weekday": true
      }
    ],
    "model_info": {
      "order": [5, 1, 0],
      "aic": 1234.5,
      "bic": 1256.7
    }
  }
}
```

#### GET /api/v1/prediction/forecast
快速预测（使用默认参数）

查询参数：
- `days`: 预测天数，默认 20
- `confidence_level`: 置信水平，默认 0.95
- `auto_train`: 是否自动训练，默认 false

示例：
```bash
curl "http://localhost:8080/api/v1/prediction/forecast?days=30&auto_train=true"
```

### 训练接口

#### POST /api/v1/prediction/train
训练 ARIMA 模型

请求体：
```json
{
  "p": 5,
  "d": 1,
  "q": 0,
  "auto_optimize": true,
  "test_size": 30
}
```

#### GET /api/v1/prediction/train
快速训练（使用默认参数）

查询参数：
- `p`: 自回归阶数，默认 5
- `d`: 差分阶数，默认 1
- `q`: 移动平均阶数，默认 0
- `auto_optimize`: 是否自动寻找最优参数，默认 false
- `test_size`: 验证集大小，默认 30

示例：
```bash
curl "http://localhost:8080/api/v1/prediction/train?auto_optimize=true"
```

### 其他接口

#### GET /api/v1/prediction/model-info
获取模型信息

#### GET /api/v1/prediction/health
健康检查

#### GET /health
服务健康检查

#### GET /
服务信息

## Docker 部署

### 构建镜像

```bash
docker build -t prediction-service .
```

### 运行容器

```bash
docker run -p 8080:8080 prediction-service
```

### 使用 Docker Compose

```yaml
version: '3.8'
services:
  prediction-service:
    build: .
    ports:
      - "8080:8080"
    volumes:
      - ./application.yml:/app/application.yml  # 挂载配置文件
      - ./model:/app/model
      - ./data:/app/data
```

## 配置说明

配置文件统一使用 YAML 格式（`application.yml`），与 Spring Boot 保持一致：

### application.yml 示例

```yaml
spring:
  application:
    name: prediction-service
  profiles:
    active: dev
  cloud:
    nacos:
      server-addr: 103.115.43.55:8848
      username: nacos
      password: nacos
      config:
        namespace: 80338282-5c6a-4b82-89d4-5c90adfd6997
        group: DEFAULT_GROUP
        file-extension: yaml
      discovery:
        namespace: 80338282-5c6a-4b82-89d4-5c90adfd6997
        group: DEFAULT_GROUP
        enabled: true

server:
  host: 0.0.0.0
  port: 8080

prediction:
  default-days: 20
  max-days: 365
  confidence-level: 0.95

jwt:
  secret-key: ""
  token-header: "Bearer "

cors:
  origins:
    - "http://localhost:3000"
    - "http://localhost:8080"
```

### 配置类使用

配置文件通过 Pydantic `BaseSettings` 加载，支持 IDE 自动补全和运行时校验：

```python
from app.core.config import settings

# IDE 会自动提示可用的配置项
print(settings.app_name)      # 应用名称
print(settings.port)          # 服务器端口（int 类型）
print(settings.nacos_enabled) # Nacos 是否启用（bool 类型）
print(settings.data_path)     # 数据目录（Path 对象）

# 所有配置项都有类型注解和校验
# 非法值会在启动时报错
```

### 配置优先级

1. 初始化参数（最高优先级）
2. 环境变量
3. `application.yml` 文件（主要配置）
4. 代码默认值（最低优先级）

### 环境变量覆盖

如需覆盖特定配置，可设置对应的环境变量：

```bash
# 覆盖服务器端口
export PORT=9090

# 覆盖 Nacos 地址
export NACOS_SERVER_ADDR=localhost:8848
```

## 使用示例

### 完整流程示例

```bash
# 1. 先训练模型
curl -X POST http://localhost:8080/api/v1/prediction/train \
  -H "Content-Type: application/json" \
  -d '{"auto_optimize": true}'

# 2. 查看模型信息
curl http://localhost:8080/api/v1/prediction/model-info

# 3. 进行预测
curl -X POST http://localhost:8080/api/v1/prediction/forecast \
  -H "Content-Type: application/json" \
  -d '{"days": 30, "confidence_level": 0.95}'
```

## 注意事项

1. **首次使用**：需要先训练模型，或设置 `auto_train=true` 自动训练
2. **模型文件**：保存在 `model/` 目录下
3. **数据文件**：默认从 `data/airline-passengers.csv` 加载
4. **时区**：所有时间相关操作均使用北京时间（UTC+8）

## 许可证

MIT License
