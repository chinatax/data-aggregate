package com.dataagg.service.service;

import com.dataagg.common.entity.Book;
import com.dataagg.common.entity.BookCategory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BookSyncService {

    @Autowired
    private BookCategoryService bookCategoryService;

    @Autowired
    private BookService bookService;

    private static final Pattern DOWNLOAD_PATTERN = Pattern.compile("\\[下载\\]\\(([^)]+)\\)");

    public String computeMd5(String content) {
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

    public Map<String, Object> syncSingleFile(Path mdFile) throws IOException {
        String fileName = mdFile.getFileName().toString();
        String categoryName = fileName.substring(0, fileName.length() - 3);
        String content = Files.readString(mdFile);
        List<Book> books = parseBooks(content);

        Map<String, Object> result = new HashMap<>();
        if (books.isEmpty()) {
            BookCategory category = bookCategoryService.getByName(categoryName);
            if (category != null) {
                bookService.deleteByCategoryId(category.getId());
                bookCategoryService.removeById(category.getId());
            }
            result.put("category", categoryName);
            result.put("bookCount", 0);
            result.put("isNewCategory", false);
            result.put("isEmpty", true);
            return result;
        }

        BookCategory category = bookCategoryService.getByName(categoryName);
        boolean isNewCategory = category == null;
        if (isNewCategory) {
            category = new BookCategory();
            category.setName(categoryName);
            bookCategoryService.save(category);
        }

        bookService.deleteByCategoryId(category.getId());
        for (Book book : books) {
            book.setCategoryId(category.getId());
            bookService.save(book);
        }

        result.put("categoryId", category.getId());
        result.put("category", categoryName);
        result.put("bookCount", books.size());
        result.put("isNewCategory", isNewCategory);
        return result;
    }

    public Map<String, Object> deleteCategoryForFile(String fileName) {
        String categoryName = fileName.endsWith(".md")
                ? fileName.substring(0, fileName.length() - 3)
                : fileName;

        Map<String, Object> result = new HashMap<>();
        BookCategory category = bookCategoryService.getByName(categoryName);
        if (category != null) {
            List<Book> books = bookService.lambdaQuery()
                    .eq(Book::getCategoryId, category.getId())
                    .list();
            int count = books.size();
            bookService.deleteByCategoryId(category.getId());
            bookCategoryService.removeById(category.getId());
            result.put("categoryName", categoryName);
            result.put("booksDeleted", count);
        } else {
            result.put("categoryName", categoryName);
            result.put("booksDeleted", 0);
        }
        return result;
    }

    public List<Book> parseBooks(String content) {
        List<Book> books = new ArrayList<>();
        String[] lines = content.split("\n");
        boolean inTable = false;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("| 书名")) {
                inTable = true;
                continue;
            }
            if (trimmed.matches("^\\|[\\s\\-:]+\\|.*")) {
                continue;
            }
            if (!inTable || !trimmed.startsWith("|")) {
                if (inTable && !trimmed.startsWith("|")) {
                    inTable = false;
                }
                continue;
            }

            String[] cols = trimmed.split("\\|");
            if (cols.length < 3) {
                continue;
            }

            String title = cols[1].trim();
            String author = cols[2].trim();
            String linkCell = cols.length > 3 ? cols[3].trim() : "";

            if (title.isEmpty()) {
                continue;
            }

            Matcher m = DOWNLOAD_PATTERN.matcher(linkCell);
            String downloadUrl = m.find() ? m.group(1) : "";

            Book book = new Book();
            book.setTitle(title);
            book.setAuthor(author);
            book.setDownloadUrl(downloadUrl);
            books.add(book);
        }

        return books;
    }
}
