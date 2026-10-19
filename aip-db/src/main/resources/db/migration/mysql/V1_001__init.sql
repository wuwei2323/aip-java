
DROP TABLE IF EXISTS `flow_category`;

CREATE TABLE `flow_category` (
  `category_id` bigint(20) NOT NULL COMMENT '流程分类ID',
  `tenant_id` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT '000000' COMMENT '租户编号',
  `parent_id` bigint(20) DEFAULT 0 COMMENT '父流程分类id',
  `ancestors` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '祖级列表',
  `category_name` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '流程分类名称',
  `order_num` int(4) DEFAULT 0 COMMENT '显示顺序',
  `del_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程分类';

INSERT INTO `flow_category` VALUES (100,'000000',0,'0','OA审批',0,'0',103,1,'2026-01-04 13:38:16',NULL,NULL),(101,'000000',100,'0,100','假勤管理',0,'0',103,1,'2026-01-04 13:38:16',NULL,NULL),(102,'000000',100,'0,100','人事管理',1,'0',103,1,'2026-01-04 13:38:16',NULL,NULL),(103,'000000',101,'0,100,101','请假',0,'0',103,1,'2026-01-04 13:38:16',NULL,NULL),(104,'000000',101,'0,100,101','出差',1,'0',103,1,'2026-01-04 13:38:16',NULL,NULL),(105,'000000',101,'0,100,101','加班',2,'0',103,1,'2026-01-04 13:38:16',NULL,NULL),(106,'000000',101,'0,100,101','换班',3,'0',103,1,'2026-01-04 13:38:16',NULL,NULL),(107,'000000',101,'0,100,101','外出',4,'0',103,1,'2026-01-04 13:38:16',NULL,NULL),(108,'000000',102,'0,100,102','转正',1,'0',103,1,'2026-01-04 13:38:16',NULL,NULL),(109,'000000',102,'0,100,102','离职',2,'0',103,1,'2026-01-04 13:38:16',NULL,NULL);

DROP TABLE IF EXISTS `flow_definition`;
CREATE TABLE `flow_definition` (
  `id` bigint(20) NOT NULL COMMENT '主键id',
  `flow_code` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '流程编码',
  `flow_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '流程名称',
  `model_value` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'CLASSICS' COMMENT '设计器模型（CLASSICS经典模型 MIMIC仿钉钉模型）',
  `category` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '流程类别',
  `version` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '流程版本',
  `is_publish` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否发布（0未发布 1已发布 9失效）',
  `form_custom` char(1) COLLATE utf8mb4_unicode_ci DEFAULT 'N' COMMENT '审批表单是否自定义（Y是 N否）',
  `form_path` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审批表单路径',
  `activity_status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '流程激活状态（0挂起 1激活）',
  `listener_type` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '监听器类型',
  `listener_path` varchar(400) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '监听器路径',
  `ext` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '业务详情 存业务表对象json字符串',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新人',
  `del_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '删除标志',
  `tenant_id` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程定义表';

DROP TABLE IF EXISTS `flow_his_task`;

CREATE TABLE `flow_his_task` (
  `id` bigint(20) NOT NULL COMMENT '主键id',
  `definition_id` bigint(20) NOT NULL COMMENT '对应flow_definition表的id',
  `instance_id` bigint(20) NOT NULL COMMENT '对应flow_instance表的id',
  `task_id` bigint(20) NOT NULL COMMENT '对应flow_task表的id',
  `node_code` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '开始节点编码',
  `node_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '开始节点名称',
  `node_type` tinyint(1) DEFAULT NULL COMMENT '开始节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
  `target_node_code` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '目标节点编码',
  `target_node_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '结束节点名称',
  `approver` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审批人',
  `cooperate_type` tinyint(1) NOT NULL DEFAULT 0 COMMENT '协作方式(1审批 2转办 3委派 4会签 5票签 6加签 7减签)',
  `collaborator` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '协作人',
  `skip_type` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '流转类型（PASS通过 REJECT退回 NONE无动作）',
  `flow_status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）',
  `form_custom` char(1) COLLATE utf8mb4_unicode_ci DEFAULT 'N' COMMENT '审批表单是否自定义（Y是 N否）',
  `form_path` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审批表单路径',
  `message` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审批意见',
  `variable` text COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '任务变量',
  `ext` text COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '业务详情 存业务表对象json字符串',
  `create_time` datetime DEFAULT NULL COMMENT '任务开始时间',
  `update_time` datetime DEFAULT NULL COMMENT '审批完成时间',
  `del_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '删除标志',
  `tenant_id` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='历史任务记录表';

DROP TABLE IF EXISTS `flow_instance`;
CREATE TABLE `flow_instance` (
  `id` bigint(20) NOT NULL COMMENT '主键id',
  `definition_id` bigint(20) NOT NULL COMMENT '对应flow_definition表的id',
  `business_id` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '业务id',
  `node_type` tinyint(1) NOT NULL COMMENT '节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
  `node_code` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '流程节点编码',
  `node_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '流程节点名称',
  `variable` text COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '任务变量',
  `flow_status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）',
  `activity_status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '流程激活状态（0挂起 1激活）',
  `def_json` text COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '流程定义json',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新人',
  `ext` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '扩展字段，预留给业务系统使用',
  `del_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '删除标志',
  `tenant_id` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程实例表';

DROP TABLE IF EXISTS `flow_instance_biz_ext`;
CREATE TABLE `flow_instance_biz_ext` (
  `id` bigint(20) NOT NULL COMMENT '主键id',
  `tenant_id` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT '000000' COMMENT '租户编号',
  `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `business_code` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '业务编码',
  `business_title` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '业务标题',
  `del_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `instance_id` bigint(20) DEFAULT NULL COMMENT '流程实例Id',
  `business_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '业务Id',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程实例业务扩展表';

DROP TABLE IF EXISTS `flow_node`;

CREATE TABLE `flow_node` (
  `id` bigint(20) NOT NULL COMMENT '主键id',
  `node_type` tinyint(1) NOT NULL COMMENT '节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
  `definition_id` bigint(20) NOT NULL COMMENT '流程定义id',
  `node_code` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '流程节点编码',
  `node_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '流程节点名称',
  `permission_flag` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '权限标识（权限类型:权限标识，可以多个，用@@隔开)',
  `node_ratio` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '流程签署比例值',
  `coordinate` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '坐标',
  `any_node_skip` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '任意结点跳转',
  `listener_type` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '监听器类型',
  `listener_path` varchar(400) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '监听器路径',
  `form_custom` char(1) COLLATE utf8mb4_unicode_ci DEFAULT 'N' COMMENT '审批表单是否自定义（Y是 N否）',
  `form_path` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审批表单路径',
  `version` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '版本',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新人',
  `ext` text COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '节点扩展属性',
  `del_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '删除标志',
  `tenant_id` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程节点表';


DROP TABLE IF EXISTS `flow_skip`;

CREATE TABLE `flow_skip` (
  `id` bigint(20) NOT NULL COMMENT '主键id',
  `definition_id` bigint(20) NOT NULL COMMENT '流程定义id',
  `now_node_code` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '当前流程节点的编码',
  `now_node_type` tinyint(1) DEFAULT NULL COMMENT '当前节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
  `next_node_code` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '下一个流程节点的编码',
  `next_node_type` tinyint(1) DEFAULT NULL COMMENT '下一个节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
  `skip_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '跳转名称',
  `skip_type` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '跳转类型（PASS审批通过 REJECT退回）',
  `skip_condition` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '跳转条件',
  `coordinate` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '坐标',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新人',
  `del_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '删除标志',
  `tenant_id` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='节点跳转关联表';

DROP TABLE IF EXISTS `flow_spel`;

CREATE TABLE `flow_spel` (
  `id` bigint(20) NOT NULL COMMENT '主键id',
  `component_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '组件名称',
  `method_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '方法名',
  `method_params` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '参数',
  `view_spel` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '预览spel表达式',
  `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `status` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `del_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '删除标志',
  `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程spel表达式定义表';

INSERT INTO `flow_spel` VALUES (1,'spelRuleComponent','selectDeptLeaderById','initiatorDeptId','#{@spelRuleComponent.selectDeptLeaderById(#initiatorDeptId)}','根据部门id获取部门负责人','0','0',103,1,'2026-01-04 13:38:16',1,'2026-01-04 13:38:16'),(2,NULL,NULL,'initiator','${initiator}','流程发起人','0','0',103,1,'2026-01-04 13:38:16',1,'2026-01-04 13:38:16');

DROP TABLE IF EXISTS `flow_task`;
CREATE TABLE `flow_task` (
  `id` bigint(20) NOT NULL COMMENT '主键id',
  `definition_id` bigint(20) NOT NULL COMMENT '对应flow_definition表的id',
  `instance_id` bigint(20) NOT NULL COMMENT '对应flow_instance表的id',
  `node_code` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '节点编码',
  `node_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '节点名称',
  `node_type` tinyint(1) NOT NULL COMMENT '节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
  `flow_status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）',
  `form_custom` char(1) COLLATE utf8mb4_unicode_ci DEFAULT 'N' COMMENT '审批表单是否自定义（Y是 N否）',
  `form_path` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审批表单路径',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新人',
  `del_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '删除标志',
  `tenant_id` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='待办任务表';

DROP TABLE IF EXISTS `flow_user`;
CREATE TABLE `flow_user` (
  `id` bigint(20) NOT NULL COMMENT '主键id',
  `type` char(1) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '人员类型（1待办任务的审批人权限 2待办任务的转办人权限 3待办任务的委托人权限）',
  `processed_by` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '权限人',
  `associated` bigint(20) NOT NULL COMMENT '任务表id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建人',
  `del_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '删除标志',
  `tenant_id` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `user_processed_type` (`processed_by`,`type`),
  KEY `user_associated` (`associated`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程用户表';

DROP TABLE IF EXISTS `gen_table`;

CREATE TABLE `gen_table` (
  `table_id` bigint(20) NOT NULL COMMENT '编号',
  `data_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '数据源名称',
  `table_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '表名称',
  `table_comment` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '表描述',
  `sub_table_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关联子表的表名',
  `sub_table_fk_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '子表关联的外键名',
  `class_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '实体类名称',
  `tpl_category` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT 'crud' COMMENT '使用的模板（crud单表操作 tree树表操作）',
  `package_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生成包路径',
  `module_name` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生成模块名',
  `business_name` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生成业务名',
  `function_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生成功能名',
  `function_author` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生成功能作者',
  `gen_type` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '生成代码方式（0zip压缩包 1自定义路径）',
  `gen_path` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT '/' COMMENT '生成路径（不填默认项目路径）',
  `options` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '其它生成选项',
  `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`table_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='代码生成业务表';

DROP TABLE IF EXISTS `gen_table_column`;
CREATE TABLE `gen_table_column` (
  `column_id` bigint(20) NOT NULL COMMENT '编号',
  `table_id` bigint(20) DEFAULT NULL COMMENT '归属表编号',
  `column_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '列名称',
  `column_comment` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '列描述',
  `column_type` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '列类型',
  `java_type` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'JAVA类型',
  `java_field` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'JAVA字段名',
  `is_pk` char(1) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '是否主键（1是）',
  `is_increment` char(1) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '是否自增（1是）',
  `is_required` char(1) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '是否必填（1是）',
  `is_insert` char(1) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '是否为插入字段（1是）',
  `is_edit` char(1) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '是否编辑字段（1是）',
  `is_list` char(1) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '是否列表字段（1是）',
  `is_query` char(1) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '是否查询字段（1是）',
  `query_type` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT 'EQ' COMMENT '查询方式（等于、不等于、大于、小于、范围）',
  `html_type` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '显示类型（文本框、文本域、下拉框、复选框、单选框、日期控件）',
  `dict_type` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '字典类型',
  `sort` int(11) DEFAULT NULL COMMENT '排序',
  `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`column_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='代码生成业务表字段';

DROP TABLE IF EXISTS `sj_distributed_lock`;
CREATE TABLE `sj_distributed_lock` (
  `name` varchar(64) NOT NULL COMMENT '锁名称',
  `lock_until` timestamp(3) NOT NULL DEFAULT current_timestamp(3) ON UPDATE current_timestamp(3) COMMENT '锁定时长',
  `locked_at` timestamp(3) NOT NULL DEFAULT current_timestamp(3) COMMENT '锁定时间',
  `locked_by` varchar(255) NOT NULL COMMENT '锁定者',
  `create_dt` datetime NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  `update_dt` datetime NOT NULL DEFAULT current_timestamp() COMMENT '修改时间',
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='锁定表';

DROP TABLE IF EXISTS `sj_group_config`;

CREATE TABLE `sj_group_config` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `namespace_id` varchar(64) NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
  `group_name` varchar(64) NOT NULL DEFAULT '' COMMENT '组名称',
  `description` varchar(256) NOT NULL DEFAULT '' COMMENT '组描述',
  `token` varchar(64) NOT NULL DEFAULT 'SJ_cKqBTPzCsWA3VyuCfFoccmuIEGXjr5KT' COMMENT 'token',
  `group_status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '组状态 0、未启用 1、启用',
  `version` int(11) NOT NULL COMMENT '版本号',
  `group_partition` int(11) NOT NULL COMMENT '分区',
  `id_generator_mode` tinyint(4) NOT NULL DEFAULT 1 COMMENT '唯一id生成模式 默认号段模式',
  `init_scene` tinyint(4) NOT NULL DEFAULT 0 COMMENT '是否初始化场景 0:否 1:是',
  `create_dt` datetime NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  `update_dt` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_namespace_id_group_name` (`namespace_id`,`group_name`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COMMENT='组配置';

INSERT INTO `sj_group_config` VALUES (1,'dev','ruoyi_group','','SJ_cKqBTPzCsWA3VyuCfFoccmuIEGXjr5KT',1,1,0,1,1,'2026-01-04 13:38:01','2026-01-04 13:38:01'),(2,'prod','ruoyi_group','','SJ_cKqBTPzCsWA3VyuCfFoccmuIEGXjr5KT',1,1,0,1,1,'2026-01-04 13:38:01','2026-01-04 13:38:01');

DROP TABLE IF EXISTS `sj_job`;

CREATE TABLE `sj_job` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `namespace_id` varchar(64) NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
  `group_name` varchar(64) NOT NULL COMMENT '组名称',
  `job_name` varchar(64) NOT NULL COMMENT '名称',
  `args_str` text DEFAULT NULL COMMENT '执行方法参数',
  `args_type` tinyint(4) NOT NULL DEFAULT 1 COMMENT '参数类型 ',
  `next_trigger_at` bigint(13) NOT NULL COMMENT '下次触发时间',
  `job_status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '任务状态 0、关闭、1、开启',
  `task_type` tinyint(4) NOT NULL DEFAULT 1 COMMENT '任务类型 1、集群 2、广播 3、切片',
  `route_key` tinyint(4) NOT NULL DEFAULT 4 COMMENT '路由策略',
  `executor_type` tinyint(4) NOT NULL DEFAULT 1 COMMENT '执行器类型',
  `executor_info` varchar(255) DEFAULT NULL COMMENT '执行器名称',
  `trigger_type` tinyint(4) NOT NULL COMMENT '触发类型 1.CRON 表达式 2. 固定时间',
  `trigger_interval` varchar(255) NOT NULL COMMENT '间隔时长',
  `block_strategy` tinyint(4) NOT NULL DEFAULT 1 COMMENT '阻塞策略 1、丢弃 2、覆盖 3、并行 4、恢复',
  `executor_timeout` int(11) NOT NULL DEFAULT 0 COMMENT '任务执行超时时间，单位秒',
  `max_retry_times` int(11) NOT NULL DEFAULT 0 COMMENT '最大重试次数',
  `parallel_num` int(11) NOT NULL DEFAULT 1 COMMENT '并行数',
  `retry_interval` int(11) NOT NULL DEFAULT 0 COMMENT '重试间隔(s)',
  `bucket_index` int(11) NOT NULL DEFAULT 0 COMMENT 'bucket',
  `resident` tinyint(4) NOT NULL DEFAULT 0 COMMENT '是否是常驻任务',
  `notify_ids` varchar(128) NOT NULL DEFAULT '' COMMENT '通知告警场景配置id列表',
  `owner_id` bigint(20) DEFAULT NULL COMMENT '负责人id',
  `labels` varchar(512) DEFAULT '' COMMENT '标签',
  `description` varchar(256) NOT NULL DEFAULT '' COMMENT '描述',
  `ext_attrs` varchar(256) DEFAULT '' COMMENT '扩展字段',
  `deleted` tinyint(4) NOT NULL DEFAULT 0 COMMENT '逻辑删除 1、删除',
  `create_dt` datetime NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  `update_dt` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_namespace_id_group_name` (`namespace_id`,`group_name`),
  KEY `idx_job_status_bucket_index` (`job_status`,`bucket_index`),
  KEY `idx_create_dt` (`create_dt`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COMMENT='任务信息';

INSERT INTO `sj_job` VALUES (1,'dev','ruoyi_group','demo-job',NULL,1,1710344035622,1,1,4,1,'testJobExecutor',2,'60',1,60,3,1,1,116,0,'',1,'','','',0,'2026-01-04 13:38:04','2026-01-04 13:38:04');

DROP TABLE IF EXISTS `sj_job_executor`;

CREATE TABLE `sj_job_executor` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `namespace_id` varchar(64) NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
  `group_name` varchar(64) NOT NULL COMMENT '组名称',
  `executor_info` varchar(256) NOT NULL COMMENT '任务执行器名称',
  `executor_type` varchar(3) NOT NULL COMMENT '1:java 2:python 3:go',
  `create_dt` datetime NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  `update_dt` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_namespace_id_group_name` (`namespace_id`,`group_name`),
  KEY `idx_create_dt` (`create_dt`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务执行器信息';

DROP TABLE IF EXISTS `sj_job_log_message`;

CREATE TABLE `sj_job_log_message` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `namespace_id` varchar(64) NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
  `group_name` varchar(64) NOT NULL COMMENT '组名称',
  `job_id` bigint(20) NOT NULL COMMENT '任务信息id',
  `task_batch_id` bigint(20) NOT NULL COMMENT '任务批次id',
  `task_id` bigint(20) NOT NULL COMMENT '调度任务id',
  `message` longtext NOT NULL COMMENT '调度信息',
  `log_num` int(11) NOT NULL DEFAULT 1 COMMENT '日志数量',
  `real_time` bigint(13) NOT NULL DEFAULT 0 COMMENT '上报时间',
  `ext_attrs` varchar(256) DEFAULT '' COMMENT '扩展字段',
  `create_dt` datetime NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_batch_id_task_id` (`task_batch_id`,`task_id`),
  KEY `idx_create_dt` (`create_dt`),
  KEY `idx_namespace_id_group_name` (`namespace_id`,`group_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='调度日志';

DROP TABLE IF EXISTS `sj_job_summary`;

CREATE TABLE `sj_job_summary` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `namespace_id` varchar(64) NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
  `group_name` varchar(64) NOT NULL DEFAULT '' COMMENT '组名称',
  `business_id` bigint(20) NOT NULL COMMENT '业务id (job_id或workflow_id)',
  `system_task_type` tinyint(4) NOT NULL DEFAULT 3 COMMENT '任务类型 3、JOB任务 4、WORKFLOW任务',
  `trigger_at` datetime NOT NULL DEFAULT current_timestamp() COMMENT '统计时间',
  `success_num` int(11) NOT NULL DEFAULT 0 COMMENT '执行成功-日志数量',
  `fail_num` int(11) NOT NULL DEFAULT 0 COMMENT '执行失败-日志数量',
  `fail_reason` varchar(512) NOT NULL DEFAULT '' COMMENT '失败原因',
  `stop_num` int(11) NOT NULL DEFAULT 0 COMMENT '执行失败-日志数量',
  `stop_reason` varchar(512) NOT NULL DEFAULT '' COMMENT '失败原因',
  `cancel_num` int(11) NOT NULL DEFAULT 0 COMMENT '执行失败-日志数量',
  `cancel_reason` varchar(512) NOT NULL DEFAULT '' COMMENT '失败原因',
  `create_dt` datetime NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  `update_dt` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_trigger_at_system_task_type_business_id` (`trigger_at`,`system_task_type`,`business_id`) USING BTREE,
  KEY `idx_namespace_id_group_name_business_id` (`namespace_id`,`group_name`,`business_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='DashBoard_Job';

DROP TABLE IF EXISTS `sj_job_task`;

CREATE TABLE `sj_job_task` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `namespace_id` varchar(64) NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
  `group_name` varchar(64) NOT NULL COMMENT '组名称',
  `job_id` bigint(20) NOT NULL COMMENT '任务信息id',
  `task_batch_id` bigint(20) NOT NULL COMMENT '调度任务id',
  `parent_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '父执行器id',
  `task_status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '执行的状态 0、失败 1、成功',
  `retry_count` int(11) NOT NULL DEFAULT 0 COMMENT '重试次数',
  `mr_stage` tinyint(4) DEFAULT NULL COMMENT '动态分片所处阶段 1:map 2:reduce 3:mergeReduce',
  `leaf` tinyint(4) NOT NULL DEFAULT 1 COMMENT '叶子节点',
  `task_name` varchar(255) NOT NULL DEFAULT '' COMMENT '任务名称',
  `client_info` varchar(128) DEFAULT NULL COMMENT '客户端地址 clientId#ip:port',
  `wf_context` text DEFAULT NULL COMMENT '工作流全局上下文',
  `result_message` text NOT NULL COMMENT '执行结果',
  `args_str` text DEFAULT NULL COMMENT '执行方法参数',
  `args_type` tinyint(4) NOT NULL DEFAULT 1 COMMENT '参数类型 ',
  `ext_attrs` varchar(256) DEFAULT '' COMMENT '扩展字段',
  `create_dt` datetime NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  `update_dt` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_batch_id_task_status` (`task_batch_id`,`task_status`),
  KEY `idx_create_dt` (`create_dt`),
  KEY `idx_namespace_id_group_name` (`namespace_id`,`group_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务实例';

DROP TABLE IF EXISTS `sj_job_task_batch`;

CREATE TABLE `sj_job_task_batch` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `namespace_id` varchar(64) NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
  `group_name` varchar(64) NOT NULL COMMENT '组名称',
  `job_id` bigint(20) NOT NULL COMMENT '任务id',
  `workflow_node_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '工作流节点id',
  `parent_workflow_node_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '工作流任务父批次id',
  `workflow_task_batch_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '工作流任务批次id',
  `task_batch_status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '任务批次状态 0、失败 1、成功',
  `operation_reason` tinyint(4) NOT NULL DEFAULT 0 COMMENT '操作原因',
  `execution_at` bigint(13) NOT NULL DEFAULT 0 COMMENT '任务执行时间',
  `system_task_type` tinyint(4) NOT NULL DEFAULT 3 COMMENT '任务类型 3、JOB任务 4、WORKFLOW任务',
  `parent_id` varchar(64) NOT NULL DEFAULT '' COMMENT '父节点',
  `ext_attrs` varchar(256) DEFAULT '' COMMENT '扩展字段',
  `deleted` tinyint(4) NOT NULL DEFAULT 0 COMMENT '逻辑删除 1、删除',
  `create_dt` datetime NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  `update_dt` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_job_id_task_batch_status` (`job_id`,`task_batch_status`),
  KEY `idx_create_dt` (`create_dt`),
  KEY `idx_namespace_id_group_name` (`namespace_id`,`group_name`),
  KEY `idx_workflow_task_batch_id_workflow_node_id` (`workflow_task_batch_id`,`workflow_node_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务批次';

DROP TABLE IF EXISTS `sj_namespace`;

CREATE TABLE `sj_namespace` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(64) NOT NULL COMMENT '名称',
  `unique_id` varchar(64) NOT NULL COMMENT '唯一id',
  `description` varchar(256) NOT NULL DEFAULT '' COMMENT '描述',
  `deleted` tinyint(4) NOT NULL DEFAULT 0 COMMENT '逻辑删除 1、删除',
  `create_dt` datetime NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  `update_dt` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_unique_id` (`unique_id`),
  KEY `idx_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COMMENT='命名空间';

INSERT INTO `sj_namespace` VALUES (1,'Development','dev','',0,'2026-01-04 13:38:01','2026-01-04 13:38:01'),(2,'Production','prod','',0,'2026-01-04 13:38:01','2026-01-04 13:38:01');

DROP TABLE IF EXISTS `sj_notify_config`;

CREATE TABLE `sj_notify_config` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `namespace_id` varchar(64) NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
  `group_name` varchar(64) NOT NULL COMMENT '组名称',
  `notify_name` varchar(64) NOT NULL DEFAULT '' COMMENT '通知名称',
  `system_task_type` tinyint(4) NOT NULL DEFAULT 3 COMMENT '任务类型 1. 重试任务 2. 重试回调 3、JOB任务 4、WORKFLOW任务',
  `notify_status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '通知状态 0、未启用 1、启用',
  `recipient_ids` varchar(128) NOT NULL COMMENT '接收人id列表',
  `notify_threshold` int(11) NOT NULL DEFAULT 0 COMMENT '通知阈值',
  `notify_scene` tinyint(4) NOT NULL DEFAULT 0 COMMENT '通知场景',
  `rate_limiter_status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '限流状态 0、未启用 1、启用',
  `rate_limiter_threshold` int(11) NOT NULL DEFAULT 0 COMMENT '每秒限流阈值',
  `description` varchar(256) NOT NULL DEFAULT '' COMMENT '描述',
  `create_dt` datetime NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  `update_dt` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_namespace_id_group_name_scene_name` (`namespace_id`,`group_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知配置';

DROP TABLE IF EXISTS `sj_notify_recipient`;

CREATE TABLE `sj_notify_recipient` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `namespace_id` varchar(64) NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
  `recipient_name` varchar(64) NOT NULL COMMENT '接收人名称',
  `notify_type` tinyint(4) NOT NULL DEFAULT 0 COMMENT '通知类型 1、钉钉 2、邮件 3、企业微信 4 飞书 5 webhook',
  `notify_attribute` varchar(512) NOT NULL COMMENT '配置属性',
  `description` varchar(256) NOT NULL DEFAULT '' COMMENT '描述',
  `create_dt` datetime NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  `update_dt` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_namespace_id` (`namespace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警通知接收人';

DROP TABLE IF EXISTS `sj_retry`;

CREATE TABLE `sj_retry` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `namespace_id` varchar(64) NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
  `group_name` varchar(64) NOT NULL COMMENT '组名称',
  `group_id` bigint(20) NOT NULL COMMENT '组Id',
  `scene_name` varchar(64) NOT NULL COMMENT '场景名称',
  `scene_id` bigint(20) NOT NULL COMMENT '场景ID',
  `idempotent_id` varchar(64) NOT NULL COMMENT '幂等id',
  `biz_no` varchar(64) NOT NULL DEFAULT '' COMMENT '业务编号',
  `executor_name` varchar(512) NOT NULL DEFAULT '' COMMENT '执行器名称',
  `args_str` text NOT NULL COMMENT '执行方法参数',
  `ext_attrs` text NOT NULL COMMENT '扩展字段',
  `serializer_name` varchar(32) NOT NULL DEFAULT 'jackson' COMMENT '执行方法参数序列化器名称',
  `next_trigger_at` bigint(13) NOT NULL COMMENT '下次触发时间',
  `retry_count` int(11) NOT NULL DEFAULT 0 COMMENT '重试次数',
  `retry_status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '重试状态 0、重试中 1、成功 2、最大重试次数',
  `task_type` tinyint(4) NOT NULL DEFAULT 1 COMMENT '任务类型 1、重试数据 2、回调数据',
  `bucket_index` int(11) NOT NULL DEFAULT 0 COMMENT 'bucket',
  `parent_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '父节点id',
  `deleted` bigint(20) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_dt` datetime NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  `update_dt` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scene_tasktype_idempotentid_deleted` (`scene_id`,`task_type`,`idempotent_id`,`deleted`),
  KEY `idx_biz_no` (`biz_no`),
  KEY `idx_idempotent_id` (`idempotent_id`),
  KEY `idx_retry_status_bucket_index` (`retry_status`,`bucket_index`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_create_dt` (`create_dt`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='重试信息表';

DROP TABLE IF EXISTS `sj_retry_dead_letter`;

CREATE TABLE `sj_retry_dead_letter` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `namespace_id` varchar(64) NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
  `group_name` varchar(64) NOT NULL COMMENT '组名称',
  `group_id` bigint(20) NOT NULL COMMENT '组Id',
  `scene_name` varchar(64) NOT NULL COMMENT '场景名称',
  `scene_id` bigint(20) NOT NULL COMMENT '场景ID',
  `idempotent_id` varchar(64) NOT NULL COMMENT '幂等id',
  `biz_no` varchar(64) NOT NULL DEFAULT '' COMMENT '业务编号',
  `executor_name` varchar(512) NOT NULL DEFAULT '' COMMENT '执行器名称',
  `serializer_name` varchar(32) NOT NULL DEFAULT 'jackson' COMMENT '执行方法参数序列化器名称',
  `args_str` text NOT NULL COMMENT '执行方法参数',
  `ext_attrs` text NOT NULL COMMENT '扩展字段',
  `create_dt` datetime NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_namespace_id_group_name_scene_name` (`namespace_id`,`group_name`,`scene_name`),
  KEY `idx_idempotent_id` (`idempotent_id`),
  KEY `idx_biz_no` (`biz_no`),
  KEY `idx_create_dt` (`create_dt`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='死信队列表';

DROP TABLE IF EXISTS `sj_retry_scene_config`;

CREATE TABLE `sj_retry_scene_config` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `namespace_id` varchar(64) NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
  `scene_name` varchar(64) NOT NULL COMMENT '场景名称',
  `group_name` varchar(64) NOT NULL COMMENT '组名称',
  `scene_status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '组状态 0、未启用 1、启用',
  `max_retry_count` int(11) NOT NULL DEFAULT 5 COMMENT '最大重试次数',
  `back_off` tinyint(4) NOT NULL DEFAULT 1 COMMENT '1、默认等级 2、固定间隔时间 3、CRON 表达式',
  `trigger_interval` varchar(16) NOT NULL DEFAULT '' COMMENT '间隔时长',
  `notify_ids` varchar(128) NOT NULL DEFAULT '' COMMENT '通知告警场景配置id列表',
  `deadline_request` bigint(20) unsigned NOT NULL DEFAULT 60000 COMMENT 'Deadline Request 调用链超时 单位毫秒',
  `executor_timeout` int(11) unsigned NOT NULL DEFAULT 5 COMMENT '任务执行超时时间，单位秒',
  `route_key` tinyint(4) NOT NULL DEFAULT 4 COMMENT '路由策略',
  `block_strategy` tinyint(4) NOT NULL DEFAULT 1 COMMENT '阻塞策略 1、丢弃 2、覆盖 3、并行',
  `cb_status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '回调状态 0、不开启 1、开启',
  `cb_trigger_type` tinyint(4) NOT NULL DEFAULT 1 COMMENT '1、默认等级 2、固定间隔时间 3、CRON 表达式',
  `cb_max_count` int(11) NOT NULL DEFAULT 16 COMMENT '回调的最大执行次数',
  `cb_trigger_interval` varchar(16) NOT NULL DEFAULT '' COMMENT '回调的最大执行次数',
  `owner_id` bigint(20) DEFAULT NULL COMMENT '负责人id',
  `labels` varchar(512) DEFAULT '' COMMENT '标签',
  `description` varchar(256) NOT NULL DEFAULT '' COMMENT '描述',
  `create_dt` datetime NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  `update_dt` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_namespace_id_group_name_scene_name` (`namespace_id`,`group_name`,`scene_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='场景配置';

DROP TABLE IF EXISTS `sj_retry_summary`;

CREATE TABLE `sj_retry_summary` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `namespace_id` varchar(64) NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
  `group_name` varchar(64) NOT NULL DEFAULT '' COMMENT '组名称',
  `scene_name` varchar(64) NOT NULL DEFAULT '' COMMENT '场景名称',
  `trigger_at` datetime NOT NULL DEFAULT current_timestamp() COMMENT '统计时间',
  `running_num` int(11) NOT NULL DEFAULT 0 COMMENT '重试中-日志数量',
  `finish_num` int(11) NOT NULL DEFAULT 0 COMMENT '重试完成-日志数量',
  `max_count_num` int(11) NOT NULL DEFAULT 0 COMMENT '重试到达最大次数-日志数量',
  `suspend_num` int(11) NOT NULL DEFAULT 0 COMMENT '暂停重试-日志数量',
  `create_dt` datetime NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  `update_dt` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scene_name_trigger_at` (`namespace_id`,`group_name`,`scene_name`,`trigger_at`) USING BTREE,
  KEY `idx_trigger_at` (`trigger_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='DashBoard_Retry';

DROP TABLE IF EXISTS `sj_retry_task`;

CREATE TABLE `sj_retry_task` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `namespace_id` varchar(64) NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
  `group_name` varchar(64) NOT NULL COMMENT '组名称',
  `scene_name` varchar(64) NOT NULL COMMENT '场景名称',
  `retry_id` bigint(20) NOT NULL COMMENT '重试信息Id',
  `ext_attrs` text NOT NULL COMMENT '扩展字段',
  `task_status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '重试状态',
  `task_type` tinyint(4) NOT NULL DEFAULT 1 COMMENT '任务类型 1、重试数据 2、回调数据',
  `operation_reason` tinyint(4) NOT NULL DEFAULT 0 COMMENT '操作原因',
  `client_info` varchar(128) DEFAULT NULL COMMENT '客户端地址 clientId#ip:port',
  `create_dt` datetime NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  `update_dt` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_group_name_scene_name` (`namespace_id`,`group_name`,`scene_name`),
  KEY `task_status` (`task_status`),
  KEY `idx_create_dt` (`create_dt`),
  KEY `idx_retry_id` (`retry_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='重试任务表';

DROP TABLE IF EXISTS `sj_retry_task_log_message`;

CREATE TABLE `sj_retry_task_log_message` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `namespace_id` varchar(64) NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
  `group_name` varchar(64) NOT NULL COMMENT '组名称',
  `retry_id` bigint(20) NOT NULL COMMENT '重试信息Id',
  `retry_task_id` bigint(20) NOT NULL COMMENT '重试任务Id',
  `message` longtext NOT NULL COMMENT '异常信息',
  `log_num` int(11) NOT NULL DEFAULT 1 COMMENT '日志数量',
  `real_time` bigint(13) NOT NULL DEFAULT 0 COMMENT '上报时间',
  `create_dt` datetime NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_namespace_id_group_name_retry_task_id` (`namespace_id`,`group_name`,`retry_task_id`),
  KEY `idx_create_dt` (`create_dt`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务调度日志信息记录表';

DROP TABLE IF EXISTS `sj_server_node`;

CREATE TABLE `sj_server_node` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `namespace_id` varchar(64) NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
  `group_name` varchar(64) NOT NULL COMMENT '组名称',
  `host_id` varchar(64) NOT NULL COMMENT '主机id',
  `host_ip` varchar(64) NOT NULL COMMENT '机器ip',
  `host_port` int(16) NOT NULL COMMENT '机器端口',
  `expire_at` datetime NOT NULL COMMENT '过期时间',
  `node_type` tinyint(4) NOT NULL COMMENT '节点类型 1、客户端 2、是服务端',
  `ext_attrs` varchar(256) DEFAULT '' COMMENT '扩展字段',
  `labels` varchar(512) DEFAULT '' COMMENT '标签',
  `create_dt` datetime NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  `update_dt` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_host_id_host_ip` (`host_id`,`host_ip`),
  KEY `idx_namespace_id_group_name` (`namespace_id`,`group_name`),
  KEY `idx_expire_at_node_type` (`expire_at`,`node_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务器节点';

DROP TABLE IF EXISTS `sj_system_user`;

CREATE TABLE `sj_system_user` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` varchar(64) NOT NULL COMMENT '账号',
  `password` varchar(128) NOT NULL COMMENT '密码',
  `role` tinyint(4) NOT NULL DEFAULT 0 COMMENT '角色：1-普通用户、2-管理员',
  `create_dt` datetime NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  `update_dt` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

INSERT INTO `sj_system_user` VALUES (1,'admin','465c194afb65670f38322df087f0a9bb225cc257e43eb4ac5a0c98ef5b3173ac',2,'2026-01-04 13:38:04','2026-01-04 13:38:04');

DROP TABLE IF EXISTS `sj_system_user_permission`;
CREATE TABLE `sj_system_user_permission` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `group_name` varchar(64) NOT NULL COMMENT '组名称',
  `namespace_id` varchar(64) NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
  `system_user_id` bigint(20) NOT NULL COMMENT '系统用户id',
  `create_dt` datetime NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  `update_dt` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_namespace_id_group_name_system_user_id` (`namespace_id`,`group_name`,`system_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户权限表';

DROP TABLE IF EXISTS `sj_workflow`;

CREATE TABLE `sj_workflow` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `workflow_name` varchar(64) NOT NULL COMMENT '工作流名称',
  `namespace_id` varchar(64) NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
  `group_name` varchar(64) NOT NULL COMMENT '组名称',
  `workflow_status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '工作流状态 0、关闭、1、开启',
  `trigger_type` tinyint(4) NOT NULL COMMENT '触发类型 1.CRON 表达式 2. 固定时间',
  `trigger_interval` varchar(255) NOT NULL COMMENT '间隔时长',
  `next_trigger_at` bigint(20) NOT NULL COMMENT '下次触发时间',
  `block_strategy` tinyint(4) NOT NULL DEFAULT 1 COMMENT '阻塞策略 1、丢弃 2、覆盖 3、并行',
  `executor_timeout` int(11) NOT NULL DEFAULT 0 COMMENT '任务执行超时时间，单位秒',
  `description` varchar(256) NOT NULL DEFAULT '' COMMENT '描述',
  `flow_info` text DEFAULT NULL COMMENT '流程信息',
  `wf_context` text DEFAULT NULL COMMENT '上下文',
  `notify_ids` varchar(128) NOT NULL DEFAULT '' COMMENT '通知告警场景配置id列表',
  `bucket_index` int(11) NOT NULL DEFAULT 0 COMMENT 'bucket',
  `version` int(11) NOT NULL COMMENT '版本号',
  `owner_id` bigint(20) DEFAULT NULL COMMENT '负责人id',
  `ext_attrs` varchar(256) DEFAULT '' COMMENT '扩展字段',
  `deleted` tinyint(4) NOT NULL DEFAULT 0 COMMENT '逻辑删除 1、删除',
  `create_dt` datetime NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  `update_dt` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_create_dt` (`create_dt`),
  KEY `idx_namespace_id_group_name` (`namespace_id`,`group_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流';

DROP TABLE IF EXISTS `sj_workflow_node`;

CREATE TABLE `sj_workflow_node` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `namespace_id` varchar(64) NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
  `node_name` varchar(64) NOT NULL COMMENT '节点名称',
  `group_name` varchar(64) NOT NULL COMMENT '组名称',
  `job_id` bigint(20) NOT NULL COMMENT '任务信息id',
  `workflow_id` bigint(20) NOT NULL COMMENT '工作流ID',
  `node_type` tinyint(4) NOT NULL DEFAULT 1 COMMENT '1、任务节点 2、条件节点',
  `expression_type` tinyint(4) NOT NULL DEFAULT 0 COMMENT '1、SpEl、2、Aviator 3、QL',
  `fail_strategy` tinyint(4) NOT NULL DEFAULT 1 COMMENT '失败策略 1、跳过 2、阻塞',
  `workflow_node_status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '工作流节点状态 0、关闭、1、开启',
  `priority_level` int(11) NOT NULL DEFAULT 1 COMMENT '优先级',
  `node_info` text DEFAULT NULL COMMENT '节点信息 ',
  `version` int(11) NOT NULL COMMENT '版本号',
  `ext_attrs` varchar(256) DEFAULT '' COMMENT '扩展字段',
  `deleted` tinyint(4) NOT NULL DEFAULT 0 COMMENT '逻辑删除 1、删除',
  `create_dt` datetime NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  `update_dt` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_create_dt` (`create_dt`),
  KEY `idx_namespace_id_group_name` (`namespace_id`,`group_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流节点';

DROP TABLE IF EXISTS `sj_workflow_task_batch`;

CREATE TABLE `sj_workflow_task_batch` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `namespace_id` varchar(64) NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
  `group_name` varchar(64) NOT NULL COMMENT '组名称',
  `workflow_id` bigint(20) NOT NULL COMMENT '工作流任务id',
  `task_batch_status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '任务批次状态 0、失败 1、成功',
  `operation_reason` tinyint(4) NOT NULL DEFAULT 0 COMMENT '操作原因',
  `flow_info` text DEFAULT NULL COMMENT '流程信息',
  `wf_context` text DEFAULT NULL COMMENT '全局上下文',
  `execution_at` bigint(13) NOT NULL DEFAULT 0 COMMENT '任务执行时间',
  `ext_attrs` varchar(256) DEFAULT '' COMMENT '扩展字段',
  `version` int(11) NOT NULL DEFAULT 1 COMMENT '版本号',
  `deleted` tinyint(4) NOT NULL DEFAULT 0 COMMENT '逻辑删除 1、删除',
  `create_dt` datetime NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  `update_dt` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_job_id_task_batch_status` (`workflow_id`,`task_batch_status`),
  KEY `idx_create_dt` (`create_dt`),
  KEY `idx_namespace_id_group_name` (`namespace_id`,`group_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流批次';

DROP TABLE IF EXISTS `sys_client`;

CREATE TABLE `sys_client` (
  `id` bigint(20) NOT NULL COMMENT 'id',
  `client_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '客户端id',
  `client_key` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '客户端key',
  `client_secret` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '客户端秘钥',
  `grant_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '授权类型',
  `device_type` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '设备类型',
  `active_timeout` int(11) DEFAULT 1800 COMMENT 'token活跃超时时间',
  `timeout` int(11) DEFAULT 604800 COMMENT 'token固定超时',
  `status` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `del_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统授权表';

INSERT INTO `sys_client` VALUES (1,'e5cd7e4891bf95d1d19206ce24a7b32e','pc','pc123','password,social','pc',1800,604800,'0','0',103,1,'2026-01-04 13:37:52',1,'2026-01-04 13:37:52'),(2,'428a8310cd442757ae699df5d894f051','app','app123','password,sms,social','android',1800,604800,'0','0',103,1,'2026-01-04 13:37:52',1,'2026-01-04 13:37:52');

DROP TABLE IF EXISTS `sys_config`;

CREATE TABLE `sys_config` (
  `config_id` bigint(20) NOT NULL COMMENT '参数主键',
  `tenant_id` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT '000000' COMMENT '租户编号',
  `config_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '参数名称',
  `config_key` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '参数键名',
  `config_value` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '参数键值',
  `config_type` char(1) COLLATE utf8mb4_unicode_ci DEFAULT 'N' COMMENT '系统内置（Y是 N否）',
  `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`config_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='参数配置表';

INSERT INTO `sys_config` VALUES (1,'000000','主框架页-默认皮肤样式名称','sys.index.skinName','skin-blue','Y',103,1,'2026-01-04 13:37:50',NULL,NULL,'蓝色 skin-blue、绿色 skin-green、紫色 skin-purple、红色 skin-red、黄色 skin-yellow'),(2,'000000','用户管理-账号初始密码','sys.user.initPassword','123456','Y',103,1,'2026-01-04 13:37:50',NULL,NULL,'初始化密码 123456'),(3,'000000','主框架页-侧边栏主题','sys.index.sideTheme','theme-dark','Y',103,1,'2026-01-04 13:37:50',NULL,NULL,'深色主题theme-dark，浅色主题theme-light'),(5,'000000','账号自助-是否开启用户注册功能','sys.account.registerUser','false','Y',103,1,'2026-01-04 13:37:50',NULL,NULL,'是否开启注册用户功能（true开启，false关闭）'),(11,'000000','OSS预览列表资源开关','sys.oss.previewListResource','true','Y',103,1,'2026-01-04 13:37:50',NULL,NULL,'true:开启, false:关闭');

DROP TABLE IF EXISTS `sys_dept`;

CREATE TABLE `sys_dept` (
  `dept_id` bigint(20) NOT NULL COMMENT '部门id',
  `tenant_id` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT '000000' COMMENT '租户编号',
  `parent_id` bigint(20) DEFAULT 0 COMMENT '父部门id',
  `ancestors` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '祖级列表',
  `dept_name` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '部门名称',
  `dept_category` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '部门类别编码',
  `order_num` int(4) DEFAULT 0 COMMENT '显示顺序',
  `leader` bigint(20) DEFAULT NULL COMMENT '负责人',
  `phone` varchar(11) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '联系电话',
  `email` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邮箱',
  `status` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '部门状态（0正常 1停用）',
  `del_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门表';

INSERT INTO `sys_dept` VALUES (100,'000000',0,'0','XXX科技',NULL,0,NULL,'15888888888','xxx@qq.com','0','0',103,1,'2026-01-04 13:37:38',NULL,NULL),(101,'000000',100,'0,100','深圳总公司',NULL,1,NULL,'15888888888','xxx@qq.com','0','0',103,1,'2026-01-04 13:37:38',NULL,NULL),(102,'000000',100,'0,100','长沙分公司',NULL,2,NULL,'15888888888','xxx@qq.com','0','0',103,1,'2026-01-04 13:37:38',NULL,NULL),(103,'000000',101,'0,100,101','研发部门',NULL,1,1,'15888888888','xxx@qq.com','0','0',103,1,'2026-01-04 13:37:38',NULL,NULL),(104,'000000',101,'0,100,101','市场部门',NULL,2,NULL,'15888888888','xxx@qq.com','0','0',103,1,'2026-01-04 13:37:38',NULL,NULL),(105,'000000',101,'0,100,101','测试部门',NULL,3,NULL,'15888888888','xxx@qq.com','0','0',103,1,'2026-01-04 13:37:38',NULL,NULL),(106,'000000',101,'0,100,101','财务部门',NULL,4,NULL,'15888888888','xxx@qq.com','0','0',103,1,'2026-01-04 13:37:38',NULL,NULL),(107,'000000',101,'0,100,101','运维部门',NULL,5,NULL,'15888888888','xxx@qq.com','0','0',103,1,'2026-01-04 13:37:38',NULL,NULL),(108,'000000',102,'0,100,102','市场部门',NULL,1,NULL,'15888888888','xxx@qq.com','0','0',103,1,'2026-01-04 13:37:38',NULL,NULL),(109,'000000',102,'0,100,102','财务部门',NULL,2,NULL,'15888888888','xxx@qq.com','0','0',103,1,'2026-01-04 13:37:38',NULL,NULL);

DROP TABLE IF EXISTS `sys_dict_data`;

CREATE TABLE `sys_dict_data` (
  `dict_code` bigint(20) NOT NULL COMMENT '字典编码',
  `tenant_id` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT '000000' COMMENT '租户编号',
  `dict_sort` int(4) DEFAULT 0 COMMENT '字典排序',
  `dict_label` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '字典标签',
  `dict_value` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '字典键值',
  `dict_type` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '字典类型',
  `css_class` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '样式属性（其他样式扩展）',
  `list_class` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '表格回显样式',
  `is_default` char(1) COLLATE utf8mb4_unicode_ci DEFAULT 'N' COMMENT '是否默认（Y是 N否）',
  `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`dict_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典数据表';

INSERT INTO `sys_dict_data` VALUES (1,'000000',1,'男','0','sys_user_sex','','','Y',103,1,'2026-01-04 13:37:48',NULL,NULL,'性别男'),(2,'000000',2,'女','1','sys_user_sex','','','N',103,1,'2026-01-04 13:37:48',NULL,NULL,'性别女'),(3,'000000',3,'未知','2','sys_user_sex','','','N',103,1,'2026-01-04 13:37:48',NULL,NULL,'性别未知'),(4,'000000',1,'显示','0','sys_show_hide','','primary','Y',103,1,'2026-01-04 13:37:48',NULL,NULL,'显示菜单'),(5,'000000',2,'隐藏','1','sys_show_hide','','danger','N',103,1,'2026-01-04 13:37:48',NULL,NULL,'隐藏菜单'),(6,'000000',1,'正常','0','sys_normal_disable','','primary','Y',103,1,'2026-01-04 13:37:48',NULL,NULL,'正常状态'),(7,'000000',2,'停用','1','sys_normal_disable','','danger','N',103,1,'2026-01-04 13:37:48',NULL,NULL,'停用状态'),(12,'000000',1,'是','Y','sys_yes_no','','primary','Y',103,1,'2026-01-04 13:37:48',NULL,NULL,'系统默认是'),(13,'000000',2,'否','N','sys_yes_no','','danger','N',103,1,'2026-01-04 13:37:48',NULL,NULL,'系统默认否'),(14,'000000',1,'通知','1','sys_notice_type','','warning','Y',103,1,'2026-01-04 13:37:49',NULL,NULL,'通知'),(15,'000000',2,'公告','2','sys_notice_type','','success','N',103,1,'2026-01-04 13:37:49',NULL,NULL,'公告'),(16,'000000',1,'正常','0','sys_notice_status','','primary','Y',103,1,'2026-01-04 13:37:49',NULL,NULL,'正常状态'),(17,'000000',2,'关闭','1','sys_notice_status','','danger','N',103,1,'2026-01-04 13:37:49',NULL,NULL,'关闭状态'),(18,'000000',1,'新增','1','sys_oper_type','','info','N',103,1,'2026-01-04 13:37:49',NULL,NULL,'新增操作'),(19,'000000',2,'修改','2','sys_oper_type','','info','N',103,1,'2026-01-04 13:37:49',NULL,NULL,'修改操作'),(20,'000000',3,'删除','3','sys_oper_type','','danger','N',103,1,'2026-01-04 13:37:49',NULL,NULL,'删除操作'),(21,'000000',4,'授权','4','sys_oper_type','','primary','N',103,1,'2026-01-04 13:37:49',NULL,NULL,'授权操作'),(22,'000000',5,'导出','5','sys_oper_type','','warning','N',103,1,'2026-01-04 13:37:49',NULL,NULL,'导出操作'),(23,'000000',6,'导入','6','sys_oper_type','','warning','N',103,1,'2026-01-04 13:37:49',NULL,NULL,'导入操作'),(24,'000000',7,'强退','7','sys_oper_type','','danger','N',103,1,'2026-01-04 13:37:49',NULL,NULL,'强退操作'),(25,'000000',8,'生成代码','8','sys_oper_type','','warning','N',103,1,'2026-01-04 13:37:49',NULL,NULL,'生成操作'),(26,'000000',9,'清空数据','9','sys_oper_type','','danger','N',103,1,'2026-01-04 13:37:49',NULL,NULL,'清空操作'),(27,'000000',1,'成功','0','sys_common_status','','primary','N',103,1,'2026-01-04 13:37:49',NULL,NULL,'正常状态'),(28,'000000',2,'失败','1','sys_common_status','','danger','N',103,1,'2026-01-04 13:37:49',NULL,NULL,'停用状态'),(29,'000000',99,'其他','0','sys_oper_type','','info','N',103,1,'2026-01-04 13:37:49',NULL,NULL,'其他操作'),(30,'000000',0,'密码认证','password','sys_grant_type','el-check-tag','default','N',103,1,'2026-01-04 13:37:49',NULL,NULL,'密码认证'),(31,'000000',0,'短信认证','sms','sys_grant_type','el-check-tag','default','N',103,1,'2026-01-04 13:37:49',NULL,NULL,'短信认证'),(32,'000000',0,'邮件认证','email','sys_grant_type','el-check-tag','default','N',103,1,'2026-01-04 13:37:49',NULL,NULL,'邮件认证'),(33,'000000',0,'小程序认证','xcx','sys_grant_type','el-check-tag','default','N',103,1,'2026-01-04 13:37:49',NULL,NULL,'小程序认证'),(34,'000000',0,'三方登录认证','social','sys_grant_type','el-check-tag','default','N',103,1,'2026-01-04 13:37:49',NULL,NULL,'三方登录认证'),(35,'000000',0,'PC','pc','sys_device_type','','default','N',103,1,'2026-01-04 13:37:49',NULL,NULL,'PC'),(36,'000000',0,'安卓','android','sys_device_type','','default','N',103,1,'2026-01-04 13:37:49',NULL,NULL,'安卓'),(37,'000000',0,'iOS','ios','sys_device_type','','default','N',103,1,'2026-01-04 13:37:49',NULL,NULL,'iOS'),(38,'000000',0,'小程序','xcx','sys_device_type','','default','N',103,1,'2026-01-04 13:37:49',NULL,NULL,'小程序'),(39,'000000',1,'已撤销','cancel','wf_business_status','','danger','N',103,1,'2026-01-04 13:38:18',NULL,NULL,'已撤销'),(40,'000000',2,'草稿','draft','wf_business_status','','info','N',103,1,'2026-01-04 13:38:18',NULL,NULL,'草稿'),(41,'000000',3,'待审核','waiting','wf_business_status','','primary','N',103,1,'2026-01-04 13:38:18',NULL,NULL,'待审核'),(42,'000000',4,'已完成','finish','wf_business_status','','success','N',103,1,'2026-01-04 13:38:18',NULL,NULL,'已完成'),(43,'000000',5,'已作废','invalid','wf_business_status','','danger','N',103,1,'2026-01-04 13:38:18',NULL,NULL,'已作废'),(44,'000000',6,'已退回','back','wf_business_status','','danger','N',103,1,'2026-01-04 13:38:18',NULL,NULL,'已退回'),(45,'000000',7,'已终止','termination','wf_business_status','','danger','N',103,1,'2026-01-04 13:38:18',NULL,NULL,'已终止'),(46,'000000',1,'自定义表单','static','wf_form_type','','success','N',103,1,'2026-01-04 13:38:18',NULL,NULL,'自定义表单'),(47,'000000',2,'动态表单','dynamic','wf_form_type','','primary','N',103,1,'2026-01-04 13:38:18',NULL,NULL,'动态表单'),(48,'000000',1,'撤销','cancel','wf_task_status','','danger','N',103,1,'2026-01-04 13:38:18',NULL,NULL,'撤销'),(49,'000000',2,'通过','pass','wf_task_status','','success','N',103,1,'2026-01-04 13:38:18',NULL,NULL,'通过'),(50,'000000',3,'待审核','waiting','wf_task_status','','primary','N',103,1,'2026-01-04 13:38:18',NULL,NULL,'待审核'),(51,'000000',4,'作废','invalid','wf_task_status','','danger','N',103,1,'2026-01-04 13:38:18',NULL,NULL,'作废'),(52,'000000',5,'退回','back','wf_task_status','','danger','N',103,1,'2026-01-04 13:38:18',NULL,NULL,'退回'),(53,'000000',6,'终止','termination','wf_task_status','','danger','N',103,1,'2026-01-04 13:38:18',NULL,NULL,'终止'),(54,'000000',7,'转办','transfer','wf_task_status','','primary','N',103,1,'2026-01-04 13:38:18',NULL,NULL,'转办'),(55,'000000',8,'委托','depute','wf_task_status','','primary','N',103,1,'2026-01-04 13:38:18',NULL,NULL,'委托'),(56,'000000',9,'抄送','copy','wf_task_status','','primary','N',103,1,'2026-01-04 13:38:18',NULL,NULL,'抄送'),(57,'000000',10,'加签','sign','wf_task_status','','primary','N',103,1,'2026-01-04 13:38:18',NULL,NULL,'加签'),(58,'000000',11,'减签','sign_off','wf_task_status','','danger','N',103,1,'2026-01-04 13:38:18',NULL,NULL,'减签'),(59,'000000',11,'超时','timeout','wf_task_status','','danger','N',103,1,'2026-01-04 13:38:18',NULL,NULL,'超时');

DROP TABLE IF EXISTS `sys_dict_type`;

CREATE TABLE `sys_dict_type` (
  `dict_id` bigint(20) NOT NULL COMMENT '字典主键',
  `tenant_id` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT '000000' COMMENT '租户编号',
  `dict_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '字典名称',
  `dict_type` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '字典类型',
  `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`dict_id`),
  UNIQUE KEY `tenant_id` (`tenant_id`,`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典类型表';

INSERT INTO `sys_dict_type` VALUES (1,'000000','用户性别','sys_user_sex',103,1,'2026-01-04 13:37:48',NULL,NULL,'用户性别列表'),(2,'000000','菜单状态','sys_show_hide',103,1,'2026-01-04 13:37:48',NULL,NULL,'菜单状态列表'),(3,'000000','系统开关','sys_normal_disable',103,1,'2026-01-04 13:37:48',NULL,NULL,'系统开关列表'),(6,'000000','系统是否','sys_yes_no',103,1,'2026-01-04 13:37:48',NULL,NULL,'系统是否列表'),(7,'000000','通知类型','sys_notice_type',103,1,'2026-01-04 13:37:48',NULL,NULL,'通知类型列表'),(8,'000000','通知状态','sys_notice_status',103,1,'2026-01-04 13:37:48',NULL,NULL,'通知状态列表'),(9,'000000','操作类型','sys_oper_type',103,1,'2026-01-04 13:37:48',NULL,NULL,'操作类型列表'),(10,'000000','系统状态','sys_common_status',103,1,'2026-01-04 13:37:48',NULL,NULL,'登录状态列表'),(11,'000000','授权类型','sys_grant_type',103,1,'2026-01-04 13:37:48',NULL,NULL,'认证授权类型'),(12,'000000','设备类型','sys_device_type',103,1,'2026-01-04 13:37:48',NULL,NULL,'客户端设备类型'),(13,'000000','业务状态','wf_business_status',103,1,'2026-01-04 13:38:18',NULL,NULL,'业务状态列表'),(14,'000000','表单类型','wf_form_type',103,1,'2026-01-04 13:38:18',NULL,NULL,'表单类型列表'),(15,'000000','任务状态','wf_task_status',103,1,'2026-01-04 13:38:18',NULL,NULL,'任务状态');

DROP TABLE IF EXISTS `sys_logininfor`;

CREATE TABLE `sys_logininfor` (
  `info_id` bigint(20) NOT NULL COMMENT '访问ID',
  `tenant_id` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT '000000' COMMENT '租户编号',
  `user_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '用户账号',
  `client_key` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '客户端',
  `device_type` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '设备类型',
  `ipaddr` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '登录IP地址',
  `login_location` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '登录地点',
  `browser` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '浏览器类型',
  `os` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '操作系统',
  `status` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '登录状态（0成功 1失败）',
  `msg` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '提示消息',
  `login_time` datetime DEFAULT NULL COMMENT '访问时间',
  PRIMARY KEY (`info_id`),
  KEY `idx_sys_logininfor_s` (`status`),
  KEY `idx_sys_logininfor_lt` (`login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统访问记录';

INSERT INTO `sys_logininfor` VALUES (2007697637324152833,'000000','admin','pc','pc','0:0:0:0:0:0:0:1','内网IP','Firefox','Windows 10 or Windows Server 2016','0','登录成功','2026-01-04 14:16:32'),(2007700697089892353,'000000','admin','pc','pc','0:0:0:0:0:0:0:1','内网IP','Firefox','Windows 10 or Windows Server 2016','0','退出成功','2026-01-04 14:28:42'),(2007705350133309441,'000000','admin','pc','pc','0:0:0:0:0:0:0:1','内网IP','Firefox','Windows 10 or Windows Server 2016','0','登录成功','2026-01-04 14:47:11'),(2007706585104179202,'000000','admin','pc','pc','0:0:0:0:0:0:0:1','内网IP','Firefox','Windows 10 or Windows Server 2016','0','退出成功','2026-01-04 14:52:05'),(2007711552753950722,'000000','admin','pc','pc','0:0:0:0:0:0:0:1','内网IP','Firefox','Windows 10 or Windows Server 2016','1','验证码已失效','2026-01-04 15:11:50'),(2007711589156315138,'000000','admin','pc','pc','0:0:0:0:0:0:0:1','内网IP','Firefox','Windows 10 or Windows Server 2016','0','登录成功','2026-01-04 15:11:58'),(2007713473933926401,'000000','admin','pc','pc','0:0:0:0:0:0:0:1','内网IP','Firefox','Windows 10 or Windows Server 2016','0','登录成功','2026-01-04 15:19:28'),(2007718047021326338,'000000','admin','pc','pc','0:0:0:0:0:0:0:1','内网IP','Firefox','Windows 10 or Windows Server 2016','0','退出成功','2026-01-04 15:37:38'),(2007718067992846337,'000000','admin','pc','pc','0:0:0:0:0:0:0:1','内网IP','Firefox','Windows 10 or Windows Server 2016','0','登录成功','2026-01-04 15:37:43'),(2007727066876272642,'000000','admin','pc','pc','0:0:0:0:0:0:0:1','内网IP','Firefox','Windows 10 or Windows Server 2016','0','退出成功','2026-01-04 16:13:29'),(2007727084051947521,'000000','admin','pc','pc','0:0:0:0:0:0:0:1','内网IP','Firefox','Windows 10 or Windows Server 2016','0','登录成功','2026-01-04 16:13:33'),(2007756347098587138,'000000','admin','pc','pc','0:0:0:0:0:0:0:1','内网IP','Firefox','Windows 10 or Windows Server 2016','0','登录成功','2026-01-04 18:09:50'),(2007756727106723841,'000000','admin','pc','pc','0:0:0:0:0:0:0:1','内网IP','Firefox','Windows 10 or Windows Server 2016','0','退出成功','2026-01-04 18:11:20'),(2007756824875950082,'000000','admin','pc','pc','0:0:0:0:0:0:0:1','内网IP','Firefox','Windows 10 or Windows Server 2016','0','登录成功','2026-01-04 18:11:44'),(2007772174678024194,'000000','admin','pc','pc','0:0:0:0:0:0:0:1','内网IP','Firefox','Windows 10 or Windows Server 2016','0','退出成功','2026-01-04 19:12:43'),(2007772197641838594,'000000','admin','pc','pc','0:0:0:0:0:0:0:1','内网IP','Firefox','Windows 10 or Windows Server 2016','0','登录成功','2026-01-04 19:12:49'),(2007785816985792513,'000000','admin','pc','pc','183.14.133.73','中国|广东省|深圳市|电信','Firefox','Windows 10 or Windows Server 2016','0','登录成功','2026-01-04 20:06:56'),(2007787625590648834,'000000','admin','pc','pc','183.14.133.73','中国|广东省|深圳市|电信','MSEdge','Windows 10 or Windows Server 2016','0','登录成功','2026-01-04 20:14:07'),(2007787956059860993,'000000','admin','pc','pc','180.79.247.152','中国|北京|北京市|歌华','Firefox','Windows 10 or Windows Server 2016','0','登录成功','2026-01-04 20:15:26'),(2007804669862178818,'000000','admin','pc','pc','120.229.34.58','中国|广东省|深圳市|移动','Chrome','OSX','0','登录成功','2026-01-04 21:21:51'),(2007982405847183361,'000000','admin','pc','pc','103.95.70.98','中国|北京|北京市|铁通','Chrome','OSX','0','登录成功','2026-01-05 09:08:06'),(2007982439359672322,'000000','admin','pc','pc','103.95.70.98','中国|北京|北京市|铁通','Chrome','OSX','0','登录成功','2026-01-05 09:08:14'),(2007984779005358081,'000000','admin','pc','pc','103.95.70.98','中国|北京|北京市|铁通','Chrome','OSX','0','登录成功','2026-01-05 09:17:32'),(2007989656058163201,'000000','admin','pc','pc','183.14.132.233','中国|广东省|深圳市|电信','Firefox','Windows 10 or Windows Server 2016','0','登录成功','2026-01-05 09:36:55'),(2008376055227838466,'000000','admin','pc','pc','103.95.70.98','中国|北京|北京市|铁通','MicroMessenger','OSX','0','登录成功','2026-01-06 11:12:20'),(2008399357065392129,'000000','admin','pc','pc','183.14.132.233','中国|广东省|深圳市|电信','Firefox','Windows 10 or Windows Server 2016','0','登录成功','2026-01-06 12:44:55');

DROP TABLE IF EXISTS `sys_menu`;

CREATE TABLE `sys_menu` (
  `menu_id` bigint(20) NOT NULL COMMENT '菜单ID',
  `menu_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '菜单名称',
  `parent_id` bigint(20) DEFAULT 0 COMMENT '父菜单ID',
  `order_num` int(4) DEFAULT 0 COMMENT '显示顺序',
  `path` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '路由地址',
  `component` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '组件路径',
  `query_param` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '路由参数',
  `is_frame` int(1) DEFAULT 1 COMMENT '是否为外链（0是 1否）',
  `is_cache` int(1) DEFAULT 0 COMMENT '是否缓存（0缓存 1不缓存）',
  `menu_type` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '菜单类型（M目录 C菜单 F按钮）',
  `visible` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '显示状态（0显示 1隐藏）',
  `status` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '菜单状态（0正常 1停用）',
  `perms` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '权限标识',
  `icon` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '#' COMMENT '菜单图标',
  `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  PRIMARY KEY (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜单权限表';

INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,`path`,component,query_param,is_frame,is_cache,menu_type,visible,status,perms,icon,create_dept,create_by,create_time,update_by,update_time,remark) VALUES
	 (1,'系统管理',0,10,'system','','',1,0,'M','0','0','','system',103,1,'2026-01-04 13:37:39',1,'2026-01-31 13:05:26','系统管理目录'),
	 (2,'系统监控',0,11,'monitor','','',1,0,'M','0','0','','monitor',103,1,'2026-01-04 13:37:40',1,'2026-01-31 13:05:13','系统监控目录'),
	 (3,'系统工具',0,12,'tool','','',1,0,'M','1','1','','tool',103,1,'2026-01-04 13:37:40',1,'2026-01-31 13:05:02','系统工具目录'),
	 (4,'PLUS官网',0,16,'https://gitee.com/dromara/RuoYi-Vue-Plus','','',0,0,'M','1','1','','guide',103,1,'2026-01-04 13:37:40',1,'2026-01-31 13:04:43','RuoYi-Vue-Plus官网地址'),
	 (5,'测试菜单',0,13,'demo','','',1,0,'M','1','1','','star',103,1,'2026-01-04 13:37:40',1,'2026-01-31 13:04:31','测试菜单'),
	 (6,'租户管理',0,9,'tenant','','',1,0,'M','1','1','','chart',103,1,'2026-01-04 13:37:39',1,'2026-01-31 13:05:46','租户管理目录'),
	 (100,'用户管理',1,1,'user','system/user/index','',1,0,'C','0','0','system:user:list','user',103,1,'2026-01-04 13:37:40',NULL,NULL,'用户管理菜单'),
	 (101,'角色管理',1,2,'role','system/role/index','',1,0,'C','0','0','system:role:list','peoples',103,1,'2026-01-04 13:37:40',NULL,NULL,'角色管理菜单'),
	 (102,'菜单管理',1,3,'menu','system/menu/index','',1,0,'C','0','0','system:menu:list','tree-table',103,1,'2026-01-04 13:37:40',NULL,NULL,'菜单管理菜单'),
	 (103,'部门管理',1,4,'dept','system/dept/index','',1,0,'C','0','0','system:dept:list','tree',103,1,'2026-01-04 13:37:40',NULL,NULL,'部门管理菜单');
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,`path`,component,query_param,is_frame,is_cache,menu_type,visible,status,perms,icon,create_dept,create_by,create_time,update_by,update_time,remark) VALUES
	 (104,'岗位管理',1,5,'post','system/post/index','',1,0,'C','0','0','system:post:list','post',103,1,'2026-01-04 13:37:40',NULL,NULL,'岗位管理菜单'),
	 (105,'字典管理',1,6,'dict','system/dict/index','',1,0,'C','0','0','system:dict:list','dict',103,1,'2026-01-04 13:37:40',NULL,NULL,'字典管理菜单'),
	 (106,'参数设置',1,7,'config','system/config/index','',1,0,'C','0','0','system:config:list','edit',103,1,'2026-01-04 13:37:40',NULL,NULL,'参数设置菜单'),
	 (107,'通知公告',1,8,'notice','system/notice/index','',1,0,'C','0','0','system:notice:list','message',103,1,'2026-01-04 13:37:40',NULL,NULL,'通知公告菜单'),
	 (108,'日志管理',1,9,'log','','',1,0,'M','0','0','','log',103,1,'2026-01-04 13:37:40',NULL,NULL,'日志管理菜单'),
	 (109,'在线用户',2,1,'online','monitor/online/index','',1,0,'C','0','0','monitor:online:list','online',103,1,'2026-01-04 13:37:40',NULL,NULL,'在线用户菜单'),
	 (113,'缓存监控',2,5,'cache','monitor/cache/index','',1,0,'C','0','0','monitor:cache:list','redis',103,1,'2026-01-04 13:37:40',NULL,NULL,'缓存监控菜单'),
	 (115,'代码生成',3,2,'gen','tool/gen/index','',1,0,'C','0','0','tool:gen:list','code',103,1,'2026-01-04 13:37:40',NULL,NULL,'代码生成菜单'),
	 (116,'修改生成配置',3,2,'gen-edit/index/:tableId','tool/gen/editTable','',1,1,'C','1','0','tool:gen:edit','#',103,1,'2026-01-04 13:37:40',NULL,NULL,'/tool/gen'),
	 (117,'Admin监控',2,5,'Admin','monitor/admin/index','',1,0,'C','0','0','monitor:admin:list','dashboard',103,1,'2026-01-04 13:37:40',NULL,NULL,'Admin监控菜单');
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,`path`,component,query_param,is_frame,is_cache,menu_type,visible,status,perms,icon,create_dept,create_by,create_time,update_by,update_time,remark) VALUES
	 (118,'文件管理',1,10,'oss','system/oss/index','',1,0,'C','0','0','system:oss:list','upload',103,1,'2026-01-04 13:37:40',NULL,NULL,'文件管理菜单'),
	 (120,'任务调度中心',2,6,'snailjob','monitor/snailjob/index','',1,0,'C','0','0','monitor:snailjob:list','job',103,1,'2026-01-04 13:37:40',NULL,NULL,'SnailJob控制台菜单'),
	 (121,'租户管理',6,1,'tenant','system/tenant/index','',1,0,'C','0','0','system:tenant:list','list',103,1,'2026-01-04 13:37:40',NULL,NULL,'租户管理菜单'),
	 (122,'租户套餐管理',6,2,'tenantPackage','system/tenantPackage/index','',1,0,'C','0','0','system:tenantPackage:list','form',103,1,'2026-01-04 13:37:40',NULL,NULL,'租户套餐管理菜单'),
	 (123,'客户端管理',1,11,'client','system/client/index','',1,0,'C','0','0','system:client:list','international',103,1,'2026-01-04 13:37:40',NULL,NULL,'客户端管理菜单'),
	 (130,'分配用户',1,2,'role-auth/user/:roleId','system/role/authUser','',1,1,'C','1','0','system:role:edit','#',103,1,'2026-01-04 13:37:40',NULL,NULL,'/system/role'),
	 (131,'分配角色',1,1,'user-auth/role/:userId','system/user/authRole','',1,1,'C','1','0','system:user:edit','#',103,1,'2026-01-04 13:37:40',NULL,NULL,'/system/user'),
	 (132,'字典数据',1,6,'dict-data/index/:dictId','system/dict/data','',1,1,'C','1','0','system:dict:list','#',103,1,'2026-01-04 13:37:40',NULL,NULL,'/system/dict'),
	 (133,'文件配置管理',1,10,'oss-config/index','system/oss/config','',1,1,'C','1','0','system:ossConfig:list','#',103,1,'2026-01-04 13:37:40',NULL,NULL,'/system/oss'),
	 (500,'操作日志',108,1,'operlog','monitor/operlog/index','',1,0,'C','0','0','monitor:operlog:list','form',103,1,'2026-01-04 13:37:40',NULL,NULL,'操作日志菜单');
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,`path`,component,query_param,is_frame,is_cache,menu_type,visible,status,perms,icon,create_dept,create_by,create_time,update_by,update_time,remark) VALUES
	 (501,'登录日志',108,2,'logininfor','monitor/logininfor/index','',1,0,'C','0','0','monitor:logininfor:list','logininfor',103,1,'2026-01-04 13:37:40',NULL,NULL,'登录日志菜单'),
	 (1001,'用户查询',100,1,'','','',1,0,'F','0','0','system:user:query','#',103,1,'2026-01-04 13:37:40',NULL,NULL,''),
	 (1002,'用户新增',100,2,'','','',1,0,'F','0','0','system:user:add','#',103,1,'2026-01-04 13:37:40',NULL,NULL,''),
	 (1003,'用户修改',100,3,'','','',1,0,'F','0','0','system:user:edit','#',103,1,'2026-01-04 13:37:40',NULL,NULL,''),
	 (1004,'用户删除',100,4,'','','',1,0,'F','0','0','system:user:remove','#',103,1,'2026-01-04 13:37:40',NULL,NULL,''),
	 (1005,'用户导出',100,5,'','','',1,0,'F','0','0','system:user:export','#',103,1,'2026-01-04 13:37:40',NULL,NULL,''),
	 (1006,'用户导入',100,6,'','','',1,0,'F','0','0','system:user:import','#',103,1,'2026-01-04 13:37:41',NULL,NULL,''),
	 (1007,'重置密码',100,7,'','','',1,0,'F','0','0','system:user:resetPwd','#',103,1,'2026-01-04 13:37:41',NULL,NULL,''),
	 (1008,'角色查询',101,1,'','','',1,0,'F','0','0','system:role:query','#',103,1,'2026-01-04 13:37:41',NULL,NULL,''),
	 (1009,'角色新增',101,2,'','','',1,0,'F','0','0','system:role:add','#',103,1,'2026-01-04 13:37:41',NULL,NULL,'');
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,`path`,component,query_param,is_frame,is_cache,menu_type,visible,status,perms,icon,create_dept,create_by,create_time,update_by,update_time,remark) VALUES
	 (1010,'角色修改',101,3,'','','',1,0,'F','0','0','system:role:edit','#',103,1,'2026-01-04 13:37:41',NULL,NULL,''),
	 (1011,'角色删除',101,4,'','','',1,0,'F','0','0','system:role:remove','#',103,1,'2026-01-04 13:37:41',NULL,NULL,''),
	 (1012,'角色导出',101,5,'','','',1,0,'F','0','0','system:role:export','#',103,1,'2026-01-04 13:37:41',NULL,NULL,''),
	 (1013,'菜单查询',102,1,'','','',1,0,'F','0','0','system:menu:query','#',103,1,'2026-01-04 13:37:41',NULL,NULL,''),
	 (1014,'菜单新增',102,2,'','','',1,0,'F','0','0','system:menu:add','#',103,1,'2026-01-04 13:37:41',NULL,NULL,''),
	 (1015,'菜单修改',102,3,'','','',1,0,'F','0','0','system:menu:edit','#',103,1,'2026-01-04 13:37:41',NULL,NULL,''),
	 (1016,'菜单删除',102,4,'','','',1,0,'F','0','0','system:menu:remove','#',103,1,'2026-01-04 13:37:41',NULL,NULL,''),
	 (1017,'部门查询',103,1,'','','',1,0,'F','0','0','system:dept:query','#',103,1,'2026-01-04 13:37:41',NULL,NULL,''),
	 (1018,'部门新增',103,2,'','','',1,0,'F','0','0','system:dept:add','#',103,1,'2026-01-04 13:37:41',NULL,NULL,''),
	 (1019,'部门修改',103,3,'','','',1,0,'F','0','0','system:dept:edit','#',103,1,'2026-01-04 13:37:41',NULL,NULL,'');
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,`path`,component,query_param,is_frame,is_cache,menu_type,visible,status,perms,icon,create_dept,create_by,create_time,update_by,update_time,remark) VALUES
	 (1020,'部门删除',103,4,'','','',1,0,'F','0','0','system:dept:remove','#',103,1,'2026-01-04 13:37:41',NULL,NULL,''),
	 (1021,'岗位查询',104,1,'','','',1,0,'F','0','0','system:post:query','#',103,1,'2026-01-04 13:37:41',NULL,NULL,''),
	 (1022,'岗位新增',104,2,'','','',1,0,'F','0','0','system:post:add','#',103,1,'2026-01-04 13:37:41',NULL,NULL,''),
	 (1023,'岗位修改',104,3,'','','',1,0,'F','0','0','system:post:edit','#',103,1,'2026-01-04 13:37:41',NULL,NULL,''),
	 (1024,'岗位删除',104,4,'','','',1,0,'F','0','0','system:post:remove','#',103,1,'2026-01-04 13:37:41',NULL,NULL,''),
	 (1025,'岗位导出',104,5,'','','',1,0,'F','0','0','system:post:export','#',103,1,'2026-01-04 13:37:41',NULL,NULL,''),
	 (1026,'字典查询',105,1,'#','','',1,0,'F','0','0','system:dict:query','#',103,1,'2026-01-04 13:37:41',NULL,NULL,''),
	 (1027,'字典新增',105,2,'#','','',1,0,'F','0','0','system:dict:add','#',103,1,'2026-01-04 13:37:41',NULL,NULL,''),
	 (1028,'字典修改',105,3,'#','','',1,0,'F','0','0','system:dict:edit','#',103,1,'2026-01-04 13:37:41',NULL,NULL,''),
	 (1029,'字典删除',105,4,'#','','',1,0,'F','0','0','system:dict:remove','#',103,1,'2026-01-04 13:37:41',NULL,NULL,'');
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,`path`,component,query_param,is_frame,is_cache,menu_type,visible,status,perms,icon,create_dept,create_by,create_time,update_by,update_time,remark) VALUES
	 (1030,'字典导出',105,5,'#','','',1,0,'F','0','0','system:dict:export','#',103,1,'2026-01-04 13:37:41',NULL,NULL,''),
	 (1031,'参数查询',106,1,'#','','',1,0,'F','0','0','system:config:query','#',103,1,'2026-01-04 13:37:41',NULL,NULL,''),
	 (1032,'参数新增',106,2,'#','','',1,0,'F','0','0','system:config:add','#',103,1,'2026-01-04 13:37:41',NULL,NULL,''),
	 (1033,'参数修改',106,3,'#','','',1,0,'F','0','0','system:config:edit','#',103,1,'2026-01-04 13:37:41',NULL,NULL,''),
	 (1034,'参数删除',106,4,'#','','',1,0,'F','0','0','system:config:remove','#',103,1,'2026-01-04 13:37:41',NULL,NULL,''),
	 (1035,'参数导出',106,5,'#','','',1,0,'F','0','0','system:config:export','#',103,1,'2026-01-04 13:37:41',NULL,NULL,''),
	 (1036,'公告查询',107,1,'#','','',1,0,'F','0','0','system:notice:query','#',103,1,'2026-01-04 13:37:41',NULL,NULL,''),
	 (1037,'公告新增',107,2,'#','','',1,0,'F','0','0','system:notice:add','#',103,1,'2026-01-04 13:37:41',NULL,NULL,''),
	 (1038,'公告修改',107,3,'#','','',1,0,'F','0','0','system:notice:edit','#',103,1,'2026-01-04 13:37:41',NULL,NULL,''),
	 (1039,'公告删除',107,4,'#','','',1,0,'F','0','0','system:notice:remove','#',103,1,'2026-01-04 13:37:41',NULL,NULL,'');
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,`path`,component,query_param,is_frame,is_cache,menu_type,visible,status,perms,icon,create_dept,create_by,create_time,update_by,update_time,remark) VALUES
	 (1040,'操作查询',500,1,'#','','',1,0,'F','0','0','monitor:operlog:query','#',103,1,'2026-01-04 13:37:41',NULL,NULL,''),
	 (1041,'操作删除',500,2,'#','','',1,0,'F','0','0','monitor:operlog:remove','#',103,1,'2026-01-04 13:37:41',NULL,NULL,''),
	 (1042,'日志导出',500,4,'#','','',1,0,'F','0','0','monitor:operlog:export','#',103,1,'2026-01-04 13:37:41',NULL,NULL,''),
	 (1043,'登录查询',501,1,'#','','',1,0,'F','0','0','monitor:logininfor:query','#',103,1,'2026-01-04 13:37:42',NULL,NULL,''),
	 (1044,'登录删除',501,2,'#','','',1,0,'F','0','0','monitor:logininfor:remove','#',103,1,'2026-01-04 13:37:42',NULL,NULL,''),
	 (1045,'日志导出',501,3,'#','','',1,0,'F','0','0','monitor:logininfor:export','#',103,1,'2026-01-04 13:37:42',NULL,NULL,''),
	 (1046,'在线查询',109,1,'#','','',1,0,'F','0','0','monitor:online:query','#',103,1,'2026-01-04 13:37:42',NULL,NULL,''),
	 (1047,'批量强退',109,2,'#','','',1,0,'F','0','0','monitor:online:batchLogout','#',103,1,'2026-01-04 13:37:42',NULL,NULL,''),
	 (1048,'单条强退',109,3,'#','','',1,0,'F','0','0','monitor:online:forceLogout','#',103,1,'2026-01-04 13:37:42',NULL,NULL,''),
	 (1050,'账户解锁',501,4,'#','','',1,0,'F','0','0','monitor:logininfor:unlock','#',103,1,'2026-01-04 13:37:42',NULL,NULL,'');
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,`path`,component,query_param,is_frame,is_cache,menu_type,visible,status,perms,icon,create_dept,create_by,create_time,update_by,update_time,remark) VALUES
	 (1055,'生成查询',115,1,'#','','',1,0,'F','0','0','tool:gen:query','#',103,1,'2026-01-04 13:37:42',NULL,NULL,''),
	 (1056,'生成修改',115,2,'#','','',1,0,'F','0','0','tool:gen:edit','#',103,1,'2026-01-04 13:37:42',NULL,NULL,''),
	 (1057,'生成删除',115,3,'#','','',1,0,'F','0','0','tool:gen:remove','#',103,1,'2026-01-04 13:37:42',NULL,NULL,''),
	 (1058,'导入代码',115,2,'#','','',1,0,'F','0','0','tool:gen:import','#',103,1,'2026-01-04 13:37:42',NULL,NULL,''),
	 (1059,'预览代码',115,4,'#','','',1,0,'F','0','0','tool:gen:preview','#',103,1,'2026-01-04 13:37:42',NULL,NULL,''),
	 (1060,'生成代码',115,5,'#','','',1,0,'F','0','0','tool:gen:code','#',103,1,'2026-01-04 13:37:42',NULL,NULL,''),
	 (1061,'客户端管理查询',123,1,'#','','',1,0,'F','0','0','system:client:query','#',103,1,'2026-01-04 13:37:42',NULL,NULL,''),
	 (1062,'客户端管理新增',123,2,'#','','',1,0,'F','0','0','system:client:add','#',103,1,'2026-01-04 13:37:42',NULL,NULL,''),
	 (1063,'客户端管理修改',123,3,'#','','',1,0,'F','0','0','system:client:edit','#',103,1,'2026-01-04 13:37:43',NULL,NULL,''),
	 (1064,'客户端管理删除',123,4,'#','','',1,0,'F','0','0','system:client:remove','#',103,1,'2026-01-04 13:37:43',NULL,NULL,'');
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,`path`,component,query_param,is_frame,is_cache,menu_type,visible,status,perms,icon,create_dept,create_by,create_time,update_by,update_time,remark) VALUES
	 (1065,'客户端管理导出',123,5,'#','','',1,0,'F','0','0','system:client:export','#',103,1,'2026-01-04 13:37:43',NULL,NULL,''),
	 (1500,'测试单表',5,1,'demo','demo/demo/index','',1,0,'C','0','0','demo:demo:list','#',103,1,'2026-01-04 13:37:43',NULL,NULL,'测试单表菜单'),
	 (1501,'测试单表查询',1500,1,'#','','',1,0,'F','0','0','demo:demo:query','#',103,1,'2026-01-04 13:37:43',NULL,NULL,''),
	 (1502,'测试单表新增',1500,2,'#','','',1,0,'F','0','0','demo:demo:add','#',103,1,'2026-01-04 13:37:43',NULL,NULL,''),
	 (1503,'测试单表修改',1500,3,'#','','',1,0,'F','0','0','demo:demo:edit','#',103,1,'2026-01-04 13:37:43',NULL,NULL,''),
	 (1504,'测试单表删除',1500,4,'#','','',1,0,'F','0','0','demo:demo:remove','#',103,1,'2026-01-04 13:37:43',NULL,NULL,''),
	 (1505,'测试单表导出',1500,5,'#','','',1,0,'F','0','0','demo:demo:export','#',103,1,'2026-01-04 13:37:43',NULL,NULL,''),
	 (1506,'测试树表',5,1,'tree','demo/tree/index','',1,0,'C','0','0','demo:tree:list','#',103,1,'2026-01-04 13:37:43',NULL,NULL,'测试树表菜单'),
	 (1507,'测试树表查询',1506,1,'#','','',1,0,'F','0','0','demo:tree:query','#',103,1,'2026-01-04 13:37:43',NULL,NULL,''),
	 (1508,'测试树表新增',1506,2,'#','','',1,0,'F','0','0','demo:tree:add','#',103,1,'2026-01-04 13:37:43',NULL,NULL,'');
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,`path`,component,query_param,is_frame,is_cache,menu_type,visible,status,perms,icon,create_dept,create_by,create_time,update_by,update_time,remark) VALUES
	 (1509,'测试树表修改',1506,3,'#','','',1,0,'F','0','0','demo:tree:edit','#',103,1,'2026-01-04 13:37:43',NULL,NULL,''),
	 (1510,'测试树表删除',1506,4,'#','','',1,0,'F','0','0','demo:tree:remove','#',103,1,'2026-01-04 13:37:43',NULL,NULL,''),
	 (1511,'测试树表导出',1506,5,'#','','',1,0,'F','0','0','demo:tree:export','#',103,1,'2026-01-04 13:37:43',NULL,NULL,''),
	 (1600,'文件查询',118,1,'#','','',1,0,'F','0','0','system:oss:query','#',103,1,'2026-01-04 13:37:42',NULL,NULL,''),
	 (1601,'文件上传',118,2,'#','','',1,0,'F','0','0','system:oss:upload','#',103,1,'2026-01-04 13:37:42',NULL,NULL,''),
	 (1602,'文件下载',118,3,'#','','',1,0,'F','0','0','system:oss:download','#',103,1,'2026-01-04 13:37:42',NULL,NULL,''),
	 (1603,'文件删除',118,4,'#','','',1,0,'F','0','0','system:oss:remove','#',103,1,'2026-01-04 13:37:42',NULL,NULL,''),
	 (1606,'租户查询',121,1,'#','','',1,0,'F','0','0','system:tenant:query','#',103,1,'2026-01-04 13:37:42',NULL,NULL,''),
	 (1607,'租户新增',121,2,'#','','',1,0,'F','0','0','system:tenant:add','#',103,1,'2026-01-04 13:37:42',NULL,NULL,''),
	 (1608,'租户修改',121,3,'#','','',1,0,'F','0','0','system:tenant:edit','#',103,1,'2026-01-04 13:37:42',NULL,NULL,'');
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,`path`,component,query_param,is_frame,is_cache,menu_type,visible,status,perms,icon,create_dept,create_by,create_time,update_by,update_time,remark) VALUES
	 (1609,'租户删除',121,4,'#','','',1,0,'F','0','0','system:tenant:remove','#',103,1,'2026-01-04 13:37:42',NULL,NULL,''),
	 (1610,'租户导出',121,5,'#','','',1,0,'F','0','0','system:tenant:export','#',103,1,'2026-01-04 13:37:42',NULL,NULL,''),
	 (1611,'租户套餐查询',122,1,'#','','',1,0,'F','0','0','system:tenantPackage:query','#',103,1,'2026-01-04 13:37:42',NULL,NULL,''),
	 (1612,'租户套餐新增',122,2,'#','','',1,0,'F','0','0','system:tenantPackage:add','#',103,1,'2026-01-04 13:37:42',NULL,NULL,''),
	 (1613,'租户套餐修改',122,3,'#','','',1,0,'F','0','0','system:tenantPackage:edit','#',103,1,'2026-01-04 13:37:42',NULL,NULL,''),
	 (1614,'租户套餐删除',122,4,'#','','',1,0,'F','0','0','system:tenantPackage:remove','#',103,1,'2026-01-04 13:37:42',NULL,NULL,''),
	 (1615,'租户套餐导出',122,5,'#','','',1,0,'F','0','0','system:tenantPackage:export','#',103,1,'2026-01-04 13:37:42',NULL,NULL,''),
	 (1620,'配置列表',118,5,'#','','',1,0,'F','0','0','system:ossConfig:list','#',103,1,'2026-01-04 13:37:42',NULL,NULL,''),
	 (1621,'配置添加',118,6,'#','','',1,0,'F','0','0','system:ossConfig:add','#',103,1,'2026-01-04 13:37:42',NULL,NULL,''),
	 (1622,'配置编辑',118,6,'#','','',1,0,'F','0','0','system:ossConfig:edit','#',103,1,'2026-01-04 13:37:42',NULL,NULL,'');
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,`path`,component,query_param,is_frame,is_cache,menu_type,visible,status,perms,icon,create_dept,create_by,create_time,update_by,update_time,remark) VALUES
	 (1623,'配置删除',118,6,'#','','',1,0,'F','0','0','system:ossConfig:remove','#',103,1,'2026-01-04 13:37:42',NULL,NULL,''),
	 (11616,'工作流',0,14,'workflow','','',1,0,'M','1','1','','workflow',103,1,'2026-01-04 13:38:17',1,'2026-01-31 13:04:22',''),
	 (11618,'我的任务',0,15,'task','','',1,0,'M','1','1','','my-task',103,1,'2026-01-04 13:38:17',1,'2026-01-31 13:04:11',''),
	 (11619,'我的待办',11618,2,'taskWaiting','workflow/task/taskWaiting','',1,1,'C','0','0','','waiting',103,1,'2026-01-04 13:38:17',NULL,NULL,''),
	 (11620,'流程定义',11616,3,'processDefinition','workflow/processDefinition/index','',1,1,'C','0','0','','process-definition',103,1,'2026-01-04 13:38:17',NULL,NULL,''),
	 (11621,'流程实例',11630,1,'processInstance','workflow/processInstance/index','',1,1,'C','0','0','','tree-table',103,1,'2026-01-04 13:38:17',NULL,NULL,''),
	 (11622,'流程分类',11616,1,'category','workflow/category/index','',1,0,'C','0','0','workflow:category:list','category',103,1,'2026-01-04 13:38:17',NULL,NULL,''),
	 (11623,'流程分类查询',11622,1,'#','','',1,0,'F','0','0','workflow:category:query','#',103,1,'2026-01-04 13:38:17',NULL,NULL,''),
	 (11624,'流程分类新增',11622,2,'#','','',1,0,'F','0','0','workflow:category:add','#',103,1,'2026-01-04 13:38:17',NULL,NULL,''),
	 (11625,'流程分类修改',11622,3,'#','','',1,0,'F','0','0','workflow:category:edit','#',103,1,'2026-01-04 13:38:17',NULL,NULL,'');
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,`path`,component,query_param,is_frame,is_cache,menu_type,visible,status,perms,icon,create_dept,create_by,create_time,update_by,update_time,remark) VALUES
	 (11626,'流程分类删除',11622,4,'#','','',1,0,'F','0','0','workflow:category:remove','#',103,1,'2026-01-04 13:38:17',NULL,NULL,''),
	 (11627,'流程分类导出',11622,5,'#','','',1,0,'F','0','0','workflow:category:export','#',103,1,'2026-01-04 13:38:17',NULL,NULL,''),
	 (11629,'我发起的',11618,1,'myDocument','workflow/task/myDocument','',1,1,'C','0','0','','guide',103,1,'2026-01-04 13:38:17',NULL,NULL,''),
	 (11630,'流程监控',11616,4,'monitor','','',1,0,'M','0','0','','monitor',103,1,'2026-01-04 13:38:17',NULL,NULL,''),
	 (11631,'待办任务',11630,2,'allTaskWaiting','workflow/task/allTaskWaiting','',1,1,'C','0','0','','waiting',103,1,'2026-01-04 13:38:17',NULL,NULL,''),
	 (11632,'我的已办',11618,3,'taskFinish','workflow/task/taskFinish','',1,1,'C','0','0','','finish',103,1,'2026-01-04 13:38:17',NULL,NULL,''),
	 (11633,'我的抄送',11618,4,'taskCopyList','workflow/task/taskCopyList','',1,1,'C','0','0','','my-copy',103,1,'2026-01-04 13:38:17',NULL,NULL,''),
	 (11638,'请假申请',5,1,'leave','workflow/leave/index','',1,0,'C','0','0','workflow:leave:list','#',103,1,'2026-01-04 13:38:18',NULL,NULL,'请假申请菜单'),
	 (11639,'请假申请查询',11638,1,'#','','',1,0,'F','0','0','workflow:leave:query','#',103,1,'2026-01-04 13:38:18',NULL,NULL,''),
	 (11640,'请假申请新增',11638,2,'#','','',1,0,'F','0','0','workflow:leave:add','#',103,1,'2026-01-04 13:38:18',NULL,NULL,'');
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,`path`,component,query_param,is_frame,is_cache,menu_type,visible,status,perms,icon,create_dept,create_by,create_time,update_by,update_time,remark) VALUES
	 (11641,'请假申请修改',11638,3,'#','','',1,0,'F','0','0','workflow:leave:edit','#',103,1,'2026-01-04 13:38:18',NULL,NULL,''),
	 (11642,'请假申请删除',11638,4,'#','','',1,0,'F','0','0','workflow:leave:remove','#',103,1,'2026-01-04 13:38:18',NULL,NULL,''),
	 (11643,'请假申请导出',11638,5,'#','','',1,0,'F','0','0','workflow:leave:export','#',103,1,'2026-01-04 13:38:18',NULL,NULL,''),
	 (11700,'流程设计',11616,5,'design/index','workflow/processDefinition/design','',1,1,'C','1','0','workflow:leave:edit','#',103,1,'2026-01-04 13:38:17',NULL,NULL,'/workflow/processDefinition'),
	 (11701,'请假申请',11616,6,'leaveEdit/index','workflow/leave/leaveEdit','',1,1,'C','1','0','workflow:leave:edit','#',103,1,'2026-01-04 13:38:17',NULL,NULL,''),
	 (11801,'流程表达式',11616,2,'spel','workflow/spel/index','',1,0,'C','0','0','workflow:spel:list','input',103,1,'2026-01-04 13:38:17',1,'2026-01-04 13:38:17','流程达式定义菜单'),
	 (11802,'流程达式定义查询',11801,1,'#','',NULL,1,0,'F','0','0','workflow:spel:query','#',103,1,'2026-01-04 13:38:17',NULL,NULL,''),
	 (11803,'流程达式定义新增',11801,2,'#','',NULL,1,0,'F','0','0','workflow:spel:add','#',103,1,'2026-01-04 13:38:17',NULL,NULL,''),
	 (11804,'流程达式定义修改',11801,3,'#','',NULL,1,0,'F','0','0','workflow:spel:edit','#',103,1,'2026-01-04 13:38:17',NULL,NULL,''),
	 (11805,'流程达式定义删除',11801,4,'#','',NULL,1,0,'F','0','0','workflow:spel:remove','#',103,1,'2026-01-04 13:38:17',NULL,NULL,'');
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,`path`,component,query_param,is_frame,is_cache,menu_type,visible,status,perms,icon,create_dept,create_by,create_time,update_by,update_time,remark) VALUES
	 (11806,'流程达式定义导出',11801,5,'#','',NULL,1,0,'F','0','0','workflow:spel:export','#',103,1,'2026-01-04 13:38:17',NULL,NULL,''),
	 (2007719445767184385,'内容管理',0,5,'content','',NULL,1,0,'M','0','0',NULL,'#',103,1,'2026-01-04 15:43:12',1,'2026-01-31 13:00:30',''),
	 (2007722330223599617,'通知公告',2007719445767184385,1,'news','content/notice/index',NULL,1,0,'C','0','0',NULL,'#',103,1,'2026-01-04 15:54:39',1,'2026-01-31 13:21:19',''),
	 (2007723016520785922,'应用集成',0,1,'integration','',NULL,1,0,'M','0','0',NULL,'#',103,1,'2026-01-04 15:57:23',1,'2026-01-31 13:09:20',''),
	 (2007723377579057154,'数据统计',0,6,'analysis','',NULL,1,0,'M','0','0',NULL,'#',103,1,'2026-01-04 15:58:49',1,'2026-01-31 13:14:16',''),
	 (2007724365341843458,'集成列表',2007723016520785922,2,'index','integration/index',NULL,1,0,'C','0','0',NULL,'#',103,1,'2026-01-04 16:02:45',1,'2026-01-31 13:24:24',''),
	 (2007724845128278018,'访问统计',2007723377579057154,3,'visit-statistics','visit-statistics/index',NULL,1,0,'C','0','0',NULL,'#',103,1,'2026-01-04 16:04:39',1,'2026-01-31 13:19:39',''),
	 (2007725345844289537,'报名统计',2007723377579057154,1,'registration-statistics','registration-statistics/index',NULL,1,0,'C','0','0',NULL,'#',103,1,'2026-01-04 16:06:38',1,'2026-01-31 13:19:17',''),
	 (2007726035928932353,'财务管理',0,4,'finance','',NULL,1,0,'M','0','0',NULL,'#',103,1,'2026-01-04 16:09:23',1,'2026-01-31 13:03:10',''),
	 (2007726312207736833,'发票管理',2007726035928932353,1,'invoice/index','invoice/index',NULL,1,0,'C','0','0',NULL,'#',103,1,'2026-01-04 16:10:29',1,'2026-01-31 12:54:55','');
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,`path`,component,query_param,is_frame,is_cache,menu_type,visible,status,perms,icon,create_dept,create_by,create_time,update_by,update_time,remark) VALUES
	 (2007726514670985217,'平台用户',0,3,'user','',NULL,1,0,'M','0','0',NULL,'#',103,1,'2026-01-04 16:11:17',1,'2026-01-31 13:02:40',''),
	 (2007726845874200577,'会员列表',2007726514670985217,1,'user/index','user/index',NULL,1,0,'C','0','0',NULL,'#',103,1,'2026-01-04 16:12:36',1,'2026-01-04 16:13:08',''),
	 (2017463443098218497,'订单管理',0,2,'order','',NULL,1,0,'M','0','0',NULL,'#',103,1,'2026-01-31 13:02:22',1,'2026-01-31 13:02:22',''),
	 (2017469599866728449,'退费管理',2007726035928932353,2,'refund','refund/index',NULL,1,0,'C','0','0',NULL,'#',103,1,'2026-01-31 13:26:50',1,'2026-01-31 13:26:50',''),
	 (2017470061756067841,'订单列表',2017463443098218497,1,'list','order/index',NULL,1,0,'C','0','0',NULL,'#',103,1,'2026-01-31 13:28:40',1,'2026-01-31 13:28:40','');


DROP TABLE IF EXISTS `sys_notice`;

CREATE TABLE `sys_notice` (
  `notice_id` bigint(20) NOT NULL COMMENT '公告ID',
  `tenant_id` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT '000000' COMMENT '租户编号',
  `notice_title` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '公告标题',
  `notice_type` char(1) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '公告类型（1通知 2公告）',
  `notice_content` longblob DEFAULT NULL COMMENT '公告内容',
  `status` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '公告状态（0正常 1关闭）',
  `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`notice_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知公告表';

INSERT INTO `sys_notice` VALUES (1,'000000','温馨提醒：2018-07-01 新版本发布啦','2',_binary '新版本内容','0',103,1,'2026-01-04 13:37:50',NULL,NULL,'管理员'),(2,'000000','维护通知：2018-07-01 系统凌晨维护','1',_binary '维护内容','0',103,1,'2026-01-04 13:37:50',NULL,NULL,'管理员');

DROP TABLE IF EXISTS `sys_oper_log`;
CREATE TABLE `sys_oper_log` (
  `oper_id` bigint(20) NOT NULL COMMENT '日志主键',
  `tenant_id` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT '000000' COMMENT '租户编号',
  `title` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '模块标题',
  `business_type` int(2) DEFAULT 0 COMMENT '业务类型（0其它 1新增 2修改 3删除）',
  `method` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '方法名称',
  `request_method` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '请求方式',
  `operator_type` int(1) DEFAULT 0 COMMENT '操作类别（0其它 1后台用户 2手机端用户）',
  `oper_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '操作人员',
  `dept_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '部门名称',
  `oper_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '请求URL',
  `oper_ip` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '主机地址',
  `oper_location` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '操作地点',
  `oper_param` varchar(4000) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '请求参数',
  `json_result` varchar(4000) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '返回参数',
  `status` int(1) DEFAULT 0 COMMENT '操作状态（0正常 1异常）',
  `error_msg` varchar(4000) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '错误消息',
  `oper_time` datetime DEFAULT NULL COMMENT '操作时间',
  `cost_time` bigint(20) DEFAULT 0 COMMENT '消耗时间',
  PRIMARY KEY (`oper_id`),
  KEY `idx_sys_oper_log_bt` (`business_type`),
  KEY `idx_sys_oper_log_s` (`status`),
  KEY `idx_sys_oper_log_ot` (`oper_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志记录';

INSERT INTO `sys_oper_log` VALUES (2007711823131369473,'000000','菜单管理',2,'org.dromara.system.controller.system.SysMenuController.edit()','PUT',1,'admin','研发部门','/system/menu','0:0:0:0:0:0:0:1','内网IP','{\"createDept\":null,\"createBy\":null,\"createTime\":null,\"updateBy\":null,\"updateTime\":null,\"menuId\":6,\"parentId\":0,\"menuName\":\"租户管理\",\"orderNum\":2,\"path\":\"tenant\",\"component\":\"\",\"queryParam\":\"\",\"isFrame\":\"1\",\"isCache\":\"0\",\"menuType\":\"M\",\"visible\":\"1\",\"status\":\"1\",\"perms\":\"\",\"icon\":\"chart\",\"remark\":\"租户管理目录\"}','{\"code\":200,\"msg\":\"操作成功\",\"data\":null}',0,'','2026-01-04 15:12:54',73),(2007711893209800706,'000000','菜单管理',2,'org.dromara.system.controller.system.SysMenuController.edit()','PUT',1,'admin','研发部门','/system/menu','0:0:0:0:0:0:0:1','内网IP','{\"createDept\":null,\"createBy\":null,\"createTime\":null,\"updateBy\":null,\"updateTime\":null,\"menuId\":4,\"parentId\":0,\"menuName\":\"PLUS官网\",\"orderNum\":5,\"path\":\"https://gitee.com/dromara/RuoYi-Vue-Plus\",\"component\":\"\",\"queryParam\":\"\",\"isFrame\":\"0\",\"isCache\":\"0\",\"menuType\":\"M\",\"visible\":\"1\",\"status\":\"1\",\"perms\":\"\",\"icon\":\"guide\",\"remark\":\"RuoYi-Vue-Plus官网地址\"}','{\"code\":200,\"msg\":\"操作成功\",\"data\":null}',0,'','2026-01-04 15:13:11',46),(2007711919877185537,'000000','菜单管理',2,'org.dromara.system.controller.system.SysMenuController.edit()','PUT',1,'admin','研发部门','/system/menu','0:0:0:0:0:0:0:1','内网IP','{\"createDept\":null,\"createBy\":null,\"createTime\":null,\"updateBy\":null,\"updateTime\":null,\"menuId\":11618,\"parentId\":0,\"menuName\":\"我的任务\",\"orderNum\":7,\"path\":\"task\",\"component\":\"\",\"queryParam\":\"\",\"isFrame\":\"1\",\"isCache\":\"0\",\"menuType\":\"M\",\"visible\":\"1\",\"status\":\"1\",\"perms\":\"\",\"icon\":\"my-task\",\"remark\":\"\"}','{\"code\":200,\"msg\":\"操作成功\",\"data\":null}',0,'','2026-01-04 15:13:17',37),(2007711942979411970,'000000','菜单管理',2,'org.dromara.system.controller.system.SysMenuController.edit()','PUT',1,'admin','研发部门','/system/menu','0:0:0:0:0:0:0:1','内网IP','{\"createDept\":null,\"createBy\":null,\"createTime\":null,\"updateBy\":null,\"updateTime\":null,\"menuId\":11616,\"parentId\":0,\"menuName\":\"工作流\",\"orderNum\":6,\"path\":\"workflow\",\"component\":\"\",\"queryParam\":\"\",\"isFrame\":\"1\",\"isCache\":\"0\",\"menuType\":\"M\",\"visible\":\"1\",\"status\":\"1\",\"perms\":\"\",\"icon\":\"workflow\",\"remark\":\"\"}','{\"code\":200,\"msg\":\"操作成功\",\"data\":null}',0,'','2026-01-04 15:13:23',40),(2007712025900802050,'000000','菜单管理',2,'org.dromara.system.controller.system.SysMenuController.edit()','PUT',1,'admin','研发部门','/system/menu','0:0:0:0:0:0:0:1','内网IP','{\"createDept\":null,\"createBy\":null,\"createTime\":null,\"updateBy\":null,\"updateTime\":null,\"menuId\":5,\"parentId\":0,\"menuName\":\"测试菜单\",\"orderNum\":5,\"path\":\"demo\",\"component\":\"\",\"queryParam\":\"\",\"isFrame\":\"1\",\"isCache\":\"0\",\"menuType\":\"M\",\"visible\":\"1\",\"status\":\"1\",\"perms\":\"\",\"icon\":\"star\",\"remark\":\"测试菜单\"}','{\"code\":200,\"msg\":\"操作成功\",\"data\":null}',0,'','2026-01-04 15:13:43',59),(2007715746596577281,'000000','菜单管理',2,'org.dromara.system.controller.system.SysMenuController.edit()','PUT',1,'admin','研发部门','/system/menu','0:0:0:0:0:0:0:1','内网IP','{\"createDept\":null,\"createBy\":null,\"createTime\":null,\"updateBy\":null,\"updateTime\":null,\"menuId\":3,\"parentId\":0,\"menuName\":\"系统工具\",\"orderNum\":4,\"path\":\"tool\",\"component\":\"\",\"queryParam\":\"\",\"isFrame\":\"1\",\"isCache\":\"0\",\"menuType\":\"M\",\"visible\":\"1\",\"status\":\"1\",\"perms\":\"\",\"icon\":\"tool\",\"remark\":\"系统工具目录\"}','{\"code\":200,\"msg\":\"操作成功\",\"data\":null}',0,'','2026-01-04 15:28:30',43),(2007719019172913153,'000000','菜单管理',2,'org.dromara.system.controller.system.SysMenuController.edit()','PUT',1,'admin','研发部门','/system/menu','0:0:0:0:0:0:0:1','内网IP','{\"createDept\":null,\"createBy\":null,\"createTime\":null,\"updateBy\":null,\"updateTime\":null,\"menuId\":11618,\"parentId\":0,\"menuName\":\"我的任务\",\"orderNum\":12,\"path\":\"task\",\"component\":\"\",\"queryParam\":\"\",\"isFrame\":\"1\",\"isCache\":\"0\",\"menuType\":\"M\",\"visible\":\"1\",\"status\":\"1\",\"perms\":\"\",\"icon\":\"my-task\",\"remark\":\"\"}','{\"code\":200,\"msg\":\"操作成功\",\"data\":null}',0,'','2026-01-04 15:41:30',43),(2007719041952178177,'000000','菜单管理',2,'org.dromara.system.controller.system.SysMenuController.edit()','PUT',1,'admin','研发部门','/system/menu','0:0:0:0:0:0:0:1','内网IP','{\"createDept\":null,\"createBy\":null,\"createTime\":null,\"updateBy\":null,\"updateTime\":null,\"menuId\":11616,\"parentId\":0,\"menuName\":\"工作流\",\"orderNum\":11,\"path\":\"workflow\",\"component\":\"\",\"queryParam\":\"\",\"isFrame\":\"1\",\"isCache\":\"0\",\"menuType\":\"M\",\"visible\":\"1\",\"status\":\"1\",\"perms\":\"\",\"icon\":\"workflow\",\"remark\":\"\"}','{\"code\":200,\"msg\":\"操作成功\",\"data\":null}',0,'','2026-01-04 15:41:35',33),(2007719066136535042,'000000','菜单管理',2,'org.dromara.system.controller.system.SysMenuController.edit()','PUT',1,'admin','研发部门','/system/menu','0:0:0:0:0:0:0:1','内网IP','{\"createDept\":null,\"createBy\":null,\"createTime\":null,\"updateBy\":null,\"updateTime\":null,\"menuId\":5,\"parentId\":0,\"menuName\":\"测试菜单\",\"orderNum\":10,\"path\":\"demo\",\"component\":\"\",\"queryParam\":\"\",\"isFrame\":\"1\",\"isCache\":\"0\",\"menuType\":\"M\",\"visible\":\"1\",\"status\":\"1\",\"perms\":\"\",\"icon\":\"star\",\"remark\":\"测试菜单\"}','{\"code\":200,\"msg\":\"操作成功\",\"data\":null}',0,'','2026-01-04 15:41:41',42),(2007719107819528194,'000000','菜单管理',2,'org.dromara.system.controller.system.SysMenuController.edit()','PUT',1,'admin','研发部门','/system/menu','0:0:0:0:0:0:0:1','内网IP','{\"createDept\":null,\"createBy\":null,\"createTime\":null,\"updateBy\":null,\"updateTime\":null,\"menuId\":4,\"parentId\":0,\"menuName\":\"PLUS官网\",\"orderNum\":9,\"path\":\"https://gitee.com/dromara/RuoYi-Vue-Plus\",\"component\":\"\",\"queryParam\":\"\",\"isFrame\":\"0\",\"isCache\":\"0\",\"menuType\":\"M\",\"visible\":\"1\",\"status\":\"1\",\"perms\":\"\",\"icon\":\"guide\",\"remark\":\"RuoYi-Vue-Plus官网地址\"}','{\"code\":200,\"msg\":\"操作成功\",\"data\":null}',0,'','2026-01-04 15:41:51',32),(2007719136865083394,'000000','菜单管理',2,'org.dromara.system.controller.system.SysMenuController.edit()','PUT',1,'admin','研发部门','/system/menu','0:0:0:0:0:0:0:1','内网IP','{\"createDept\":null,\"createBy\":null,\"createTime\":null,\"updateBy\":null,\"updateTime\":null,\"menuId\":3,\"parentId\":0,\"menuName\":\"系统工具\",\"orderNum\":8,\"path\":\"tool\",\"component\":\"\",\"queryParam\":\"\",\"isFrame\":\"1\",\"isCache\":\"0\",\"menuType\":\"M\",\"visible\":\"1\",\"status\":\"1\",\"perms\":\"\",\"icon\":\"tool\",\"remark\":\"系统工具目录\"}','{\"code\":200,\"msg\":\"操作成功\",\"data\":null}',0,'','2026-01-04 15:41:58',30),(2007719165721894914,'000000','菜单管理',2,'org.dromara.system.controller.system.SysMenuController.edit()','PUT',1,'admin','研发部门','/system/menu','0:0:0:0:0:0:0:1','内网IP','{\"createDept\":null,\"createBy\":null,\"createTime\":null,\"updateBy\":null,\"updateTime\":null,\"menuId\":2,\"parentId\":0,\"menuName\":\"系统监控\",\"orderNum\":7,\"path\":\"monitor\",\"component\":\"\",\"queryParam\":\"\",\"isFrame\":\"1\",\"isCache\":\"0\",\"menuType\":\"M\",\"visible\":\"0\",\"status\":\"0\",\"perms\":\"\",\"icon\":\"monitor\",\"remark\":\"系统监控目录\"}','{\"code\":200,\"msg\":\"操作成功\",\"data\":null}',0,'','2026-01-04 15:42:05',31),(2007719214438735873,'000000','菜单管理',2,'org.dromara.system.controller.system.SysMenuController.edit()','PUT',1,'admin','研发部门','/system/menu','0:0:0:0:0:0:0:1','内网IP','{\"createDept\":null,\"createBy\":null,\"createTime\":null,\"updateBy\":null,\"updateTime\":null,\"menuId\":6,\"parentId\":0,\"menuName\":\"租户管理\",\"orderNum\":6,\"path\":\"tenant\",\"component\":\"\",\"queryParam\":\"\",\"isFrame\":\"1\",\"isCache\":\"0\",\"menuType\":\"M\",\"visible\":\"1\",\"status\":\"1\",\"perms\":\"\",\"icon\":\"chart\",\"remark\":\"租户管理目录\"}','{\"code\":200,\"msg\":\"操作成功\",\"data\":null}',0,'','2026-01-04 15:42:17',33),(2007719244709027841,'000000','菜单管理',2,'org.dromara.system.controller.system.SysMenuController.edit()','PUT',1,'admin','研发部门','/system/menu','0:0:0:0:0:0:0:1','内网IP','{\"createDept\":null,\"createBy\":null,\"createTime\":null,\"updateBy\":null,\"updateTime\":null,\"menuId\":1,\"parentId\":0,\"menuName\":\"系统管理\",\"orderNum\":5,\"path\":\"system\",\"component\":\"\",\"queryParam\":\"\",\"isFrame\":\"1\",\"isCache\":\"0\",\"menuType\":\"M\",\"visible\":\"0\",\"status\":\"0\",\"perms\":\"\",\"icon\":\"system\",\"remark\":\"系统管理目录\"}','{\"code\":200,\"msg\":\"操作成功\",\"data\":null}',0,'','2026-01-04 15:42:24',26),(2007719445888819201,'000000','菜单管理',1,'org.dromara.system.controller.system.SysMenuController.add()','POST',1,'admin','研发部门','/system/menu','0:0:0:0:0:0:0:1','内网IP','{\"createDept\":null,\"createBy\":null,\"createTime\":null,\"updateBy\":null,\"updateTime\":null,\"menuId\":null,\"parentId\":0,\"menuName\":\"内容管理\",\"orderNum\":1,\"path\":\"content\",\"component\":\"\",\"queryParam\":null,\"isFrame\":\"1\",\"isCache\":\"0\",\"menuType\":\"M\",\"visible\":\"0\",\"status\":\"0\",\"icon\":null,\"remark\":null}','{\"code\":200,\"msg\":\"操作成功\",\"data\":null}',0,'','2026-01-04 15:43:12',35),(2007720274511323138,'000000','菜单管理',1,'org.dromara.system.controller.system.SysMenuController.add()','POST',1,'admin','研发部门','/system/menu','0:0:0:0:0:0:0:1','内网IP','{\"createDept\":null,\"createBy\":null,\"createTime\":null,\"updateBy\":null,\"updateTime\":null,\"menuId\":null,\"parentId\":0,\"menuName\":\"广告管理\",\"orderNum\":1,\"path\":\"content/banner\",\"component\":\"content/banner\",\"queryParam\":null,\"isFrame\":\"1\",\"isCache\":\"0\",\"menuType\":\"C\",\"visible\":\"0\",\"status\":\"0\",\"icon\":null,\"remark\":null}','{\"code\":200,\"msg\":\"操作成功\",\"data\":null}',0,'','2026-01-04 15:46:29',35),(2007720329788055554,'000000','菜单管理',2,'org.dromara.system.controller.system.SysMenuController.edit()','PUT',1,'admin','研发部门','/system/menu','0:0:0:0:0:0:0:1','内网IP','{\"createDept\":null,\"createBy\":null,\"createTime\":null,\"updateBy\":null,\"updateTime\":null,\"menuId\":\"2007720274385494018\",\"parentId\":\"2007719445767184385\",\"menuName\":\"广告管理\",\"orderNum\":1,\"path\":\"content/banner\",\"component\":\"content/banner\",\"queryParam\":null,\"isFrame\":\"1\",\"isCache\":\"0\",\"menuType\":\"C\",\"visible\":\"0\",\"status\":\"0\",\"icon\":\"#\",\"remark\":\"\"}','{\"code\":200,\"msg\":\"操作成功\",\"data\":null}',0,'','2026-01-04 15:46:42',32),(2007720415343468546,'000000','菜单管理',2,'org.dromara.system.controller.system.SysMenuController.edit()','PUT',1,'admin','研发部门','/system/menu','0:0:0:0:0:0:0:1','内网IP','{\"createDept\":null,\"createBy\":null,\"createTime\":null,\"updateBy\":null,\"updateTime\":null,\"menuId\":\"2007720274385494018\",\"parentId\":\"2007719445767184385\",\"menuName\":\"广告管理\",\"orderNum\":1,\"path\":\"content/banner/index\",\"component\":\"content/banner\",\"queryParam\":null,\"isFrame\":\"1\",\"isCache\":\"0\",\"menuType\":\"C\",\"visible\":\"0\",\"status\":\"0\",\"icon\":\"#\",\"remark\":\"\"}','{\"code\":200,\"msg\":\"操作成功\",\"data\":null}',0,'','2026-01-04 15:47:03',37),(2007720705044045825,'000000','菜单管理',1,'org.dromara.system.controller.system.SysMenuController.add()','POST',1,'admin','研发部门','/system/menu','0:0:0:0:0:0:0:1','内网IP','{\"createDept\":null,\"createBy\":null,\"createTime\":null,\"updateBy\":null,\"updateTime\":null,\"menuId\":null,\"parentId\":\"2007719445767184385\",\"menuName\":\"培训认证\",\"orderNum\":2,\"path\":\"content/training/index\",\"component\":\"\",\"queryParam\":null,\"isFrame\":\"1\",\"isCache\":\"0\",\"menuType\":\"M\",\"visible\":\"0\",\"status\":\"0\",\"icon\":null,\"remark\":null}','{\"code\":200,\"msg\":\"操作成功\",\"data\":null}',0,'','2026-01-04 15:48:12',46),(2007720820928471041,'000000','菜单管理',2,'org.dromara.system.controller.system.SysMenuController.edit()','PUT',1,'admin','研发部门','/system/menu','0:0:0:0:0:0:0:1','内网IP','{\"createDept\":null,\"createBy\":null,\"createTime\":null,\"updateBy\":null,\"updateTime\":null,\"menuId\":\"2007720704851107841\",\"parentId\":\"2007719445767184385\",\"menuName\":\"培训认证\",\"orderNum\":2,\"path\":\"content/training\",\"component\":\"content/training/index\",\"queryParam\":null,\"isFrame\":\"1\",\"isCache\":\"0\",\"menuType\":\"C\",\"visible\":\"0\",\"status\":\"0\",\"icon\":\"#\",\"remark\":\"\"}','{\"code\":200,\"msg\":\"操作成功\",\"data\":null}',0,'','2026-01-04 15:48:40',42),(2007721931328512002,'000000','菜单管理',1,'org.dromara.system.controller.system.SysMenuController.add()','POST',1,'admin','研发部门','/system/menu','0:0:0:0:0:0:0:1','内网IP','{\"createDept\":null,\"createBy\":null,\"createTime\":null,\"updateBy\":null,\"updateTime\":null,\"menuId\":null,\"parentId\":\"2007719445767184385\",\"menuName\":\"关于我们\",\"orderNum\":0,\"path\":\"content/about\",\"component\":\"content/about/index\",\"queryParam\":null,\"isFrame\":\"1\",\"isCache\":\"0\",\"menuType\":\"C\",\"visible\":\"0\",\"status\":\"0\",\"icon\":null,\"remark\":null}','{\"code\":200,\"msg\":\"操作成功\",\"data\":null}',0,'','2026-01-04 15:53:04',40),(2007722033371734018,'000000','菜单管理',2,'org.dromara.system.controller.system.SysMenuController.edit()','PUT',1,'admin','研发部门','/system/menu','0:0:0:0:0:0:0:1','内网IP','{\"createDept\":null,\"createBy\":null,\"createTime\":null,\"updateBy\":null,\"updateTime\":null,\"menuId\":\"2007721931202682882\",\"parentId\":\"2007719445767184385\",\"menuName\":\"关于我们\",\"orderNum\":4,\"path\":\"content/about\",\"component\":\"content/about/index\",\"queryParam\":null,\"isFrame\":\"1\",\"isCache\":\"0\",\"menuType\":\"C\",\"visible\":\"0\",\"status\":\"0\",\"icon\":\"#\",\"remark\":\"\"}','{\"code\":200,\"msg\":\"操作成功\",\"data\":null}',0,'','2026-01-04 15:53:29',32),(2007722063012880385,'000000','菜单管理',2,'org.dromara.system.controller.system.SysMenuController.edit()','PUT',1,'admin','研发部门','/system/menu','0:0:0:0:0:0:0:1','内网IP','{\"createDept\":null,\"createBy\":null,\"createTime\":null,\"updateBy\":null,\"updateTime\":null,\"menuId\":\"2007720704851107841\",\"parentId\":\"2007719445767184385\",\"menuName\":\"培训认证\",\"orderNum\":3,\"path\":\"content/training\",\"component\":\"content/training/index\",\"queryParam\":null,\"isFrame\":\"1\",\"isCache\":\"0\",\"menuType\":\"C\",\"visible\":\"0\",\"status\":\"0\",\"icon\":\"#\",\"remark\":\"\"}','{\"code\":200,\"msg\":\"操作成功\",\"data\":null}',0,'','2026-01-04 15:53:36',25),(2007722330420731906,'000000','菜单管理',1,'org.dromara.system.controller.system.SysMenuController.add()','POST',1,'admin','研发部门','/system/menu','0:0:0:0:0:0:0:1','内网IP','{\"createDept\":null,\"createBy\":null,\"createTime\":null,\"updateBy\":null,\"updateTime\":null,\"menuId\":null,\"parentId\":\"2007719445767184385\",\"menuName\":\"公告管理\",\"orderNum\":2,\"path\":\"content/news\",\"component\":\"content/news/index\",\"queryParam\":null,\"isFrame\":\"1\",\"isCache\":\"0\",\"menuType\":\"C\",\"visible\":\"0\",\"status\":\"0\",\"icon\":null,\"remark\":null}','{\"code\":200,\"msg\":\"操作成功\",\"data\":null}',0,'','2026-01-04 15:54:39',57),(2007723016650809345,'000000','菜单管理',1,'org.dromara.system.controller.system.SysMenuController.add()','POST',1,'admin','研发部门','/system/menu','0:0:0:0:0:0:0:1','内网IP','{\"createDept\":null,\"createBy\":null,\"createTime\":null,\"updateBy\":null,\"updateTime\":null,\"menuId\":null,\"parentId\":0,\"menuName\":\"赛事管理\",\"orderNum\":2,\"path\":\"competition \",\"component\":\"\",\"queryParam\":null,\"isFrame\":\"1\",\"isCache\":\"0\",\"menuType\":\"M\",\"visible\":\"0\",\"status\":\"0\",\"icon\":null,\"remark\":null}','{\"code\":200,\"msg\":\"操作成功\",\"data\":null}',0,'','2026-01-04 15:57:23',38),(2007723377767800833,'000000','菜单管理',1,'org.dromara.system.controller.system.SysMenuController.add()','POST',1,'admin','研发部门','/system/menu','0:0:0:0:0:0:0:1','内网IP','{\"createDept\":null,\"createBy\":null,\"createTime\":null,\"updateBy\":null,\"updateTime\":null,\"menuId\":null,\"parentId\":\"2007723016520785922\",\"menuName\":\"赛事管理\",\"orderNum\":1,\"path\":\"competition/index\",\"component\":\"competition/index\",\"queryParam\":null,\"isFrame\":\"1\",\"isCache\":\"0\",\"menuType\":\"C\",\"visible\":\"0\",\"status\":\"0\",\"icon\":null,\"remark\":null}','{\"code\":200,\"msg\":\"操作成功\",\"data\":null}',0,'','2026-01-04 15:58:49',37),(2007724365467672577,'000000','菜单管理',1,'org.dromara.system.controller.system.SysMenuController.add()','POST',1,'admin','研发部门','/system/menu','0:0:0:0:0:0:0:1','内网IP','{\"createDept\":null,\"createBy\":null,\"createTime\":null,\"updateBy\":null,\"updateTime\":null,\"menuId\":null,\"parentId\":0,\"menuName\":\"专题管理\",\"orderNum\":0,\"path\":\"competition/topic\",\"component\":\"competition/topic/index\",\"queryParam\":null,\"isFrame\":\"1\",\"isCache\":\"0\",\"menuType\":\"C\",\"visible\":\"0\",\"status\":\"0\",\"icon\":null,\"remark\":null}','{\"code\":200,\"msg\":\"操作成功\",\"data\":null}',0,'','2026-01-04 16:02:45',37),(2007724438003965953,'000000','菜单管理',2,'org.dromara.system.controller.system.SysMenuController.edit()','PUT',1,'admin','研发部门','/system/menu','0:0:0:0:0:0:0:1','内网IP','{\"createDept\":null,\"createBy\":null,\"createTime\":null,\"updateBy\":null,\"updateTime\":null,\"menuId\":\"2007724365341843458\",\"parentId\":\"2007723016520785922\",\"menuName\":\"专题管理\",\"orderNum\":2,\"path\":\"competition/topic\",\"component\":\"competition/topic/index\",\"queryParam\":null,\"isFrame\":\"1\",\"isCache\":\"0\",\"menuType\":\"C\",\"visible\":\"0\",\"status\":\"0\",\"icon\":\"#\",\"remark\":\"\"}','{\"code\":200,\"msg\":\"操作成功\",\"data\":null}',0,'','2026-01-04 16:03:02',40),(2007724845384130562,'000000','菜单管理',1,'org.dromara.system.controller.system.SysMenuController.add()','POST',1,'admin','研发部门','/system/menu','0:0:0:0:0:0:0:1','内网IP','{\"createDept\":null,\"createBy\":null,\"createTime\":null,\"updateBy\":null,\"updateTime\":null,\"menuId\":null,\"parentId\":\"2007723016520785922\",\"menuName\":\"模版管理\",\"orderNum\":3,\"path\":\"competition/template\",\"component\":\"competition/template/index\",\"queryParam\":null,\"isFrame\":\"1\",\"isCache\":\"0\",\"menuType\":\"C\",\"visible\":\"0\",\"status\":\"0\",\"icon\":null,\"remark\":null}','{\"code\":200,\"msg\":\"操作成功\",\"data\":null}',0,'','2026-01-04 16:04:39',68),(2007725345974312961,'000000','菜单管理',1,'org.dromara.system.controller.system.SysMenuController.add()','POST',1,'admin','研发部门','/system/menu','0:0:0:0:0:0:0:1','内网IP','{\"createDept\":null,\"createBy\":null,\"createTime\":null,\"updateBy\":null,\"updateTime\":null,\"menuId\":null,\"parentId\":\"2007723016520785922\",\"menuName\":\"报名统计\",\"orderNum\":4,\"path\":\"competition/registration\",\"component\":\"competition/registration/index\",\"queryParam\":null,\"isFrame\":\"1\",\"isCache\":\"0\",\"menuType\":\"C\",\"visible\":\"0\",\"status\":\"0\",\"icon\":null,\"remark\":null}','{\"code\":200,\"msg\":\"操作成功\",\"data\":null}',0,'','2026-01-04 16:06:38',32),(2007726036054761473,'000000','菜单管理',1,'org.dromara.system.controller.system.SysMenuController.add()','POST',1,'admin','研发部门','/system/menu','0:0:0:0:0:0:0:1','内网IP','{\"createDept\":null,\"createBy\":null,\"createTime\":null,\"updateBy\":null,\"updateTime\":null,\"menuId\":null,\"parentId\":0,\"menuName\":\"航拍作品\",\"orderNum\":3,\"path\":\"photography\",\"component\":\"\",\"queryParam\":null,\"isFrame\":\"1\",\"isCache\":\"0\",\"menuType\":\"M\",\"visible\":\"0\",\"status\":\"0\",\"icon\":null,\"remark\":null}','{\"code\":200,\"msg\":\"操作成功\",\"data\":null}',0,'','2026-01-04 16:09:23',38),(2007726312337760257,'000000','菜单管理',1,'org.dromara.system.controller.system.SysMenuController.add()','POST',1,'admin','研发部门','/system/menu','0:0:0:0:0:0:0:1','内网IP','{\"createDept\":null,\"createBy\":null,\"createTime\":null,\"updateBy\":null,\"updateTime\":null,\"menuId\":null,\"parentId\":\"2007726035928932353\",\"menuName\":\"航拍作品\",\"orderNum\":1,\"path\":\"photography/index\",\"component\":\"photography/index\",\"queryParam\":null,\"isFrame\":\"1\",\"isCache\":\"0\",\"menuType\":\"C\",\"visible\":\"0\",\"status\":\"0\",\"icon\":null,\"remark\":null}','{\"code\":200,\"msg\":\"操作成功\",\"data\":null}',0,'','2026-01-04 16:10:29',32),(2007726514922643458,'000000','菜单管理',1,'org.dromara.system.controller.system.SysMenuController.add()','POST',1,'admin','研发部门','/system/menu','0:0:0:0:0:0:0:1','内网IP','{\"createDept\":null,\"createBy\":null,\"createTime\":null,\"updateBy\":null,\"updateTime\":null,\"menuId\":null,\"parentId\":0,\"menuName\":\"会员管理\",\"orderNum\":4,\"path\":\"user\",\"component\":\"\",\"queryParam\":null,\"isFrame\":\"1\",\"isCache\":\"0\",\"menuType\":\"M\",\"visible\":\"0\",\"status\":\"0\",\"icon\":null,\"remark\":null}','{\"code\":200,\"msg\":\"操作成功\",\"data\":null}',0,'','2026-01-04 16:11:17',52),(2007726747626823681,'000000','菜单管理',1,'org.dromara.system.controller.system.SysMenuController.add()','POST',1,'admin','研发部门','/system/menu','0:0:0:0:0:0:0:1','内网IP','{\"createDept\":null,\"createBy\":null,\"createTime\":null,\"updateBy\":null,\"updateTime\":null,\"menuId\":null,\"parentId\":0,\"menuName\":\"会员管理\",\"orderNum\":1,\"path\":\"user/index\",\"component\":\"user/index\",\"queryParam\":null,\"isFrame\":\"1\",\"isCache\":\"0\",\"menuType\":\"C\",\"visible\":\"0\",\"status\":\"0\",\"icon\":null,\"remark\":null}','{\"code\":500,\"msg\":\"新增菜单\'会员管理\'失败，菜单名称已存在\",\"data\":null}',0,'','2026-01-04 16:12:13',14),(2007726846004224002,'000000','菜单管理',1,'org.dromara.system.controller.system.SysMenuController.add()','POST',1,'admin','研发部门','/system/menu','0:0:0:0:0:0:0:1','内网IP','{\"createDept\":null,\"createBy\":null,\"createTime\":null,\"updateBy\":null,\"updateTime\":null,\"menuId\":null,\"parentId\":0,\"menuName\":\"会员列表\",\"orderNum\":1,\"path\":\"user/index\",\"component\":\"user/index\",\"queryParam\":null,\"isFrame\":\"1\",\"isCache\":\"0\",\"menuType\":\"C\",\"visible\":\"0\",\"status\":\"0\",\"icon\":null,\"remark\":null}','{\"code\":200,\"msg\":\"操作成功\",\"data\":null}',0,'','2026-01-04 16:12:36',43),(2007726891927658497,'000000','菜单管理',2,'org.dromara.system.controller.system.SysMenuController.edit()','PUT',1,'admin','研发部门','/system/menu','0:0:0:0:0:0:0:1','内网IP','{\"createDept\":null,\"createBy\":null,\"createTime\":null,\"updateBy\":null,\"updateTime\":null,\"menuId\":\"2007726845874200577\",\"parentId\":\"2007726845874200577\",\"menuName\":\"会员列表\",\"orderNum\":1,\"path\":\"user/index\",\"component\":\"user/index\",\"queryParam\":null,\"isFrame\":\"1\",\"isCache\":\"0\",\"menuType\":\"C\",\"visible\":\"0\",\"status\":\"0\",\"icon\":\"#\",\"remark\":\"\"}','{\"code\":500,\"msg\":\"修改菜单\'会员列表\'失败，上级菜单不能选择自己\",\"data\":null}',0,'','2026-01-04 16:12:47',4),(2007726981413134338,'000000','菜单管理',2,'org.dromara.system.controller.system.SysMenuController.edit()','PUT',1,'admin','研发部门','/system/menu','0:0:0:0:0:0:0:1','内网IP','{\"createDept\":null,\"createBy\":null,\"createTime\":null,\"updateBy\":null,\"updateTime\":null,\"menuId\":\"2007726845874200577\",\"parentId\":\"2007726514670985217\",\"menuName\":\"会员列表\",\"orderNum\":1,\"path\":\"user/index\",\"component\":\"user/index\",\"queryParam\":null,\"isFrame\":\"1\",\"isCache\":\"0\",\"menuType\":\"C\",\"visible\":\"0\",\"status\":\"0\",\"icon\":\"#\",\"remark\":\"\"}','{\"code\":200,\"msg\":\"操作成功\",\"data\":null}',0,'','2026-01-04 16:13:08',32),(2007765216638066690,'000000','菜单管理',2,'org.dromara.system.controller.system.SysMenuController.edit()','PUT',1,'admin','研发部门','/system/menu','0:0:0:0:0:0:0:1','内网IP','{\"createDept\":null,\"createBy\":null,\"createTime\":null,\"updateBy\":null,\"updateTime\":null,\"menuId\":\"2007720274385494018\",\"parentId\":\"2007719445767184385\",\"menuName\":\"广告管理\",\"orderNum\":1,\"path\":\"banner/index\",\"component\":\"content/banner\",\"queryParam\":null,\"isFrame\":\"1\",\"isCache\":\"0\",\"menuType\":\"C\",\"visible\":\"0\",\"status\":\"0\",\"icon\":\"#\",\"remark\":\"\"}','{\"code\":200,\"msg\":\"操作成功\",\"data\":null}',0,'','2026-01-04 18:45:04',409),(2007765273642852353,'000000','菜单管理',2,'org.dromara.system.controller.system.SysMenuController.edit()','PUT',1,'admin','研发部门','/system/menu','0:0:0:0:0:0:0:1','内网IP','{\"createDept\":null,\"createBy\":null,\"createTime\":null,\"updateBy\":null,\"updateTime\":null,\"menuId\":\"2007722330223599617\",\"parentId\":\"2007719445767184385\",\"menuName\":\"公告管理\",\"orderNum\":2,\"path\":\"news\",\"component\":\"content/news/index\",\"queryParam\":null,\"isFrame\":\"1\",\"isCache\":\"0\",\"menuType\":\"C\",\"visible\":\"0\",\"status\":\"0\",\"icon\":\"#\",\"remark\":\"\"}','{\"code\":200,\"msg\":\"操作成功\",\"data\":null}',0,'','2026-01-04 18:45:18',290),(2007765322091257858,'000000','菜单管理',2,'org.dromara.system.controller.system.SysMenuController.edit()','PUT',1,'admin','研发部门','/system/menu','0:0:0:0:0:0:0:1','内网IP','{\"createDept\":null,\"createBy\":null,\"createTime\":null,\"updateBy\":null,\"updateTime\":null,\"menuId\":\"2007720704851107841\",\"parentId\":\"2007719445767184385\",\"menuName\":\"培训认证\",\"orderNum\":3,\"path\":\"training\",\"component\":\"content/training/index\",\"queryParam\":null,\"isFrame\":\"1\",\"isCache\":\"0\",\"menuType\":\"C\",\"visible\":\"0\",\"status\":\"0\",\"icon\":\"#\",\"remark\":\"\"}','{\"code\":200,\"msg\":\"操作成功\",\"data\":null}',0,'','2026-01-04 18:45:29',285),(2007765386314440706,'000000','菜单管理',2,'org.dromara.system.controller.system.SysMenuController.edit()','PUT',1,'admin','研发部门','/system/menu','0:0:0:0:0:0:0:1','内网IP','{\"createDept\":null,\"createBy\":null,\"createTime\":null,\"updateBy\":null,\"updateTime\":null,\"menuId\":\"2007721931202682882\",\"parentId\":\"2007719445767184385\",\"menuName\":\"关于我们\",\"orderNum\":4,\"path\":\"about\",\"component\":\"content/about/index\",\"queryParam\":null,\"isFrame\":\"1\",\"isCache\":\"0\",\"menuType\":\"C\",\"visible\":\"0\",\"status\":\"0\",\"icon\":\"#\",\"remark\":\"\"}','{\"code\":200,\"msg\":\"操作成功\",\"data\":null}',0,'','2026-01-04 18:45:45',296),(2007765492040261634,'000000','菜单管理',2,'org.dromara.system.controller.system.SysMenuController.edit()','PUT',1,'admin','研发部门','/system/menu','0:0:0:0:0:0:0:1','内网IP','{\"createDept\":null,\"createBy\":null,\"createTime\":null,\"updateBy\":null,\"updateTime\":null,\"menuId\":\"2007723377579057154\",\"parentId\":\"2007723016520785922\",\"menuName\":\"赛事管理\",\"orderNum\":1,\"path\":\"competition\",\"component\":\"competition/index\",\"queryParam\":null,\"isFrame\":\"1\",\"isCache\":\"0\",\"menuType\":\"C\",\"visible\":\"0\",\"status\":\"0\",\"icon\":\"#\",\"remark\":\"\"}','{\"code\":200,\"msg\":\"操作成功\",\"data\":null}',0,'','2026-01-04 18:46:10',313),(2007772070067888130,'000000','菜单管理',2,'org.dromara.system.controller.system.SysMenuController.edit()','PUT',1,'admin','研发部门','/system/menu','0:0:0:0:0:0:0:1','内网IP','{\"createDept\":null,\"createBy\":null,\"createTime\":null,\"updateBy\":null,\"updateTime\":null,\"menuId\":\"2007720274385494018\",\"parentId\":\"2007719445767184385\",\"menuName\":\"广告管理\",\"orderNum\":1,\"path\":\"banner\",\"component\":\"content/banner\",\"queryParam\":null,\"isFrame\":\"1\",\"isCache\":\"0\",\"menuType\":\"C\",\"visible\":\"0\",\"status\":\"0\",\"icon\":\"#\",\"remark\":\"\"}','{\"code\":200,\"msg\":\"操作成功\",\"data\":null}',0,'','2026-01-04 19:12:18',283),(2007773709822648322,'000000','菜单管理',2,'org.dromara.system.controller.system.SysMenuController.edit()','PUT',1,'admin','研发部门','/system/menu','0:0:0:0:0:0:0:1','内网IP','{\"createDept\":null,\"createBy\":null,\"createTime\":null,\"updateBy\":null,\"updateTime\":null,\"menuId\":\"2007720274385494018\",\"parentId\":\"2007719445767184385\",\"menuName\":\"广告管理\",\"orderNum\":1,\"path\":\"banner\",\"component\":\"content/banner/index\",\"queryParam\":null,\"isFrame\":\"1\",\"isCache\":\"0\",\"menuType\":\"C\",\"visible\":\"0\",\"status\":\"0\",\"icon\":\"#\",\"remark\":\"\"}','{\"code\":200,\"msg\":\"操作成功\",\"data\":null}',0,'','2026-01-04 19:18:49',286);

DROP TABLE IF EXISTS `sys_oss`;

CREATE TABLE `sys_oss` (
  `oss_id` bigint(20) NOT NULL COMMENT '对象存储主键',
  `tenant_id` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT '000000' COMMENT '租户编号',
  `file_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '文件名',
  `original_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '原名',
  `file_suffix` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '文件后缀名',
  `url` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'URL地址',
  `ext1` text COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '扩展字段',
  `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` bigint(20) DEFAULT NULL COMMENT '上传人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新人',
  `service` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'minio' COMMENT '服务商',
  PRIMARY KEY (`oss_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OSS对象存储表';


DROP TABLE IF EXISTS `sys_oss_config`;

CREATE TABLE `sys_oss_config` (
  `oss_config_id` bigint(20) NOT NULL COMMENT '主键',
  `tenant_id` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT '000000' COMMENT '租户编号',
  `config_key` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '配置key',
  `access_key` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'accessKey',
  `secret_key` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '秘钥',
  `bucket_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '桶名称',
  `prefix` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '前缀',
  `endpoint` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '访问站点',
  `domain` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '自定义域名',
  `is_https` char(1) COLLATE utf8mb4_unicode_ci DEFAULT 'N' COMMENT '是否https（Y=是,N=否）',
  `region` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '域',
  `access_policy` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '1' COMMENT '桶权限类型(0=private 1=public 2=custom)',
  `status` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '1' COMMENT '是否默认（0=是,1=否）',
  `ext1` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '扩展字段',
  `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`oss_config_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对象存储配置表';

INSERT INTO `sys_oss_config` VALUES (1,'000000','minio','ruoyi','ruoyi123','ruoyi','','127.0.0.1:9000','','N','','1','0','',103,1,'2026-01-04 13:37:52',1,'2026-01-04 13:37:52',NULL),(2,'000000','qiniu','XXXXXXXXXXXXXXX','XXXXXXXXXXXXXXX','ruoyi','','s3-cn-north-1.qiniucs.com','','N','','1','1','',103,1,'2026-01-04 13:37:52',1,'2026-01-04 13:37:52',NULL),(3,'000000','aliyun','XXXXXXXXXXXXXXX','XXXXXXXXXXXXXXX','ruoyi','','oss-cn-beijing.aliyuncs.com','','N','','1','1','',103,1,'2026-01-04 13:37:52',1,'2026-01-04 13:37:52',NULL),(4,'000000','qcloud','XXXXXXXXXXXXXXX','XXXXXXXXXXXXXXX','ruoyi-1240000000','','cos.ap-beijing.myqcloud.com','','N','ap-beijing','1','1','',103,1,'2026-01-04 13:37:52',1,'2026-01-04 13:37:52',NULL),(5,'000000','image','ruoyi','ruoyi123','ruoyi','image','127.0.0.1:9000','','N','','1','1','',103,1,'2026-01-04 13:37:52',1,'2026-01-04 13:37:52',NULL);

DROP TABLE IF EXISTS `sys_post`;

CREATE TABLE `sys_post` (
  `post_id` bigint(20) NOT NULL COMMENT '岗位ID',
  `tenant_id` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT '000000' COMMENT '租户编号',
  `dept_id` bigint(20) NOT NULL COMMENT '部门id',
  `post_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '岗位编码',
  `post_category` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '岗位类别编码',
  `post_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '岗位名称',
  `post_sort` int(4) NOT NULL COMMENT '显示顺序',
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '状态（0正常 1停用）',
  `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='岗位信息表';

INSERT INTO `sys_post` VALUES (1,'000000',103,'ceo',NULL,'董事长',1,'0',103,1,'2026-01-04 13:37:39',NULL,NULL,''),(2,'000000',100,'se',NULL,'项目经理',2,'0',103,1,'2026-01-04 13:37:39',NULL,NULL,''),(3,'000000',100,'hr',NULL,'人力资源',3,'0',103,1,'2026-01-04 13:37:39',NULL,NULL,''),(4,'000000',100,'user',NULL,'普通员工',4,'0',103,1,'2026-01-04 13:37:39',NULL,NULL,'');

DROP TABLE IF EXISTS `sys_role`;

CREATE TABLE `sys_role` (
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  `tenant_id` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT '000000' COMMENT '租户编号',
  `role_name` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色名称',
  `role_key` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色权限字符串',
  `role_sort` int(4) NOT NULL COMMENT '显示顺序',
  `data_scope` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '1' COMMENT '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限 5：仅本人数据权限 6：部门及以下或本人数据权限）',
  `menu_check_strictly` tinyint(1) DEFAULT 1 COMMENT '菜单树选择项是否关联显示',
  `dept_check_strictly` tinyint(1) DEFAULT 1 COMMENT '部门树选择项是否关联显示',
  `status` char(1) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色状态（0正常 1停用）',
  `del_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色信息表';

INSERT INTO `sys_role` VALUES (1,'000000','超级管理员','superadmin',1,'1',1,1,'0','0',103,1,'2026-01-04 13:37:39',NULL,NULL,'超级管理员'),(3,'000000','本部门及以下','test1',3,'4',1,1,'0','0',103,1,'2026-01-04 13:37:39',NULL,NULL,''),(4,'000000','仅本人','test2',4,'5',1,1,'0','0',103,1,'2026-01-04 13:37:39',NULL,NULL,'');

DROP TABLE IF EXISTS `sys_role_dept`;

CREATE TABLE `sys_role_dept` (
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  `dept_id` bigint(20) NOT NULL COMMENT '部门ID',
  PRIMARY KEY (`role_id`,`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色和部门关联表';

DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  `menu_id` bigint(20) NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`role_id`,`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色和菜单关联表';


INSERT INTO `sys_role_menu` VALUES (3,1),(3,5),(3,100),(3,101),(3,102),(3,103),(3,104),(3,105),(3,106),(3,107),(3,108),(3,118),(3,123),(3,130),(3,131),(3,132),(3,133),(3,500),(3,501),(3,1001),(3,1002),(3,1003),(3,1004),(3,1005),(3,1006),(3,1007),(3,1008),(3,1009),(3,1010),(3,1011),(3,1012),(3,1013),(3,1014),(3,1015),(3,1016),(3,1017),(3,1018),(3,1019),(3,1020),(3,1021),(3,1022),(3,1023),(3,1024),(3,1025),(3,1026),(3,1027),(3,1028),(3,1029),(3,1030),(3,1031),(3,1032),(3,1033),(3,1034),(3,1035),(3,1036),(3,1037),(3,1038),(3,1039),(3,1040),(3,1041),(3,1042),(3,1043),(3,1044),(3,1045),(3,1050),(3,1061),(3,1062),(3,1063),(3,1064),(3,1065),(3,1500),(3,1501),(3,1502),(3,1503),(3,1504),(3,1505),(3,1506),(3,1507),(3,1508),(3,1509),(3,1510),(3,1511),(3,1600),(3,1601),(3,1602),(3,1603),(3,1620),(3,1621),(3,1622),(3,1623),(3,11616),(3,11618),(3,11619),(3,11622),(3,11623),(3,11629),(3,11632),(3,11633),(3,11638),(3,11639),(3,11640),(3,11641),(3,11642),(3,11643),(3,11701),(4,5),(4,1500),(4,1501),(4,1502),(4,1503),(4,1504),(4,1505),(4,1506),(4,1507),(4,1508),(4,1509),(4,1510),(4,1511);

DROP TABLE IF EXISTS `sys_social`;

CREATE TABLE `sys_social` (
  `id` bigint(20) NOT NULL COMMENT '主键',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `tenant_id` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT '000000' COMMENT '租户id',
  `auth_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '平台+平台唯一id',
  `source` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户来源',
  `open_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '平台编号唯一id',
  `user_name` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '登录账号',
  `nick_name` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '用户昵称',
  `email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '用户邮箱',
  `avatar` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '头像地址',
  `access_token` varchar(2000) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户的授权令牌',
  `expire_in` int(11) DEFAULT NULL COMMENT '用户的授权令牌的有效期，部分平台可能没有',
  `refresh_token` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '刷新令牌，部分平台可能没有',
  `access_code` varchar(2000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '平台的授权信息，部分平台可能没有',
  `union_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户的 unionid',
  `scope` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '授予的权限，部分平台可能没有',
  `token_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '个别平台的授权信息，部分平台可能没有',
  `id_token` varchar(2000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'id token，部分平台可能没有',
  `mac_algorithm` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '小米平台用户的附带属性，部分平台可能没有',
  `mac_key` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '小米平台用户的附带属性，部分平台可能没有',
  `code` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户的授权code，部分平台可能没有',
  `oauth_token` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Twitter平台用户的附带属性，部分平台可能没有',
  `oauth_token_secret` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Twitter平台用户的附带属性，部分平台可能没有',
  `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社会化关系表';

DROP TABLE IF EXISTS `sys_tenant`;

CREATE TABLE `sys_tenant` (
  `id` bigint(20) NOT NULL COMMENT 'id',
  `tenant_id` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户编号',
  `contact_user_name` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '联系人',
  `contact_phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '联系电话',
  `company_name` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '企业名称',
  `license_number` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '统一社会信用代码',
  `address` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '地址',
  `intro` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '企业简介',
  `domain` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '域名',
  `remark` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `package_id` bigint(20) DEFAULT NULL COMMENT '租户套餐编号',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间',
  `account_count` int(11) DEFAULT -1 COMMENT '用户数量（-1不限制）',
  `status` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '租户状态（0正常 1停用）',
  `del_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户表';

INSERT INTO `sys_tenant` VALUES (1,'000000','管理组','15917487355','中国航空器拥有者及驾驶员协会',NULL,NULL,'多租户通用后台管理管理系统',NULL,NULL,NULL,NULL,-1,'0','0',103,1,'2026-01-04 13:37:38',NULL,NULL);

DROP TABLE IF EXISTS `sys_tenant_package`;

CREATE TABLE `sys_tenant_package` (
  `package_id` bigint(20) NOT NULL COMMENT '租户套餐id',
  `package_name` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '套餐名称',
  `menu_ids` varchar(3000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关联菜单id',
  `remark` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `menu_check_strictly` tinyint(1) DEFAULT 1 COMMENT '菜单树选择项是否关联显示',
  `status` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `del_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`package_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户套餐表';

DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `tenant_id` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT '000000' COMMENT '租户编号',
  `dept_id` bigint(20) DEFAULT NULL COMMENT '部门ID',
  `user_name` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户账号',
  `nick_name` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户昵称',
  `user_type` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT 'sys_user' COMMENT '用户类型（sys_user系统用户）',
  `email` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '用户邮箱',
  `phonenumber` varchar(11) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '手机号码',
  `sex` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '用户性别（0男 1女 2未知）',
  `avatar` bigint(20) DEFAULT NULL COMMENT '头像地址',
  `password` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '密码',
  `status` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '帐号状态（0正常 1停用）',
  `del_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `login_ip` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '最后登录IP',
  `login_date` datetime DEFAULT NULL COMMENT '最后登录时间',
  `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户信息表';

INSERT INTO `sys_user` VALUES (1,'000000',103,'admin','管理员','sys_user','tim.lai@dino-info.com','15888888888','1',NULL,'$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','0','0','183.14.132.233','2026-01-06 12:44:55',103,1,'2026-01-04 13:37:39',-1,'2026-01-06 12:44:55','管理员'),(3,'000000',108,'test','本部门及以下 密码666666','sys_user','','','0',NULL,'$2a$10$b8yUzN0C71sbz.PhNOCgJe.Tu1yWC3RNrTyjSQ8p1W0.aaUXUJ.Ne','0','0','127.0.0.1','2026-01-04 13:37:39',103,1,'2026-01-04 13:37:39',3,'2026-01-04 13:37:39',NULL),(4,'000000',102,'test1','仅本人 密码666666','sys_user','','','0',NULL,'$2a$10$b8yUzN0C71sbz.PhNOCgJe.Tu1yWC3RNrTyjSQ8p1W0.aaUXUJ.Ne','0','0','127.0.0.1','2026-01-04 13:37:39',103,1,'2026-01-04 13:37:39',4,'2026-01-04 13:37:39',NULL);

DROP TABLE IF EXISTS `sys_user_post`;
CREATE TABLE `sys_user_post` (
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `post_id` bigint(20) NOT NULL COMMENT '岗位ID',
  PRIMARY KEY (`user_id`,`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户与岗位关联表';

INSERT INTO `sys_user_post` VALUES (1,1);

DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户和角色关联表';

INSERT INTO `sys_user_role` VALUES (1,1),(3,3),(4,4);

DROP TABLE IF EXISTS `test_demo`;

CREATE TABLE `test_demo` (
  `id` bigint(20) NOT NULL COMMENT '主键',
  `tenant_id` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT '000000' COMMENT '租户编号',
  `dept_id` bigint(20) DEFAULT NULL COMMENT '部门id',
  `user_id` bigint(20) DEFAULT NULL COMMENT '用户id',
  `order_num` int(11) DEFAULT 0 COMMENT '排序号',
  `test_key` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'key键',
  `value` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '值',
  `version` int(11) DEFAULT 0 COMMENT '版本',
  `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新人',
  `del_flag` int(11) DEFAULT 0 COMMENT '删除标志',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='测试单表';

INSERT INTO `test_demo` VALUES (1,'000000',102,4,1,'测试数据权限','测试',0,103,'2026-01-04 13:37:53',1,NULL,NULL,0),(2,'000000',102,3,2,'子节点1','111',0,103,'2026-01-04 13:37:53',1,NULL,NULL,0),(3,'000000',102,3,3,'子节点2','222',0,103,'2026-01-04 13:37:53',1,NULL,NULL,0),(4,'000000',108,4,4,'测试数据','demo',0,103,'2026-01-04 13:37:53',1,NULL,NULL,0),(5,'000000',108,3,13,'子节点11','1111',0,103,'2026-01-04 13:37:53',1,NULL,NULL,0),(6,'000000',108,3,12,'子节点22','2222',0,103,'2026-01-04 13:37:53',1,NULL,NULL,0),(7,'000000',108,3,11,'子节点33','3333',0,103,'2026-01-04 13:37:53',1,NULL,NULL,0),(8,'000000',108,3,10,'子节点44','4444',0,103,'2026-01-04 13:37:53',1,NULL,NULL,0),(9,'000000',108,3,9,'子节点55','5555',0,103,'2026-01-04 13:37:53',1,NULL,NULL,0),(10,'000000',108,3,8,'子节点66','6666',0,103,'2026-01-04 13:37:53',1,NULL,NULL,0),(11,'000000',108,3,7,'子节点77','7777',0,103,'2026-01-04 13:37:53',1,NULL,NULL,0),(12,'000000',108,3,6,'子节点88','8888',0,103,'2026-01-04 13:37:53',1,NULL,NULL,0),(13,'000000',108,3,5,'子节点99','9999',0,103,'2026-01-04 13:37:53',1,NULL,NULL,0);

DROP TABLE IF EXISTS `test_leave`;

CREATE TABLE `test_leave` (
  `id` bigint(20) NOT NULL COMMENT 'id',
  `tenant_id` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT '000000' COMMENT '租户编号',
  `apply_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '申请编号',
  `leave_type` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '请假类型',
  `start_date` datetime NOT NULL COMMENT '开始时间',
  `end_date` datetime NOT NULL COMMENT '结束时间',
  `leave_days` int(10) NOT NULL COMMENT '请假天数',
  `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '请假原因',
  `status` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '状态',
  `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='请假申请表';

DROP TABLE IF EXISTS `test_tree`;

CREATE TABLE `test_tree` (
  `id` bigint(20) NOT NULL COMMENT '主键',
  `tenant_id` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT '000000' COMMENT '租户编号',
  `parent_id` bigint(20) DEFAULT 0 COMMENT '父id',
  `dept_id` bigint(20) DEFAULT NULL COMMENT '部门id',
  `user_id` bigint(20) DEFAULT NULL COMMENT '用户id',
  `tree_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '值',
  `version` int(11) DEFAULT 0 COMMENT '版本',
  `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新人',
  `del_flag` int(11) DEFAULT 0 COMMENT '删除标志',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='测试树表';

INSERT INTO `test_tree` VALUES (1,'000000',0,102,4,'测试数据权限',0,103,'2026-01-04 13:37:53',1,NULL,NULL,0),(2,'000000',1,102,3,'子节点1',0,103,'2026-01-04 13:37:53',1,NULL,NULL,0),(3,'000000',2,102,3,'子节点2',0,103,'2026-01-04 13:37:53',1,NULL,NULL,0),(4,'000000',0,108,4,'测试树1',0,103,'2026-01-04 13:37:53',1,NULL,NULL,0),(5,'000000',4,108,3,'子节点11',0,103,'2026-01-04 13:37:53',1,NULL,NULL,0),(6,'000000',4,108,3,'子节点22',0,103,'2026-01-04 13:37:53',1,NULL,NULL,0),(7,'000000',4,108,3,'子节点33',0,103,'2026-01-04 13:37:53',1,NULL,NULL,0),(8,'000000',5,108,3,'子节点44',0,103,'2026-01-04 13:37:53',1,NULL,NULL,0),(9,'000000',6,108,3,'子节点55',0,103,'2026-01-04 13:37:53',1,NULL,NULL,0),(10,'000000',7,108,3,'子节点66',0,103,'2026-01-04 13:37:53',1,NULL,NULL,0),(11,'000000',7,108,3,'子节点77',0,103,'2026-01-04 13:37:53',1,NULL,NULL,0),(12,'000000',10,108,3,'子节点88',0,103,'2026-01-04 13:37:53',1,NULL,NULL,0),(13,'000000',10,108,3,'子节点99',0,103,'2026-01-04 13:37:53',1,NULL,NULL,0);
