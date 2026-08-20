# 数据聚合项目部署文档

## 服务器信息

| 项目 | 详情 |
|------|------|
| 域名 | www.3ks2u.com |
| IP | 112.124.35.167 |
| 系统 | Linux (阿里云 ECS) |
| SSH | `ssh root@112.124.35.167` |
| 项目路径 | `/opt/dataagg/` |

## 架构

```
浏览器 → Nginx (:80) → 前端静态资源 (/usr/share/nginx/html)
                      → /api/* → Backend (:8081)
                                        → MySQL (:3306)
                                        → Redis (:6379)
```

## 构建

### 前端

```bash
cd frontend
npm run build
# 产物：frontend/dist/
```

### 后端

```bash
cd backend
mvn clean package
# 产物：backend/dataagg-start/target/dataagg-start-1.0.0-SNAPSHOT.jar
```

## 部署

### 仅前端更新（常用）

```bash
# 1. 构建
cd frontend && npm run build

# 2. 上传 dist 到远程服务器
sshpass -p '<password>' scp -o StrictHostKeyChecking=no -r \
  frontend/dist/* root@112.124.35.167:/opt/dataagg/frontend/dist/

# 3. 重启 Nginx
sshpass -p '<password>' ssh -o StrictHostKeyChecking=no root@112.124.35.167 \
  "docker restart dataagg-nginx"
```

### 全量部署（前后端 + 中间件）

```bash
# 1. 构建前后端
cd frontend && npm run build
cd backend && mvn clean package

# 2. 上传构建产物到远程服务器
sshpass -p '<password>' scp -o StrictHostKeyChecking=no -r \
  frontend/dist/* root@112.124.35.167:/opt/dataagg/frontend/dist/

# 3. 通过 Docker Compose 启动所有服务
sshpass -p '<password>' ssh -o StrictHostKeyChecking=no root@112.124.35.167 \
  "cd /opt/dataagg && docker compose -f docker-compose-prod.yml up -d --build"
```

## Docker 服务

| 容器名 | 镜像 | 端口 |
|--------|------|------|
| dataagg-mysql | mysql:8.0 | 3306 |
| dataagg-redis | redis:7-alpine | 6379 |
| dataagg-backend | amazoncorretto:17-alpine | 8081 |
| dataagg-nginx | nginx:alpine | 80 |

### 常用命令

```bash
# 查看容器状态
docker ps --filter "name=dataagg"

# 查看日志
docker logs -f --tail 100 dataagg-backend
docker logs -f --tail 100 dataagg-nginx

# 重启单个服务
docker restart dataagg-nginx
docker restart dataagg-backend

# 全部重启
cd /opt/dataagg && docker compose -f docker-compose-prod.yml up -d
```

## Nginx 配置要点

- `/` — SPA fallback，`index.html` 不缓存
- `/assets/` — 静态资源长缓存（文件名带 hash）
- `/api/` — 反代到 `backend:8081`
- `/uploads/` — 上传文件，映射自 `/opt/host-uploads/`

## 环境变量

| 变量 | 说明 |
|------|------|
| `SPRING_PROFILES_ACTIVE=prod` | 后端运行环境（prod） |
| `MYSQL_ROOT_PASSWORD` | 数据库 root 密码 |
| `MYSQL_USER` / `MYSQL_PASSWORD` | 数据库应用账号 |

## 故障排查

```bash
# 检查后端健康状态
curl http://localhost:8081/health

# 检查各容器
docker ps -a --filter "name=dataagg"

# 查看最近容器日志
docker logs --tail 50 dataagg-backend
docker logs --tail 50 dataagg-nginx
```
