# AIP 应用集成平台（Application Integration Platform）

## 1. 基本介绍

AIP 应用集成平台（Application Integration Platform）是基于 [RuoYi-Vue-Plus 5.5.2](https://gitee.com/dromara/RuoYi-Vue-Plus.git) 架构开发的现代化企业级系统与数据集成中台。系统采用微服务/模块化前后端分离架构，支持多租户、多端访问与高并发处理，提供异构系统互通、服务编排、消息路由适配、工作流流转与统一管控等完整的应用集成能力。

### 1.1 技术栈

- **后端框架**: Spring Boot 3.5.10 + JDK 17+
- **前端框架**: Vue3 + TypeScript + ElementPlus
- **数据库**: MySQL 8.0+
- **ORM框架**: Mybatis-Plus 3.5.14
- **权限认证**: Sa-Token 1.44.0 + JWT
- **缓存**: Redis 5-7 + Redisson 3.52.0
- **数据库连接池**: HikariCP
- **数据库版本管理**: Flyway
- **Web容器**: Undertow

### 1.2 系统特性

- 支持多租户隔离，各租户数据完全独立
- 支持PC端、移动端多端访问
- 完善的权限管理体系，支持角色、部门、岗位多维度权限控制
- 提供代码生成器，快速生成业务代码
- 支持文件存储、短信通知、邮件发送等扩展功能

## 2. 基础组件及中间件介绍

### 2.1 核心框架

- **Spring Boot**: 基础框架，提供自动化配置、依赖管理、嵌入式容器等功能
- **Spring MVC**: Web层框架，提供RESTful API支持
- **Spring AOP**: 面向切面编程，用于日志、权限、事务等横切关注点

### 2.2 持久层

- **Mybatis-Plus**: 基于Mybatis的增强工具，提供CRUD操作、分页、多租户、数据权限等功能
- **Dynamic-Datasource**: 动态多数据源框架，支持异构数据库切换和事务管理
- **HikariCP**: 高性能数据库连接池，Spring Boot官方默认连接池

### 2.3 缓存与消息

- **Redis**: 内存数据库，用于缓存、分布式锁、分布式限流等功能
- **Redisson**: Redis官方推荐的Java客户端，提供分布式对象、分布式锁、分布式集合等功能
- **Lock4j**: 基于Redisson的分布式锁框架

### 2.4 权限认证

- **Sa-Token**: 轻量级权限认证框架，支持登录校验、角色校验、权限校验、二级认证等
- **Sa-Token-JWT**: 集成JWT令牌机制，支持无状态认证
- **JustAuth**: 第三方登录组件，支持微信、钉钉等数十种三方认证

### 2.5 任务调度

- **SnailJob**: 分布式任务调度框架，支持分片、重试、DAG任务流等高级功能
- **Spring Task**: Spring内置的异步任务执行器

### 2.6 工作流

- **Warm-Flow**: 国产工作流引擎，支持复杂审批流程、流转、委派、加减签、会签等功能

### 2.7 工具类库

- **Hutool**: Java工具类库，提供日期、加密、字符串、IO等上百种工具方法
- **Lombok**: 代码简化工具，通过注解自动生成getter、setter、toString等方法
- **MapStruct-Plus**: 对象映射框架，支持对象转换、集合映射等功能

### 2.8 监控与文档

- **Spring Boot Admin**: 应用监控平台，实时监控服务状态、健康检查、在线日志等
- **SpringDoc**: API文档生成工具，基于OpenAPI 3.0规范，无注解零入侵
- **P6Spy**: SQL性能分析插件，输出完整SQL与执行时间

### 2.9 文件与通知

- **Minio**: 分布式文件存储，支持多机、多硬盘、多分片存储
- **AWS S3 SDK**: 云存储客户端，支持阿里云、腾讯云、七牛等支持S3协议的云存储
- **SMS4J**: 短信融合包，支持数十种短信厂商
- **Mail API**: 邮件发送组件，支持SMTP协议

## 3. FlywayDB介绍

### 3.1 Flyway简介

Flyway是数据库版本管理工具，用于跟踪、管理和应用数据库变更。系统采用Flyway进行数据库迁移管理，确保数据库结构的一致性和可追溯性。

### 3.2 Flyway配置

配置文件位置: `aip-db/src/main/resources/common.yml`

```yaml
spring:
  flyway:
    # 迁移脚本位置，支持多个路径
    locations: classpath:db/migration, classpath:cn/org/aip/migrations
    # 首次运行时是否创建基线
    baseline-on-migrate: true
    # 忽略缺失的迁移脚本
    ignore-migration-patterns:
    - "*:missing"
    # 允许无序执行迁移脚本
    out-of-order: true
    # 占位符配置
    placeholders:
      initiator: "#{initiator}"
```

### 3.3 迁移脚本命名规范

Flyway迁移脚本存放在 `aip-db/src/main/resources/db/migration` 目录下，命名格式如下：

```
V{版本号}__{描述}.sql
```

示例: `V1_001__init.sql`

- `V` 表示版本迁移（Versioned）
- 版本号采用下划线分隔，如 `1_001`、`1_002`
- 双下划线 `__` 分隔版本号和描述
- 描述采用小写字母和下划线，清晰说明脚本功能

### 3.4 Flyway使用注意事项

1. **脚本不可修改**: 已执行的迁移脚本禁止修改，否则会导致版本校验失败
2. **顺序执行**: 迁移脚本按版本号顺序执行，确保依赖关系正确
3. **事务性**: 每个迁移脚本在一个事务中执行，失败则自动回滚
4. **记录表**: Flyway自动创建 `flyway_schema_history` 表记录执行历史
5. **开发流程**: 新建表、修改表结构、数据初始化等数据库操作必须通过Flyway迁移脚本执行
6. **版本控制**: 迁移脚本纳入Git版本控制，确保团队协作一致性

## 4. 构建打包说明

### 4.1 环境要求

- JDK 17 或 JDK 21
- Maven 3.6+
- MySQL 8.0+
- Redis 5.0+

### 4.2 构建命令

#### Windows环境

```bash
mvn clean package -Dmaven.test.skip=true
```

或使用批处理脚本：

```bash
build.bat
```

#### Linux/Mac环境

```bash
mvn clean package -Dmaven.test.skip=true
```

### 4.3 环境配置

项目支持多环境配置，通过Maven Profile切换：

- **local**: 本地开发环境
- **dev**: 开发环境（默认）
- **prod**: 生产环境

指定环境打包：

```bash
# 开发环境打包（默认）
mvn clean package -Dmaven.test.skip=true

# 生产环境打包
mvn clean package -Pprod -Dmaven.test.skip=true

# 本地环境打包
mvn clean package -Plocal -Dmaven.test.skip=true
```

### 4.4 打包产物

打包完成后，在 `aip-admin/target` 目录下生成可执行JAR文件：

```
aip-admin/target/aip-admin.jar
```

### 4.5 启动应用

```bash
# 使用默认配置启动（dev环境）
java -jar aip-admin/target/aip-admin.jar

# 指定配置文件启动
java -jar aip-admin/target/aip-admin.jar --spring.profiles.active=prod

# 指定端口启动
java -jar aip-admin/target/aip-admin.jar --server.port=8080
```

### 4.6 Docker部署

项目提供Docker编排文件，位于 `script/docker` 目录：

```bash
# 启动所有服务
cd script/docker
docker-compose up -d

# 查看日志
docker-compose logs -f

# 停止服务
docker-compose down
```

## 5. 代码提交规范

### 5.1 Git分支管理

- **main**: 主分支，用于生产环境部署
- **develop**: 开发分支，用于日常开发
- **feature/***: 功能分支，从develop分支检出，开发完成后合并回develop
- **hotfix/***: 紧急修复分支，从main分支检出，修复完成后合并回main和develop
- **release/***: 发布分支，用于版本发布准备

### 5.2 提交信息规范

采用约定式提交（Conventional Commits）格式：

```
<type>(<scope>): <subject>

<body>

<footer>
```

#### Type类型

- **feat**: 新功能（feature）
- **fix**: 修复bug
- **docs**: 文档修改
- **style**: 代码格式修改，不影响功能
- **refactor**: 代码重构
- **perf**: 性能优化
- **test**: 测试相关
- **chore**: 构建/工具链相关
- **revert**: 回滚提交

#### Scope范围

- **system**: 系统基础模块
- **integration**: 集成核心服务
- **adapter**: 协议与系统适配器
- **message**: 消息路由与转换
- **workflow**: 工作流编排模块
- **common**: 通用核心组件
- **db**: 数据库与迁移脚本
- **admin**: 管理调度门户
- **other**: 其他

#### 示例

```bash
# 新增功能
git commit -m "feat(integration): 添加第三方系统适配器对接能力"

# 修复bug
git commit -m "fix(system): 修复用户登录时token过期判断错误"

# 文档更新
git commit -m "docs(readme): 更新应用集成平台部署文档"

# 代码重构
git commit -m "refactor(message): 优化消息转换与路由处理性能"

# 性能优化
git commit -m "perf(integration): 优化集成流程与数据管道吞吐性能"
```

### 5.3 代码规范

#### 5.3.1 Java代码规范

遵循《阿里巴巴Java开发手册》：

1. **命名规范**
   - 类名使用大驼峰命名法：`IntegrationService`
   - 方法名和变量名使用小驼峰命名法：`getIntegrationById`
   - 常量使用全大写下划线分隔：`MAX_INTEGRATION_COUNT`

2. **注释规范**
   - 类注释包含功能描述、作者、日期
   - 方法注释包含参数说明、返回值说明、异常说明
   - 复杂业务逻辑添加行内注释

3. **代码格式**
   - 使用统一的代码格式化配置（项目中已提供 `.editorconfig`）
   - 缩进使用4个空格
   - 每行代码不超过120个字符

#### 5.3.2 SQL规范

遵循 `aip-db/README.md` 中的数据库命名规约：

1. **表命名**: 使用下划线命名的小写英文，采用单数形式
2. **字段命名**: 使用下划线命名的小写英文
3. **索引命名**:
   - 普通索引: `idx_` + 字段名
   - 唯一索引: `uk_` + 字段名
4. **迁移脚本**: 必须通过Flyway管理，命名遵循Flyway规范

### 5.4 提交前检查

提交代码前请完成以下检查：

1. [ ] 代码格式化符合规范
2. [ ] 代码编译通过，无警告
3. [ ] 单元测试通过（如有）
4. [ ] 提交信息符合规范
5. [ ] 敏感信息已移除（密码、密钥等）
6. [ ] 数据库变更已通过Flyway迁移脚本管理
7. [ ] 代码注释清晰，逻辑易读

### 5.5 Code Review

1. 提交Pull Request前先进行自测
2. 代码Review时关注点：
   - 代码逻辑是否正确
   - 是否符合代码规范
   - 是否存在安全漏洞
   - 性能是否优化
3. Review意见及时响应和修改
