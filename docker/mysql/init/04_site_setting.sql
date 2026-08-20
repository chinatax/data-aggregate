SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `site_setting` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `setting_key` VARCHAR(100) NOT NULL COMMENT '配置键',
    `setting_value` TEXT COMMENT '配置值',
    `setting_type` VARCHAR(20) DEFAULT 'text' COMMENT '值类型: text/color/html',
    `group_name` VARCHAR(50) DEFAULT 'identity' COMMENT '分组: identity/module/contact/email/upload',
    `description` VARCHAR(255) DEFAULT '' COMMENT '配置说明',
    `sort_order` INT DEFAULT 0 COMMENT '排序序号',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_setting_key` (`setting_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='站点设置表';

INSERT INTO `site_setting` (`setting_key`, `setting_value`, `setting_type`, `group_name`, `description`, `sort_order`) VALUES
('site_name', '数据聚合智能AI', 'text', 'identity', '站点名称（顶栏Logo、登录/注册页）', 1),
('site_subtitle', '数据聚合智能应用', 'text', 'identity', '站点副标题（浏览器标签页）', 2),
('login_slogan', '分享技术见解，记录成长点滴', 'text', 'identity', '登录页标语', 3),
('login_slogan_color', '#ffffff', 'color', 'identity', '登录页标语颜色', 4),
('register_slogan', '加入我们，开始创作', 'text', 'identity', '注册页标语', 5),
('register_slogan_color', '#ffffff', 'color', 'identity', '注册页标语颜色', 6),
('home_hero_description', '汇聚股票、电子书集、行政区划等多维度数据，提供一站式数据查询与洞察服务', 'text', 'identity', '首页Hero描述', 7),
('home_hero_description_color', '#ffffff', 'color', 'identity', '首页Hero描述颜色', 8),
('blog_slogan', '分享技术见解，记录成长点滴', 'text', 'identity', '博客页标语', 9),

('stock_desc', '实时行情、历史走势、智能分析', 'text', 'module', '股票模块描述', 11),
('ebook_desc', '海量图书、分类检索、智能推荐', 'text', 'module', '电子书模块描述', 12),
('district_desc', '全国省市区县、层级关联、数据查询', 'text', 'module', '行政区划模块描述', 13),
('module_placeholder', '模块开发中，敬请期待...', 'text', 'module', '开发中模块提示语', 14),

('system_version', 'v1.0.0', 'text', 'contact', '系统版本号', 21),
('author_bio', '技术博主 · 分享干货', 'text', 'contact', '默认作者签名', 22),
('copyright_text', '', 'text', 'contact', '页脚版权信息', 23),

('email_subject_prefix', '数据聚合', 'text', 'email', '邮件标题前缀', 31),
('verify_code_ttl_minutes', '5', 'text', 'email', '验证码有效期（分钟）', 32),

('upload_allowed_extensions', 'jpg,jpeg,png,gif,webp,bmp,svg', 'text', 'upload', '允许上传的图片格式', 41),
('upload_max_size_mb', '10', 'text', 'upload', '上传文件大小上限（MB）', 42),

('channels', '[{"path":"/home","name":"首页概览","icon":"HomeFilled"},{"path":"/blog","name":"博客","icon":"Document"},{"path":"/stock","name":"股票数据","icon":"TrendCharts"},{"path":"/ebook","name":"电子书集","icon":"Reading"},{"path":"/district","name":"行政区划","icon":"MapLocation"}]', 'json', 'channel', '频道列表（JSON格式，支持增删改）', 51);
