# short-link

短链系统（Spring Boot）项目说明文档。  
本文档覆盖两种启动方式：

1. 本地开发启动（IDEA / 命令行）
2. Docker Compose 一键启动（含多实例）

---

## 1. 项目结构（关键文件）

- `pom.xml`：Maven 依赖管理
- `src/main/resources/application.yml`：主配置
- `src/main/resources/application-dev.yml`：本地开发配置
- `src/main/resources/application-docker.yml`：Docker 运行配置
- `Dockerfile`：应用镜像构建文件
- `docker-compose.yml`：容器编排（zookeeper/kafka/nginx/shortlink 多实例）

---

## 2. 环境要求

### 2.1 本地启动需要

- JDK 17（建议与项目编译版本一致）
- Maven 3.8+
- MySQL（或你配置文件中对应的数据库）
- Redis
- Kafka（如果业务路径依赖消息队列）
- Git

### 2.2 Docker 启动需要

- Docker
- Docker Compose（或 `docker compose` 插件）
- Git

> 说明：你当前 `docker-compose.yml` 已包含 `zookeeper`、`kafka`、`nginx`、`shortlink1~3`。  
> MySQL / Redis 是否由 Compose 托管，取决于你的配置文件和 compose 实际内容；若未定义，需要你本机或外部先准备好。

---

## 3. 从拉取代码开始（通用）

```bash
git clone <你的仓库地址>.git
cd short-link
```

---

## 4. 本地启动（推荐先跑通）

## 4.1 准备中间件

请确保以下服务可连接（地址/端口以 `application-dev.yml` 为准）：

- MySQL
- Redis
- Kafka（及其 Zookeeper，如你使用传统模式）

可用你自己的本地安装，也可使用容器单独启动这些依赖。

## 4.2 安装依赖并编译

```bash
./mvnw clean package -DskipTests
```

如果是首次构建较慢，属正常现象（会下载 Maven 依赖）。

## 4.3 启动应用（dev 环境）

### 方式 A：命令行启动

```bash
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

或启动打包后的 jar：

```bash
java -jar target/short-link-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

### 方式 B：IDEA 启动

1. 用 IDEA 打开项目
2. 等待 Maven 依赖导入完成
3. 找到启动类（通常是 `ShortLinkApplication`）
4. VM options / Program args 中指定：
   - `--spring.profiles.active=dev`
5. 点击 Run

## 4.4 本地启动验证

- 查看日志是否出现 `Started ... in ... seconds`
- 访问你配置的服务端口（以 `application*.yml` 中 `server.port` 为准）
- 若有健康检查端点，可测试：
  - `GET /actuator/health`（前提是已开启 actuator）

---

## 5. Docker 启动（Compose，多实例）

## 5.1 构建应用镜像

在项目根目录执行：

```bash
./mvnw clean package -DskipTests
docker build -t short-link-app:2.0 .
```

> 镜像 tag 请与你的 `docker-compose.yml` 中 `image: short-link-app:2.0` 保持一致。

## 5.2 检查 Compose 配置

你当前 Compose 中有：

- `zookeeper`
- `kafka`
- `nginx`
- `shortlink1`
- `shortlink2`
- `shortlink3`

并通过环境变量指定：

- `SPRING_PROFILES_ACTIVE=docker`
- `WORKER_ID=1/2/3`

启动前重点确认：

1. `application-docker.yml` 中 MySQL/Redis/Kafka 地址是否指向容器可达地址  
   - 容器间访问建议用服务名（如 `kafka`），不要写 `localhost`
2. `nginx` 配置文件挂载路径是否存在：  
   - `~/nginx/conf/nginx.conf:/etc/nginx/nginx.conf`
3. 如果项目依赖 MySQL/Redis，但 compose 未定义对应服务，请先单独提供并保证网络可达

## 5.3 启动 Compose

```bash
docker compose up -d
```

如果你的环境是旧版插件，也可用：

```bash
docker-compose up -d
```

## 5.4 查看运行状态与日志

```bash
docker compose ps
docker compose logs -f shortlink1
docker compose logs -f shortlink2
docker compose logs -f shortlink3
```

查看 Kafka / Nginx：

```bash
docker compose logs -f kafka
docker compose logs -f nginx
```

## 5.5 访问与验证

- 通过 Nginx 暴露端口访问（你当前是 `80:80`）
- 多发几次请求，观察是否由 3 个 `shortlink` 实例共同处理
- 检查应用日志中数据库、Redis、Kafka 连接是否成功

---

## 6. 停止与清理

停止服务：

```bash
docker compose down
```

停止并删除卷（慎用）：

```bash
docker compose down -v
```

---

## 7. 常见问题排查

### 7.1 容器内连不上中间件

- 不要在容器配置里写 `localhost` 指向其他容器
- 使用 Compose 服务名（如 `kafka`、`mysql`、`redis`）
- 确认端口与协议一致

### 7.2 `shortlink` 启动失败

- 先看日志：
  - `docker compose logs -f shortlink1`
- 常见原因：
  - profile 配置文件缺失/参数错误
  - 数据库账号密码错误
  - Redis/Kafka 地址不可达
  - Nginx 配置语法错误导致网关不可用

### 7.3 Kafka 可用但应用消费不到消息

- 检查 `bootstrap-servers` 是否与容器网络一致
- 检查 topic 是否存在、消费者组是否正确
- 检查应用 profile 是否确实为 `docker`

### 7.4 本地和 Docker 配置混用

- 本地运行用 `dev`
- 容器运行用 `docker`
- 避免把本地 `localhost` 配置带到容器 profile

---

## 8. 推荐启动顺序

1. 先本地 `dev` 跑通
2. 再构建镜像并起单实例容器验证
3. 最后通过 Compose 拉起 3 实例 + Nginx 做完整联调

---

## 9. 一组最小可用命令（复制即用）

```bash
git clone https://github.com/LUK-jiao/short-link.git
cd short-link
./mvnw clean package -DskipTests
docker build -t short-link-app:2.0 .
docker compose up -d
docker compose ps
docker compose logs -f shortlink1
```

---

## 10. 短链接使用说明（用户指南）

### 创建短链接

在终端或命令行工具中执行：

```bash
curl -X POST http://studywithus.dpdns.org/createBatch \
  -H "Content-Type: application/json" \
  -d '{"longUrl":"你的原始网址"}'
```

**示例：**
```bash
curl -X POST http://studywithus.dpdns.org/createBatch \
  -H "Content-Type: application/json" \
  -d '{"longUrl":"https://www.baidu.com"}'
```

**返回结果：**
```json
{"success":true,"message":"send short link :zhIdWLGHg0"}
```

**记住短码**：`zhIdWLGHg0`（冒号后面的部分）

---

### 使用短链接

在浏览器地址栏输入：

```
http://studywithus.dpdns.org/protect/zhIdWLGHg0
```

**原理**：把短码拼在 `/protect/` 后面，浏览器会自动跳转到原始网址。

---

### 总结

| 你做的事 | 结果 |
|---------|------|
| 发送命令创建短链接 | 得到一个短码（如 `zhIdWLGHg0`） |
| 浏览器打开 `http://studywithus.dpdns.org/protect/短码` | 自动跳转到原始网址 |

---

### 注意事项

- 短链接有效期 **24 小时**，过期失效
- 原始网址要完整，包含 `https://` 或 `http://`

