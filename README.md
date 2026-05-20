# 菁选 - 校园作品展示平台

基于 Spring Boot 3 + Vue 3 的全栈校园作品展示平台，支持学生提交作品、教师匿名评分、管理员审核发布、前台公开展示等功能。

## 技术栈

**后端**
- Java 17 + Spring Boot 3.2.5
- MyBatis-Plus 3.5.7 + MySQL
- Redis (Lettuce)
- Spring Security + JWT (jjwt 0.12.5)
- Knife4j (API 文档)
- DeepSeek API (内容安全审核)

**前端**
- Vue 3 + TypeScript + Vite 8
- Element Plus 2.14 + Pinia 3
- Axios + Vue Router

## 快速启动

### 环境要求

- JDK 17+
- Maven 3.9+
- Node.js 20+
- MySQL 8.0+
- Redis 7+

### 数据库初始化

```sql
-- 建库
CREATE DATABASE jingxuan CHARACTER SET utf8mb4;

-- 建表
source backend/sql/base/init_schema.sql;
source backend/sql/business/work_schema.sql;

-- 测试数据（可选）
source backend/sql/business/test_data.sql;
```

### 配置

1. 复制 `.env.example` 为 `.env`，填入 DeepSeek API Key（内容审核用，不配也能跑）
2. 修改 `backend/src/main/resources/application-dev.yml` 中的数据库和 Redis 连接信息

### 一键启动

直接双击 `start.bat`，会自动打包后端、启动后端（端口 8080）、启动前端（端口 5173）。

或手动启动：

```bash
# 后端
cd backend
mvn package -DskipTests
java -jar target/jingxuan-backend-1.0.0.jar --spring.profiles.active=dev

# 前端
cd frontend
npm install
npm run dev
```

- 前端地址：http://localhost:5173
- 后端地址：http://localhost:8080
- API 文档：http://localhost:8080/doc.html

## 功能概览

| 角色 | 功能 |
|------|------|
| 学生 | 注册登录、提交作品、管理作品 |
| 教师 | 匿名评分（创新性/技术难度/完成度/实用性）、排行榜 |
| 管理员 | 审核发布、精选作品、评分批次、奖项配置、系统管理 |
| 游客 | 浏览作品、排行榜、作品详情 |

## 项目结构

```
菁选/
├── backend/          # 后端源码
│   └── src/main/java/com/jingxuan/
│       ├── config/         # 配置类
│       ├── security/       # JWT 认证
│       ├── modules/        # 业务模块
│       │   ├── work/       # 作品管理
│       │   ├── audit/      # 审核管理
│       │   ├── score/      # 评分管理
│       │   ├── publish/    # 发布管理
│       │   ├── rank/       # 排行榜
│       │   ├── sensitive/  # 内容审核
│       │   └── ...
│       └── adapter/        # API 适配控制器
├── frontend/         # 前端源码
│   └── src/
│       ├── views/          # 页面（按角色分）
│       ├── api/            # 接口封装
│       ├── router/         # 路由
│       └── layout/         # 布局
└── docs/             # 文档
```

## 评分规则

四个维度，满分 100 分：
- 创新性：25 分
- 技术难度：25 分
- 完成度：30 分
- 实用性：20 分
