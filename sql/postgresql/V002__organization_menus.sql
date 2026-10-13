-- organization 模块（postgresql）—— 后台菜单与按钮权限种子。
--
-- 父子关系用模块内唯一的菜单名在本语句内连接（与 module-infra / device 基线同一模式），
-- 跨模块不共享任何编号。加菜单：往对应层级的 VALUES 里加一行；不要挑号。
--
-- component 与 neton-application-front-organization 的 manifest page key 一一对应
-- （organization/index、organization/member/index）。
--
-- 授予角色：super_admin 全量；其余角色由后台「角色管理」界面按需分配。

WITH lvl1 AS (
    INSERT INTO system_menus (name, permission, type, parent_id, path, component, icon, sort, status, created_at, updated_at)
    VALUES
        ('组织管理', '', 1, 0, '/organization', NULL, 'ant-design:apartment-outlined', 4, 1, (extract(epoch from now()) * 1000)::bigint, (extract(epoch from now()) * 1000)::bigint)
    RETURNING id, name
),
lvl2 AS (
    INSERT INTO system_menus (name, permission, type, parent_id, path, component, icon, sort, status, created_at, updated_at)
    SELECT v.name, v.permission, v.type,
           (SELECT p.id FROM lvl1 p WHERE p.name = v.parent_name),
           v.path, v.component, v.icon, v.sort, v.status, (extract(epoch from now()) * 1000)::bigint, (extract(epoch from now()) * 1000)::bigint
    FROM (VALUES
        ('组织列表', 'organization:org:query', 2, '组织管理', 'list', 'organization/index', 'ant-design:team-outlined', 1, 1),
        ('组织成员', 'organization:member:query', 2, '组织管理', 'member', 'organization/member/index', 'ant-design:user-outlined', 2, 1)
    ) AS v(name, permission, type, parent_name, path, component, icon, sort, status)
    RETURNING id, name
),
lvl3 AS (
    INSERT INTO system_menus (name, permission, type, parent_id, path, component, icon, sort, status, created_at, updated_at)
    SELECT v.name, v.permission, v.type,
           (SELECT p.id FROM lvl2 p WHERE p.name = v.parent_name),
           v.path, v.component, v.icon, v.sort, v.status, (extract(epoch from now()) * 1000)::bigint, (extract(epoch from now()) * 1000)::bigint
    FROM (VALUES
        ('组织启停', 'organization:org:update', 3, '组织列表', '', NULL, '', 2, 1),
        ('组织删除', 'organization:org:delete', 3, '组织列表', '', NULL, '', 3, 1),
        ('成员批量分配', 'organization:member:assign', 3, '组织成员', '', NULL, '', 2, 1),
        ('成员改角色', 'organization:member:update', 3, '组织成员', '', NULL, '', 3, 1),
        ('成员移除', 'organization:member:delete', 3, '组织成员', '', NULL, '', 4, 1)
    ) AS v(name, permission, type, parent_name, path, component, icon, sort, status)
    RETURNING id, name
),
inserted AS (
        SELECT id, name FROM lvl2
        UNION ALL SELECT id, name FROM lvl3
)
INSERT INTO system_role_menus (role_id, menu_id, created_at)
SELECT r.id, m.id, (extract(epoch from now()) * 1000)::bigint
FROM system_roles r
JOIN inserted m ON TRUE
WHERE r.code IN ('super_admin')
ON CONFLICT DO NOTHING;
