alter table replies add column status text not null default 'ACTIVE';

alter table replies add constraint reply_status_check check (status in ('ACTIVE', 'DELETED'));
