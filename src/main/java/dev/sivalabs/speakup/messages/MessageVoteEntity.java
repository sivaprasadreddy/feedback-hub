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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "message_votes",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "message_vote_user_message_unique",
                        columnNames = {"user_id", "message_id"}))
class MessageVoteEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "message_vote_id_generator")
    @SequenceGenerator(name = "message_vote_id_generator", sequenceName = "message_vote_id_seq")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity voter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    private MessageEntity message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VoteType voteType;

    public Long getId() {
        return id;
    }

    public UserEntity getVoter() {
        return voter;
    }

    public void setVoter(UserEntity voter) {
        this.voter = voter;
    }

    public MessageEntity getMessage() {
        return message;
    }

    public void setMessage(MessageEntity message) {
        this.message = message;
    }

    public VoteType getVoteType() {
        return voteType;
    }

    public void setVoteType(VoteType voteType) {
        this.voteType = voteType;
    }
}
