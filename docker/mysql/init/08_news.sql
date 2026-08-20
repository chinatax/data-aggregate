-- 新闻快讯缓存表
CREATE TABLE IF NOT EXISTS `news_article` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(500) NOT NULL COMMENT '标题',
    `source` VARCHAR(100) DEFAULT '' COMMENT '来源',
    `description` TEXT COMMENT '摘要',
    `link` VARCHAR(500) NOT NULL COMMENT '原文链接',
    `pub_date` DATETIME COMMENT '发布时间',
    `image_url` VARCHAR(500) DEFAULT NULL COMMENT '封面图',
    `country` VARCHAR(50) DEFAULT '' COMMENT '国家',
    `category` VARCHAR(50) DEFAULT '' COMMENT '分类',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
    UNIQUE KEY `uk_link` (`link`),
    INDEX `idx_pub_date` (`pub_date`),
    INDEX `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='新闻快讯缓存';
