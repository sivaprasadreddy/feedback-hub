package dev.sivalabs.feedbackhub.users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_profile_pictures")
class ProfilePictureEntity {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(nullable = false)
    private byte[] content;

    protected ProfilePictureEntity() {}

    ProfilePictureEntity(Long userId, String contentType, byte[] content) {
        this.userId = userId;
        this.contentType = contentType;
        this.content = content;
    }

    String getContentType() {
        return contentType;
    }

    byte[] getContent() {
        return content;
    }
}
