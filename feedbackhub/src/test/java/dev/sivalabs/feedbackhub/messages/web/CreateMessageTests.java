package dev.sivalabs.feedbackhub.messages.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import dev.sivalabs.feedbackhub.BaseIT;
import dev.sivalabs.feedbackhub.messages.domain.MessageService;
import dev.sivalabs.feedbackhub.messages.domain.models.MessageDto;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

class CreateMessageTests extends BaseIT {
    @Autowired
    MessageService messageService;

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

        var stored = jdbcClient
                .sql("select created_by_user_id, anonymous from messages where content = ?")
                .param(content)
                .query()
                .singleRow();
        assertThat(stored).containsEntry("created_by_user_id", 1L).containsEntry("anonymous", true);

        var regularUserFeed = mvc.get()
                .uri("/")
                .session(session(login("siva@gmail.com", "secret")))
                .exchange();
        assertThat(regularUserFeed).bodyText().contains("Anonymous", content);
        // other seeded messages legitimately show "Admin", so check the author of this message only
        assertThat(messageService.findRecentMessages(2L, 1).data())
                .filteredOn(message -> message.content().equals(content))
                .singleElement()
                .extracting(MessageDto::visibleAuthor)
                .isEqualTo("Anonymous");
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
