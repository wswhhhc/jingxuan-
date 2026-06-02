# Runtime Demo Package

This folder contains a minimal uploadable demo project package for the
Jingxuan runtime feature.

Structure:

- `backend/` Spring Boot demo application
- `frontend/` Vite + Vue demo frontend
- `sql/init.sql` sample schema file

To create a zip for upload on Windows PowerShell:

```powershell
Compress-Archive -Path .\samples\runtime-demo-package\* -DestinationPath .\outputs\runtime-demo-package.zip -Force
```

Expected runtime behavior:

- Backend starts on the port injected by the platform
- Frontend starts on the injected Vite port
- Frontend reads `VITE_API_BASE_URL`
- Backend responds on `/`
