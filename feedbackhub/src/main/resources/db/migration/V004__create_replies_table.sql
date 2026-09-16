create sequence reply_id_seq start with 100 increment by 50;

create table replies
(
    id                 bigint    not null default nextval('reply_id_seq'),
    message_id         bigint    not null,
    content            text      not null,
    created_by_user_id bigint    not null,
    anonymous          boolean   not null,
    created_at         timestamp not null default CURRENT_TIMESTAMP,
    updated_at         timestamp,
    version            bigint    not null default 0,
    primary key (id),
    constraint reply_message_fk foreign key (message_id) references messages (id),
    constraint reply_created_by_user_fk foreign key (created_by_user_id) references users (id)
);

create index reply_message_created_at_idx on replies (message_id, created_at);
