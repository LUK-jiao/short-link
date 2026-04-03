FROM swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/openjdk:17.0.2-slim-linuxarm64

LABEL authors="shawluke"

# 设置应用目录
WORKDIR /short-link-app

# 拷贝 jar 包到容器中
COPY target/short-link-0.0.1-SNAPSHOT.jar app.jar

# 暴露端口
EXPOSE 8081

# 运行应用
ENTRYPOINT ["java", "-jar", "app.jar"]