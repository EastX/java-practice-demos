-- 数据库
CREATE DATABASE IF NOT EXISTS `java-practice-demos` DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci;
USE `java-practice-demos`;

-- 加密示例用户表
CREATE TABLE IF NOT EXISTS `crypto_user` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(32) NOT NULL COMMENT '用户名',
  `password` varchar(32) NOT NULL COMMENT '加密后的密码，MD5加盐',
  `salt` varchar(32) NOT NULL COMMENT '加密密码使用的盐',
  `phone` varchar(50) NOT NULL COMMENT '手机号码，整体加密',
  `email` varchar(255) NOT NULL COMMENT '邮箱，模糊加密',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='加密示例用户表';

