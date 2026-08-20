package com.dataagg.service.service;

import com.dataagg.common.entity.Blog;
import com.dataagg.common.entity.BlogFileHash;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BlogSyncService {

    private final BlogService blogService;
    private final BlogFileHashService blogFileHashService;

    public Map<String, Object> syncFile(Path mdFile, String relativePath, String category) throws IOException {
        String content = readWithEncodingDetection(mdFile);
        String md5 = computeMd5(content);

        String[] parts = extractTitleAndContent(content);

        BlogFileHash existingHash = blogFileHashService.getByFileName(relativePath);

        if (existingHash != null && md5.equals(existingHash.getMd5Hash())) {
            return Map.of("action", "skip", "reason", "unchanged",
                    "blogId", existingHash.getBlogId());
        }

        Blog blog;
        if (existingHash != null && existingHash.getBlogId() != null) {
            blog = blogService.getById(existingHash.getBlogId());
            if (blog == null) {
                blog = new Blog();
            }
        } else {
            blog = new Blog();
        }

        blog.setTitle(parts[0]);
        blog.setContent(parts[1]);
        blog.setSummary(extractSummary(parts[1], 200));
        blog.setAuthor("admin");
        blog.setStatus("PUBLISHED");
        blog.setCategory(category);
        blog.setCoverImage("");
        blog.setViewCount(0);
        blog.setLikeCount(0);

        String action;
        if (blog.getId() != null) {
            blogService.updateById(blog);
            action = "updated";
        } else {
            blogService.save(blog);
            action = "created";
        }

        blogFileHashService.upsert(relativePath, md5, blog.getId());

        return Map.of("action", action, "blogId", blog.getId(), "title", blog.getTitle());
    }

    public void handleFileDeletion(String relativePath) {
        blogFileHashService.removeByFileName(relativePath);
    }

    public static String computeMd5(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    static String readWithEncodingDetection(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        // UTF-8 BOM
        if (bytes.length >= 3
                && bytes[0] == (byte) 0xEF
                && bytes[1] == (byte) 0xBB
                && bytes[2] == (byte) 0xBF) {
            return new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        }
        // Try UTF-8
        String content = new String(bytes, StandardCharsets.UTF_8);
        if (content.contains("�")) {
            // Try GBK
            content = new String(bytes, Charset.forName("GBK"));
            if (content.contains("�")) {
                // Try GB2312 as last resort
                content = new String(bytes, Charset.forName("GB2312"));
            }
        }
        return content;
    }

    static String[] extractTitleAndContent(String raw) {
        String text = raw.trim();
        String title = "Untitled";
        String body = text;

        String[] lines = text.split("\n", 2);
        String firstLine = lines[0].trim();
        if (firstLine.startsWith("# ")) {
            title = firstLine.substring(2).trim();
            body = lines.length > 1 ? lines[1].trim() : "";
        } else if (firstLine.startsWith("#") && firstLine.length() > 1) {
            title = firstLine.substring(1).trim();
            body = lines.length > 1 ? lines[1].trim() : "";
        }
        return new String[]{title, body};
    }

    static String extractSummary(String markdown, int maxLen) {
        // Strip common markdown syntax to get plain text
        String plain = markdown
                .replaceAll("#+\\s", "")
                .replaceAll("\\*\\*|__", "")
                .replaceAll("\\[([^\\]]+)\\]\\([^)]+\\)", "$1")
                .replaceAll("`{1,3}[^`]*`{1,3}", "")
                .replaceAll(">\\s", "")
                .replaceAll("\\|", " ")
                .replaceAll("[-*+]\\s", "")
                .replaceAll("\\n{2,}", "\n")
                .replaceAll("\\s+", " ")
                .trim();

        if (plain.length() <= maxLen) {
            return plain;
        }
        // Try to cut at sentence end first, then word boundary, then hard cut
        int cut = plain.lastIndexOf('。', maxLen); // Chinese period 。
        if (cut > maxLen / 2) {
            return plain.substring(0, cut + 1);
        }
        cut = plain.indexOf(' ', maxLen);
        if (cut > 0) {
            return plain.substring(0, cut) + "...";
        }
        return plain.substring(0, maxLen) + "...";
    }
}
