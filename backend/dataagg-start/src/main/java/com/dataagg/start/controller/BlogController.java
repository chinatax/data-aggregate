package com.dataagg.start.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dataagg.common.entity.Blog;
import com.dataagg.common.entity.Category;
import com.dataagg.common.entity.User;
import com.dataagg.common.result.Result;
import com.dataagg.service.service.BlogFavoriteService;
import com.dataagg.service.service.BlogLikeService;
import com.dataagg.service.service.BlogService;
import com.dataagg.service.service.CategoryService;
import com.dataagg.service.service.UserService;
import com.dataagg.start.annotation.RequireAuth;
import com.dataagg.start.cache.MultiLevelCache;
import com.dataagg.start.util.JwtUtil;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.util.Iterator;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/blog")
public class BlogController {

    @Autowired
    private BlogService blogService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private UserService userService;

    @Autowired
    private MultiLevelCache cache;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private BlogFavoriteService blogFavoriteService;

    @Autowired
    private BlogLikeService blogLikeService;

    /**
     * 从请求头中提取当前用户名（无需 @RequireAuth）
     */
    private String getCurrentUsername() {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                return jwtUtil.getUsername(header.substring(7));
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private Long getCurrentUserId() {
        return (Long) request.getAttribute("userId");
    }

    private void resolveAuthorAvatars(List<Blog> blogs) {
        List<String> authors = blogs.stream()
                .map(Blog::getAuthor)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (authors.isEmpty()) return;
        Map<String, String> avatarMap = userService.getAvatarsByUsernames(authors);
        for (Blog blog : blogs) {
            if (blog.getAuthor() != null) {
                blog.setAuthorAvatar(avatarMap.get(blog.getAuthor()));
            }
        }
    }

    private void resolveFavorites(List<Blog> blogs) {
        List<Long> blogIds = blogs.stream().map(Blog::getId).collect(Collectors.toList());
        Map<Long, Long> countMap = blogFavoriteService.countByBlogIds(blogIds);
        Long userId = getCurrentUserId();
        List<Long> favIds = userId != null ? blogFavoriteService.getFavoritedBlogIds(userId) : List.of();
        for (Blog blog : blogs) {
            blog.setFavoriteCount(countMap.getOrDefault(blog.getId(), 0L));
            if (userId != null) {
                blog.setFavorited(favIds.contains(blog.getId()));
            }
        }
    }

    @GetMapping("/list")
    public Result<Page<Blog>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category) {
        String username = getCurrentUsername();
        String cacheKey = pageNum + ":" + pageSize + ":" + (keyword != null ? keyword : "") + ":" + (category != null ? category : "") + ":" + (username != null ? username : "");
        JavaType type = objectMapper.getTypeFactory().constructParametricType(Page.class, Blog.class);
        Page<Blog> page = cache.get("blogPages", cacheKey, type, () -> {
            Page<Blog> p = new Page<>(pageNum, pageSize);
            Page<Blog> result = blogService.pageByCondition(p, keyword, category, null, username);
            resolveAuthorAvatars(result.getRecords());
            resolveFavorites(result.getRecords());
            for (Blog blog : result.getRecords()) {
                if (blog.getSummary() != null && blog.getSummary().length() > 150) {
                    blog.setSummary(blog.getSummary().substring(0, 150));
                }
            }
            return result;
        });
        return Result.success(page);
    }

    @GetMapping("/{id}")
    public Result<Blog> getById(@PathVariable Long id) {
        Blog blog = cache.get("blogs", String.valueOf(id), Blog.class,
                () -> blogService.getById(id));
        if (blog == null) {
            return Result.success(null);
        }
        // 非公开文章仅作者和管理员可见
        if (!"PUBLISHED".equals(blog.getStatus())) {
            String username = getCurrentUsername();
            String role = null;
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                try {
                    role = jwtUtil.getClaim(header.substring(7), "role");
                } catch (Exception ignored) {}
            }
            boolean isAuthor = username != null && username.equals(blog.getAuthor());
            boolean isAdmin = "ADMIN".equalsIgnoreCase(role);
            if (!isAuthor && !isAdmin) {
                return Result.success(null);
            }
        }
        // 异步更新浏览计数，不阻塞响应
        CompletableFuture.runAsync(() -> blogService.incrementViewCount(id));
        int currentViews = blog.getViewCount() != null ? blog.getViewCount() : 0;
        blog.setViewCount(currentViews + 1);
        cache.evict("blogs", String.valueOf(id));
        if (blog.getAuthor() != null) {
            User author = userService.getByUsername(blog.getAuthor());
            if (author != null && author.getAvatar() != null) {
                blog.setAuthorAvatar(author.getAvatar());
            }
        }
        Long userId = getCurrentUserId();
        blog.setFavoriteCount(blogFavoriteService.countByBlogId(id));
        if (userId != null) {
            blog.setFavorited(blogFavoriteService.isFavorited(userId, id));
        }
        return Result.success(blog);
    }

    @PostMapping
    @RequireAuth
    public Result<Long> save(@RequestBody Blog blog) {
        if (blog.getStatus() == null || blog.getStatus().isEmpty()) {
            blog.setStatus("DRAFT");
        }
        if (blog.getViewCount() == null) {
            blog.setViewCount(0);
        }
        if (blog.getLikeCount() == null) {
            blog.setLikeCount(0);
        }
        if (blog.getAuthor() == null || blog.getAuthor().isEmpty()) {
            String username = (String) request.getAttribute("username");
            blog.setAuthor(username != null ? username : "匿名用户");
        }
        blogService.save(blog);
        cache.evictAll("blogPages");
        return Result.success(blog.getId());
    }

    @PutMapping("/{id}")
    @RequireAuth
    public Result<Void> update(@PathVariable Long id, @RequestBody Blog blog) {
        blog.setId(id);
        blogService.updateById(blog);
        cache.evict("blogs", String.valueOf(id));
        cache.evictAll("blogPages");
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequireAuth
    public Result<Void> delete(@PathVariable Long id) {
        blogService.removeById(id);
        cache.evict("blogs", String.valueOf(id));
        cache.evictAll("blogPages");
        return Result.success();
    }

    @Value("${upload.path:uploads}")
    private String uploadPath;

    @Autowired
    private RestTemplate restTemplate;

    @PostMapping("/generate-cover")
    @RequireAuth
    public Result<Map<String, String>> generateCover(@RequestBody Map<String, String> body) {
        String prompt = body.get("prompt");
        if (prompt == null || prompt.isBlank()) {
            return Result.error("提示词不能为空");
        }

        try {
            // 1. 调用阿里云百炼文生图 API
            String apiKey = "sk-9cda7001eea34e7180a89ca496fc2d87";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("X-DashScope-Async", "enable");

            Map<String, Object> input = new LinkedHashMap<>();
            // 禁止渲染文字，避免中文乱码
            input.put("prompt", prompt + ". IMPORTANT: no text, no words, no letters, no numbers, no typography, pure visual illustration only");

            Map<String, Object> params = new LinkedHashMap<>();
            params.put("size", "1024*576");
            params.put("n", 1);

            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", "wanx2.1-t2i-turbo");
            requestBody.put("input", input);
            requestBody.put("parameters", params);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> submitResp = restTemplate.exchange(
                    "https://dashscope.aliyuncs.com/api/v1/services/aigc/text2image/image-synthesis",
                    HttpMethod.POST, entity, Map.class);

            if (submitResp.getBody() == null || submitResp.getBody().get("output") == null) {
                return Result.error("AI 生成失败：无响应");
            }

            Map<String, Object> output = (Map<String, Object>) submitResp.getBody().get("output");
            String taskId = (String) output.get("task_id");
            if (taskId == null) {
                return Result.error("AI 生成失败：未获取到任务 ID");
            }

            // 2. 轮询任务结果
            String resultUrl = null;
            for (int i = 0; i < 30; i++) {
                Thread.sleep(2000);
                HttpHeaders pollHeaders = new HttpHeaders();
                pollHeaders.set("Authorization", "Bearer " + apiKey);
                HttpEntity<?> pollEntity = new HttpEntity<>(pollHeaders);
                ResponseEntity<Map> pollResp = restTemplate.exchange(
                        "https://dashscope.aliyuncs.com/api/v1/tasks/" + taskId,
                        HttpMethod.GET, pollEntity, Map.class);

                if (pollResp.getBody() == null) continue;

                Map<String, Object> pollOutput = (Map<String, Object>) pollResp.getBody().get("output");
                if (pollOutput == null) continue;

                String status = (String) pollOutput.get("task_status");
                if ("SUCCEEDED".equals(status)) {
                    List<Map<String, Object>> results = (List<Map<String, Object>>) pollOutput.get("results");
                    if (results != null && !results.isEmpty()) {
                        resultUrl = (String) results.get(0).get("url");
                    }
                    break;
                } else if ("FAILED".equals(status)) {
                    return Result.error("AI 生成图片失败");
                }
            }

            if (resultUrl == null) {
                return Result.error("AI 生成超时，请重试");
            }

            // 3. 下载图片并保存到上传目录（用 java.net.URL 避免签名被篡改）
            byte[] imageBytes;
            try (InputStream in = new URI(resultUrl).toURL().openStream()) {
                imageBytes = in.readAllBytes();
            }
            if (imageBytes == null || imageBytes.length == 0) {
                return Result.error("下载生成的图片失败");
            }

            // 4. 按照 UploadController 的规则保存
            String dateDir = new SimpleDateFormat("yyyyMMdd").format(new Date());
            Path uploadBase = Paths.get(uploadPath);
            if (!uploadBase.isAbsolute()) {
                uploadBase = Paths.get(System.getProperty("user.dir"), uploadPath);
            }
            Path dirPath = uploadBase.resolve("images").resolve(dateDir);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            String baseName = UUID.randomUUID().toString().replace("-", "");
            String newFilename = baseName + ".png";
            Path filePath = dirPath.resolve(newFilename);
            Files.write(filePath, imageBytes);

            // 生成缩略图（360px 宽，JPEG），与 UploadController 一致
            String thumbFilename = baseName + "_thumb.jpg";
            Path thumbPath = dirPath.resolve(thumbFilename);
            generateThumbnail(filePath.toFile(), thumbPath.toFile(), 360);

            // 生成 WebP 版本（可选，服务器需安装 libwebp）
            generateWebP(filePath.toFile(), new File(dirPath.toFile(), baseName + ".webp"));

            String accessUrl = "https://www.3ks2u.com/uploads/images/" + dateDir + "/" + newFilename;

            return Result.success(Map.of("url", accessUrl));
        } catch (Exception e) {
            return Result.error("AI 生成失败: " + e.getMessage());
        }
    }

    @PostMapping("/fetch-url")
    @RequireAuth
    public Result<Map<String, String>> fetchUrl(@RequestBody Map<String, String> body) {
        String url = body.get("url");
        if (url == null || url.isBlank()) {
            return Result.error("URL 不能为空");
        }
        try {
            Document doc = Jsoup.connect(url.trim())
                    .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .header("Accept-Encoding", "gzip, deflate")
                    .header("Cache-Control", "no-cache")
                    .header("Pragma", "no-cache")
                    .header("Sec-Ch-Ua", "\"Google Chrome\";v=\"131\", \"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"")
                    .header("Sec-Ch-Ua-Mobile", "?0")
                    .header("Sec-Ch-Ua-Platform", "\"macOS\"")
                    .header("Sec-Fetch-Dest", "document")
                    .header("Sec-Fetch-Mode", "navigate")
                    .header("Sec-Fetch-Site", "none")
                    .header("Sec-Fetch-User", "?1")
                    .header("Upgrade-Insecure-Requests", "1")
                    .maxBodySize(0)
                    .timeout(15000)
                    .followRedirects(true)
                    .get();

            // 检测反爬/验证页面
            String bodyText = doc.body().text();
            boolean isWechatVerify = bodyText.contains("环境异常") && bodyText.contains("去验证")
                    && doc.selectFirst("#js_verify") != null;
            boolean isWaf = bodyText.contains("aliyun_waf") || bodyText.contains("renderData")
                    || doc.selectFirst("meta[name=aliyun_waf_aa]") != null
                    || doc.selectFirst("meta[name=aliyun_waf_bb]") != null;
            if (isWechatVerify) {
                return Result.error("当前服务器 IP 被微信反爬拦截，无法通过 URL 自动抓取。请用浏览器打开页面后，在编辑器的「粘贴网页」标签中导入（F12 → Elements → 右键 <html> → Copy → Copy outerHTML）。");
            }
            if (isWaf) {
                return Result.error("该网站开启了反爬保护（WAF），无法通过 URL 自动抓取。请用浏览器打开页面后，复制网页源代码，在编辑器的「粘贴网页」标签中导入。");
            }

            // 提取标题（优先 og:title → twitter:title → <title> → h1）
            String title = "";
            Element ogTitle = doc.selectFirst("meta[property=og:title]");
            if (ogTitle != null) {
                title = ogTitle.attr("content");
            }
            if (title.isBlank()) {
                Element twTitle = doc.selectFirst("meta[name=twitter:title]");
                if (twTitle != null) title = twTitle.attr("content");
            }
            if (title.isBlank()) {
                title = doc.title();
            }
            if (title.isBlank()) {
                Element h1 = doc.selectFirst("h1");
                if (h1 != null) title = h1.text();
            }

            // 提取封面图（og:image → twitter:image → 首张 > 200px 图片）
            String coverImage = "";
            Element ogImg = doc.selectFirst("meta[property=og:image]");
            if (ogImg != null) {
                coverImage = ogImg.attr("content");
                if (coverImage != null && !coverImage.startsWith("http")) {
                    coverImage = resolveUrl(url, coverImage);
                }
            }
            if (coverImage.isBlank()) {
                Element twImg = doc.selectFirst("meta[name=twitter:image]");
                if (twImg != null) {
                    coverImage = twImg.attr("content");
                    if (coverImage != null && !coverImage.startsWith("http")) {
                        coverImage = resolveUrl(url, coverImage);
                    }
                }
            }

            // 提取正文内容
            // 1) 微信公众号专用选择器
            Element contentEl = doc.selectFirst("#js_content");
            if (contentEl == null) {
                contentEl = doc.selectFirst(".rich_media_content");
            }
            // 2) 通用语义标签
            if (contentEl == null) {
                contentEl = doc.selectFirst("article");
            }
            // 3) 按类名匹配，取内容最多的元素（避免匹配到空 wrapper，如 WeChat 的 fullscreen-layout-padding__content）
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
            // 4) 回退到 body
            if (contentEl == null) {
                contentEl = doc.body();
            }
            // 5) 微信文章内容可能被 visibility:hidden 隐藏，移除该属性以便保留内容
            if (contentEl != null && "js_content".equals(contentEl.id())) {
                contentEl.removeAttr("style");
            }

            // 移除不必要元素
            if (contentEl != null) {
                contentEl.select("script, style, nav, footer, iframe, .sidebar, .comment, .ad, [class*=comment], [class*=sidebar], [class*=advertisement]").remove();
            }

            String content = contentEl != null ? contentEl.html() : "";
            String summary = contentEl != null ? contentEl.text() : "";

            // 内容为空或过短 → 返回错误，提示用户用「粘贴网页」方式
            if (summary.isBlank() || summary.length() < 50) {
                return Result.error("无法从该页面提取到有效正文内容（仅 " + summary.length() + " 字）。"
                        + "该页面可能为 SPA 动态渲染或需登录，请用浏览器打开后，使用编辑器的「粘贴网页」标签导入（F12 → Elements → 右键 <html> → Copy → Copy outerHTML）。");
            }

            // 摘要取前 300 字
            if (summary.length() > 300) {
                summary = summary.substring(0, 300);
            }

            return Result.success(Map.of(
                    "title", title.trim(),
                    "content", content.trim(),
                    "summary", summary.trim(),
                    "coverImage", coverImage != null ? coverImage.trim() : ""
            ));
        } catch (Exception e) {
            return Result.error("获取文章失败: " + e.getMessage());
        }
    }

    private String resolveUrl(String baseUrl, String path) {
        try {
            return new java.net.URL(new java.net.URL(baseUrl), path).toString();
        } catch (Exception e) {
            return path;
        }
    }

    @GetMapping("/categories")
    public Result<List<Map<String, Object>>> categories() {
        List<Map<String, Object>> list = cache.get("categories", "blogCategories",
                (Class<List<Map<String, Object>>>) (Class<?>) List.class,
                () -> categoryService.lambdaQuery()
                        .orderByAsc(Category::getSortOrder)
                        .list()
                        .stream()
                        .map(c -> Map.<String, Object>of("id", c.getId(), "code", c.getCode(), "name", c.getName()))
                        .collect(Collectors.toList()));
        return Result.success(list);
    }

    @PostMapping("/{id}/favorite")
    @RequireAuth
    public Result<Map<String, Object>> toggleFavorite(@PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        boolean favorited = blogFavoriteService.toggle(userId, id);
        cache.evictAll("blogPages");
        return Result.success(Map.of("favorited", favorited,
                "favoriteCount", blogFavoriteService.countByBlogId(id)));
    }

    @GetMapping("/{id}/favorite")
    public Result<Map<String, Object>> checkFavorite(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                try {
                    userId = jwtUtil.getUserId(header.substring(7));
                } catch (Exception ignored) {}
            }
        }
        boolean favorited = userId != null && blogFavoriteService.isFavorited(userId, id);
        return Result.success(Map.of("favorited", favorited,
                "favoriteCount", blogFavoriteService.countByBlogId(id)));
    }

    @PostMapping("/{id}/like")
    public Result<Map<String, Object>> toggleLike(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        Boolean incr = body.get("incr");
        if (incr == null) return Result.error("参数错误");
        Blog blog = blogService.getById(id);
        if (blog == null) return Result.success(null);
        long current = blog.getLikeCount() != null ? blog.getLikeCount() : 0;
        long newCount = Math.max(0, incr ? current + 1 : current - 1);
        blogService.lambdaUpdate().set(Blog::getLikeCount, (int) newCount).eq(Blog::getId, id).update();
        cache.evict("blogs", String.valueOf(id));
        cache.evictAll("blogPages");
        return Result.success(Map.of("likeCount", newCount));
    }

    @GetMapping("/user/favorites")
    @RequireAuth
    public Result<List<Blog>> userFavorites() {
        Long userId = (Long) request.getAttribute("userId");
        List<Long> ids = blogFavoriteService.getFavoritedBlogIds(userId);
        if (ids.isEmpty()) return Result.success(List.of());
        List<Blog> blogs = blogService.listByIds(ids);
        resolveAuthorAvatars(blogs);
        return Result.success(blogs);
    }

    private void generateThumbnail(File source, File target, int maxWidth) {
        String name = source.getName().toLowerCase();
        if (name.endsWith(".svg") || name.endsWith(".gif")) return;
        try {
            BufferedImage original = ImageIO.read(source);
            if (original == null) return;
            int w = original.getWidth();
            int h = original.getHeight();
            if (w <= maxWidth) {
                Files.copy(source.toPath(), target.toPath());
                return;
            }
            int newH = (int) ((double) h / w * maxWidth);
            BufferedImage thumb = new BufferedImage(maxWidth, newH, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = thumb.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.drawImage(original, 0, 0, maxWidth, newH, null);
            g2d.dispose();
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            if (writers.hasNext()) {
                ImageWriter writer = writers.next();
                try (ImageOutputStream ios = ImageIO.createImageOutputStream(target)) {
                    writer.setOutput(ios);
                    ImageWriteParam param = writer.getDefaultWriteParam();
                    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    param.setCompressionQuality(0.8f);
                    writer.write(null, new IIOImage(thumb, null, null), param);
                    writer.dispose();
                }
            } else {
                ImageIO.write(thumb, "jpg", target);
            }
        } catch (IOException e) {
            // 缩略图生成失败不阻塞
        }
    }

    private void generateWebP(File source, File target) {
        String name = source.getName().toLowerCase();
        if (name.endsWith(".svg") || name.endsWith(".gif")) return;
        try {
            ProcessBuilder pb = new ProcessBuilder("cwebp", "-q", "80", source.getAbsolutePath(), "-o", target.getAbsolutePath());
            pb.redirectErrorStream(true);
            Process p = pb.start();
            p.waitFor();
        } catch (Exception e) {
            // WebP 生成失败不阻塞（cwebp 可能未安装）
        }
    }
}
