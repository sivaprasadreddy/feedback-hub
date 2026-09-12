create sequence user_id_seq start with 100 increment by 50;

create table users
(
    id         bigint    not null default nextval('user_id_seq'),
    tenant_id  text,
    email      text      not null,
    password   text      not null,
    name       text      not null,
    role       text      not null,
    created_at timestamp not null default CURRENT_TIMESTAMP,
    updated_at timestamp,
    version    bigint       not null default 0,
    primary key (id),
    constraint user_email_unique unique (email)
);

insert into users(id, tenant_id, email, password, name, role, created_at) values
(1, NULL, 'superadmin@gmail.com', '$2a$10$T4wbudS9gFmfUxbCQFf6Mu3wR991dbf1FQTBrxtM6rACOB/A1Lj6G', 'Super Admin', 'ROLE_SUPER_ADMIN', CURRENT_TIMESTAMP);
