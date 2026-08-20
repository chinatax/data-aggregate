# 2026-06-01 变更记录

## Bug 修复

### 1. 首页/博客无数据（严重）

**现象**: 所有页面数据为空，菜单显示旧默认值"首页概览"、"行政区划"

**根因**:
- 前端所有 API URL 缺少 `/api/` 前缀（如 `/blog/list` → 应 `/api/blog/list`）
- 开发时 Vite proxy 掩盖问题，生产 nginx 无前缀请求走 SPA fallback 返回 HTML
- 拦截器 `return res` 未解包 `Result<T>` 包装

**修复**: 12 个文件加 `/api/` 前缀；拦截器改为 `return res.data ?? res`

### 2. 微信登录"用户不存在"

**根因**: `AuthInterceptor` 用 `claims.get("userId")` 读 JWT，但 `JwtUtil` 把 userId 存在 `subject` 里

**修复**: 改为 `claims.getSubject()`

### 3. 东财快讯链接 404

**根因**: 前端 URL 格式 `kuaixun.eastmoney.com/{code}.html` 无效

**修复**: 改为 `finance.eastmoney.com/a/{code}.html`

### 4. 市场指数只剩 1 个

**根因**: 腾讯 API 返回的指数 code 带 `\n` 前缀，MAIN_INDEX_CODES 过滤几乎全失败

**修复**: 解析时加 `.trim()`

### 5. MySQL max_allowed_packet 仅 2KB

**根因**: MySQL 容器配置异常，max_allowed_packet=2048，博客查询包 4974 字节超限

**修复**: `SET GLOBAL max_allowed_packet=16M` + docker-compose 持久化

## 新功能

### 修改密码

- 后端: `PUT /api/user/change-password`（验证旧密码、BCrypt 加密新密码）
- 前端: 登录下拉菜单新增"修改密码"入口 + 弹窗组件
- 微信用户（无密码）提示"当前账号未设置密码"

### 性能优化

- MultiLevelCache 加 Caffeine L1 内存缓存（L1→Redis→DB 三级）
- blog-list summary 截断 150 字符（响应 125KB→13KB）
- nginx gzip 压缩 JSON（压缩率 ~58%）
- 并发加载保护（Caffeine.get 内置 stampede prevention）
- blogPages L2 TTL 5min→15min
- stock-market 数据削减（20 指数→8，30 快讯→8）

**blog-list**: 3 QPS / 3s → 33 QPS / 180ms（10.5x）
**stock-market**: 12 QPS / 779ms → 28 QPS / 242ms（2.4x）

## 修改文件清单

| 文件 | 变更 |
|------|------|
| `backend/.../cache/MultiLevelCache.java` | 新增 Caffeine L1 缓存 + 并发保护 |
| `backend/.../config/CacheBuilderConfig.java` | 各缓存 L1/L2 TTL 配置 |
| `backend/.../config/AuthInterceptor.java` | 修复 compile 错误 + 修复 userId 读取 |
| `backend/.../controller/BlogController.java` | summary 截断 |
| `backend/.../controller/StockV2Controller.java` | market VO 缓存 + 数据削减 |
| `backend/.../controller/UserController.java` | 新增 change-password 端点 |
| `backend/.../service/StockDataService.java` | indices L1 + code trim |
| `frontend/src/utils/request.ts` | 拦截器解包 Result 包装 |
| `frontend/src/api/settings.ts` | 修正 URL 前缀 |
| `frontend/src/api/auth.ts` | 修正 URL 前缀 + 新增 changePasswordApi |
| `frontend/src/api/stock.ts` | 修正 URL 前缀 + 返回类型标注 |
| `frontend/src/api/*.ts` | 批量修正 URL 前缀 |
| `frontend/src/stores/auth.ts` | 移除死导入 refreshTokenApi |
| `frontend/src/views/home/index.vue` | 修正东财 URL |
| `frontend/src/views/blog/*.vue` | 批量修正 URL 前缀 |
| `frontend/src/components/AppHeader.vue` | 修改密码入口 + 去掉邮箱 |
| `frontend/src/components/ChangePasswordDialog.vue` | 新建弹窗组件 |
| `nginx.conf` | gzip 压缩 |
| `docker-compose-prod.yml` | MySQL max_allowed_packet |

## 后续修复（当晚）

### 博客列表返回后分页错乱

**根因**: `onActivated` 重取第 1 页但 `pageNum` 未重置 → 滚动时跳到旧页码 → `hasMore` 提前 false

**修复**: `loadArticles()` 不加 append 时自动 `pageNum = 1`

### Axios 超时 30s → 60s

服务器资源紧张时冷缓存查询偶发超 30s

### 隐藏文章不弹错误提示

`Result.error("文章不存在")` → `Result.success(null)`

### 其他小改

- blog #11 隐藏 (status=PRIVATE), blog #12 作者改为 admin
- 登录下拉去掉邮箱显示
- 修改密码成功不弹提示

### 博客编辑器发布 405

**根因**: 之前 sed 批量加 `/api/` 匹配 `request.post('/blog/'`（带斜杠），漏了编辑器里 `request.post('/blog'`（不带斜杠）
**修复**: 手动补上 `/api/` 前缀
