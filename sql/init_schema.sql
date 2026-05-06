-- 创建数据库并指定字符集，防止中文乱码
CREATE DATABASE IF NOT EXISTS `pit_safety_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `pit_safety_db`;

-- ==========================================================
-- 模块一：核心权限系统 (RBAC)
-- ==========================================================

-- 1. 角色表：定义系统中有哪些角色
CREATE TABLE `sys_role` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称(展示用)',
    `role_code` VARCHAR(50) NOT NULL UNIQUE COMMENT '角色编码(代码逻辑用)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 2. 用户表：存储所有登录账号信息
CREATE TABLE `sys_user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '登录账号',
    `password` VARCHAR(255) NOT NULL COMMENT '登录密码(BCrypt加密)',
    `real_name` VARCHAR(50) COMMENT '真实姓名',
    `phone` VARCHAR(20) COMMENT '联系电话',
    `status` TINYINT DEFAULT 1 COMMENT '状态(1:正常, 0:禁用)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 3. 用户-角色关联表：实现多对多关系
CREATE TABLE `sys_user_role` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';


-- ==========================================================
-- 模块二：设备档案与基础业务层
-- ==========================================================

-- 4. 设备档案表：记录设备的静态属性和生命周期状态
CREATE TABLE `device_info` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `device_code` VARCHAR(100) NOT NULL UNIQUE COMMENT '设备编码/传感器编码(如6501955, FRHY-01)',
    `device_name` VARCHAR(100) NOT NULL COMMENT '设备名称/位置标号(如 平台测点5, L3B)',
    `device_type` VARCHAR(50) NOT NULL COMMENT '设备类型(全站仪/温度传感器/伺服轴力计等)',
    `owner_id` BIGINT COMMENT '所属施工方(购买方)的用户ID',
    `ip_grade` VARCHAR(20) COMMENT '防尘防水等级(如IP68)',
    `maintenance_cycle` INT DEFAULT 30 COMMENT '维保周期(天)',
    `status` TINYINT DEFAULT 1 COMMENT '状态(1:正常, 2:异常预警, 3:故障待修, 4:已报废)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='基坑设备档案表';

-- 5. 故障/维保工单表：记录设备的维修流转过程
CREATE TABLE `work_order` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_no` VARCHAR(50) NOT NULL UNIQUE COMMENT '工单流水号(如WO202604010001)',
    `device_id` BIGINT NOT NULL COMMENT '关联设备ID',
    `creator_id` BIGINT COMMENT '发起人ID(自动预警可为空)',
    `repairer_id` BIGINT COMMENT '接单维修工ID',
    `fault_desc` TEXT COMMENT '故障现象描述或预警原因',
    `repair_log` TEXT COMMENT '维修人员回填的检修日志及耗材',
    `status` TINYINT DEFAULT 0 COMMENT '工单状态(0:待派单, 1:待接单, 2:维修中, 3:待验收, 4:已完成)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '报修/预警时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备维保流转工单表';


-- ==========================================================
-- 模块三：基坑真实监测数据表 (时序数据层)
-- ==========================================================

-- 6. 全站仪位移监测数据表 (对应: 全站仪数据.csv)
CREATE TABLE `data_total_station` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `sensor_code` VARCHAR(100) NOT NULL COMMENT '传感器编码(外键关联device_info,如 FRHY-01)',
    `collect_time` DATETIME NOT NULL COMMENT '数据采集时间',
    `delta_x` DECIMAL(10,3) COMMENT '单次X位移量 ΔX(mm)',
    `delta_y` DECIMAL(10,3) COMMENT '单次Y位移量 ΔY(mm)',
    `delta_h` DECIMAL(10,3) COMMENT '单次高程位移 ΔH(mm)',
    `total_x` DECIMAL(10,3) COMMENT '累计X位移 ∑X(mm)',
    `total_y` DECIMAL(10,3) COMMENT '累计Y位移 ∑Y(mm)',
    `total_h` DECIMAL(10,3) COMMENT '累计高程位移 ∑H(mm)',
    `temperature` DECIMAL(8,2) COMMENT '环境温度(℃)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '系统落库时间',
    INDEX `idx_sensor_time` (`sensor_code`, `collect_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='全站仪位移时序数据';

-- 7. 伺服轴力监测数据表 (对应: sp1_data.xlsx, 4p1_data.xlsx)
CREATE TABLE `data_axial_force` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `sensor_code` VARCHAR(100) NOT NULL COMMENT '传感器编码(外键关联device_info,如 SP1)',
    `collect_time` DATETIME NOT NULL COMMENT '数据采集时间 (Time)',
    `w_force` DECIMAL(10,2) COMMENT '轴力值 WForce (kN)',
    `f_position` DECIMAL(10,2) COMMENT '行程/位移 FPosition',
    `temperature` DECIMAL(8,2) COMMENT '支撑温度 Temperature',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_sensor_time` (`sensor_code`, `collect_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='伺服轴力计监测数据';

-- 8. 钢支撑温度监测数据表 (对应: 钢支撑温度数据.csv)
CREATE TABLE `data_steel_temperature` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `sensor_code` VARCHAR(100) NOT NULL COMMENT '传感器编码(外键关联device_info,如 6501945)',
    `collect_time` DATETIME NOT NULL COMMENT '数据采集时间',
    `temperature` DECIMAL(10,6) COMMENT '精确温度值',
    `measure_val` DECIMAL(10,4) COMMENT '原始测量值',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_sensor_time` (`sensor_code`, `collect_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='钢支撑表面温度数据';


-- ==========================================================
-- 模块四：维保与调整记录扩展层
-- ==========================================================

-- 9. 现场参数调整记录表 (对应: 轴力调整记录.xlsx)
CREATE TABLE `maintenance_adjust_record` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `sensor_code` VARCHAR(100) NOT NULL COMMENT '调整对象(关联device_info,如 SP1)',
    `adjust_content` VARCHAR(255) NOT NULL COMMENT '调整内容描述(如:轴力调整为 200 kN)',
    `operate_time` DATETIME NOT NULL COMMENT '记录发生时间 (CreateTime)',
    `operator_id` BIGINT COMMENT '操作人(关联系统用户ID)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='现场设备参数调整记录';


-- ==========================================================
-- 模块五：系统初始化测试数据
-- ==========================================================

-- 1. 初始化三种角色
INSERT INTO `sys_role` (`id`, `role_name`, `role_code`) VALUES 
(1, '监控中心管理员', 'ROLE_ADMIN'),
(2, '施工方购买用户', 'ROLE_BUYER'),
(3, '现场维修工程师', 'ROLE_REPAIRER');

-- 2. 初始化超管账号 (密码默认明文 123456，供开发期使用)
INSERT INTO `sys_user` (`id`, `username`, `password`, `real_name`) VALUES 
(1, 'admin', '123456', '系统超管');

-- 3. 关联超管角色
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES (1, 1);

-- 4. 初始化部分真实设备档案 (根据你提供的图片数据提取)
INSERT INTO `device_info` (`device_code`, `device_name`, `device_type`) VALUES 
('6501955', '平台测点5', '传感器'),
('6501961', '平台测点4', '传感器'),
('6501945', 'L3B', '温度传感器'),
('FRHY-01', '基准点/平台测点', '全站仪'),
('SP1', 'SP1轴力计', '伺服轴力计');
