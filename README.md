# 🏗️ 基坑安全管理系统

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)
![Vue.js](https://img.shields.io/badge/Vue.js-3.x-blue.svg)
![MySQL](https://img.shields.io/badge/MySQL-8.0-orange.svg)
![Three.js](https://img.shields.io/badge/Three.js-r170-black.svg)
![DeepSeek AI](https://img.shields.io/badge/AI-DeepSeek-purple.svg)

> 《JavaWeb应用开发》期末综合项目考核 · 目标：A档优秀项目 🏆

---

## 📖 项目简介

基坑工程是建筑施工中**最危险的环节**之一。本项目面向**监控中心、施工方、维修工程师**三类角色，构建了一套集**设备管理、工单调度、AI故障诊断、趋势预测、3D数字孪生**于一体的基坑安全管理系统。

- **35 万条**真实基坑监测数据（全站仪位移、伺服轴力、钢支撑温度）
- 覆盖 **24 台**传感器，数据时间跨 3 个月
- 基于 **PitMonitor-TwinBench** 学术基准数据集校准
- AI 识别 **9 种**异常类型（物理失效 / 传感器故障 / 温度补偿误差 等）

---

## ✨ 核心功能

| 模块 | 功能 | 技术亮点 |
|------|------|----------|
| 👥 **角色权限** | admin/buyer/repairer 三级隔离 | 菜单不同、按钮不同、数据互不可见 |
| 📦 **设备管理** | 设备档案 CRUD + 状态流转（正常→预警→故障→报废） | 14 台真实传感器 |
| 🔄 **工单流转** | 创建→指派→签到→维修→验收 全链路状态机 | SQL WHERE 前驱状态校验防并发 |
| 🤖 **AI 诊断** | 健康监测 / 检修助手 / 趋势预测 三种模式 | DeepSeek + System Prompt 注入校准参数 |
| 🪞 **3D 数字孪生** | 基坑模型 + 24 传感器嵌入 + 血条 | Three.js + CSS2DRenderer |
| 📊 **健康预测** | 每台设备剩余安全运行时间预测 | Python scikit-learn 线性回归 + calibration.json |
| 📈 **数据大屏** | 设备状态饼图 + 告警趋势折线图 | ECharts 实时渲染 |
| ⏱ **监测页面** | 3 类数据 Tab 切换 + 时序曲线缩放 | dataZoom 交互 + 智能预测一键触发 |

---

## 🛠️ 技术栈

| 层 | 技术 |
|----|------|
| **后端** | Java 17 · Spring Boot 3.2 · MyBatis · Spring Security · JWT · WebFlux |
| **前端** | Vue 3 · Element Plus · ECharts · Three.js · CSS2DRenderer · Axios · Vue Router |
| **数据库** | MySQL 8.0 |
| **AI** | DeepSeek API (deepseek-chat) |
| **预测算法** | Python 3 · scikit-learn · NumPy · pymysql |

---

## 📁 目录结构

```text
FoundationPitSafetyManagement-System/
├── backend/
│   └── src/main/java/cn/edu/cdu/pitsafety/
│       ├── ai/              # DeepSeek AI 模块
│       ├── dashboard/        # 大屏统计 API
│       ├── monitor/          # 监测数据时序 API
│       ├── predict/          # 设备健康预测 API
│       ├── security/         # Spring Security + JWT
│       └── system/           # 用户/设备/工单 CRUD
├── frontend/
│   └── src/
│       ├── api/              # Axios 接口封装 (6个模块)
│       ├── views/            # 页面组件 (含 FoundationPit3D)
│       └── router/           # 路由 + 角色守卫
├── sql/
│   ├── init_schema.sql       # 9张表建表 + 种子数据
│   ├── import_data.py        # 数据导入脚本
│   ├── train_model.py        # 健康预测模型训练脚本
│   ├── calibration.json      # PitMonitor-TwinBench 校准参数
│   └── data/                 # 原始 CSV/Excel 数据文件
├── openspec/                 # 开发文档 + 分工安排
└── README.md
```

---

## 🚀 快速开始

### 1. 环境要求

- JDK 17+
- Node.js 16+
- MySQL 8.0
- Python 3 + pip

### 2. 初始化数据库

```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS pit_safety_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 建表 + 种子数据（3个角色 + admin + 14台设备）
mysql -u root -p pit_safety_db < sql/init_schema.sql
```

### 3. 导入监测数据

```bash
pip3 install pandas openpyxl

# 生成 INSERT SQL 文件
python3 sql/import_data.py

# 导入 35 万条数据（按文件从小到大）
mysql -u root -p pit_safety_db < sql/import_adjust.sql
mysql -u root -p pit_safety_db < sql/import_total_station.sql
mysql -u root -p pit_safety_db < sql/import_steel_temp.sql
mysql -u root -p pit_safety_db < sql/import_axial_force.sql
```

### 4. 训练健康预测模型

```bash
pip3 install pymysql scikit-learn
python3 sql/train_model.py
# 输出: backend/src/main/resources/model_params.json
```

### 5. 配置 DeepSeek API Key

```bash
echo 'export DEEPSEEK_API_KEY="sk-你的key"' >> ~/.zshrc
source ~/.zshrc
```

### 6. 修改数据库密码

编辑 `backend/src/main/resources/application.yml`，把 `password` 改成你的 MySQL 密码。

### 7. 启动后端

```bash
cd backend
./mvnw spring-boot:run
# → http://localhost:8081
```

### 8. 启动前端

```bash
cd frontend
npm install
npm run dev
# → http://localhost:5173
```

### 9. 登录

| 账号 | 密码 | 角色 |
|------|------|------|
| `admin` | `123456` | 监控中心管理员 |
| `buyer1` | `123456` | 施工方购买用户 |
| `repairer1` | `123456` | 现场维修工程师 |

---

## 📊 数据概览

| 表 | 数据量 | 说明 |
|----|--------|------|
| `data_axial_force` | 181,527 | 伺服轴力(SP1/4P1/4P2) |
| `data_steel_temperature` | 122,127 | 钢支撑温度(8台) |
| `data_total_station` | 46,622 | 全站仪位移(11台) |
| `maintenance_adjust_record` | 1,449 | 轴力调整记录 |
| `device_info` | 14 | 设备档案 |
| `work_order` | 运行时 | 工单流转 |
| **合计** | **351,725** | 跨越 2025-12 ~ 2026-03 |

---

罗志林 (文档与模型算法)：项目模型算法设计、期末汇报文档撰写。
## 🎮 功能演示路线

### 新手上手 5 步

1. **登录 admin** → 首页大盘看设备统计和告警趋势
2. **设备管理** → 查看 14 台传感器档案
3. **监测数据** → 切「伺服轴力」Tab → 选 SP1 → 看时序曲线 → 点 🔮智能预测
4. **基坑模型** → 3D 全景 → 旋转 / 缩放 → 点传感器球看数据 → 看血条健康度
5. **AI 助手** → 切「健康监测」→ 粘贴异常数据 → AI 诊断故障类型

### 三角色协作流程

```
buyer1 创建工单 → admin 派单给张工 → repairer1 签到 → 维修完成 → buyer1 验收
```

> 💡 三个账号用不同浏览器（Chrome / Firefox / Safari）同时登录，可演示完整协作。

---

## 👨‍💻 开发团队

| 成员 | 核心贡献 |
|------|----------|
| **胡昱璠** (组长) | 架构设计 · AI 模块 · 3D 基坑模型 · 健康预测算法 · 数据入库 · Code Review |
| **袁梓昊** (后端) | RBAC 权限 · 监测数据 API · 大屏统计 API · JWT 认证 · Spring Security |
| **吴博** (后端) | 设备管理 CRUD · 工单全链路 API · 角色数据隔离 · 状态机 SQL |
| **胡智慧** (前端) | 全部页面搭建 · ECharts 图表 · 角色隔离 UI · 前后端联调 · API 封装 |
| **罗志林** (文档和建模、模型算法) | 数据入库 · 期末报告 · 集成测试 · 3D 模型处理算法 |

---

## 📝 Git 协作规范

```bash
# 开工前
git pull origin main

# 提交
git add .
git commit -m "feat: xxx"   # fix: xxx / docs: xxx / style: xxx

# 推送
git push origin main
```

> 仓库地址：`github.com/Real-Madrid-Club/FoundationPitSafetyManagement-System`
