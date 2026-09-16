alter table messages add column deleted_by_admin_user_id bigint;
alter table messages add column deleted_by_admin_at timestamp;

alter table messages add constraint message_deleted_by_admin_user_fk
    foreign key (deleted_by_admin_user_id) references users (id);

alter table messages add constraint message_admin_deletion_audit_check
    check ((deleted_by_admin_user_id is null and deleted_by_admin_at is null)
        or (deleted_by_admin_user_id is not null and deleted_by_admin_at is not null));
