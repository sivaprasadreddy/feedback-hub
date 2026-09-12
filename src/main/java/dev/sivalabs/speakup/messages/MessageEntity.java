package dev.sivalabs.speakup.messages;

import dev.sivalabs.speakup.shared.BaseEntity;
import dev.sivalabs.speakup.users.UserEntity;
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
import jakarta.persistence.ManyToOne;
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private UserEntity creator;

    @Column(nullable = false)
    private boolean anonymous;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageStatus status = MessageStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by_admin_user_id")
    private UserEntity deletedByAdmin;

    @Column(name = "deleted_by_admin_at")
    private Instant deletedByAdminAt;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "message_labels", joinColumns = @JoinColumn(name = "message_id"))
    @Column(name = "label", nullable = false)
    private Set<String> labels = new LinkedHashSet<>();

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

    public UserEntity getCreator() {
        return creator;
    }

    public void setCreator(UserEntity creator) {
        this.creator = creator;
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

    public UserEntity getDeletedByAdmin() {
        return deletedByAdmin;
    }

    public void setDeletedByAdmin(UserEntity deletedByAdmin) {
        this.deletedByAdmin = deletedByAdmin;
    }

    public Instant getDeletedByAdminAt() {
        return deletedByAdminAt;
    }

    public void setDeletedByAdminAt(Instant deletedByAdminAt) {
        this.deletedByAdminAt = deletedByAdminAt;
    }

    public Set<String> getLabels() {
        return labels;
    }

    public void setLabels(Set<String> labels) {
        this.labels.clear();
        this.labels.addAll(labels);
    }

    public MessageSentiment getSentiment() {
        return sentiment;
    }

    public void setSentiment(MessageSentiment sentiment) {
        this.sentiment = sentiment;
    }
}
