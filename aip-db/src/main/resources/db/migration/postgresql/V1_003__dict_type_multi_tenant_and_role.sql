-- 字典类型表改造：支持多应用关联、角色关联、组件配置

-- 1. 新增字段
ALTER TABLE sys_dict_type ADD COLUMN IF NOT EXISTS category_type varchar(10) default 'system';
ALTER TABLE sys_dict_type ADD COLUMN IF NOT EXISTS role_ids text;
ALTER TABLE sys_dict_type ADD COLUMN IF NOT EXISTS component_info text;

COMMENT ON COLUMN sys_dict_type.category_type IS '字典分类（system=系统 role=角色）';
COMMENT ON COLUMN sys_dict_type.role_ids IS '关联角色ID列表（JSON数组）';
COMMENT ON COLUMN sys_dict_type.component_info IS '组件配置信息（JSON）';

-- 2. 创建字典类型与应用关联表
CREATE TABLE IF NOT EXISTS sys_dict_type_tenant
(
    dict_id   int8        NOT NULL,
    tenant_id varchar(20) NOT NULL,
    CONSTRAINT sys_dict_type_tenant_pk PRIMARY KEY (dict_id, tenant_id)
);

COMMENT ON TABLE sys_dict_type_tenant IS '字典类型与应用关联表';
COMMENT ON COLUMN sys_dict_type_tenant.dict_id IS '字典主键';
COMMENT ON COLUMN sys_dict_type_tenant.tenant_id IS '应用编号';

CREATE INDEX IF NOT EXISTS sys_dict_type_tenant_tenant_id_index ON sys_dict_type_tenant (tenant_id);

-- 3. 迁移现有 tenant_id 数据到关联表
--    历史版本会为每个应用复制一份字典，因此按 dict_type 归并到保留下来的字典主键上
INSERT INTO sys_dict_type_tenant (dict_id, tenant_id)
SELECT keep.dict_id, src.tenant_id
FROM sys_dict_type src
JOIN (
    SELECT dict_type, MIN(dict_id) AS dict_id
    FROM sys_dict_type
    GROUP BY dict_type
) keep ON keep.dict_type = src.dict_type
WHERE src.tenant_id IS NOT NULL
ON CONFLICT DO NOTHING;

-- 3.1 删除历史按应用复制产生的重复字典类型（保留最小 dict_id）
DELETE FROM sys_dict_type src
USING (
    SELECT dict_type, MIN(dict_id) AS dict_id
    FROM sys_dict_type
    GROUP BY dict_type
) keep
WHERE src.dict_type = keep.dict_type
  AND src.dict_id <> keep.dict_id;

-- 3.2 删除历史按应用复制产生的重复字典数据（保留最小 dict_code）
DELETE FROM sys_dict_data src
USING sys_dict_data keep
WHERE src.dict_type = keep.dict_type
  AND src.dict_value = keep.dict_value
  AND src.dict_label = keep.dict_label
  AND src.tenant_id <> keep.tenant_id
  AND src.dict_code > keep.dict_code;

-- 4. 删除旧唯一约束并创建新约束
DROP INDEX IF EXISTS sys_dict_type_index1;
CREATE UNIQUE INDEX sys_dict_type_index1 ON sys_dict_type (dict_type);

-- 5. 移除 tenant_id 字段
ALTER TABLE sys_dict_type DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE sys_dict_data DROP COLUMN IF EXISTS tenant_id;

-- 6. 字典数据表补充数据名称样式字段
ALTER TABLE sys_dict_data ADD COLUMN IF NOT EXISTS label_style char(1) DEFAULT '0'::bpchar;
COMMENT ON COLUMN sys_dict_data.label_style IS '数据名称样式（0正常 1停用）';
UPDATE sys_dict_data SET label_style = '0' WHERE label_style IS NULL;
