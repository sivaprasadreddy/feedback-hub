package dev.sivalabs.feedbackhub.messages.web;

import static org.assertj.core.api.Assertions.assertThat;

import dev.sivalabs.feedbackhub.BaseIT;
import dev.sivalabs.feedbackhub.messages.domain.MessageService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ViewMessageTests extends BaseIT {
    private static final long USER_MESSAGE_ID = 2021L;
    private static final long ANONYMOUS_ADMIN_MESSAGE_ID = 2022L;
    private static final long DELETED_MESSAGE_ID = 2023L;

    @Test
    void activeUserCanViewMessageAndCurrentEngagementInformation() {
        var userSession = session(login("siva@gmail.com", "secret"));

        assertThat(mvc.get()
                        .uri("/messages/{id}", USER_MESSAGE_ID)
                        .session(userSession)
                        .exchange())
                .hasStatusOk()
                .hasViewName("messages/view")
                .bodyText()
                .contains("Siva", "View seed message details 2021", "Upvotes", "Downvotes", "Replies")
                .doesNotContain("Your vote", "No vote");
    }

    @Test
    void anonymousMessageDoesNotExposeItsCreatorToRegularUser() {
        var userSession = session(login("siva@gmail.com", "secret"));

        assertThat(mvc.get()
                        .uri("/messages/{id}", ANONYMOUS_ADMIN_MESSAGE_ID)
                        .session(userSession)
                        .exchange())
                .bodyText()
                .contains("Anonymous", "View seed private creator 2022")
                .doesNotContain("Admin", "admin@gmail.com");
    }

    @Test
    void deletedMessageShowsPlaceholderInsteadOfOriginalContent() {
        var userSession = session(login("siva@gmail.com", "secret"));

        assertThat(mvc.get()
                        .uri("/messages/{id}", DELETED_MESSAGE_ID)
                        .session(userSession)
                        .exchange())
                .bodyText()
                .contains(MessageService.DELETED_CONTENT)
                .doesNotContain("View seed deleted content 2023");

        assertThat(mvc.get().uri("/").session(userSession).exchange())
                .bodyText()
                .doesNotContain("View seed deleted content 2023");
    }

    @Test
    void unauthenticatedAndInactiveUsersCannotViewMessageDetails() {
        assertThat(mvc.get().uri("/messages/1").exchange()).hasStatus(HttpStatus.FOUND);
        assertThat(login("prasad@gmail.com", "secret"))
                .hasStatus(HttpStatus.FOUND)
                .hasRedirectedUrl("/login?error");
    }
}
