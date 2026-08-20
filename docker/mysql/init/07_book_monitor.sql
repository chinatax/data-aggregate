-- 电子书文件哈希表（用于变更检测）
CREATE TABLE IF NOT EXISTS `book_file_hash` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `file_name` VARCHAR(255) NOT NULL COMMENT '文件名',
    `md5_hash` VARCHAR(64) NOT NULL COMMENT 'MD5哈希',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_file_name` (`file_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电子书文件哈希表';

-- 电子书监控日志表
CREATE TABLE IF NOT EXISTS `book_monitor_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `scan_time` DATETIME NOT NULL COMMENT '扫描开始时间',
    `end_time` DATETIME DEFAULT NULL COMMENT '扫描结束时间',
    `files_added` INT DEFAULT 0 COMMENT '新增文件数',
    `files_modified` INT DEFAULT 0 COMMENT '修改文件数',
    `files_deleted` INT DEFAULT 0 COMMENT '删除文件数',
    `books_added` INT DEFAULT 0 COMMENT '新增书籍数',
    `books_updated` INT DEFAULT 0 COMMENT '更新书籍数',
    `books_removed` INT DEFAULT 0 COMMENT '移除书籍数',
    `error_message` TEXT DEFAULT NULL COMMENT '错误信息（JSON数组）',
    `status` VARCHAR(20) NOT NULL DEFAULT 'SUCCESS' COMMENT '状态：SUCCESS/PARTIAL/FAILED',
    `details` TEXT DEFAULT NULL COMMENT '详情（JSON）',
    PRIMARY KEY (`id`),
    KEY `idx_scan_time` (`scan_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电子书监控日志表';
