package dev.sivalabs.speakup.messages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import dev.sivalabs.speakup.BaseIT;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

class CreateMessageTests extends BaseIT {
    @Autowired
    MessageRepository messageRepository;

    @Test
    void activeUserCanPostAsThemselvesAndMessageAppearsFirstInRecentFeed() throws Exception {
        var userSession = session(login("siva@gmail.com", "secret"));
        var firstContent = "First feedback " + UUID.randomUUID();
        var newestContent = "Newest feedback " + UUID.randomUUID();

        createMessage(userSession, firstContent, "IDENTIFIED");
        createMessage(userSession, newestContent, "IDENTIFIED");

        var feed = mvc.get().uri("/").session(userSession).exchange();
        assertThat(feed).hasStatusOk().hasViewName("index").bodyText().contains("Siva", firstContent, newestContent);
        var html = feed.getMvcResult().getResponse().getContentAsString();
        assertThat(html.indexOf(newestContent)).isLessThan(html.indexOf(firstContent));
    }

    @Test
    void adminCanPostAnonymouslyWithoutExposingCreatorToRegularUsers() {
        var adminSession = session(login("admin@gmail.com", "secret"));
        var content = "Anonymous feedback " + UUID.randomUUID();

        createMessage(adminSession, content, "ANONYMOUS");

        var stored = messageRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(message -> message.getContent().equals(content))
                .findFirst()
                .orElseThrow();
        assertThat(stored.getCreator().getId()).isEqualTo(1L);
        assertThat(stored.isAnonymous()).isTrue();

        var regularUserFeed = mvc.get()
                .uri("/")
                .session(session(login("siva@gmail.com", "secret")))
                .exchange();
        assertThat(regularUserFeed).bodyText().contains("Anonymous", content).doesNotContain("Admin");
    }

    @Test
    void contentAndPostingIdentityAreRequired() {
        var userSession = session(login("siva@gmail.com", "secret"));

        var result = mvc.post()
                .uri("/messages")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("content", "   ")
                .session(userSession)
                .with(csrf())
                .exchange();

        assertThat(result)
                .hasStatusOk()
                .hasViewName("index")
                .bodyText()
                .contains("Message is required", "Choose how to post");
    }

    @Test
    void unauthenticatedAndInactiveUsersCannotCreateMessages() {
        assertThat(mvc.post()
                        .uri("/messages")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("content", "Not allowed")
                        .param("postingIdentity", "IDENTIFIED")
                        .with(csrf())
                        .exchange())
                .hasStatus(HttpStatus.FOUND);

        var inactiveLogin = login("prasad@gmail.com", "secret");
        assertThat(inactiveLogin).hasStatus(HttpStatus.FOUND).hasRedirectedUrl("/login?error");
    }

    private void createMessage(MockHttpSession session, String content, String postingIdentity) {
        assertThat(mvc.post()
                        .uri("/messages")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("content", content)
                        .param("postingIdentity", postingIdentity)
                        .session(session)
                        .with(csrf())
                        .exchange())
                .hasStatus(HttpStatus.FOUND)
                .hasRedirectedUrl("/");
    }
}
