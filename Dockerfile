FROM openjdk:17.0.2-slim

LABEL authors="shawluke"

# 设置应用目录
WORKDIR /app

# 拷贝 jar 包到容器中
COPY target/short-link-0.0.1-SNAPSHOT.jar app.jar

# 暴露端口（实际端口由 SERVER_PORT 环境变量决定）
EXPOSE 8080

# 运行应用
ENTRYPOINT ["java", "-jar", "app.jar"]