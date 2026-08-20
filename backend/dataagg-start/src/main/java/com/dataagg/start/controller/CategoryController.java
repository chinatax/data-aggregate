package com.dataagg.start.controller;

import com.dataagg.common.entity.Blog;
import com.dataagg.common.entity.Category;
import com.dataagg.common.result.Result;
import com.dataagg.service.service.BlogService;
import com.dataagg.service.service.CategoryService;
import com.dataagg.start.annotation.RequireAuth;
import com.dataagg.start.cache.MultiLevelCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private BlogService blogService;

    @Autowired
    private MultiLevelCache cache;

    private static final String FAVORITES_CODE = "__FAVORITES__";

    @GetMapping("/api/categories")
    @SuppressWarnings("unchecked")
    public Result<List<Map<String, Object>>> list() {
        List<Map<String, Object>> list = cache.get("categories", "all",
                (Class<List<Map<String, Object>>>) (Class<?>) List.class,
                () -> categoryService.lambdaQuery()
                        .orderByAsc(Category::getSortOrder)
                        .list()
                        .stream()
                        .map(c -> Map.<String, Object>of(
                                "id", c.getId(),
                                "code", c.getCode(),
                                "name", c.getName(),
                                "system", c.getSystem() != null && c.getSystem() == 1))
                        .collect(Collectors.toList()));
        return Result.success(list);
    }

    @PostMapping("/api/admin/categories")
    @RequireAuth("ADMIN")
    public Result<Void> create(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        String name = body.get("name");
        if (code == null || code.trim().isEmpty()) return Result.error("分类标识不能为空");
        if (name == null || name.trim().isEmpty()) return Result.error("分类名称不能为空");
        code = code.trim().toUpperCase().replaceAll("[^A-Z0-9_]", "");
        if (code.isEmpty()) return Result.error("分类标识无效，请使用字母数字下划线");
        if (FAVORITES_CODE.equals(code)) return Result.error("该标识为系统保留");
        if (categoryService.getByCode(code) != null) return Result.error("分类标识已存在");
        Category c = new Category();
        c.setCode(code);
        c.setName(name.trim());
        // 计算最大 sortOrder + 1，避免多个 99 导致移动无效
        Category last = categoryService.lambdaQuery()
                .orderByDesc(Category::getSortOrder)
                .last("LIMIT 1")
                .one();
        c.setSortOrder(last != null ? last.getSortOrder() + 1 : 1);
        categoryService.save(c);
        cache.evictAll("categories");
        return Result.success();
    }

    @PutMapping("/api/admin/categories/{id}")
    @RequireAuth("ADMIN")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Category c = categoryService.getById(id);
        if (c == null) return Result.error("分类不存在");
        if (FAVORITES_CODE.equals(c.getCode())) return Result.error("系统分类不可修改");
        String name = body.get("name");
        if (name == null || name.trim().isEmpty()) return Result.error("分类名称不能为空");
        c.setName(name.trim());
        categoryService.updateById(c);
        cache.evictAll("categories");
        return Result.success();
    }

    @PutMapping("/api/admin/categories/{id}/move-up")
    @RequireAuth("ADMIN")
    public Result<Void> moveUp(@PathVariable Long id) {
        Category current = categoryService.getById(id);
        if (current == null) return Result.error("分类不存在");
        List<Category> list = categoryService.lambdaQuery()
                .orderByAsc(Category::getSortOrder)
                .list();
        int idx = -1;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(id)) { idx = i; break; }
        }
        if (idx <= 0) return Result.success();
        Category prev = list.get(idx - 1);
        int tmp = current.getSortOrder();
        current.setSortOrder(prev.getSortOrder());
        prev.setSortOrder(tmp);
        categoryService.updateBatchById(List.of(current, prev));
        cache.evictAll("categories");
        return Result.success();
    }

    @PutMapping("/api/admin/categories/{id}/move-down")
    @RequireAuth("ADMIN")
    public Result<Void> moveDown(@PathVariable Long id) {
        Category current = categoryService.getById(id);
        if (current == null) return Result.error("分类不存在");
        List<Category> list = categoryService.lambdaQuery()
                .orderByAsc(Category::getSortOrder)
                .list();
        int idx = -1;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(id)) { idx = i; break; }
        }
        if (idx < 0 || idx >= list.size() - 1) return Result.success();
        Category next = list.get(idx + 1);
        int tmp = current.getSortOrder();
        current.setSortOrder(next.getSortOrder());
        next.setSortOrder(tmp);
        categoryService.updateBatchById(List.of(current, next));
        cache.evictAll("categories");
        return Result.success();
    }

    @DeleteMapping("/api/admin/categories/{id}")
    @RequireAuth("ADMIN")
    public Result<Void> delete(@PathVariable Long id) {
        Category c = categoryService.getById(id);
        if (c == null) return Result.error("分类不存在");
        if (c.getSystem() != null && c.getSystem() == 1) return Result.error("系统分类不可删除");
        long count = blogService.lambdaQuery().eq(Blog::getCategory, c.getCode()).count();
        if (count > 0) return Result.error("该分类下有 " + count + " 篇文章，无法删除");
        categoryService.removeById(id);
        cache.evictAll("categories");
        return Result.success();
    }
}
