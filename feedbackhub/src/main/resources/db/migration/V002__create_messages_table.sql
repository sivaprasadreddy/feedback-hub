create sequence message_id_seq start with 100 increment by 50;

create table messages
(
    id                 bigint    not null default nextval('message_id_seq'),
    content            text      not null,
    created_by_user_id bigint    not null,
    anonymous          boolean   not null,
    created_at         timestamp not null default CURRENT_TIMESTAMP,
    updated_at         timestamp,
    version            bigint    not null default 0,
    primary key (id),
    constraint message_created_by_user_fk foreign key (created_by_user_id) references users (id)
);

create index message_created_at_idx on messages (created_at desc);
