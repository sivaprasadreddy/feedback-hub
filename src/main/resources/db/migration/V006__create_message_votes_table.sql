create sequence message_vote_id_seq start with 100 increment by 50;

create table message_votes
(
    id         bigint    not null default nextval('message_vote_id_seq'),
    user_id    bigint    not null,
    message_id bigint    not null,
    vote_type  text      not null,
    created_at timestamp not null default CURRENT_TIMESTAMP,
    updated_at timestamp,
    version    bigint    not null default 0,
    primary key (id),
    constraint message_vote_user_fk foreign key (user_id) references users (id),
    constraint message_vote_message_fk foreign key (message_id) references messages (id),
    constraint message_vote_type_check check (vote_type in ('UPVOTE', 'DOWNVOTE')),
    constraint message_vote_user_message_unique unique (user_id, message_id)
);

create index message_vote_message_type_idx on message_votes (message_id, vote_type);
