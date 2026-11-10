# Keycloak 统一身份认证与单点登录（SSO）服务部署与运维手册

> **文档版本**：v1.9\
> **编写团队**：集成平台架构与运维团队  
> **最后修订日期**：2026年11月10日\
> **适用范围**：面向医院现场实施工程师、系统管理员、DevOps 运维团队

---

## 变更历史记录

| 版本 | 修订日期 | 修订人 | 修订说明 |
| :--- | :--- | :--- | :--- |
| **v1.0** | 2026-10-29 | 实施运维组 | 初始版本。完成专属服务器环境编排、Keycloak 26.x + PostgreSQL 15 容器化部署、用户联合存储 SPI 插件部署及 Realm `aip` 初始化配置。 |
| **v1.1** | 2026-10-30 | 实施运维组 | 增补定制化登录主题插件（`aip-keycloak-theme`）部署规范。包含单 JAR 介质发布规范、安全行为验证码服务联调、标题动态配置与中英双语国际化配置。 |
| **v1.2** | 2026-11-02 | 实施运维组 | **架构统一为方案 B 极简模式**：全面剔除开发源码细节与代码片段；前后端统一为方案 B（前端直接重定向 Keycloak + 前端落地页直接接收 Code 并换票），免去二次重定向与 Ticket 临时表；明确单点登录与验证码服务基于回调地址（`redirect_uri`）自动动态定位后端端口的免配置机制；升级登录主题为标准中英双语与下拉多语言菜单。 |
| **v1.3** | 2026-11-03 | 实施运维组 | **规范应用客户端接入与认证策略**：明确接入应用通过管理后台自动化同步 Keycloak Client，固化机密客户端模式（PKCE S256、授权码+刷新令牌），规范 Token 生命周期在 Realm 控制台全局管理。 |
| **v1.4** | 2026-11-06 | 实施运维组 | **验证码服务接入方式调整**：登录主题支持由平台统一配置验证码服务地址，子应用无需代理验证码接口。 |
| **v1.5** | 2026-11-06 | 实施运维组 | **发布局域网统一访问入口**：新增宿主机/虚拟机两级端口转发发布规范（对外 `http://<宿主机IP>:8082`），支撑局域网终端正常跳转与登录。 |
| **v1.6** | 2026-11-06 | 实施运维组 | **修复联邦用户标准声明缺失**：用户联合存储适配器补充暴露 `username`、`email`、`firstName` 用户属性，使 ID Token / UserInfo 正常返回 `preferred_username` 等标准 claim，解决子应用报 `Missing attribute 'preferred_username' in attributes` 的登录失败问题。 |
| **v1.7** | 2026-11-06 | 实施运维组 | **验证码内聚与滑块改造**：登录验证码由汉字点选改为拼图滑块，接口内聚到 Keycloak Provider JAR；新增服务端一次性票据校验，浏览器认证流需绑定 `aip-username-password-form` 执行项。 |
| **v1.8** | 2026-11-07 | 实施运维组 | **发布验证码安全修复版**：替换主题 Provider 并完成重启；更新有效登录会话要求、请求限流、升级后刷新登录页与现场验收说明。 |
| **v1.9** | 2026-11-10 | 实施运维组 | **子应用退出配置及验收规范**：平台统一维护退出回跳和后通道通知地址，补充网络可达性、账号切换及失败重试验收；不代表当前所有子应用已完成上线验收。 |

### 最近一次部署记录（2026-11-07）

- 目标：`192.168.43.100`，容器 `keycloak-server`，镜像 `quay.io/keycloak/keycloak:26.1.3`，启动命令 `start-dev`。
- 发布介质：`/opt/keycloak/providers/aip-keycloak-theme-1.0.0.jar`，大小 `2,796,178` 字节。
- SHA-256：`8ce9f8012ac20d01671d2e4051b382de08ff2c34e2da3e1a092ab81e19ca2850`，上传前后核对一致。
- 本次按操作方明确要求**未新建旧 JAR 备份**；常规发布仍建议按插件部署手册保留回滚介质。
- 执行结果：替换 JAR 后重启容器，自动重新增强成功；日志确认 Provider 注册、验证码素材加载和初始化成功，Realm 恢复 HTTP 200。
- 配置检查：Login theme 为 `aip-theme`，Browser Flow 为 `aip-browser`，自定义表单为 `REQUIRED`，标准表单未启用，SSO Cookie 分支保留；本次未修改 Realm/客户端配置。
- 实机冒烟：新脚本版本 `20261107a` 可访问；内网直连与局域网入口 `http://192.168.3.82:8082` 均可通过真实登录会话出图（HTTP 200）；无 Cookie、伪造 tabId、无效校验和重复提交被拒绝；不携带验证码票据直接提交登录返回安全验证失败。
- 验收边界：本次未使用真实业务账号执行成功登录，也未进行多节点测试；发布后应从门户重新进入登录页，完成人工滑块登录与 SSO 静默登录验收。


### 子应用配置联调记录（2026-11-10）

- 在平台应用管理保存 notice（APP-002），认证同步成功；只读核对 Keycloak 中的登录回调、退出回跳和通知地址与平台一致，未手工修改 Keycloak 或轮换密钥。
- 浏览器入口：`http://192.168.3.82:8092/admin`；登录回调：`http://192.168.3.82:8092/login/oauth2/code/aip`。
- 退出回跳：`http://192.168.3.82:8092/admin?loggedOut=1`；后通道通知：`http://192.168.43.1:8083/logout/connect/back-channel/aip`；要求通知携带会话标识。
- 从认证服务器访问通知地址可达，非法通知返回 400。子应用必须兼容 Keycloak 的 `logout+jwt` 通知类型；若返回类型校验失败，应更新子应用后重启再验收，不关闭签名或声明校验。
- 本条只确认配置下发及网络可达性；真实 A 退出、B 登录和各子应用会话失效验收尚待完成，不能据此宣称全链路验收成功。

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

### 2.1.1 局域网统一访问入口（Host-Only / NAT 虚拟机部署场景）

当 Keycloak 运行于 Host-Only / NAT 虚拟网络（如 `192.168.43.100`），局域网终端无法直接路由该地址时，必须由平台宿主机统一发布 Keycloak 服务：

1. **宿主机端口转发**（Windows 管理员命令提示符执行，对外端口示例为 `8082`）：
   ```bat
   netsh interface portproxy add v4tov4 listenaddress=0.0.0.0 listenport=8082 connectaddress=192.168.43.100 connectport=8082
   netsh advfirewall firewall add rule name="Keycloak-8082" dir=in action=allow protocol=TCP localport=8082
   ```
2. **虚拟机 firewalld 转发**（将外部 `8082` 转发至 Keycloak 容器 `8080`）：
   ```bash
   firewall-cmd --zone=public --add-port=8082/tcp --permanent
   firewall-cmd --zone=public --add-forward-port=port=8082:proto=tcp:toport=8080 --permanent
   firewall-cmd --reload
   ```
3. **对外统一地址**：`http://<宿主机局域网IP>:8082`。Keycloak 采用动态 Hostname（`KC_HOSTNAME_STRICT=false`），会自动以请求的 Host 生成 issuer、授权端点与 Token 端点。
4. **配套配置一致性**：前端 `VITE_APP_KEYCLOAK_URL` 与平台后端 `KEYCLOAK_SERVER_URL` 必须统一指向上述对外地址。
5. **验证方式**：访问 `http://<宿主机局域网IP>:8082/realms/aip/.well-known/openid-configuration`，返回的 `issuer` 应为该对外地址。

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

> **标准声明映射说明**：Keycloak 内置 `profile` / `email` Client Scope 的 Mapper 按**用户属性**读取 `username`、`email`、`firstName` 等字段。联邦存储适配器必须显式暴露这些属性，否则 ID Token / UserInfo 将缺失 `preferred_username`、`email` 等标准 claim，导致子应用（如 Spring Security 配置 `user-name-attribute=preferred_username`）在回调阶段报 `Missing attribute 'preferred_username' in attributes`。当前交付制品已内置该映射。

### 3.2 插件部署步骤
1. **获取交付制品**：获取发版包中的 `aip-keycloak-user-storage-1.0.0.jar`；
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
登录界面采用高可用单 JAR 插件形式交付（`aip-keycloak-theme-1.0.0.jar`），内部已封装设计规范模板、背景图、插画、SVG 图标、国际化字典、拼图滑块素材，以及 Keycloak Java Provider。无需在服务器散装解压，拷贝即用。

### 4.2 主题部署步骤
1. **获取交付制品**：获取发版包中的 `aip-keycloak-theme-1.0.0.jar`（含主题资源与 Java Provider，无需构建参数）；
2. **复制到插件目录**：将文件放置到宿主机 `/opt/keycloak/providers/`；
3. **重新增强并重启容器**：Java Provider 变更需要 Keycloak 重新构建增强，建议执行：
   ```bash
   docker exec keycloak-server /opt/keycloak/bin/kc.sh build
   docker restart keycloak-server
   ```
   若容器以 `start-dev` 启动，重启时也会重新增强；生产环境使用 `--optimized` 启动时必须先执行 `kc.sh build`。
4. **管理控制台启用主题**：
   * 登录 Keycloak 管理后台，切换到 Realm **`aip`**；
   * 左侧菜单点击 **`Realm settings`** -> 标签页切换到 **`Themes`**；
   * 将 **`Login theme`** 下拉框选择为 **`aip-theme`**；
   * 点击页面底部 **`Save`** 保存。

### 4.3 验证码接口部署说明

验证码接口随 Keycloak Provider JAR 交付，部署时无需配置外部验证码服务参数。接口信息如下：

| 项目 | 说明 |
| --- | --- |
| 获取接口 | `/realms/aip/aip-captcha/get`（`captchaType=blockPuzzle`） |
| 校验接口 | `/realms/aip/aip-captcha/check` |
| 素材与字体 | 滑块底图随 JAR 交付；Provider 跳过文字水印绘制，不依赖容器 fontconfig 或系统字体，也无需安装字体包 |
| 绘制依赖 | 不依赖 GPL 字体；Keycloak 精简容器无需额外安装 fontconfig |

实施要点：

1. 更换 JAR 后需重新增强并重启 Keycloak（见 4.2）；
2. 浏览器网络面板中验证码请求应指向 `/realms/aip/aip-captcha/...`；若请求失败，按第七部分排查；
3. 子应用只需配置自身 OIDC 回调地址。

### 4.4 浏览器认证流绑定（服务端强制校验）

仅部署 Provider JAR 还不会自动拦截登录表单，必须在 Realm 中绑定自定义认证执行项：

1. 登录 Keycloak 管理后台，进入 Realm `aip` 的 **Authentication** -> **Flows**；
2. 复制内置 **`browser`** 流为 **`aip-browser`**（保持顶层 `Cookie` 为 ALTERNATIVE 且位于第一位，`Kerberos`/`Identity Provider Redirector` 不变）；
3. 在 `aip-browser` 的 **forms** 子流中，将 **`Username Password Form`**（`auth-username-password-form`）替换为 **`AIP Username Password Form with CAPTCHA`**（`aip-username-password-form`），Requirements 设为 **REQUIRED**；`Conditional OTP` 保持 CONDITIONAL；
4. 在 **Authentication** -> **Required Actions / Bindings**（或 Realm 设置中的 Browser Flow）将 **Browser Flow** 绑定为 **`aip-browser`**；
5. 验证：
   - 使用错误/空验证码票据直接 POST 登录请求应被拒绝；
   - 正常拖动滑块通过后可完成登录；
   - SSO Cookie 命中时走 `Cookie` 分支，不弹出滑块。

> 注意：Realm 重新导入或重建后，需重新执行上述绑定；否则验证码仅显示、不参与服务端校验。
>
> 也可在部署 Provider JAR 后执行 `python configure_keycloak.py`，脚本会幂等地创建/绑定 `aip-browser` 流；若自定义执行项添加失败（例如 Provider 尚未加载），脚本会在绑定 Realm 之前中止，不会影响现有登录流。

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

### 5.3 拼图滑块验证码部署验证

部署完成后，打开登录页确认滑块底图可正常加载；直接提交登录请求且不携带 `captchaVerification` 时，应提示“安全验证未通过，请重新完成拼图”。详细部署与排障步骤见《aip-keycloak-theme 部署手册》。

---

## 第六部分：应用客户端接入与运维配置规范

集成平台已实现应用接入与 Keycloak Client 的自动化联动，实施与运维人员遵循以下规范进行日常维护：

### 6.1 客户端生命周期自动化管理
1. **自动化创建与同步**：
   在集成平台管理端添加或修改应用后，后台将通过管理接口自动在 Keycloak Realm `aip` 下同步创建或更新对应的 Client，无需运维人员手动在 Keycloak 管理后台逐个添加客户端；
2. **应用凭据管理规范**：
   - 客户端密钥由平台自动生成高强度密文并同步至 Keycloak；
   - 生产环境中**严禁直接在 Keycloak 控制台手动重置或修改业务客户端的 Secret**，否则会导致平台与 Keycloak 凭据脱节；如需重置密钥，必须通过集成平台管理端执行“重置密钥”，系统将自动双向同步；
3. **状态联动控制**：
   在平台管理端启用或停用应用时，下发 Keycloak 客户端状态。须确认“认证同步”为成功；同步失败时远端配置可能仍为旧值，修正网络或授权后在平台重试，不声称已立即生效。

### 6.2 OIDC 全局认证安全策略
平台预设了标准的安全认证策略，确保各子系统以最高安全标准接入：
* **认证方式**：统一采用机密客户端密钥认证（`client_secret_basic`）；
* **授权流程**：启用标准授权码流程（`authorization_code`）与刷新令牌（`refresh_token`），禁用不安全的直接密码与隐式流；
* **防篡改挑战**：强制开启 PKCE `S256` 挑战验证；
* **授权范围**：固定使用 `openid profile email`；
* **单点登出协同**：平台统一登记子应用的退出回跳地址及 Back-Channel Logout 通知地址并同步到 Keycloak。子应用必须实现通知校验和自身会话失效；不要求共享平台数据库或 Redis。已打开页面还需重新校验身份，重新登录后不得沿用旧账号数据。

### 6.3 统一控制台全局 Token 策略维护
全局令牌有效期不在各客户端分散配置，运维人员统一在 Realm 级别集中调优：
1. 登录 Keycloak 管理控制台，切换至 Realm **`aip`**；
2. 进入左侧菜单 **`Realm settings`** -> 点击 **`Tokens`** 标签页；
3. 根据医院安全等级要求调整以下核心参数：
   * **Access Token Lifespan**：访问令牌有效期（默认 30 分钟 / 1800 秒）；
   * **SSO Session Idle**：单点登录空闲超时（默认 30 分钟 / 1800 秒）；
   * **SSO Session Max**：单点登录最大存活时间（默认 10 小时 / 36000 秒）；
   * **Client Session Idle / Max**：客户端会话超时；
4. 点击页面底部 **`Save`** 保存即时生效。

### 6.4 子应用退出通知与账号切换现场验收

1. 在平台应用管理填写登录回调、退出后返回地址，以及子应用厂商提供的统一退出通知地址。保存后确认认证同步成功；失败时从平台重试，不逐应用手工修改 Keycloak。
2. 从 Keycloak 服务器网络验证通知地址可达。浏览器能访问前端不代表认证服务器能访问子应用后端；localhost 指向认证服务器自身，不能作为远程子应用地址。
3. notice 开发示例：浏览器入口 `http://192.168.3.82:8092/admin`；退出回跳 `http://192.168.3.82:8092/admin?loggedOut=1`；通知地址可使用 `http://192.168.43.1:8083/logout/connect/back-channel/aip`，以现场网络可达性为准。生产使用受控 HTTPS 地址。
4. 用账号 A 同时登录门户及子应用，保持子应用页面打开；在门户退出后，确认子应用旧会话不能继续操作，再在门户登录 B。切回子应用应重新确认身份并进入 B，不使用 A 的旧表单和权限。
5. 验证另一浏览器会话不被误切换，重复或延迟退出通知不影响后来建立的会话。通知丢失时需验证子应用的中心会话校验兜底，不能仅依据 JWT 未过期判定已登录。
6. 同 IP 不同端口的独立应用应隔离 Cookie 名称或路径；Cookie 不按端口隔离。认证服务不可达时应阻止无法确认身份的敏感操作。
7. 代码测试或模拟浏览器成功不等于现场链路验收完成。上线记录须包含真实账号、实际通知到达及会话失效检查；后台休眠、网络中断不承诺瞬时页面刷新。

**2026-11-10 开发联调记录（仅 notice / APP-002）**：退出地址已通过平台应用管理同步。实际 Keycloak 测试确认平台退出后子应用旧会话约 1.2～1.6 秒内失效，服务器日志记录已验证的后通道通知，中心会话检查返回 inactive；平台切换新账号后子应用无需重复输入密码，旧操作上下文被拒绝。子应用主动退出正确回到 8092 已退出页并使平台会话失效。未修改测试账号权限或轮换密钥，测试结束后注销本次会话。其他应用不能据此视为已验收。

退出通知被子应用以 HTTP 400 拒绝时，除网络配置外还要核对其接入组件是否支持当前 Keycloak 的通知格式；修复后须重新启动子应用并复测，不通过关闭签名、受众或时效校验绕过问题。

---

## 第七部分：现场实施常见故障排查手册

### 排查 1：滑块加载失败、请求 404/429 或登录提示“安全验证未通过”
* **排查方向**：
  1. 检查浏览器网络面板中验证码请求是否指向 `/realms/aip/aip-captcha/get` 与 `/check`；
  2. 确认 Keycloak Provider JAR 已放入 `/opt/keycloak/providers/`，且已执行 `kc.sh build` 并重启；
  3. 查看 Keycloak 启动日志中是否出现 `AJ-Captcha blockPuzzle engine initialized`；若初始化失败，会失败关闭并拒绝登录；
  4. 返回 429 表示触发会话或 IP 总量限流/锁定，等待窗口结束后重试，并检查代理是否正确转发真实客户端 IP；
  5. 返回 400 时确认请求来自有效登录页且带有当前会话 Cookie；自造 tabId 的裸请求会被拒绝，会话过期应从门户重新登录；
  6. 确认浏览器认证流已绑定 `aip-username-password-form`（见 4.4），并复测无验证码直接登录应被拒绝；
  7. 检查 Keycloak 代理/网关是否放行 `/realms/aip/aip-captcha/**` 路径。

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

### 排查 5：子应用登录回调报 `Missing attribute 'preferred_username' in attributes`
* **现象**：Keycloak 登录成功并携带 `code` 回调子应用后，后端抛出 `java.lang.IllegalArgumentException: Missing attribute 'preferred_username' in attributes`，随后跳回 `ssoError` 页面。
* **原因**：Keycloak `profile` Client Scope 的 `username` Mapper 读取的是用户**属性** `username`；联邦存储用户若未暴露该属性，ID Token / UserInfo 中就不会出现 `preferred_username`，而 Spring Security 配置了 `user-name-attribute: preferred_username`。
* **排查方向**：
  1. 解码 ID Token 或调用 `/protocol/openid-connect/userinfo`，确认是否缺少 `preferred_username`、`email` 等标准声明；
  2. 确认 `/opt/keycloak/providers/aip-keycloak-user-storage-*.jar` 为包含标准属性映射的版本（适配器需暴露 `username`、`email`、`firstName`）；
  3. 替换插件后执行 `docker restart keycloak-server` 使新适配器生效。

---

## 第八部分：日常巡检与维护指令速查

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
