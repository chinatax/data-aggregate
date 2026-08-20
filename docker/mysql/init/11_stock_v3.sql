-- 年报财务数据（原始数据落库，全市场近5年）
CREATE TABLE IF NOT EXISTS `stock_financial` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `code` VARCHAR(10) NOT NULL COMMENT '股票代码',
    `name` VARCHAR(50) COMMENT '股票名称',
    `report_year` INT NOT NULL COMMENT '财年',
    `main_revenue` DECIMAL(20,2) COMMENT '主营业务收入(元)',
    `deducted_net_profit` DECIMAL(20,2) COMMENT '扣非净利润(元)',
    `net_profit` DECIMAL(20,2) COMMENT '净利润(元)',
    `operating_cash_flow` DECIMAL(20,2) COMMENT '经营活动现金流净额(元)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
    UNIQUE KEY `uk_code_year` (`code`, `report_year`),
    INDEX `idx_code` (`code`),
    INDEX `idx_report_year` (`report_year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='年报财务数据';

-- 评分结果（全市场，每次计算全量覆盖）
CREATE TABLE IF NOT EXISTS `stock_quality_score` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `code` VARCHAR(10) NOT NULL COMMENT '股票代码',
    `name` VARCHAR(50) COMMENT '股票名称',
    `industry` VARCHAR(100) COMMENT '申万行业',
    `score` DECIMAL(5,2) COMMENT '综合加权评分(0-1.0)',
    `revenue_pass` TINYINT DEFAULT 0 COMMENT '收入不塌方 0/1',
    `cash_revenue_ratio` DECIMAL(10,4) COMMENT '经营现金流/营收(5年均值)',
    `profit_pass` TINYINT DEFAULT 0 COMMENT '扣非持续为正 0/1',
    `cash_profit_ratio` DECIMAL(10,4) COMMENT '经营现金流/净利润(5年均值)',
    `detail_json` TEXT COMMENT '5年明细JSON，含每年三项数值',
    `trade_date` DATE COMMENT '计算日期',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
    UNIQUE KEY `uk_code_date` (`code`, `trade_date`),
    INDEX `idx_score` (`score`),
    INDEX `idx_industry` (`industry`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优质公司三标准评分';
