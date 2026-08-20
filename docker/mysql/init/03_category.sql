SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `code` VARCHAR(50) NOT NULL COMMENT '唯一标识，不可变',
    `name` VARCHAR(50) NOT NULL COMMENT '显示名称',
    `sort_order` INT DEFAULT 0 COMMENT '排序序号',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='博客分类表';

INSERT INTO `category` (`code`, `name`, `sort_order`) VALUES
('FRONTEND', '前端', 1),
('BACKEND', '后端', 2),
('DATABASE', '数据库', 3),
('DEVOPS', '运维', 4),
('AI', '人工智能', 5);
