# CLAUDE.md — 菁选 (Jingxuan) 生产部署包

本仓库是**生产部署包**，不包含完整源码。存放服务器部署所需的编译后产物和配置文件。

## 部署结构

```
/opt/jingxuan/
├── backend/jingxuan-backend-1.0.0.jar   # 后端 JAR（Spring Boot 3.2.5 + Java 17）
├── frontend/dist/                         # 前端构建产物（Vue 3 + Vite 8）
├── sql/                                   # 数据库脚本（base + 增量迁移）
├── ecosystem.config.cjs                   # PM2 配置
├── nginx-jingxuan.conf                    # Nginx 反向代理
└── deploy-server.sh                       # 一键部署
```

## 部署命令

```bash
# 首次部署
mysql -u root -p jingxuan < sql/base/init_schema.sql
mysql -u root -p jingxuan < sql/business/work_schema.sql
# 按时间顺序执行 sql/business/ 下的增量迁移
cp .env.example .env   # 编辑配置
bash deploy-server.sh
```

## 关键技术点

- **端口**：后端 8080，前端由 Nginx 托管（80）
- **Nginx 配置**：`/api/` 反向代理到后端（去 `/api` 前缀），`/uploads/` 代理到后端上传路径，`/api/file/` 保留前缀
- **JVM 参数**：`-Xmx256m -Xms128m -XX:+UseSerialGC -XX:MaxMetaspaceSize=96m`（低内存优化）
- **前端路由**：SPA，所有非文件路径 fallback 到 `index.html`
- **缓存策略**：HTML 不缓存，JS/CSS 使用不可变长期缓存（文件名含内容哈希）

## 数据库

- 基础表 8 张（用户、角色、菜单等）在 `sql/base/init_schema.sql`
- 业务表 12 张（作品、评分、评论、批次等）在 `sql/business/work_schema.sql`
- 增量迁移按 `yyyy-MM-dd-*.sql` 命名，按时间顺序执行
- 所有实体继承 BaseEntity（雪花算法 id + createTime + updateTime + 逻辑删除 deleted）

## 安全

- `.env` 含 DeepSeek API Key 和数据库密码，已 `.gitignore`
- 生产环境 DB 密码从环境变量 `${DB_PASSWORD}` 读
- JWT 认证，验证码邮箱验证
