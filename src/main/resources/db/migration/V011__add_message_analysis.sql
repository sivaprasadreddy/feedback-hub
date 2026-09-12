alter table messages add column sentiment text;

alter table messages add constraint message_sentiment_check
    check (sentiment is null or sentiment in ('HAPPY', 'SAD', 'ANGRY', 'DISAPPOINTED'));

create table message_labels
(
    message_id bigint not null,
    label      text   not null,
    primary key (message_id, label),
    constraint message_label_message_fk foreign key (message_id) references messages (id)
);
