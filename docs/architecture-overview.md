# 数据聚合项目 — 体系框架与架构

## 项目概况

**数据聚合智能AI**，一站式多维度数据聚合平台，覆盖股票行情、电子书集、博客内容、行政区划等数据域。B/S 架构，前后端分离，Docker Compose 容器化部署。

---

## 技术栈

### 后端

| 组件 | 版本 | 用途 |
|------|------|------|
| Java | **17** | 运行环境 |
| Spring Boot | **3.2.5** | 应用框架 |
| Spring Cloud Alibaba | **2023.0.1.0** | 微服务集成（含 RocketMQ） |
| MyBatis Plus | **3.5.7** | ORM |
| MySQL | **8.0** | 主数据库 |
| Redis | **7.x (Alpine)** | 二级缓存 |
| Caffeine | **3.x** | 一级本地缓存 |
| RocketMQ | **5.1.4** (client **2.3.0**) | 消息队列 |
| JJWT | **0.12.6** | JWT 鉴权 |
| Spring Security Crypto | **6.x** | BCrypt 密码加密 |
| jsoup | **1.18.1** | HTML 解析（URL 抓取） |
| HikariCP | (Spring Boot 内置) | 数据库连接池 (max=20) |
| Lettuce | (Spring Boot 内置) | Redis 客户端 (max-active=8) |

### 前端

| 组件 | 版本 | 用途 |
|------|------|------|
| Vue | **3.5.x** | 前端框架 |
| TypeScript | **5.6.x** | 类型安全 |
| Vite | **5.x** | 构建工具 |
| Vue Router | **4.6.x** | 路由 |
| Pinia | **3.0.x** | 状态管理 |
| Element Plus | **2.13.x** | UI 组件库 |
| Axios | **1.15.x** | HTTP 客户端 |
| Sass | **1.77.x** | CSS 预处理 |
| markdown-it | **14.x** | Markdown 渲染 |
| highlight.js | **11.11.x** | 代码高亮 |

### 基础设施

| 组件 | 版本 |
|------|------|
| Nginx | Alpine (latest) |
| Docker Compose | **3.8** |
| SSL | Let's Encrypt |
| 部署服务器 | 112.124.35.167 (1.7G RAM, 40G 磁盘) |

---

## 模块架构

```
data-aggregate/
├── backend/
│   ├── dataagg-common/    公共模块：Entity、DTO、Result 封装
│   ├── dataagg-service/   业务模块：Mapper、Service、数据库交互
│   └── dataagg-start/     启动模块：Controller、Config、Scheduler、Cache
└── frontend/
    └── src/
        ├── api/            API 封装层
        ├── components/     通用组件
        ├── stores/         Pinia 状态管理
        ├── views/          页面（home, blog, stock, ebook, admin...）
        ├── router/         路由配置
        └── utils/          工具（request 拦截器、JWT、缓存）
```

### 后端分层

```
Controller (REST API)
    ↓
Service (业务逻辑)
    ↓
Mapper (MyBatis-Plus BaseMapper)
    ↓
MySQL (持久化)
```

### 缓存架构

```
请求 → Caffeine L1 (内存, per-key TTL)
         ↓ miss
       Redis L2 (TTL 5-120min)
         ↓ miss
       MySQL
```

L2 TTL 配置：categories(60min) / blogPages(15min) / blogs(10min) / users(30min) / siteConfig(120min) / marketOverview(2min)

---

## 主要功能模块

### 1. 首页概览
- 市场概况：实时指数（8 个主要指数）、快讯、热点题材
- 资金面：北向资金、融资融券
- 龙虎榜、连板天梯、股东户数
- 数据来源：东方财富、腾讯、财联社

### 2. 股票数据
- 指数行情、K线、质量评分、全球新闻
- 三层缓存（Caffeine + Redis + DB）、定时调度采集

### 3. 博客
- 文章 CRUD、Markdown 编辑、AI 封面生成（阿里百炼）
- URL 抓取导入、微信公众号兼容
- 分类管理、点赞收藏、评论
- 代码高亮、图片缩略图/WebP

### 4. 电子书集
- 图书分类检索、收藏下载
- 定期同步监控

### 5. 用户系统
- 用户名/密码注册登录（BCrypt + 邮箱验证码）
- 微信 OAuth 登录
- JWT 双令牌方案（Access 2h + Refresh 7d）
- 修改密码、角色管理

### 6. 后台管理
- 站点设置（名称、导航菜单、描述等动态配置）
- 分类管理、性能监控、定时任务面板

---

## 系统性能

### 优化后基线 (pageSize=6, gzip, keep-alive)

| 并发 | QPS | p50 | p95 | 失败 |
|------|-----|-----|-----|------|
| C=10 | **52** | 133ms | 449ms | 0 |
| C=50 | **34** | 672ms | 2565ms | 0 |
| C=100 | **33** | 1255ms | 5066ms | 0 |

### 优化措施

| 措施 | 效果 |
|------|------|
| Caffeine L1 内存缓存 | 避免 Redis 网络 IO |
| blog list summary 截断 | 响应体 125KB→13KB (90%) |
| nginx gzip | JSON 再压缩 58% |
| Caffeine.get 并发保护 | 防缓存雪崩 |
| summary 截断 + gzip 后总压缩比 | 125KB → 4KB (97%) |

### 瓶颈

- 服务器 ~300M 空闲内存，33 QPS 触 CPU 天花板
- 高并发下需升配内存和 CPU

---

## 外部集成

| 服务 | 用途 |
|------|------|
| 东方财富 API | 快讯、全球新闻、资金面 |
| 腾讯 qt.gtimg.cn | 指数行情 |
| 财联社 cls.cn | 电报快讯 |
| 微信开放平台 | OAuth 扫码登录 |
| 阿里百炼 DashScope | AI 封面生成 |
| QQ 邮箱 SMTP | 验证码发送 |
| NewsData.io | 新闻聚合 |
| Let's Encrypt | SSL 证书 |
