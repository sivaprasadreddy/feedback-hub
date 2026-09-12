create sequence user_id_seq start with 100 increment by 50;

create table users
(
    id         bigint    not null default nextval('user_id_seq'),
    email      text      not null,
    password   text      not null,
    name       text      not null,
    role       text      not null default 'ROLE_USER',
    active     boolean   not null default true,
    created_at timestamp not null default CURRENT_TIMESTAMP,
    updated_at timestamp,
    version    bigint    not null default 0,
    primary key (id),
    constraint user_email_unique unique (email)
);

-- pwd is 'secret'
insert into users(id, email, password, name, role, active, created_at) values
(1, 'admin@gmail.com','$2a$10$2bF0hrLWv/bH9kJPzOq4qe3.ky6cMSMl9MbNkAGUG8E2nxjibFtxi','Admin', 'ROLE_ADMIN', true, CURRENT_TIMESTAMP),
(2, 'siva@gmail.com','$2a$10$2bF0hrLWv/bH9kJPzOq4qe3.ky6cMSMl9MbNkAGUG8E2nxjibFtxi','Siva', 'ROLE_USER', true, CURRENT_TIMESTAMP),
(3, 'prasad@gmail.com','$2a$10$2bF0hrLWv/bH9kJPzOq4qe3.ky6cMSMl9MbNkAGUG8E2nxjibFtxi','Prasad', 'ROLE_USER', false, CURRENT_TIMESTAMP);
