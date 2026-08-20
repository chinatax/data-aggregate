CREATE TABLE IF NOT EXISTS `stock_holder_top10` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `end_date` DATE NOT NULL COMMENT '报告期',
    `code` VARCHAR(10) NOT NULL COMMENT '股票代码',
    `name` VARCHAR(50) COMMENT '股票简称',
    `holder_name` VARCHAR(200) COMMENT '股东名称',
    `hold_num` BIGINT COMMENT '持股数量',
    `hold_ratio` DECIMAL(10,2) COMMENT '持股比例(%)',
    `hold_change` VARCHAR(20) COMMENT '变动方向',
    `holder_rank` INT COMMENT '排名(1-10)',
    `shares_type` VARCHAR(20) COMMENT '股份类型',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
    UNIQUE KEY `uk_code_date_rank` (`code`, `end_date`, `holder_rank`),
    INDEX `idx_end_date` (`end_date`),
    INDEX `idx_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='十大流通股东';
