-- 博客性能优化索引
-- 复合索引: 加速最常见的列表查询
ALTER TABLE blog ADD INDEX idx_list_query (deleted, status, category, create_time);

-- FULLTEXT 索引: 加速中文关键词搜索 (MySQL 8.0+ 需 ngram parser)
ALTER TABLE blog ADD FULLTEXT INDEX ft_search (title, summary, tags) WITH PARSER ngram;

-- blog_favorite 索引
ALTER TABLE blog_favorite ADD INDEX idx_blog_id (blog_id);
ALTER TABLE blog_favorite ADD INDEX idx_user_blog (user_id, blog_id);

-- blog_like 索引
ALTER TABLE blog_like ADD INDEX idx_blog_id (blog_id);
ALTER TABLE blog_like ADD INDEX idx_user_blog (user_id, blog_id);

-- comment 索引: 加速按博客加载评论树
ALTER TABLE comment ADD INDEX idx_blog_parent (blog_id, parent_id);
