-- 评论表
CREATE TABLE IF NOT EXISTS `comment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `blog_id` BIGINT NOT NULL COMMENT '所属文章ID',
    `parent_id` BIGINT DEFAULT NULL COMMENT '父评论ID，null=顶级留言',
    `author_name` VARCHAR(50) NOT NULL COMMENT '留言者名称',
    `content` TEXT NOT NULL COMMENT '留言内容',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    KEY `idx_blog_id` (`blog_id`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章评论表';
