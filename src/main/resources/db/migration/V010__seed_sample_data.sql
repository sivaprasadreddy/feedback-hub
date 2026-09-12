-- All seeded users have the password 'secret'.
insert into users(email, password, name, role, active, created_at)
select 'user' || lpad(user_no::text, 2, '0') || '@speakup.local',
       '$2a$10$2bF0hrLWv/bH9kJPzOq4qe3.ky6cMSMl9MbNkAGUG8E2nxjibFtxi',
       'Seed User ' || lpad(user_no::text, 2, '0'),
       'ROLE_USER',
       true,
       CURRENT_TIMESTAMP - ((16 - user_no) * interval '1 day')
from generate_series(1, 15) as seed_users(user_no);

insert into messages(content, created_by_user_id, anonymous, created_at)
select 'Seed feedback message ' || lpad(message_no::text, 2, '0')
           || ': ' || case message_no % 5
               when 0 then 'Could we simplify the release process and publish a short checklist?'
               when 1 then 'The team meetings would be more useful with an agenda shared in advance.'
               when 2 then 'Please consider adding more quiet spaces for focused work.'
               when 3 then 'A monthly demo session could help everyone understand current projects.'
               else 'The onboarding guide would benefit from clearer examples and ownership details.'
           end,
       (select id
        from users
        where email = 'user' || lpad((((message_no - 1) % 15) + 1)::text, 2, '0') || '@speakup.local'),
       message_no % 4 = 0,
       CURRENT_TIMESTAMP - ((25 - message_no) * interval '3 hours')
from generate_series(1, 25) as seed_messages(message_no);

with seed_messages as (
    select id,
           created_at,
           row_number() over (order by created_at, id) as message_no
    from messages
    where content like 'Seed feedback message %'
), seed_users as (
    select id,
           row_number() over (order by email) as user_no
    from users
    where email like 'user%@speakup.local'
)
insert into replies(message_id, content, created_by_user_id, anonymous, created_at)
select message.id,
       'Seed reply ' || reply.reply_no || ' for message ' || lpad(message.message_no::text, 2, '0')
           || ': ' || case reply.reply_no
               when 1 then 'This sounds useful and is worth discussing further.'
               when 2 then 'I agree; a small first step could validate the idea.'
               when 3 then 'Could we collect a few examples before deciding?'
               else 'I can help document the outcome if this moves forward.'
           end,
       author.id,
       (message.message_no + reply.reply_no) % 5 = 0,
       message.created_at + (reply.reply_no * interval '20 minutes')
from seed_messages message
cross join lateral generate_series(1, 2 + ((message.message_no - 1) % 3)::integer) as reply(reply_no)
join seed_users author
  on author.user_no = ((message.message_no + reply.reply_no - 1) % 15) + 1;

with seed_messages as (
    select id,
           created_by_user_id,
           row_number() over (order by created_at, id) as message_no
    from messages
    where content like 'Seed feedback message %'
), seed_users as (
    select id,
           row_number() over (order by email) as user_no
    from users
    where email like 'user%@speakup.local'
)
insert into message_votes(user_id, message_id, vote_type, created_at)
select voter.id,
       message.id,
       case when voter.vote_no <= 3 then 'UPVOTE' else 'DOWNVOTE' end,
       CURRENT_TIMESTAMP
from seed_messages message
cross join lateral (
    select candidate.id,
           row_number() over (order by ((candidate.user_no + message.message_no) % 15)) as vote_no
    from seed_users candidate
    where candidate.id <> message.created_by_user_id
    order by ((candidate.user_no + message.message_no) % 15)
    limit 4
) voter;

with seed_replies as (
    select id,
           created_by_user_id,
           row_number() over (order by created_at, id) as reply_no
    from replies
    where content like 'Seed reply %'
), seed_users as (
    select id,
           row_number() over (order by email) as user_no
    from users
    where email like 'user%@speakup.local'
)
insert into reply_votes(user_id, reply_id, vote_type, created_at)
select voter.id,
       reply.id,
       case when voter.vote_no <= 2 then 'UPVOTE' else 'DOWNVOTE' end,
       CURRENT_TIMESTAMP
from seed_replies reply
cross join lateral (
    select candidate.id,
           row_number() over (order by ((candidate.user_no + reply.reply_no) % 15)) as vote_no
    from seed_users candidate
    where candidate.id <> reply.created_by_user_id
    order by ((candidate.user_no + reply.reply_no) % 15)
    limit 3
) voter;
