# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目定位

本仓库是 **菁选校园作品展示平台的生产部署包**，存放服务器部署所需的编译后产物和配置文件，**不包含完整源码**。

## 部署命令

```bash
# 首次部署
mysql -u root -p jingxuan < sql/base/init_schema.sql
mysql -u root -p jingxuan < sql/business/work_schema.sql
# 按时间顺序执行 sql/business/ 下所有 yyyy-MM-dd-*.sql 增量迁移文件
cp .env.example .env   # 编辑配置
bash deploy-server.sh
```

一键部署脚本 `deploy-server.sh` 自动执行：
- 从 `.env` 加载环境变量（`DB_PASSWORD`、`DEEPSEEK_API_KEY`、`MAIL_*`）
- 创建 `uploads/` 目录
- PM2 启动后端 JAR（端口 8080，profile=prod）
- 复制 Nginx 配置并重载

### 运维命令

```bash
# PM2
pm2 status                  # 查看进程状态
pm2 logs jingxuan-back      # 查看实时日志
pm2 restart jingxuan-back   # 重启后端

# Nginx
nginx -t                    # 配置语法检查
nginx -s reload             # 重载配置

# 数据库迁移（执行增量 SQL）
mysql -u root -p jingxuan < sql/business/2026-06-xx-新的迁移.sql
```

## 架构概览

```
Nginx (80)
 ├── / → frontend/dist/ (SPA 静态文件)
 ├── /api/* → localhost:8080/* (去 /api 前缀)
 ├── /api/file/* → localhost:8080/api/file/* (保留前缀)
 └── /uploads/* → localhost:8080/uploads/* (用户上传文件)

后端 localhost:8080 (Spring Boot 3.2.5 + Java 17)
 ├── JWT 认证 (jjwt 0.12.5)
 ├── Security 忽略路径: /auth/**, /public/**, /comment/list, /swagger-ui/**, ...
 ├── MySQL 8 + Redis 7
 └── DeepSeek API (内容安全审核)

前端 SPA (Vue 3 + Element Plus)
 ├── 四端路由: admin / teacher / student / public
 ├── API 前缀 /api/ (Vite 代理或 Nginx 转发)
 └── 角色: admin(3) / teacher(2) / student(1)
```

## 后端模块（JAR 内）

### 四端 Adapter 控制器（核心入口）

| 控制器 | 职责 |
|---|---|
| `AdminApiController` | 管理端所有 API（审核、用户、字典、日志、奖项等） |
| `TeacherApiController` | 教师端 API（评分、排行榜、历史记录等） |
| `StudentApiController` | 学生端 API（作品 CRUD、提交、排行等） |
| `PublicApiController` | 公开 API（作品浏览、排行榜、评论等） |
| `AdminNotifyController` | 管理端消息通知发送 |

### 业务模块

| 模块 | 包路径 | 职责 |
|---|---|---|
| `work` | `modules/work/` | 作品 CRUD + 文件上传 + 成员管理 + 内容审核 |
| `audit` | `modules/audit/` | 作品审核（提交/驳回/通过） |
| `score` | `modules/score/` | 教师评分（Upsert 模式） |
| `scorebatch` | `modules/scorebatch/` | 评分批次管理（CRUD + 状态控制） |
| `rank` | `modules/rank/` | 排行榜（作品维度聚合评分） |
| `publish` | `modules/publish/` | 作品发布/下线/精选 |
| `comment` | `modules/comment/` | 评论（登录用户 + 游客留言） |
| `auth` | `modules/auth/` | 注册（含邮箱验证码）、验证码发送 |
| `notice` | `modules/notice/` | 公告管理 |
| `notification` | `modules/notification/` | 用户消息通知 |
| `prize` | `modules/prize/` | 奖项配置 + 奖品发放 + 发放记录 |
| `sensitive` | `modules/sensitive/` | DeepSeek 内容安全审核 + 敏感词 DFA |
| `userimport` | `modules/userimport/` | AI 批量导入用户 |
| `dict` | `modules/dict/` | 数据字典 |
| `log` | `modules/log/` | 操作日志 |

### 配置与安全

| 类 | 职责 |
|---|---|
| `SecurityConfig` | Spring Security 配置（JWT 过滤器链、角色权限） |
| `RegistrationSecurityConfig` | 注册接口的独立安全配置 |
| `GuestCommentSecurityConfig` | 游客评论的安全配置 |
| `JwtTokenProvider` | JWT token 生成 & 验证 |
| `JwtAuthenticationFilter` | 请求 token 解析 & SecurityContext 注入 |
| `SecurityUtils` | 获取当前登录用户信息（`SecurityUtils.getCurrentUserId()`） |
| `PublicRateLimitFilter` | 公开 API 限流 |
| `JacksonConfig` | Long→String 序列化（防雪花 ID 精度丢失）、LocalDateTime 格式 |
| `MyBatisPlusConfig` | 分页插件、乐观锁、自动填充（createTime/updateTime） |

### 后端关键约定

- **统一返回**：所有 API 返回 `Result<T>`（`ok(data)`/`fail(msg)`），异常由 `GlobalExceptionHandler` 统一捕获
- **BaseEntity**：所有实体继承（雪花 id + createTime + updateTime + 逻辑删除 deleted）
- **雪花 ID**：Jackson 全局 Long→String 转换，防前端 JS 精度丢失
- **分页**：MyBatis-Plus `Page<T>` + `PageUtil.toPageResult()` → `PageResult<T>`
- **评分 Upsert**：先查 `selectByWorkAndTeacher`，存在则 update，否则 insert
- **作品规则**：同批次唯一、成员唯一、附件先上传后绑定、DeepSeek 审核
- **DeepSeek fallback**: `bypass`(开发/测试) / `reject`(生产) / `warning`

## 数据库

### 表结构

- `sql/base/init_schema.sql` — 8 张系统表（`sys_user`, `sys_role`, `sys_menu`, `sys_dict`, `sys_notice` 等）
- `sql/business/work_schema.sql` — 12 张业务表（`work`, `work_attachment`, `work_member`, `work_score`, `work_comment`, `work_audit`, `work_publish`, `work_like`, `work_tag`, `score_batch`, `tag`, `sys_notification` 等）

### 迁移规范

- 增量迁移文件命名：`sql/business/yyyy-MM-dd-简短描述.sql`
- 按文件名排序顺序执行（时间顺序）
- 测试数据：`sql/business/test_data.sql`

### 实体枚举

| 枚举 | 值 |
|---|---|
| `AuditStatusEnum` | 0=草稿, 1=已提交, 2=已驳回, 3=已通过 |
| `PublishStatusEnum` | 0=未发布, 1=已发布, 2=已下线 |
| `UserStatusEnum` | 0=禁用, 1=启用 |
| `RoleEnum` | 1=STUDENT, 2=TEACHER, 3=ADMIN |
| `MenuTypeEnum` | 0=目录, 1=菜单, 2=按钮 |

## 安全注意事项

- `.env` 文件含 `DB_PASSWORD`、`DEEPSEEK_API_KEY`、邮件配置，**严禁提交到 Git**
- 生产环境 DB 密码从 `${DB_PASSWORD}` 环境变量读取（`application-prod.yml` 未纳入版本控制）
- JWT 密钥在 `application.yml` 的 `jwt.secret` 中，生产环境请替换
- `application.yml` 的 `ignored.paths` 配置无需认证的公开路径（含 `/auth/register`、`/public/**`、`/comment/list` 等）

## 前端架构

- **SPA**：Vue 3 + Element Plus + Vite 8，由 Nginx 托管静态文件
- **路由**：`/assets/*` 哈希文件长期缓存（`max-age=31536000`），HTML 不缓存
- **页面预加载**：首页通过 `<script>` 内联请求 `/api/public/classes` 预填班级数据
- **上传限制**：Nginx `client_max_body_size 1600m`，后端 `max-file-size 1536MB`

## 部署环境

- JDK 17+ / MySQL 8+ / Redis 7+ / Nginx / PM2
- JVM 低内存优化：`-Xmx256m -Xms128m -XX:+UseSerialGC -XX:MaxMetaspaceSize=96m`
- Spring Boot profile: `prod`（生产环境），`dev`（本地开发，从 `application-dev.yml` 读取配置）
