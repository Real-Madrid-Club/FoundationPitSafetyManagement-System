# 🏗️ 基坑安全管理系统 (Foundation Pit Safety Management System)

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)
![Vue.js](https://img.shields.io/badge/Vue.js-3.x-blue.svg)
![MySQL](https://img.shields.io/badge/MySQL-8.0-orange.svg)
![License](https://img.shields.io/badge/License-MIT-green.svg)

> 《JavaWeb应用开发》期末综合项目考核 - 目标：A档优秀项目 🏆

## 📖 项目简介

基坑工程是建筑施工中最危险的环节之一，设备正常运行是保障安全的核心。本项目是一个面向多角色的**基坑设备维保与智能工单调度系统**。
系统不仅实现了基础的设备生命周期管理与工单流转，还引入了 RBAC 细粒度权限控制，并探索性地融入了 AI/智能预警算法，以解决复杂施工环境下的设备运维痛点。

## ✨ 核心功能与业务闭环

* **👥 多角色权限隔离 (RBAC)**：系统划分为购买用户（施工方）、管理方（监控中心）与现场维修工，各司其职，数据绝对隔离。
* **📦 设备档案中心**：全面管理测斜仪、水位计、土压力计等专业设备的入库、防尘防水等级(IP)、标定周期及报废流程。
* **🔄 智能工单流转**：涵盖“一键报修 -> 智能派单 -> 现场签到 -> 结果回填 -> 验收确认”的全链路状态机。
* **🤖 AI/算法赋能 (亮点)**：在工单维修环节接入 AI 检修助手，辅助现场人员快速排障；后台持续监测设备健康度，预防异常跳变。

## 🛠️ 技术栈选型

本项目采用主流的**前后端分离**架构进行开发：

### 后端 (Backend)
* **核心框架**: Spring Boot
* **持久层**: MyBatis / MyBatis-Plus
* **数据库**: MySQL 8.0
* **安全与权限**: Spring Security + JWT 加密鉴权
* **创新层**: Python 预警算法 / LLM 大模型 API 接入

### 前端 (Frontend)
* **核心框架**: Vue.js 3.x
* **UI 组件库**: Element Plus
* **网络请求**: Axios
* **路由管理**: Vue Router

## 📁 目录结构

```text
FoundationPitSafetyManagement-System/
├── backend/            # Spring Boot 后端工程代码
├── frontend/           # Vue 3 前端工程代码
├── docs/               # 项目文档、接口文档与系统架构图
├── sql/                # 数据库初始化脚本 (init_schema.sql)
└── README.md           # 项目说明文件
## 🚀 快速开始

### 1. 环境准备

- JDK 17+
- Node.js 16+
- MySQL 8.0
- Python 3（数据导入用）

### 2. 初始化数据库

```bash
# 登录 MySQL 创建数据库
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS pit_safety_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 建表 + 种子数据
mysql -u root -p pit_safety_db < sql/init_schema.sql

# 生成监测数据导入文件（需先安装 pandas: pip3 install pandas openpyxl）
python3 sql/import_data.py

# 导入 35 万条真实监测数据
mysql -u root -p pit_safety_db < sql/import_adjust.sql
mysql -u root -p pit_safety_db < sql/import_total_station.sql
mysql -u root -p pit_safety_db < sql/import_steel_temp.sql
mysql -u root -p pit_safety_db < sql/import_axial_force.sql
```

### 3. 配置 DeepSeek API Key

```bash
export DEEPSEEK_API_KEY="sk-你的key"
```

或写入 `~/.zshrc` 永久生效：
```bash
echo 'export DEEPSEEK_API_KEY="sk-你的key"' >> ~/.zshrc
source ~/.zshrc
```

### 4. 修改数据库密码

打开 `backend/src/main/resources/application.yml`，将 `password` 改为你的 MySQL 密码：

```yaml
spring:
  datasource:
    password: 你的密码
```

### 5. 启动后端

```bash
cd backend
./mvnw spring-boot:run
```
后端运行在 `http://localhost:8081`

### 6. 启动前端

```bash
cd frontend
npm install
npm run dev
```
前端运行在 `http://localhost:5173`

### 7. 登录

| 账号 | 密码 | 角色 |
|------|------|------|
| `admin` | `123456` | 监控中心管理员 |
| `buyer1` | `123456` | 施工方购买用户 |
| `repairer1` | `123456` | 现场维修工程师 |
👨‍💻 开发团队与核心分工 (计科24-1)
本项目由 5 人团队协作完成：

胡昱璠 (技术负责人)：架构设计、数据库模型、核心算法/AI对接、Git 工作流审查与代码合并。

袁梓昊 (后端开发)：RBAC 权限模块、用户认证体系(Token/拦截器)搭建。

吴博   (后端开发)：设备档案管理、故障报修与工单流转全链路接口开发。

胡智慧 (前端开发)：Vue 3 页面搭建、Element Plus 组件封装、前后端接口联调。

罗志林 (文档与测开)：项目协作管理(Worktile记录)、期末汇报文档撰写、系统各角色集成测试。


## 📝 团队 Git 协作必读指南 (小白保姆级教程)

为了防止代码互相覆盖或仓库混乱，请所有前端和后端组员**严格遵守**以下提交流程。

### 🚨 第 0 步：永远先确认你所在的文件夹！
遇到 `fatal: not a git repository` 报错，是因为你没进到项目文件夹里。
每次打开终端准备敲 Git 命令前，**第一件事必须是进入项目根目录**：
```bash
# Mac 用户：
cd ~/FoundationPitSafetyManagement-System

# Windows 用户 (根据你存放的实际盘符和路径修改)：
cd D:\你的文件夹\FoundationPitSafetyManagement-System
```
🌅 第 1 步：每天开工第一件事 —— 拉取最新代码
千万不要一上来就写代码！先把你队友昨晚提交的代码拉到你自己的电脑上：

```Bash
git pull origin main
```
💻 第 2 步：愉快地写代码...
🌙 第 3 步：写完收工 —— 提交代码“三部曲”
写完一个功能或下班前，按照顺序执行以下三行命令：

```Bash
# 1. 把你修改的所有文件统统装进“暂存区”（注意最后有个小圆点，代表全部文件）
git add .
```
```
# 2. 给这次提交写个清晰的备注（必须按照下面的规范写）
git commit -m "feat: 写清楚你这次干了什么，比如：吴博完成了设备列表查询接口"
```
```
# 3. 把代码推送到云端仓库
git push origin main
```
🏷️ 附：Commit 备注前缀规范
feat: 开发了新功能（例如：feat: 新增登录页 UI）

fix: 修复了 Bug（例如：fix: 修复点击查询按钮报错的问题）

docs: 只修改了文档或 README

style: 修改了代码格式、注释（不影响具体逻辑）
