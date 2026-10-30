# Keycloak 统一身份认证与单点登录（SSO）服务部署与运维手册

> **文档版本**：v1.1  
> **编写团队**：集成平台架构与研发团队  
> **最后修订日期**：2026年10月30日  
> **适用范围**：面向医院现场实施工程师、DevOps 运维团队、集成平台技术支持团队

---

## 变更历史记录

| 版本 | 修订日期 | 修订人 | 修订说明 |
| :--- | :--- | :--- | :--- |
| **v1.0** | 2026-10-29 | Michelle.Chung | 初始版本。完成专属服务器环境编排、Keycloak 26.x + PostgreSQL 15 容器化部署、User Storage SPI（`aip-keycloak-user-storage`）插件集成及 Realm `aip` 初始化配置。 |
| **v1.1** | 2026-10-30 | Michelle.Chung | **增补定制化登录主题（`aip-keycloak-theme`）部署章节**。包含单 JAR 插件化交付规范、Vue 登录页视觉还原与响应式适配、AJ-Captcha 汉字点选验证码服务配置、标题两级动态配置机制及中英双语国际化（i18n）落地指南；新增排障指南第 11、12 条。 |

---

## 第一部分：部署架构与核心节点规划

### 1.1 总体架构拓扑

```
                           [ 医院局域网终端浏览器 ]
                                     │
           ┌─────────────────────────┴─────────────────────────┐
           │ 访问门户前端                                      │ 统一认证跳转
           ▼ (HTTP :9090)                                      ▼ (HTTP :8080)
┌───────────────────────────────┐                  ┌───────────────────────────────┐
│     集成平台宿主机节点        │                  │    Keycloak 专属认证节点      │
│   (IP: 192.168.3.82/43.1)     │                  │      (IP: 192.168.43.100)     │
│                               │                  │                               │
│  ┌─────────────────────────┐  │                  │  ┌─────────────────────────┐  │
│  │ plus-ui (前端门户)      │  │                  │  │ Keycloak 26.1.3 容器    │  │
│  │ 端口: 9090              │  │                  │  │ (Quarkus 架构 / 端口8080) │  │
│  └─────────────────────────┘  │                  │  │                         │  │
│                               │                  │  │ [Providers 挂载目录]:   │  │
│  ┌─────────────────────────┐  │ SPI 内部联合认证 │  │  ├─ user-storage.jar    │  │
│  │ aip-admin (平台后端服务)│◄─┼──────────────────┼──┤  └─ aip-theme.jar (新增)│  │
│  │ 端口: 8080              │  │ (HTTP :8080)     │  │                         │  │
│  │ - /internal/keycloak/** │  │                  │  └────────────┬────────────┘  │
│  │ - /captcha/** (AJ验证码)│  │                  │               │ JDBC (5432)   │
│  └─────────────────────────┘  │                  │  ┌────────────▼────────────┐  │
│                               │                  │  │ PostgreSQL 15 容器      │  │
│  ┌─────────────────────────┐  │                  │  │ (keycloak 元数据库)     │  │
│  │ PostgreSQL / Redis 服务 │  │                  │  └─────────────────────────┘  │
│  └─────────────────────────┘  │                  │                               │
└───────────────────────────────┘                  └───────────────────────────────┘
```

### 1.2 主机节点与核心账号密码清单

> ⚠️ **高危安全提示**：生产环境部署交付后，请第一时间通过管理后台修改超级管理员密码及数据库密码，并同步更新配置。

| 组件角色 | 部署宿主机 / 容器 | IP / 访问地址 | 默认账号 | 默认密码 / 密钥 | 备注说明 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Linux 宿主机** | CentOS 7 x86_64 | `192.168.43.100` | `root` | `Dino#1!` | SSH 端口 22，内存 1.8G + Swap 2G |
| **Keycloak 管理台** | Docker 容器 | `http://192.168.43.100:8080/admin/` | `admin` | `Admin#Keycloak2026` | Master Realm 超级管理员 |
| **Keycloak 元数据库** | PostgreSQL 15 容器 | `192.168.43.100:5432` | `keycloak` | `KeycloakDbPass#2026` | 数据库名 `keycloak` |
| **业务 Realm 标识** | Keycloak Realm | `aip` | - | - | 集成平台核心认证 Realm |
| **门户 OIDC Client** | Keycloak Client | `000000` | `000000` | `aip-portal-secret-2026` | 授权码流程，开启 PKCE S256 |
| **管理 Service Account** | Keycloak Client | `aip-admin-svc` | `aip-admin-svc` | `aip-admin-svc-secret-2026` | 凭证式，授予客户端管理角色 |
| **SPI 内部通信密钥** | Header 共享密钥 | `X-Internal-Secret` | - | `aip-sso-internal-secret-token` | 保护平台内部查用户与验密接口 |

---

## 第二部分：Keycloak 容器化安装与运行配置

### 2.1 宿主机环境依赖

* **操作系统**：CentOS 7.9 64位 或更高版本 Linux。
* **Docker 引擎**：Docker CE 24.0+，Docker Compose v2.20+。
* **网络与防火墙**：
  ```bash
  # 开放 8080/tcp (Keycloak 访问) 与 5432/tcp (PostgreSQL，可选仅内网)
  firewall-cmd --zone=public --add-port=8080/tcp --permanent
  firewall-cmd --reload
  ```

### 2.2 编排文件配置（`docker-compose.yml`）

在服务器 `/opt/keycloak` 目录下编排并运行服务：

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
      # Providers 插件挂载目录（包含 User Storage SPI 与 Theme JAR）
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

---

## 第三部分：用户联合存储 SPI 插件部署（`aip-keycloak-user-storage`）

### 3.1 插件功能说明
平台密码只保留在业务库 `sys_user` 中，Keycloak 不保存用户密码哈希。通过基于 Keycloak 26.x（Quarkus 架构）的 **User Storage SPI**，在用户登录时调用集成平台内部接口 `/internal/keycloak/verify-password` 完成 BCrypt 与防暴力破解连续重试锁定的统一鉴权。

### 3.2 编译打包与部署
1. **打包命令**：
   ```bash
   mvn clean package -pl aip-extend/aip-keycloak-user-storage -DskipTests
   ```
2. **部署产物**：将生成的 `aip-keycloak-user-storage-5.5.2.jar` 上传至服务器目录：
   ```text
   /opt/keycloak/providers/aip-keycloak-user-storage-5.5.2.jar
   ```
3. **Quarkus 自动装配**：重启 `keycloak-server` 容器后，Keycloak 在启动日志中会自动完成 Augmentation 构建：
   ```text
   Updating the configuration and installing your custom providers, if any. Please wait.
   Quarkus augmentation completed in ...
   ```
4. **管理台配置 User Federation**：
   * 登录 `http://192.168.43.100:8080/admin` -> 进入 Realm `aip`；
   * 点击 **User federation** -> **Add provider** -> 选择 **`aip-user-storage`**；
   * 配置：
     * **Platform Internal Base URL**：`http://192.168.43.1:8080`（指向宿主机后端内网地址）；
     * **Internal Shared Secret**：`aip-sso-internal-secret-token`；
     * **Cache Policy**：`NO_CACHE`（确保改密与禁用账号实时生效）。

---

## 第四部分：定制化登录主题插件部署（`aip-keycloak-theme`）【v1.1新增】

### 4.1 架构设计与交付标准
为保证企业级交付规范、杜绝散装静态文件丢失或 Linux 权限错乱，定制登录页采用 **单 JAR 包（Theme Provider SPI）容器化交付模式**：
* **模块工程**：`aip-extend/aip-keycloak-theme`
* **资源封装**：HTML 模板（`login.ftl`）、样式表（`login.css` / `captcha.css`）、前端背景大图、业务插画、高保真 SVG 图标以及行为验证码脚本（`captcha.js` / `crypto-js.min.js`）**全部打包进单个 JAR 文件内部**。

```
aip-keycloak-theme-5.5.2.jar
├── META-INF/
│   └── keycloak-themes.json                     # 声明主题标识 aip-theme
└── theme/
    └── aip-theme/
        └── login/
            ├── theme.properties                 # 主题元数据（父主题、CSS与脚本声明、支持语言）
            ├── login.ftl                        # 原生 FreeMarker 登录页模板
            ├── messages/
            │   ├── messages_zh-CN.properties    # 中文国际化资源字典
            │   └── messages_en.properties       # 英文国际化资源字典
            └── resources/
                ├── css/                         # 登录与验证码样式
                ├── js/                          # 验证码交互与加密库
                ├── img/                         # 背景大图与插画
                └── icons/                       # 矢量操作图标
```

### 4.2 编译打包指南
在代码工程根目录下执行 Maven 打包命令：
```bash
mvn clean package -pl aip-extend/aip-keycloak-theme -DskipTests
```
构建成功后产物输出路径为：
```text
aip-extend/aip-keycloak-theme/target/aip-keycloak-theme-5.5.2.jar
```

### 4.3 服务器部署与热加载
1. **上传文件**：通过 SFTP / SCP 将产物上传至 Keycloak 的插件挂载目录：
   ```bash
   scp aip-keycloak-theme-5.5.2.jar root@192.168.43.100:/opt/keycloak/providers/
   ```
2. **重启容器生效**：
   ```bash
   docker restart keycloak-server
   ```
   > 容器启动时会自动检测到新的主题提供者，并打印 `Updating the configuration and installing your custom providers`，启动完成后即被内核注册就绪。

### 4.4 启用定制化主题（Login Theme）

有两种方式将 Realm `aip` 的登录主题切换为 `aip-theme`：

#### 途径 A：管理控制台图形化配置（推荐）
1. 浏览器打开 `http://192.168.43.100:8080/admin`，登录超级管理员；
2. 左上角 Realm 下拉选择 **`aip`**；
3. 左侧导航进入 **`Realm settings`** -> 切换到 **`Themes`** 标签页；
4. 将 **`Login theme`** 下拉框选择为 **`aip-theme`**；
5. 点击页面底部 **`Save`** 保存。

#### 途径 B：Admin REST API 命令行一键配置
```bash
# 1. 获取 Master 管理员 Token
ADMIN_TOKEN=$(curl -s -d "client_id=admin-cli" -d "username=admin" -d "password=Admin#Keycloak2026" -d "grant_type=password" \
  http://192.168.43.100:8080/realms/master/protocol/openid-connect/token | jq -r '.access_token')

# 2. 修改 aip realm 的 loginTheme
curl -X PUT -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"loginTheme": "aip-theme"}' \
  http://192.168.43.100:8080/admin/realms/aip
```

---

## 第五部分：主题业务特性与高级运维配置

### 5.1 登录页系统标题两级动态配置机制

无需修改模板代码或重新打包，可在 Keycloak 后台按需自定义登录卡片与浏览器标签页标题：

```
                    ┌───────────────────────────────┐
                    │  客户端独立标题 (Client Name)  │ (优先读取)
                    └───────────────┬───────────────┘
                                    │ 为空时回退
                                    ▼
                    ┌───────────────────────────────┐
                    │ 全局认证标题 (Realm Display)  │
                    └───────────────┬───────────────┘
                                    │ 为空时兜底
                                    ▼
                    ┌───────────────────────────────┐
                    │   默认值: "Keycloak登录系统"   │
                    └───────────────────────────────┘
```

* **配置客户端独立标题**（推荐用于区分门户与各子应用）：
  * **操作路径**：进入 **`Clients`** -> 点击目标客户端（如 `000000`）-> **`Client details`**；
  * **配置项**：在 **`Name`** 字段中填入想要显示的名称（如 `拜耳数字化集成平台` 或 `供应链协同系统统一认证`）；
  * **保存即生效**：点击 **`Save`**，刷新登录页面立即生效。
* **配置全局 Realm 默认标题**：
  * **操作路径**：进入 **`Realm settings`** -> **`General`** -> 修改 **`Display name`** 字段并保存。

### 5.2 多语言国际化（i18n）配置与后台动态词条覆盖

主题原生支持中英文自动适配及用户手动切换：

#### 1. 开启 Realm 国际化
1. 进入 **`Realm settings`** -> 切换到 **`Localization`** 标签页；
2. 打开 **`Internationalization`** 开关（ON）；
3. **`Supported locales`** 添加：`zh-CN`（简体中文）与 `en`（英语）；
4. **`Default locale`** 选择：`zh-CN`；
5. 点击 **`Save`**。此时登录页右上角会自动渲染中英语言切换下拉框！

#### 2. 在线定制中英文翻译词条（覆盖默认文案）
在 **`Localization`** -> **`Realm-specific overrides`** 中，可直接在浏览器界面上增加中英翻译映射，无需触碰服务器代码：
* **第一步**：添加翻译词条：
  * Locale: `zh-CN` | Key: `portalTitle` | Value: `拜耳数字化集成平台`
  * Locale: `en` | Key: `portalTitle` | Value: `Bayer Digital Integration Platform`
* **第二步**：在客户端配置里引用该词条：
  * 进入 Client `000000` 的 **`Name`** 字段，输入格式：
    ```text
    ${portalTitle}
    ```
  * 保存后，用户在中文模式下看到 `拜耳数字化集成平台`，点击切换为 English 时自动变更为 `Bayer Digital Integration Platform`。

### 5.3 AJ-Captcha 汉字点选行为验证码联动

为保障高安全性，登录页集成了汉字点选行为验证码（clickWord）：
1. **交互逻辑**：
   * 用户输入账号密码点击「立即登录」时，前端 `captcha.js` 自动拦截表单提交；
   * 异步调用平台后端接口获取汉字点选底图（分辨率 310×155）；
   * 用户按序点击汉字并点击「确定」；
   * 前端使用 AES-ECB 对坐标点密文加密并调用 `/captcha/check` 完成人机校验；
   * 校验通过后回填 `captchaVerification` 凭证并真正提交给 Keycloak 登录。
2. **跨域与路由（CORS）**：
   * 后端 `AjcaptchaController` 与 Spring Security 必须放行 `/captcha/**` 接口；
   * `captcha.js` 具备智能动态基地址探测：优先提取当前认证 URL 中的 `redirect_uri` 作为后端地址（如 `http://192.168.3.82:8080`），彻底自适应不同局域网 IP 与生产域名。

---

## 第六部分：生产部署真实踩坑与故障排障手册（新增至12项）

### 踩坑 11：汉字点选验证码报错 `this.clickWordFont is null`（空指针异常）
* **故障现象**：点击登录弹出验证码时，界面提示 `获取验证码失败: Cannot invoke "java.awt.Font.deriveFont(java.awt.geom.AffineTransform)" because "this.clickWordFont" is null`。
* **根本原因**：
  * AJ-Captcha 官方依赖 `com.anji-plus:captcha:1.3.0` 源码中，点选验证码默认寻找 `SourceHanSansCN-Normal.otf` 字体；
  * 但该依赖 JAR 包内部**根本没有打包该字体**，而是包含了 `fonts/WenQuanZhengHei.ttf`（文泉驿正黑）；
  * 类库加载字体抛出未显式中断的异常导致 `clickWordFont` 为 `null`，后续计算文字旋转角度时报空指针。
* **解决方案**：
  1. 将 `WenQuanZhengHei.ttf` 提取并固化至 `aip-common-ajcaptcha/src/main/resources/fonts/WenQuanZhengHei.ttf`；
  2. 在 `AjCaptchaConfig.java` 初始化中显式指定：
     ```java
     Properties clickConfig = new Properties();
     clickConfig.put(Const.CAPTCHA_TYPE, CaptchaTypeEnum.CLICKWORD.getCodeValue());
     clickConfig.put(Const.CAPTCHA_FONT_TYPE, "WenQuanZhengHei.ttf");
     CaptchaServiceFactory.getInstance(clickConfig);
     ```
  3. 提取 17 张官方点选背景图至 `classpath:captcha/pic-click/*.png` 并执行预加载。

---

### 踩坑 12：国际化未开启时 FreeMarker 模板渲染报 500（`locale` 变量缺失）
* **故障现象**：访问登录页面时 Keycloak 报 `HTTP 500 Internal Server Error`，后台日志为：
  ```text
  freemarker.core.InvalidReferenceException: The following has evaluated to null or missing: ==> locale [in template "login.ftl"]
  ```
* **根本原因**：
  * 在 Keycloak Realm 未开启 `Internationalization` 时，FreeMarker 模板上下文中**不会注入 `locale` 变量**；
  * 直接书写 `locale.currentLanguageTag` 会因为一级对象 `locale` 为 null 抛出未捕获异常。
* **解决方案**：
  在 FreeMarker 模板中必须对外层变量添加括号级保护：
  ```ftl
  <#-- 规范写法：支持 locale 缺失时的安全兜底 -->
  <html lang="${((locale.currentLanguageTag)!'zh-CN')}">
  <#if (realm.internationalizationEnabled?? && realm.internationalizationEnabled && (locale.supported)?? && (locale.supported?size > 1))>
      ...
  </#if>
  ```

---

## 第七部分：日常巡检与维护指令速查

```bash
# 1. 检查 Keycloak 与 PostgreSQL 容器运行健康度
docker ps --filter "name=keycloak"

# 2. 实时查看 Keycloak 运行与认证日志
docker logs -f keycloak-server --tail 100

# 3. 查看容器内存占用情况
docker stats keycloak-server keycloak-db --no-stream

# 4. 重新构建/热加载 Providers 插件（更新 SPI 或 Theme 后执行）
docker restart keycloak-server

# 5. 备份 Keycloak 业务数据库
docker exec -t keycloak-db pg_dump -U keycloak keycloak > /opt/keycloak/backup_$(date +%Y%m%d).sql
```
