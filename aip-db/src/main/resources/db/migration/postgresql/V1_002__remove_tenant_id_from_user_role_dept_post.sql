-- 移除用户、角色、部门、岗位表的应用ID字段，实现全应用共享
ALTER TABLE "sys_user" DROP COLUMN IF EXISTS "tenant_id";
ALTER TABLE "sys_role" DROP COLUMN IF EXISTS "tenant_id";
ALTER TABLE "sys_dept" DROP COLUMN IF EXISTS "tenant_id";
ALTER TABLE "sys_post" DROP COLUMN IF EXISTS "tenant_id";
