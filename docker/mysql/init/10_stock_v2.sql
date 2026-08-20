-- 龙虎榜全市场
CREATE TABLE IF NOT EXISTS `stock_dragon_board` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `trade_date` DATE NOT NULL COMMENT '交易日期',
    `code` VARCHAR(10) NOT NULL COMMENT '股票代码',
    `name` VARCHAR(50) COMMENT '股票名称',
    `reason` VARCHAR(200) COMMENT '上榜原因',
    `close_price` DECIMAL(10,2) COMMENT '收盘价',
    `change_pct` DECIMAL(10,2) COMMENT '涨跌幅%',
    `net_buy` DECIMAL(16,2) COMMENT '净买额(元)',
    `buy_amt` DECIMAL(16,2) COMMENT '买入金额(元)',
    `sell_amt` DECIMAL(16,2) COMMENT '卖出金额(元)',
    `turnover_pct` DECIMAL(10,2) COMMENT '换手率%',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
    UNIQUE KEY `uk_code_date` (`code`, `trade_date`),
    INDEX `idx_trade_date` (`trade_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='龙虎榜全市场';

-- 限售解禁日历
CREATE TABLE IF NOT EXISTS `stock_lockup_expiry` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `code` VARCHAR(10) NOT NULL COMMENT '股票代码',
    `name` VARCHAR(50) COMMENT '股票名称',
    `free_date` DATE NOT NULL COMMENT '解禁日期',
    `lockup_type` VARCHAR(50) COMMENT '限售类型',
    `shares` DECIMAL(16,2) COMMENT '解禁数量(股)',
    `ratio` DECIMAL(10,4) COMMENT '解禁比例',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
    INDEX `idx_free_date` (`free_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='限售解禁日历';

-- 连板天梯日数据
CREATE TABLE IF NOT EXISTS `stock_board_daily` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `trade_date` DATE NOT NULL COMMENT '交易日期',
    `code` VARCHAR(10) NOT NULL COMMENT '股票代码',
    `name` VARCHAR(50) COMMENT '股票名称',
    `change_pct` DECIMAL(10,2) COMMENT '涨跌幅%',
    `close_price` DECIMAL(10,2) COMMENT '收盘价',
    `turnover` DECIMAL(10,2) COMMENT '换手率%',
    `reason` VARCHAR(500) COMMENT '题材归因',
    `board_count` INT DEFAULT 1 COMMENT '连板数',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
    UNIQUE KEY `uk_bd_code_date` (`code`, `trade_date`),
    INDEX `idx_bd_trade_date` (`trade_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='连板天梯日数据';

-- 东财全球资讯7x24
CREATE TABLE IF NOT EXISTS `stock_global_news` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `news_code` VARCHAR(100) NOT NULL COMMENT '资讯编码',
    `title` VARCHAR(500) NOT NULL COMMENT '标题',
    `summary` TEXT COMMENT '摘要',
    `show_time` VARCHAR(30) COMMENT '发布时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
    UNIQUE KEY `uk_news_code` (`news_code`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='东财全球资讯7x24';
