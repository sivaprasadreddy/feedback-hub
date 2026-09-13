DELETE FROM reply_votes;
DELETE FROM message_votes;
DELETE FROM message_topics;
DELETE FROM replies;
DELETE FROM messages;
DELETE FROM users;

-- pwd is 'secret'
insert into users(id, email, password, name, role, active, created_at) values
(1, 'admin@gmail.com','$2a$10$2bF0hrLWv/bH9kJPzOq4qe3.ky6cMSMl9MbNkAGUG8E2nxjibFtxi','Admin', 'ROLE_ADMIN', true, CURRENT_TIMESTAMP),
(2, 'siva@gmail.com','$2a$10$2bF0hrLWv/bH9kJPzOq4qe3.ky6cMSMl9MbNkAGUG8E2nxjibFtxi','Siva', 'ROLE_USER', true, CURRENT_TIMESTAMP),
(3, 'prasad@gmail.com','$2a$10$2bF0hrLWv/bH9kJPzOq4qe3.ky6cMSMl9MbNkAGUG8E2nxjibFtxi','Prasad', 'ROLE_USER', false, CURRENT_TIMESTAMP);
