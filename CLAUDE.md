# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目定位

**菁选校园作品展示平台** — IT 学院学生作品展示、教师评分、管理员审核的全流程管理平台。

- **前端**：Vue 3 + TypeScript + Element Plus + Vite 8 SPA
- **后端**：Spring Boot 3.2.5 + Java 17 + MyBatis-Plus 3.5.7
- **数据库**：MySQL 8 + Redis 7
- **部署**：Docker / PM2 + Nginx
- **CI/CD**：GitHub Actions（push 自动跑测试 + 构建）

## 快速代码探索

本项目使用 Codegraph 建立代码索引。需要理解某段逻辑、查找符号或追踪调用链路时，优先使用：

- `codegraph_explore` — 回答"X 怎么工作的 / 在哪 / 架构是怎样的"，直接返回相关源码（Read 等价，一次性返回多个文件）
- `codegraph_search` — 按名称查找符号位置
- `codegraph_callers` / `codegraph_callees` / `codegraph_impact` — 调用链追踪
- `codegraph_files` — 按目录/语言查看文件树

**不要**用 grep + Read 绕路——codegraph 已经建好索引了，一次调用就能拿到完整源码。

## 开发命令

```bash
# 前端
cd frontend && npm run dev          # 开发服务器（Vite HMR，端口 5173）
npm run build                       # 类型检查 + 生产构建 → dist/
npm run test                        # Vitest 全部测试
npm run test -- --run src/views/admin/audit/__tests__/index.test.ts  # 单个测试文件
npm run lint                        # ESLint
npm run format                      # Prettier
npx vue-tsc --noEmit                # TypeScript 类型检查

# 后端（需在 backend/ 目录下）
mvn compile                           # 编译
mvn test                              # 全部测试（含集成测试，需 MySQL+Redis）
mvn test -Dtest=WorkServiceImplTest   # 单个测试类
mvn test -Dtest="AdminApiTest,FileUploadTest"  # 多个测试类
mvn test -Dtest="com.jingxuan.modules.work.**" # 按包运行
mvn test -DfailIfNoTests=false        # 无测试不报错
mvn compile -q                        # 编译检查
mvn package -Dmaven.test.skip=true    # 打包 JAR → target/jingxuan-backend-1.0.0.jar

# 部署
cd /opt/jingxuan/backend
mvn package -Dmaven.test.skip=true
cp target/jingxuan-backend-1.0.0.jar .
pm2 restart jingxuan-back

# PM2 管理
pm2 start ecosystem.config.cjs        # 启动后端（见 PM2 配置）
pm2 restart jingxuan-back             # 重启
pm2 stop jingxuan-back                # 停止
pm2 logs jingxuan-back                # 查看日志
pm2 status                            # 查看进程状态

# 冒烟测试
bash scripts/smoke-test.sh http://localhost:8080
```

### Docker

```bash
docker compose up -d              # 启动全部服务（MySQL + Redis + 后端 + Nginx）
docker compose logs -f backend    # 查看后端日志
docker compose down               # 停止
docker compose up -d --build backend  # 重新构建并启动后端
```

### CI/CD

```bash
git push  # 自动触发 GitHub Actions：编译 → 单元测试 → 打包（.github/workflows/ci.yml）
```

## 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `DB_PASSWORD` | MySQL 数据库密码 | `jingxuan123` |
| `DEEPSEEK_API_KEY` | DeepSeek API 密钥（留空 dev 环境 bypass） | — |
| `MAIL_HOST` | 邮件服务器地址 | `smtp.qq.com` |
| `MAIL_PORT` | 邮件服务器端口 | `587` |
| `MAIL_USERNAME` | 邮箱用户名 | — |
| `MAIL_PASSWORD` | 邮箱密码/授权码 | — |
| `MAIL_FROM` | 发件人地址 | — |

定义在项目根目录 `.env` 文件中。

## 关键文件位置

| 文件 | 用途 |
|------|------|
| `backend/src/main/resources/application.yml` | 主配置（数据源、JWT、Redis、上传路径） |
| `application-dev.yml` | 开发环境（DeepSeek bypass） |
| `application-test.yml` | 测试环境（独立数据库 jingxuan_test） |
| `application-prod.yml` | 生产环境（DeepSeek reject） |
| `frontend/vite.config.ts` | Vite 构建 + 开发代理配置 |
| `nginx-jingxuan.conf` | Nginx 反代配置（路由、安全头、Gzip） |
| `ecosystem.config.cjs` | PM2 进程配置（JVM 参数） |
| `docker-compose.yml` | Docker 编排 |
| `.env` | 敏感配置（**不提交 Git**） |
| `sql/` | 数据库迁移脚本 |

## 架构概览

### 请求流转

```
用户 → Nginx (80)
 ├── /            → frontend/dist/（SPA 静态文件）
 ├── /api/*       → localhost:8080/*（Nginx 剥离 /api 前缀）
 ├── /api/file/*  → localhost:8080/api/file/*（文件上传保留前缀）
 └── /uploads/*   → localhost:8080/uploads/*（用户上传文件）
```

`/novel2/` 路由代理到另一个独立项目 `/opt/Novel2Script/`，与本项目无关。

### 开发代理（Vite）

前端开发服务器 (`localhost:5173`) 通过 vite.config.ts 中的 proxy 将 `/api/*` 转发到后端 `localhost:8080`，请求路径中 `/api` 前缀会被剥离（文件上传路径保留）。

### Adapter 控制器模式

所有前端请求 → Nginx（剥离 `/api`）→ Adapter 控制器 → Service：

| 控制器 | 路径 | 角色 |
|--------|------|------|
| `AdminApiController` | `/admin/*` | 管理员（48 端点） |
| `TeacherApiController` | `/teacher/*` | 教师（14 端点） |
| `StudentApiController` | `/student/*` | 学生（14 端点） |
| `PublicApiController` | `/public/*` | 游客（16 端点） |
| `AuthController` | `/auth/*` | 认证（9 端点） |
| `NotificationController` | `/{role}/notify/*` | 三端通知 |

Adapter 控制器（`modules/adapter/`）负责：
- 接收前端请求，调用 Facade / Service 执行业务逻辑
- 返回 `Result<T>` 统一响应

### 安全认证流程

1. 用户登录 → `AuthController` 验证凭据 → `JwtTokenProvider` 生成 JWT
2. 后续请求携带 `Authorization: Bearer <token>`
3. `JwtAuthenticationFilter` 解析 token → 注入 `SecurityContext`
4. `SecurityUtils.requireCurrentUserId()` 获取当前用户 ID
5. `SecurityConfig` 按角色（STUDENT/TEACHER/ADMIN）配置端点权限
6. 登入 token 加入 `InMemoryTokenBlacklistService`（Caffeine Cache）

### 业务模块（modules/）

```
work → audit → publish → score → scorebatch → rank → comment → prize
       → notification → notice → sensitive → userimport → auth → dict → log
```

模块间依赖方向：work → audit → publish → score/scorebatch → rank（只进不出）。

### 核心业务流程

```
学生创建作品 → 上传附件/视频 → 提交审核
                                         → 管理员审核通过 → 教师评分 → 排行榜 → 发布展示
                                         → 管理员驳回 → 学生修改后重新提交
```

## 前端架构

### 目录结构

```
frontend/src/
 ├── api/             # API 层（按角色分 admin/teacher/student/public）
 │   ├── request.ts   # Axios 实例 + 拦截器（token 注入、统一错误处理）
 │   ├── types.ts     # 共享类型（WorkListVO/WorkDetailVO/RankItem/UserInfo）
 │   ├── notify.ts    # 通知 API（三端共用，role 参数化）
 │   └── workAdapter.ts  # 作品响应数据适配转换
 ├── components/      # 共享组件（NotificationList/PaginationBar/AppThemeToggle）
 ├── composables/     # 组合式函数（useApiList/useCrudDialog/useNotificationPolling）
 ├── layout/          # 四端布局（Admin/Teacher/Student/Public）
 ├── router/          # 路由配置（modules/ 下按角色拆分）
 ├── stores/          # Pinia 状态（auth, theme）
 ├── utils/           # 工具函数（auth.ts, format.ts）
 └── views/           # 页面（admin 12页/teacher 4页/student 5页/public 4页）
```

### 关键约定

- **响应拦截**：`request.ts` 中 `res.code === 0` 为成功，非零或网络错误弹出 `ElMessage.error()`
- **分页**：`useApiList<T>(fetchFn)` + `<PaginationBar>`，page/size 由视图管理
- **弹窗 CRUD**：admin 管理页用 `useCrudDialog()` 统一管理 create/edit/delete 状态
- **通知**：三端共用 `<NotificationList>` + `useNotificationPolling`
- **深色模式**：`<html data-theme="dark">` 切换，CSS 变量在 `style.css`

### Vite 构建

- `vue-tsc -b` 类型检查 + `vite build` 生产构建
- Element Plus 按需导入（unplugin-auto-import + unplugin-vue-components）
- gzip 预压缩（vite-plugin-compression）
- 手动 chunk 拆分：vendor-vue（Vue 生态）、vendor-element（Element Plus）、vendor-echarts（仅控制台页面使用）

## 后端关键约定

- **统一返回**：`Result<T>`（`ok(data)` / `fail(msg)`），异常由 `GlobalExceptionHandler` 捕获
- **BaseEntity**：所有实体继承（雪花 id + createTime + updateTime + 逻辑删除 deleted）
- **雪花 ID**：Jackson 全局 Long→String，防前端 JS 精度丢失
- **分页**：`PageUtil.query(pageNum, pageSize, mapper, wrapperConsumer)` → `PageResult<T>`
- **评分 Upsert**：`WorkScoreMapper.upsert()` 原子 INSERT ... ON DUPLICATE KEY UPDATE
- **班级范围**：`ClassScopeUtil.parseToStringSet()` 解析 JSON 数组或旧版逗号分隔
- **DeepSeek fallback**：`bypass`(dev) / `reject`(prod) / `warning`(test)
- **文件上传校验**：扩展名白名单 + Hutool FileTypeUtil 魔数双重检测（拦截 HTML/JS/SVG 伪装）

### 实体枚举

| 枚举 | 值 |
|---|---|
| `AuditStatusEnum` | 0=草稿, 1=已提交, 2=已驳回, 3=已通过 |
| `PublishStatusEnum` | 0=未发布, 1=已发布, 2=已下线 |
| `UserStatusEnum` | 0=禁用, 1=启用 |
| `RoleEnum` | 1=STUDENT, 2=TEACHER, 3=ADMIN |

## 测试指南

### 测试结构

**命名规范：**
- 后端单元测试：`*Test.java`（如 `WorkServiceImplTest.java`），扩展 `BaseServiceTest`，Mockito 模拟 Mapper 层
- 后端集成测试：`*ApiTest.java`（如 `AdminApiTest.java`），扩展 `BaseApiTest`（Spring Boot Test + 随机端口）
- 前端测试：`*.test.ts`（如 `index.test.ts`），Vitest + `@vue/test-utils`
- 前端测试工具：`src/views/__tests__/test-utils.ts` 提供通用测试辅助函数

**后端集成测试**：扩展 `BaseApiTest`，使用内置 `ApiClient` 发送 HTTP 请求并解析 JSON 响应：

- `adminApi`：管理员（admin / admin123）
- `teacherApi`：教师（t001 / test123）
- `studentApi`：学生（2022001 / test123）
- `testStuApi`：测试学生账号（teststu / test123）
- `publicApi`：无 token 的公开端

支持链式断言：`adminApi.get("/admin/work/list").assertOk().getData(...)`

**后端单元测试**：扩展 `BaseServiceTest`，使用 Mockito 模拟 Mapper 层。

**集成测试依赖**：需运行中的 MySQL（数据库 jingxuan_test）+ Redis，配置在 `application-test.yml`。

```bash
# 运行集成测试时需设置数据库密码
export DB_PASSWORD=your_password
mvn test -Dtest="AdminApiTest"
```

**前端测试**：Vitest + @vue/test-utils。

**测试数据**：`sql/business/test_data.sql` 包含学生、教师、作品、评分等演示数据，用于开发调试和答辩演示。

### 测试状态

| 类型 | 通过率 | 运行方式 |
|------|--------|---------|
| 前端单元测试 | 56/56 100% | `npm run test` |
| 后端单元测试 | 97/97 100% | `mvn test -Dtest="com.jingxuan.modules.**"` |
| 集成测试 | 77/77 100% | `mvn test`（需 MySQL+Redis） |
| 接口测试 | 109/109 100% | 同集成测试 |
| 冒烟测试 | 14/14 100% | `bash scripts/smoke-test.sh` |

详细报告请参阅 `docs/` 目录（16 份文档：系统说明书、需求文档、测试计划/用例/报告、运维手册等）。

## 数据库迁移

- 基础表：`sql/base/init_schema.sql`
- 业务表：`sql/business/work_schema.sql`
- 增量迁移：`sql/business/yyyy-MM-dd-描述.sql`
- 所有实体继承 `BaseEntity`（雪花算法 id、createTime、updateTime、逻辑删除 deleted）
- Docker 首次部署自动执行 `init_schema.sql`

## 评分规则

四维度评分，满分 100 分：创新性(25) + 技术难度(25) + 完成度(30) + 实用性(20)。多位教师评分取平均分，按总分→完成度→创新性→提交时间排序。

## 安全注意事项

- `.env` 含 `DB_PASSWORD`、`DEEPSEEK_API_KEY`、邮件配置，**严禁提交到 Git**
- JWT 密钥在 `application.yml` 的 `jwt.secret`，生产环境请替换
- 文件上传：扩展名白名单 + 魔数（Hutool FileTypeUtil）双重检测
- 缓存/限流：`PublicRateLimitFilter` 用 Caffeine Cache 限流（20 次/秒），`RankServiceImpl` 用 Redis 缓存排行榜
- Token 黑名单：`InMemoryTokenBlacklistService` 用 Caffeine Cache 存储登出 token
- Nginx 已配置 CSP/X-Frame-Options/X-Content-Type-Options 等安全头
