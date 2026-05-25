-- 集成测试清理 — 按依赖顺序删除数据
-- 注意：H2 不支持 TRUNCATE ... CASCADE，需按外键反向顺序 DELETE
DELETE FROM work_like;
DELETE FROM work_tag;
DELETE FROM work_score;
DELETE FROM work_publish;
DELETE FROM work_audit;
DELETE FROM work_attachment;
DELETE FROM work_member;
DELETE FROM work_comment;
DELETE FROM rank_reward;
DELETE FROM reward_issue;
DELETE FROM reward_config;
DELETE FROM work;
DELETE FROM score_batch;
DELETE FROM sensitive_rule;
DELETE FROM port_manage;
DELETE FROM tag;
DELETE FROM sys_notification;
DELETE FROM sys_notice;
DELETE FROM sys_log;
DELETE FROM sys_role_menu;
DELETE FROM sys_menu;
DELETE FROM sys_dict;
DELETE FROM sys_user;
DELETE FROM sys_role;
