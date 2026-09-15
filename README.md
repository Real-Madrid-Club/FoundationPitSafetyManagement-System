# 基坑安全管理系统

基于 Spring Boot、Vue 3、MySQL 和 Three.js 的基坑安全管理系统，面向监控中心、施工方和维修工程师，提供设备管理、监测数据分析、工单流转、AI 故障诊断、健康预测和三维数字孪生等功能。

## 一、项目功能

- **角色权限管理**：支持管理员、施工方用户和维修工程师等角色。
- **设备管理**：维护传感器档案，管理设备正常、预警、故障和报废状态。
- **工单管理**：支持创建、派单、签到、维修和验收等完整流程。
- **监测数据分析**：展示全站仪位移、伺服轴力和钢支撑温度等时序数据。
- **AI 故障诊断**：接入 DeepSeek API，提供健康监测、检修助手和趋势预测能力。
- **健康预测**：使用 Python 和 scikit-learn 训练设备安全运行时间预测模型。
- **三维数字孪生**：基于 Three.js 展示基坑模型和传感器状态。
- **数据大屏**：展示设备统计、告警趋势和待处理工单。

## 二、技术栈

| 模块 | 技术 |
| --- | --- |
| 后端 | Java 17、Spring Boot 3.2、MyBatis、Spring Security、JWT、WebFlux |
| 前端 | Vue 3、Vite、Element Plus、ECharts、Three.js、Axios |
| 数据库 | MySQL 8.0 |
| AI 服务 | DeepSeek API（deepseek-chat） |
| 预测算法 | Python、pandas、NumPy、scikit-learn、pymysql |

## 三、项目结构

```text
FoundationPitSafetyManagement-System/
├── backend/                    # Spring Boot 后端
│   └── src/main/
│       ├── java/cn/edu/cdu/pitsafety/
│       └── resources/
├── frontend/                   # Vue 3 前端
│   └── src/
│       ├── api/                # 接口封装
│       ├── views/              # 页面组件
│       └── router/             # 路由与权限守卫
├── sql/
│   ├── init_schema.sql         # 数据库表结构和基础数据
│   ├── import_*.sql            # 监测数据导入文件
│   ├── train_model.py          # 健康预测模型训练脚本
│   └── data/                   # 原始监测数据
└── README.md
```

## 四、环境要求

- JDK 17 或更高版本
- Node.js 16 或更高版本
- MySQL 8.0
- Python 3（仅在需要重新导入数据或训练模型时使用）

## 五、数据库初始化

项目后端默认连接以下数据库配置：

```text
数据库名：pit_safety_db
用户名：root
端口：3306
```

请先启动 MySQL，然后创建数据库并导入表结构：

```bash
cd /Users/vanvan/FoundationPitSafetyManagement-System

mysql -h 127.0.0.1 -P 3306 -u root -p \
  -e "CREATE DATABASE IF NOT EXISTS pit_safety_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

mysql -h 127.0.0.1 -P 3306 -u root -p pit_safety_db < sql/init_schema.sql
```

后端数据库密码配置位于：

`backend/src/main/resources/application.yml`

如果需要导入完整监测数据，继续执行：

```bash
mysql -h 127.0.0.1 -P 3306 -u root -p pit_safety_db < sql/import_adjust.sql
mysql -h 127.0.0.1 -P 3306 -u root -p pit_safety_db < sql/import_total_station.sql
mysql -h 127.0.0.1 -P 3306 -u root -p pit_safety_db < sql/import_steel_temp.sql
mysql -h 127.0.0.1 -P 3306 -u root -p pit_safety_db < sql/import_axial_force.sql
```

## 六、启动项目

### 1. 启动 MySQL

macOS 使用 Homebrew 安装 MySQL 时，可以执行：

```bash
brew services start mysql@8.0
```

### 2. 启动后端

在第一个终端执行：

```bash
cd /Users/vanvan/FoundationPitSafetyManagement-System/backend
./mvnw spring-boot:run
```

后端地址：`http://localhost:8081`

### 3. 启动前端

在第二个终端执行：

```bash
cd /Users/vanvan/FoundationPitSafetyManagement-System/frontend
npm install
npm run dev
```

前端地址：`http://localhost:5173`

如果已经安装过依赖，可以直接执行：

```bash
npm run dev
```

## 七、登录账号

| 账号 | 密码 | 角色 |
| --- | --- | --- |
| `admin` | `123456` | 监控中心管理员 |
| `buyer1` | `123456` | 施工方用户 |
| `repairer1` | `123456` | 现场维修工程师 |

## 八、DeepSeek 配置

AI 功能需要配置 DeepSeek API Key：

```bash
export DEEPSEEK_API_KEY="sk-你的key"
```

配置后重新启动后端即可。未配置 API Key 时，系统的登录、设备管理、工单管理和普通监测功能仍可使用，但 AI 相关功能无法正常调用。

## 九、功能演示流程

1. 使用 `admin` 登录，查看首页数据大屏。
2. 进入设备管理，查看传感器档案和设备状态。
3. 进入监测数据，选择传感器查看时序曲线。
4. 进入基坑模型，查看三维基坑和传感器状态。
5. 使用 AI 助手进行故障诊断或趋势预测。
6. 使用 `buyer1`、`admin` 和 `repairer1` 演示工单创建、派单、维修和验收流程。

## 十、项目分工

| 成员 | 身份 | 主要分工 |
| --- | --- | --- |
| 胡昱璠 | 组长 | 项目统筹、系统架构、后端核心功能、AI 模块、三维数字孪生、健康预测和代码整合 |
| 罗志林 | 组员 | 监测数据处理、模型算法支持、数据导入、项目文档和测试协助 |

## 十一、数据说明

项目包含以下主要监测数据：

| 数据表 | 内容 |
| --- | --- |
| `data_axial_force` | 伺服轴力数据 |
| `data_steel_temperature` | 钢支撑温度数据 |
| `data_total_station` | 全站仪位移数据 |
| `maintenance_adjust_record` | 轴力调整记录 |
| `device_info` | 设备档案 |
| `work_order` | 维修工单 |
