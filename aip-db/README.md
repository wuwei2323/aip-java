# 数据库管理

## 1. 快速开始
请参考以下章节:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/docs/2.1.8.RELEASE/maven-plugin/)
* [JDBC API](https://docs.spring.io/spring-boot/docs/2.1.8.RELEASE/reference/htmlsingle/#boot-features-sql)
* [Flyway Migration](https://docs.spring.io/spring-boot/docs/2.1.8.RELEASE/reference/htmlsingle/#howto-execute-flyway-database-migrations-on-startup)
* [Spring Web](https://docs.spring.io/spring-boot/docs/2.1.8.RELEASE/reference/htmlsingle/#boot-features-developing-web-applications)

## 2. 重要！！！

* 对数据库表的新增、修改、数据初始化均需通过flywaydb处理

## 3. 数据库命名规约
 
* 基本原则上参考《阿里巴巴Java开发手册》；

## 3.1. 总体原则

- 符合三范式
- 每一列表单一个意思、每一行代表一个实例、每一行有唯一键、表内没有其它表的非主键信息
- 避免保留字（SQL关键字）
- 避免可为空的列
- 使用下划线命名，语义化全小写英文
- 每个表固定以下字段
   - 序号 ```id```
   - 创建时间 ```create_time```，字段类型为```datetime```
   - 更新时间 ```update_time```，字段类型为```datetime```

## 3.2. 命名规范

### 3.2.1. 数据库

- 使用下划线命名：即统一采用小写形式，多个单词采用```_```连接
- 正例：
  ```
  sys_user
  ```
- 反例：
  ```
  sys-user
  ```
### 3.2.1. 表

- 统一采用单数形式
- 尽量使用完整的```英文单词```作为表名
- 严禁使用拼音与英文混合的方式，更不允许直接使用中文的方式
- 使用下划线命名：即统一采用小写形式，多个单词采用```_```连接
- 原则上，尽量控制表名字符长度在64字符以内（MSSQL Server支持长达128个字符，但为确保程序也兼容MYSQL需作此控制）
- 避免保留字（SQL关键字）

### 3.2.2. 字段

- 尽量使用完整的```英文单词```作为字段名
- 使用下划线命名：即统一采用小写形式，多个单词采用```_```连接
- 原则上，尽量控制表名字符长度在64字符以内（MSSQL Server支持长达128个字符，但为确保程序也兼容MYSQL需作此控制）
- 避免保留字（SQL关键字）
- 必须有主键字段（通常为id）,允许联合主键
- 合理结合业务给表字段添加索引和唯一索引，具体要求可参考```《阿里巴巴Java开发手册》```

### 3.2.3. 对象
- 存储过程以```sp_```为前缀
- 触发器以```tr_```为前缀
- 函数以```fn_```为前缀
- 外键以```fk_```为前缀
- 主键以```pk_```为前缀
- 普通索引以```idx_```为前缀
- 唯一索引以```uk_```为前缀


