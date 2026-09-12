create sequence reply_vote_id_seq start with 100 increment by 50;

create table reply_votes
(
    id         bigint    not null default nextval('reply_vote_id_seq'),
    user_id    bigint    not null,
    reply_id   bigint    not null,
    vote_type  text      not null,
    created_at timestamp not null default CURRENT_TIMESTAMP,
    updated_at timestamp,
    version    bigint    not null default 0,
    primary key (id),
    constraint reply_vote_user_fk foreign key (user_id) references users (id),
    constraint reply_vote_reply_fk foreign key (reply_id) references replies (id),
    constraint reply_vote_type_check check (vote_type in ('UPVOTE', 'DOWNVOTE')),
    constraint reply_vote_user_reply_unique unique (user_id, reply_id)
);

create index reply_vote_reply_type_idx on reply_votes (reply_id, vote_type);
