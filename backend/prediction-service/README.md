# 客流预测服务 (Prediction Service)

基于 FastAPI + ARIMA 模型的客流预测微服务（纯 Python 实现）。

## 功能特性

- **客流预测**：基于 ARIMA 模型预测未来客流
- **模型训练**：支持手动参数设置和自动参数优化
- **RESTful API**：完整的 HTTP API 接口
- **Swagger 文档**：自动生成 API 文档（/docs）
- **Docker 支持**：一键部署

## 技术栈

- Python 3.11
- FastAPI
- Uvicorn
- Pandas / NumPy
- Statsmodels (ARIMA)
- Scikit-learn

## 目录结构

```
prediction-service/
├── app/                    # FastAPI 应用主目录
│   ├── api/               # API 路由
│   │   └── routes/
│   │       └── prediction.py
│   ├── core/              # 核心配置
│   │   ├── config.py      # 配置类
│   │   └── logging.py     # 日志配置
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
├── .env                   # 环境变量
├── .env.example           # 环境变量示例
├── start.py               # 启动脚本
└── README.md              # 本文档
```

## 快速开始

### 1. 安装依赖

```bash
pip install -r requirements.txt
```

### 2. 启动服务

开发模式（热重载）：
```bash
python start.py
```

或使用 uvicorn：
```bash
uvicorn app.main:app --reload --host 0.0.0.0 --port 8080
```

### 3. 访问 API 文档

启动后访问：http://localhost:8080/docs

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

响应示例：
```json
{
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
    environment:
      - APP_ENV=production
      - PORT=8080
    volumes:
      - ./model:/app/model
      - ./data:/app/data
```

## 配置说明

通过环境变量或 `.env` 文件配置：

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| APP_ENV | development | 应用环境 (development/production) |
| HOST | 0.0.0.0 | 监听地址 |
| PORT | 8080 | 监听端口 |
| DEFAULT_DAYS | 20 | 默认预测天数 |
| MAX_DAYS | 365 | 最大预测天数 |
| CONFIDENCE_LEVEL | 0.95 | 默认置信水平 |

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
