alter table messages
    add column upvote_count bigint not null default 0,
    add column downvote_count bigint not null default 0,
    add column reply_count bigint not null default 0;

alter table replies
    add column upvote_count bigint not null default 0,
    add column downvote_count bigint not null default 0;

update messages m
set upvote_count = (select count(*) from message_votes v where v.message_id = m.id and v.vote_type = 'UPVOTE'),
    downvote_count = (select count(*) from message_votes v where v.message_id = m.id and v.vote_type = 'DOWNVOTE'),
    reply_count = (select count(*) from replies r where r.message_id = m.id and r.status = 'ACTIVE');

update replies r
set upvote_count = (select count(*) from reply_votes v where v.reply_id = r.id and v.vote_type = 'UPVOTE'),
    downvote_count = (select count(*) from reply_votes v where v.reply_id = r.id and v.vote_type = 'DOWNVOTE');

alter table messages
    add constraint message_upvote_count_non_negative check (upvote_count >= 0),
    add constraint message_downvote_count_non_negative check (downvote_count >= 0),
    add constraint message_reply_count_non_negative check (reply_count >= 0);

alter table replies
    add constraint reply_upvote_count_non_negative check (upvote_count >= 0),
    add constraint reply_downvote_count_non_negative check (downvote_count >= 0);

create index message_popularity_idx on messages (upvote_count desc, created_at desc);
