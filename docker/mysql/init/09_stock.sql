-- 财联社快讯
CREATE TABLE IF NOT EXISTS `stock_news_flash` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `news_id` VARCHAR(50) NOT NULL COMMENT '财联社新闻ID',
    `title` VARCHAR(500) NOT NULL COMMENT '标题',
    `content` TEXT COMMENT '正文',
    `publish_time` BIGINT COMMENT '发布时间戳(秒)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
    UNIQUE KEY `uk_news_id` (`news_id`),
    INDEX `idx_publish_time` (`publish_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='财联社快讯';

-- 每日热点题材
CREATE TABLE IF NOT EXISTS `stock_hot_topic` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `topic_date` DATE NOT NULL COMMENT '日期',
    `code` VARCHAR(10) NOT NULL COMMENT '股票代码',
    `name` VARCHAR(50) COMMENT '股票名称',
    `reason` VARCHAR(500) COMMENT '概念归因',
    `change_pct` DECIMAL(10,2) COMMENT '涨幅%',
    `turnover` DECIMAL(10,2) COMMENT '换手率%',
    `volume` BIGINT COMMENT '成交额(元)',
    `close_price` DECIMAL(10,2) COMMENT '收盘价',
    `market` VARCHAR(10) COMMENT '市场(沪/深/北)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
    INDEX `idx_date` (`topic_date`),
    INDEX `idx_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日热点题材';

-- 行业排名快照
CREATE TABLE IF NOT EXISTS `stock_industry_rank` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `snap_time` DATETIME NOT NULL COMMENT '快照时间',
    `code` VARCHAR(20) NOT NULL COMMENT '行业代码',
    `name` VARCHAR(50) COMMENT '行业名称',
    `change_pct` DECIMAL(10,2) COMMENT '涨跌幅%',
    `up_count` INT COMMENT '上涨家数',
    `down_count` INT COMMENT '下跌家数',
    `leader_name` VARCHAR(50) COMMENT '领涨股',
    `leader_change` DECIMAL(10,2) COMMENT '领涨股涨跌幅',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
    INDEX `idx_snap_time` (`snap_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='行业排名快照';
