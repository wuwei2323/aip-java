-- 角色类字典：字典与角色关联表、角色字典配置值表

-- ----------------------------
-- 1、字典类型与角色关联表（替代 sys_dict_type.role_ids JSON 数组）
-- ----------------------------
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

-- 迁移历史 role_ids JSON 数组（兼容 '["1","2"]' 与 '[1,2]'）
INSERT INTO sys_dict_type_role (dict_id, role_id)
SELECT t.dict_id, r.value::int8
FROM sys_dict_type t
         CROSS JOIN LATERAL jsonb_array_elements_text(
    CASE
        WHEN t.role_ids IS NULL OR btrim(t.role_ids) IN ('', '[]') THEN '[]'::jsonb
        ELSE t.role_ids::jsonb
        END) AS r(value)
WHERE t.role_ids IS NOT NULL
ON CONFLICT DO NOTHING;

ALTER TABLE sys_dict_type DROP COLUMN IF EXISTS role_ids;

-- ----------------------------
-- 2、角色字典配置值表（EAV）
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_role_dict_value
(
    id          int8        NOT NULL,
    role_id     int8        NOT NULL,
    tenant_id   varchar(20) NOT NULL,
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
COMMENT ON COLUMN sys_role_dict_value.tenant_id IS '应用编号';
COMMENT ON COLUMN sys_role_dict_value.dict_id IS '字典主键';
COMMENT ON COLUMN sys_role_dict_value.field_key IS '组件字段标识（取 component_info.fieldKey）';
COMMENT ON COLUMN sys_role_dict_value.field_value IS '字段值（多选为JSON数组字符串）';

CREATE UNIQUE INDEX IF NOT EXISTS sys_role_dict_value_uk
    ON sys_role_dict_value (role_id, tenant_id, dict_id, field_key);

CREATE INDEX IF NOT EXISTS sys_role_dict_value_dict_index ON sys_role_dict_value (dict_id);
