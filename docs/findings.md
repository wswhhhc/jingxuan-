# 发现与决策

## 需求结论
- 需求文档 V1.8 已定稿，包含 9 大功能模块、19 张数据库表。
- 用户角色固定为学生、教师、管理员，采用管理员预置账号，不开放自助注册。
- 审核状态与发布状态分离，避免一个字段同时承担两类业务语义。
- 精选作品仅 1 至 2 个支持在线体验，其余作品以截图、视频和说明为主。

## 技术研究
- Spring Boot 3.x 需要 JDK 17+。
- Vue 3 + Element Plus 适合快速搭建管理系统前端。
- MyBatis-Plus 3.5.x 可与 Spring Boot 3.x 配合使用。
- DeepSeek 内容审核接口采用官方 API，模型为 `deepseek-v4-flash`。

## 技术决策
| 决策 | 说明 |
|------|------|
| BCrypt 密码加密 | 满足 Spring Security 常规安全实践 |
| JWT 无状态认证 | 适合前后端分离，避免 session 依赖 |
| 文件上传白名单 | 限制可执行文件与高风险格式 |
| API Key 环境变量配置 | 不暴露给前端或仓库配置 |
| 本地文件存储优先 | 更适合课程实训环境 |
| MinIO 作为可选项 | 仅在有余力时再增强 |
| Nginx 仅用于精选预览 | 不进入主业务主线 |

## 已识别问题
| 问题 | 处理方式 |
|------|----------|
| 审核状态与发布状态混淆 | 拆分为 `work.status` 和 `work_publish.publish_status` |
| 班级数据来源不一致 | 已注册成员以 `sys_user.class_id` 为准，未注册成员保留冗余文本 |
| 一名学生一批次一作品约束无法覆盖全部成员 | 仅对已注册账号强制约束 |
| `D-07` 与 `P-01` 重叠 | `D-07` 调整为作品下线，精选标记并入 `P-01` |

## 规划优化建议
- `P0` 应该优先完成闭环，而不是只罗列功能点。
- `LLM 内容安全审核`、`评分批次管理`、`消息通知` 建议视为主流程必做项。
- `审核记录管理(A-04)` 建议落入审核或系统管理阶段，避免后台追溯能力缺失。
- `Redis`、`Nginx`、文件存储方案应尽早定案，减少开发期反复摇摆。
- 每个阶段最好补一条可验收标准，例如“能跑通一次完整端到端流程”。
- DeepSeek 联通性、文件上传校验、排行榜计算建议在早期先做最小验证。
- 测试数据最好在数据库结构明确后同步准备，而不是拖到最后一阶段。

## 回归验证阶段（2026-05-19）新发现

### 新增 Bug

| # | 发现 | 位置 | 严重度 | 说明 |
|---|------|------|--------|------|
| B-01 | 登录日志被静默丢弃 | `AuthServiceImpl.login()` → `LogServiceImpl.recordAction()` | P1 | login 时 SecurityContext 未设置，`getCurrentUserId()` 返回 null，`recordAction` 直接 return 不记录 |
| B-02 | 奖励字段映射错误 | `RankServiceImpl.attachRewards()` 第 220 行 | P1 | `rank.setRewardName(reward.getPrizeName())` 应为 `reward.getRewardName()`，导致 `rewardName` 和 `prizeName` 值相同 |
| B-03 | 评论 roleId NPE | `CommentServiceImpl.buildCommentVO()` 第 117 行 | P2 | `user.getRoleId()` 为 Integer 可 null，`.longValue()` 直接拆箱抛 NPE |
| B-04 | 多级回复在前端不可见 | `WorkDetail.vue` 第 200-206 行 | P2 | 模板只渲染 `c.replies` 一层，嵌套的 `r.replies` 被忽略 |

### 新增缺口

| # | 缺口 | 位置 | 严重度 | 说明 |
|---|------|------|--------|------|
| G-01 | 附件关联未校验 workId NULL | `WorkServiceImpl.createWork()` 第 136-142 行 | P2 | UPDATE 无条件，可跨作品窃取附件 |
| G-02 | submitWork 无附件校验 | `WorkServiceImpl.submitWork()` | P2 | 无附件也可提交审核 |
| G-03 | 文件上传 InputStream 未关闭 | `FileUploadController.uploadFile()` 第 88 行 | P2 | 应用 `file.transferTo()` 替代 |
| G-04 | 排行榜发布前不验证评分完成 | `ScoreBatchServiceImpl.publishRanking()` | P2 | 随时可公示，可能显示不完整评分 |
| G-05 | 取消公示不清缓存 | `ScoreBatchServiceImpl.unpublishRanking()` 第 120-128 行 | P2 | Redis 缓存残留 |
| G-06 | 字典 CRUD 不记日志 | `AdminApiController` dict 部分 | P3 | 字典写操作无审计追踪 |
| G-07 | 日志 IP/路径/方法始终空 | `LogServiceImpl.recordAction()` 第 44 行 | P3 | recordAction 传空串，前端对应列空白 |
| G-08 | 评论区不展示 roleName | `WorkDetail.vue` 评论模板 | P3 | 后端下发但不渲染 |
| G-09 | 学生 my-ranks N+1 查询 | `StudentApiController.getMyRanks()` 第 106-117 行 | P3 | 每作品加载全量排行 |
| G-10 | updateWork 非原子清除-设置 | `WorkServiceImpl.updateWork()` 第 224-235 行 | P3 | 并发下短暂无附件 |

### 验证覆盖说明

本次回归验证方法：
- **Runtime（真实验证）**：启动后端，curl 发送真实 HTTP 请求，验证返回状态码和数据结构
  - 覆盖：公共端点 /public/\*, 只读 GET /admin/\*, /teacher/\*, /student/\*
- **Code Review（代码审查）**：静态阅读源码，确认实现完整性和潜在问题
  - 覆盖：写操作链路（POST/PUT/DELETE）
- **未验证**：因 auto mode 阻止写操作，审核/评分/评论发表/字典 CRUD 等写操作链路未做 Runtime 验证

### 修复后验证（2026-05-19）

4 个并行修复任务对阶段 14 发现的 B-01~B-04 / G-01~G-10 进行了修复。

#### 代码审查确认已修复（8/8）
| # | 问题 | 审查结论 |
|---|------|----------|
| B-01 | 登录日志丢失 | ✅ `recordLog` 直传用户信息（不再依赖 SecurityContext），阶段 14v3 运行验证已确认日志正常入库 |
| B-02 | 奖励字段映射 | ✅ `rewardLevel/rewardName` 字段语义正确，阶段 14v3 运行验证已确认 |
| B-03 | 评论 roleId NPE | ✅ 已加 null 检查 |
| B-04 | 多级回复不可见 | ✅ 递归 CommentThread 组件 |
| G-01 | 附件跨作品绑定 | ✅ 双重防护（查询校验 + UPDATE 条件） |
| G-02 | 提交审核缺附件校验 | ✅ countQuery + 抛异常 |
| G-05 | 取消公示缓存残留 | ✅ unpublishRanking 调用 clearRankCache，阶段 14v3 运行验证已确认 |
| G-06 | 字典 CRUD 日志补齐 | ✅ DictController 三处 recordAction |

#### 修复有争议（已确认正确，无争议）
| # | 问题 | 结论 |
|---|------|------|
| B-02 | 奖励字段映射 | `rewardLevel`（VO 字段名"获奖等级文案"）="一等奖" 正确；`rewardName` 废弃别名同值；`prizeName`="荣誉证书 + 500元京东卡"。字段映射无误。 |

## 最终回归确认（2026-05-19）

在阶段 14v2 修复 + 阶段 14v3 验证后，以下为最终状态：

### 已修复并运行验证通过（7 项）

| # | 问题 | 验证方式 | 说明 |
|---|------|----------|------|
| R-01 | 登录返回 HTTP 500 | Runtime | 重启后自动消失，三种角色登录正常 |
| B-01 | 登录日志被静默丢弃 | Runtime | `/admin/log/list` 确认登录记录已入库 |
| B-02 | 奖励字段映射错误 | Runtime | 排行榜数据确认 `rewardLevel`="一等奖" 等正确 |
| G-03 | 文件上传 InputStream 未关闭 | Runtime + Code Review | `file.transferTo()` 替代流操作 |
| G-04 | 发布前未验证评分完成 | Runtime | `publishRanking` 成功执行（有评分作品） |
| G-05 | 取消公示不清缓存 | Runtime | `unpublish` 后排行立即返回空 |
| G-09 | 学生 my-ranks N+1 | Code Review | 批量查询 + 缓存排行 + 内存匹配 |

### 已修复（代码审查确认，未 runtime 验证）（5 项）

| # | 问题 | 说明 |
|---|------|------|
| B-03 | 评论 roleId NPE | null 检查已加 |
| B-04 | 多级回复不可见 | 递归 CommentThread 组件 |
| G-01 | 附件跨作品绑定 | 双重防护（查询校验 + UPDATE 条件） |
| G-02 | 提交审核缺附件校验 | countQuery + 抛异常 |
| G-06 | 字典 CRUD 日志补齐 | DictController 三处 recordAction |

### 已收敛/不再跟踪的观察项

| # | 项目 | 当前结论 |
|---|------|----------|
| G-07 | 登录日志 IP/路径/方法 | 登录流程已在 `AuthServiceImpl.login()` 中主动采集请求上下文并传入 `recordLog`；仅在极少数拿不到 `RequestContextHolder` 的场景下降级为空，不再作为缺陷跟踪 |
| G-10 | updateWork 附件更新一致性 | 已由“全量清空再重绑”改为差量解绑/绑定；事务内存在中间步骤，但对外不可见，当前不再作为缺陷跟踪 |

### 当前无阻塞项验证结论

所有阶段 14 主问题和回归问题 R-01 均已确认修复。G-07、G-10 已收敛为观察项，不再作为剩余缺陷跟踪。里程碑 M3/M4/M5 阻塞已消除。

## 参考资源
- DeepSeek API 文档：https://api.deepseek.com
- Spring Boot 3.x 文档：https://docs.spring.io/spring-boot/
- Vue 3 文档：https://cn.vuejs.org/
- Element Plus 文档：https://element-plus.org/
- MyBatis-Plus 文档：https://baomidou.com/

## 教师端补强规划发现（2026-05-19）

### 当前现状

- 教师端当前只有 `作品评分`、`排行榜`、`消息通知` 三个页面。
- 教师端缺少首页工作台，因此登录后缺乏“待处理事项”和“进度感”。
- 教师端后端已经提供 `GET /teacher/score/history`，但前端没有对应入口，属于明显的高性价比缺口。
- 当前评分页已具备匿名评审主链路，但材料查看入口仍偏弱，容易让教师在评分前信息不充分。

### 结论

教师端不是“功能错误”，而是“功能密度不足”。最优先补的不是新业务，而是：
1. 首页工作台
2. 我的评分记录
3. 评分页材料查看与未评分筛选

### 规划判断

- 若只补 1 项：优先做“我的评分记录”
- 若做一轮完整补强：建议做“工作台 + 评分记录 + 评分页增强”
- 若追求更完整的教师工作体验：再追加评分进度可视化和导出能力
