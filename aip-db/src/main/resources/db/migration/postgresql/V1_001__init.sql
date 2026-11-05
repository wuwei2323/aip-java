-- ============================================================
-- PostgreSQL 合并初始化脚本 (V1_001 ~ V1_007 终态归并版)
-- 包含：RuoYi-Vue-Plus 核心表、SnailJob 调度表
-- ============================================================

-- 第三方平台授权表
create table sys_social
(
    id int8 not null,
    user_id int8 not null,
    tenant_id varchar (20) default '000000':: varchar,
    auth_id varchar (255) not null,
    source varchar (255) not null,
    open_id varchar (255) default null :: varchar,
    user_name varchar (30) not null,
    nick_name varchar (30) default '':: varchar,
    email varchar (255) default '':: varchar,
    avatar varchar (500) default '':: varchar,
    access_token varchar (2000) not null,
    expire_in int8 default null,
    refresh_token varchar (2000) default null :: varchar,
    access_code varchar (255) default null :: varchar,
    union_id varchar (255) default null :: varchar,
    scope varchar (255) default null :: varchar,
    token_type varchar (255) default null :: varchar,
    id_token varchar (2000) default null :: varchar,
    mac_algorithm varchar (255) default null :: varchar,
    mac_key varchar (255) default null :: varchar,
    code varchar (255) default null :: varchar,
    oauth_token varchar (255) default null :: varchar,
    oauth_token_secret varchar (255) default null :: varchar,
    create_dept int8,
    create_by int8,
    create_time timestamp,
    update_by int8,
    update_time timestamp,
    del_flag char default '0'::bpchar,
    constraint "pk_sys_social" primary key (id)
);

comment on table sys_social is '社会化关系表';
comment on column sys_social.id is '主键';
comment on column sys_social.user_id is '用户ID';
comment on column sys_social.tenant_id is '应用id';
comment on column sys_social.auth_id is '平台+平台唯一id';
comment on column sys_social.source is '用户来源';
comment on column sys_social.open_id is '平台编号唯一id';
comment on column sys_social.user_name is '登录账号';
comment on column sys_social.nick_name is '用户昵称';
comment on column sys_social.email is '用户邮箱';
comment on column sys_social.avatar is '头像地址';
comment on column sys_social.access_token is '用户的授权令牌';
comment on column sys_social.expire_in is '用户的授权令牌的有效期，部分平台可能没有';
comment on column sys_social.refresh_token is '刷新令牌，部分平台可能没有';
comment on column sys_social.access_code is '平台的授权信息，部分平台可能没有';
comment on column sys_social.union_id is '用户的 unionid';
comment on column sys_social.scope is '授予的权限，部分平台可能没有';
comment on column sys_social.token_type is '个别平台的授权信息，部分平台可能没有';
comment on column sys_social.id_token is 'id token，部分平台可能没有';
comment on column sys_social.mac_algorithm is '小米平台用户的附带属性，部分平台可能没有';
comment on column sys_social.mac_key is '小米平台用户的附带属性，部分平台可能没有';
comment on column sys_social.code is '用户的授权code，部分平台可能没有';
comment on column sys_social.oauth_token is 'Twitter平台用户的附带属性，部分平台可能没有';
comment on column sys_social.oauth_token_secret is 'Twitter平台用户的附带属性，部分平台可能没有';
comment on column sys_social.create_dept is '创建部门';
comment on column sys_social.create_by is '创建者';
comment on column sys_social.create_time is '创建时间';
comment on column sys_social.update_by is '更新者';
comment on column sys_social.update_time is '更新时间';
comment on column sys_social.del_flag is '删除标志（0代表存在 1代表删除）';

-- 租户表
create table if not exists sys_tenant
(
    id                int8,
    tenant_id         varchar(20)   not null,
    contact_user_name varchar(20)   default null::varchar,
    contact_phone     varchar(20)   default null::varchar,
    company_name      varchar(30)   default null::varchar,
    license_number    varchar(30)   default null::varchar,
    address           varchar(200)  default null::varchar,
    intro             varchar(200)  default null::varchar,
    domain            varchar(200)  default null::varchar,
    remark            varchar(200)  default null::varchar,
    package_id        int8,
    expire_time       timestamp,
    account_count     int4          default -1,
    status            char          default '0'::bpchar,
    del_flag          char          default '0'::bpchar,
    create_dept       int8,
    create_by         int8,
    create_time       timestamp,
    update_by         int8,
    update_time       timestamp,
    constraint "pk_sys_tenant" primary key (id)
);

comment on table   sys_tenant                    is '租户表';
comment on column  sys_tenant.tenant_id          is '租户编号';
comment on column  sys_tenant.contact_phone      is '联系电话';
comment on column  sys_tenant.company_name       is '企业/租户名称';
comment on column  sys_tenant.contact_user_name  is '联系人';
comment on column  sys_tenant.license_number     is '统一社会信用代码';
comment on column  sys_tenant.address            is '地址';
comment on column  sys_tenant.intro              is '企业简介';
comment on column  sys_tenant.domain             is '域名';
comment on column  sys_tenant.remark             is '备注';
comment on column  sys_tenant.package_id         is '应用套餐编号';
comment on column  sys_tenant.expire_time        is '过期时间';
comment on column  sys_tenant.account_count      is '用户数量（-1不限制）';
comment on column  sys_tenant.status             is '租户状态（0正常 1停用）';
comment on column  sys_tenant.del_flag           is '删除标志（0代表存在 1代表删除）';

-- 初始化默认唯一租户数据（集成平台）
insert into sys_tenant (id, tenant_id, contact_user_name, contact_phone, company_name, license_number, address, intro, domain, remark, package_id, expire_time, account_count, status, del_flag, create_dept, create_by, create_time, update_by, update_time)
values (1, '000000', '超级管理员', '18888888888', '集成平台', null, null, '集成平台管理系统', null, null, null, null, -1, '0', '0', 103, 1, now(), null, null)
on conflict (id) do nothing;

-- 应用注册表
CREATE TABLE IF NOT EXISTS sys_application (
    id                int8 PRIMARY KEY,
    app_id            varchar(64)  NOT NULL,
    tenant_id         varchar(64)  DEFAULT '000000',
    app_name          varchar(100) NOT NULL,
    category          varchar(64)  DEFAULT 'system',
    description       varchar(500),
    client_secret_enc text,
    redirect_uris     text,
    entry_url         varchar(500),
    icon              varchar(255),
    status            char(1)      DEFAULT '0',
    del_flag          char(1)      DEFAULT '0',
    create_dept       int8,
    create_by         int8,
    create_time       timestamp,
    update_by         int8,
    update_time       timestamp
);

CREATE UNIQUE INDEX IF NOT EXISTS sys_application_app_id_uk ON sys_application (app_id);

COMMENT ON TABLE sys_application IS '应用注册表';
COMMENT ON COLUMN sys_application.id IS '主键';
COMMENT ON COLUMN sys_application.app_id IS 'OIDC客户端ID(client_id)';
COMMENT ON COLUMN sys_application.tenant_id IS '租户编号(默认为集成平台000000)';
COMMENT ON COLUMN sys_application.app_name IS '应用名称';
COMMENT ON COLUMN sys_application.category IS '应用分类';
COMMENT ON COLUMN sys_application.description IS '应用简介';
COMMENT ON COLUMN sys_application.client_secret_enc IS '客户端密钥(密文)';
COMMENT ON COLUMN sys_application.redirect_uris IS '登录重定向URI列表(JSON)';
COMMENT ON COLUMN sys_application.entry_url IS '应用入口地址';
COMMENT ON COLUMN sys_application.icon IS '应用图标';
COMMENT ON COLUMN sys_application.status IS '状态（0正常 1停用）';
COMMENT ON COLUMN sys_application.del_flag IS '删除标志（0代表存在 1代表删除）';

-- 初始化门户应用数据
INSERT INTO sys_application (id, app_id, tenant_id, app_name, category, description, entry_url,
                             status, del_flag, create_by, create_time)
VALUES (1, '000000', '000000', '集成平台门户', 'system', '多应用通用后台管理管理系统', '/',
        '0', '0', 1, now());

-- 1、部门表
create table if not exists sys_dept
(
    dept_id int8,
    tenant_id varchar (20) default '000000':: varchar,
    parent_id int8 default 0,
    ancestors varchar (500) default '':: varchar,
    dept_name varchar (30) default '':: varchar,
    dept_category varchar (100) default null :: varchar,
    order_num int4 default 0,
    leader int8 default null,
    phone varchar (11) default null :: varchar,
    email varchar (50) default null :: varchar,
    status char default '0'::bpchar,
    del_flag char default '0'::bpchar,
    create_dept int8,
    create_by int8,
    create_time timestamp,
    update_by int8,
    update_time timestamp,
    constraint "sys_dept_pk" primary key (dept_id)
);

comment on table sys_dept is '部门表';
comment on column sys_dept.dept_id is '部门ID';
comment on column sys_dept.tenant_id is '租户编号';
comment on column sys_dept.parent_id is '父部门ID';
comment on column sys_dept.ancestors is '祖级列表';
comment on column sys_dept.dept_name is '部门名称';
comment on column sys_dept.dept_category is '部门类别编码';
comment on column sys_dept.order_num is '显示顺序';
comment on column sys_dept.leader is '负责人';
comment on column sys_dept.phone is '联系电话';
comment on column sys_dept.email is '邮箱';
comment on column sys_dept.status is '部门状态（0正常 1停用）';
comment on column sys_dept.del_flag is '删除标志（0代表存在 1代表删除）';
comment on column sys_dept.create_dept is '创建部门';
comment on column sys_dept.create_by is '创建者';
comment on column sys_dept.create_time is '创建时间';
comment on column sys_dept.update_by is '更新者';
comment on column sys_dept.update_time is '更新时间';

-- 初始化-部门表数据
insert into sys_dept values(100, '000000', 0,   '0',          'XXX科技',   null, 0, null, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, now(), null, null);
insert into sys_dept
values (101, '000000', 100, '0,100', '深圳总公司', null, 1, null, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, now(), null,
        null);
insert into sys_dept
values (102, '000000', 100, '0,100', '长沙分公司', null, 2, null, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, now(), null,
        null);
insert into sys_dept
values (103, '000000', 101, '0,100,101', '研发部门', null, 1, 1, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, now(), null,
        null);
insert into sys_dept
values (104, '000000', 101, '0,100,101', '市场部门', null, 2, null, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, now(), null,
        null);
insert into sys_dept
values (105, '000000', 101, '0,100,101', '测试部门', null, 3, null, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, now(), null,
        null);
insert into sys_dept
values (106, '000000', 101, '0,100,101', '财务部门', null, 4, null, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, now(), null,
        null);
insert into sys_dept
values (107, '000000', 101, '0,100,101', '运维部门', null, 5, null, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, now(), null,
        null);
insert into sys_dept
values (108, '000000', 102, '0,100,102', '市场部门', null, 1, null, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, now(), null,
        null);
insert into sys_dept
values (109, '000000', 102, '0,100,102', '财务部门', null, 2, null, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, now(), null,
        null);

-- 2、用户信息表（包含 SSO 与认证扩展字段）
create table if not exists sys_user
(
    user_id int8,
    tenant_id varchar (20) default '000000':: varchar,
    dept_id int8,
    user_name varchar (30) not null,
    nick_name varchar (30) not null,
    user_type varchar (10) default 'sys_user':: varchar,
    email varchar (50) default '':: varchar,
    phonenumber varchar (11) default '':: varchar,
    sex char default '0'::bpchar,
    avatar int8,
    password varchar (100) default '':: varchar,
    status char default '0'::bpchar,
    del_flag char default '0'::bpchar,
    login_ip varchar (128) default '':: varchar,
    login_date timestamp,
    create_dept int8,
    create_by int8,
    create_time timestamp,
    update_by int8,
    update_time timestamp,
    remark varchar (500) default null :: varchar,
    employee_no varchar (64),
    keycloak_user_id varchar (64),
    auth_source varchar (20) default 'PLATFORM',
    password_reset_required bool default false,
    constraint "sys_user_pk" primary key (user_id)
);

CREATE UNIQUE INDEX IF NOT EXISTS sys_user_employee_no_uk
    ON sys_user (employee_no) WHERE del_flag = '0' AND employee_no IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS sys_user_keycloak_id_uk
    ON sys_user (keycloak_user_id) WHERE keycloak_user_id IS NOT NULL;

comment on table sys_user is '用户信息表';
comment on column sys_user.user_id is '用户ID';
comment on column sys_user.tenant_id is '租户编号';
comment on column sys_user.dept_id is '部门ID';
comment on column sys_user.user_name is '用户账号';
comment on column sys_user.nick_name is '用户昵称';
comment on column sys_user.user_type is '用户类型（sys_user系统用户）';
comment on column sys_user.email is '用户邮箱';
comment on column sys_user.phonenumber is '手机号码';
comment on column sys_user.sex is '用户性别（0男 1女 2未知）';
comment on column sys_user.avatar is '头像地址';
comment on column sys_user.password is '密码';
comment on column sys_user.status is '帐号状态（0正常 1停用）';
comment on column sys_user.del_flag is '删除标志（0代表存在 1代表删除）';
comment on column sys_user.login_ip is '最后登陆IP';
comment on column sys_user.login_date is '最后登陆时间';
comment on column sys_user.create_dept is '创建部门';
comment on column sys_user.create_by is '创建者';
comment on column sys_user.create_time is '创建时间';
comment on column sys_user.update_by is '更新者';
comment on column sys_user.update_time is '更新时间';
comment on column sys_user.remark is '备注';
comment on column sys_user.employee_no is '员工工号(全局唯一)';
comment on column sys_user.keycloak_user_id is 'Keycloak用户标识';
comment on column sys_user.auth_source is '认证来源(PLATFORM/LOCAL/LDAP)';
comment on column sys_user.password_reset_required is '是否需要重置密码(首次登录强制改密)';

-- 初始化-用户信息表数据
insert into sys_user values(1, '000000', 103, 'admin', '疯狂的狮子Li', 'sys_user', 'crazyLionLi@163.com', '15888888888', '1', null, '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', now(), 103, 1, now(), null, null, '管理员', null, null, 'PLATFORM', false);
insert into sys_user
values (3, '000000', 108, 'test', '本部门及以下 密码666666', 'sys_user', '', '', '0', null,
        '$2a$10$b8yUzN0C71sbz.PhNOCgJe.Tu1yWC3RNrTyjSQ8p1W0.aaUXUJ.Ne', '0', '0', '127.0.0.1', now(), 103, 1, now(), 3,
        now(), NULL, null, null, 'PLATFORM', false);
insert into sys_user
values (4, '000000', 102, 'test1', '仅本人 密码666666', 'sys_user', '', '', '0', null,
        '$2a$10$b8yUzN0C71sbz.PhNOCgJe.Tu1yWC3RNrTyjSQ8p1W0.aaUXUJ.Ne', '0', '0', '127.0.0.1', now(), 103, 1, now(), 4,
        now(), NULL, null, null, 'PLATFORM', false);

-- 3、岗位信息表
create table if not exists sys_post
(
    post_id int8,
    tenant_id varchar (20) default '000000':: varchar,
    dept_id int8,
    post_code varchar (64) not null,
    post_category varchar (100) default null,
    post_name varchar (50) not null,
    post_sort int4 not null,
    status char not null,
    create_dept int8,
    create_by int8,
    create_time timestamp,
    update_by int8,
    update_time timestamp,
    remark varchar (500) default null :: varchar,
    constraint "sys_post_pk" primary key (post_id)
);

comment on table sys_post is '岗位信息表';
comment on column sys_post.post_id is '岗位ID';
comment on column sys_post.tenant_id is '租户编号';
comment on column sys_post.dept_id is '部门id';
comment on column sys_post.post_code is '岗位编码';
comment on column sys_post.post_category is '岗位类别编码';
comment on column sys_post.post_name is '岗位名称';
comment on column sys_post.post_sort is '显示顺序';
comment on column sys_post.status is '状态（0正常 1停用）';
comment on column sys_post.create_dept is '创建部门';
comment on column sys_post.create_by is '创建者';
comment on column sys_post.create_time is '创建时间';
comment on column sys_post.update_by is '更新者';
comment on column sys_post.update_time is '更新时间';
comment on column sys_post.remark is '备注';

-- 初始化-岗位信息表数据
insert into sys_post values(1, '000000', 103, 'ceo',  null, '董事长',   1, '0', 103, 1, now(), null, null, '');
insert into sys_post
values (2, '000000', 100, 'se', null, '项目经理', 2, '0', 103, 1, now(), null, null, '');
insert into sys_post
values (3, '000000', 100, 'hr', null, '人力资源', 3, '0', 103, 1, now(), null, null, '');
insert into sys_post
values (4, '000000', 100, 'user', null, '普通员工', 4, '0', 103, 1, now(), null, null, '');

-- 4、角色信息表
create table if not exists sys_role
(
    role_id int8,
    tenant_id varchar (20) default '000000':: varchar,
    role_name varchar (30) not null,
    role_key varchar (100) not null,
    role_sort int4 not null,
    data_scope char default '1'::bpchar,
    menu_check_strictly bool default true,
    dept_check_strictly bool default true,
    status char not null,
    del_flag char default '0'::bpchar,
    create_dept int8,
    create_by int8,
    create_time timestamp,
    update_by int8,
    update_time timestamp,
    remark varchar (500) default null :: varchar,
    constraint "sys_role_pk" primary key (role_id)
);

comment on table sys_role is '角色信息表';
comment on column sys_role.role_id is '角色ID';
comment on column sys_role.tenant_id is '租户编号';
comment on column sys_role.role_name is '角色名称';
comment on column sys_role.role_key is '角色权限字符串';
comment on column sys_role.role_sort is '显示顺序';
comment on column sys_role.data_scope is '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限 5：仅本人数据权限 6：部门及以下或本人数据权限）';
comment on column sys_role.menu_check_strictly is '菜单树选择项是否关联显示';
comment on column sys_role.dept_check_strictly is '部门树选择项是否关联显示';
comment on column sys_role.status is '角色状态（0正常 1停用）';
comment on column sys_role.del_flag is '删除标志（0代表存在 1代表删除）';
comment on column sys_role.create_dept is '创建部门';
comment on column sys_role.create_by is '创建者';
comment on column sys_role.create_time is '创建时间';
comment on column sys_role.update_by is '更新者';
comment on column sys_role.update_time is '更新时间';
comment on column sys_role.remark is '备注';

-- 初始化-角色信息表数据
insert into sys_role values('1', '000000', '超级管理员',   'superadmin', 1, '1', 't', 't', '0', '0', 103, 1, now(), null, null, '超级管理员');
insert into sys_role
values ('3', '000000', '本部门及以下', 'test1', 3, '4', 't', 't', '0', '0', 103, 1, now(), NULL, NULL, '');
insert into sys_role
values ('4', '000000', '仅本人', 'test2', 4, '5', 't', 't', '0', '0', 103, 1, now(), NULL, NULL, '');

-- 角色在各应用下的数据权限与联动配置表（来自 V1_005）
CREATE TABLE IF NOT EXISTS sys_role_data_scope
(
    id int8 NOT NULL,
    role_id int8 NOT NULL,
    tenant_id varchar (20) DEFAULT '000000' NOT NULL,
    app_id varchar (20) DEFAULT '000000' NOT NULL,
    data_scope char (1) DEFAULT '1'::bpchar,
    menu_check_strictly bool DEFAULT true,
    dept_check_strictly bool DEFAULT true,
    create_dept int8,
    create_by int8,
    create_time timestamp,
    update_by int8,
    update_time timestamp,
    CONSTRAINT sys_role_data_scope_pk PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS sys_role_data_scope_uk ON sys_role_data_scope (role_id, app_id);
CREATE INDEX IF NOT EXISTS sys_role_data_scope_role_id_index ON sys_role_data_scope (role_id);
CREATE INDEX IF NOT EXISTS sys_role_data_scope_app_id_index ON sys_role_data_scope (app_id);
CREATE INDEX IF NOT EXISTS sys_role_data_scope_tenant_id_index ON sys_role_data_scope (tenant_id);

COMMENT ON TABLE sys_role_data_scope IS '角色在应用下的数据权限与联动配置表';
COMMENT ON COLUMN sys_role_data_scope.id IS '主键';
COMMENT ON COLUMN sys_role_data_scope.role_id IS '角色ID';
COMMENT ON COLUMN sys_role_data_scope.tenant_id IS '租户编号(固定000000)';
COMMENT ON COLUMN sys_role_data_scope.app_id IS '应用编号';
COMMENT ON COLUMN sys_role_data_scope.data_scope IS '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限 5：仅本人数据权限 6：部门及以下或本人数据权限）';
COMMENT ON COLUMN sys_role_data_scope.menu_check_strictly IS '菜单树选择项是否关联显示';
COMMENT ON COLUMN sys_role_data_scope.dept_check_strictly IS '部门树选择项是否关联显示';

INSERT INTO sys_role_data_scope (id, role_id, tenant_id, app_id, data_scope, menu_check_strictly, dept_check_strictly,
                                 create_time)
VALUES (1, 1, '000000', '000000', '1', true, true, now()),
       (3, 3, '000000', '000000', '4', true, true, now()),
       (4, 4, '000000', '000000', '5', true, true, now());

-- 5、菜单权限表（包含 app_id 字段，并已完成 V1_006/V1_007 菜单收敛）
create table if not exists sys_menu
(
    menu_id int8,
    tenant_id varchar (20) default '000000':: varchar,
    menu_name varchar (50) not null,
    parent_id int8 default 0,
    order_num int4 default 0,
    path varchar (200) default '':: varchar,
    component varchar (255) default null :: varchar,
    query_param varchar (255) default null :: varchar,
    is_frame char default '1'::bpchar,
    is_cache char default '0'::bpchar,
    menu_type char default ''::bpchar,
    visible char default '0'::bpchar,
    status char default '0'::bpchar,
    perms varchar (100) default null :: varchar,
    icon varchar (100) default '#':: varchar,
    create_dept int8,
    create_by int8,
    create_time timestamp,
    update_by int8,
    update_time timestamp,
    remark varchar (500) default '':: varchar,
    app_id varchar (64),
    constraint "sys_menu_pk" primary key (menu_id)
);

CREATE INDEX IF NOT EXISTS sys_menu_app_id_idx ON sys_menu (app_id);

comment on table sys_menu is '菜单权限表';
comment on column sys_menu.menu_id is '菜单ID';
comment on column sys_menu.tenant_id is '租户编号';
comment on column sys_menu.menu_name is '菜单名称';
comment on column sys_menu.parent_id is '父菜单ID';
comment on column sys_menu.order_num is '显示顺序';
comment on column sys_menu.path is '路由地址';
comment on column sys_menu.component is '组件路径';
comment on column sys_menu.query_param is '路由参数';
comment on column sys_menu.is_frame is '是否为外链（0是 1否）';
comment on column sys_menu.is_cache is '是否缓存（0缓存 1不缓存）';
comment on column sys_menu.menu_type is '菜单类型（M目录 C菜单 F按钮）';
comment on column sys_menu.visible is '显示状态（0显示 1隐藏）';
comment on column sys_menu.status is '菜单状态（0正常 1停用）';
comment on column sys_menu.perms is '权限标识';
comment on column sys_menu.icon is '菜单图标';
comment on column sys_menu.create_dept is '创建部门';
comment on column sys_menu.create_by is '创建者';
comment on column sys_menu.create_time is '创建时间';
comment on column sys_menu.update_by is '更新者';
comment on column sys_menu.update_time is '更新时间';
comment on column sys_menu.remark is '备注';
comment on column sys_menu.app_id is '关联应用ID(仅menu_type=C可配置，未关联为普通菜单)';

-- 初始化-菜单信息表数据（已剔除废弃菜单 6, 121, 122 及其按钮，保留收敛后的 124 应用管理）
-- 一级菜单
insert into sys_menu
values ('1', '000000', '系统管理', '0', '1', 'system', null, '', '1', '0', 'M', '0', '0', '', 'system', 103, 1, now(), null, null,
        '系统管理目录');

-- 二级菜单
insert into sys_menu
values ('100', '000000', '用户管理', '1', '1', 'user', 'system/user/index', '', '1', '0', 'C', '0', '0', 'system:user:list',
        'user', 103, 1, now(), null, null, '用户管理菜单');
insert into sys_menu
values ('101', '000000', '角色管理', '1', '2', 'role', 'system/role/index', '', '1', '0', 'C', '0', '0', 'system:role:list',
        'peoples', 103, 1, now(), null, null, '角色管理菜单');
insert into sys_menu
values ('102', '000000', '菜单管理', '1', '3', 'menu', 'system/menu/index', '', '1', '0', 'C', '0', '0', 'system:menu:list',
        'tree-table', 103, 1, now(), null, null, '菜单管理菜单');
insert into sys_menu
values ('103', '000000', '部门管理', '1', '4', 'dept', 'system/dept/index', '', '1', '0', 'C', '0', '0', 'system:dept:list',
        'tree', 103, 1, now(), null, null, '部门管理菜单');

insert into sys_menu
values ('105', '000000', '字典管理', '1', '6', 'dict', 'system/dict/index', '', '1', '0', 'C', '0', '0', 'system:dict:list',
        'dict', 103, 1, now(), null, null, '字典管理菜单');
insert into sys_menu
values ('106', '000000', '参数设置', '1', '7', 'config', 'system/config/index', '', '1', '0', 'C', '0', '0', 'system:config:list',
        'edit', 103, 1, now(), null, null, '参数设置菜单');
insert into sys_menu
values ('107', '000000', '通知公告', '1', '8', 'notice', 'system/notice/index', '', '1', '0', 'C', '0', '0', 'system:notice:list',
        'message', 103, 1, now(), null, null, '通知公告菜单');
insert into sys_menu
values ('108', '000000', '日志管理', '1', '9', 'log', '', '', '1', '0', 'M', '0', '0', '', 'log', 103, 1, now(), null, null,
        '日志管理菜单');

insert into sys_menu
values ('124', '000000', '应用管理', '1', '12', 'application', 'system/application/index', '', '1', '0', 'C', '0', '0',
        'system:application:list', 'app', 103, 1, now(), null, null, '应用管理菜单');

insert into sys_menu
values ('130', '000000', '分配用户', '1', '2', 'role-auth/user/:roleId', 'system/role/authUser', '', '1', '1', 'C', '1', '0',
        'system:role:edit', '#', 103, 1, now(), null, null, '/system/role');
insert into sys_menu
values ('131', '000000', '分配角色', '1', '1', 'user-auth/role/:userId', 'system/user/authRole', '', '1', '1', 'C', '1', '0',
        'system:user:edit', '#', 103, 1, now(), null, null, '/system/user');
insert into sys_menu
values ('132', '000000', '字典数据', '1', '6', 'dict-data/index/:dictId', 'system/dict/data', '', '1', '1', 'C', '1', '0',
        'system:dict:list', '#', 103, 1, now(), null, null, '/system/dict');

-- 三级菜单
insert into sys_menu
values ('500', '000000', '操作日志', '108', '1', 'operlog', 'monitor/operlog/index', '', '1', '0', 'C', '0', '0',
        'monitor:operlog:list', 'form', 103, 1, now(), null, null, '操作日志菜单');
insert into sys_menu
values ('501', '000000', '登录日志', '108', '2', 'logininfor', 'monitor/logininfor/index', '', '1', '0', 'C', '0', '0',
        'monitor:logininfor:list', 'logininfor', 103, 1, now(), null, null, '登录日志菜单');

-- 用户管理按钮
insert into sys_menu
values ('1001', '000000', '用户查询', '100', '1', '', '', '', '1', '0', 'F', '0', '0', 'system:user:query', '#', 103, 1, now(),
        null, null, '');
insert into sys_menu
values ('1002', '000000', '用户新增', '100', '2', '', '', '', '1', '0', 'F', '0', '0', 'system:user:add', '#', 103, 1, now(),
        null, null, '');
insert into sys_menu
values ('1003', '000000', '用户修改', '100', '3', '', '', '', '1', '0', 'F', '0', '0', 'system:user:edit', '#', 103, 1, now(),
        null, null, '');
insert into sys_menu
values ('1004', '000000', '用户删除', '100', '4', '', '', '', '1', '0', 'F', '0', '0', 'system:user:remove', '#', 103, 1, now(),
        null, null, '');
insert into sys_menu
values ('1005', '000000', '用户导出', '100', '5', '', '', '', '1', '0', 'F', '0', '0', 'system:user:export', '#', 103, 1, now(),
        null, null, '');
insert into sys_menu
values ('1006', '000000', '用户导入', '100', '6', '', '', '', '1', '0', 'F', '0', '0', 'system:user:import', '#', 103, 1, now(),
        null, null, '');
insert into sys_menu
values ('1007', '000000', '重置密码', '100', '7', '', '', '', '1', '0', 'F', '0', '0', 'system:user:resetPwd', '#', 103, 1, now(),
        null, null, '');

-- 角色管理按钮
insert into sys_menu
values ('1008', '000000', '角色查询', '101', '1', '', '', '', '1', '0', 'F', '0', '0', 'system:role:query', '#', 103, 1, now(),
        null, null, '');
insert into sys_menu
values ('1009', '000000', '角色新增', '101', '2', '', '', '', '1', '0', 'F', '0', '0', 'system:role:add', '#', 103, 1, now(),
        null, null, '');
insert into sys_menu
values ('1010', '000000', '角色修改', '101', '3', '', '', '', '1', '0', 'F', '0', '0', 'system:role:edit', '#', 103, 1, now(),
        null, null, '');
insert into sys_menu
values ('1011', '000000', '角色删除', '101', '4', '', '', '', '1', '0', 'F', '0', '0', 'system:role:remove', '#', 103, 1, now(),
        null, null, '');
insert into sys_menu
values ('1012', '000000', '角色导出', '101', '5', '', '', '', '1', '0', 'F', '0', '0', 'system:role:export', '#', 103, 1, now(),
        null, null, '');

-- 菜单管理按钮
insert into sys_menu
values ('1013', '000000', '菜单查询', '102', '1', '', '', '', '1', '0', 'F', '0', '0', 'system:menu:query', '#', 103, 1, now(),
        null, null, '');
insert into sys_menu
values ('1014', '000000', '菜单新增', '102', '2', '', '', '', '1', '0', 'F', '0', '0', 'system:menu:add', '#', 103, 1, now(),
        null, null, '');
insert into sys_menu
values ('1015', '000000', '菜单修改', '102', '3', '', '', '', '1', '0', 'F', '0', '0', 'system:menu:edit', '#', 103, 1, now(),
        null, null, '');
insert into sys_menu
values ('1016', '000000', '菜单删除', '102', '4', '', '', '', '1', '0', 'F', '0', '0', 'system:menu:remove', '#', 103, 1, now(),
        null, null, '');

-- 部门管理按钮
insert into sys_menu
values ('1017', '000000', '部门查询', '103', '1', '', '', '', '1', '0', 'F', '0', '0', 'system:dept:query', '#', 103, 1, now(),
        null, null, '');
insert into sys_menu
values ('1018', '000000', '部门新增', '103', '2', '', '', '', '1', '0', 'F', '0', '0', 'system:dept:add', '#', 103, 1, now(),
        null, null, '');
insert into sys_menu
values ('1019', '000000', '部门修改', '103', '3', '', '', '', '1', '0', 'F', '0', '0', 'system:dept:edit', '#', 103, 1, now(),
        null, null, '');
insert into sys_menu
values ('1020', '000000', '部门删除', '103', '4', '', '', '', '1', '0', 'F', '0', '0', 'system:dept:remove', '#', 103, 1, now(),
        null, null, '');

-- 字典管理按钮
insert into sys_menu
values ('1026', '000000', '字典查询', '105', '1', '#', '', '', '1', '0', 'F', '0', '0', 'system:dict:query', '#', 103, 1, now(),
        null, null, '');
insert into sys_menu
values ('1027', '000000', '字典新增', '105', '2', '#', '', '', '1', '0', 'F', '0', '0', 'system:dict:add', '#', 103, 1, now(),
        null, null, '');
insert into sys_menu
values ('1028', '000000', '字典修改', '105', '3', '#', '', '', '1', '0', 'F', '0', '0', 'system:dict:edit', '#', 103, 1, now(),
        null, null, '');
insert into sys_menu
values ('1029', '000000', '字典删除', '105', '4', '#', '', '', '1', '0', 'F', '0', '0', 'system:dict:remove', '#', 103, 1, now(),
        null, null, '');
insert into sys_menu
values ('1030', '000000', '字典导出', '105', '5', '#', '', '', '1', '0', 'F', '0', '0', 'system:dict:export', '#', 103, 1, now(),
        null, null, '');

-- 参数设置按钮
insert into sys_menu
values ('1031', '000000', '参数查询', '106', '1', '#', '', '', '1', '0', 'F', '0', '0', 'system:config:query', '#', 103, 1, now(),
        null, null, '');
insert into sys_menu
values ('1032', '000000', '参数新增', '106', '2', '#', '', '', '1', '0', 'F', '0', '0', 'system:config:add', '#', 103, 1, now(),
        null, null, '');
insert into sys_menu
values ('1033', '000000', '参数修改', '106', '3', '#', '', '', '1', '0', 'F', '0', '0', 'system:config:edit', '#', 103, 1, now(),
        null, null, '');
insert into sys_menu
values ('1034', '000000', '参数删除', '106', '4', '#', '', '', '1', '0', 'F', '0', '0', 'system:config:remove', '#', 103, 1,
        now(), null, null, '');
insert into sys_menu
values ('1035', '000000', '参数导出', '106', '5', '#', '', '', '1', '0', 'F', '0', '0', 'system:config:export', '#', 103, 1,
        now(), null, null, '');

-- 通知公告按钮
insert into sys_menu
values ('1036', '000000', '公告查询', '107', '1', '#', '', '', '1', '0', 'F', '0', '0', 'system:notice:query', '#', 103, 1, now(),
        null, null, '');
insert into sys_menu
values ('1037', '000000', '公告新增', '107', '2', '#', '', '', '1', '0', 'F', '0', '0', 'system:notice:add', '#', 103, 1, now(),
        null, null, '');
insert into sys_menu
values ('1038', '000000', '公告修改', '107', '3', '#', '', '', '1', '0', 'F', '0', '0', 'system:notice:edit', '#', 103, 1, now(),
        null, null, '');
insert into sys_menu
values ('1039', '000000', '公告删除', '107', '4', '#', '', '', '1', '0', 'F', '0', '0', 'system:notice:remove', '#', 103, 1,
        now(), null, null, '');

-- 操作日志按钮
insert into sys_menu
values ('1040', '000000', '操作查询', '500', '1', '#', '', '', '1', '0', 'F', '0', '0', 'monitor:operlog:query', '#', 103, 1,
        now(), null, null, '');
insert into sys_menu
values ('1041', '000000', '操作删除', '500', '2', '#', '', '', '1', '0', 'F', '0', '0', 'monitor:operlog:remove', '#', 103, 1,
        now(), null, null, '');
insert into sys_menu
values ('1042', '000000', '日志导出', '500', '4', '#', '', '', '1', '0', 'F', '0', '0', 'monitor:operlog:export', '#', 103, 1,
        now(), null, null, '');

-- 登录日志按钮
insert into sys_menu
values ('1043', '000000', '登录查询', '501', '1', '#', '', '', '1', '0', 'F', '0', '0', 'monitor:logininfor:query', '#', 103, 1,
        now(), null, null, '');
insert into sys_menu
values ('1044', '000000', '登录删除', '501', '2', '#', '', '', '1', '0', 'F', '0', '0', 'monitor:logininfor:remove', '#', 103, 1,
        now(), null, null, '');
insert into sys_menu
values ('1045', '000000', '日志导出', '501', '3', '#', '', '', '1', '0', 'F', '0', '0', 'monitor:logininfor:export', '#', 103, 1,
        now(), null, null, '');
insert into sys_menu
values ('1050', '000000', '账户解锁', '501', '4', '#', '', '', '1', '0', 'F', '0', '0', 'monitor:logininfor:unlock', '#', 103, 1,
        now(), null, null, '');

-- 应用管理按钮（来自 V1_006）
insert into sys_menu
values ('1630', '000000', '应用查询', '124', '1', '#', '', '', '1', '0', 'F', '0', '0', 'system:application:query', '#', 103, 1,
        now(), null, null, '');
insert into sys_menu
values ('1631', '000000', '应用新增', '124', '2', '#', '', '', '1', '0', 'F', '0', '0', 'system:application:add', '#', 103, 1,
        now(), null, null, '');
insert into sys_menu
values ('1632', '000000', '应用修改', '124', '3', '#', '', '', '1', '0', 'F', '0', '0', 'system:application:edit', '#', 103, 1,
        now(), null, null, '');
insert into sys_menu
values ('1633', '000000', '应用删除', '124', '4', '#', '', '', '1', '0', 'F', '0', '0', 'system:application:remove', '#', 103, 1,
        now(), null, null, '');
insert into sys_menu
values ('1634', '000000', '重置密钥', '124', '5', '#', '', '', '1', '0', 'F', '0', '0', 'system:application:resetSecret', '#',
        103, 1, now(), null, null, '');

-- 6、用户和角色关联表  用户N-1角色
create table if not exists sys_user_role
(
    user_id int8 not null,
    role_id int8 not null,
    constraint sys_user_role_pk primary key (user_id, role_id)
);

comment on table sys_user_role is '用户和角色关联表';
comment on column sys_user_role.user_id is '用户ID';
comment on column sys_user_role.role_id is '角色ID';

-- 初始化-用户和角色关联表数据
insert into sys_user_role values ('1', '1');
insert into sys_user_role
values ('3', '3');
insert into sys_user_role
values ('4', '4');

-- 7、角色和菜单关联表  角色1-N菜单（按应用独立配置，来自 V1_005）
create table if not exists sys_role_menu
(
    role_id int8 not null,
    menu_id int8 not null,
    tenant_id varchar (20) default '000000' not null,
    app_id varchar (20) default '000000' not null,
    constraint sys_role_menu_pk primary key (role_id, menu_id, app_id)
);

CREATE INDEX IF NOT EXISTS sys_role_menu_app_id_index ON sys_role_menu (app_id);
CREATE INDEX IF NOT EXISTS sys_role_menu_tenant_id_index ON sys_role_menu (tenant_id);

comment on table sys_role_menu is '角色和菜单关联表';
comment on column sys_role_menu.role_id is '角色ID';
comment on column sys_role_menu.menu_id is '菜单ID';
comment on column sys_role_menu.tenant_id is '租户编号(固定000000)';
comment on column sys_role_menu.app_id is '应用编号';

-- 初始化-角色和菜单关联表数据
insert into sys_role_menu values ('1', '124', '000000', '000000');
insert into sys_role_menu
values ('1', '1630', '000000', '000000');
insert into sys_role_menu
values ('1', '1631', '000000', '000000');
insert into sys_role_menu
values ('1', '1632', '000000', '000000');
insert into sys_role_menu
values ('1', '1633', '000000', '000000');
insert into sys_role_menu
values ('1', '1634', '000000', '000000');
insert into sys_role_menu
values ('3', '1', '000000', '000000');

insert into sys_role_menu
values ('3', '100', '000000', '000000');
insert into sys_role_menu
values ('3', '101', '000000', '000000');
insert into sys_role_menu
values ('3', '102', '000000', '000000');
insert into sys_role_menu
values ('3', '103', '000000', '000000');

insert into sys_role_menu
values ('3', '105', '000000', '000000');
insert into sys_role_menu
values ('3', '106', '000000', '000000');
insert into sys_role_menu
values ('3', '107', '000000', '000000');
insert into sys_role_menu
values ('3', '108', '000000', '000000');

insert into sys_role_menu
values ('3', '130', '000000', '000000');
insert into sys_role_menu
values ('3', '131', '000000', '000000');
insert into sys_role_menu
values ('3', '132', '000000', '000000');

insert into sys_role_menu
values ('3', '500', '000000', '000000');
insert into sys_role_menu
values ('3', '501', '000000', '000000');
insert into sys_role_menu
values ('3', '1001', '000000', '000000');
insert into sys_role_menu
values ('3', '1002', '000000', '000000');
insert into sys_role_menu
values ('3', '1003', '000000', '000000');
insert into sys_role_menu
values ('3', '1004', '000000', '000000');
insert into sys_role_menu
values ('3', '1005', '000000', '000000');
insert into sys_role_menu
values ('3', '1006', '000000', '000000');
insert into sys_role_menu
values ('3', '1007', '000000', '000000');
insert into sys_role_menu
values ('3', '1008', '000000', '000000');
insert into sys_role_menu
values ('3', '1009', '000000', '000000');
insert into sys_role_menu
values ('3', '1010', '000000', '000000');
insert into sys_role_menu
values ('3', '1011', '000000', '000000');
insert into sys_role_menu
values ('3', '1012', '000000', '000000');
insert into sys_role_menu
values ('3', '1013', '000000', '000000');
insert into sys_role_menu
values ('3', '1014', '000000', '000000');
insert into sys_role_menu
values ('3', '1015', '000000', '000000');
insert into sys_role_menu
values ('3', '1016', '000000', '000000');
insert into sys_role_menu
values ('3', '1017', '000000', '000000');
insert into sys_role_menu
values ('3', '1018', '000000', '000000');
insert into sys_role_menu
values ('3', '1019', '000000', '000000');
insert into sys_role_menu
values ('3', '1020', '000000', '000000');

insert into sys_role_menu
values ('3', '1026', '000000', '000000');
insert into sys_role_menu
values ('3', '1027', '000000', '000000');
insert into sys_role_menu
values ('3', '1028', '000000', '000000');
insert into sys_role_menu
values ('3', '1029', '000000', '000000');
insert into sys_role_menu
values ('3', '1030', '000000', '000000');
insert into sys_role_menu
values ('3', '1031', '000000', '000000');
insert into sys_role_menu
values ('3', '1032', '000000', '000000');
insert into sys_role_menu
values ('3', '1033', '000000', '000000');
insert into sys_role_menu
values ('3', '1034', '000000', '000000');
insert into sys_role_menu
values ('3', '1035', '000000', '000000');
insert into sys_role_menu
values ('3', '1036', '000000', '000000');
insert into sys_role_menu
values ('3', '1037', '000000', '000000');
insert into sys_role_menu
values ('3', '1038', '000000', '000000');
insert into sys_role_menu
values ('3', '1039', '000000', '000000');
insert into sys_role_menu
values ('3', '1040', '000000', '000000');
insert into sys_role_menu
values ('3', '1041', '000000', '000000');
insert into sys_role_menu
values ('3', '1042', '000000', '000000');
insert into sys_role_menu
values ('3', '1043', '000000', '000000');
insert into sys_role_menu
values ('3', '1044', '000000', '000000');
insert into sys_role_menu
values ('3', '1045', '000000', '000000');
insert into sys_role_menu
values ('3', '1050', '000000', '000000');

-- 8、角色和部门关联表  角色1-N部门（按应用独立配置，来自 V1_005）
create table if not exists sys_role_dept
(
    role_id int8 not null,
    dept_id int8 not null,
    tenant_id varchar (20) default '000000' not null,
    constraint sys_role_dept_pk primary key (role_id, dept_id, tenant_id)
);

CREATE INDEX IF NOT EXISTS sys_role_dept_tenant_id_index ON sys_role_dept (tenant_id);

comment on table sys_role_dept is '角色和部门关联表';
comment on column sys_role_dept.role_id is '角色ID';
comment on column sys_role_dept.dept_id is '部门ID';

-- 9、用户与岗位关联表  用户1-N岗位
create table if not exists sys_user_post
(
    user_id int8 not null,
    post_id int8 not null,
    constraint sys_user_post_pk primary key (user_id, post_id)
);

comment on table sys_user_post is '用户与岗位关联表';
comment on column sys_user_post.user_id is '用户ID';
comment on column sys_user_post.post_id is '岗位ID';

-- 初始化-用户与岗位关联表数据
insert into sys_user_post values ('1', '1');

-- 10、操作日志记录
create table if not exists sys_oper_log
(
    oper_id int8,
    tenant_id varchar (20) default '000000':: varchar,
    title varchar (50) default '':: varchar,
    business_type int4 default 0,
    method varchar (100) default '':: varchar,
    request_method varchar (10) default '':: varchar,
    operator_type int4 default 0,
    oper_name varchar (50) default '':: varchar,
    dept_name varchar (50) default '':: varchar,
    oper_url varchar (255) default '':: varchar,
    oper_ip varchar (128) default '':: varchar,
    oper_location varchar (255) default '':: varchar,
    oper_param varchar (4000) default '':: varchar,
    json_result varchar (4000) default '':: varchar,
    status int4 default 0,
    error_msg varchar (4000) default '':: varchar,
    oper_time timestamp,
    cost_time int8 default 0,
    constraint sys_oper_log_pk primary key (oper_id)
);

create index idx_sys_oper_log_bt ON sys_oper_log (business_type);
create index idx_sys_oper_log_s ON sys_oper_log (status);
create index idx_sys_oper_log_ot ON sys_oper_log (oper_time);

comment on table sys_oper_log is '操作日志记录';
comment on column sys_oper_log.oper_id is '日志主键';
comment on column sys_oper_log.tenant_id is '应用编号';
comment on column sys_oper_log.title is '模块标题';
comment on column sys_oper_log.business_type is '业务类型（0其它 1新增 2修改 3删除）';
comment on column sys_oper_log.method is '方法名称';
comment on column sys_oper_log.request_method is '请求方式';
comment on column sys_oper_log.operator_type is '操作类别（0其它 1后台用户 2手机端用户）';
comment on column sys_oper_log.oper_name is '操作人员';
comment on column sys_oper_log.dept_name is '部门名称';
comment on column sys_oper_log.oper_url is '请求URL';
comment on column sys_oper_log.oper_ip is '主机地址';
comment on column sys_oper_log.oper_location is '操作地点';
comment on column sys_oper_log.oper_param is '请求参数';
comment on column sys_oper_log.json_result is '返回参数';
comment on column sys_oper_log.status is '操作状态（0正常 1异常）';
comment on column sys_oper_log.error_msg is '错误消息';
comment on column sys_oper_log.oper_time is '操作时间';
comment on column sys_oper_log.cost_time is '消耗时间';

-- 11、字典类型表（已合并 V1_003、V1_004、V1_007 变更）
create table if not exists sys_dict_type
(
    dict_id int8,
    dict_name varchar (100) default '':: varchar,
    dict_type varchar (100) default '':: varchar,
    category_type varchar (10) default 'system',
    component_info text,
    create_dept int8,
    create_by int8,
    create_time timestamp,
    update_by int8,
    update_time timestamp,
    remark varchar (500) default null :: varchar,
    constraint sys_dict_type_pk primary key (dict_id)
);

create unique index sys_dict_type_index1 ON sys_dict_type (dict_type);

comment on table sys_dict_type is '字典类型表';
comment on column sys_dict_type.dict_id is '字典主键';
comment on column sys_dict_type.dict_name is '字典名称';
comment on column sys_dict_type.dict_type is '字典类型';
comment on column sys_dict_type.category_type is '字典分类（system=系统 role=角色）';
comment on column sys_dict_type.component_info is '组件配置信息（JSON）';
comment on column sys_dict_type.create_dept is '创建部门';
comment on column sys_dict_type.create_by is '创建者';
comment on column sys_dict_type.create_time is '创建时间';
comment on column sys_dict_type.update_by is '更新者';
comment on column sys_dict_type.update_time is '更新时间';
comment on column sys_dict_type.remark is '备注';

insert into sys_dict_type
values (1, '用户性别', 'sys_user_sex', 'system', null, 103, 1, now(), null, null, '用户性别列表');
insert into sys_dict_type
values (2, '菜单状态', 'sys_show_hide', 'system', null, 103, 1, now(), null, null, '菜单状态列表');
insert into sys_dict_type
values (3, '系统开关', 'sys_normal_disable', 'system', null, 103, 1, now(), null, null, '系统开关列表');
insert into sys_dict_type
values (6, '系统是否', 'sys_yes_no', 'system', null, 103, 1, now(), null, null, '系统是否列表');
insert into sys_dict_type
values (7, '通知类型', 'sys_notice_type', 'system', null, 103, 1, now(), null, null, '通知类型列表');
insert into sys_dict_type
values (8, '通知状态', 'sys_notice_status', 'system', null, 103, 1, now(), null, null, '通知状态列表');
insert into sys_dict_type
values (9, '操作类型', 'sys_oper_type', 'system', null, 103, 1, now(), null, null, '操作类型列表');
insert into sys_dict_type
values (10, '系统状态', 'sys_common_status', 'system', null, 103, 1, now(), null, null, '登录状态列表');
insert into sys_dict_type
values (11, '授权类型', 'sys_grant_type', 'system', null, 103, 1, now(), null, null, '认证授权类型');
insert into sys_dict_type
values (12, '设备类型', 'sys_device_type', 'system', null, 103, 1, now(), null, null, '客户端设备类型');
INSERT INTO "public"."sys_dict_type" ("dict_id", "dict_name", "dict_type", "create_dept", "create_by", "create_time", "update_by", "update_time", "remark", "category_type", "component_info") VALUES (13, '应用分类', 'app_category', 103, 1, '2026-11-03 13:20:57.906263', NULL, NULL, '应用分类列表', 'system', NULL);

-- 字典类型与应用关联表（来自 V1_003，重命名为 sys_dict_type_app）
CREATE TABLE IF NOT EXISTS sys_dict_type_app
(
    dict_id int8 NOT NULL,
    app_id varchar (64) NOT NULL,
    CONSTRAINT sys_dict_type_app_pk PRIMARY KEY (dict_id, app_id)
);

COMMENT ON TABLE sys_dict_type_app IS '字典类型与应用关联表';
COMMENT ON COLUMN sys_dict_type_app.dict_id IS '字典主键';
COMMENT ON COLUMN sys_dict_type_app.app_id IS '应用ID';

CREATE INDEX IF NOT EXISTS sys_dict_type_app_app_id_index ON sys_dict_type_app (app_id);

-- 字典类型与角色关联表 & 角色字典配置值表（来自 V1_004）
CREATE TABLE IF NOT EXISTS sys_dict_type_role
(
    dict_id int8 NOT NULL,
    role_id int8 NOT NULL,
    CONSTRAINT sys_dict_type_role_pk PRIMARY KEY (dict_id, role_id)
);

COMMENT ON TABLE sys_dict_type_role IS '字典类型与角色关联表';
COMMENT ON COLUMN sys_dict_type_role.dict_id IS '字典主键';
COMMENT ON COLUMN sys_dict_type_role.role_id IS '角色ID';

CREATE INDEX IF NOT EXISTS sys_dict_type_role_role_id_index ON sys_dict_type_role (role_id);

CREATE TABLE IF NOT EXISTS sys_role_dict_value
(
    id          int8        NOT NULL,
    role_id     int8        NOT NULL,
    tenant_id   varchar(20) DEFAULT '000000' NOT NULL,
    app_id      varchar(20) DEFAULT '000000' NOT NULL,
    dict_id     int8        NOT NULL,
    field_key   varchar(64) NOT NULL,
    field_value text,
    create_dept int8,
    create_by   int8,
    create_time timestamp,
    update_by   int8,
    update_time timestamp,
    CONSTRAINT sys_role_dict_value_pk PRIMARY KEY (id)
);

COMMENT ON TABLE sys_role_dict_value IS '角色字典配置值表';
COMMENT ON COLUMN sys_role_dict_value.id IS '主键';
COMMENT ON COLUMN sys_role_dict_value.role_id IS '角色ID';
COMMENT ON COLUMN sys_role_dict_value.tenant_id IS '租户编号(固定000000)';
COMMENT ON COLUMN sys_role_dict_value.app_id IS '应用编号';
COMMENT ON COLUMN sys_role_dict_value.dict_id IS '字典主键';
COMMENT ON COLUMN sys_role_dict_value.field_key IS '组件字段标识（取 component_info.fieldKey）';
COMMENT ON COLUMN sys_role_dict_value.field_value IS '字段值（多选为JSON数组字符串）';

CREATE UNIQUE INDEX IF NOT EXISTS sys_role_dict_value_uk
    ON sys_role_dict_value (role_id, app_id, dict_id, field_key);

CREATE INDEX IF NOT EXISTS sys_role_dict_value_app_id_index ON sys_role_dict_value (app_id);
CREATE INDEX IF NOT EXISTS sys_role_dict_value_tenant_id_index ON sys_role_dict_value (tenant_id);
CREATE INDEX IF NOT EXISTS sys_role_dict_value_dict_index ON sys_role_dict_value (dict_id);

-- 12、字典数据表
create table if not exists sys_dict_data
(
    dict_code int8,
    dict_sort int4 default 0,
    dict_label varchar (100) default '':: varchar,
    dict_value varchar (100) default '':: varchar,
    dict_type varchar (100) default '':: varchar,
    css_class varchar (100) default null :: varchar,
    list_class varchar (100) default null :: varchar,
    is_default char default 'N'::bpchar,
    create_dept int8,
    create_by int8,
    create_time timestamp,
    update_by int8,
    update_time timestamp,
    remark varchar (500) default null :: varchar,
    label_style char (1) default '0'::bpchar,
    constraint sys_dict_data_pk primary key (dict_code)
);

comment on table sys_dict_data is '字典数据表';
comment on column sys_dict_data.dict_code is '字典编码';
comment on column sys_dict_data.dict_sort is '字典排序';
comment on column sys_dict_data.dict_label is '字典标签';
comment on column sys_dict_data.dict_value is '字典键值';
comment on column sys_dict_data.dict_type is '字典类型';
comment on column sys_dict_data.css_class is '样式属性（其他样式扩展）';
comment on column sys_dict_data.list_class is '表格回显样式';
comment on column sys_dict_data.is_default is '是否默认（Y是 N否）';
comment on column sys_dict_data.create_dept is '创建部门';
comment on column sys_dict_data.create_by is '创建者';
comment on column sys_dict_data.create_time is '创建时间';
comment on column sys_dict_data.update_by is '更新者';
comment on column sys_dict_data.update_time is '更新时间';
comment on column sys_dict_data.remark is '备注';
comment on column sys_dict_data.label_style is '数据名称样式（0正常 1停用）';

insert into sys_dict_data
values (1, 1, '男', '0', 'sys_user_sex', '', '', 'Y', 103, 1, now(), null, null, '性别男', '0');
insert into sys_dict_data
values (2, 2, '女', '1', 'sys_user_sex', '', '', 'N', 103, 1, now(), null, null, '性别女', '0');
insert into sys_dict_data
values (3, 3, '未知', '2', 'sys_user_sex', '', '', 'N', 103, 1, now(), null, null, '性别未知', '0');
insert into sys_dict_data
values (4, 1, '显示', '0', 'sys_show_hide', '', 'primary', 'Y', 103, 1, now(), null, null, '显示菜单', '0');
insert into sys_dict_data
values (5, 2, '隐藏', '1', 'sys_show_hide', '', 'danger', 'N', 103, 1, now(), null, null, '隐藏菜单', '0');
insert into sys_dict_data
values (6, 1, '正常', '0', 'sys_normal_disable', '', 'primary', 'Y', 103, 1, now(), null, null, '正常状态', '0');
insert into sys_dict_data
values (7, 2, '停用', '1', 'sys_normal_disable', '', 'danger', 'N', 103, 1, now(), null, null, '停用状态', '0');
insert into sys_dict_data
values (12, 1, '是', 'Y', 'sys_yes_no', '', 'primary', 'Y', 103, 1, now(), null, null, '系统默认是', '0');
insert into sys_dict_data
values (13, 2, '否', 'N', 'sys_yes_no', '', 'danger', 'N', 103, 1, now(), null, null, '系统默认否', '0');
insert into sys_dict_data
values (14, 1, '通知', '1', 'sys_notice_type', '', 'warning', 'Y', 103, 1, now(), null, null, '通知', '0');
insert into sys_dict_data
values (15, 2, '公告', '2', 'sys_notice_type', '', 'success', 'N', 103, 1, now(), null, null, '公告', '0');
insert into sys_dict_data
values (16, 1, '正常', '0', 'sys_notice_status', '', 'primary', 'Y', 103, 1, now(), null, null, '正常状态', '0');
insert into sys_dict_data
values (17, 2, '关闭', '1', 'sys_notice_status', '', 'danger', 'N', 103, 1, now(), null, null, '关闭状态', '0');
insert into sys_dict_data
values (29, 99, '其他', '0', 'sys_oper_type', '', 'info', 'N', 103, 1, now(), null, null, '其他操作', '0');
insert into sys_dict_data
values (18, 1, '新增', '1', 'sys_oper_type', '', 'info', 'N', 103, 1, now(), null, null, '新增操作', '0');
insert into sys_dict_data
values (19, 2, '修改', '2', 'sys_oper_type', '', 'info', 'N', 103, 1, now(), null, null, '修改操作', '0');
insert into sys_dict_data
values (20, 3, '删除', '3', 'sys_oper_type', '', 'danger', 'N', 103, 1, now(), null, null, '删除操作', '0');
insert into sys_dict_data
values (21, 4, '授权', '4', 'sys_oper_type', '', 'primary', 'N', 103, 1, now(), null, null, '授权操作', '0');
insert into sys_dict_data
values (22, 5, '导出', '5', 'sys_oper_type', '', 'warning', 'N', 103, 1, now(), null, null, '导出操作', '0');
insert into sys_dict_data
values (23, 6, '导入', '6', 'sys_oper_type', '', 'warning', 'N', 103, 1, now(), null, null, '导入操作', '0');
insert into sys_dict_data
values (24, 7, '强退', '7', 'sys_oper_type', '', 'danger', 'N', 103, 1, now(), null, null, '强退操作', '0');
insert into sys_dict_data
values (25, 8, '生成代码', '8', 'sys_oper_type', '', 'warning', 'N', 103, 1, now(), null, null, '生成操作', '0');
insert into sys_dict_data
values (26, 9, '清空数据', '9', 'sys_oper_type', '', 'danger', 'N', 103, 1, now(), null, null, '清空操作', '0');
insert into sys_dict_data
values (27, 1, '成功', '0', 'sys_common_status', '', 'primary', 'N', 103, 1, now(), null, null, '正常状态', '0');
insert into sys_dict_data
values (28, 2, '失败', '1', 'sys_common_status', '', 'danger', 'N', 103, 1, now(), null, null, '停用状态', '0');
insert into sys_dict_data
values (30, 0, '密码认证', 'password', 'sys_grant_type', '', 'default', 'N', 103, 1, now(), null, null, '密码认证',
        '0');
insert into sys_dict_data
values (31, 0, '短信认证', 'sms', 'sys_grant_type', '', 'default', 'N', 103, 1, now(), null, null, '短信认证', '0');
insert into sys_dict_data
values (32, 0, '邮件认证', 'email', 'sys_grant_type', '', 'default', 'N', 103, 1, now(), null, null, '邮件认证', '0');
insert into sys_dict_data
values (33, 0, '小程序认证', 'xcx', 'sys_grant_type', '', 'default', 'N', 103, 1, now(), null, null, '小程序认证', '0');
insert into sys_dict_data
values (34, 0, '三方登录认证', 'social', 'sys_grant_type', '', 'default', 'N', 103, 1, now(), null, null,
        '三方登录认证', '0');
insert into sys_dict_data
values (35, 0, 'PC', 'pc', 'sys_device_type', '', 'default', 'N', 103, 1, now(), null, null, 'PC', '0');
insert into sys_dict_data
values (36, 0, '安卓', 'android', 'sys_device_type', '', 'default', 'N', 103, 1, now(), null, null, '安卓', '0');
insert into sys_dict_data
values (37, 0, 'iOS', 'ios', 'sys_device_type', '', 'default', 'N', 103, 1, now(), null, null, 'iOS', '0');
insert into sys_dict_data
values (38, 0, '小程序', 'xcx', 'sys_device_type', '', 'default', 'N', 103, 1, now(), null, null, '小程序', '0');
insert into sys_dict_data
values (39, 1, '系统', 'system', 'app_category', null, 'default', 'Y', 103, 1, now(), null, null, '系统应用', '0');
insert into sys_dict_data
values (40, 2, '业务', 'business', 'app_category', null, 'default', 'N', 103, 1, now(), null, null, '业务应用', '0');
insert into sys_dict_data
values (41, 3, '工具', 'tool', 'app_category', null, 'default', 'N', 103, 1, now(), null, null, '工具应用', '0');
insert into sys_dict_data
values (42, 4, '第三方', 'third_party', 'app_category', null, 'default', 'N', 103, 1, now(), null, null, '第三方应用', '0');

-- 13、参数配置表
create table if not exists sys_config
(
    config_id int8,
    tenant_id varchar (20) default '000000':: varchar,
    config_name varchar (100) default '':: varchar,
    config_key varchar (100) default '':: varchar,
    config_value varchar (500) default '':: varchar,
    config_type char default 'N'::bpchar,
    create_dept int8,
    create_by int8,
    create_time timestamp,
    update_by int8,
    update_time timestamp,
    remark varchar (500) default null :: varchar,
    app_id varchar (64),
    constraint sys_config_pk primary key (config_id)
);

CREATE INDEX IF NOT EXISTS sys_config_app_id_idx ON sys_config (app_id);

comment on table sys_config is '参数配置表';
comment on column sys_config.config_id is '参数主键';
comment on column sys_config.tenant_id is '租户编号';
comment on column sys_config.config_name is '参数名称';
comment on column sys_config.config_key is '参数键名';
comment on column sys_config.config_value is '参数键值';
comment on column sys_config.config_type is '系统内置（Y是 N否）';
comment on column sys_config.create_dept is '创建部门';
comment on column sys_config.create_by is '创建者';
comment on column sys_config.create_time is '创建时间';
comment on column sys_config.update_by is '更新者';
comment on column sys_config.update_time is '更新时间';
comment on column sys_config.remark is '备注';
comment on column sys_config.app_id is '应用ID';

insert into sys_config
values (1, '000000', '主框架页-默认皮肤样式名称', 'sys.index.skinName', 'skin-blue', 'Y', 103, 1, now(), null, null,
        '蓝色 skin-blue、绿色 skin-green、紫色 skin-purple、红色 skin-red、黄色 skin-yellow', '000000');
insert into sys_config
values (2, '000000', '用户管理-账号初始密码', 'sys.user.initPassword', '123456', 'Y', 103, 1, now(), null, null,
        '初始化密码 123456', '000000');
insert into sys_config
values (3, '000000', '主框架页-侧边栏主题', 'sys.index.sideTheme', 'theme-dark', 'Y', 103, 1, now(), null, null,
        '深色主题theme-dark，浅色主题theme-light', '000000');
insert into sys_config
values (5, '000000', '账号自助-是否开启用户注册功能', 'sys.account.registerUser', 'false', 'Y', 103, 1, now(), null,
        null, '是否开启注册用户功能（true开启，false关闭）', '000000');
insert into sys_config
values (11, '000000', 'OSS预览列表资源开关', 'sys.oss.previewListResource', 'true', 'Y', 103, 1, now(), null, null,
        'true:开启, false:关闭', '000000');

-- 14、系统访问记录
create table if not exists sys_logininfor
(
    info_id int8,
    tenant_id varchar (20) default '000000':: varchar,
    user_name varchar (50) default '':: varchar,
    client_key varchar (32) default '':: varchar,
    device_type varchar (32) default '':: varchar,
    ipaddr varchar (128) default '':: varchar,
    login_location varchar (255) default '':: varchar,
    browser varchar (50) default '':: varchar,
    os varchar (50) default '':: varchar,
    status char default '0'::bpchar,
    msg varchar (255) default '':: varchar,
    login_time timestamp,
    constraint sys_logininfor_pk primary key (info_id)
);

create index idx_sys_logininfor_s ON sys_logininfor (status);
create index idx_sys_logininfor_lt ON sys_logininfor (login_time);

comment on table sys_logininfor is '系统访问记录';
comment on column sys_logininfor.info_id is '访问ID';
comment on column sys_logininfor.tenant_id is '应用编号';
comment on column sys_logininfor.user_name is '用户账号';
comment on column sys_logininfor.client_key is '客户端';
comment on column sys_logininfor.device_type is '设备类型';
comment on column sys_logininfor.ipaddr is '登录IP地址';
comment on column sys_logininfor.login_location is '登录地点';
comment on column sys_logininfor.browser is '浏览器类型';
comment on column sys_logininfor.os is '操作系统';
comment on column sys_logininfor.status is '登录状态（0成功 1失败）';
comment on column sys_logininfor.msg is '提示消息';
comment on column sys_logininfor.login_time is '访问时间';

-- 17、通知公告表
create table if not exists sys_notice
(
    notice_id int8,
    tenant_id varchar (20) default '000000':: varchar,
    notice_title varchar (50) not null,
    notice_type char not null,
    notice_content text,
    status char default '0'::bpchar,
    create_dept int8,
    create_by int8,
    create_time timestamp,
    update_by int8,
    update_time timestamp,
    remark varchar (255) default null :: varchar,
    constraint sys_notice_pk primary key (notice_id)
);

comment on table sys_notice is '通知公告表';
comment on column sys_notice.notice_id is '公告ID';
comment on column sys_notice.tenant_id is '应用编号';
comment on column sys_notice.notice_title is '公告标题';
comment on column sys_notice.notice_type is '公告类型（1通知 2公告）';
comment on column sys_notice.notice_content is '公告内容';
comment on column sys_notice.status is '公告状态（0正常 1关闭）';
comment on column sys_notice.create_dept is '创建部门';
comment on column sys_notice.create_by is '创建者';
comment on column sys_notice.create_time is '创建时间';
comment on column sys_notice.update_by is '更新者';
comment on column sys_notice.update_time is '更新时间';
comment on column sys_notice.remark is '备注';

-- 初始化-公告信息表数据
insert into sys_notice values('1', '000000', '温馨提醒：2018-07-01 新版本发布啦', '2', '新版本内容', '0', 103, 1, now(), null, null, '管理员');
insert into sys_notice
values ('2', '000000', '维护通知：2018-07-01 系统凌晨维护', '1', '维护内容', '0', 103, 1, now(), null, null, '管理员');

-- 18、代码生成业务表
create table if not exists gen_table
(
    table_id int8,
    data_name varchar (200) default '':: varchar,
    table_name varchar (200) default '':: varchar,
    table_comment varchar (500) default '':: varchar,
    sub_table_name varchar (64) default '':: varchar,
    sub_table_fk_name varchar (64) default '':: varchar,
    class_name varchar (100) default '':: varchar,
    tpl_category varchar (200) default 'crud':: varchar,
    package_name varchar (100) default null :: varchar,
    module_name varchar (30) default null :: varchar,
    business_name varchar (30) default null :: varchar,
    function_name varchar (50) default null :: varchar,
    function_author varchar (50) default null :: varchar,
    gen_type char default '0'::bpchar not null,
    gen_path varchar (200) default '/':: varchar,
    options varchar (1000) default null :: varchar,
    create_dept int8,
    create_by int8,
    create_time timestamp,
    update_by int8,
    update_time timestamp,
    remark varchar (500) default null :: varchar,
    constraint gen_table_pk primary key (table_id)
);

comment on table gen_table is '代码生成业务表';
comment on column gen_table.table_id is '编号';
comment on column gen_table.data_name is '数据源名称';
comment on column gen_table.table_name is '表名称';
comment on column gen_table.table_comment is '表描述';
comment on column gen_table.sub_table_name is '关联子表的表名';
comment on column gen_table.sub_table_fk_name is '子表关联的外键名';
comment on column gen_table.class_name is '实体类名称';
comment on column gen_table.tpl_category is '使用的模板（CRUD单表操作 TREE树表操作）';
comment on column gen_table.package_name is '生成包路径';
comment on column gen_table.module_name is '生成模块名';
comment on column gen_table.business_name is '生成业务名';
comment on column gen_table.function_name is '生成功能名';
comment on column gen_table.function_author is '生成功能作者';
comment on column gen_table.gen_type is '生成代码方式（0zip压缩包 1自定义路径）';
comment on column gen_table.gen_path is '生成路径（不填默认项目路径）';
comment on column gen_table.options is '其它生成选项';
comment on column gen_table.create_dept is '创建部门';
comment on column gen_table.create_by is '创建者';
comment on column gen_table.create_time is '创建时间';
comment on column gen_table.update_by is '更新者';
comment on column gen_table.update_time is '更新时间';
comment on column gen_table.remark is '备注';

-- 19、代码生成业务表字段
create table if not exists gen_table_column
(
    column_id int8,
    table_id int8,
    column_name varchar (200) default null :: varchar,
    column_comment varchar (500) default null :: varchar,
    column_type varchar (100) default null :: varchar,
    java_type varchar (500) default null :: varchar,
    java_field varchar (200) default null :: varchar,
    is_pk char default null ::bpchar,
    is_increment char default null ::bpchar,
    is_required char default null ::bpchar,
    is_insert char default null ::bpchar,
    is_edit char default null ::bpchar,
    is_list char default null ::bpchar,
    is_query char default null ::bpchar,
    query_type varchar (200) default 'EQ':: varchar,
    html_type varchar (200) default null :: varchar,
    dict_type varchar (200) default '':: varchar,
    sort int4,
    create_dept int8,
    create_by int8,
    create_time timestamp,
    update_by int8,
    update_time timestamp,
    constraint gen_table_column_pk primary key (column_id)
);

comment on table gen_table_column is '代码生成业务表字段';
comment on column gen_table_column.column_id is '编号';
comment on column gen_table_column.table_id is '归属表编号';
comment on column gen_table_column.column_name is '列名称';
comment on column gen_table_column.column_comment is '列描述';
comment on column gen_table_column.column_type is '列类型';
comment on column gen_table_column.java_type is 'JAVA类型';
comment on column gen_table_column.java_field is 'JAVA字段名';
comment on column gen_table_column.is_pk is '是否主键（1是）';
comment on column gen_table_column.is_increment is '是否自增（1是）';
comment on column gen_table_column.is_required is '是否必填（1是）';
comment on column gen_table_column.is_insert is '是否为插入字段（1是）';
comment on column gen_table_column.is_edit is '是否编辑字段（1是）';
comment on column gen_table_column.is_list is '是否列表字段（1是）';
comment on column gen_table_column.is_query is '是否查询字段（1是）';
comment on column gen_table_column.query_type is '查询方式（等于、不等于、大于、小于、范围）';
comment on column gen_table_column.html_type is '显示类型（文本框、文本域、下拉框、复选框、单选框、日期控件）';
comment on column gen_table_column.dict_type is '字典类型';
comment on column gen_table_column.sort is '排序';
comment on column gen_table_column.create_dept is '创建部门';
comment on column gen_table_column.create_by is '创建者';
comment on column gen_table_column.create_time is '创建时间';
comment on column gen_table_column.update_by is '更新者';
comment on column gen_table_column.update_time is '更新时间';

-- OSS对象存储表
create table if not exists sys_oss
(
    oss_id int8,
    tenant_id varchar (20) default '000000':: varchar,
    file_name varchar (255) default '':: varchar not null,
    original_name varchar (255) default '':: varchar not null,
    file_suffix varchar (10) default '':: varchar not null,
    url varchar (500) default '':: varchar not null,
    ext1 varchar (500) default '':: varchar,
    create_dept int8,
    create_by int8,
    create_time timestamp,
    update_by int8,
    update_time timestamp,
    service varchar (20) default 'minio':: varchar,
    constraint sys_oss_pk primary key (oss_id)
);

comment on table sys_oss is 'OSS对象存储表';
comment on column sys_oss.oss_id is '对象存储主键';
comment on column sys_oss.tenant_id is '应用编码';
comment on column sys_oss.file_name is '文件名';
comment on column sys_oss.original_name is '原名';
comment on column sys_oss.file_suffix is '文件后缀名';
comment on column sys_oss.url is 'URL地址';
comment on column sys_oss.ext1 is '扩展字段';
comment on column sys_oss.create_by is '上传人';
comment on column sys_oss.create_dept is '创建部门';
comment on column sys_oss.create_time is '创建时间';
comment on column sys_oss.update_by is '更新者';
comment on column sys_oss.update_time is '更新时间';
comment on column sys_oss.service is '服务商';

-- OSS对象存储动态配置表
create table if not exists sys_oss_config
(
    oss_config_id int8,
    tenant_id varchar (20) default '000000':: varchar,
    config_key varchar (20) default '':: varchar not null,
    access_key varchar (255) default '':: varchar,
    secret_key varchar (255) default '':: varchar,
    bucket_name varchar (255) default '':: varchar,
    prefix varchar (255) default '':: varchar,
    endpoint varchar (255) default '':: varchar,
    domain varchar (255) default '':: varchar,
    is_https char default 'N'::bpchar,
    region varchar (255) default '':: varchar,
    access_policy char (1) default '1'::bpchar not null,
    status char default '1'::bpchar,
    ext1 varchar (255) default '':: varchar,
    create_dept int8,
    create_by int8,
    create_time timestamp,
    update_by int8,
    update_time timestamp,
    remark varchar (500) default '':: varchar,
    constraint sys_oss_config_pk primary key (oss_config_id)
);

comment on table sys_oss_config is '对象存储配置表';
comment on column sys_oss_config.oss_config_id is '主键';
comment on column sys_oss_config.tenant_id is '应用编码';
comment on column sys_oss_config.config_key is '配置key';
comment on column sys_oss_config.access_key is 'accessKey';
comment on column sys_oss_config.secret_key is '秘钥';
comment on column sys_oss_config.bucket_name is '桶名称';
comment on column sys_oss_config.prefix is '前缀';
comment on column sys_oss_config.endpoint is '访问站点';
comment on column sys_oss_config.domain is '自定义域名';
comment on column sys_oss_config.is_https is '是否https（Y=是,N=否）';
comment on column sys_oss_config.region is '域';
comment on column sys_oss_config.access_policy is '桶权限类型(0=private 1=public 2=custom)';
comment on column sys_oss_config.status is '是否默认（0=是,1=否）';
comment on column sys_oss_config.ext1 is '扩展字段';
comment on column sys_oss_config.create_dept is '创建部门';
comment on column sys_oss_config.create_by is '创建者';
comment on column sys_oss_config.create_time is '创建时间';
comment on column sys_oss_config.update_by is '更新者';
comment on column sys_oss_config.update_time is '更新时间';
comment on column sys_oss_config.remark is '备注';

insert into sys_oss_config
values (1, '000000', 'minio', 'ruoyi', 'ruoyi123', 'ruoyi', '', '127.0.0.1:9000', '', 'N', '', '1', '1', '', 103, 1,
        now(), 1, now(), null);
insert into sys_oss_config
values (2, '000000', 'qiniu', 'XXXXXXXXXXXXXXX', 'XXXXXXXXXXXXXXX', 'ruoyi', '', 's3-cn-north-1.qiniucs.com', '', 'N',
        '', '1', '1', '', 103, 1, now(), 1, now(), null);
insert into sys_oss_config
values (3, '000000', 'aliyun', 'XXXXXXXXXXXXXXX', 'XXXXXXXXXXXXXXX', 'ruoyi', '', 'oss-cn-beijing.aliyuncs.com', '',
        'N', '', '1', '1', '', 103, 1, now(), 1, now(), null);
insert into sys_oss_config
values (4, '000000', 'qcloud', 'XXXXXXXXXXXXXXX', 'XXXXXXXXXXXXXXX', 'ruoyi-1240000000', '',
        'cos.ap-beijing.myqcloud.com', '', 'N', 'ap-beijing', '1', '1', '', 103, 1, now(), 1, now(), null);
insert into sys_oss_config
values (5, '000000', 'image', 'ruoyi', 'ruoyi123', 'ruoyi', 'image', '127.0.0.1:9000', '', 'N', '', '1', '1', '', 103,
        1, now(), 1, now(), NULL);
insert into sys_oss_config
values (6, '000000', 'local', '', '', '', 'upload', 'local', '', 'N', '', '1', '0', '', 103,
        1, now(), 1, now(), '本地文件存储');

-- 系统授权表
create table sys_client (
    id             int8,
    client_id      varchar(64)  default ''::varchar,
    client_key     varchar(32)  default ''::varchar,
    client_secret  varchar(255) default ''::varchar,
    grant_type     varchar(255) default ''::varchar,
    device_type    varchar(32)  default ''::varchar,
    active_timeout int4         default 1800,
    timeout        int4         default 604800,
    status         char(1)      default '0'::bpchar,
    del_flag       char(1)      default '0'::bpchar,
    create_dept    int8,
    create_by      int8,
    create_time    timestamp,
    update_by      int8,
    update_time    timestamp,
    constraint sys_client_pk primary key (id)
);

comment on table sys_client is '系统授权表';
comment on column sys_client.id is '主键';
comment on column sys_client.client_id is '客户端id';
comment on column sys_client.client_key is '客户端key';
comment on column sys_client.client_secret is '客户端秘钥';
comment on column sys_client.grant_type is '授权类型';
comment on column sys_client.device_type is '设备类型';
comment on column sys_client.active_timeout is 'token活跃超时时间';
comment on column sys_client.timeout is 'token固定超时';
comment on column sys_client.status is '状态（0正常 1停用）';
comment on column sys_client.del_flag is '删除标志（0代表存在 1代表删除）';
comment on column sys_client.create_dept is '创建部门';
comment on column sys_client.create_by is '创建者';
comment on column sys_client.create_time is '创建时间';
comment on column sys_client.update_by is '更新者';
comment on column sys_client.update_time is '更新时间';

insert into sys_client
values (1, 'e5cd7e4891bf95d1d19206ce24a7b32e', 'pc', 'pc123', 'password,social', 'pc', 1800, 604800, 0, 0, 103, 1,
        now(), 1, now());
insert into sys_client
values (2, '428a8310cd442757ae699df5d894f051', 'app', 'app123', 'password,sms,social', 'android', 1800, 604800, 0, 0,
        103, 1, now(), 1, now());

-- 字符串自动转时间 避免框架时间查询报错问题
create or replace function cast_varchar_to_timestamp(varchar) returns timestamptz as $$
select to_timestamp($1, 'yyyy-mm-dd hh24:mi:ss');
$$ language sql strict;

drop cast if exists (varchar as timestamptz);

create cast (varchar as timestamptz) with function cast_varchar_to_timestamp as IMPLICIT;
-- ============================================================
-- Source: postgres_ry_job.sql (SnailJob 表结构及初始数据)
-- ============================================================

-- sj_namespace
CREATE TABLE sj_namespace
(
    id          bigserial PRIMARY KEY,
    name        varchar(64)  NOT NULL,
    unique_id   varchar(64)  NOT NULL,
    description varchar(256) NOT NULL DEFAULT '',
    deleted     smallint     NOT NULL DEFAULT 0,
    create_dt   timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt   timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sj_namespace_01 ON sj_namespace (name);

COMMENT ON COLUMN sj_namespace.id IS '主键';
COMMENT ON COLUMN sj_namespace.name IS '名称';
COMMENT ON COLUMN sj_namespace.unique_id IS '唯一id';
COMMENT ON COLUMN sj_namespace.description IS '描述';
COMMENT ON COLUMN sj_namespace.deleted IS '逻辑删除 1、删除';
COMMENT ON COLUMN sj_namespace.create_dt IS '创建时间';
COMMENT ON COLUMN sj_namespace.update_dt IS '修改时间';
COMMENT ON TABLE sj_namespace IS '命名空间';

INSERT INTO sj_namespace
VALUES (1, 'Development', 'dev', '', 0, now(), now());
INSERT INTO sj_namespace
VALUES (2, 'Production', 'prod', '', 0, now(), now());

-- sj_group_config
CREATE TABLE sj_group_config
(
    id                bigserial PRIMARY KEY,
    namespace_id      varchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name        varchar(64)  NOT NULL DEFAULT '',
    description       varchar(256) NOT NULL DEFAULT '',
    token             varchar(64)  NOT NULL DEFAULT 'SJ_cKqBTPzCsWA3VyuCfFoccmuIEGXjr5KT',
    group_status      smallint     NOT NULL DEFAULT 0,
    version           int          NOT NULL,
    group_partition   int          NOT NULL,
    id_generator_mode smallint     NOT NULL DEFAULT 1,
    init_scene        smallint     NOT NULL DEFAULT 0,
    create_dt         timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt         timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_sj_group_config_01 ON sj_group_config (namespace_id, group_name);

COMMENT ON COLUMN sj_group_config.id IS '主键';
COMMENT ON COLUMN sj_group_config.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_group_config.group_name IS '组名称';
COMMENT ON COLUMN sj_group_config.description IS '组描述';
COMMENT ON COLUMN sj_group_config.token IS 'token';
COMMENT ON COLUMN sj_group_config.group_status IS '组状态 0、未启用 1、启用';
COMMENT ON COLUMN sj_group_config.version IS '版本号';
COMMENT ON COLUMN sj_group_config.group_partition IS '分区';
COMMENT ON COLUMN sj_group_config.id_generator_mode IS '唯一id生成模式 默认号段模式';
COMMENT ON COLUMN sj_group_config.init_scene IS '是否初始化场景 0:否 1:是';
COMMENT ON COLUMN sj_group_config.create_dt IS '创建时间';
COMMENT ON COLUMN sj_group_config.update_dt IS '修改时间';
COMMENT ON TABLE sj_group_config IS '组配置';

INSERT INTO sj_group_config
VALUES (1, 'dev', 'ruoyi_group', '', 'SJ_cKqBTPzCsWA3VyuCfFoccmuIEGXjr5KT', 1, 1, 0, 1, 1, now(), now());
INSERT INTO sj_group_config
VALUES (2, 'prod', 'ruoyi_group', '', 'SJ_cKqBTPzCsWA3VyuCfFoccmuIEGXjr5KT', 1, 1, 0, 1, 1, now(), now());

-- sj_notify_config
CREATE TABLE sj_notify_config
(
    id                     bigserial PRIMARY KEY,
    namespace_id           varchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name             varchar(64)  NOT NULL,
    notify_name            varchar(64)  NOT NULL DEFAULT '',
    system_task_type       smallint     NOT NULL DEFAULT 3,
    notify_status          smallint     NOT NULL DEFAULT 0,
    recipient_ids          varchar(128) NOT NULL,
    notify_threshold       int          NOT NULL DEFAULT 0,
    notify_scene           smallint     NOT NULL DEFAULT 0,
    rate_limiter_status    smallint     NOT NULL DEFAULT 0,
    rate_limiter_threshold int          NOT NULL DEFAULT 0,
    description            varchar(256) NOT NULL DEFAULT '',
    create_dt              timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt              timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sj_notify_config_01 ON sj_notify_config (namespace_id, group_name);

COMMENT ON COLUMN sj_notify_config.id IS '主键';
COMMENT ON COLUMN sj_notify_config.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_notify_config.group_name IS '组名称';
COMMENT ON COLUMN sj_notify_config.notify_name IS '通知名称';
COMMENT ON COLUMN sj_notify_config.system_task_type IS '任务类型 1. 重试任务 2. 重试回调 3、JOB任务 4、WORKFLOW任务';
COMMENT ON COLUMN sj_notify_config.notify_status IS '通知状态 0、未启用 1、启用';
COMMENT ON COLUMN sj_notify_config.recipient_ids IS '接收人id列表';
COMMENT ON COLUMN sj_notify_config.notify_threshold IS '通知阈值';
COMMENT ON COLUMN sj_notify_config.notify_scene IS '通知场景';
COMMENT ON COLUMN sj_notify_config.rate_limiter_status IS '限流状态 0、未启用 1、启用';
COMMENT ON COLUMN sj_notify_config.rate_limiter_threshold IS '每秒限流阈值';
COMMENT ON COLUMN sj_notify_config.description IS '描述';
COMMENT ON COLUMN sj_notify_config.create_dt IS '创建时间';
COMMENT ON COLUMN sj_notify_config.update_dt IS '修改时间';
COMMENT ON TABLE sj_notify_config IS '通知配置';

-- sj_notify_recipient
CREATE TABLE sj_notify_recipient
(
    id               bigserial PRIMARY KEY,
    namespace_id     varchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    recipient_name   varchar(64)  NOT NULL,
    notify_type      smallint     NOT NULL DEFAULT 0,
    notify_attribute varchar(512) NOT NULL,
    description      varchar(256) NOT NULL DEFAULT '',
    create_dt        timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt        timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sj_notify_recipient_01 ON sj_notify_recipient (namespace_id);

COMMENT ON COLUMN sj_notify_recipient.id IS '主键';
COMMENT ON COLUMN sj_notify_recipient.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_notify_recipient.recipient_name IS '接收人名称';
COMMENT ON COLUMN sj_notify_recipient.notify_type IS '通知类型 1、钉钉 2、邮件 3、企业微信 4 飞书 5 webhook';
COMMENT ON COLUMN sj_notify_recipient.notify_attribute IS '配置属性';
COMMENT ON COLUMN sj_notify_recipient.description IS '描述';
COMMENT ON COLUMN sj_notify_recipient.create_dt IS '创建时间';
COMMENT ON COLUMN sj_notify_recipient.update_dt IS '修改时间';
COMMENT ON TABLE sj_notify_recipient IS '告警通知接收人';

-- sj_retry_dead_letter
CREATE TABLE sj_retry_dead_letter
(
    id              bigserial PRIMARY KEY,
    namespace_id    varchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name      varchar(64)  NOT NULL,
    group_id        bigint       NOT NULL,
    scene_name      varchar(64)  NOT NULL,
    scene_id        bigint       NOT NULL,
    idempotent_id   varchar(64)  NOT NULL,
    biz_no          varchar(64)  NOT NULL DEFAULT '',
    executor_name   varchar(512) NOT NULL DEFAULT '',
    serializer_name varchar(32)  NOT NULL DEFAULT 'jackson',
    args_str        text         NOT NULL,
    ext_attrs       text         NOT NULL,
    create_dt       timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sj_retry_dead_letter_01 ON sj_retry_dead_letter (namespace_id, group_name, scene_name);
CREATE INDEX idx_sj_retry_dead_letter_02 ON sj_retry_dead_letter (idempotent_id);
CREATE INDEX idx_sj_retry_dead_letter_03 ON sj_retry_dead_letter (biz_no);
CREATE INDEX idx_sj_retry_dead_letter_04 ON sj_retry_dead_letter (create_dt);

COMMENT ON COLUMN sj_retry_dead_letter.id IS '主键';
COMMENT ON COLUMN sj_retry_dead_letter.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_retry_dead_letter.group_name IS '组名称';
COMMENT ON COLUMN sj_retry_dead_letter.group_id IS '组Id';
COMMENT ON COLUMN sj_retry_dead_letter.scene_name IS '场景名称';
COMMENT ON COLUMN sj_retry_dead_letter.scene_id IS '场景ID';
COMMENT ON COLUMN sj_retry_dead_letter.idempotent_id IS '幂等id';
COMMENT ON COLUMN sj_retry_dead_letter.biz_no IS '业务编号';
COMMENT ON COLUMN sj_retry_dead_letter.executor_name IS '执行器名称';
COMMENT ON COLUMN sj_retry_dead_letter.serializer_name IS '执行方法参数序列化器名称';
COMMENT ON COLUMN sj_retry_dead_letter.args_str IS '执行方法参数';
COMMENT ON COLUMN sj_retry_dead_letter.ext_attrs IS '扩展字段';
COMMENT ON COLUMN sj_retry_dead_letter.create_dt IS '创建时间';
COMMENT ON TABLE sj_retry_dead_letter IS '死信队列表';

-- sj_retry
CREATE TABLE sj_retry
(
    id              bigserial PRIMARY KEY,
    namespace_id    varchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name      varchar(64)  NOT NULL,
    group_id        bigint       NOT NULL,
    scene_name      varchar(64)  NOT NULL,
    scene_id        bigint       NOT NULL,
    idempotent_id   varchar(64)  NOT NULL,
    biz_no          varchar(64)  NOT NULL DEFAULT '',
    executor_name   varchar(512) NOT NULL DEFAULT '',
    args_str        text         NOT NULL,
    ext_attrs       text         NOT NULL,
    serializer_name varchar(32)  NOT NULL DEFAULT 'jackson',
    next_trigger_at bigint       NOT NULL,
    retry_count     int          NOT NULL DEFAULT 0,
    retry_status    smallint     NOT NULL DEFAULT 0,
    task_type       smallint     NOT NULL DEFAULT 1,
    bucket_index    int          NOT NULL DEFAULT 0,
    parent_id       bigint       NOT NULL DEFAULT 0,
    deleted         bigint       NOT NULL DEFAULT 0,
    create_dt       timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt       timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_sj_retry_01 ON sj_retry (scene_id, task_type, idempotent_id, deleted);
CREATE INDEX idx_sj_retry_01 ON sj_retry (biz_no);
CREATE INDEX idx_sj_retry_02 ON sj_retry (idempotent_id);
CREATE INDEX idx_sj_retry_03 ON sj_retry (retry_status, bucket_index);
CREATE INDEX idx_sj_retry_04 ON sj_retry (parent_id);
CREATE INDEX idx_sj_retry_05 ON sj_retry (create_dt);

COMMENT ON COLUMN sj_retry.id IS '主键';
COMMENT ON COLUMN sj_retry.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_retry.group_name IS '组名称';
COMMENT ON COLUMN sj_retry.group_id IS '组Id';
COMMENT ON COLUMN sj_retry.scene_name IS '场景名称';
COMMENT ON COLUMN sj_retry.scene_id IS '场景ID';
COMMENT ON COLUMN sj_retry.idempotent_id IS '幂等id';
COMMENT ON COLUMN sj_retry.biz_no IS '业务编号';
COMMENT ON COLUMN sj_retry.executor_name IS '执行器名称';
COMMENT ON COLUMN sj_retry.args_str IS '执行方法参数';
COMMENT ON COLUMN sj_retry.ext_attrs IS '扩展字段';
COMMENT ON COLUMN sj_retry.serializer_name IS '执行方法参数序列化器名称';
COMMENT ON COLUMN sj_retry.next_trigger_at IS '下次触发时间';
COMMENT ON COLUMN sj_retry.retry_count IS '重试次数';
COMMENT ON COLUMN sj_retry.retry_status IS '重试状态 0、重试中 1、成功 2、最大重试次数';
COMMENT ON COLUMN sj_retry.task_type IS '任务类型 1、重试数据 2、回调数据';
COMMENT ON COLUMN sj_retry.bucket_index IS 'bucket';
COMMENT ON COLUMN sj_retry.parent_id IS '父节点id';
COMMENT ON COLUMN sj_retry.deleted IS '逻辑删除';
COMMENT ON COLUMN sj_retry.create_dt IS '创建时间';
COMMENT ON COLUMN sj_retry.update_dt IS '修改时间';
COMMENT ON TABLE sj_retry IS '重试信息表';

-- sj_retry_task
CREATE TABLE sj_retry_task
(
    id               bigserial PRIMARY KEY,
    namespace_id     varchar(64) NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name       varchar(64) NOT NULL,
    scene_name       varchar(64) NOT NULL,
    retry_id         bigint      NOT NULL,
    ext_attrs        text        NOT NULL,
    task_status      smallint    NOT NULL DEFAULT 1,
    task_type        smallint    NOT NULL DEFAULT 1,
    operation_reason smallint    NOT NULL DEFAULT 0,
    client_info      varchar(128) NULL     DEFAULT NULL,
    create_dt        timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt        timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sj_retry_task_01 ON sj_retry_task (namespace_id, group_name, scene_name);
CREATE INDEX idx_sj_retry_task_02 ON sj_retry_task (task_status);
CREATE INDEX idx_sj_retry_task_03 ON sj_retry_task (create_dt);
CREATE INDEX idx_sj_retry_task_04 ON sj_retry_task (retry_id);

COMMENT ON COLUMN sj_retry_task.id IS '主键';
COMMENT ON COLUMN sj_retry_task.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_retry_task.group_name IS '组名称';
COMMENT ON COLUMN sj_retry_task.scene_name IS '场景名称';
COMMENT ON COLUMN sj_retry_task.retry_id IS '重试信息Id';
COMMENT ON COLUMN sj_retry_task.ext_attrs IS '扩展字段';
COMMENT ON COLUMN sj_retry_task.task_status IS '重试状态';
COMMENT ON COLUMN sj_retry_task.task_type IS '任务类型 1、重试数据 2、回调数据';
COMMENT ON COLUMN sj_retry_task.operation_reason IS '操作原因';
COMMENT ON COLUMN sj_retry_task.client_info IS '客户端地址 clientId#ip:port';
COMMENT ON COLUMN sj_retry_task.create_dt IS '创建时间';
COMMENT ON COLUMN sj_retry_task.update_dt IS '修改时间';
COMMENT ON TABLE sj_retry_task IS '重试任务表';

-- sj_retry_task_log_message
CREATE TABLE sj_retry_task_log_message
(
    id            bigserial PRIMARY KEY,
    namespace_id  varchar(64) NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name    varchar(64) NOT NULL,
    retry_id      bigint      NOT NULL,
    retry_task_id bigint      NOT NULL,
    message       text        NOT NULL,
    log_num       int         NOT NULL DEFAULT 1,
    real_time     bigint      NOT NULL DEFAULT 0,
    create_dt     timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sj_retry_task_log_message_01 ON sj_retry_task_log_message (namespace_id, group_name, retry_task_id);
CREATE INDEX idx_sj_retry_task_log_message_02 ON sj_retry_task_log_message (create_dt);

COMMENT ON COLUMN sj_retry_task_log_message.id IS '主键';
COMMENT ON COLUMN sj_retry_task_log_message.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_retry_task_log_message.group_name IS '组名称';
COMMENT ON COLUMN sj_retry_task_log_message.retry_id IS '重试信息Id';
COMMENT ON COLUMN sj_retry_task_log_message.retry_task_id IS '重试任务Id';
COMMENT ON COLUMN sj_retry_task_log_message.message IS '异常信息';
COMMENT ON COLUMN sj_retry_task_log_message.log_num IS '日志数量';
COMMENT ON COLUMN sj_retry_task_log_message.real_time IS '上报时间';
COMMENT ON COLUMN sj_retry_task_log_message.create_dt IS '创建时间';
COMMENT ON TABLE sj_retry_task_log_message IS '任务调度日志信息记录表';

-- sj_retry_scene_config
CREATE TABLE sj_retry_scene_config
(
    id                  bigserial PRIMARY KEY,
    namespace_id        varchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    scene_name          varchar(64)  NOT NULL,
    group_name          varchar(64)  NOT NULL,
    scene_status        smallint     NOT NULL DEFAULT 0,
    max_retry_count     int          NOT NULL DEFAULT 5,
    back_off            smallint     NOT NULL DEFAULT 1,
    trigger_interval    varchar(16)  NOT NULL DEFAULT '',
    notify_ids          varchar(128) NOT NULL DEFAULT '',
    deadline_request    bigint       NOT NULL DEFAULT 60000,
    executor_timeout    int          NOT NULL DEFAULT 5,
    route_key           smallint     NOT NULL DEFAULT 4,
    block_strategy      smallint     NOT NULL DEFAULT 1,
    cb_status           smallint     NOT NULL DEFAULT 0,
    cb_trigger_type     smallint     NOT NULL DEFAULT 1,
    cb_max_count        int          NOT NULL DEFAULT 16,
    cb_trigger_interval varchar(16)  NOT NULL DEFAULT '',
    owner_id            bigint NULL     DEFAULT NULL,
    labels              varchar(512) NULL     DEFAULT '',
    description         varchar(256) NOT NULL DEFAULT '',
    create_dt           timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt           timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_sj_retry_scene_config_01 ON sj_retry_scene_config (namespace_id, group_name, scene_name);

COMMENT ON COLUMN sj_retry_scene_config.id IS '主键';
COMMENT ON COLUMN sj_retry_scene_config.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_retry_scene_config.scene_name IS '场景名称';
COMMENT ON COLUMN sj_retry_scene_config.group_name IS '组名称';
COMMENT ON COLUMN sj_retry_scene_config.scene_status IS '组状态 0、未启用 1、启用';
COMMENT ON COLUMN sj_retry_scene_config.max_retry_count IS '最大重试次数';
COMMENT ON COLUMN sj_retry_scene_config.back_off IS '1、默认等级 2、固定间隔时间 3、CRON 表达式';
COMMENT ON COLUMN sj_retry_scene_config.trigger_interval IS '间隔时长';
COMMENT ON COLUMN sj_retry_scene_config.notify_ids IS '通知告警场景配置id列表';
COMMENT ON COLUMN sj_retry_scene_config.deadline_request IS 'Deadline Request 调用链超时 单位毫秒';
COMMENT ON COLUMN sj_retry_scene_config.executor_timeout IS '任务执行超时时间，单位秒';
COMMENT ON COLUMN sj_retry_scene_config.route_key IS '路由策略';
COMMENT ON COLUMN sj_retry_scene_config.block_strategy IS '阻塞策略 1、丢弃 2、覆盖 3、并行';
COMMENT ON COLUMN sj_retry_scene_config.cb_status IS '回调状态 0、不开启 1、开启';
COMMENT ON COLUMN sj_retry_scene_config.cb_trigger_type IS '1、默认等级 2、固定间隔时间 3、CRON 表达式';
COMMENT ON COLUMN sj_retry_scene_config.cb_max_count IS '回调的最大执行次数';
COMMENT ON COLUMN sj_retry_scene_config.cb_trigger_interval IS '回调的最大执行次数';
COMMENT ON COLUMN sj_retry_scene_config.owner_id IS '负责人id';
COMMENT ON COLUMN sj_retry_scene_config.labels IS '标签';
COMMENT ON COLUMN sj_retry_scene_config.description IS '描述';
COMMENT ON COLUMN sj_retry_scene_config.create_dt IS '创建时间';
COMMENT ON COLUMN sj_retry_scene_config.update_dt IS '修改时间';
COMMENT ON TABLE sj_retry_scene_config IS '场景配置';

-- sj_server_node
CREATE TABLE sj_server_node
(
    id           bigserial PRIMARY KEY,
    namespace_id varchar(64) NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name   varchar(64) NOT NULL,
    host_id      varchar(64) NOT NULL,
    host_ip      varchar(64) NOT NULL,
    host_port    int         NOT NULL,
    expire_at    timestamp   NOT NULL,
    node_type    smallint    NOT NULL,
    ext_attrs    varchar(256) NULL     DEFAULT '',
    labels       varchar(512) NULL     DEFAULT '',
    create_dt    timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt    timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_sj_server_node_01 ON sj_server_node (host_id, host_ip);
CREATE INDEX idx_sj_server_node_01 ON sj_server_node (namespace_id, group_name);
CREATE INDEX idx_sj_server_node_02 ON sj_server_node (expire_at, node_type);

COMMENT ON COLUMN sj_server_node.id IS '主键';
COMMENT ON COLUMN sj_server_node.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_server_node.group_name IS '组名称';
COMMENT ON COLUMN sj_server_node.host_id IS '主机id';
COMMENT ON COLUMN sj_server_node.host_ip IS '机器ip';
COMMENT ON COLUMN sj_server_node.host_port IS '机器端口';
COMMENT ON COLUMN sj_server_node.expire_at IS '过期时间';
COMMENT ON COLUMN sj_server_node.node_type IS '节点类型 1、客户端 2、是服务端';
COMMENT ON COLUMN sj_server_node.ext_attrs IS '扩展字段';
COMMENT ON COLUMN sj_server_node.labels IS '标签';
COMMENT ON COLUMN sj_server_node.create_dt IS '创建时间';
COMMENT ON COLUMN sj_server_node.update_dt IS '修改时间';
COMMENT ON TABLE sj_server_node IS '服务器节点';

-- sj_distributed_lock
CREATE TABLE sj_distributed_lock
(
    name       varchar(64)  NOT NULL PRIMARY KEY,
    lock_until timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    locked_at  timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    locked_by  varchar(255) NOT NULL,
    create_dt  timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt  timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON COLUMN sj_distributed_lock.name IS '锁名称';
COMMENT ON COLUMN sj_distributed_lock.lock_until IS '锁定时长';
COMMENT ON COLUMN sj_distributed_lock.locked_at IS '锁定时间';
COMMENT ON COLUMN sj_distributed_lock.locked_by IS '锁定者';
COMMENT ON COLUMN sj_distributed_lock.create_dt IS '创建时间';
COMMENT ON COLUMN sj_distributed_lock.update_dt IS '修改时间';
COMMENT ON TABLE sj_distributed_lock IS '锁定表';

-- sj_system_user
CREATE TABLE sj_system_user
(
    id        bigserial PRIMARY KEY,
    username  varchar(64)  NOT NULL,
    password  varchar(128) NOT NULL,
    role      smallint     NOT NULL DEFAULT 0,
    create_dt timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON COLUMN sj_system_user.id IS '主键';
COMMENT ON COLUMN sj_system_user.username IS '账号';
COMMENT ON COLUMN sj_system_user.password IS '密码';
COMMENT ON COLUMN sj_system_user.role IS '角色：1-普通用户、2-管理员';
COMMENT ON COLUMN sj_system_user.create_dt IS '创建时间';
COMMENT ON COLUMN sj_system_user.update_dt IS '修改时间';
COMMENT ON TABLE sj_system_user IS '系统用户表';

INSERT INTO sj_system_user (username, password, role)
VALUES ('admin', '465c194afb65670f38322df087f0a9bb225cc257e43eb4ac5a0c98ef5b3173ac', 2);

-- sj_system_user_permission
CREATE TABLE sj_system_user_permission
(
    id             bigserial PRIMARY KEY,
    group_name     varchar(64) NOT NULL,
    namespace_id   varchar(64) NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    system_user_id bigint      NOT NULL,
    create_dt      timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt      timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_sj_system_user_permission_01 ON sj_system_user_permission (namespace_id, group_name, system_user_id);

COMMENT ON COLUMN sj_system_user_permission.id IS '主键';
COMMENT ON COLUMN sj_system_user_permission.group_name IS '组名称';
COMMENT ON COLUMN sj_system_user_permission.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_system_user_permission.system_user_id IS '系统用户id';
COMMENT ON COLUMN sj_system_user_permission.create_dt IS '创建时间';
COMMENT ON COLUMN sj_system_user_permission.update_dt IS '修改时间';
COMMENT ON TABLE sj_system_user_permission IS '系统用户权限表';

-- sj_job
CREATE TABLE sj_job
(
    id               bigserial PRIMARY KEY,
    namespace_id     varchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name       varchar(64)  NOT NULL,
    job_name         varchar(64)  NOT NULL,
    args_str         text NULL     DEFAULT NULL,
    args_type        smallint     NOT NULL DEFAULT 1,
    next_trigger_at  bigint       NOT NULL,
    job_status       smallint     NOT NULL DEFAULT 1,
    task_type        smallint     NOT NULL DEFAULT 1,
    route_key        smallint     NOT NULL DEFAULT 4,
    executor_type    smallint     NOT NULL DEFAULT 1,
    executor_info    varchar(255) NULL     DEFAULT NULL,
    trigger_type     smallint     NOT NULL,
    trigger_interval varchar(255) NOT NULL,
    block_strategy   smallint     NOT NULL DEFAULT 1,
    executor_timeout int          NOT NULL DEFAULT 0,
    max_retry_times  int          NOT NULL DEFAULT 0,
    parallel_num     int          NOT NULL DEFAULT 1,
    retry_interval   int          NOT NULL DEFAULT 0,
    bucket_index     int          NOT NULL DEFAULT 0,
    resident         smallint     NOT NULL DEFAULT 0,
    notify_ids       varchar(128) NOT NULL DEFAULT '',
    owner_id         bigint NULL     DEFAULT NULL,
    labels           varchar(512) NULL     DEFAULT '',
    description      varchar(256) NOT NULL DEFAULT '',
    ext_attrs        varchar(256) NULL     DEFAULT '',
    deleted          smallint     NOT NULL DEFAULT 0,
    create_dt        timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt        timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sj_job_01 ON sj_job (namespace_id, group_name);
CREATE INDEX idx_sj_job_02 ON sj_job (job_status, bucket_index);
CREATE INDEX idx_sj_job_03 ON sj_job (create_dt);

COMMENT ON COLUMN sj_job.id IS '主键';
COMMENT ON COLUMN sj_job.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_job.group_name IS '组名称';
COMMENT ON COLUMN sj_job.job_name IS '名称';
COMMENT ON COLUMN sj_job.args_str IS '执行方法参数';
COMMENT ON COLUMN sj_job.args_type IS '参数类型 ';
COMMENT ON COLUMN sj_job.next_trigger_at IS '下次触发时间';
COMMENT ON COLUMN sj_job.job_status IS '任务状态 0、关闭、1、开启';
COMMENT ON COLUMN sj_job.task_type IS '任务类型 1、集群 2、广播 3、切片';
COMMENT ON COLUMN sj_job.route_key IS '路由策略';
COMMENT ON COLUMN sj_job.executor_type IS '执行器类型';
COMMENT ON COLUMN sj_job.executor_info IS '执行器名称';
COMMENT ON COLUMN sj_job.trigger_type IS '触发类型 1.CRON 表达式 2. 固定时间';
COMMENT ON COLUMN sj_job.trigger_interval IS '间隔时长';
COMMENT ON COLUMN sj_job.block_strategy IS '阻塞策略 1、丢弃 2、覆盖 3、并行 4、恢复';
COMMENT ON COLUMN sj_job.executor_timeout IS '任务执行超时时间，单位秒';
COMMENT ON COLUMN sj_job.max_retry_times IS '最大重试次数';
COMMENT ON COLUMN sj_job.parallel_num IS '并行数';
COMMENT ON COLUMN sj_job.retry_interval IS '重试间隔 ( s)';
COMMENT ON COLUMN sj_job.bucket_index IS 'bucket';
COMMENT ON COLUMN sj_job.resident IS '是否是常驻任务';
COMMENT ON COLUMN sj_job.notify_ids IS '通知告警场景配置id列表';
COMMENT ON COLUMN sj_job.owner_id IS '负责人id';
COMMENT ON COLUMN sj_job.labels IS '标签';
COMMENT ON COLUMN sj_job.description IS '描述';
COMMENT ON COLUMN sj_job.ext_attrs IS '扩展字段';
COMMENT ON COLUMN sj_job.deleted IS '逻辑删除 1、删除';
COMMENT ON COLUMN sj_job.create_dt IS '创建时间';
COMMENT ON COLUMN sj_job.update_dt IS '修改时间';
COMMENT ON TABLE sj_job IS '任务信息';

INSERT INTO sj_job
VALUES (1, 'dev', 'ruoyi_group', 'demo-job', null, 1, 1710344035622, 1, 1, 4, 1, 'testJobExecutor', 2, '60', 1, 60, 3,
        1, 1, 116, 0, '', 1, '', '', '', 0, now(), now());

-- sj_job_log_message
CREATE TABLE sj_job_log_message
(
    id            bigserial PRIMARY KEY,
    namespace_id  varchar(64) NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name    varchar(64) NOT NULL,
    job_id        bigint      NOT NULL,
    task_batch_id bigint      NOT NULL,
    task_id       bigint      NOT NULL,
    message       text        NOT NULL,
    log_num       int         NOT NULL DEFAULT 1,
    real_time     bigint      NOT NULL DEFAULT 0,
    ext_attrs     varchar(256) NULL     DEFAULT '',
    create_dt     timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sj_job_log_message_01 ON sj_job_log_message (task_batch_id, task_id);
CREATE INDEX idx_sj_job_log_message_02 ON sj_job_log_message (create_dt);
CREATE INDEX idx_sj_job_log_message_03 ON sj_job_log_message (namespace_id, group_name);

COMMENT ON COLUMN sj_job_log_message.id IS '主键';
COMMENT ON COLUMN sj_job_log_message.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_job_log_message.group_name IS '组名称';
COMMENT ON COLUMN sj_job_log_message.job_id IS '任务信息id';
COMMENT ON COLUMN sj_job_log_message.task_batch_id IS '任务批次id';
COMMENT ON COLUMN sj_job_log_message.task_id IS '调度任务id';
COMMENT ON COLUMN sj_job_log_message.message IS '调度信息';
COMMENT ON COLUMN sj_job_log_message.log_num IS '日志数量';
COMMENT ON COLUMN sj_job_log_message.real_time IS '上报时间';
COMMENT ON COLUMN sj_job_log_message.ext_attrs IS '扩展字段';
COMMENT ON COLUMN sj_job_log_message.create_dt IS '创建时间';
COMMENT ON TABLE sj_job_log_message IS '调度日志';

-- sj_job_task
CREATE TABLE sj_job_task
(
    id             bigserial PRIMARY KEY,
    namespace_id   varchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name     varchar(64)  NOT NULL,
    job_id         bigint       NOT NULL,
    task_batch_id  bigint       NOT NULL,
    parent_id      bigint       NOT NULL DEFAULT 0,
    task_status    smallint     NOT NULL DEFAULT 0,
    retry_count    int          NOT NULL DEFAULT 0,
    mr_stage       smallint NULL     DEFAULT NULL,
    leaf           smallint     NOT NULL DEFAULT '1',
    task_name      varchar(255) NOT NULL DEFAULT '',
    client_info    varchar(128) NULL     DEFAULT NULL,
    wf_context     text NULL     DEFAULT NULL,
    result_message text         NOT NULL,
    args_str       text NULL     DEFAULT NULL,
    args_type      smallint     NOT NULL DEFAULT 1,
    ext_attrs      varchar(256) NULL     DEFAULT '',
    create_dt      timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt      timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sj_job_task_01 ON sj_job_task (task_batch_id, task_status);
CREATE INDEX idx_sj_job_task_02 ON sj_job_task (create_dt);
CREATE INDEX idx_sj_job_task_03 ON sj_job_task (namespace_id, group_name);

COMMENT ON COLUMN sj_job_task.id IS '主键';
COMMENT ON COLUMN sj_job_task.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_job_task.group_name IS '组名称';
COMMENT ON COLUMN sj_job_task.job_id IS '任务信息id';
COMMENT ON COLUMN sj_job_task.task_batch_id IS '调度任务id';
COMMENT ON COLUMN sj_job_task.parent_id IS '父执行器id';
COMMENT ON COLUMN sj_job_task.task_status IS '执行的状态 0、失败 1、成功';
COMMENT ON COLUMN sj_job_task.retry_count IS '重试次数';
COMMENT ON COLUMN sj_job_task.mr_stage IS '动态分片所处阶段 1:map 2:reduce 3:mergeReduce';
COMMENT ON COLUMN sj_job_task.leaf IS '叶子节点';
COMMENT ON COLUMN sj_job_task.task_name IS '任务名称';
COMMENT ON COLUMN sj_job_task.client_info IS '客户端地址 clientId#ip:port';
COMMENT ON COLUMN sj_job_task.wf_context IS '工作流全局上下文';
COMMENT ON COLUMN sj_job_task.result_message IS '执行结果';
COMMENT ON COLUMN sj_job_task.args_str IS '执行方法参数';
COMMENT ON COLUMN sj_job_task.args_type IS '参数类型 ';
COMMENT ON COLUMN sj_job_task.ext_attrs IS '扩展字段';
COMMENT ON COLUMN sj_job_task.create_dt IS '创建时间';
COMMENT ON COLUMN sj_job_task.update_dt IS '修改时间';
COMMENT ON TABLE sj_job_task IS '任务实例';

-- sj_job_task_batch
CREATE TABLE sj_job_task_batch
(
    id                      bigserial PRIMARY KEY,
    namespace_id            varchar(64) NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name              varchar(64) NOT NULL,
    job_id                  bigint      NOT NULL,
    workflow_node_id        bigint      NOT NULL DEFAULT 0,
    parent_workflow_node_id bigint      NOT NULL DEFAULT 0,
    workflow_task_batch_id  bigint      NOT NULL DEFAULT 0,
    task_batch_status       smallint    NOT NULL DEFAULT 0,
    operation_reason        smallint    NOT NULL DEFAULT 0,
    execution_at            bigint      NOT NULL DEFAULT 0,
    system_task_type        smallint    NOT NULL DEFAULT 3,
    parent_id               varchar(64) NOT NULL DEFAULT '',
    ext_attrs               varchar(256) NULL     DEFAULT '',
    deleted                 smallint    NOT NULL DEFAULT 0,
    create_dt               timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt               timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sj_job_task_batch_01 ON sj_job_task_batch (job_id, task_batch_status);
CREATE INDEX idx_sj_job_task_batch_02 ON sj_job_task_batch (create_dt);
CREATE INDEX idx_sj_job_task_batch_03 ON sj_job_task_batch (namespace_id, group_name);
CREATE INDEX idx_sj_job_task_batch_04 ON sj_job_task_batch (workflow_task_batch_id, workflow_node_id);

COMMENT ON COLUMN sj_job_task_batch.id IS '主键';
COMMENT ON COLUMN sj_job_task_batch.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_job_task_batch.group_name IS '组名称';
COMMENT ON COLUMN sj_job_task_batch.job_id IS '任务id';
COMMENT ON COLUMN sj_job_task_batch.workflow_node_id IS '工作流节点id';
COMMENT ON COLUMN sj_job_task_batch.parent_workflow_node_id IS '工作流任务父批次id';
COMMENT ON COLUMN sj_job_task_batch.workflow_task_batch_id IS '工作流任务批次id';
COMMENT ON COLUMN sj_job_task_batch.task_batch_status IS '任务批次状态 0、失败 1、成功';
COMMENT ON COLUMN sj_job_task_batch.operation_reason IS '操作原因';
COMMENT ON COLUMN sj_job_task_batch.execution_at IS '任务执行时间';
COMMENT ON COLUMN sj_job_task_batch.system_task_type IS '任务类型 3、JOB任务 4、WORKFLOW任务';
COMMENT ON COLUMN sj_job_task_batch.parent_id IS '父节点';
COMMENT ON COLUMN sj_job_task_batch.ext_attrs IS '扩展字段';
COMMENT ON COLUMN sj_job_task_batch.deleted IS '逻辑删除 1、删除';
COMMENT ON COLUMN sj_job_task_batch.create_dt IS '创建时间';
COMMENT ON COLUMN sj_job_task_batch.update_dt IS '修改时间';
COMMENT ON TABLE sj_job_task_batch IS '任务批次';

-- sj_job_summary
CREATE TABLE sj_job_summary
(
    id               bigserial PRIMARY KEY,
    namespace_id     varchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name       varchar(64)  NOT NULL DEFAULT '',
    business_id      bigint       NOT NULL,
    system_task_type smallint     NOT NULL DEFAULT 3,
    trigger_at       timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    success_num      int          NOT NULL DEFAULT 0,
    fail_num         int          NOT NULL DEFAULT 0,
    fail_reason      varchar(512) NOT NULL DEFAULT '',
    stop_num         int          NOT NULL DEFAULT 0,
    stop_reason      varchar(512) NOT NULL DEFAULT '',
    cancel_num       int          NOT NULL DEFAULT 0,
    cancel_reason    varchar(512) NOT NULL DEFAULT '',
    create_dt        timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt        timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_sj_job_summary_01 ON sj_job_summary (trigger_at, system_task_type, business_id);
CREATE INDEX idx_sj_job_summary_01 ON sj_job_summary (namespace_id, group_name, business_id);

COMMENT ON COLUMN sj_job_summary.id IS '主键';
COMMENT ON COLUMN sj_job_summary.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_job_summary.group_name IS '组名称';
COMMENT ON COLUMN sj_job_summary.business_id IS '业务id  ( job_id或workflow_id)';
COMMENT ON COLUMN sj_job_summary.system_task_type IS '任务类型 3、JOB任务 4、WORKFLOW任务';
COMMENT ON COLUMN sj_job_summary.trigger_at IS '统计时间';
COMMENT ON COLUMN sj_job_summary.success_num IS '执行成功-日志数量';
COMMENT ON COLUMN sj_job_summary.fail_num IS '执行失败-日志数量';
COMMENT ON COLUMN sj_job_summary.fail_reason IS '失败原因';
COMMENT ON COLUMN sj_job_summary.stop_num IS '执行失败-日志数量';
COMMENT ON COLUMN sj_job_summary.stop_reason IS '失败原因';
COMMENT ON COLUMN sj_job_summary.cancel_num IS '执行失败-日志数量';
COMMENT ON COLUMN sj_job_summary.cancel_reason IS '失败原因';
COMMENT ON COLUMN sj_job_summary.create_dt IS '创建时间';
COMMENT ON COLUMN sj_job_summary.update_dt IS '修改时间';
COMMENT ON TABLE sj_job_summary IS 'DashBoard_Job';

-- sj_retry_summary
CREATE TABLE sj_retry_summary
(
    id            bigserial PRIMARY KEY,
    namespace_id  varchar(64) NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name    varchar(64) NOT NULL DEFAULT '',
    scene_name    varchar(64) NOT NULL DEFAULT '',
    trigger_at    timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    running_num   int         NOT NULL DEFAULT 0,
    finish_num    int         NOT NULL DEFAULT 0,
    max_count_num int         NOT NULL DEFAULT 0,
    suspend_num   int         NOT NULL DEFAULT 0,
    create_dt     timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt     timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_sj_retry_summary_01 ON sj_retry_summary (namespace_id, group_name, scene_name, trigger_at);
CREATE INDEX idx_sj_retry_summary_01 ON sj_retry_summary (trigger_at);

COMMENT ON COLUMN sj_retry_summary.id IS '主键';
COMMENT ON COLUMN sj_retry_summary.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_retry_summary.group_name IS '组名称';
COMMENT ON COLUMN sj_retry_summary.scene_name IS '场景名称';
COMMENT ON COLUMN sj_retry_summary.trigger_at IS '统计时间';
COMMENT ON COLUMN sj_retry_summary.running_num IS '重试中-日志数量';
COMMENT ON COLUMN sj_retry_summary.finish_num IS '重试完成-日志数量';
COMMENT ON COLUMN sj_retry_summary.max_count_num IS '重试到达最大次数-日志数量';
COMMENT ON COLUMN sj_retry_summary.suspend_num IS '暂停重试-日志数量';
COMMENT ON COLUMN sj_retry_summary.create_dt IS '创建时间';
COMMENT ON COLUMN sj_retry_summary.update_dt IS '修改时间';
COMMENT ON TABLE sj_retry_summary IS 'DashBoard_Retry';

-- sj_workflow
CREATE TABLE sj_workflow
(
    id               bigserial PRIMARY KEY,
    workflow_name    varchar(64)  NOT NULL,
    namespace_id     varchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name       varchar(64)  NOT NULL,
    workflow_status  smallint     NOT NULL DEFAULT 1,
    trigger_type     smallint     NOT NULL,
    trigger_interval varchar(255) NOT NULL,
    next_trigger_at  bigint       NOT NULL,
    block_strategy   smallint     NOT NULL DEFAULT 1,
    executor_timeout int          NOT NULL DEFAULT 0,
    description      varchar(256) NOT NULL DEFAULT '',
    flow_info        text NULL     DEFAULT NULL,
    wf_context       text NULL     DEFAULT NULL,
    notify_ids       varchar(128) NOT NULL DEFAULT '',
    bucket_index     int          NOT NULL DEFAULT 0,
    version          int          NOT NULL,
    owner_id         bigint NULL     DEFAULT NULL,
    ext_attrs        varchar(256) NULL     DEFAULT '',
    deleted          smallint     NOT NULL DEFAULT 0,
    create_dt        timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt        timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sj_workflow_01 ON sj_workflow (create_dt);
CREATE INDEX idx_sj_workflow_02 ON sj_workflow (namespace_id, group_name);

COMMENT ON COLUMN sj_workflow.id IS '主键';
COMMENT ON COLUMN sj_workflow.workflow_name IS '工作流名称';
COMMENT ON COLUMN sj_workflow.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_workflow.group_name IS '组名称';
COMMENT ON COLUMN sj_workflow.workflow_status IS '工作流状态 0、关闭、1、开启';
COMMENT ON COLUMN sj_workflow.trigger_type IS '触发类型 1.CRON 表达式 2. 固定时间';
COMMENT ON COLUMN sj_workflow.trigger_interval IS '间隔时长';
COMMENT ON COLUMN sj_workflow.next_trigger_at IS '下次触发时间';
COMMENT ON COLUMN sj_workflow.block_strategy IS '阻塞策略 1、丢弃 2、覆盖 3、并行';
COMMENT ON COLUMN sj_workflow.executor_timeout IS '任务执行超时时间，单位秒';
COMMENT ON COLUMN sj_workflow.description IS '描述';
COMMENT ON COLUMN sj_workflow.flow_info IS '流程信息';
COMMENT ON COLUMN sj_workflow.wf_context IS '上下文';
COMMENT ON COLUMN sj_workflow.notify_ids IS '通知告警场景配置id列表';
COMMENT ON COLUMN sj_workflow.bucket_index IS 'bucket';
COMMENT ON COLUMN sj_workflow.version IS '版本号';
COMMENT ON COLUMN sj_workflow.owner_id IS '负责人id';
COMMENT ON COLUMN sj_workflow.ext_attrs IS '扩展字段';
COMMENT ON COLUMN sj_workflow.deleted IS '逻辑删除 1、删除';
COMMENT ON COLUMN sj_workflow.create_dt IS '创建时间';
COMMENT ON COLUMN sj_workflow.update_dt IS '修改时间';
COMMENT ON TABLE sj_workflow IS '工作流';

-- sj_workflow_node
CREATE TABLE sj_workflow_node
(
    id                   bigserial PRIMARY KEY,
    namespace_id         varchar(64) NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    node_name            varchar(64) NOT NULL,
    group_name           varchar(64) NOT NULL,
    job_id               bigint      NOT NULL,
    workflow_id          bigint      NOT NULL,
    node_type            smallint    NOT NULL DEFAULT 1,
    expression_type      smallint    NOT NULL DEFAULT 0,
    fail_strategy        smallint    NOT NULL DEFAULT 1,
    workflow_node_status smallint    NOT NULL DEFAULT 1,
    priority_level       int         NOT NULL DEFAULT 1,
    node_info            text NULL     DEFAULT NULL,
    version              int         NOT NULL,
    ext_attrs            varchar(256) NULL     DEFAULT '',
    deleted              smallint    NOT NULL DEFAULT 0,
    create_dt            timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt            timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sj_workflow_node_01 ON sj_workflow_node (create_dt);
CREATE INDEX idx_sj_workflow_node_02 ON sj_workflow_node (namespace_id, group_name);

COMMENT ON COLUMN sj_workflow_node.id IS '主键';
COMMENT ON COLUMN sj_workflow_node.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_workflow_node.node_name IS '节点名称';
COMMENT ON COLUMN sj_workflow_node.group_name IS '组名称';
COMMENT ON COLUMN sj_workflow_node.job_id IS '任务信息id';
COMMENT ON COLUMN sj_workflow_node.workflow_id IS '工作流ID';
COMMENT ON COLUMN sj_workflow_node.node_type IS '1、任务节点 2、条件节点';
COMMENT ON COLUMN sj_workflow_node.expression_type IS '1、SpEl、2、Aviator 3、QL';
COMMENT ON COLUMN sj_workflow_node.fail_strategy IS '失败策略 1、跳过 2、阻塞';
COMMENT ON COLUMN sj_workflow_node.workflow_node_status IS '工作流节点状态 0、关闭、1、开启';
COMMENT ON COLUMN sj_workflow_node.priority_level IS '优先级';
COMMENT ON COLUMN sj_workflow_node.node_info IS '节点信息 ';
COMMENT ON COLUMN sj_workflow_node.version IS '版本号';
COMMENT ON COLUMN sj_workflow_node.ext_attrs IS '扩展字段';
COMMENT ON COLUMN sj_workflow_node.deleted IS '逻辑删除 1、删除';
COMMENT ON COLUMN sj_workflow_node.create_dt IS '创建时间';
COMMENT ON COLUMN sj_workflow_node.update_dt IS '修改时间';
COMMENT ON TABLE sj_workflow_node IS '工作流节点';

-- sj_workflow_task_batch
CREATE TABLE sj_workflow_task_batch
(
    id                bigserial PRIMARY KEY,
    namespace_id      varchar(64) NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name        varchar(64) NOT NULL,
    workflow_id       bigint      NOT NULL,
    task_batch_status smallint    NOT NULL DEFAULT 0,
    operation_reason  smallint    NOT NULL DEFAULT 0,
    flow_info         text NULL     DEFAULT NULL,
    wf_context        text NULL     DEFAULT NULL,
    execution_at      bigint      NOT NULL DEFAULT 0,
    ext_attrs         varchar(256) NULL     DEFAULT '',
    version           int         NOT NULL DEFAULT 1,
    deleted           smallint    NOT NULL DEFAULT 0,
    create_dt         timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt         timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sj_workflow_task_batch_01 ON sj_workflow_task_batch (workflow_id, task_batch_status);
CREATE INDEX idx_sj_workflow_task_batch_02 ON sj_workflow_task_batch (create_dt);
CREATE INDEX idx_sj_workflow_task_batch_03 ON sj_workflow_task_batch (namespace_id, group_name);

COMMENT ON COLUMN sj_workflow_task_batch.id IS '主键';
COMMENT ON COLUMN sj_workflow_task_batch.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_workflow_task_batch.group_name IS '组名称';
COMMENT ON COLUMN sj_workflow_task_batch.workflow_id IS '工作流任务id';
COMMENT ON COLUMN sj_workflow_task_batch.task_batch_status IS '任务批次状态 0、失败 1、成功';
COMMENT ON COLUMN sj_workflow_task_batch.operation_reason IS '操作原因';
COMMENT ON COLUMN sj_workflow_task_batch.flow_info IS '流程信息';
COMMENT ON COLUMN sj_workflow_task_batch.wf_context IS '全局上下文';
COMMENT ON COLUMN sj_workflow_task_batch.execution_at IS '任务执行时间';
COMMENT ON COLUMN sj_workflow_task_batch.ext_attrs IS '扩展字段';
COMMENT ON COLUMN sj_workflow_task_batch.version IS '版本号';
COMMENT ON COLUMN sj_workflow_task_batch.deleted IS '逻辑删除 1、删除';
COMMENT ON COLUMN sj_workflow_task_batch.create_dt IS '创建时间';
COMMENT ON COLUMN sj_workflow_task_batch.update_dt IS '修改时间';
COMMENT ON TABLE sj_workflow_task_batch IS '工作流批次';

-- sj_job_executor
CREATE TABLE sj_job_executor
(
    id            bigserial PRIMARY KEY,
    namespace_id  varchar(64)  NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a',
    group_name    varchar(64)  NOT NULL,
    executor_info varchar(256) NOT NULL,
    executor_type varchar(3)   NOT NULL,
    create_dt     timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_dt     timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sj_job_executor_01 ON sj_job_executor (namespace_id, group_name);
CREATE INDEX idx_sj_job_executor_02 ON sj_job_executor (create_dt);

COMMENT ON COLUMN sj_job_executor.id IS '主键';
COMMENT ON COLUMN sj_job_executor.namespace_id IS '命名空间id';
COMMENT ON COLUMN sj_job_executor.group_name IS '组名称';
COMMENT ON COLUMN sj_job_executor.executor_info IS '任务执行器名称';
COMMENT ON COLUMN sj_job_executor.executor_type IS '1:java 2:python 3:go';
COMMENT ON COLUMN sj_job_executor.create_dt IS '创建时间';
COMMENT ON COLUMN sj_job_executor.update_dt IS '修改时间';
COMMENT ON TABLE sj_job_executor IS '任务执行器信息';

