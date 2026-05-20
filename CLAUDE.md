# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

学院作品展示平台 — 前后端分离的全栈应用，用于学生提交作品、教师评分（四个维度：创新性25分+技术难度25分+完成度30分+实用性20分，满分100）、管理员审核发布及前台公开展示。学生不可自助注册，账号由管理员预置。

## 启动与构建

### 后端 (Spring Boot 3.2.5 + Java 17 + Maven)
- 打包：在 `backend/` 下运行 `mvn package -DskipTests -Dmaven.clean.skip=true`（跳过 clean 避免 Windows 文件锁问题），输出 `target/jingxuan-backend-1.0.0.jar`
- 启动：`java -jar backend/target/jingxuan-backend-1.0.0.jar --spring.profiles.active=dev`
- 端口：8080
- 依赖：MySQL (端口 3306, 数据库 jingxuan)、Redis (端口 6379)
- 重启后端：双击 `start.bat` 或依次运行 `stop.bat` → `mvn package -DskipTests -Dmaven.clean.skip=true -q` → `java -jar backend/target/jingxuan-backend-1.0.0.jar --spring.profiles.active=dev`

### 前端 (Vue 3 + TypeScript + Vite 8)
- 安装依赖：在 `frontend/` 下运行 `npm install`
- 开发启动：`npm run dev`（端口 5173）
- 构建：`npm run build`（含 vue-tsc 类型检查）
- 预览：`npm run preview`

### 一键启停
- `start.bat` — 先 kill 旧进程，Maven 打包，启动后端，等 15s 后再启动前端
- `stop.bat` — kill 8080/5173 端口及 jingxuan/vite 相关进程

## 数据库

- 基础表：`backend/sql/base/init_schema.sql`（用户、角色、菜单、字典、日志、公告、通知 8 张系统表）
- 业务表：`backend/sql/business/work_schema.sql`（作品、成员、附件、审核、发布、评分、评论、批次、敏感规则、奖项、奖品发放、端口管理 12 张业务表）
- 测试数据：`backend/sql/business/test_data.sql`
- 表设计：所有业务实体继承 `BaseEntity`（id=ASSIGN_ID, createTime, updateTime, 逻辑删除），使用 utf8mb4

## 后端架构 (`com.jingxuan`)

### 分层与关键模式

```
Application.java              — 启动入口
config/                       — 配置类（Security, MyBatis-Plus, Jackson, Knife4j, DeepSeek, WebMvc）
security/                     — JWT 认证（TokenProvider, AuthenticationFilter, UserDetailsService, SecurityUtils, RestAccessDeniedHandler, RestAuthenticationEntryPoint）
common/                       — BaseEntity, Result<T>(code=200成功), PageResult<T>, PageUtil
exception/                    — BusinessException(运行时), UnauthorizedException, GlobalExceptionHandler
enums/                        — AuditStatus, PublishStatus, RoleEnum, MenuTypeEnum, UserStatusEnum
constant/                     — CommonConstants, SecurityConstants(TOKEN_HEADER, TOKEN_PREFIX=Bearer )
entity/                       — MyBatis-Plus 实体（与数据库表一一对应）
mapper/                       — MyBatis-Plus Mapper 接口
service/                      — SysUser/SysRole/SysMenu 基础服务（interface + impl）
auth/                         — 认证模块（controller, model, service）
modules/
  adapter/                    — ★ 核心模式：各端 API 适配控制器直接注入 service 和 mapper，不经过 Controller 层
  work/                       — 作品 CRUD（controller/service/impl/dto）
  audit/                      — 审核管理
  score/                      — 评分管理
  rank/                       — 排行榜
  publish/                    — 发布/下线/精选
  comment/                    — 评论
  notice/                     — 公告管理
  notification/               — 用户通知
  prize/                      — 奖项/奖品配置
  dict/                       — 数据字典
  log/                        — 操作日志
  port/                       — 端口管理
  scorebatch/                 — 评分批次
  sensitive/                  — DeepSeek 内容审核
util/                         — FileUtil, IpUtil
```

### ★ 核心架构模式：Adapter 控制器

这是本项目最关键的架构决策：四个 `*ApiController`（`AdminApiController`, `TeacherApiController`, `StudentApiController`, `PublicApiController`）直接注入 `service` 和 `mapper`，集中暴露各自端的所有 API。业务模块内部的 `controller/` 目录是备选方案，非必需。**新增 API 时优先在 adapter 控制器中加方法，而不是新建 Controller 类。**

### API 设计
- `/public/*` — 无需认证，前台展示（作品列表/详情/排行榜/评论）
- `/auth/*` 或 `/api/auth/*` — 登录/登出/用户信息（前端 `/api` 代理会去掉前缀）
- `/admin/*` — 管理端（仪表盘/审核/公告/字典/日志/评分批次/奖项/榜单）
- `/teacher/*` — 教师端（评分/排行榜/通知）
- `/student/*` — 学生端（作品 CRUD 及提交）
- `/file/upload` — 文件上传
- `/uploads/**` — 静态文件

### 安全配置
- JWT 放在 `Authorization: Bearer <token>` 头中
- 忽略路径配置在 `application.yml` 的 `ignored.paths`（逗号分隔，AntPathMatcher 匹配），无需认证的路径在此配置即可
- `JwtAuthenticationFilter` 继承 `OncePerRequestFilter`，先在 `shouldNotFilter()` 中跳过忽略路径，再解析 JWT 设置 SecurityContext
- token 验证失败不会阻断请求（`filterChain.doFilter` 继续执行），由 Spring Security 的 `anyRequest().authenticated()` 处理
- 两个自定义异常处理器：`RestAccessDeniedHandler`（403 → `Result.forbidden()`）和 `RestAuthenticationEntryPoint`（401 → `Result.unauthorized()`），确保认证/授权失败返回统一 JSON 而非重定向
- 三个角色：`RoleEnum.STUDENT(value=1)`, `TEACHER(value=2)`, `ADMIN(value=3)`，前端路由守卫根据角色权限控制页面访问
- ★ **新增公开 API 的三步操作**：① `application.yml` 的 `ignored.paths` 加入路径 → ② adapter 控制器加方法 → ③ `shouldNotFilter()` 自动跳过无需额外配置
- ★ **MyBatis-Plus 全局约定**：`id-type: ASSIGN_ID`（雪花算法）、逻辑删除字段 `deleted`（1=已删 0=正常）、Mapper XML 位置 `classpath*:mapper/**/*.xml`

### 关键业务状态枚举
- 作品审核状态：0=草稿 1=已提交 2=已驳回 3=已通过（`AuditStatusEnum`）
- 发布状态：0=未发布 1=已发布 2=已下线（`PublishStatusEnum`）
- DeepSeek fallback 策略：reject(默认) / bypass(开发环境) / warning — 见 `application-dev.yml`

### 后端开发惯例（从源码提炼）
- **统一返回**：所有 API 返回 `Result<T>`（成功 `Result.ok(data)`，失败 `Result.fail(msg)`）
- **分页**：使用 `PageRequest` 工具类创建 `Page<T>`，返回 `PageResult.of(records, total, pageNum, pageSize)`
- **事务**：写操作使用 `@Transactional(rollbackFor = Exception.class)`
- **参数校验**：DTO 中使用 `jakarta.validation` 注解 + `@Valid`
- **依赖注入**：构造函数注入（`@RequiredArgsConstructor`），或 `@Autowired` 单独字段
- **评分 Upsert 模式**：先查 `selectByWorkAndTeacher`，存在则 update，不存在则 insert
- **Mapper 自定义查询**：在 Mapper 接口加 `@Select` 注解方法，如 `selectByWorkAndTeacher(workId, teacherId)`、`selectScoreDistribution()`
- **API 文档**：使用 Knife4j（`@Tag/@Operation/@Schema`），地址 `http://localhost:8080/doc.html`

## 前端架构 (Vue 3 + TypeScript + Element Plus + Pinia + Vue Router)

### 目录结构
```
src/
  main.ts               — 入口（Pinia + Router + Element Plus 中文）
  App.vue
  style.css
  env.d.ts
  router/
    index.ts            — 汇总所有路由（含全局 beforeEach 鉴权）
    modules/
      public.ts         — /works, /works/:id, /ranking（meta.noAuth=true，无需登录）
      student.ts        — /login(noAuth), /student/home, /student/works, /student/works/create,
                          /student/works/edit/:id, /student/works/view/:id, /student/ranking
      admin.ts          — /admin/dashboard, /admin/audit, /admin/notice, /admin/comment,
                          /admin/rules, /admin/port, /admin/prize, /admin/score-batch,
                          /admin/roles, /admin/users, /admin/notify, /admin/log, /admin/dict
      teacher.ts        — /teacher/dashboard, /teacher/score, /teacher/history,
                          /teacher/ranking, /teacher/notify
  layout/
    PublicLayout.vue    — 前台展示布局（作品浏览/排行/详情）
    AdminLayout.vue     — 管理端布局
    TeacherLayout.vue   — 教师端布局
    StudentLayout.vue   — 学生端布局
  views/                — 页面组件（admin/student/teacher/public 四类子目录 + ChangePassword, Profile）
  api/
    request.ts          — Axios 实例（baseURL=/api, token 注入, 401 跳登录, Element Plus 提示）
    types.ts            — 与后端 DTO 对齐的 TypeScript 接口（WorkListVO, WorkDetailVO）
    admin/              — 管理端 API（audit, comment, dashboard, dict, log, menu,
                          notice, notify, port, prize, role, rule, scoreBatch, user — 14 个文件）
    student/            — 学生端 API（auth.ts, work.ts）
    teacher/            — 教师端 API（dashboard, notify, ranking, score, work — 5 个文件）
    public/             — 公开 API（comment, work — 2 个文件）
  stores/
    student/auth.ts     — Pinia 状态管理
```

### ★ 前端核心模式：数据适配层

`api/student/work.ts` 包含了重要的数据适配模式：
- **前后端状态枚举映射**：前端用 `'draft'|'submitted'|'rejected'|'approved'`，后端用 `0|1|2|3`，通过 `STATUS_MAP`/`STATUS_REV_MAP` 双向转换
- **VO 适配函数**：`adaptListVO()` / `adaptDetailVO()` 将后端下划线字段（`runDesc`, `publishStatus`）转为前端驼峰（`runDescription`, `publishStatus`），数值状态转字符串枚举
- **表单序列化**：`toCreateRequest()` / `toUpdateRequest()` 将前端表单转为后端请求体格式
- **分页适配**：`adaptPageResult()` 统一处理后端 `PageResult` 的分页字段

**前端新增 API 时必须实现对应的适配函数**，否则前后端字段命名和状态枚举会不匹配。

### 关键设计决策
- vite 代理：`/api/*` → `localhost:8080`（去掉 `/api` 前缀），`/api/file/*` → 保持 `/api` 前缀（不走 rewrite），`/uploads/*` → `localhost:8080`
- 路由守卫：`beforeEach` 检查 token 是否存在，公开路由设 `meta.noAuth = true`
- Token 存储：sessionStorage 优先，localStorage 兜底
- 前端接口响应拦截：`res.code === 0 || res.code === 200` 视为成功，`res.code === 401` 清 token 跳登录
- 前端类型定义：共享 VO 类型集中在 `api/types.ts`，各模块 API 文件内定义局部类型

## 常见任务

- **新增后端模块**：在 `modules/` 下建目录（service/impl/dto），然后在 adapter 控制器中加 API 方法
- **新增前端页面**：在 `views/<role>/` 下建 vue 文件，`router/modules/<role>.ts` 加路由，`api/<role>/` 加 API 文件（含适配函数）
- **新增数据库表**：写 SQL 到 `backend/sql/`，建 entity + mapper，service 中写业务逻辑
- **配置文件**：`application.yml`（通用配置，含 JWT/DeepSeek/上传路径/ignored.paths），`application-dev.yml`（数据源/Redis/日志级别）
- **.env 配置**：`DEEPSEEK_API_KEY` 环境变量（不配也能跑，开发环境 fallback=bypass 放行），复制 `.env.example` 为 `.env`
- **DeepSeek fallback 三值**：`reject`（默认，拦截违规）、`bypass`（跳过审核，仅 dev 用）、`warning`（记录警告不拦截）
- **评分批次关键字段**：`status`（1=进行中）、`rankPublished`（0=未公示 1=已公示），详见 `ScoreBatch.java`
- **文件上传**：multipart 上限 200MB（`application.yml`），上传路径 `./uploads`（可配置），MIME 类型校验在 `FileUtil.isAllowedMimeType()`
