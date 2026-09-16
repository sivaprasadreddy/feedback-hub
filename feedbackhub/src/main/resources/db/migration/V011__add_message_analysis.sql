alter table messages add column sentiment text;

alter table messages add constraint message_sentiment_check
    check (sentiment is null or sentiment in ('NEUTRAL', 'HAPPY', 'SAD', 'ANGRY', 'DISAPPOINTED'));

create table message_topics
(
    message_id bigint not null,
    topic      text   not null,
    primary key (message_id, topic),
    constraint message_topic_message_fk foreign key (message_id) references messages (id)
);
