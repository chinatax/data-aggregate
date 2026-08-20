ALTER TABLE `category` ADD COLUMN `system` TINYINT NOT NULL DEFAULT 0 COMMENT '是否系统内置';

INSERT INTO `category` (`code`, `name`, `sort_order`, `system`) VALUES ('__FAVORITES__', '我的收藏', 99, 1)
ON DUPLICATE KEY UPDATE `system` = 1;
