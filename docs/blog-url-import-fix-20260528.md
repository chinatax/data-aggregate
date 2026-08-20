# 博客 URL 导入微信公众号文章失败修复

**日期**: 2026-05-28
**文件**: `backend/dataagg-start/src/main/java/com/dataagg/start/controller/BlogController.java`

## 问题现象

博客编辑器中使用「URL 导入」功能导入微信公众号文章（如 `https://mp.weixin.qq.com/s/...`），无法提取到文章内容，返回空正文。

## 根因分析

微信公众号页面的 HTML 结构存在内容容器顺序问题：

1. `.fullscreen-layout-padding__content` — 一个近乎空白的 wrapper div，位于页面 DOM 较前位置
2. `.rich_media_content` / `#js_content` — **真正的文章正文**，位于更后面

原代码使用 Jsoup 的 `selectFirst("[class*=content]")` 只取第一个 CSS 类名包含 `content` 的元素，每次都命中空的 `.fullscreen-layout-padding__content`，导致 `contentEl` 几乎没有文本。后端返回空内容给前端，导入失败。

另外 `#js_content` 默认带有 `visibility: hidden; opacity: 0` 的内联样式，微信通过 JS 动态改为可见，但 Jsoup 抓取时内容仍处于隐藏状态。

## 修复方案

改动位置：`BlogController.java:205-236`

### 1. 新增微信公众号专用选择器（最高优先级）

```java
Element contentEl = doc.selectFirst("#js_content");
if (contentEl == null) {
    contentEl = doc.selectFirst(".rich_media_content");
}
```

直接通过微信特有的 id/class 定位正文容器，跳过无关 wrapper。

### 2. 通用类名匹配改为取内容最多者

```java
if (contentEl == null) {
    Elements candidates = doc.select("[class*=article], [class*=post], [class*=content], main");
    Element best = null;
    int bestLen = 0;
    for (Element el : candidates) {
        String text = el.text().trim();
        if (text.length() > bestLen) {
            bestLen = text.length();
            best = el;
        }
    }
    contentEl = best;
}
```

不再用 `selectFirst` 取第一个匹配元素，而是遍历所有候选，取文本长度最大的。这个策略对任意网站都更健壮。

### 3. 移除微信的 visibility 隐藏样式

```java
if (contentEl != null && "js_content".equals(contentEl.id())) {
    contentEl.removeAttr("style");
}
```

### 选择器优先级（完整链路）

| 优先级 | 选择器 | 说明 |
|--------|--------|------|
| 1 | `#js_content` | 微信专用（id） |
| 2 | `.rich_media_content` | 微信专用（class） |
| 3 | `article` | 通用语义标签 |
| 4 | `[class*=article/post/content], main` | 按类名取文本最多者 |
| 5 | `body` | 最终回退 |
