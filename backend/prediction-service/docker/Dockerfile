# 客流预测服务 - FastAPI Docker 镜像
FROM python:3.11-slim

# 设置工作目录
WORKDIR /app

# 安装系统依赖（使用阿里云镜像加速，解决 Windows 上网络慢的问题）
RUN sed -i 's/deb.debian.org/mirrors.aliyun.com/g' /etc/apt/sources.list.d/debian.sources \
    && apt-get update \
    && apt-get install -y --no-install-recommends gcc g++ \
    && rm -rf /var/lib/apt/lists/*

# 复制依赖文件
COPY prediction-service/requirements.txt .

# 安装 Python 依赖
RUN pip install --no-cache-dir -r requirements.txt

# 复制应用代码
COPY prediction-service/app ./app/
COPY prediction-service/prediction ./prediction/
COPY prediction-service/training ./training/
COPY prediction-service/utils ./utils/

# 复制配置文件
COPY prediction-service/application.yml ./

# 复制数据文件
COPY prediction-service/data ./data/

# 创建模型目录
RUN mkdir -p model

# 暴露端口
EXPOSE 8085

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8085/health')" || exit 1

# 启动命令
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8085"]
