package com.dataagg.start.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dataagg.common.entity.Book;
import com.dataagg.common.entity.BookCategory;
import com.dataagg.common.result.Result;
import com.dataagg.service.service.BookCategoryService;
import com.dataagg.service.service.BookService;
import com.dataagg.service.service.BookSyncService;
import com.dataagg.service.service.UserDownloadService;
import com.dataagg.service.service.UserFavoriteService;
import com.dataagg.start.annotation.RequireAuth;
import com.dataagg.start.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class BookController {

    @Autowired
    private BookCategoryService bookCategoryService;

    @Autowired
    private BookService bookService;

    @Autowired
    private UserFavoriteService userFavoriteService;

    @Autowired
    private UserDownloadService userDownloadService;

    @Autowired
    private BookSyncService bookSyncService;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${book.data-path:book/}")
    private String bookDataPath;

    @GetMapping("/books/categories")
    public Result<List<Map<String, Object>>> categories() {
        Map<Long, Long> countMap = bookService.countByCategory();
        List<Map<String, Object>> list = bookCategoryService.lambdaQuery()
                .orderByAsc(BookCategory::getName)
                .list()
                .stream()
                .map(c -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", c.getId());
                    m.put("name", c.getName());
                    m.put("count", countMap.getOrDefault(c.getId(), 0L));
                    return m;
                })
                .collect(Collectors.toList());
        return Result.success(list);
    }

    @GetMapping("/books")
    public Result<Page<Book>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<Long> bookIds) {
        Page<Book> page = new Page<>(pageNum, pageSize);
        page = bookService.pageByCondition(page, categoryId, keyword, bookIds);
        return Result.success(page);
    }

    @GetMapping("/books/search")
    public Result<Page<Book>> search(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam String keyword) {
        Page<Book> page = new Page<>(pageNum, pageSize);
        page = bookService.pageByCondition(page, null, keyword, null);
        return Result.success(page);
    }

    @PostMapping("/books/{id}/favorite")
    @RequireAuth
    public Result<Map<String, Object>> toggleFavorite(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        boolean favorited = userFavoriteService.toggle(userId, id);
        Map<String, Object> result = new HashMap<>();
        result.put("favorited", favorited);
        return Result.success(result);
    }

    @GetMapping("/user/favorites")
    @RequireAuth
    public Result<List<Long>> userFavorites(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<Long> bookIds = userFavoriteService.getFavoritedBookIds(userId);
        return Result.success(bookIds);
    }

    @PostMapping("/books/{id}/download")
    @RequireAuth
    public Result<Void> recordDownload(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        com.dataagg.common.entity.UserDownload download = new com.dataagg.common.entity.UserDownload();
        download.setUserId(userId);
        download.setBookId(id);
        userDownloadService.save(download);
        return Result.success();
    }

    @GetMapping("/books/{id}/download-file")
    public void downloadFile(@PathVariable Long id, @RequestParam String token, HttpServletResponse response) {
        Long userId;
        try {
            userId = jwtUtil.getUserId(token);
        } catch (Exception e) {
            try { response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "认证失败"); } catch (IOException ignored) {}
            return;
        }
        Book book = bookService.getById(id);
        if (book == null || book.getDownloadUrl() == null || book.getDownloadUrl().isEmpty()) {
            try {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "下载链接不可用");
            } catch (IOException ignored) {}
            return;
        }

        // 记录下载
        com.dataagg.common.entity.UserDownload download = new com.dataagg.common.entity.UserDownload();
        download.setUserId(userId);
        download.setBookId(id);
        userDownloadService.save(download);

        HttpURLConnection conn = null;
        try {
            URI uri = URI.create(book.getDownloadUrl());
            conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(60000);
            conn.setInstanceFollowRedirects(true);
            conn.connect();

            String fileName = book.getTitle() + ".rar";
            String encodedName = java.net.URLEncoder.encode(fileName, "UTF-8").replace("+", "%20");
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedName);
            response.setHeader("Content-Length", String.valueOf(conn.getContentLengthLong()));

            try (InputStream in = conn.getInputStream();
                 OutputStream out = response.getOutputStream()) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1) {
                    out.write(buf, 0, n);
                }
            }
        } catch (Exception e) {
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "下载失败");
            } catch (IOException ignored) {}
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    @GetMapping("/books/count")
    public Result<Map<String, Object>> bookCount() {
        long total = bookService.count();
        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        return Result.success(result);
    }

    @PostMapping("/admin/books/sync")
    @RequireAuth("ADMIN")
    public Result<Map<String, Object>> sync() {
        Path dir = Paths.get(bookDataPath);
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return Result.error("书籍数据目录不存在: " + bookDataPath);
        }

        int categoryCount = 0;
        int bookCount = 0;

        try {
            List<Path> mdFiles = Files.list(dir)
                    .filter(p -> p.toString().endsWith(".md"))
                    .collect(Collectors.toList());

            for (Path mdFile : mdFiles) {
                Map<String, Object> r = bookSyncService.syncSingleFile(mdFile);
                if (Boolean.TRUE.equals(r.get("isEmpty"))) {
                    continue;
                }
                if (Boolean.TRUE.equals(r.get("isNewCategory"))) {
                    categoryCount++;
                }
                bookCount += (int) r.getOrDefault("bookCount", 0);
            }
        } catch (IOException e) {
            return Result.error("同步失败: " + e.getMessage());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("categories", categoryCount);
        result.put("books", bookCount);
        return Result.success(result);
    }
}
