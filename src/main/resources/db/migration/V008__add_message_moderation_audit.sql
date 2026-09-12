alter table messages add column moderated_by_user_id bigint;
alter table messages add column moderated_at timestamp;

alter table messages add constraint message_moderated_by_user_fk
    foreign key (moderated_by_user_id) references users (id);

alter table messages add constraint message_moderation_audit_check
    check ((moderated_by_user_id is null and moderated_at is null)
        or (moderated_by_user_id is not null and moderated_at is not null));
