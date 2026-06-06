# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目定位

**菁选校园作品展示平台** — 学生作品展示、教师评分、管理员审核的全流程管理平台。

- **前端**：Vue 3 + TypeScript + Element Plus + Vite 8 SPA
- **后端**：Spring Boot 3.2.5 + Java 17 + MyBatis-Plus 3.5.7
- **数据库**：MySQL 8 + Redis 7
- **部署**：Nginx + PM2（生产部署包在此仓库中）

## 开发命令

```bash
# 前端
cd frontend && npm run dev          # 开发服务器（Vite HMR）
npm run build                       # 类型检查 + 生产构建 → frontend/dist/
npm run test                        # Vitest 单元测试
npm run test:watch                  # 监听模式

# 后端（需在 backend/ 目录下）
mvn compile                         # 编译
mvn package -Dmaven.test.skip=true  # 打包 JAR（跳过测试）
mvn test                            # 运行单元测试

# 部署
cd /opt/jingxuan/backend
mvn package -Dmaven.test.skip=true
cp target/jingxuan-backend-1.0.0.jar .
pm2 restart jingxuan-back
```

### 运维命令

```bash
pm2 status                    # 查看进程状态
pm2 logs jingxuan-back        # 查看实时日志
nginx -t                      # Nginx 配置语法检查
nginx -s reload               # 重载 Nginx 配置
```

## 架构概览

```
Nginx (80)
 ├── /           → frontend/dist/ (SPA 静态文件)
 ├── /api/*      → localhost:8080/* (去 /api 前缀)
 ├── /api/file/* → localhost:8080/api/file/* (文件上传保留前缀)
 └── /uploads/*  → localhost:8080/uploads/* (用户上传文件)

后端 localhost:8080 (Spring Boot 3.2.5 + Java 17)
 ├── JWT 认证 (jjwt 0.12.5)
 ├── Redis 7 (排行榜缓存、限流计数器)
 ├── MySQL 8 (MyBatis-Plus ORM, 雪花 ID, 逻辑删除)
 └── DeepSeek API (内容安全审核)

前端 SPA (Vue 3 + Element Plus + Vite 8)
 ├── 四端路由: admin / teacher / student / public
 ├── API 前缀 /api/ (Vite proxy → Nginx → Backend)
 └── 角色: admin(3) / teacher(2) / student(1)
```

## 前端架构

### 目录结构

```
frontend/src/
 ├── api/             # API 调用层（按角色分 admin/teacher/student/public）
 │   ├── request.ts   # Axios 实例 + 拦截器（token 注入、统一错误处理）
 │   ├── types.ts     # 共享类型定义
 │   └── workAdapter.ts  # 作品数据适配转换
 ├── components/      # 共享组件
 │   ├── NotificationList.vue   # 通知列表（三端共用）
 │   ├── PaginationBar.vue      # 统一分页条
 │   └── AppThemeToggle.vue     # 深色/浅色主题切换
 ├── composables/     # 共享组合式函数
 │   ├── useApiList.ts           # API 分页列表（消除 try-finally 重复）
 │   ├── useCrudDialog.ts        # CRUD 弹窗状态管理
 │   └── useNotificationPolling.ts # 通知未读轮询
 ├── layout/          # 四端布局（Admin/Teacher/Student/Public）
 ├── router/          # 路由配置（modules/ 下按角色拆分）
 ├── stores/          # Pinia 状态（auth, theme）
 ├── utils/           # 工具函数
 │   ├── auth.ts      # token 存取
 │   └── format.ts    # rewardTagType, formatDateTime
 └── views/           # 页面视图（按角色分目录）
     ├── admin/       # 管理端（audit, user, prize, scoreBatch, dict 等 12 页）
     ├── teacher/     # 教师端（score, ranking, history, dashboard）
     ├── student/     # 学生端（WorkSubmit, MyWorks, MyRanking, Home）
     └── public/      # 公开端（WorkList, WorkDetail, Ranking, CommentThread）
```

### 关键约定

- **响应拦截**：`request.ts` 拦截器统一处理 `res.code === 0` 为成功，非零或网络错误弹出 `ElMessage.error()`
- **分页**：所有列表页用 `useApiList<T>(fetchFn)` + `<PaginationBar>`，page/size 由视图管理
- **弹窗 CRUD**：admin 管理页用 `useCrudDialog()` 统一管理 create/edit 状态
- **通知**：三端通知列表共用 `<NotificationList>` + `useNotificationPolling`
- **深色模式**：通过 `<html data-theme="dark">` 切换，全局 CSS 变量在 `style.css`

## 后端架构

### 控制器层

四端通过 Adapter 控制器桥接前端 URL 路径与后端业务模块：

| 控制器 | 路径前缀 | 职责 |
|---|---|---|
| `AdminApiController` | `/admin/*` | 审核、用户、字典、日志、奖项、评分批次等 |
| `TeacherApiController` | `/teacher/*` | 评分、排行榜、历史记录、仪表盘 |
| `StudentApiController` | `/student/*` | 作品 CRUD、提交、我的排行 |
| `PublicApiController` | `/public/*` | 作品浏览、排行榜、评论 |
| `NotificationController` | `/{role}/notify/*` | 统一处理三端通知（list/read/read-all/unread-count/delete-read） |

### 业务模块

```
modules/
 ├── work/         作品 CRUD + 文件上传 + 成员管理 + 内容审核
 ├── audit/        审核（提交/驳回/通过）+ 审核历史
 ├── score/        评分（Upsert 模式）+ 平均分计算
 ├── scorebatch/   评分批次管理（CRUD + 状态 + 通知）
 ├── rank/         排行榜（SQL 聚合评分 + Redis 缓存）
 ├── publish/      发布/下线/精选
 ├── comment/      评论（登录用户 + 游客留言）
 ├── prize/        奖项配置 + 奖品发放（含小组成员通知）
 ├── notification/ 消息通知（通用发送 + 多角色统一查询）
 ├── notice/       公告管理
 ├── sensitive/    DeepSeek 内容安全审核 + 敏感词 DFA
 ├── userimport/   AI 批量导入用户
 ├── auth/         注册 + 邮箱验证码
 ├── dict/         数据字典
 └── log/          操作日志
```

### 配置与安全

| 类 | 职责 |
|---|---|
| `SecurityConfig` | Spring Security（JWT 过滤器链、角色权限） |
| `JwtTokenProvider` | JWT 生成 & 验证（启动时校验 secret 是否为空） |
| `JwtAuthenticationFilter` | 请求 token 解析 & SecurityContext 注入 |
| `SecurityUtils` | 获取当前用户 ID（`requireCurrentUserId()`） |
| `PublicRateLimitFilter` | 公开 API 限流（Caffeine Cache） |
| `InMemoryTokenBlacklistService` | 退出登录 token 黑名单（Caffeine Cache） |
| `JacksonConfig` | Long→String（防雪花 ID 精度丢失）、LocalDateTime 格式 |
| `DeepSeekApiClient` | 共享 HttpClient + DeepSeek API 调用 |

### 后端关键约定

- **统一返回**：`Result<T>`（`ok(data)` / `fail(msg)`），异常由 `GlobalExceptionHandler` 捕获
- **BaseEntity**：所有实体继承（雪花 id + createTime + updateTime + 逻辑删除 deleted）
- **雪花 ID**：Jackson 全局 Long→String，防前端 JS 精度丢失
- **分页**：`PageUtil.query(pageNum, pageSize, mapper, wrapperConsumer)` → `PageResult<T>`
- **评分 Upsert**：`WorkScoreMapper.upsert()` 原子 INSERT ... ON DUPLICATE KEY UPDATE
- **班级范围**：`ClassScopeUtil.parseToStringSet()` 解析 JSON 数组或旧版逗号分隔
- **DeepSeek fallback**：`bypass`(开发) / `reject`(生产) / `warning`
- **@Transactional**：审核、评分、发放奖品、发送通知均需事务

### 实体枚举

| 枚举 | 值 |
|---|---|
| `AuditStatusEnum` | 0=草稿, 1=已提交, 2=已驳回, 3=已通过 |
| `PublishStatusEnum` | 0=未发布, 1=已发布, 2=已下线 |
| `UserStatusEnum` | 0=禁用, 1=启用 |
| `RoleEnum` | 1=STUDENT, 2=TEACHER, 3=ADMIN |

## 安全注意事项

- `.env` 含 `DB_PASSWORD`、`DEEPSEEK_API_KEY`、邮件配置，**严禁提交到 Git**
- JWT 密钥在 `application.yml` 的 `jwt.secret`，生产环境请替换
- 文件上传校验：扩展名白名单 + 魔数（Hutool FileTypeUtil）双重检测
- Nginx 已配置安全头（X-Content-Type-Options, CSP, X-Frame-Options 等）
