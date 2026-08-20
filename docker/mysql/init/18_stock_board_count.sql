-- 上榜次数统计表（每月1号更新，今年内成交额进入前100的天数）
CREATE TABLE IF NOT EXISTS `stock_board_count` (
    `id`          BIGINT AUTO_INCREMENT PRIMARY KEY,
    `code`        VARCHAR(10) NOT NULL COMMENT '股票代码',
    `board_count` INT DEFAULT 0 COMMENT '今年上榜次数',
    `update_date` DATE NOT NULL COMMENT '统计截止日期',
    UNIQUE KEY `uk_code` (`code`),
    INDEX `idx_board_count` (`board_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='上榜次数统计';
