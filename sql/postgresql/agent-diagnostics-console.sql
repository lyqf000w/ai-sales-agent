-- AI sales champion diagnostics console menu.

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component,
                         component_name, status, visible, keep_alive, always_show, creator,
                         create_time, updater, update_time, deleted)
VALUES (6066, '链路诊断', 'agent:diagnostics:query', 2, 15, 6000, 'diagnostics',
        'ep:monitor', 'agent/diagnostics/index', 'AgentDiagnostics', 0, true, true, true,
        'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    permission = EXCLUDED.permission,
    sort = EXCLUDED.sort,
    parent_id = EXCLUDED.parent_id,
    path = EXCLUDED.path,
    icon = EXCLUDED.icon,
    component = EXCLUDED.component,
    component_name = EXCLUDED.component_name,
    status = EXCLUDED.status,
    visible = EXCLUDED.visible,
    keep_alive = EXCLUDED.keep_alive,
    always_show = EXCLUDED.always_show,
    updater = EXCLUDED.updater,
    update_time = CURRENT_TIMESTAMP,
    deleted = 0;

SELECT setval('system_menu_seq', GREATEST((SELECT last_value FROM system_menu_seq), 6067), false);
