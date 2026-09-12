DELETE FROM users;

insert into users(id, tenant_id, email, password, name, role, created_at) values
(1, NULL, 'superadmin@gmail.com', '$2a$10$T4wbudS9gFmfUxbCQFf6Mu3wR991dbf1FQTBrxtM6rACOB/A1Lj6G', 'Super Admin', 'ROLE_SUPER_ADMIN', CURRENT_TIMESTAMP);

