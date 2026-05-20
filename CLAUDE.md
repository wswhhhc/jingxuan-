# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

学院作品展示平台 — 前后端分离的全栈应用。学生提交作品、教师评分（四维度：创新性25 + 技术难度25 + 完成度30 + 实用性20，满分100）、管理员审核发布、前台公开展示。学生不可自助注册，账号由管理员预置。

## 启动与构建

### 后端 (Spring Boot 3.2.5 + Java 17 + Maven)
- 打包：`mvn package -DskipTests -Dmaven.clean.skip=true -q`（跳 clean 避免 Windows 文件锁，-q 减噪音）
- 启动：`java -jar target/jingxuan-backend-1.0.0.jar --spring.profiles.active=dev`
- 端口：8080。依赖：MySQL (3306, 库 jingxuan)、Redis (6379)

### 前端 (Vue 3 + TypeScript + Vite 8)
- 安装：`npm install`
- 开发：`npm run dev`（端口 5173）
- 构建：`npm run build`（含 vue-tsc 类型检查）

### 一键启停
- `start.bat` — kill旧进程 → Maven打包 → 启动后端 → 15s后启动前端
- `stop.bat` — kill 8080/5173 及 jingxuan/vite 进程

### 开发调试
- 后端 debug 日志：`application-dev.yml` 已设 `com.jingxuan: debug`，加 `@Slf4j` + `log.debug()` 即可输出
- 前端控制台日志：用 `console.log('[DEBUG] ...')` 在浏览器 F12 查看
- API 文档：`http://localhost:8080/doc.html`（Knife4j）

## 数据库

- 基础表：`backend/sql/base/init_schema.sql`（8张系统表）
- 业务表：`backend/sql/business/work_schema.sql`（12张业务表）
- 测试数据：`backend/sql/business/test_data.sql`
- 增量迁移：`backend/sql/business/` 下的 `yyyy-MM-dd-*.sql` 文件
- 所有业务实体继承 `BaseEntity`（id=雪花算法, createTime, updateTime, 逻辑删除 deleted=0正常/1已删），字符集 utf8mb4

## ★ 核心架构模式：Adapter 控制器

四个 `*ApiController`（`AdminApiController`, `TeacherApiController`, `StudentApiController`, `PublicApiController`）直接注入 service 和 mapper，集中暴露各端所有 API。业务模块内部的 `controller/` 目录是备选方案。**新增 API 优先在 adapter 控制器加方法，而非新建 Controller 类。**

## 后端分层 (com.jingxuan)

| 层 | 说明 |
|---|---|
| `config/` | 配置类（Security, MyBatis-Plus, Jackson, Knife4j, DeepSeek, WebMvc） |
| `security/` | JWT 认证（TokenProvider, AuthenticationFilter, SecurityUtils, 异常处理器） |
| `modules/adapter/` | ★ 四端 API 适配控制器（Admin/Teacher/Student/Public） |
| `modules/work/` | 作品 CRUD + 文件上传 |
| `modules/audit/` | 审核管理 |
| `modules/score/` | 评分（Upsert 模式：先查 `selectByWorkAndTeacher`，存在则 update，否则 insert） |
| `modules/scorebatch/` | 评分批次管理 |
| `modules/rank/` | 排行榜 |
| `modules/publish/` | 发布/下线/精选 |
| `modules/comment/` | 评论 |
| `modules/sensitive/` | DeepSeek 内容安全审核 |
| `modules/userimport/` | AI 批量导入用户（DeepSeek 解析自然语言列表） |
| `modules/notice/` | 公告管理 |
| `modules/notification/` | 用户通知 |
| `modules/prize/` | 奖项配置与奖品发放 |
| `modules/port/` | 端口管理（学生作品在线体验端口分配/释放） |
| `modules/dict/` | 数据字典 |
| `modules/log/` | 操作日志 |

### 后端关键约定

- **统一返回**：所有 API 返回 `Result<T>`（成功 `Result.ok(data)`，失败 `Result.fail(msg)`），异常由 `GlobalExceptionHandler` 捕获
- **分页**：`PageRequest` 工具类创建 `Page<T>`，返回 `PageResult.of(records, total, pageNum, pageSize)`
- **事务**：Service impl 类级别 `@Transactional(rollbackFor = Exception.class)`
- **参数校验**：DTO 中 `jakarta.validation` 注解 + `@Valid`
- **依赖注入**：`@RequiredArgsConstructor` 构造注入，个别循环依赖用 `@Autowired` 字段注入
- **状态枚举**：`AuditStatusEnum`(0草稿/1已提交/2已驳回/3已通过)，`PublishStatusEnum`(0未发布/1已发布/2已下线)
- **角色**：`RoleEnum.STUDENT(1)` / `TEACHER(2)` / `ADMIN(3)`
- **安全**：JWT in `Authorization: Bearer <token>`，忽略路径配在 `application.yml` 的 `ignored.paths`（逗号分隔 AntPathMatcher），`JwtAuthenticationFilter` 继承 `OncePerRequestFilter`，token 验证失败不阻断，由 Spring Security `anyRequest().authenticated()` 处理
- **★★ 新增公开 API 三步**：① `application.yml.ignored.paths` 加路径 → ② adapter 控制器加方法 → ③ 无需再配

### Snowflake ID 精度处理

MyBatis-Plus `ASSIGN_ID`（雪花算法）生成 19 位数字，超出 JS `Number.MAX_SAFE_INTEGER`。**后端返回给前端时必须转 String**：
- Jackson 全局 `Long` → String 序列化（`JacksonConfig` 中 `ToStringSerializer`）
- `Map<String, Object>` 中的 Long 手动转：`result.put("id", String.valueOf(attachment.getId()))`
- DTO 中 `attachmentIds` 字段类型用 `List<String>` 而非 `List<Long>`，后端用 `parseAttachmentIds()` 转 `List<Long>` 再查库

### 作品业务关键规则（WorkServiceImpl）

- **同批次唯一**：同一学生在一个活跃评分批次中只能有一个作品
- **成员唯一**：团队成员在同一批次中只参与一个作品
- **附件绑定**：先上传获取附件 ID（`work_id`=NULL），提交时绑定（`update set work_id=...`），校验不可被其他作品占用
- **提交检查**：至少有一个附件且 `work_id` 已绑定到当前作品
- **内容审核**：提交时调用 DeepSeek 审核标题/简介/运行说明，`deepseek.fallback` 控制失败策略（`bypass`=放行, `reject`=拦截, `warning`=仅警告）
- **DeepSeek fallback** 在 `application-dev.yml` 默认 `bypass`（开发环境），`application.yml` 默认为 `reject`（生产）

## 前端架构 (Vue 3 + TypeScript + Element Plus + Pinia)

### 目录结构

```
src/
  router/index.ts         — 汇总所有路由 + 全局 beforeEach 鉴权（token + RBAC）
  router/modules/         — public/student/admin/teacher 四端路由，meta.noAuth=公开
  layout/                 — PublicLayout / AdminLayout / StudentLayout / TeacherLayout
  views/                  — 页面组件（按 admin/student/teacher/public 四类组织）
  api/
    request.ts            — Axios 实例（baseURL=/api, token注入, 401跳登录, Element Plus 提示）
    types.ts              — 共享 VO 类型定义
    student/work.ts       — ★ 数据适配层参考实现（枚举映射/VO适配/表单序列化/分页适配）
    admin/                — 14 个 API 文件
    teacher/              — 5 个 API 文件
    public/               — 2 个 API 文件
  stores/                 — Pinia 状态管理 (student/auth.ts, theme.ts)
```

### ★ 前端数据适配层

所有 API 文件必须实现以下适配模式（以 `api/student/work.ts` 为参考）：
1. **枚举映射**：后端数值枚举 ↔ 前端字符串枚举（如 `0|1|2|3` ↔ `'draft'|'submitted'|'rejected'|'approved'`）
2. **VO 适配**：后端下划线字段 → 前端驼峰（`runDesc` → `runDescription`，`publishStatus` → `publishStatus`）
3. **表单序列化**：前端表单 → 后端请求体格式（`toCreateRequest()` / `toUpdateRequest()`）
4. **分页适配**：统一处理后端 `PageResult` 字段名差异

### 关键设计决策
- Vite 代理：`/api/*` → `localhost:8080`（去 `/api` 前缀），`/api/file/*` → 保留 `/api` 前缀，`/uploads/*` → `localhost:8080`
- Token 存储：sessionStorage 优先，localStorage 兜底
- 响应拦截：`res.code === 0 || res.code === 200` 视为成功，`res.code === 401` 清 token 跳登录
- 路由守卫：检查 token 是否存在；公开路由 `meta.noAuth = true`；RBAC 角色校验（`meta.roles` 数组）

## 常见任务

- **新增后端模块**：`modules/` 下建目录（service/impl/dto/mapper 等）→ adapter 控制器加 API → 需要公开则加 `ignored.paths`
- **新增前端页面**：`views/<role>/` 下建 Vue 文件 → `router/modules/<role>.ts` 加路由 → `api/<role>/` 加 API 文件（含适配函数）
- **新增数据库表**：建 SQL → 建 Entity + Mapper → Service 写业务逻辑
- **修改 API 路径**：前端 Vite 代理 `/api` 前缀会被去掉（除非 `/api/file`），后端实际路径不含 `/api`
- **调试文件上传**：`beforeFileUpload` 中 `el-upload` 返回 `false` 后会触发 `on-remove`，注意区分 auto-remove 和用户手动删除
