package dev.sivalabs.speakup.messages;

import dev.sivalabs.speakup.shared.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "messages")
class MessageEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "message_id_generator")
    @SequenceGenerator(name = "message_id_generator", sequenceName = "message_id_seq")
    private Long id;

    @Column(nullable = false)
    private String content;

    @Column(name = "created_by_user_id", nullable = false)
    private Long creatorUserId;

    @Column(nullable = false)
    private boolean anonymous;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageStatus status = MessageStatus.ACTIVE;

    @Column(name = "deleted_by_admin_user_id")
    private Long deletedByAdminUserId;

    @Column(name = "deleted_by_admin_at")
    private Instant deletedByAdminAt;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "message_topics", joinColumns = @JoinColumn(name = "message_id"))
    @Column(name = "topic", nullable = false)
    private Set<String> topics = new LinkedHashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "sentiment")
    private MessageSentiment sentiment;

    public Long getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getCreatorUserId() {
        return creatorUserId;
    }

    public void setCreatorUserId(Long creatorUserId) {
        this.creatorUserId = creatorUserId;
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    public void setAnonymous(boolean anonymous) {
        this.anonymous = anonymous;
    }

    public MessageStatus getStatus() {
        return status;
    }

    public void setStatus(MessageStatus status) {
        this.status = status;
    }

    public Long getDeletedByAdminUserId() {
        return deletedByAdminUserId;
    }

    public void setDeletedByAdminUserId(Long deletedByAdminUserId) {
        this.deletedByAdminUserId = deletedByAdminUserId;
    }

    public Instant getDeletedByAdminAt() {
        return deletedByAdminAt;
    }

    public void setDeletedByAdminAt(Instant deletedByAdminAt) {
        this.deletedByAdminAt = deletedByAdminAt;
    }

    public Set<String> getTopics() {
        return topics;
    }

    public void setTopics(Set<String> topics) {
        this.topics.clear();
        this.topics.addAll(topics);
    }

    public MessageSentiment getSentiment() {
        return sentiment;
    }

    public void setSentiment(MessageSentiment sentiment) {
        this.sentiment = sentiment;
    }
}
