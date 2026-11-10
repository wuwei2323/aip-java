-- 给菜单表增加 group_id 字段 (1=管理后台 2=集成平台)
ALTER TABLE sys_menu ADD COLUMN IF NOT EXISTS group_id int8 DEFAULT 1;
COMMENT ON COLUMN sys_menu.group_id IS '菜单分组（1=管理后台 2=集成平台）';
UPDATE sys_menu SET group_id = 1 WHERE group_id IS NULL;
CREATE INDEX IF NOT EXISTS sys_menu_group_id_idx ON sys_menu (group_id);
