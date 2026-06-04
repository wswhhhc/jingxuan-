# 菁选 (Jingxuan) — 校园作品展示平台

基于 **Spring Boot 3.2.5 + Vue 3** 的全栈校园作品展示平台，支持学生提交作品、教师匿名评分、管理员审核发布、前台公开展示。

## 项目定位

本仓库是 **生产部署包**，包含编译后的后端 JAR、构建后的前端静态资源、SQL 迁移脚本及部署配置。完整源码位于其他分支。

## 技术栈

| 层 | 技术 |
|---|---|
| 后端框架 | Spring Boot 3.2.5 / Java 17 |
| ORM | MyBatis-Plus 3.5.7 |
| 数据库 | MySQL 8.0+（字符集 utf8mb4） |
| 缓存 | Redis 7+（Lettuce） |
| 安全 | Spring Security + JWT（jjwt 0.12.5） |
| API 文档 | Knife4j 4.5.0（OpenAPI 3） |
| 内容审核 | DeepSeek API |
| 前端 | Vue 3 + TypeScript + Vite 8 |
| UI | Element Plus 2.14 + Pinia 3 |
| 部署 | PM2 + Nginx |

## 功能概览

| 角色 | 功能 |
|------|------|
| 学生 | 提交作品、审核状态跟踪、查看评分与排行 |
| 教师 | 匿名评分（创新性 25 + 技术难度 25 + 完成度 30 + 实用性 20）、排行榜 |
| 管理员 | 审核发布、精选作品、评分批次、奖项配置、系统管理、发送通知 |
| 游客 | 浏览作品、排行榜、作品详情 |

## 服务器部署

### 环境要求

- JDK 17+
- MySQL 8.0+
- Redis 7+
- Nginx
- PM2（`npm install -g pm2`）

### 快速部署

```bash
# 1. 建库
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS jingxuan CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 2. 导入基础表
mysql -u root -p jingxuan < sql/base/init_schema.sql
mysql -u root -p jingxuan < sql/business/work_schema.sql

# 3. 按时间顺序执行增量迁移（sql/business/ 下 yyyy-MM-dd-*.sql 文件）
mysql -u root -p jingxuan < sql/business/2026-05-19-fix-work-attachment-nullable.sql
mysql -u root -p jingxuan < sql/business/2026-05-24-like-fav-view-tag.sql
mysql -u root -p jingxuan < sql/business/2026-06-02-runtime-support.sql
mysql -u root -p jingxuan < sql/business/2026-06-03-server-preview-clean-runtime.sql
mysql -u root -p jingxuan < sql/business/2026-06-04-email-verification.sql
mysql -u root -p jingxuan < sql/business/2026-06-04-remove-sourcecode-concept.sql

# 4. 测试数据（可选）
mysql -u root -p jingxuan < sql/business/test_data.sql

# 5. 配置环境变量
cp .env.example .env
# 编辑 .env，填入 DB_PASSWORD、DEEPSEEK_API_KEY、邮件配置等

# 6. 一键部署
bash deploy-server.sh
```

部署脚本会自动：
- 从 `.env` 加载环境变量
- 创建 `uploads/` 目录
- 通过 PM2 启动后端 JAR（端口 8080，profile=prod）
- 复制 Nginx 配置并重载

### 访问地址

| 服务 | 地址 |
|------|------|
| 前端页面 | `http://服务器IP/` |
| 后端 API | `http://127.0.0.1:8080/` |
| API 文档 | `http://127.0.0.1:8080/doc.html` |

## 项目结构

```
/opt/jingxuan/
├── backend/
│   ├── jingxuan-backend-1.0.0.jar    # 编译后的后端可执行 JAR
│   ├── AdminNotifyController.java     # 管理端通知控制器（源码参考）
│   ├── RegistrationController.java    # 注册控制器（源码参考）
│   └── META-INF/maven/.../pom.xml     # Maven 依赖清单
├── frontend/dist/                     # 构建后的前端静态资源
│   └── index.html                     # SPA 入口
├── sql/
│   ├── base/init_schema.sql           # 基础建表（8 张系统表）
│   └── business/
│       ├── work_schema.sql            # 业务表（12 张）
│       ├── test_data.sql              # 测试种子数据
│       └── yyyy-MM-dd-*.sql           # 增量迁移（按时间执行）
├── uploads/                           # 用户上传文件（前端 .gitignore 忽略）
├── .env.example                       # 环境变量模板
├── .gitignore
├── ecosystem.config.cjs               # PM2 进程配置
├── nginx-jingxuan.conf                # Nginx 反向代理配置
└── deploy-server.sh                   # 一键部署脚本
```

## 评分规则

四维度评分，满分 100 分：

| 维度 | 分值 |
|------|------|
| 创新性 | 25 |
| 技术难度 | 25 |
| 完成度 | 30 |
| 实用性 | 20 |

## 数据库迁移规范

- 基础表：`sql/base/init_schema.sql`
- 业务表：`sql/business/work_schema.sql`
- 增量迁移：`sql/business/yyyy-MM-dd-描述.sql`
- 所有实体继承 `BaseEntity`（雪花算法 id、createTime、updateTime、逻辑删除 deleted）

## 安全注意

- `.env` 文件含 DeepSeek API Key 和数据库密码，**严禁提交到 Git**
- 生产环境数据库密码从环境变量 `${DB_PASSWORD}` 读取
- Nginx 配置了 1600m 的客户端最大上传大小
