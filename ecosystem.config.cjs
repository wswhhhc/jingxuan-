module.exports = {
  apps: [
    {
      name: 'jingxuan-back',
      cwd: '/opt/jingxuan',
      script: 'java',
      args: '-Xmx256m -Xms128m -XX:+UseSerialGC -XX:MaxMetaspaceSize=96m -XX:+UseStringDeduplication -jar backend/jingxuan-backend-1.0.0.jar --spring.profiles.active=prod --spring.main.lazy-initialization=true',
      env: {
        NODE_ENV: 'production'
      }
    }
  ]
}

