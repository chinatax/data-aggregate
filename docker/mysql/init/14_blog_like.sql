CREATE TABLE IF NOT EXISTS `blog_like` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '点赞ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `blog_id` BIGINT NOT NULL COMMENT '博客ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_blog` (`user_id`, `blog_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='博客点赞表';
