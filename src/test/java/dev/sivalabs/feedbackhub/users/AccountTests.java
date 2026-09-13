package dev.sivalabs.feedbackhub.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

import dev.sivalabs.feedbackhub.BaseIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class AccountTests extends BaseIT {
    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void accountPageShowsThreePanelsAndBasicDetails() {
        var session = session(login("siva@gmail.com", "secret"));

        assertThat(mvc.get().uri("/account").session(session).exchange())
                .hasStatusOk()
                .hasViewName("account")
                .bodyText()
                .contains(
                        "Account",
                        "Siva",
                        "siva@gmail.com",
                        "Profile picture",
                        "Maximum size: 1 MB",
                        "Change password");
    }

    @Test
    void userCanUploadAndReplaceProfilePicture() throws Exception {
        var session = session(login("siva@gmail.com", "secret"));
        var firstImage = new MockMultipartFile("profilePicture", "avatar.png", "image/png", new byte[] {1, 2, 3});

        assertThat(mvc.perform(multipart("/account/profile-picture")
                        .file(firstImage)
                        .session(session)
                        .with(csrf())))
                .hasStatus(HttpStatus.FOUND)
                .hasRedirectedUrl("/account");
        var storedImage =
                mvc.get().uri("/account/profile-picture").session(session).exchange();
        assertThat(storedImage).hasStatusOk().hasContentType(MediaType.IMAGE_PNG);
        assertThat(storedImage.getMvcResult().getResponse().getContentAsByteArray())
                .containsExactly(1, 2, 3);

        var replacement = new MockMultipartFile("profilePicture", "avatar.webp", "image/webp", new byte[] {4, 5});
        assertThat(mvc.perform(multipart("/account/profile-picture")
                        .file(replacement)
                        .session(session)
                        .with(csrf())))
                .hasStatus(HttpStatus.FOUND);
        var replacedImage =
                mvc.get().uri("/account/profile-picture").session(session).exchange();
        assertThat(replacedImage.getMvcResult().getResponse().getContentAsByteArray())
                .containsExactly(4, 5);
    }

    @Test
    void profilePictureMustBeSupportedImageWithinOneMegabyte() throws Exception {
        var session = session(login("siva@gmail.com", "secret"));
        var oversized = new MockMultipartFile(
                "profilePicture",
                "large.png",
                "image/png",
                new byte[(int) AccountService.MAX_PROFILE_PICTURE_SIZE + 1]);

        assertThat(mvc.perform(multipart("/account/profile-picture")
                        .file(oversized)
                        .session(session)
                        .with(csrf())))
                .hasStatus(HttpStatus.FOUND)
                .flash()
                .containsEntry("errorMessage", "Profile picture must not exceed 1 MB");
        var textFile = new MockMultipartFile("profilePicture", "notes.txt", "text/plain", new byte[] {1});
        assertThat(mvc.perform(multipart("/account/profile-picture")
                        .file(textFile)
                        .session(session)
                        .with(csrf())))
                .hasStatus(HttpStatus.FOUND)
                .flash()
                .containsEntry("errorMessage", "Use a JPEG, PNG, GIF, or WebP image");
    }

    @Test
    void userCanChangePasswordAfterProvidingCurrentPasswordAndConfirmation() {
        var session = session(login("siva@gmail.com", "secret"));

        assertThat(mvc.post()
                        .uri("/account/password")
                        .param("currentPassword", "secret")
                        .param("newPassword", "new-secret-123")
                        .param("confirmPassword", "new-secret-123")
                        .session(session)
                        .with(csrf())
                        .exchange())
                .hasStatus(HttpStatus.FOUND)
                .hasRedirectedUrl("/account");
        assertThat(passwordEncoder.matches(
                        "new-secret-123",
                        userRepository.findById(2L).orElseThrow().getPassword()))
                .isTrue();
    }

    @Test
    void passwordChangeValidatesCurrentPasswordAndConfirmation() {
        var session = session(login("siva@gmail.com", "secret"));

        assertThat(mvc.post()
                        .uri("/account/password")
                        .param("currentPassword", "wrong-password")
                        .param("newPassword", "new-secret-123")
                        .param("confirmPassword", "new-secret-123")
                        .session(session)
                        .with(csrf())
                        .exchange())
                .hasStatusOk()
                .bodyText()
                .contains("Current password is incorrect");
        assertThat(mvc.post()
                        .uri("/account/password")
                        .param("currentPassword", "secret")
                        .param("newPassword", "new-secret-123")
                        .param("confirmPassword", "different-password")
                        .session(session)
                        .with(csrf())
                        .exchange())
                .hasStatusOk()
                .bodyText()
                .contains("New passwords do not match");
    }
}
