package dev.sivalabs.speakup.messages;

import dev.sivalabs.speakup.shared.BaseEntity;
import dev.sivalabs.speakup.users.UserEntity;
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private UserEntity creator;

    @Column(nullable = false)
    private boolean anonymous;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReplyStatus status = ReplyStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moderated_by_user_id")
    private UserEntity moderator;

    @Column(name = "moderated_at")
    private Instant moderatedAt;

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

    public ReplyStatus getStatus() {
        return status;
    }

    public void setStatus(ReplyStatus status) {
        this.status = status;
    }

    public UserEntity getModerator() {
        return moderator;
    }

    public void setModerator(UserEntity moderator) {
        this.moderator = moderator;
    }

    public Instant getModeratedAt() {
        return moderatedAt;
    }

    public void setModeratedAt(Instant moderatedAt) {
        this.moderatedAt = moderatedAt;
    }
}
