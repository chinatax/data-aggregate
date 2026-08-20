-- 个股日成交数据（收盘后批量抓取）
CREATE TABLE IF NOT EXISTS `stock_daily_amount` (
    `id`               BIGINT AUTO_INCREMENT PRIMARY KEY,
    `trade_date`       DATE NOT NULL COMMENT '交易日期',
    `code`             VARCHAR(10) NOT NULL COMMENT '股票代码',
    `name`             VARCHAR(50) COMMENT '股票名称',
    `amount`           DECIMAL(20,4) COMMENT '成交额(万元)',
    `turnover_pct`     DECIMAL(10,4) COMMENT '换手率%',
    `volume_ratio`     DECIMAL(10,4) COMMENT '量比',
    `float_market_cap` DECIMAL(20,4) COMMENT '流通市值(亿)',
    `industry`         VARCHAR(100) COMMENT '行业(申万二级)',
    `concept`          VARCHAR(500) COMMENT '概念标签',
    `region`           VARCHAR(50) COMMENT '地区板块',
    `create_time`      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
    UNIQUE KEY `uk_code_date` (`code`, `trade_date`),
    INDEX `idx_trade_date` (`trade_date`),
    INDEX `idx_industry` (`industry`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='个股日成交数据';
