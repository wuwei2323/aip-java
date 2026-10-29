import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())

try:
    ssh.connect('192.168.43.100', port=22, username='root', password='Dino#1!', timeout=10)

    # Clean up prior failed containers and data
    stdin, stdout, stderr = ssh.exec_command('cd /opt/keycloak && docker compose down -v && rm -rf /opt/keycloak/data/postgres/*')
    print('Cleaned up:', stdout.read().decode())

    sftp = ssh.open_sftp()

    compose_content = """services:
  keycloak-db:
    image: postgres:15-alpine
    container_name: keycloak-db
    restart: always
    security_opt:
      - seccomp:unconfined
    environment:
      POSTGRES_DB: keycloak
      POSTGRES_USER: keycloak
      POSTGRES_PASSWORD: KeycloakDbPass#2026
    volumes:
      - /opt/keycloak/data/postgres:/var/lib/postgresql/data:Z
    networks:
      - keycloak-net
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U keycloak -d keycloak"]
      interval: 5s
      timeout: 3s
      retries: 10

  keycloak:
    image: quay.io/keycloak/keycloak:26.1.3
    container_name: keycloak-server
    restart: always
    security_opt:
      - seccomp:unconfined
    depends_on:
      keycloak-db:
        condition: service_healthy
    command:
      - start-dev
    environment:
      KC_DB: postgres
      KC_DB_URL: jdbc:postgresql://keycloak-db:5432/keycloak
      KC_DB_USERNAME: keycloak
      KC_DB_PASSWORD: KeycloakDbPass#2026
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: Admin#Keycloak2026
      KC_HEALTH_ENABLED: "true"
      KC_METRICS_ENABLED: "true"
      KC_HTTP_ENABLED: "true"
      KC_HOSTNAME_STRICT: "false"
      JAVA_OPTS_APPEND: -Xms512m -Xmx768m -XX:MetaspaceSize=96M -XX:MaxMetaspaceSize=256M
    ports:
      - "8080:8080"
    volumes:
      - /opt/keycloak/providers:/opt/keycloak/providers:ro,Z
      - /opt/keycloak/realm-config:/opt/keycloak/data/import:ro,Z
    networks:
      - keycloak-net

networks:
  keycloak-net:
    driver: bridge
"""
    with sftp.file('/opt/keycloak/docker-compose.yml', 'w') as f:
        f.write(compose_content)
    sftp.close()

    print('Updated docker-compose.yml successfully.')

    stdin, stdout, stderr = ssh.exec_command('cd /opt/keycloak && docker compose up -d')
    print('Compose up stdout:', stdout.read().decode())
    print('Compose up stderr:', stderr.read().decode())

finally:
    ssh.close()
