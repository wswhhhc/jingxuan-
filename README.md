# Jingxuan cloud deployment bundle

This folder is used to assemble the files needed on the cloud server.

Expected server runtime:

- JDK 17+
- MySQL 8
- Redis 7
- Nginx
- PM2

Files copied into the final bundle:

- `backend/jingxuan-backend-1.0.0.jar`
- `frontend/dist/`
- `sql/`
- `.env.example`
- `ecosystem.config.cjs`
- `nginx-jingxuan.conf`
- `deploy-server.sh`

Typical server layout:

```bash
/opt/jingxuan/
  backend/jingxuan-backend-1.0.0.jar
  frontend/dist/
  sql/
  .env
  ecosystem.config.cjs
```

On first deploy, create the database and import SQL in this order:

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS jingxuan CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -p jingxuan < sql/base/init_schema.sql
mysql -u root -p jingxuan < sql/business/work_schema.sql
mysql -u root -p jingxuan < sql/business/test_data.sql
```

Then fill `/opt/jingxuan/.env` and run:

```bash
bash deploy-server.sh
```

