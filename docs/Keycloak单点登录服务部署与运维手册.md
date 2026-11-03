# Keycloak 统一身份认证与单点登录（SSO）服务部署与运维手册

> **文档版本**：v1.2  
> **编写团队**：集成平台架构与运维团队  
> **最后修订日期**：2026年11月02日  
> **适用范围**：面向医院现场实施工程师、系统管理员、DevOps 运维团队

---

## 变更历史记录

| 版本 | 修订日期 | 修订人 | 修订说明 |
| :--- | :--- | :--- | :--- |
| **v1.0** | 2026-10-29 | 实施运维组 | 初始版本。完成专属服务器环境编排、Keycloak 26.x + PostgreSQL 15 容器化部署、用户联合存储 SPI 插件部署及 Realm `aip` 初始化配置。 |
| **v1.1** | 2026-10-30 | 实施运维组 | 增补定制化登录主题插件（`aip-keycloak-theme`）部署规范。包含单 JAR 介质发布规范、安全行为验证码服务联调、标题动态配置与中英双语国际化配置。 |
| **v1.2** | 2026-11-02 | 实施运维组 | **架构统一为方案 B 极简模式**：全面剔除开发源码细节与代码片段；前后端统一为方案 B（前端直接重定向 Keycloak + 前端落地页直接接收 Code 并换票），免去二次重定向与 Ticket 临时表；明确单点登录与验证码服务基于回调地址（`redirect_uri`）自动动态定位后端端口的免配置机制；升级登录主题为标准中英双语与下拉多语言菜单。 |

---

## 第一部分：部署架构与服务节点规划

### 1.1 总体架构拓扑

```
                           [ 医院局域网终端浏览器 ]
                                     │
           ┌─────────────────────────┴─────────────────────────┐
           │ 访问门户前端                                      │ 统一认证跳转
           ▼                                                   ▼
┌───────────────────────────────┐                  ┌───────────────────────────────┐
│       集成平台宿主机节点      │                  │    Keycloak 专属认证节点      │
│                               │                  │                               │
│  ┌─────────────────────────┐  │                  │  ┌─────────────────────────┐  │
│  │ 门户前端系统 (plus-ui)  │  │                  │  │ Keycloak 26.1.3 容器    │  │
│  │ (HTTP 访问入口)         │  │                  │  │                         │  │
│  └─────────────────────────┘  │                  │  │ [插件挂载目录 providers]│  │
│                               │                  │  │  ├─ user-storage.jar    │  │
│  ┌─────────────────────────┐  │ 用户密码联合鉴权 │  │  └─ aip-theme.jar       │  │
│  │ 平台后端服务 (aip-admin)│◄─┼──────────────────┼──┤                         │  │
│  │ - 账号密码校验接口      │  │ (HTTP 内网直连)  │  └────────────┬────────────┘  │
│  │ - 行为验证码服务        │  │                  │               │ 内部通信 (JDBC)
│  └─────────────────────────┘  │                  │  ┌────────────▼────────────┐  │
│                               │                  │  │ PostgreSQL 15 容器      │  │
│  ┌─────────────────────────┐  │                  │  │ (Keycloak 元数据库)     │  │
│  │ PostgreSQL / Redis 基础 │  │                  │  └─────────────────────────┘  │
│  └─────────────────────────┘  │                  │                               │
└───────────────────────────────┘                  └───────────────────────────────┘
```

### 1.2 节点与服务信息一览表

> ⚠️ **高危安全提示**：生产环境部署交付后，请务必第一时间通过管理后台修改超级管理员密码及数据库密码。

| 服务角色 | 部署形式 | 默认访问地址 / 端口 | 默认账号 | 默认密码 / 密钥 | 备注说明 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Linux 宿主机** | 操作系统 | 专属认证服务器 IP | `root` | 现场初始化密码 | CentOS 7.9+ 64位，内存 ≥ 2G |
| **Keycloak 管理台** | Docker 容器 | `http://<服务器IP>:<服务端口>/admin/` | `admin` | `Admin#Keycloak2026` | Master Realm 超级管理员 |
| **Keycloak 数据库** | Docker 容器 | 容器内部 `keycloak-db:5432` | `keycloak` | `KeycloakDbPass#2026` | 数据库名 `keycloak` |
| **业务 Realm 标识** | Keycloak 配置 | `aip` | - | - | 集成平台核心认证 Realm |
| **门户认证客户端** | Keycloak Client | `000000` | `000000` | `aip-portal-secret-2026` | 授权码流程 (方案 B：前端直跳 + Code 直接换发 Sa-Token) |
| **管理服务客户端** | Keycloak Client | `aip-admin-svc` | `aip-admin-svc` | `aip-admin-svc-secret-2026` | 凭证式，授予客户端管理角色 |
| **内部接口通信密钥** | HTTP Header | `X-Internal-Secret` | - | `aip-sso-internal-secret-token` | 保护平台内部用户与验密接口 |

---

## 第二部分：Keycloak 容器化安装与运行配置

### 2.1 宿主机环境前置准备

1. **安装容器环境**：确认宿主机已安装 Docker CE 24.0+ 与 Docker Compose v2.20+。
2. **开放网络防火墙端口**：
   ```bash
   # 开放 Keycloak 服务访问端口（默认 8080，如修改请按实际填写）
   firewall-cmd --zone=public --add-port=8080/tcp --permanent
   firewall-cmd --reload
   ```
3. **创建运行目录结构**：
   ```bash
   mkdir -p /opt/keycloak/data/postgres
   mkdir -p /opt/keycloak/providers
   ```

### 2.2 编排部署配置（`docker-compose.yml`）

在 `/opt/keycloak/docker-compose.yml` 中放置容器编排配置：

```yaml
version: '3.8'

services:
  keycloak-db:
    image: postgres:15-alpine
    container_name: keycloak-db
    restart: always
    environment:
      POSTGRES_DB: keycloak
      POSTGRES_USER: keycloak
      POSTGRES_PASSWORD: KeycloakDbPass#2026
    volumes:
      - /opt/keycloak/data/postgres:/var/lib/postgresql/data:Z
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U keycloak -d keycloak"]
      interval: 5s
      timeout: 5s
      retries: 10
    networks:
      - keycloak-net

  keycloak-server:
    image: quay.io/keycloak/keycloak:26.1.3
    container_name: keycloak-server
    restart: always
    command:
      - start-dev
      - --http-port=8080
      - --proxy-headers=xforwarded
    environment:
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: Admin#Keycloak2026
      KC_DB: postgres
      KC_DB_URL: jdbc:postgresql://keycloak-db:5432/keycloak
      KC_DB_USERNAME: keycloak
      KC_DB_PASSWORD: KeycloakDbPass#2026
      KC_HTTP_ENABLED: "true"
      KC_HOSTNAME_STRICT: "false"
      # JVM 堆内存限制：防止物理内存耗尽触发 OOM Killer
      JAVA_OPTS_APPEND: "-Xms512m -Xmx768m -XX:MetaspaceSize=96M -XX:MaxMetaspaceSize=256M"
    volumes:
      # 插件与主题制品挂载目录
      - /opt/keycloak/providers:/opt/keycloak/providers:ro,Z
    ports:
      - "8080:8080"
    depends_on:
      keycloak-db:
        condition: service_healthy
    networks:
      - keycloak-net

networks:
  keycloak-net:
    driver: bridge
```

### 2.3 容器启动与验证
```bash
cd /opt/keycloak
docker compose up -d

# 检查容器状态（应为 Up 状态）
docker compose ps
```

---

## 第三部分：用户联合存储 SPI 插件部署

### 3.1 插件功能说明
平台所有用户数据与密码存放在集成平台主数据库中，Keycloak 本地不保存密码哈希。通过在 Keycloak 安装用户联合存储插件（User Storage Provider），当用户登录时，由 Keycloak 实时回连平台后端完成账号状态与密码校验。

### 3.2 插件部署步骤
1. **获取交付制品**：获取发版包中的 `aip-keycloak-user-storage-5.5.2.jar`；
2. **复制到插件目录**：将文件放置到宿主机 `/opt/keycloak/providers/`；
3. **重启容器生效**：
   ```bash
   docker restart keycloak-server
   ```
4. **管理控制台配置**：
   * 浏览器访问 `http://<服务器IP>:<端口>/admin`，登录超级管理员；
   * 左上角切换 Realm 为 **`aip`**；
   * 左侧菜单进入 **User federation** -> 点击 **Add provider** -> 选择 **`aip-user-storage`**；
   * 填写配置参数：
     * **Platform Internal Base URL**：填写平台后端服务实际内网地址与端口（如 `http://192.168.43.1:8080`）；
     * **Internal Shared Secret**：填写双方约定一致的内部密钥（默认 `aip-sso-internal-secret-token`）；
     * **Cache Policy**：设置为 **`NO_CACHE`**（确保用户改密、封禁等操作即时生效）；
   * 点击 **Save** 保存。

---

## 第四部分：定制化登录主题插件部署

### 4.1 主题功能与交付形式
登录界面采用高可用单 JAR 插件形式交付（`aip-keycloak-theme-5.5.2.jar`），内部已预先封装设计规范模板、背景图、插画、SVG 图标、国际化字典及安全行为验证码组件。无需在服务器散装解压，拷贝即用。

### 4.2 主题部署步骤
1. **获取交付制品**：获取发版包中的 `aip-keycloak-theme-5.5.2.jar`；
2. **复制到插件目录**：将文件放置到宿主机 `/opt/keycloak/providers/`；
3. **重启容器热加载**：
   ```bash
   docker restart keycloak-server
   ```
4. **管理控制台启用主题**：
   * 登录 Keycloak 管理后台，切换到 Realm **`aip`**；
   * 左侧菜单点击 **`Realm settings`** -> 标签页切换到 **`Themes`**；
   * 将 **`Login theme`** 下拉框选择为 **`aip-theme`**；
   * 点击页面底部 **`Save`** 保存。

---

## 第五部分：主题业务特性与实施运维配置

### 5.1 登录页系统标题配置与多语言表现
登录界面卡片标题与浏览器标签页已实现标准化中英自适应：
* **默认显示效果**：
  * 中文环境下自动显示：**`Keycloak登录系统`**
  * 英文环境下自动显示：**`Keycloak Login System`**
* **零代码个性化配置（若客户有自定义系统名称需求）**：
  无需重新打包介质，直接在管理控制台按需配置覆盖：
  1. 进入 **`Realm settings`** -> **`Localization`** -> **`Realm-specific overrides`**；
  2. 点击 **Add localization text**：
     * 中文名称：Locale 选择 `zh-CN`，Key 填 `loginTitle`，Value 输入自定义系统名称（如 `XX医院统一身份认证平台`）；
     * 英文名称：Locale 选择 `en`，Key 填 `loginTitle`，Value 输入英文名称；
  3. 点击 **Save** 保存，刷新登录页面立即生效。

### 5.2 开启中英双语国际化切换菜单
1. 登录管理员进入 **`Realm settings`** -> 切换到 **`Localization`** 标签页；
2. 将 **`Internationalization`** 开关设置为 **`ON`**；
3. **`Supported locales`** 中添加：`zh-CN`（简体中文）与 `en`（English）；
4. **`Default locale`** 选择：`zh-CN`；
5. 点击 **Save** 保存。登录页右上角会自动渲染支持即时切换的中英文下拉菜单。

### 5.3 行为验证码服务与后端端口定位机制（免配置规则）
系统在用户点击登录时调用平台后端的汉字点选人机验证码服务：
* **免配置原则**：主题内部完全解耦了后端服务地址，**严禁且无需在主题中硬编码任何主机 IP 或端口**；
* **定位规则**：
  前端应用（门户）在发起 SSO 单点登录跳转时，已在跳转链接中规范携带了认证回调地址（`redirect_uri` / `client_data`）；登录主题在弹出验证码时，**自动从该回调地址中提取实际的协议、主机 IP 与端口**，直接向提供该回调的后端服务请求验证码；
* **实施注意**：
  无论现场部署在标准端口、多端口映射、容器集群还是经由 Nginx 统一反向代理，实施人员仅需确保在平台系统配置中正确配置了门户回调地址，无需对 Keycloak 登录主题进行任何网络端口的二次改动。

---

## 第六部分：现场实施常见故障排查手册

### 排查 1：验证码弹出提示“网络连接失败”或请求返回 404
* **排查方向**：
  1. 检查浏览器网络面板中验证码请求的 Target URL，确认请求的 Host 和 Port 是否与集成平台后端服务一致；
  2. 确认平台后端服务（`aip-admin`）已正常运行且网络连通；
  3. 检查平台后端防火墙是否放行了后端服务端口，并且后端网关/过滤器已对 `/captcha/**` 接口开启免鉴权访问。

### 排查 2：用户输入正确账号密码提示“Invalid username or password”
* **排查方向**：
  1. 登录 Keycloak 管理台，进入 **User federation** -> 查看 `aip-user-storage` 状态；
  2. 确认 **Platform Internal Base URL** 是否能够从 Keycloak 容器内正常连通后端（可通过 `docker exec -it keycloak-server curl <后端地址>/internal/keycloak/...` 进行排查）；
  3. 确认 **Internal Shared Secret** 与平台后端 `application.yml` 中的 `keycloak.internal-secret` 配置严格一致；
  4. 查看平台后端服务日志，检索 `/internal/keycloak/verify-password` 是否收到验密请求及返回的错误原因。

### 排查 3：Keycloak 容器启动频繁重启或退出（OOM 退出）
* **排查方向**：
  1. 执行 `docker inspect keycloak-server --format='{{.State.OOMKilled}}'`，若输出 `true`，说明物理机可用内存不足触发 Linux 系统的 OOM Killer；
  2. 检查 `docker-compose.yml` 中的 `JAVA_OPTS_APPEND`，确保 JVM 堆内存限制（`-Xmx768m`）与容器内存相适应；
  3. 在宿主机上配置并开启 2G 以上的 Swap 交换分区缓冲瞬时内存峰值。

### 排查 4：修改了客户端标题或多语言后页面未发生变化
* **排查方向**：
  1. 确认在管理控制台修改后已点击页面下方的 **Save** 按钮；
  2. 按 `Ctrl + F5` 强制刷新浏览器清除前端静态页面缓存。

---

## 第七部分：日常巡检与维护指令速查

```bash
# 1. 检查 Keycloak 与 PostgreSQL 容器运行健康度
docker ps --filter "name=keycloak"

# 2. 实时查看 Keycloak 运行与认证日志
docker logs -f keycloak-server --tail 100

# 3. 查看容器当前内存与 CPU 资源占用情况
docker stats keycloak-server keycloak-db --no-stream

# 4. 更新插件制品后重启加载
docker restart keycloak-server

# 5. 备份 Keycloak 业务数据库
docker exec -t keycloak-db pg_dump -U keycloak keycloak > /opt/keycloak/backup_$(date +%Y%m%d).sql
```
