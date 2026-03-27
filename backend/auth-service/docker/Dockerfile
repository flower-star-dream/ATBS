# 阶段一：构建阶段
FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder

WORKDIR /build

# 复制父 pom
COPY pom.xml .

# 复制所有模块的 pom.xml（保持目录结构，Maven 需要检查 modules）
COPY atbs-gateway/pom.xml atbs-gateway/
COPY atbs-tools/pom.xml atbs-tools/
COPY auth-service/pom.xml auth-service/
COPY auth-service/auth-api/pom.xml auth-service/auth-api/
COPY auth-service/auth-ao/pom.xml auth-service/auth-ao/
COPY auth-service/auth-biz/pom.xml auth-service/auth-biz/
COPY auth-service/auth-bo/pom.xml auth-service/auth-bo/
COPY auth-service/auth-boot/pom.xml auth-service/auth-boot/
COPY auth-service/auth-common/pom.xml auth-service/auth-common/
COPY user-service/pom.xml user-service/
COPY user-service/user-api/pom.xml user-service/user-api/
COPY user-service/user-ao/pom.xml user-service/user-ao/
COPY user-service/user-biz/pom.xml user-service/user-biz/
COPY user-service/user-bo/pom.xml user-service/user-bo/
COPY user-service/user-boot/pom.xml user-service/user-boot/
COPY user-service/user-common/pom.xml user-service/user-common/
COPY order-service/pom.xml order-service/
COPY order-service/order-api/pom.xml order-service/order-api/
COPY order-service/order-ao/pom.xml order-service/order-ao/
COPY order-service/order-biz/pom.xml order-service/order-biz/
COPY order-service/order-bo/pom.xml order-service/order-bo/
COPY order-service/order-boot/pom.xml order-service/order-boot/
COPY order-service/order-common/pom.xml order-service/order-common/
COPY airplane-service/pom.xml airplane-service/
COPY airplane-service/airplane-api/pom.xml airplane-service/airplane-api/
COPY airplane-service/airplane-ao/pom.xml airplane-service/airplane-ao/
COPY airplane-service/airplane-biz/pom.xml airplane-service/airplane-biz/
COPY airplane-service/airplane-bo/pom.xml airplane-service/airplane-bo/
COPY airplane-service/airplane-boot/pom.xml airplane-service/airplane-boot/
COPY airplane-service/airplane-common/pom.xml airplane-service/airplane-common/

# 本地构建命令（需要 Docker BuildKit）：
#   Windows: set DOCKER_BUILDKIT=1 & docker build --secret id=settings,src=%USERPROFILE%\.m2\settings.xml -f auth-service/docker/Dockerfile -t auth-service .
#   Linux:   DOCKER_BUILDKIT=1 docker build --secret id=settings,src=$HOME/.m2/settings.xml -f auth-service/docker/Dockerfile -t auth-service .
# 云效流水线：不需要加 --secret

# 安装父POM（不构建子模块）
RUN --mount=type=secret,id=settings,dst=/root/.m2/settings.xml,required=false \
    mvn install -N -B -Drevision=release-1.0.0

# 下载依赖（只处理 auth-boot，依赖从私库拉取）
RUN --mount=type=secret,id=settings,dst=/root/.m2/settings.xml,required=false \
    mvn dependency:go-offline -B -pl auth-service/auth-boot -Drevision=release-1.0.0

# 复制 auth-service 源码（只构建这个模块）
COPY auth-service/auth-api/src/ auth-service/auth-api/src/
COPY auth-service/auth-ao/src/ auth-service/auth-ao/src/
COPY auth-service/auth-biz/src/ auth-service/auth-biz/src/
COPY auth-service/auth-bo/src/ auth-service/auth-bo/src/
COPY auth-service/auth-boot/src/ auth-service/auth-boot/src/
COPY auth-service/auth-common/src/ auth-service/auth-common/src/

# 构建（只构建 auth-boot，依赖从私库拉取）
RUN --mount=type=secret,id=settings,dst=/root/.m2/settings.xml,required=false \
    mvn clean package -DskipTests -pl auth-service/auth-boot -B -Drevision=release-1.0.0

# 阶段二：运行阶段
FROM eclipse-temurin:17-jre-alpine
ENV TZ=Asia/Shanghai
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app
COPY --from=builder /build/auth-service/auth-boot/target/*.jar app.jar
USER appuser
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
