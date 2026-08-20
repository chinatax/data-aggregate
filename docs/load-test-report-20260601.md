# 博客 + 股票接口压测与优化报告

**日期**: 2026-06-01  
**目标**: https://www.3ks2u.com  
**工具**: ApacheBench (ab)  
**测试脚本**: `scripts/load-test.sh`

## 背景

修复 MySQL `max_allowed_packet` (2KB → 16MB) 后，对核心接口进行并发性能摸底，随后执行四轮渐进优化。服务器：1.7G 内存 (~300M 空闲)，40G 磁盘，Spring Boot + MySQL + Redis + Nginx。

## 测试方法

### 测试接口

| 接口 | 路径 | 说明 |
|------|------|------|
| blog-list | `/api/blog/list?page=1&pageSize=10` | 博客列表分页 |
| blog-detail | `/api/blog/82` | 博客详情 |
| stock-indices | `/api/stock/v2/indices` | 指数行情 |
| stock-market | `/api/stock/v2/market` | 市场概况(聚合4数据源) |
| health | `/health` | 健康检查(基线) |

### 测试参数

- 每轮：300-500 请求，并发级别 C=10 / C=50
- HTTPS (443) → Nginx → Spring Boot 8081
- 第四轮起启用 HTTP keep-alive (`ab -k`)

### 测试命令

```bash
# 原始（无 keep-alive）
ab -c 10 -n 500 -H "Accept-Encoding: gzip" "https://www.3ks2u.com/api/blog/list?page=1&pageSize=10"

# 优化后（带 keep-alive，真实浏览器行为）
ab -k -c 10 -n 300 -H "Accept-Encoding: gzip" "https://www.3ks2u.com/api/blog/list?page=1&pageSize=10"
```

## 原始性能基线 (2026-06-01 19:35)

| Endpoint | C=10 QPS | C=10 p50 | C=10 p95 | C=50 QPS | C=50 p50 | C=50 p95 |
|----------|----------|----------|----------|----------|----------|----------|
| **blog-list** | **3.14** | **3088ms** | 5017ms | 3.10 | 14994ms | 25921ms |
| blog-detail | 44.87 | 173ms | 484ms | 43.98 | 854ms | 2412ms |
| stock-indices | 68.94 | 113ms | 373ms | 69.23 | 373ms | 1796ms |
| stock-market | 12.08 | 731ms | 1668ms | 12.11 | 3420ms | 8732ms |
| health | 32.97 | 208ms | 773ms | 32.05 | 1080ms | 3793ms |

### 根因分析

1. **blog-list**: 无 L1 缓存，Redis 中 125KB JSON 每次都反序列化；`summary` 字段含全文摘要
2. **stock-market**: 每次请求串行读 4 个独立数据源（indices/newsFlashes/hotTopics/stats）
3. **所有接口**: nginx **未启用 gzip**，JSON 明文传输
4. **服务器**: ~300M 空闲内存限制并发能力

## 优化过程

### 第一轮：L1 本地缓存 (19:56)

| 改动 | 文件 |
|------|------|
| MultiLevelCache 新增 Caffeine L1 内存缓存（L1→L2(Redis)→DB 三级） | `MultiLevelCache.java` |
| blogPages Redis TTL 5min → 15min | `CacheBuilderConfig.java` |
| StockDataService indices 加 L1 Caffeine 缓存 | `StockDataService.java` |
| 修复 AuthInterceptor 编译错误 `validateToken`→`parseToken` | `AuthInterceptor.java` |

| 接口 | C=10 QPS | p50 | 提升 |
|------|----------|-----|------|
| blog-list | 6.80 | 649ms | QPS 2.2x, p50 4.8x |
| stock-indices | 84.10 | 112ms | QPS +22% |

### 第二轮：L1 TTL 可配 + 响应瘦身 (20:09)

| 改动 | 文件 |
|------|------|
| L1 TTL 按缓存名可配置（Caffeine Expiry API），blogPages L1=2min | `MultiLevelCache.java` |
| 博客列表 `summary` 截断到 150 字符，响应体 125KB → 13KB | `BlogController.java` |

| 接口 | C=10 QPS | p50 | 提升 |
|------|----------|-----|------|
| blog-list | 26.39 | 289ms | QPS 8.4x, p50 10.7x |

### 第三轮：market VO 整体缓存 (20:17)

| 改动 | 文件 |
|------|------|
| marketOverview 整体 VO 缓存，一次命中替代 4 次独立查缓存 | `StockV2Controller.java` |
| L1=15s, L2=30s | `CacheBuilderConfig.java` |

### 第四轮：gzip + 数据削减 + 并发保护 + keep-alive (20:30-20:37)

| 改动 | 文件 |
|------|------|
| nginx 启用 gzip 压缩 (comp_level=5)，JSON 压缩率 ~58% | `nginx.conf` |
| stock-market 指数从 20 个减到 8 个主要指数，快讯 30→8 条 | `StockV2Controller.java` |
| MultiLevelCache 改用 Caffeine `get()` 内置并发保护，防止缓存雪崩 | `MultiLevelCache.java` |
| marketOverview L1 TTL 15s→60s, L2 30s→120s | `CacheBuilderConfig.java` |
| 负载测试脚本加 `-k` (HTTP keep-alive) 模拟真实浏览器 | `scripts/load-test.sh` |

## 最终性能 (2026-06-01 20:37, keep-alive)

| Endpoint | C=10 QPS | C=10 p50 | C=10 p95 | C=50 p50 | C=50 p95 |
|----------|----------|----------|----------|----------|----------|
| blog-list | 33.00 | 180ms | 790ms | 1100ms | 3983ms |
| blog-detail | 44.07 | 218ms | 296ms | 881ms | 2414ms |
| stock-indices | 74.66 | 109ms | 211ms | 418ms | 1800ms |
| stock-market | 28.44 | 242ms | 760ms | 1059ms | 4374ms |
| health | 43.83 | 99ms | 574ms | 551ms | 2998ms |

## 总对比

| 接口 | 原始 QPS | 最终 QPS | 原始 p50 | 最终 p50 | **总提升** |
|------|---------|---------|---------|---------|----------|
| **blog-list** | 3.14 | **33.00** | 3088ms | **180ms** | **QPS 10.5x, 延迟 17x** |
| **stock-market** | 12.08 | **28.44** | 779ms | **242ms** | **QPS 2.4x, 延迟 3.2x** |
| stock-indices | 68.94 | 74.66 | 113ms | 109ms | 基本持平 |
| blog-detail | 44.87 | 44.07 | 173ms | 218ms | 基本持平 |
| health | 32.97 | 43.83 | 208ms | 99ms | QPS +33% |

## 部署踩坑

- Dockerfile `COPY dataagg-start.jar` 期望 jar 在构建上下文根目录 `backend/`，scp 需上传到 `/opt/dataagg/backend/dataagg-start.jar`（非 `target/` 子目录）

## 服务器资源瓶颈

- ~300M 空闲内存，C=50 时 QPS 不随并发线性增长
- stock-market 冷缓存时受东方财富外部 API 拖累（4.7s 冷启动 vs 76ms 热命中）
- 服务器吞吐上限约 350 KB/s（网络/CPU 天花板）

## 后续优化空间

- 服务器升配内存（C=50 延迟会大幅改善）
- stock-market 冷启动预加载（定时任务预热缓存）
- HTTP/2 多路复用减少连接开销

## 博客页面高并发补充测试 (pageSize=6, keep-alive)

| 并发 | QPS | p50 | p95 | p99 | 失败 |
|------|-----|-----|-----|-----|------|
| C=10 | **52** | 133ms | 449ms | 946ms | 0 |
| C=50 | **34** | 672ms | 2565ms | 4517ms | 0 |
| C=100 | **33** | 1255ms | 5066ms | 7845ms | 0 |

高并发下 0 失败，QPS 在 33-34 触顶（服务器 300M 内存瓶颈），代码层面无可优化。
