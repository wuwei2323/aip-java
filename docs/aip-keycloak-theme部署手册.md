# aip-keycloak-theme 插件部署手册

> 适用范围：Keycloak 26.x（现场基准 26.1.3）、Realm `aip`。
> 本手册仅覆盖 `aip-extend/aip-keycloak-theme` 插件自身的构建、部署、主题启用、认证流绑定、验证与回滚；全局 SSO 环境（数据库、用户联合存储、OIDC 客户端、端口发布等）请参阅《Keycloak 单点登录服务部署与运维手册》。

## 一、文档信息

| 项目 | 内容 |
|---|---|
| 文档版本 | v1.1 |
| 更新日期 | 2026-11-07 |
| 适用插件 | `aip-keycloak-theme-1.0.0.jar` |
| 插件性质 | 登录主题资源 + Keycloak Java Provider（SPI） |
| 目标 Keycloak 版本 | 26.1.3（其他 26.x 版本需重新编译并回归） |
| 部署目录 | `/opt/keycloak/providers/`（**不能放 `themes/`**） |
| 部署形态 | 单 JAR Provider 插件（主题资源 + Keycloak SPI） |

## 二、部署影响

- JAR 为 Keycloak Provider 插件，必须放入 `/opt/keycloak/providers/`，不能放入 `themes/`。
- 更新 Java Provider 后，按启动方式重新增强并重启（见 5.3）。
- 部署后必须在 Realm 中绑定 `aip-browser` 认证流（见第七节），否则验证码不参与服务端校验。

## 三、构建与交付物

### 3.1 构建环境

| 项目 | 要求 |
|---|---|
| JDK | 17 |
| Maven | 3.9+ |
| 仓库 | 能访问 Maven 中央仓库或企业私服（需解析 Keycloak 26.1.3 与 AJ-Captcha 1.3.0 依赖） |

### 3.2 本地构建

```bash
cd <项目根目录>
mvn -f aip-extend/aip-keycloak-theme/pom.xml clean package -DskipTests=false -Dgroups=dev
node --test aip-extend/aip-keycloak-theme/src/test/js/captcha.test.cjs
```

构建成功标志：

```text
Tests run: 26, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

前端回归测试应为 `11` 项通过、`0` 失败；执行需要 Node.js 18+，无需安装 npm 依赖。

交付物：

```text
aip-extend/aip-keycloak-theme/target/aip-keycloak-theme-1.0.0.jar
```

### 3.3 交付物自检

```bash
# 文件大小（通常约 2.7 MB）
ls -lh aip-extend/aip-keycloak-theme/target/aip-keycloak-theme-1.0.0.jar

# SHA-256（用于上传后一致性校验）
sha256sum aip-extend/aip-keycloak-theme/target/aip-keycloak-theme-1.0.0.jar
# Windows 可用：certutil -hashfile <jar路径> SHA256
```

JAR 内容检查：

```bash
# 主题描述文件
unzip -p aip-extend/aip-keycloak-theme/target/aip-keycloak-theme-1.0.0.jar META-INF/keycloak-themes.json

# SPI 注册（应存在以下文件）
unzip -l aip-extend/aip-keycloak-theme/target/aip-keycloak-theme-1.0.0.jar | grep 'META-INF/services'

# 滑块素材（应有 10 张）
unzip -l aip-extend/aip-keycloak-theme/target/aip-keycloak-theme-1.0.0.jar | grep -E 'captcha/(original|slidingBlock)'

# 不应出现字体/默认图片/日志依赖
unzip -l aip-extend/aip-keycloak-theme/target/aip-keycloak-theme-1.0.0.jar | grep -E 'fonts/|defaultImages/|fastjson|slf4j'
```

## 四、部署前检查

| 检查项 | 命令/位置 | 期望结果 |
|---|---|---|
| Keycloak 容器运行 | `docker ps` | `keycloak-server` 为 Up |
| Keycloak 版本 | `docker inspect keycloak-server --format '{{.Config.Image}}'` | `quay.io/keycloak/keycloak:26.1.3` |
| 启动方式 | `docker inspect keycloak-server --format '{{json .Config.Cmd}}'` | `["start-dev"]` 或生产启动命令 |
| providers 目录 | `ls -lh /opt/keycloak/providers/` | 已存在旧版主题 JAR 与用户联合存储 JAR |
| Realm 可访问 | `curl -s -o /dev/null -w '%{http_code}' http://<Keycloak地址>:<端口>/realms/aip` | `200` |
| 管理员权限 | Keycloak 管理台 | 可修改 Realm 的 Authentication Flow |

## 五、部署步骤

### 5.1 备份旧插件

```bash
STAMP=$(date +%Y%m%d-%H%M%S)
cp /opt/keycloak/providers/aip-keycloak-theme-1.0.0.jar \
   /opt/keycloak/providers/aip-keycloak-theme-1.0.0.jar.bak-$STAMP
ls -lh /opt/keycloak/providers/aip-keycloak-theme-1.0.0.jar.bak-$STAMP
```

> 记录备份文件名，回滚时使用。

### 5.2 上传新 JAR

```bash
scp aip-extend/aip-keycloak-theme/target/aip-keycloak-theme-1.0.0.jar \
    root@<Keycloak主机>:/opt/keycloak/providers/

# 上传后校验
sha256sum /opt/keycloak/providers/aip-keycloak-theme-1.0.0.jar
chmod 644 /opt/keycloak/providers/aip-keycloak-theme-1.0.0.jar
```

上传前后 SHA-256 必须一致。

### 5.3 重新增强并重启

按现场 Keycloak 启动方式选择：

#### 方式 A：`start-dev`（当前测试/开发环境）

```bash
docker restart keycloak-server
```

`start-dev` 启动时会自动重新增强并加载新 Provider。等待就绪：

```bash
for i in $(seq 1 90); do
  code=$(curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/realms/aip)
  [ "$code" = "200" ] && echo READY && break
  sleep 2
done
```

#### 方式 B：生产模式 `start`（未加 `--optimized`）

```bash
docker restart keycloak-server
```

启动时会检查配置并按需增强；观察日志确认增强完成。

#### 方式 C：生产模式 `start --optimized`

```bash
# 必须先构建增强，否则新 Provider 不会进入运行时
docker exec keycloak-server /opt/keycloak/bin/kc.sh build
docker restart keycloak-server
```

> 生产环境若容器已停止，可在同一容器配置/挂载下执行 `kc.sh build` 后再启动。

### 5.4 启动日志确认

```bash
docker logs --since 5m keycloak-server 2>&1 | \
  grep -E 'aip-captcha|aip-username-password-form|CAPTCHA assets|self-check|AJ-Captcha|ERROR'
```

期望看到（`KC-SERVICES0047` 为 Keycloak 对内部 SPI 的正常提示，可忽略）：

```text
KC-SERVICES0047: aip-captcha ... realm-restapi-extension
KC-SERVICES0047: aip-username-password-form ... authenticator
CAPTCHA assets loaded: original=6, slidingBlock=4
CAPTCHA image cache self-check: true
AJ-Captcha blockPuzzle engine initialized
```

若出现 `Failed to initialize AJ-Captcha blockPuzzle engine`，插件会失败关闭（登录被拒），按第十节排查。

## 六、启用登录主题

1. 登录 Keycloak 管理台，切换到 Realm **`aip`**；
2. `Realm settings` → `Themes`；
3. `Login theme` 选择 **`aip-theme`**；
4. 点击 `Save`。

> 若之前已启用 `aip-theme`，替换 JAR 后无需重复选择；但浏览器可能缓存旧 JS/CSS，建议强制刷新登录页（Ctrl+F5）。插件已为 `captcha.js`/`captcha.css` 增加版本参数。

## 七、绑定浏览器认证流（必做）

### 7.1 控制台手工配置

1. 进入 Realm `aip` → `Authentication` → `Flows`；
2. 复制内置 **`browser`** 流为 **`aip-browser`**：
   - 保持顶层 `Cookie` 为 `ALTERNATIVE` 且位于第一位；
   - `Kerberos` / `Identity Provider Redirector` 保持不变；
3. 打开 `aip-browser` 下的 **forms** 子流：
   - 将 **`Username Password Form`**（`auth-username-password-form`）替换为 **`AIP Username Password Form with CAPTCHA`**（`aip-username-password-form`）；
   - 该执行项 Requirements 设为 **`REQUIRED`**；
   - `Conditional OTP` 保持 `CONDITIONAL`；
4. 回到 `Authentication` → `Bindings`（或 Realm 设置中的 Browser Flow）：
   - 将 **Browser Flow** 绑定为 **`aip-browser`**；
5. 保存并退出。

### 7.2 脚本自动化（可选）

在确保以下条件后，可执行：

```bash
python configure_keycloak.py
```

脚本会幂等地创建/绑定 `aip-browser` 流，并在自定义执行项添加失败时于绑定 Realm 之前中止。

> 注意：该脚本同时会维护 Realm 的用户联合存储、OIDC 客户端与 Mapper。若只想调整验证码流程，建议使用 7.1 控制台步骤；在正式环境执行脚本前先确认脚本内的地址、Realm 与管理员凭据。

### 7.3 绑定后的结构检查

控制台中应看到：

```text
aip-browser (top-level)
├─ Cookie (ALTERNATIVE)
├─ Kerberos (DISABLED/ALTERNATIVE)
├─ Identity Provider Redirector (ALTERNATIVE)
├─ aip-browser forms (ALTERNATIVE)
│  ├─ AIP Username Password Form with CAPTCHA (REQUIRED)
│  └─ Conditional OTP (CONDITIONAL)
```

## 八、部署后验证

### 8.1 验证码接口

验证码接口现在要求有效的登录会话，不能使用自造 `tabId` 的裸 `curl` 请求作为成功验收。

1. 从门户发起登录，在浏览器登录页输入账号密码并打开滑块；
2. 在浏览器 Network 面板查看 `/realms/aip/aip-captcha/get` 请求，确认请求带有当前登录会话 Cookie，以及页面对应的 `clientId`、`tabId`；
3. 确认 HTTP `200`，JSON 包含 `originalImageBase64`、`jigsawImageBase64`、`token`、`secretKey`；
4. 如需命令行复核，可使用该请求的 **Copy as cURL** 在同一会话有效期内执行。不要分享或保存含 Cookie 的命令；
5. 缺少 Cookie、伪造 tabId 或登录会话过期的请求应返回 `400`，这是预期保护，不代表 Provider 未加载。

若会话过期，请从门户重新发起登录，而不是反复刷新旧验证码。

### 8.2 登录页渲染

浏览器打开 Realm `aip` 的登录页（通过门户跳转或直接访问授权端点），检查：

- 页面包含滑块轨道与拼图块元素；
- 页面源码中 `window.AIP_LOGIN_ACTION` 的 URL 使用 `&client_id=...&tab_id=...`，**不能出现 `&amp;`**；
- 加载的脚本为 `captcha.js?v=20261107a`、样式为 `captcha.css?v=20261106b`；
- 点击“立即登录”后能正常加载拼图底图。

### 8.3 服务端强制校验

不使用浏览器，直接构造登录 POST 且**不携带** `captchaVerification`（或用空值）：

- 期望返回登录页并提示“安全验证未通过，请重新完成拼图”；
- 不应出现“Invalid username or password”或直接进入密码校验；
- 这证明 `aip-username-password-form` 已在服务端生效。

### 8.4 SSO 静默登录

先用一个应用完成登录，再访问另一个应用：

- 应命中浏览器流的 `Cookie` 分支直接放行；
- 不应弹出滑块验证码。

### 8.5 异常票据验证（可选）

- 伪造/随机票据提交 → 拒绝；
- 同一票据重复提交 → 拒绝；
- 超过 180 秒后提交 → 拒绝；
- 跨 tab / 跨客户端复用 → 拒绝；
- 高频请求 → 返回 429（限流）。

## 九、运行配置与注意事项

| 项目 | 说明 |
|---|---|
| 票据有效期 | 180 秒，一次性消费 |
| 挑战有效期 | 120 秒，校验一次后失效 |
| 限流 | 每节点固定窗口：每登录会话 get 20/分钟、check 10/分钟；每 IP 总量 get 120/分钟、check 60/分钟，换标签页不会重置 IP 配额；同一节点同一会话在 1 分钟内失败 5 次，锁定 5 分钟并在其他节点的 get/check 生效；请求配额仍按节点计数 |
| 真实客户端 IP | 反向代理后需正确配置 `proxy-headers=xforwarded`，代理应覆盖客户端伪造的转发头且 Keycloak 仅接受可信代理流量；否则全部用户可能共享代理 IP 配额。企业 NAT 下同一出口用户共享 IP 总配额 |
| 字体依赖 | 无；插件跳过文字水印绘制，不需要容器 fontconfig 或字体包 |
| 浏览器缓存 | JS/CSS 已带版本参数；更新后建议 Ctrl+F5 |
| 认证流 | Realm 级配置；JAR 重启不会丢失，Realm 重建/导入后需重新绑定 |
| 用户联合存储 | 本插件不替代 `aip-keycloak-user-storage`，两者独立部署 |

## 十、常见故障排查

### 10.1 Provider 未加载

**现象**：登录页仍是旧版；管理台 Flows 中找不到 `aip-username-password-form`；日志无 `aip-captcha` 注册。

**排查**：
1. 确认 JAR 在 `/opt/keycloak/providers/`，而不是 `themes/`；
2. 确认文件权限 `644`、SHA-256 与构建产物一致；
3. `start --optimized` 是否执行了 `kc.sh build`；
4. 查看启动日志是否有 JAR 解析错误、版本不匹配或 `NoClassDefFoundError`；
5. 确认 Keycloak 版本为 26.1.3；跨大版本请重新编译。

### 10.2 `/get` 返回 500/503 或日志 `repCode=6113`

**现象**：滑块弹窗提示“验证码服务暂不可用”或网络错误；日志出现 `原始底图未初始化成功`、`6113`。

**排查**：
1. 确认使用的是包含内嵌滑块素材的当前 JAR；
2. 查看启动日志是否出现 `CAPTCHA assets loaded: original=6, slidingBlock=4` 与 `self-check: true`；
3. 若没有，重新构建并上传（素材由构建阶段复制到 JAR）；
4. 若出现 `Fontconfig head is null`，说明运行的是旧版 JAR；当前版本已跳过水印绘制，不依赖 fontconfig。

### 10.3 滑块弹窗提示“网络连接失败，请刷新重试”

**排查**：
1. 浏览器 Network 面板确认请求目标为 `/realms/aip/aip-captcha/get`；
2. 检查登录页源码中 `window.AIP_LOGIN_ACTION` 是否含 `&amp;`；若存在，确认使用最新 JAR 并强制刷新；
3. 强制刷新（Ctrl+F5）或使用无痕窗口，排除旧 JS 缓存与浏览器扩展干扰；
4. 按 8.1 从有效登录页复核验证码请求；缺少会话信息的裸请求返回 400 属于预期；
5. 若浏览器通过反向代理访问，确认代理放行 `/realms/{realm}/aip-captcha/**`。

### 10.4 滑块能显示但直接 POST 可绕过

**排查**：
1. 管理台确认 Realm 的 Browser Flow 为 `aip-browser`；
2. 确认 forms 子流中 `aip-username-password-form` 为 `REQUIRED`，标准 `auth-username-password-form` 已移除；
3. Realm 重新导入/重建后是否忘记重新绑定；
4. 绑定后重新测试无票据 POST。

### 10.5 返回 429

**原因**：触发验证码接口限流或失败锁定。

**处理**：
- 等待限流窗口（1 分钟）或锁定窗口（5 分钟）结束；
- 检查是否有自动化脚本高频请求；
- 检查可信代理是否正确转发真实 IP，避免所有用户都计入代理 IP 的总额度；
- 多节点环境的请求计数采用节点内固定窗口，极端情况下两节点请求配额约为单节点 ×2；失败锁定在所有节点的获取和校验接口生效。

### 10.6 密码错误后需要重新拖动滑块

**这是预期行为**：密码错误后需要重新完成滑块，再次提交登录。若重复提示会话无效，请从门户重新发起登录。

### 10.7 浏览器控制台出现 `content.js` / `Storage coordinator disconnected`

这是浏览器扩展（密码管理器/内容脚本）的报错，与验证码插件无关。建议在无痕窗口或禁用扩展后复测。

## 十一、回滚

### 11.1 恢复旧插件

```bash
mv /opt/keycloak/providers/aip-keycloak-theme-1.0.0.jar.bak-<STAMP> \
   /opt/keycloak/providers/aip-keycloak-theme-1.0.0.jar

# start-dev / start：重启
docker restart keycloak-server

# start --optimized：先构建增强再重启
docker exec keycloak-server /opt/keycloak/bin/kc.sh build
docker restart keycloak-server
```

### 11.2 恢复认证流

管理台将 Realm `aip` 的 Browser Flow 改回 **`browser`**（或在 Flow 中恢复标准 `Username Password Form`）。

### 11.3 恢复 Realm 配置（可选）

若变更前已导出 Realm 备份（管理台 `Realm settings` → `Partial export`，或通过 Admin API 保存 JSON），可按现场流程重新导入。

## 十二、升级与维护

1. **日常更新**：构建 → 上传 providers → 按启动方式重启/增强 → 验证 8.1~8.3；
2. **只改主题资源**：同样需要重新打包替换 JAR；前端资源带版本参数，建议强制刷新；
3. **Keycloak 版本升级**：先用目标版本重新编译插件并跑单元测试，再部署；不要直接复用旧版本编译产物；
4. **Realm 重建/导入**：必须重新绑定 `aip-browser` 流程；
5. **不要用 `deploy_keycloak.py` 做日常更新**：该脚本会重建容器并清理数据库，仅用于初始环境搭建；
6. **生产模式**：`start --optimized` 下每次 Provider 更新都必须先 `kc.sh build`；`start-dev` 重启会自动增强。
7. **本次安全修复升级**：所有 Keycloak 节点应更新到同一版 JAR，避免新旧节点混用。升级前已打开的登录页或验证码可能失效，用户应从门户重新发起登录。

## 十三、部署检查清单

- [ ] JAR 已构建，`BUILD SUCCESS`，Java 与前端回归测试全部通过
- [ ] JAR SHA-256 已记录
- [ ] 旧 JAR 已备份
- [ ] 新 JAR 已上传至 `/opt/keycloak/providers/`，权限 644，SHA-256 一致
- [ ] 已按启动方式执行重启/`kc.sh build`
- [ ] 启动日志出现 `CAPTCHA assets loaded: original=6, slidingBlock=4`
- [ ] 启动日志出现 `CAPTCHA image cache self-check: true`
- [ ] 启动日志出现 `AJ-Captcha blockPuzzle engine initialized`
- [ ] Realm `aip` 的 Login theme 为 `aip-theme`
- [ ] Browser Flow 已绑定 `aip-browser`
- [ ] forms 子流中 `aip-username-password-form` 为 REQUIRED，标准表单已移除
- [ ] 从真实登录页请求 `/aip-captcha/get` 返回 200；无会话或伪造 tabId 请求被拒绝
- [ ] 登录页能正常加载滑块
- [ ] 无票据直接 POST 被拒绝
- [ ] SSO Cookie 静默登录不弹滑块
- [ ] 回滚备份文件名已记录


