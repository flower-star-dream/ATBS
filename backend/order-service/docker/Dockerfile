# 阶段一：构建阶段
FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder

# 设置工作目录
WORKDIR /build

# 首先复制父 pom.xml 以利用依赖缓存
COPY pom.xml .

# 复制所有子模块的 pom.xml（利用 Docker 缓存层，依赖未变更时不重新下载）
COPY order-ao/pom.xml order-ao/
COPY order-bo/pom.xml order-bo/
COPY order-api/pom.xml order-api/
COPY order-biz/pom.xml order-biz/
COPY order-common/pom.xml order-common/
COPY order-boot/pom.xml order-boot/

# 下载依赖（离线模式准备，加快后续构建）
RUN mvn dependency:go-offline -B

# 复制所有源代码
COPY order-ao/src order-ao/src/
COPY order-bo/src order-bo/src/
COPY order-api/src order-api/src/
COPY order-biz/src order-biz/src/
COPY order-common/src order-common/src/
COPY order-boot/src order-boot/src/

# 执行构建，跳过测试，打包 boot 模块
RUN mvn clean package -DskipTests -pl order-boot -am -B

# 阶段二：运行阶段（精简镜像）
FROM eclipse-temurin:17-jre-alpine

# 设置时区为东八区（可选）
ENV TZ=Asia/Shanghai

# 创建非 root 用户运行应用（安全最佳实践）
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# 设置工作目录
WORKDIR /app

# 从构建阶段复制生成的 jar 包（假设 artifactId 与模块名一致）
COPY --from=builder /build/order-boot/target/*.jar app.jar

# 切换用户
USER appuser

# 暴露端口（根据你的应用调整）
EXPOSE 8083

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]