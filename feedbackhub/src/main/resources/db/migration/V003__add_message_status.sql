alter table messages add column status text not null default 'ACTIVE';

alter table messages add constraint message_status_check check (status in ('ACTIVE', 'DELETED'));
