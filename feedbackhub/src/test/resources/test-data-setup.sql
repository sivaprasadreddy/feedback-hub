DELETE FROM reply_votes;
DELETE FROM message_votes;
DELETE FROM message_topics;
DELETE FROM replies;
DELETE FROM messages;
DELETE FROM user_profile_pictures;
DELETE FROM users;

-- pwd is 'secret'
insert into users(id, email, password, name, role, active, created_at) values
(1, 'admin@gmail.com','$2a$10$2bF0hrLWv/bH9kJPzOq4qe3.ky6cMSMl9MbNkAGUG8E2nxjibFtxi','Admin', 'ROLE_ADMIN', true, CURRENT_TIMESTAMP),
(2, 'siva@gmail.com','$2a$10$2bF0hrLWv/bH9kJPzOq4qe3.ky6cMSMl9MbNkAGUG8E2nxjibFtxi','Siva', 'ROLE_USER', true, CURRENT_TIMESTAMP),
(3, 'prasad@gmail.com','$2a$10$2bF0hrLWv/bH9kJPzOq4qe3.ky6cMSMl9MbNkAGUG8E2nxjibFtxi','Prasad', 'ROLE_USER', false, CURRENT_TIMESTAMP);

-- messages, replies and votes used by the integration tests

insert into messages(id, content, created_by_user_id, anonymous, created_at, status, sentiment, upvote_count, downvote_count, reply_count) values
-- sentiment analysis
(1001, 'Sentiment seed happy one', 2, false, '2020-03-10 12:00:00', 'ACTIVE', 'HAPPY', 0, 0, 0),
(1002, 'Sentiment seed happy two', 2, false, '2020-03-10 12:00:00', 'ACTIVE', 'HAPPY', 0, 0, 0),
(1003, 'Sentiment seed angry', 2, false, '2020-03-10 12:00:00', 'ACTIVE', 'ANGRY', 0, 0, 0),
(1004, 'Sentiment seed sad previous day', 2, false, '2020-03-09 12:00:00', 'ACTIVE', 'SAD', 0, 0, 0),
-- popular feed
(1010, 'Popular fewer upvotes seed', 1, false, '2020-06-01 09:00:00', 'ACTIVE', null, 1, 0, 0),
(1011, 'Popular more upvotes seed', 1, false, '2020-06-01 09:05:00', 'ACTIVE', null, 2, 0, 0),
(1012, 'Popular tie older seed', 1, false, '2020-06-02 09:00:00', 'ACTIVE', null, 3, 0, 0),
(1013, 'Popular tie newer seed', 1, false, '2020-06-03 09:00:00', 'ACTIVE', null, 3, 0, 0),
(1014, 'Popular no downvotes older seed', 1, false, '2020-06-04 09:00:00', 'ACTIVE', null, 1, 0, 0),
(1015, 'Popular downvoted newer seed', 1, false, '2020-06-05 09:00:00', 'ACTIVE', null, 1, 2, 0),
(1016, 'Anonymous popular seed', 1, true, '2020-06-07 09:00:00', 'ACTIVE', null, 3, 0, 0),
-- recent feed
(1101, 'Older recent seed', 2, false, '2022-12-01 09:00:00', 'ACTIVE', null, 0, 0, 0),
(1102, 'Newer recent seed', 2, false, '2022-12-02 09:00:00', 'ACTIVE', null, 0, 0, 0),
(1103, 'Feed details seed', 1, false, '2022-12-03 09:00:00', 'ACTIVE', null, 1, 0, 1),
(1104, 'Downvoted feed seed', 1, false, '2022-12-04 09:00:00', 'ACTIVE', null, 0, 1, 0),
(1105, 'Home voting seed', 1, false, '2022-12-04 10:00:00', 'ACTIVE', null, 0, 0, 0),
(1106, 'Anonymous feed seed', 1, true, '2022-12-05 09:00:00', 'ACTIVE', null, 0, 0, 0),
(1107, 'Deleted feed seed', 1, false, '2022-12-06 09:00:00', 'DELETED', null, 0, 0, 0);

insert into messages(id, content, created_by_user_id, anonymous, created_at, status, sentiment, upvote_count, downvote_count, reply_count)
select 1200 + g, 'Paging seed message ' || lpad(g::text, 2, '0'), 2, false, timestamp '2020-01-01 00:00:00' + g * interval '1 minute', 'ACTIVE', null, 0, 0, 0
from generate_series(1, 11) g;

insert into replies(id, message_id, content, created_by_user_id, anonymous, created_at, status, upvote_count, downvote_count) values
(1103, 1103, 'Feed reply seed', 2, false, '2022-12-03 10:00:00', 'ACTIVE', 0, 0);

insert into message_votes(id, user_id, message_id, vote_type, created_at) values
(1010, 3, 1010, 'UPVOTE', '2020-06-01 10:00:00'),
(1011, 2, 1011, 'UPVOTE', '2020-06-01 10:00:00'),
(1012, 3, 1011, 'UPVOTE', '2020-06-01 10:00:00'),
(1013, 1, 1012, 'UPVOTE', '2020-06-02 10:00:00'),
(1014, 2, 1012, 'UPVOTE', '2020-06-02 10:00:00'),
(1015, 3, 1012, 'UPVOTE', '2020-06-02 10:00:00'),
(1016, 1, 1013, 'UPVOTE', '2020-06-03 10:00:00'),
(1017, 2, 1013, 'UPVOTE', '2020-06-03 10:00:00'),
(1018, 3, 1013, 'UPVOTE', '2020-06-03 10:00:00'),
(1019, 3, 1014, 'UPVOTE', '2020-06-04 10:00:00'),
(1020, 3, 1015, 'UPVOTE', '2020-06-05 10:00:00'),
(1021, 1, 1015, 'DOWNVOTE', '2020-06-05 10:00:00'),
(1022, 2, 1015, 'DOWNVOTE', '2020-06-05 10:00:00'),
(1023, 1, 1016, 'UPVOTE', '2020-06-07 10:00:00'),
(1024, 2, 1016, 'UPVOTE', '2020-06-07 10:00:00'),
(1025, 3, 1016, 'UPVOTE', '2020-06-07 10:00:00'),
(1030, 2, 1103, 'UPVOTE', '2022-12-03 11:00:00'),
(1031, 2, 1104, 'DOWNVOTE', '2022-12-04 11:00:00');

insert into messages(id, content, created_by_user_id, anonymous, created_at, updated_at, status, upvote_count, downvote_count, reply_count) values
(2001, 'Edit seed original content 2001', 2, false, '2020-01-01 10:00:00', '1970-01-01 00:00:00', 'ACTIVE', 0, 0, 0),
(2002, 'Edit seed anonymous original 2002', 2, true, '2020-01-01 10:01:00', null, 'ACTIVE', 0, 0, 0),
(2003, 'Edit seed unchanged content 2003', 2, false, '2020-01-01 10:02:00', null, 'ACTIVE', 0, 0, 0),
(2004, 'Edit seed other owner content 2004', 1, true, '2020-01-01 10:03:00', null, 'ACTIVE', 0, 0, 0),
(2005, 'Edit seed deleted content 2005', 2, false, '2020-01-01 10:04:00', null, 'DELETED', 0, 0, 0),
(2011, 'Delete seed owner content 2011', 2, false, '2020-01-01 11:00:00', null, 'ACTIVE', 0, 0, 0),
(2012, 'Delete seed protected anonymous content 2012', 1, true, '2020-01-01 11:01:00', null, 'ACTIVE', 0, 0, 0),
(2013, 'Delete seed already deleted content 2013', 2, false, '2020-01-01 11:02:00', null, 'DELETED', 0, 0, 0),
(2021, 'View seed message details 2021', 2, false, '2020-01-01 12:00:00', null, 'ACTIVE', 0, 0, 0),
(2022, 'View seed private creator 2022', 1, true, '2020-01-01 12:01:00', null, 'ACTIVE', 0, 0, 0),
(2023, 'View seed deleted content 2023', 2, false, '2020-01-01 12:02:00', null, 'DELETED', 0, 0, 0),
(2031, 'PDF export seed message 2031', 2, false, '2020-01-01 13:00:00', null, 'ACTIVE', 0, 0, 2),
(2032, 'PDF link seed message 2032', 1, false, '2022-06-02 00:00:00', null, 'ACTIVE', 0, 0, 0);

insert into replies(id, message_id, content, created_by_user_id, anonymous, created_at, status, spam, upvote_count, downvote_count) values
(2031, 2031, 'First PDF seed reply 2031', 2, false, '2020-01-01 13:01:00', 'ACTIVE', false, 0, 0),
(2032, 2031, 'Second anonymous PDF seed reply 2032', 2, true, '2020-01-01 13:02:00', 'ACTIVE', false, 0, 0);

insert into messages(id, content, created_by_user_id, anonymous, created_at, status, upvote_count, downvote_count, reply_count) values
(3001, 'Reply-test message for creating replies 3001', 2, false, '2020-01-01 00:00:00', 'ACTIVE', 0, 0, 0),
(3002, 'Reply-test deleted message 3002', 2, false, '2020-01-01 00:00:00', 'DELETED', 0, 0, 0),
(3010, 'Reply-test message for deleting own reply 3010', 2, false, '2020-01-01 00:00:00', 'ACTIVE', 0, 0, 1),
(3011, 'Reply-test message for deleting admin reply 3011', 2, false, '2020-01-01 00:00:00', 'ACTIVE', 0, 0, 1),
(3012, 'Reply-test message with already deleted reply 3012', 2, false, '2020-01-01 00:00:00', 'ACTIVE', 0, 0, 0),
(3020, 'Reply-test message for editing replies 3020', 2, false, '2020-01-01 00:00:00', 'ACTIVE', 0, 0, 3),
(3030, 'Reply-test discussion 3030', 2, false, '2020-01-01 00:00:00', 'ACTIVE', 0, 0, 2),
(3031, 'Reply-test deleted reply discussion 3031', 2, false, '2020-01-01 00:00:00', 'ACTIVE', 0, 0, 0),
(3032, 'Reply-test other discussion 3032', 2, false, '2020-01-01 00:00:00', 'ACTIVE', 0, 0, 1);

insert into replies(id, message_id, content, created_by_user_id, anonymous, created_at, updated_at, status, upvote_count, downvote_count) values
(3010, 3010, 'Reply to delete 3010', 2, false, '2020-01-01 00:00:00', null, 'ACTIVE', 0, 0),
(3011, 3011, 'Protected anonymous reply 3011', 1, true, '2020-01-01 00:00:00', null, 'ACTIVE', 0, 0),
(3012, 3012, 'Already deleted reply 3012', 2, false, '2020-01-01 00:00:00', null, 'DELETED', 0, 0),
(3020, 3020, 'Original reply 3020', 2, false, '2020-01-01 00:00:00', '2000-01-01 00:00:00', 'ACTIVE', 0, 0),
(3021, 3020, 'Anonymous reply 3021', 2, true, '2020-01-01 00:01:00', null, 'ACTIVE', 0, 0),
(3022, 3020, 'Other owner reply 3022', 1, false, '2020-01-01 00:02:00', null, 'ACTIVE', 0, 0),
(3023, 3020, 'Deleted reply 3023', 2, false, '2020-01-01 00:03:00', null, 'DELETED', 0, 0),
(3030, 3030, 'First visible reply 3030', 2, false, '2020-01-01 10:00:00', null, 'ACTIVE', 0, 0),
(3031, 3030, 'Second anonymous reply 3031', 1, true, '2020-01-01 11:00:00', null, 'ACTIVE', 0, 0),
(3032, 3031, 'Sensitive deleted reply 3032', 2, false, '2020-01-01 10:00:00', null, 'DELETED', 0, 0),
(3033, 3032, 'Other message reply 3033', 2, false, '2020-01-01 10:00:00', null, 'ACTIVE', 0, 0);

insert into messages(id, content, created_by_user_id, anonymous, status, upvote_count, downvote_count, reply_count, created_at) values
(4001, 'Vote seed message by admin', 1, false, 'ACTIVE', 0, 0, 0, '2020-01-01 00:00:00'),
(4002, 'Vote seed message by siva', 2, true, 'ACTIVE', 0, 0, 0, '2020-01-01 00:00:00'),
(4003, 'Vote seed deleted message', 1, false, 'DELETED', 0, 0, 0, '2020-01-01 00:00:00'),
(4004, 'Engagement seed message with zero counts', 2, false, 'ACTIVE', 0, 0, 0, '2020-01-01 00:00:00'),
(4005, 'Vote seed message upvoted by siva', 1, false, 'ACTIVE', 1, 0, 0, '2020-01-01 00:00:00'),
(4006, 'Engagement seed message with stale upvote count', 1, false, 'ACTIVE', 0, 0, 0, '2020-01-01 00:00:00'),
(4101, 'Reply vote seed message', 1, false, 'ACTIVE', 0, 0, 2, '2020-01-01 00:00:00'),
(4102, 'Reply vote seed other message', 1, false, 'ACTIVE', 0, 0, 0, '2020-01-01 00:00:00'),
(4103, 'Reply vote seed message for own reply', 1, false, 'ACTIVE', 0, 0, 1, '2020-01-01 00:00:00'),
(4201, 'Reply spam seed message', 2, false, 'ACTIVE', 0, 0, 0, '2020-01-01 00:00:00');

insert into replies(id, message_id, content, created_by_user_id, anonymous, status, upvote_count, downvote_count, created_at) values
(4101, 4101, 'Reply vote seed reply by admin', 1, false, 'ACTIVE', 0, 0, '2020-01-01 00:00:00'),
(4103, 4101, 'Reply vote seed deleted reply', 1, false, 'DELETED', 0, 0, '2020-01-01 00:00:00'),
(4102, 4103, 'Reply vote seed reply by siva', 2, true, 'ACTIVE', 0, 0, '2020-01-01 00:00:00');

insert into message_votes(id, user_id, message_id, vote_type, created_at) values
(4001, 2, 4005, 'UPVOTE', '2020-01-01 00:00:00'),
(4002, 2, 4006, 'UPVOTE', '2020-01-01 00:00:00');

insert into messages(id, content, created_by_user_id, anonymous, created_at, status, upvote_count, downvote_count, reply_count) values
(5001, 'Moderation admin-deleted message', 2, false, '2022-06-01 10:00:00', 'ACTIVE', 0, 0, 1),
(5002, 'Moderation anonymous admin deletion', 2, true, '2022-06-01 10:01:00', 'ACTIVE', 0, 0, 0),
(5003, 'Moderation protected from user deletion', 1, false, '2022-06-01 10:02:00', 'ACTIVE', 0, 0, 0),
(5004, 'Moderation review target', 2, false, '2022-06-01 10:03:00', 'ACTIVE', 0, 0, 0),
(5005, 'Moderation analyze from admin', 2, false, '2022-06-01 10:04:00', 'ACTIVE', 0, 0, 0),
(5006, 'Moderation failed admin analysis', 2, false, '2022-06-01 10:05:00', 'ACTIVE', 0, 0, 0),
(5007, 'Moderation message holding replies to moderate', 2, false, '2022-06-01 10:06:00', 'ACTIVE', 0, 0, 3),
(5008, 'Moderation message holding anonymous reply', 2, false, '2022-06-01 10:07:00', 'ACTIVE', 0, 0, 1),
(5009, 'Moderation message holding protected reply', 1, false, '2022-06-01 10:08:00', 'ACTIVE', 0, 0, 1),
(5010, 'Moderation message with paginated replies', 2, false, '2020-01-01 00:00:00', 'ACTIVE', 0, 0, 22);

insert into messages(id, content, created_by_user_id, anonymous, created_at, status, upvote_count, downvote_count, reply_count)
select 5300 + g, 'Moderation paginated admin message ' || g, 2, false, timestamp '2020-01-01 00:00:00' + g * interval '1 second', 'ACTIVE', 0, 0, 0
from generate_series(0, 21) g;

insert into replies(id, message_id, content, created_by_user_id, anonymous, created_at, status, spam, upvote_count, downvote_count) values
(5100, 5001, 'Moderation preserved reply', 2, false, '2022-06-01 11:05:00', 'ACTIVE', false, 0, 0),
(5101, 5007, 'Moderation admin-deleted reply', 2, false, '2022-06-01 11:00:00', 'ACTIVE', false, 0, 0),
(5102, 5008, 'Moderation anonymous admin-deleted reply', 2, true, '2022-06-01 11:01:00', 'ACTIVE', false, 0, 0),
(5103, 5009, 'Moderation protected reply', 1, false, '2022-06-01 11:02:00', 'ACTIVE', false, 0, 0),
(5104, 5007, 'Moderation reply review target', 2, false, '2022-06-01 11:03:00', 'ACTIVE', false, 0, 0),
(5105, 5007, 'Moderation spam reply for admin', 2, false, '2022-06-01 11:04:00', 'ACTIVE', true, 0, 0);

insert into replies(id, message_id, content, created_by_user_id, anonymous, created_at, status, spam, upvote_count, downvote_count)
select 5200 + g, 5010, 'Moderation paginated admin reply ' || g, 2, false, timestamp '2020-01-01 00:00:00' + g * interval '1 second', 'ACTIVE', false, 0, 0
from generate_series(0, 21) g;
