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
source sql/base/init_schema.sql;
source sql/business/work_schema.sql;

-- 测试数据（可选）
source sql/business/test_data.sql;
```

### 配置

1. 复制 `.env.example` 为 `.env`，填入 DeepSeek API Key（内容审核用，不配也能跑）
2. 修改 `backend/src/main/resources/application-dev.yml` 中的数据库和 Redis 连接信息

### 启动方式

#### 开发环境

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

#### 服务器部署

项目根目录为部署包结构，服务器上预期运行环境：

- JDK 17+
- MySQL 8
- Redis 7
- Nginx
- PM2

```bash
# 首次部署：建库并导入 SQL
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS jingxuan CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -p jingxuan < sql/base/init_schema.sql
mysql -u root -p jingxuan < sql/business/work_schema.sql

# 配置环境变量
cp .env.example .env
# 编辑 .env 填入实际配置

# 部署启动
bash deploy-server.sh
```

## 功能概览

| 角色 | 功能 |
|------|------|
| 学生 | 提交作品、审核状态跟踪、查看评分与排行 |
| 教师 | 匿名评分（创新性/技术难度/完成度/实用性）、排行榜 |
| 管理员 | 审核发布、精选作品、评分批次、奖项配置、系统管理 |
| 游客 | 浏览作品、排行榜、作品详情 |

## 项目结构

```
菁选/
├── backend/                 # 后端源码（Spring Boot + MyBatis-Plus）
│   ├── src/main/java/com/jingxuan/
│   │   ├── config/          # 配置类（Security、Jackson、Knife4j、DeepSeek）
│   │   ├── security/        # JWT 认证过滤器与异常处理
│   │   ├── modules/         # 业务模块（work、audit、score、rank、publish 等）
│   │   ├── adapter/         # ★ 核心模式：各端 API 适配控制器
│   │   ├── mapper/          # MyBatis-Plus Mapper
│   │   └── common/          # BaseEntity、Result、PageResult
│   └── sql/                 # 建表 SQL
├── frontend/                # 前端源码（Vue 3 + TypeScript + Element Plus）
│   └── src/
│       ├── views/           # 页面（按角色 admin/student/teacher/public 分）
│       ├── api/             # API 封装（含数据适配层：后端枚举/字段 → 前端格式）
│       ├── router/          # 路由与鉴权守卫
│       └── layout/          # 布局组件
├── sql/                     # 部署用 SQL
├── docs/                    # 设计文档与接口规范
├── ecosystem.config.cjs     # PM2 配置
└── nginx-jingxuan.conf      # Nginx 配置
```

## 评分规则

四个维度，满分 100 分：
- 创新性：25 分
- 技术难度：25 分
- 完成度：30 分
- 实用性：20 分
