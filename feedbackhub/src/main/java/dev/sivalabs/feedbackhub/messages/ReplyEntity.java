package dev.sivalabs.feedbackhub.messages;

import dev.sivalabs.feedbackhub.shared.BaseEntity;
import jakarta.persistence.Column;
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

@Entity
@Table(name = "replies")
class ReplyEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "reply_id_generator")
    @SequenceGenerator(name = "reply_id_generator", sequenceName = "reply_id_seq")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    private MessageEntity message;

    @Column(nullable = false)
    private String content;

    @Column(name = "created_by_user_id", nullable = false)
    private Long creatorUserId;

    @Column(nullable = false)
    private boolean anonymous;

    @Column(nullable = false)
    private boolean spam;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReplyStatus status = ReplyStatus.ACTIVE;

    @Column(name = "deleted_by_admin_user_id")
    private Long deletedByAdminUserId;

    @Column(name = "deleted_by_admin_at")
    private Instant deletedByAdminAt;

    @Column(name = "upvote_count", nullable = false)
    private long upvoteCount;

    @Column(name = "downvote_count", nullable = false)
    private long downvoteCount;

    public Long getId() {
        return id;
    }

    public MessageEntity getMessage() {
        return message;
    }

    public void setMessage(MessageEntity message) {
        this.message = message;
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

    public boolean isSpam() {
        return spam;
    }

    public void setSpam(boolean spam) {
        this.spam = spam;
    }

    public ReplyStatus getStatus() {
        return status;
    }

    public void setStatus(ReplyStatus status) {
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

    public long getUpvoteCount() {
        return upvoteCount;
    }

    public long getDownvoteCount() {
        return downvoteCount;
    }
}
