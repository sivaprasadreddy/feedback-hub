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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "reply_votes",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "reply_vote_user_reply_unique",
                        columnNames = {"user_id", "reply_id"}))
class ReplyVoteEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "reply_vote_id_generator")
    @SequenceGenerator(name = "reply_vote_id_generator", sequenceName = "reply_vote_id_seq")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long voterUserId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reply_id", nullable = false)
    private ReplyEntity reply;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VoteType voteType;

    public Long getId() {
        return id;
    }

    public Long getVoterUserId() {
        return voterUserId;
    }

    public void setVoterUserId(Long voterUserId) {
        this.voterUserId = voterUserId;
    }

    public ReplyEntity getReply() {
        return reply;
    }

    public void setReply(ReplyEntity reply) {
        this.reply = reply;
    }

    public VoteType getVoteType() {
        return voteType;
    }

    public void setVoteType(VoteType voteType) {
        this.voteType = voteType;
    }
}
