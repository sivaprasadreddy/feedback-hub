package dev.sivalabs.feedbackhub.messages.web;

import static org.assertj.core.api.Assertions.assertThat;

import dev.sivalabs.feedbackhub.BaseIT;
import dev.sivalabs.feedbackhub.messages.domain.MessageService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ViewRepliesTests extends BaseIT {
    private static final long DISCUSSION_MESSAGE_ID = 3030L;
    private static final String IDENTIFIED_REPLY = "First visible reply 3030";
    private static final String ANONYMOUS_REPLY = "Second anonymous reply 3031";
    private static final long DELETED_REPLY_MESSAGE_ID = 3031L;
    private static final String DELETED_REPLY = "Sensitive deleted reply 3032";
    private static final long OTHER_MESSAGE_ID = 3032L;
    private static final String OTHER_REPLY = "Other message reply 3033";

    @Test
    void repliesAppearUnderMessageWithAuthorTimestampsAndEngagementInformation() throws Exception {
        var userSession = session(login("siva@gmail.com", "secret"));

        var result = mvc.get()
                .uri("/messages/{id}", DISCUSSION_MESSAGE_ID)
                .session(userSession)
                .exchange();

        assertThat(result)
                .hasStatusOk()
                .hasViewName("messages/view")
                .bodyText()
                .contains("Replies", "Siva", IDENTIFIED_REPLY, "Anonymous", ANONYMOUS_REPLY, "Upvotes", "Downvotes")
                .doesNotContain("Your vote", "No vote", "Updated")
                .doesNotContain("Admin", "admin@gmail.com");
        var html = result.getMvcResult().getResponse().getContentAsString();
        assertThat(html.indexOf("id=\"reply-content\"")).isLessThan(html.indexOf("id=\"replies-heading\""));
        assertThat(html.indexOf(IDENTIFIED_REPLY)).isLessThan(html.indexOf(ANONYMOUS_REPLY));
        assertThat(html).containsPattern("\\d{2} [A-Z][a-z]{2} 2020 \\d{2}:\\d{2}");
    }

    @Test
    void deletedReplyShowsPlaceholderWithoutExposingOriginalContent() {
        var userSession = session(login("siva@gmail.com", "secret"));

        assertThat(mvc.get()
                        .uri("/messages/{id}", DELETED_REPLY_MESSAGE_ID)
                        .session(userSession)
                        .exchange())
                .bodyText()
                .contains(MessageService.DELETED_REPLY_CONTENT)
                .doesNotContain(DELETED_REPLY);
    }

    @Test
    void onlyDirectRepliesForCurrentMessageAreDisplayed() {
        var userSession = session(login("siva@gmail.com", "secret"));

        assertThat(mvc.get()
                        .uri("/messages/{id}", DISCUSSION_MESSAGE_ID)
                        .session(userSession)
                        .exchange())
                .bodyText()
                .contains(IDENTIFIED_REPLY)
                .doesNotContain(OTHER_REPLY);
        assertThat(mvc.get()
                        .uri("/messages/{id}", OTHER_MESSAGE_ID)
                        .session(userSession)
                        .exchange())
                .bodyText()
                .contains(OTHER_REPLY)
                .doesNotContain(IDENTIFIED_REPLY);
    }

    @Test
    void unauthenticatedUserCannotViewReplies() {
        assertThat(mvc.get().uri("/messages/1").exchange()).hasStatus(HttpStatus.FOUND);
    }
}
