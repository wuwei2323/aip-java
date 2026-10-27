-- 角色管理：菜单权限与数据权限按应用独立配置

-- ----------------------------
-- 1、角色在各应用下的数据权限与联动配置表
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_role_data_scope
(
    id                  int8        NOT NULL,
    role_id             int8        NOT NULL,
    tenant_id           varchar(20) NOT NULL,
    data_scope          char(1)     DEFAULT '1'::bpchar,
    menu_check_strictly bool        DEFAULT true,
    dept_check_strictly bool        DEFAULT true,
    create_dept         int8,
    create_by           int8,
    create_time         timestamp,
    update_by           int8,
    update_time         timestamp,
    CONSTRAINT sys_role_data_scope_pk PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS sys_role_data_scope_uk ON sys_role_data_scope (role_id, tenant_id);
CREATE INDEX IF NOT EXISTS sys_role_data_scope_role_id_index ON sys_role_data_scope (role_id);
CREATE INDEX IF NOT EXISTS sys_role_data_scope_tenant_id_index ON sys_role_data_scope (tenant_id);

COMMENT ON TABLE sys_role_data_scope IS '角色在应用下的数据权限与联动配置表';
COMMENT ON COLUMN sys_role_data_scope.id IS '主键';
COMMENT ON COLUMN sys_role_data_scope.role_id IS '角色ID';
COMMENT ON COLUMN sys_role_data_scope.tenant_id IS '应用编号';
COMMENT ON COLUMN sys_role_data_scope.data_scope IS '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限 5：仅本人数据权限 6：部门及以下或本人数据权限）';
COMMENT ON COLUMN sys_role_data_scope.menu_check_strictly IS '菜单树选择项是否关联显示';
COMMENT ON COLUMN sys_role_data_scope.dept_check_strictly IS '部门树选择项是否关联显示';

-- ----------------------------
-- 2、角色与菜单关联表支持应用独立配置
-- ----------------------------
ALTER TABLE sys_role_menu ADD COLUMN IF NOT EXISTS tenant_id varchar(20) DEFAULT '000000' NOT NULL;
ALTER TABLE sys_role_menu DROP CONSTRAINT IF EXISTS sys_role_menu_pk;
ALTER TABLE sys_role_menu ADD CONSTRAINT sys_role_menu_pk PRIMARY KEY (role_id, menu_id, tenant_id);
CREATE INDEX IF NOT EXISTS sys_role_menu_tenant_id_index ON sys_role_menu (tenant_id);

-- ----------------------------
-- 3、角色与部门关联表（自定数据权限）支持应用独立配置
-- ----------------------------
ALTER TABLE sys_role_dept ADD COLUMN IF NOT EXISTS tenant_id varchar(20) DEFAULT '000000' NOT NULL;
ALTER TABLE sys_role_dept DROP CONSTRAINT IF EXISTS sys_role_dept_pk;
ALTER TABLE sys_role_dept ADD CONSTRAINT sys_role_dept_pk PRIMARY KEY (role_id, dept_id, tenant_id);
CREATE INDEX IF NOT EXISTS sys_role_dept_tenant_id_index ON sys_role_dept (tenant_id);

-- ----------------------------
-- 4、初始化历史角色数据权限配置（默认归属 '000000'）
-- ----------------------------
INSERT INTO sys_role_data_scope (id, role_id, tenant_id, data_scope, menu_check_strictly, dept_check_strictly, create_time)
SELECT role_id, role_id, '000000', data_scope, menu_check_strictly, dept_check_strictly, now()
FROM sys_role
ON CONFLICT (role_id, tenant_id) DO NOTHING;
