-- 博客表
CREATE TABLE IF NOT EXISTS `blog` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `title` VARCHAR(200) NOT NULL COMMENT '文章标题',
    `summary` VARCHAR(500) DEFAULT NULL COMMENT '文章摘要',
    `content` LONGTEXT DEFAULT NULL COMMENT '文章内容（Markdown）',
    `cover_image` VARCHAR(500) DEFAULT NULL COMMENT '封面图URL',
    `tags` VARCHAR(200) DEFAULT NULL COMMENT '标签，逗号分隔',
    `category` VARCHAR(50) DEFAULT NULL COMMENT '分类',
    `author` VARCHAR(100) DEFAULT '管理员' COMMENT '作者',
    `view_count` INT DEFAULT 0 COMMENT '浏览量',
    `like_count` INT DEFAULT 0 COMMENT '点赞数',
    `status` VARCHAR(20) DEFAULT 'DRAFT' COMMENT '状态：DRAFT草稿/PUBLISHED已发布/PRIVATE私密',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`),
    KEY `idx_category` (`category`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='博客文章表';

-- 插入示例数据
INSERT INTO `blog` (`title`, `summary`, `content`, `tags`, `category`, `author`, `view_count`, `like_count`, `status`) VALUES
('Spring Boot 3 新特性详解与实践', 'Spring Boot 3 基于 Jakarta EE 9 和 Java 17，带来了大量新特性和性能优化。本文将深入探讨这些变化及其在实际项目中的应用...', '# Spring Boot 3 新特性详解与实践\n\nSpring Boot 3 是基于 Jakarta EE 9 和 Java 17 的重大版本更新，带来了大量新特性和性能优化。\n\n## 1. Jakarta EE 迁移\n\nSpring Boot 3 最大的变化之一是从 Java EE (javax.*) 迁移到 Jakarta EE (jakarta.*)。\n\n## 2. 原生镜像支持\n\nSpring Boot 3 正式支持 GraalVM 原生镜像。\n\n## 总结\n\nSpring Boot 3 是一次重要的升级，建议新项目直接使用。', 'Spring Boot,Java,后端', 'BACKEND', '技术博主', 2341, 156, 'PUBLISHED'),
('Vue 3 Composition API 最佳实践', 'Composition API 是 Vue 3 的核心特性之一，它提供了一种更灵活、更可复用的组织组件逻辑的方式。', '# Vue 3 Composition API 最佳实践\n\nComposition API 是 Vue 3 的核心特性之一。\n\n## setup 函数\n\nsetup 是 Composition API 的入口。\n\n## 响应式系统\n\n使用 ref 和 reactive 创建响应式数据。', 'Vue3,前端,TypeScript', 'FRONTEND', '技术博主', 1892, 98, 'PUBLISHED'),
('MySQL 索引优化：从原理到实战', '索引是数据库性能优化的关键。本文将从 B+ 树原理出发，深入讲解索引的创建策略。', '# MySQL 索引优化\n\n索引是数据库性能优化的关键。\n\n## B+ 树原理\n\nMySQL InnoDB 使用 B+ 树作为索引结构。\n\n## 最左前缀原则\n\n联合索引的使用需要遵循最左前缀原则。', 'MySQL,数据库,性能优化', 'DATABASE', '技术博主', 3105, 203, 'PUBLISHED'),
('Docker Compose 多服务编排实战', '在实际项目中，往往需要同时运行多个服务。本文将详细介绍如何使用 Docker Compose 进行多服务编排。', '# Docker Compose 多服务编排\n\nDocker Compose 是定义和运行多容器 Docker 应用的工具。\n\n## docker-compose.yml\n\n使用 YAML 文件配置应用服务。\n\n## 网络配置\n\nCompose 会自动创建一个默认网络。', 'Docker,DevOps,容器', 'DEVOPS', '技术博主', 1567, 87, 'PUBLISHED'),
('从零构建数据聚合平台：架构设计与实现', '数据聚合是现代应用的核心需求之一。本文将分享如何从零开始构建一个可扩展的数据聚合平台。', '# 从零构建数据聚合平台\n\n数据聚合是现代应用的核心需求之一。\n\n## 技术选型\n\n- Spring Boot 3\n- Vue 3\n- MySQL\n- Redis\n- RocketMQ\n\n## 架构设计\n\n采用前后端分离架构。', '架构,数据聚合,系统设计', 'BACKEND', '技术博主', 4567, 312, 'PUBLISHED'),
('RocketMQ 消息队列实战指南', '消息队列是分布式系统的重要组成部分。本文将详细介绍 RocketMQ 的核心概念。', '# RocketMQ 消息队列实战\n\nRocketMQ 是阿里巴巴开源的分布式消息中间件。\n\n## 核心概念\n\n- Producer：消息生产者\n- Consumer：消息消费者\n- Broker：消息存储和转发服务器\n- NameServer：路由注册中心', 'RocketMQ,消息队列,分布式', 'BACKEND', '技术博主', 2134, 145, 'PUBLISHED');
