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
- `start.bat` — kill 旧进程 → Maven 打包 → 启动后端 → 15s 后启动前端
- `stop.bat` — kill 8080/5173 及 jingxuan/vite 进程

### 开发调试
- 后端 debug：`application-dev.yml` 设了 `com.jingxuan: debug`，加 `@Slf4j` + `log.debug()` 输出
- 前端日志：`console.log('[DEBUG] ...')` 浏览器 F12 查看
- API 文档：`http://localhost:8080/doc.html`（Knife4j）

### 测试
- **后端测试**：`mvn test`（H2 内存数据库 + `@ActiveProfiles("test")`，不依赖 MySQL/Redis）
- **后端运行单测类**：`mvn test -Dtest=StudentApiTest`（Maven Surefire 匹配 `**/*Test.java`）
- **前端测试**：`npm run test`（Vitest + jsdom + Vue Test Utils）
- **前端 watch 模式**：`npm run test:watch`

### 安全注意
- `.env` 文件含 DeepSeek API Key，**严禁提交到 Git**（已在 `.gitignore` 中）
- `application-prod.yml` 含数据库密码（`${DB_PASSWORD}` 从环境变量读），注意权限

## 数据库

- 基础表：`backend/sql/base/init_schema.sql`（8 张系统表：用户、角色、菜单等）
- 业务表：`backend/sql/business/work_schema.sql`（12 张业务表）
- 测试数据：`backend/sql/business/test_data.sql`
- 增量迁移：`backend/sql/business/` 下的 `yyyy-MM-dd-*.sql` 文件
- 所有实体继承 `BaseEntity`（雪花算法 id, createTime, updateTime, 逻辑删除 deleted），字符集 utf8mb4

## 统一返回与异常处理

### Result<T> 状态码
| 方法 | code | 说明 |
|---|---|---|
| `Result.ok(data)` | 200 | 成功 |
| `Result.fail(msg)` | 400 | 业务错误 |
| `Result.unauthorized(msg)` | 401 | 未认证 |
| `Result.forbidden(msg)` | 403 | 无权限 |
| `Result.error(msg)` | 500 | 系统异常 |

前端响应拦截（`api/request.ts`）：`res.code === 0 || res.code === 200` 视为成功，`code === 401` 清 token 跳登录。

### 业务异常
```java
throw new BusinessException("错误信息");       // → 400
throw new BusinessException(400, "错误信息");   // → 自定义 code
```
由 `GlobalExceptionHandler` 统一捕获，返回 `Result.fail()`。其他异常类型：`NotFoundException` → 404，`UnauthorizedException` → 401/403，参数校验失败 → 400。

## ★ 核心架构模式：Adapter 控制器

四个 `*ApiController`（`AdminApiController`, `TeacherApiController`, `StudentApiController`, `PublicApiController`）直接注入 service 和 mapper，集中暴露各端所有 API。业务模块内部的 `controller/` 目录是备选方案。**新增 API 优先在 adapter 控制器加方法，而非新建 Controller 类。**

## 后端分层 (com.jingxuan)

### 根级包（跨模块公共类）
| 包 | 说明 |
|---|---|
| `entity/` | 全局 Mapper 对应实体（User, Menu, Role 等） |
| `mapper/` | MyBatis-Plus Mapper（BaseMapper 扩展） |
| `service/` + `impl/` | 用户/角色/菜单等基础 Service |
| `controller/` | 基础控制器（用户/角色/菜单 CRUD） |
| `dto/` | 跨模块共享 DTO |
| `common/` | `BaseEntity` / `Result<T>` / `PageResult` / `PageUtil` |
| `enums/` | `RoleEnum` / `AuditStatusEnum` / `PublishStatusEnum` / `UserStatusEnum` / `MenuTypeEnum` |
| `exception/` | `BusinessException` / `GlobalExceptionHandler` / `UnauthorizedException` / `NotFoundException` |
| `util/` | `FileUtil` / `IpUtil` |
| `constant/` | 常量定义 |
| `auth/` | 认证模块（controller + service + model） |

### 配置与安全
| 包 | 说明 |
|---|---|
| `config/` | 6 个配置类：SecurityConfig / JacksonConfig / MyBatisPlusConfig / Knife4jConfig / DeepSeekConfig / WebMvcConfig |
| `security/` | JWT 认证链：JwtTokenProvider / JwtAuthenticationFilter / SecurityUtils / CustomUserDetailsService + 异常处理器 |

### 配置文件名约定
- `application.yml` — 公共配置（数据源模板、JWT、DeepSeek fallback=reject、上传路径、ignored.paths）
- `application-dev.yml` — 开发环境（MySQL 连接、Redis、DeepSeek fallback=bypass、debug 日志）
- `application-test.yml` — 测试环境（H2 内存数据库、Redis 降级、DeepSeek bypass）
- `application-prod.yml` — 生产环境（MySQL + Redis 连接、info 日志；**不提交 Git**，密码从环境变量 `${DB_PASSWORD}` 读）

### 业务模块 (modules/)
| 模块 | 说明 |
|---|---|
| `adapter/` | ★ 四端 API 适配控制器 |
| `work/` | 作品 CRUD + 文件上传（含同批次唯一/成员唯一/附件绑定等规则） |
| `audit/` | 审核管理 |
| `score/` | 评分（Upsert 模式：先查 `selectByWorkAndTeacher`，存在则 update，否则 insert） |
| `scorebatch/` | 评分批次管理 |
| `rank/` | 排行榜 |
| `publish/` | 发布/下线/精选 |
| `comment/` | 评论 |
| `sensitive/` | DeepSeek 内容安全审核 |
| `userimport/` | AI 批量导入用户（DeepSeek 解析自然语言列表） |
| `notice/` | 公告管理 |
| `notification/` | 用户通知 |
| `prize/` | 奖项配置与奖品发放 |
| `port/` | 端口管理（学生作品在线体验端口分配/释放） |
| `dict/` | 数据字典 |
| `log/` | 操作日志 |

### 安全工具类 SecurityUtils

所有控制器通过 `SecurityUtils` 获取当前登录用户信息（由 `JwtAuthenticationFilter` 预先设入 SecurityContext）：

| 方法 | 返回值 |
|---|---|
| `getCurrentUserId()` | `Long` 或 null |
| `requireCurrentUserId()` | `Long`，无登录抛 `UnauthorizedException(401)` |
| `getCurrentUsername()` | 用户名 |
| `getCurrentRealName()` | 真实姓名 |
| `getCurrentRoleCode()` | 角色编码 |
| `getCurrentRoleId()` | 角色 ID |
| `isAuthenticated()` | boolean |
| `hasRole("admin")` | boolean |

公开 API（`PublicApiController`）中有一个 `getCurrentUserId()` 私有方法，未登录时不抛异常而是返回 null，用于判断"当前用户是否已点赞"等场景。

### BaseEntity 自动填充

所有业务实体继承 `BaseEntity`（id=雪花算法, createTime, updateTime, 逻辑删除 deleted）。`MyBatisPlusConfig` 中 `MetaObjectHandler` 自动填充：
- `createTime`：INSERT 时自动设 `LocalDateTime.now()`
- `updateTime`：INSERT 和 UPDATE 时自动设 `LocalDateTime.now()`

### Jackson 序列化配置（JacksonConfig）
- **Long → String**：全局 `Long` 类型自动转字符串，避免前端 JS 精度丢失（19 位雪花 ID 超出 `Number.MAX_SAFE_INTEGER`）
- **LocalDateTime**：统一格式 `yyyy-MM-dd HH:mm:ss`，同时兼容 `yyyy-MM-dd'T'HH:mm:ss` 反序列化
- 关闭 `WRITE_DATES_AS_TIMESTAMPS`

### 后端关键约定

- **统一返回**：所有 API 返回 `Result<T>`（成功 `Result.ok(data)`，失败 `Result.fail(msg)`），异常由 `GlobalExceptionHandler` 捕获
- **分页**：MyBatis-Plus `Page<T>` + `PaginationInnerInterceptor`，用 `PageUtil.toPageResult()` 转为 `PageResult<T>`（records/total/pageNum/pageSize 四字段）
- **抛业务异常**：`throw new BusinessException("提示信息")` → 前端自动 `ElMessage.error()` 展示
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

## 测试基础设施

### 后端测试（H2 + SpringBootTest）
- 基类 `BaseApiTest` 启动完整 Spring 上下文 + H2 内存数据库（MySQL 兼容模式），通过 `TestRestTemplate` 发 HTTP 请求
- `BaseServiceTest` 纯 Mockito 单元测试，不加载 Spring 上下文
- `ApiClient` 工具类封装了 `get/post/put/delete` + `ApiResponse` 的 `assertOk()`/`getData()` 链式断言
- 预置 5 个带 token 的 `ApiClient`：`adminApi` / `teacherApi` / `studentApi` / `testStuApi` / `publicApi`
- 测试资源：`src/test/resources/sql/test-schema.sql`（建表）、`test-data.sql`（种子数据）、`cleanup.sql`（清理）
- profile：`application-test.yml`（H2 + Redis 降级 + DeepSeek bypass）

### 前端测试（Vitest + jsdom + @vue/test-utils）
- 测试文件命名：`*.test.ts`，放在 `__tests__/` 目录下与被测文件相邻
- Mock 模式：API 调用在 `vi.mock()` 中 mock，组件通过 `mount()` / `shallowMount()` 测试
- Element Plus 组件需在 `mount()` 中全局注册（`global.plugins`）避免渲染报错

## 前端架构 (Vue 3 + TypeScript + Element Plus + Pinia)

### 目录结构

```
src/
  router/index.ts                 — 汇总四端路由 + 全局 beforeEach 鉴权（token + RBAC）
  router/modules/                 — admin.ts / student.ts / teacher.ts / public.ts
  layout/                         — 4 个布局组件（Admin/Student/Teacher/PublicLayout.vue）
  views/
    admin/                        — 13 个子目录（audit, comment, dashboard, dict, log, notice, notify, port, prize, role, rule, scoreBatch, user）
    student/                      — 6 个页面（Home, Login, MyWorks, WorkSubmit, MyRanking, Notify）
    teacher/                      — 5 个子目录（dashboard, history, notify, ranking, score）
    public/                       — 4 个页面（WorkList, WorkDetail, Ranking, CommentThread）
    login/                        — 通用登录页
  api/
    request.ts                    — Axios 实例（baseURL=/api, token 注入, 401 跳登录, ElMessage 提示）
    types.ts                      — 共享 VO 类型定义
    admin/                        — 14 个 API 文件
    teacher/                      — 5 个 API 文件
    student/                      — 3 个 API 文件
    public/                       — 3 个 API 文件
  stores/
    student/auth.ts               — 学生认证状态
    theme.ts                      — 全局主题状态
  components/
    student/                      — 学生端复用组件
    work/                         — 作品相关复用组件
  mock/                           — 本地 mock 数据
```

路径别名：`@` → `src/`（vite.config.ts 中 resolve.alias 配置）

### Vite 构建优化
- `unplugin-auto-import` + `unplugin-vue-components`：Element Plus 按需导入（样式 CSS），无需手动 `import`
- `vite-plugin-compression`：gzip 预压缩（阈值 10KB），部署环境需开启静态 `.gz` 优先服务
- `rollupOptions.output.manualChunks`：Vue 生态 → `vendor-vue`，Element Plus → `vendor-element`，ECharts → `vendor-echarts`

### ★ 前端数据适配层

所有 API 文件必须实现以下适配模式（以 `api/student/work.ts` 为参考）：
1. **枚举映射**：后端数值枚举 ↔ 前端字符串枚举（如 `0|1|2|3` ↔ `'draft'|'submitted'|'rejected'|'approved'`）
2. **VO 适配**：后端下划线字段 → 前端驼峰（`runDesc` → `runDescription`，`publishStatus` → `publishStatus`）。每个 API 文件包含 `adaptListVO()` / `adaptDetailVO()` 函数
3. **表单序列化**：前端表单 → 后端请求体格式（`toCreateRequest()` / `toUpdateRequest()`），注意字段名转换
4. **分页适配**：统一通过 `adaptPageResult()` 处理后端 `PageResult`，适配 `pageNum`/`pageSize` 字段差异

每个 API 文件的导出函数结构：`export async function getXxx(params)` → 调 `request()` → `res.data = adaptPageResult(res.data)` → `return res`

### 关键设计决策
- Vite 代理：`/api/*` → `localhost:8080`（去 `/api` 前缀），`/api/file/*` → 保留 `/api` 前缀，`/uploads/*` → `localhost:8080`
- Token 存储：sessionStorage 优先，localStorage 兜底
- 响应拦截：`res.code === 0 || res.code === 200` 视为成功，`res.code === 401` 清 token 跳登录
- 路由守卫：检查 token 是否存在；公开路由 `meta.noAuth = true`；RBAC 角色校验（`meta.roles` 数组）
- 前端 `.env`：`DEEPSEEK_API_KEY` 配置在后端根目录 `.env`（已 `.gitignore`），参考 `.env.example`
- 文件上传 API：`POST /file/upload`，multipart/form-data，参数 `file` + `workId`（可选），先上传获取附件 ID，再绑定到作品

## 常见任务

- **新增后端模块**：`modules/` 下建目录（service/impl/dto/mapper 等）→ adapter 控制器加 API → 需要公开则加 `ignored.paths`
- **新增前端页面**：`views/<role>/` 下建 Vue 文件 → `router/modules/<role>.ts` 加路由 → `api/<role>/` 加 API 文件（含适配函数）
- **新增数据库表**：建 SQL → 建 Entity + Mapper → Service 写业务逻辑 → adapter 控制器加 API
- **修改 API 路径**：前端 Vite 代理 `/api` 前缀会被去掉（除非 `/api/file`），后端实际路径不含 `/api`
- **调试文件上传**：`beforeFileUpload` 中 `el-upload` 返回 `false` 后会触发 `on-remove`，注意区分 auto-remove 和用户手动删除

## 文档

设计文档、接口规范和测试记录在 `docs/` 目录下，包含：
- `系统说明书.md` / `需求文档.md` — 系统整体设计
- `nginx-preview.conf` — 精选作品反向代理配置
- `测试计划.md` — 软件测试策略与用例计划
- `缺陷报告.md` — 测试缺陷跟踪
- `scripts/` 目录含 Python 文档生成脚本（测试用例生成、测试计划文档自动生成等辅助工具）
