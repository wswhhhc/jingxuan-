# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目定位

**菁选校园作品展示平台** — IT 学院学生作品展示、教师评分、管理员审核的全流程管理平台。

- **前端**：Vue 3 + TypeScript + Element Plus + Vite 8 SPA
- **后端**：Spring Boot 3.2.5 + Java 17 + MyBatis-Plus 3.5.7
- **数据库**：MySQL 8 + Redis 7
- **部署**：Docker / PM2 + Nginx
- **CI/CD**：GitHub Actions（push 自动跑测试 + 构建）

## 开发命令

```bash
# 前端
cd frontend && npm run dev          # 开发服务器（Vite HMR）
npm run build                       # 类型检查 + 生产构建
npm run test                        # Vitest 全部测试
npm run test -- --run src/views/admin/audit/__tests__/index.test.ts  # 单个测试文件

# 后端（需在 backend/ 目录下）
mvn compile                           # 编译
mvn test                              # 全部测试
mvn test -Dtest=WorkServiceImplTest   # 单个测试类
mvn test -Dtest="AdminApiTest,FileUploadTest"  # 多个测试类
mvn test -Dtest="com.jingxuan.modules.work.**" # 按包运行
mvn test -DfailIfNoTests=false        # 无测试不报错
mvn package -Dmaven.test.skip=true    # 打包 JAR

# 部署
cd /opt/jingxuan/backend
mvn package -Dmaven.test.skip=true
cp target/jingxuan-backend-1.0.0.jar .
pm2 restart jingxuan-back
```

### Docker

```bash
docker compose up -d              # 启动全部服务
docker compose logs -f backend    # 查看后端日志
docker compose down               # 停止
```

### 冒烟测试

```bash
bash scripts/smoke-test.sh http://localhost:8080
```

## 架构概览

### Nginx 路由

```
Nginx (80)
 ├── /           → frontend/dist/ (SPA 静态文件)
 ├── /api/*      → localhost:8080/* (去 /api 前缀)
 ├── /api/file/* → localhost:8080/api/file/* (文件上传保留前缀)
 └── /uploads/*  → localhost:8080/uploads/* (用户上传文件)
```

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

### 业务模块（modules/）

```
work → audit → publish → score → scorebatch → rank → comment → prize
       → notification → notice → sensitive → userimport → auth → dict → log
```

模块间依赖方向：work → audit → publish → score/scorebatch → rank（只进不出）。

### 配置与安全

| 类 | 职责 |
|---|---|
| `SecurityConfig` | Spring Security（JWT 过滤器链、角色权限） |
| `JwtTokenProvider` | JWT 生成 & 验证 |
| `JwtAuthenticationFilter` | 请求 token 解析 & SecurityContext 注入 |
| `SecurityUtils` | 获取当前用户 ID（`requireCurrentUserId()`） |
| `PublicRateLimitFilter` | 公开 API 限流（Caffeine，20 次/秒/IP） |
| `GlobalExceptionHandler` | 全局异常处理 + 404 处理 |
| `GlobalErrorController` | 不存在路由统一返回 404 JSON |
| `JacksonConfig` | Long→String（防雪花 ID 精度丢失） |
| `DeepSeekApiClient` | 共享 HttpClient + DeepSeek API 调用 |

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
- **弹窗 CRUD**：admin 管理页用 `useCrudDialog()` 统一管理 create/edit 状态
- **通知**：三端共用 `<NotificationList>` + `useNotificationPolling`
- **深色模式**：`<html data-theme="dark">` 切换，CSS 变量在 `style.css`

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

## 核心业务流程

```
学生创建作品 → 上传附件/视频 → 提交审核
                                         → 管理员审核通过 → 教师评分 → 排行榜 → 发布展示
                                         → 管理员驳回 → 学生修改后重新提交
```

## 测试状态

| 类型 | 通过率 | 运行方式 |
|------|--------|---------|
| 前端单元测试 | 56/56 100% | `npm run test` |
| 后端单元测试 | 97/97 100% | `mvn test -Dtest="com.jingxuan.modules.**"` |
| 集成测试 | 77/77 100% | `mvn test`（需 MySQL+Redis） |
| 接口测试 | 109/109 100% | 同集成测试 |
| 冒烟测试 | 14/14 100% | `bash scripts/smoke-test.sh` |

## 安全注意事项

- `.env` 含 `DB_PASSWORD`、`DEEPSEEK_API_KEY`、邮件配置，**严禁提交到 Git**
- JWT 密钥在 `application.yml` 的 `jwt.secret`，生产环境请替换
- 文件上传：扩展名白名单 + 魔数（Hutool FileTypeUtil）双重检测
- 缓存/限流：`PublicRateLimitFilter` 用 Caffeine Cache 限流（20 次/秒），`RankServiceImpl` 用 Redis 缓存排行榜
- Token 黑名单：`InMemoryTokenBlacklistService` 用 Caffeine Cache 存储登出 token

## 常见任务

### 单独运行某个测试

```bash
# 前端
npx vitest run src/views/admin/audit/__tests__/index.test.ts

# 后端
mvn test -Dtest="AdminApiTest#approveSubmittedWork"
mvn test -Dtest="com.jingxuan.modules.work.**"
```

### 查看集成测试日志

```bash
# 测试配置在 application-test.yml（MySQL jingxuan_test）
export DB_PASSWORD=****
mvn test -Dtest="AdminApiTest"
```

### 代码检查

```bash
# 前端
npx vue-tsc --noEmit          # TypeScript 类型检查
npm run lint                   # ESLint
npm run format                 # Prettier

# 后端
mvn compile -q                 # 编译检查
```
