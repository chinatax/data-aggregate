# 首页/博客无数据问题排查报告

**日期**: 2026-06-01  
**现象**: 首页、博客页均无数据，菜单显示旧默认值（"首页概览"、"行政区划"）

## 根因

两个独立 bug 叠加：

### 1. 前端 API URL 全部缺少 `/api/` 前缀

**影响范围**: 所有页面

前端代码中请求 URL 格式为 `/stock/v2/market`、`/blog/list`，缺少 `/api/` 前缀。开发时 Vite proxy 会转发 `/api/*` 到后端，但生产环境 nginx 只配置了 `location /api/` 代理。无前缀的请求匹配 `location /` → `try_files $uri /index.html`，全部返回 SPA 的 HTML 壳子。

**受影响的 12 个文件**:
- `api/settings.ts`, `api/stock.ts`, `api/category.ts`, `api/bookMonitor.ts`, `api/monitor.ts`, `api/comment.ts`, `api/ebook.ts`, `api/auth.ts`
- `views/blog/index.vue`, `views/blog/detail.vue`, `views/blog/editor/index.vue`, `views/blog/favorites.vue`

### 2. 请求拦截器未解包 `Result<T>` 包装

**文件**: `utils/request.ts`

后端统一返回 `Result.success(data)` → `{code: 200, message: "success", data: {...}}`。拦截器原代码 `return res` 返回完整包装体，但前端代码直接访问 `res.records` 而非 `res.data.records`。

**修复**: `return res` → `return res.data ?? res`

### 为什么开发时没问题？

Vite dev server 配置了 proxy：`/api` → `http://localhost:8081`。但这只能代理以 `/api` 开头的请求。开发时如果访问不带 `/api/` 前缀的 URL，同样会失败——说明开发时从未完整测试过前端页面。

## 排查过程

| 步骤 | 发现 | 结论 |
|------|------|------|
| 1. curl 测试后端 API | `https://www.3ks2u.com/api/blog/list` 返回 200 + JSON | 后端正常 |
| 2. 检查前端 JS 是否加载 | `index-*.js` 返回 200 | 静态资源正常 |
| 3. 怀疑浏览器缓存 | 确认 `index.html` 已更新 | 非缓存 |
| 4. 对比本地/远端 JS MD5 | 完全一致 | 部署正确 |
| 5. 检查编译后 JS 中的 API URL | 发现缺 `/api/` 前缀 | **找到根因 1** |
| 6. 测试 `/stock/v2/market` | 返回 HTML | 确认 URL 错误 |
| 7. 检查拦截器返回值 | `return res` 返回完整包装 | **找到根因 2** |

## 经验教训

1. **生产环境和开发环境不等价**: Vite proxy 在开发时掩盖了 URL 前缀问题。每次前端改 API 调用后，必须确认生产 nginx 能正确代理。

2. **前后端数据包装要一致**: 后端用 `Result<T>` 包装，拦截器必须解包。否则每个调用方都要手动 `.data.data`。

3. **排查"页面无数据"的标准流程**:
   - 打开浏览器 DevTools → Network 面板
   - 看 XHR 请求的 Response 是 JSON 还是 HTML
   - 如果是 HTML，URL 一定有问题（没走 nginx 代理）
   - 如果是 JSON 但数据不对，检查拦截器/包装层

4. **curl 验证 API 要用正确 URL**: 测试 API 时用了正确的 `/api/...` 路径，所以看到数据正常，误以为后端没问题。应该用前端实际调用的 URL（不带 `/api/`）测试才能发现真正的 bug。

5. **不要轻易改动拦截器**: 最初 `return res` 改为 `return res.data ?? res` 在博客页依赖 `res?.records` 的场景下是正确修改，但必须和 URL 修复一起部署。半截部署（只改拦截器不改 URL）反而让问题更难排查。
