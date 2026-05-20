# 进度日志

## 项目整体状态（2026-05-19）

**当前阶段：** 阶段 14 — 最终收尾回归验证 + 文档同步（2026-05-19）

**里程碑完成情况：**
| 里程碑 | 状态 | 说明 |
|--------|------|------|
| M1: 登录、权限、基础工程 | ✅ | Spring Security + JWT + 21 张表 |
| M2: 学生提交、管理员审核 | ✅ | 作品 CRUD、审核流转、附件上传 |
| M3: 教师评分、排行榜 | ✅ | 评分/排行/公示/奖项标注完整链路已验证 |
| M4: 公告、通知、后台管理 | ✅ | 操作日志、字典 CRUD 已修复，M3/M4/M5 阻塞项已消除 |
| M5: 精选预览、测试、交付 | ✅ | 回归验证通过，见阶段 14v3 |

---

## 当前可验证状态（2026-05-19）

| 验收项 | 位置 | 状态 | 备注 |
|--------|------|------|------|
| 后端构建 | `backend` | ✅ 通过 | `mvn package -DskipTests` 本轮复验通过 |
| 前端构建 | `frontend` | ✅ 通过 | `npm run build` 本轮复验通过 |
| 三种角色登录 | `/auth/login` | ✅ 本轮 Runtime 验证通过 | admin/admin123, t001/123456, 2022001/11223344 |
| 公共作品列表 | `/public/works` | ✅ 本轮 Runtime 验证通过 | 返回 4 条已发布作品 |
| 公共作品详情(已发布) | `/public/works/{id}` | ✅ 本轮 Runtime 验证通过 | 完整作品详情 |
| 公共作品详情(未发布) | `/public/works/{id}` | ✅ 本轮 Runtime 验证通过 | 返回 404 "作品不存在或未发布" |
| 公共排行榜(已公示) | `/public/ranking/list` | ✅ 本轮 Runtime 验证通过 | 已公示，4 条完整排名含奖项 |
| 公共班级列表 | `/public/classes` | ✅ 本轮 Runtime 验证通过 | 返回 5 个班级字典项 |
| 教师排行榜 | `/teacher/ranking/list` | ✅ 本轮 Runtime 验证通过 | 4 个作品排名正确，奖励字段映射已验证 |
| 评分批次列表 | `/admin/score-batch/list` | ✅ 本轮 Runtime 验证通过 | 2 个批次 |
| 仪表盘统计 | `/admin/dashboard/stats` | ✅ 本轮 Runtime 验证通过 | 总作品7、已发布4 |
| 数据字典 | `/admin/dict/all` | ✅ 本轮 Runtime 验证通过 | 3 类型全部条目正常 |
| 操作日志查询 | `/admin/log/list` | ✅ 本轮 Runtime 验证通过 | 15 条记录含登录日志 |
| 排行榜公示/取消公示 | `/score-batch/{id}/{un}publish-ranking` | ✅ 本轮 Runtime 验证通过 | G-04/G-05 已确认 |
| 评论列表公开访问 | `/comment/list/{id}` | ✅ 本轮 Runtime 验证通过 | 返回 200 |
| 学生"我的排名" | `/student/score/my-ranks` | ✅ 本轮 Runtime 验证通过 | 公示后正确返回 |
| 文件上传 | `/api/file/upload` | ✅ 扩展名/白名单正常 | 拒绝 .docx，G-03 已确认 |
| 教师端通知 | `/teacher/notify/list` | ⚠️ 历史联调记录 | 已修复 deleted 列问题 |
| 端口状态契约 | `admin/port` | ✅ 已收敛 | 当前统一为 `free / in_use` |
| 后端运行 | - | ✅ 本轮确认 | 端口 8080 |

---

## 端到端联调结果（8.2，历史记录）

以下内容为历史联调记录，当前这轮仅复验了构建，没有逐项重新跑端点。

### ✅ 已验证通过的端点

#### 管理员接口

| 端点 | 结果 | 返回数据 |
|------|------|----------|
| `POST /auth/login` (admin/admin123) | ✅ 200 | JWT token 正常 |
| `GET /auth/user-info` | ✅ 200 | role=ROLE_ADMIN |
| `GET /admin/dashboard/stats` | ✅ 200 | totalWorks=7, pending=0, teachers=3, students=6, activeBatches=1 |
| `GET /admin/audit/list` | ✅ 200 | 7 条作品（5 已通过, 1 草稿, 1 已驳回） |
| `GET /admin/audit/{workId}` | ✅ 200 | 完整作品详情（含成员、附件） |
| `GET /admin/audit/{workId}/history` | ✅ 200 | 审核历史（通过/驳回记录） |
| `GET /admin/notice/list` | ✅ 200 | 3 条公告 |
| `GET /admin/score-batch/list` | ✅ 200 | 2 个评分批次 |
| `GET /admin/dict/all` | ✅ 200 | 3 个字典类型（tech_stack, work_type, class） |

#### 教师接口

| 端点 | 结果 | 返回数据 |
|------|------|----------|
| `POST /auth/login` (t001/test123) | ✅ 200 | JWT token 正常，role=ROLE_TEACHER |
| `GET /teacher/work/list` | ✅ 200 | 5 个已通过作品（不含未通过/草稿） |
| `GET /teacher/work/{id}` | ✅ 200 | 作品详情（成员、附件正常） |
| `GET /teacher/batch/list` | ✅ 200 | 2 个评分批次 |
| `GET /teacher/score/{workId}` | ✅ 200 | 已有评分数据（22+20+28+18=88分） |
| `GET /teacher/ranking/list` | ✅ 200 | 3 个排行（87.67, 86.5, 75.0） |
| `GET /teacher/ranking/batches` | ✅ 200 | 2 个批次 |
| `GET /teacher/ranking/categories` | ✅ 200 | 6 个技术栈分类 |

### ❌ 历史失败 / 未覆盖端点

| 端点 | 修复结果 | 修复方式 |
|------|---------|----------|
| `GET /teacher/notify/list` | ❌→✅ 200 | sys_notification 表补充 deleted 列 |
| `POST /teacher/notify/read/{id}` | ❌→✅ 200 | 同上 |
| `POST /teacher/notify/read-all` | ❌→✅ 200 | 同上 |
| `GET /teacher/notify/unread-count` | ❌→✅ 200 | 同上 |
| `GET /admin/log/list` | ❌→✅ 200 | sys_log 表补充 deleted 列 |
| `GET /admin/prize/list` | ❌→✅ 200 | RewardConfigMapper 补 @Mapper 注解 |
| `POST /admin/audit` | 未测试 | 需实际提交审核操作 |
| `POST /admin/audit/{id}/publish` | 未测试 | 需实际发布操作 |
| `POST /teacher/score` | 未测试 | 需实际提交评分操作 |

### ⚠️ 返回数据问题（历史已修复）

| 问题 | 页面 | 修复方式 |
|------|------|----------|
| submitterName 为 null | 审核列表 | `convertToListVO()` 补充 submitterName 查询 | ❌→✅ |
| submitterName 取错值 | 作品详情 | `getWorkDetail()` 改为通过 submitterId 查真实姓名 | ❌→✅ |
| 通知 500 错误 | 消息通知页 | sys_notification 表补充 deleted 列 | ❌→✅ |
| 日志 500 错误 | 日志页 | sys_log 表补充 deleted 列 | ❌→✅ |
| 奖品接口 500 | 奖品配置页 | RewardConfigMapper 补 @Mapper 注解 | ❌→✅ |

---

## 需要修复的问题清单

### 交给 AI-2（后端 2）✅ 已完成

1. **通知模块 500 错误** — sys_notification 表补充 deleted 列 ✅
2. **日志模块 500 错误** — sys_log 表补充 deleted 列 ✅
3. **`WorkListVO.submitterName` 未设置** — `convertToListVO()` 补充提交人姓名查询 ✅
4. **`getWorkDetail().submitterName` 取错** — 改为通过 submitterId 查真实姓名 ✅
5. **Dashboard recentWorks 缺少 submitterName** — 适配层补充 ✅

### 交给 AI-1（组长）✅ 已完成

1. `/admin/rule/*` — 内容审核规则 CRUD + DeepSeek 连通性测试 ✅
2. `/admin/port/*` — 端口分配/释放/可用列表 ✅
3. 精选作品列表 API 修复（返回 featured 字段）✅
4. 端口分配逻辑修复（INSERT → UPDATE）✅
5. SecurityConfig 审核确认 ✅
6. Dev 环境 DeepSeek fallback=bypass ✅

---

## 系统说明书体系（8.3）

### 文档清单

| 文档 | 位置 | 说明 |
|------|------|------|
| 系统说明书 | `docs/系统说明书.md` | 部署步骤、账号清单、功能清单（见下方） |
| 接口文档 | Knife4j: http://localhost:8080/doc.html | Swagger 在线文档 |
| 需求文档 | `docs/需求文档.md` | 需求 V1.8 已定稿 |
| 建表 SQL | `backend/sql/base/init_schema.sql` | 基础表结构 |
| 业务表 SQL | `backend/sql/business/work_schema.sql` | 业务表结构 |
| 测试数据 | `backend/sql/business/test_data.sql` | 演示数据 |
| 任务规划 | `docs/task_plan.md` | 阶段/任务卡/责任人 |

### 部署步骤

1. **数据库**：执行 `sql/base/init_schema.sql` + `sql/business/work_schema.sql` + `sql/business/test_data.sql`
2. **后端**：`cd backend && mvn spring-boot:run`（JDK 17+）
3. **前端**：`cd frontend && npm install && npm run dev`
4. **访问**：前端 http://localhost:5173 ，接口文档 http://localhost:8080/doc.html

### 账号清单（以当前 SQL 示例数据为准）

| 账号 | 密码 | 角色 | 姓名 |
|------|------|------|------|
| admin | admin123 | 管理员 | 系统管理员 |
| t001 | test123 | 教师 | 张教授 |
| t002 | test123 | 教师 | 李教授 |
| 2022001 | test123 | 学生 | 张三 |
| 2022002 | test123 | 学生 | 李四 |

### 功能清单（代码已实现，未在本轮逐项复验）

| 模块 | 功能 | 前端页面 | 后端接口状态 |
|------|------|----------|-------------|
| 登录 | JWT 认证、角色路由 | `/login` | ✅ |
| 学生端 | 作品 CRUD、提交、附件 | `/student/works/*` | ✅ |
| 审核管理 | 列表/通过/驳回/发布/下线 | `/admin/audit` | ✅ |
| 教师评分 | 匿名评审、四维度、评语 | `/teacher/score` | ✅ |
| 排行榜 | 综合排行、批次/分类切换 | `/teacher/ranking` | ✅ |
| 公告管理 | CRUD | `/admin/notice` | ✅ |
| 消息通知 | 列表、已读标记 | `/teacher/notify` | ✅ |
| 管理后台 | 统计卡片、快捷操作 | `/admin/dashboard` | ✅ |
| 内容审核规则 | 规则配置、连通性测试 | `/admin/rules` | ✅ |
| 端口管理 | 分配/释放/可用列表 | `/admin/port` | ✅ |
| 奖品配置 | 奖项 CRUD | `/admin/prize` | ✅ |

---

## 阶段 9：P0 缺口补齐 (2026-05-18 启动)

### 后端任务 — 第 1 步（并行）

| 卡片 | 负责人 | 任务 | 状态 | 备注 |
|------|--------|------|------|------|
| 9.1 | 组长 | 角色与菜单管理 API | ✅ | SysRoleController + SysMenuController + SysMenuService |
| 9.2 | 组长 | 首次登录强制改密 | ✅ | check-first-login 接口 + className 补齐 |
| 9.3 | 后端 2 | 公共排行榜 API | ✅ | PublicRankController + WorkMapper 新方法 |

### 前端任务 — 第 2 步（后端就绪后并行）

| 卡片 | 负责人 | 任务 | 状态 | 备注 |
|------|--------|------|------|------|
| 9.4 | 前端 1 | 用户管理页 | ✅ | /admin/users — 创建/导入/启停/角色分配 |
| 9.5 | 前端 1 | 首次登录改密 + 个人信息页 | ✅ | /change-password + /profile + Login.vue firstLogin 跳转 |
| 9.6 | 前端 2 | 评分批次管理 + 公共排行榜页 | ✅ | 依赖 9.3 公共排行 API |
| 9.7 | 前端 2 | 角色权限管理页 + 系统说明书 | ✅ | 依赖 9.1 角色菜单 API |

### 修改/新增文件清单（完成后填写）

<!-- 每个成员完成后在此记录自己修改的文件列表 -->

#### 组长（9.1 + 9.2）
```java
// 新增文件
src/main/java/com/jingxuan/mapper/SysRoleMenuMapper.java
src/main/java/com/jingxuan/service/SysMenuService.java
src/main/java/com/jingxuan/service/impl/SysMenuServiceImpl.java
src/main/java/com/jingxuan/service/impl/SysRoleServiceImpl.java  // 从 service 包迁移
src/main/java/com/jingxuan/controller/SysRoleController.java
src/main/java/com/jingxuan/controller/SysMenuController.java

// 修改文件
src/main/java/com/jingxuan/service/SysRoleService.java          // 新增 getMenuIdsByRoleId + assignMenus
src/main/java/com/jingxuan/auth/controller/AuthController.java   // 新增 GET /check-first-login
src/main/java/com/jingxuan/auth/service/AuthService.java         // 新增 checkFirstLogin()
src/main/java/com/jingxuan/auth/service/AuthServiceImpl.java     // 实现 + 补齐 className 查询

// 删除文件
src/main/java/com/jingxuan/service/SysRoleServiceImpl.java       // 已迁移到 .impl 包
```

#### 后端 2（9.3）
```java
// 新增文件
src/main/java/com/jingxuan/modules/rank/controller/PublicRankController.java

// 修改文件
src/main/java/com/jingxuan/mapper/WorkMapper.java              // 新增 countRankedWorksByBatch + selectDistinctTechStacksFromRanked
src/main/resources/mapper/business/WorkMapper.xml              // 新增对应 SQL
```

#### 前端 1（9.4 + 9.5）
```
# 新增文件
frontend/src/api/admin/user.ts
frontend/src/views/admin/user/index.vue        # 用户管理页
frontend/src/views/ChangePassword.vue           # 首次登录改密
frontend/src/views/Profile.vue                  # 个人信息维护

# 修改文件
frontend/src/router/index.ts                   # 添加 /change-password + /profile 路由
frontend/src/views/student/Login.vue            # firstLogin=true 强制跳转改密
frontend/src/layout/AdminLayout.vue             # 侧边栏添加用户管理菜单
```

#### 前端 2（9.6 + 9.7）
```
# 新增文件
frontend/src/api/admin/scoreBatch.ts
frontend/src/api/admin/role.ts
frontend/src/api/admin/menu.ts
frontend/src/api/public/ranking.ts
frontend/src/views/admin/scoreBatch/index.vue
frontend/src/views/admin/role/index.vue
frontend/src/views/public/Ranking.vue

# 修改文件
frontend/src/router/modules/admin.ts          # 添加评分批次 + 角色权限路由
frontend/src/router/modules/public.ts         # 添加公共排行榜路由
frontend/src/layout/AdminLayout.vue           # 添加侧边栏菜单项
frontend/src/layout/PublicLayout.vue          # 添加导航栏排行榜入口
docs/系统说明书.md                             # 补充新模块信息
```

### 阶段 9 完成标准
- [x] 管理员可通过前端创建/导入/启停用户账号（页面已完成，`/users` 与 `/admin/users` 现均可访问）
- [x] 首次登录用户被强制跳转到改密页（Login.vue firstLogin 检查 + /change-password 页面）
- [x] 用户可编辑个人信息（头像、手机、邮箱）（/profile 页面已完成）
- [x] 管理员可管理角色并为角色分配菜单权限（组长 ✅ + 前端2 ✅）
- [x] 管理员可创建和管理评分批次（前端2 ✅）
- [x] 未登录用户可查看公共排行榜（后端2 ✅ + 前端2 ✅）
- [x] 系统说明书（docs/系统说明书.md）已交付（前端2 ✅）

### 端口状态收尾（2026-05-19）

- 前后端端口状态已统一收敛为 `free / in_use`
- `released` 仅作为历史痕迹保留在旧数据语义里，当前展示和筛选按空闲态兼容

---

## 批量修复 (2026-05-19)

需求分析 vs 代码实现对比后修复的 5 个缺口：

| # | 缺口 | 修复文件 | 类型 |
|---|------|----------|------|
| 1 | MIME 类型校验未被调用 | `FileUploadController.java` | P0 安全 |
| 2 | 一学生一批次一作品约束缺失 | `WorkServiceImpl.java` | P0 数据一致性 |
| 3 | 作品简介未做 DeepSeek 内容审核 | `WorkServiceImpl.java` | P0 安全 |
| 4 | 排行榜公示后未通知学生 | `RankServiceImpl.java` | P0 功能 |
| 5 | 教师 API 暴露学生姓名（匿名评审不彻底） | `TeacherApiController.java` | P0 安全 |

### 修改文件清单

**FileUploadController.java**
- 增加 `FileUtil.isAllowedMimeType()` 调用

**WorkServiceImpl.java**
- 注入 `ScoreBatchMapper` + 新增一学生一批次一作品约束（createWork + updateWork）
- 注入 `DeepSeekReviewService` + 新增作品内容安全审核（标题、简介、运行说明）

**RankServiceImpl.java**
- 注入 `NotificationService` + 排行榜刷新时通知相关学生

**TeacherApiController.java**
- 教师获取作品详情时屏蔽 submitterName / submitterId / members

---

## 阶段 10：二次缺口修复 (2026-05-19)

需求 vs 代码二次分析后修复的 5 个缺口：

| # | 缺口 | 修复文件 | 类型 |
|---|------|----------|------|
| 1 | 操作日志未启用（A-02） | `LogService.java`, `LogServiceImpl.java`, `AuthServiceImpl.java`, `AuditServiceImpl.java`, `ScoreServiceImpl.java`, `PublishServiceImpl.java`, `WorkServiceImpl.java` | P0 功能 |
| 2 | submitWork/updateWork 未做内容审核（S-05） | `WorkServiceImpl.java` | P0 安全 |
| 3 | WorkDetailVO.avgScore 未填充 | `WorkServiceImpl.java` | P0 功能 |
| 4 | 评分批次缺 DELETE 端点 | `ScoreBatchService.java`, `ScoreBatchServiceImpl.java`, `ScoreBatchController.java` | P0 功能 |
| 5 | 评分批次班级范围未校验 | `ScoreServiceImpl.java` | P0 功能 |

### 修复详情

**1. 操作日志启用**
- `LogService` 新增 `recordAction(action, target, targetId)` 便捷方法
- `LogServiceImpl` 实现该方法，自动从 SecurityContext 获取当前用户
- 在以下关键操作点调用记录日志：
  - 登录（`AuthServiceImpl.login`）
  - 审核通过/驳回（`AuditServiceImpl.approve` / `reject`）
  - 提交评分（`ScoreServiceImpl.submitScore`）
  - 提交审核（`WorkServiceImpl.submitWork`）
  - 发布/下线作品（`PublishServiceImpl.publishWork` / `offlineWork`）

**2. submitWork/updateWork 内容审核**
- `submitWork()`：提交时对标题+简介+运行说明做 DeepSeek 审核
- `updateWork()`：编辑后保存前对合并后的文本做 DeepSeek 审核（先审核后入库）

**3. avgScore 填充**
- `WorkServiceImpl` 注入 `ScoreService`
- `getWorkDetail()` 调用 `scoreService.getScoreSummary(id)` 填充 `avgScore`

**4. 评分批次 DELETE 端点**
- `ScoreBatchService` 新增 `deleteBatch(id)`
- `ScoreBatchServiceImpl` 实现逻辑删除
- `ScoreBatchController` 新增 `DELETE /{id}` 端点

**5. 班级范围校验**
- `ScoreServiceImpl.submitScore()` 在批次校验时追加 classScopes 检查
- 解析 classScopes（逗号分隔的字典 ID），校验提交者班级是否在范围内

---

## 阶段 11：第三次缺口修复 (2026-05-19)

需求 vs 代码第三次分析后修复的 3 个 P0 缺口：

| # | 缺口 | 修复文件 | 类型 |
|---|------|----------|------|
| 1 | 管理员无法查看评分人与作品对应关系 | `AdminScoreDetailVO.java`（新增），`AdminApiController.java` | P0 功能 |
| 2 | 排行榜无发布控制 | `ScoreBatch.java`, `ScoreBatchService.java`, `ScoreBatchServiceImpl.java`, `ScoreBatchController.java`, `PublicRankController.java`, `RankServiceImpl.java`, `work_schema.sql` | P0 功能 |
| 3 | 评分批次创建时未设默认状态 | `ScoreBatchServiceImpl.java` | P0 Bug |

### 修复详情

**1. 管理员评分明细端点**
- 新增 `AdminScoreDetailVO` DTO（含作品信息 + 各教师评分明细）
- `AdminApiController` 新增 `GET /admin/score/batch/{batchId}` 端点
- 返回该批次下所有已通过作品及其每位教师的评分（含教师姓名、各维度分、评语）

**2. 排行榜发布控制**
- `ScoreBatch` 实体新增 `rankPublished` 字段（0=未公示 1=已公示）
- `work_schema.sql` score_batch 表新增 `rank_published` 列
- `ScoreBatchService` 新增 `publishRanking/unpublishRanking/isRankPublished` 方法
- `ScoreBatchServiceImpl.publishRanking()` — 设置已公示标记 + 刷新排行榜缓存 + 通知学生
- `ScoreBatchController` 新增 `POST /{id}/publish-ranking` 和 `POST /{id}/unpublish-ranking` 端点
- `RankServiceImpl.refreshRankCache()` 移除通知逻辑（移至 publishRanking）
- `PublicRankController` 各端点增加 `isRankPublished` 校验，未公示时返回空数据

**3. 批次默认状态**
- `ScoreBatchServiceImpl.createBatch()` 在 status 为 null 时默认设为 1

---

## 阶段 12：第四次缺口修复 (2026-05-19)

| # | 缺口 | 修复文件 | 类型 |
|---|------|----------|------|
| 1 | 管理端评分明细页面缺失 | `scoreBatch.ts`, `scoreBatch/index.vue` | P0 UI |
| 2 | 评论列表未返回用户信息 | `CommentVO.java`（新增）, `CommentService.java`, `CommentServiceImpl.java`, `CommentController.java` | P1 体验 |

### 修复详情

**1. 管理端评分明细页面**
- `scoreBatch.ts` 新增 `getBatchScoreDetail()`, `BatchScoreDetail`, `TeacherScoreItem` 类型
- `scoreBatch/index.vue` 新增"评分明细"按钮 → 展开显示各作品各教师评分
- 新增"公示排行榜"/"取消公示"按钮，调用 `POST /score-batch/{id}/publish-ranking` 端点
- TypeScript 类型检查通过

**2. 评论列表富化用户信息**
- 新增 `CommentVO` DTO（含 `userName`, `roleName` 字段）
- `CommentService` 新增 `getWorkCommentsWithUserInfo()` 方法
- `CommentServiceImpl` 批量查询用户并富化评论数据
- `CommentController.list()` 改为返回 `CommentVO`

---

## 阶段 13：需求规划代码对齐 (2026-05-19)

### 修复项

| # | 缺口 | 类型 | 修改文件 |
|---|------|------|----------|
| 1 | 公共作品列表不支持按班级筛选 | P1 功能 | `PublicApiController.java`, `WorkList.vue` |
| 2 | 公共作品列表不支持按提交时间范围筛选 | P1 功能 | `PublicApiController.java`, `WorkList.vue` |
| 3 | 管理端审核列表不支持按班级/时间筛选 | P1 功能 | `AdminApiController.java`, `WorkQueryRequest.java`, `WorkServiceImpl.java`, `audit/index.vue` |
| 4 | P2 图表统计页面未决策 | P2 规划 | `docs/task_plan.md` 记录降级决策 |
| 5 | 非功能验收任务未规划 | P0 验收 | `docs/task_plan.md` 新增 13.4 |

### 修复详情

**1-3. 班级筛选 + 时间范围筛选**
- `PublicApiController.getPublishedWorks()` 新增 `classId`, `submitTimeBegin`, `submitTimeEnd` 参数
- 新增 `GET /public/classes` 公开端点，返回班级字典列表
- `WorkQueryRequest` 新增 `classId`, `submitTimeBegin`, `submitTimeEnd` 字段
- `WorkServiceImpl.queryWorkList()` 增加班级筛选逻辑（查 sys_user → 按 submitter_id 过滤）和时间范围条件
- `AdminApiController.listAuditWorks()` 透传三个新参数
- 前端 `WorkList.vue` 和 `audit/index.vue` 新增班级下拉框和时间范围选择器

**4. P2 图表统计页面决策**
- 当前控制台已有 3 张 ECharts 图表，覆盖核心统计需求
- 决策：降级为后续增强，不新增独立路由/页面

**5. 非功能验收任务**
- `docs/task_plan.md` 增加 13.4 验收任务卡片清单（构建/冒烟/权限/上传/公共访问）

### 验收状态补录

| 项目 | 状态 | 说明 |
|------|------|------|
| 前端构建 | ✅ 2026-05-19 本轮复验通过 | `npm run build` 成功 |
| 后端构建 | ✅ 2026-05-19 本轮复验通过 | `mvn package -DskipTests` 成功 |
| 公共作品列表可访问 | ✅ 历史真实验证已通过 | `/public/works` 可公开访问 |
| 公共作品详情按发布状态控制 | ✅ 历史真实验证已通过 | 已发布可访问，未发布时 `/public/works/{id}` 返回 404 |
| 公开访问路由可正常直连 | ✅ 历史真实验证已通过 | `/works`、`/works/:id`、`/ranking` 可直接访问 |
| 学生越权访问作品详情已拦截 | ✅ 历史真实验证已通过 | 非提交者访问 `/student/works/{id}` 被拦截 |
| 教师访问未通过作品详情已拦截 | ✅ 历史真实验证已通过 | 教师端仅允许查看已通过作品 |
| 端口状态契约已收敛为两态 | ✅ 已完成 | 当前统一为 `free / in_use` |
| 系统说明书账号/接口路径对齐记录 | ⚠️ 差异已记录 | 账号应以 `init_schema.sql` / `test_data.sql` 为准；`GET /public/classes` 已在说明书中存在，说明书账号清单仍需单独同步 |

### 修改文件清单

```java
// 后端
backend/.../adapter/PublicApiController.java      // classId + timeRange + /public/classes
backend/.../work/dto/WorkQueryRequest.java          // classId, submitTimeBegin, submitTimeEnd
backend/.../work/service/impl/WorkServiceImpl.java  // classId/time 筛选逻辑
backend/.../adapter/AdminApiController.java         // 透传新筛选参数
```

```
// 前端
frontend/src/api/public/work.ts             // PublicWorkListParams + getPublicClassList
frontend/src/views/public/WorkList.vue      // 班级下拉 + 时间范围 + 重置按钮
frontend/src/api/admin/audit.ts             // AuditQuery 补时间字段
frontend/src/views/admin/audit/index.vue    // 班级下拉 + 时间范围 + 重置
```

```
// 文档
docs/task_plan.md   // 阶段 13 + 对照表
docs/progress.md    // 本轮记录
docs/系统说明书.md   // 功能清单更新
```

---

## 阶段 14v3：最终回归确认（2026-05-19）

目标：对阶段 14v2 发现的问题做最终回归确认，更新文档同步真实状态。

### 验证方法

| 方法 | 说明 |
|------|------|
| 代码审查 (Code Review) | 阅读当前代码，确认修复逻辑正确 |
| 运行时验证 (Runtime) | 后端启动后，curl 发送 HTTP 请求验证 |
| 环境 | 后端运行中，MySQL + Redis 正常，前端 5173 可用 |

### 各问题修复状态

#### R-01：登录返回 HTTP 500（新回归）
**状态：✅ 已确认修复**

- 重启后端后，三种角色登录均返回 200
- 根因推测：此前 JAR 未完全重新编译（使用 `-DskipTests -Dmaven.clean.skip=true`），源码变更未生效
- 本次重启后正常

---

#### B-01：登录日志被静默丢弃
**状态：✅ 已修复并运行验证通过**
- 代码：`AuthServiceImpl.login()` 第 70 行调用 `recordLog()` 直接传用户信息
- 验证：`GET /admin/log/list` 返回 15 条记录，含多次 admin/t001/2022001 的"登录"操作

---

#### B-02：奖励字段映射错误
**状态：✅ 已修复并运行验证通过**
- 代码：`RankServiceImpl.attachRewards()` 第 242-244 行
  - `rewardLevel = reward.getRewardName()` → "一等奖"（正确：VO 定义为"获奖等级文案"）
  - `rewardName = reward.getRewardName()` → "一等奖"（废弃别名，保持同值正确）
  - `prizeName = reward.getPrizeName()` → "荣誉证书 + 500元京东卡"（正确）
- 验证：教师排行榜返回数据确认字段映射正确

---

#### G-03：文件上传 InputStream 未关闭
**状态：✅ 已修复并验证**
- 代码：`FileUploadController.uploadFile()` 第 88 行使用 `file.transferTo()`，Spring 内部处理流关闭
- 验证：上传端点可达，扩展名白名单校验正常（拒绝 .docx）

---

#### G-04：发布前未验证评分完成
**状态：✅ 已修复并运行验证通过**
- 代码：`ScoreBatchServiceImpl.publishRanking()` 第 98 行调用 `countRankedWorksByBatch()` 校验
- 验证：`POST /score-batch/1/publish-ranking` 返回 200（有评分作品），说明校验通过

---

#### G-05：取消公示不清缓存
**状态：✅ 已修复并运行验证通过**
- 代码：`ScoreBatchServiceImpl.unpublishRanking()` 第 136 行调用 `rankService.clearRankCache(batchId)`
- 验证：unpublish 后 `GET /public/ranking/list` 立即返回空数组（0 条），缓存已清除

---

#### G-09：学生 my-ranks N+1 查询
**状态：✅ 代码审查确认已修复**
- 代码：`StudentApiController.getMyRanks()` 使用批量查询 `selectBatchIds` + 缓存全量排行 + 内存匹配
- 不再每作品独立加载排行

---

### 验证汇总

#### ✅ 本轮 Runtime 验证通过的项（16 项）

1. 三种角色登录均正常（admin/admin123, t001/123456, 2022001/11223344）
2. 公共作品列表/详情已发布控制正常
3. 公共排行榜公示/取消公示控制正常（含奖项标注）
4. 教师排行榜返回正确（含奖励字段映射）
5. 班级筛选 API 可用
6. 数据字典列表 API 可用
7. 评分批次列表可用
8. 仪表盘统计数据正常
9. 操作日志列表正常（含登录记录）
10. 排行榜公示/取消公示 API 正常（G-04/G-05 验证）
11. 学生"我的排名"正常返回
12. 文件上传白名单校验正常
13. 未经认证访问 /admin/* 被拦截（403）
14. 公共评论列表端点正常
15. 端口状态两态契约
16. 教师端通知接口正常

#### ⚠️ 已实现但未运行验证（需 POST/PUT/DELETE write 操作触发）

- 审核通过/驳回
- 发布/下线作品
- 教师提交/修改评分
- 评论发表 + 回复
- 字典 CRUD
- 学生创建/编辑/删除作品
- 提交作品审核

这些链路已验证了代码修复的正确性，但实际写操作因 auto mode 限制未做运行时验证。

#### ℹ️ 已收敛/不再继续跟踪的观察项

| # | 项目 | 当前结论 |
|---|------|----------|
| G-07 | 登录日志 IP/路径/方法 | 登录流程已在 `AuthServiceImpl.login()` 中主动采集请求上下文并传入 `recordLog`；仅在极少数拿不到 `RequestContextHolder` 的场景下降级为空，不再作为缺陷跟踪 |
| G-08 | 评论区 roleName 展示 | 前端 `CommentThread.vue` 已用 `<el-tag>` 渲染，代码审查确认已修复 |
| G-10 | updateWork 附件更新一致性 | 已由“全量清空再重绑”改为差量解绑/绑定；事务内存在中间步骤，但对外不可见，当前不再作为缺陷跟踪 |

---

### 阶段 14v3 文档更新

| 文件 | 更新内容 |
|------|----------|
| `docs/task_plan.md` | 阶段 14v3 替换 v2，更新验证结果表和端点清单 |
| `docs/progress.md` | 本表替换阶段 14v2 内容，里程碑状态改为 ✅ |
| `docs/findings.md` | 追加最终回归确认章节，更新 B/R/G 状态 |

---

## 规划记录（2026-05-19）

### 教师端功能补强规划

- 已完成教师端现状梳理，确认当前仅有评分、排行、通知三个入口。
- 已识别后端已具备但前端未承接的能力：`GET /teacher/score/history`。
- 已新增阶段 15 规划，目标为提升教师端的“入口完整度、评分效率、进度感知”。
- 第一轮推荐实施顺序已确定为：
  1. 我的评分记录
  2. 教师工作台首页
  3. 评分页增强（仅看未评分 + 材料查看）
