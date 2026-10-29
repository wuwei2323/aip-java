-- ----------------------------
-- 1、应用注册表（sys_application 与 sys_tenant 1:1，tenant_id 唯一）
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_application (
    id                         int8 PRIMARY KEY,
    app_id                     varchar(64)  NOT NULL,   -- OIDC client_id
    app_name                   varchar(100) NOT NULL,
    tenant_id                  varchar(20)  NOT NULL,   -- 应用/租户编号
    client_auth_method         varchar(32)  DEFAULT 'client_secret_basic',
    client_secret_enc          text,                     -- AES 加密存储
    redirect_uris              text,                     -- JSON 数组
    post_logout_redirect_uris  text,                     -- JSON 数组
    scopes                     varchar(255) DEFAULT 'openid profile email',
    grant_types                varchar(255) DEFAULT 'authorization_code,refresh_token',
    access_token_ttl_minutes   int4         DEFAULT 30,
    refresh_token_ttl_days     int4         DEFAULT 7,
    reuse_refresh_tokens       char(1)      DEFAULT '0',
    require_pkce               char(1)      DEFAULT '1',
    domain                     varchar(255),
    entry_url                  varchar(500),
    wecom_config_key           varchar(64),
    bind_policy                varchar(32)  DEFAULT 'REJECT',
    icon                       varchar(255),
    order_num                  int4         DEFAULT 0,
    status                     char(1)      DEFAULT '0',
    del_flag                   char(1)      DEFAULT '0',
    create_dept                int8,
    create_by                  int8,
    create_time                timestamp,
    update_by                  int8,
    update_time                timestamp
);

CREATE UNIQUE INDEX IF NOT EXISTS sys_application_app_id_uk ON sys_application (app_id);
CREATE UNIQUE INDEX IF NOT EXISTS sys_application_tenant_uk ON sys_application (tenant_id);
CREATE INDEX IF NOT EXISTS sys_application_domain_idx ON sys_application (domain);

COMMENT ON TABLE sys_application IS '应用注册表';
COMMENT ON COLUMN sys_application.id IS '主键';
COMMENT ON COLUMN sys_application.app_id IS 'OIDC客户端ID(client_id)';
COMMENT ON COLUMN sys_application.app_name IS '应用名称';
COMMENT ON COLUMN sys_application.tenant_id IS '租户/应用编号';
COMMENT ON COLUMN sys_application.client_auth_method IS '客户端认证方式';
COMMENT ON COLUMN sys_application.client_secret_enc IS '客户端密钥(密文)';
COMMENT ON COLUMN sys_application.redirect_uris IS '登录重定向URI列表(JSON)';
COMMENT ON COLUMN sys_application.post_logout_redirect_uris IS '登出重定向URI列表(JSON)';
COMMENT ON COLUMN sys_application.scopes IS '授权范围';
COMMENT ON COLUMN sys_application.grant_types IS '授权类型';
COMMENT ON COLUMN sys_application.access_token_ttl_minutes IS '访问令牌有效期(分钟)';
COMMENT ON COLUMN sys_application.refresh_token_ttl_days IS '刷新令牌有效期(天)';
COMMENT ON COLUMN sys_application.reuse_refresh_tokens IS '是否复用刷新令牌';
COMMENT ON COLUMN sys_application.require_pkce IS '是否强制PKCE';
COMMENT ON COLUMN sys_application.domain IS '应用域名';
COMMENT ON COLUMN sys_application.entry_url IS '应用入口地址';
COMMENT ON COLUMN sys_application.wecom_config_key IS '企微配置标识';
COMMENT ON COLUMN sys_application.bind_policy IS '用户绑定策略';
COMMENT ON COLUMN sys_application.icon IS '应用图标';
COMMENT ON COLUMN sys_application.order_num IS '显示顺序';
COMMENT ON COLUMN sys_application.status IS '状态（0正常 1停用）';
COMMENT ON COLUMN sys_application.del_flag IS '删除标志（0代表存在 1代表删除）';

-- ----------------------------
-- 2、用户表增加认证与SSO字段
-- ----------------------------
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS employee_no varchar(64);
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS keycloak_user_id varchar(64);
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS auth_source varchar(20) DEFAULT 'PLATFORM';
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS password_reset_required bool DEFAULT false;

CREATE UNIQUE INDEX IF NOT EXISTS sys_user_employee_no_uk
    ON sys_user (employee_no) WHERE del_flag = '0' AND employee_no IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS sys_user_keycloak_id_uk
    ON sys_user (keycloak_user_id) WHERE keycloak_user_id IS NOT NULL;

COMMENT ON COLUMN sys_user.employee_no IS '员工工号(全局唯一)';
COMMENT ON COLUMN sys_user.keycloak_user_id IS 'Keycloak用户标识';
COMMENT ON COLUMN sys_user.auth_source IS '认证来源(PLATFORM/LOCAL/LDAP)';
COMMENT ON COLUMN sys_user.password_reset_required IS '是否需要重置密码(首次登录强制改密)';

-- ----------------------------
-- 3、菜单表增加关联应用字段
-- ----------------------------
ALTER TABLE sys_menu ADD COLUMN IF NOT EXISTS app_id varchar(64);
CREATE INDEX IF NOT EXISTS sys_menu_app_id_idx ON sys_menu (app_id);
COMMENT ON COLUMN sys_menu.app_id IS '关联应用ID(仅menu_type=C可配置，未关联为普通菜单)';

-- ----------------------------
-- 4、初始化门户应用数据（tenant_id='000000', app_id='000000'）
-- ----------------------------
INSERT INTO sys_application (
    id, app_id, app_name, tenant_id, client_auth_method,
    scopes, grant_types, access_token_ttl_minutes, refresh_token_ttl_days,
    reuse_refresh_tokens, require_pkce, domain, entry_url,
    status, del_flag, create_by, create_time
) VALUES (
    1, '000000', '集成平台门户', '000000', 'client_secret_basic',
    'openid profile email', 'authorization_code,refresh_token', 30, 7,
    '0', '1', null, '/',
    '0', '0', 1, now()
) ON CONFLICT (tenant_id) DO NOTHING;

-- ----------------------------
-- 5、初始化子应用管理菜单与权限
-- ----------------------------
INSERT INTO sys_menu VALUES('124', '子应用管理', '1', '12', 'application', 'system/application/index', '', '1', '0', 'C', '0', '0', 'system:application:list', 'app', 103, 1, now(), null, null, '子应用管理菜单')
ON CONFLICT (menu_id) DO NOTHING;

INSERT INTO sys_menu VALUES('1630', '应用查询', '124', '1', '#', '', '', '1', '0', 'F', '0', '0', 'system:application:query', '#', 103, 1, now(), null, null, '')
ON CONFLICT (menu_id) DO NOTHING;
INSERT INTO sys_menu VALUES('1631', '应用新增', '124', '2', '#', '', '', '1', '0', 'F', '0', '0', 'system:application:add', '#', 103, 1, now(), null, null, '')
ON CONFLICT (menu_id) DO NOTHING;
INSERT INTO sys_menu VALUES('1632', '应用修改', '124', '3', '#', '', '', '1', '0', 'F', '0', '0', 'system:application:edit', '#', 103, 1, now(), null, null, '')
ON CONFLICT (menu_id) DO NOTHING;
INSERT INTO sys_menu VALUES('1633', '应用删除', '124', '4', '#', '', '', '1', '0', 'F', '0', '0', 'system:application:remove', '#', 103, 1, now(), null, null, '')
ON CONFLICT (menu_id) DO NOTHING;
INSERT INTO sys_menu VALUES('1634', '重置密钥', '124', '5', '#', '', '', '1', '0', 'F', '0', '0', 'system:application:resetSecret', '#', 103, 1, now(), null, null, '')
ON CONFLICT (menu_id) DO NOTHING;

-- 授权给超级管理员角色（role_id=1, tenant_id='000000'）
INSERT INTO sys_role_menu (role_id, menu_id, tenant_id) VALUES
    (1, 124, '000000'),
    (1, 1630, '000000'),
    (1, 1631, '000000'),
    (1, 1632, '000000'),
    (1, 1633, '000000'),
    (1, 1634, '000000')
ON CONFLICT (role_id, menu_id, tenant_id) DO NOTHING;
