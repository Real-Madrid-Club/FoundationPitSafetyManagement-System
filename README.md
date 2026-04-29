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
```
🚀 快速开始
1. 环境准备
确保本地开发环境已安装以下软件：

JDK 17 或以上

Node.js 16.x 或以上

MySQL 8.0

2. 初始化数据库
登录本地 MySQL，创建名为 pit_safety_db 的数据库。

运行项目根目录 sql/init_schema.sql 脚本，完成表结构与基础测试数据的初始化。

3. 启动后端
```bash
cd backend
# 请在 application.yml 中修改数据库连接的账号密码
mvn spring-boot:run
4. 启动前端
```
```bash
cd frontend
npm install   # 安装依赖
npm run dev   # 启动开发服务器
前端服务默认运行在 http://localhost:5173
```
