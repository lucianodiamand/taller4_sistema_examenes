insert into permissions (code, description)
select 'users.create.any', 'Create users as admin'
where not exists (select 1 from permissions where code = 'users.create.any');

insert into permissions (code, description)
select 'users.update.any', 'Update users as admin'
where not exists (select 1 from permissions where code = 'users.update.any');

insert into permissions (code, description)
select 'users.delete.any', 'Delete users as admin'
where not exists (select 1 from permissions where code = 'users.delete.any');

insert into role_permissions (role_id, permission_id)
select r.id, p.id from roles r join permissions p on p.code = 'users.create.any'
where r.name = 'ADMIN'
and not exists (select 1 from role_permissions rp where rp.role_id = r.id and rp.permission_id = p.id);

insert into role_permissions (role_id, permission_id)
select r.id, p.id from roles r join permissions p on p.code = 'users.update.any'
where r.name = 'ADMIN'
and not exists (select 1 from role_permissions rp where rp.role_id = r.id and rp.permission_id = p.id);

insert into role_permissions (role_id, permission_id)
select r.id, p.id from roles r join permissions p on p.code = 'users.delete.any'
where r.name = 'ADMIN'
and not exists (select 1 from role_permissions rp where rp.role_id = r.id and rp.permission_id = p.id);
