package dev.sivalabs.feedbackhub.messages.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import dev.sivalabs.feedbackhub.BaseIT;
import dev.sivalabs.feedbackhub.messages.domain.MessageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

class VoteOnMessageTests extends BaseIT {
    // seeded: message 4001 by admin, 4002 by siva, 4003 deleted (by admin), 4005 upvoted by siva
    private static final long ADMIN_MESSAGE_ID = 4001L;
    private static final long OWN_MESSAGE_ID = 4002L;
    private static final long DELETED_MESSAGE_ID = 4003L;
    private static final long UPVOTED_MESSAGE_ID = 4005L;

    @Autowired
    MessageService messageService;

    @Test
    void userCanUpvoteAndDownvoteAnotherUsersMessage() {
        var messageId = ADMIN_MESSAGE_ID;
        var voterSession = session(login("siva@gmail.com", "secret"));

        assertThat(vote(voterSession, messageId, "UPVOTE"))
                .hasStatus(HttpStatus.FOUND)
                .hasRedirectedUrl("/messages/" + messageId);
        assertMessageVotes(messageId, 1, 0, "UPVOTE");

        assertThat(vote(voterSession, messageId, "DOWNVOTE")).hasStatus(HttpStatus.FOUND);
        assertMessageVotes(messageId, 0, 1, "DOWNVOTE");
        assertThat(voteType(messageId)).isEqualTo("DOWNVOTE");
    }

    @Test
    void userCanRemoveExistingVote() {
        var messageId = UPVOTED_MESSAGE_ID;
        var voterSession = session(login("siva@gmail.com", "secret"));

        assertThat(removeVote(voterSession, messageId))
                .hasStatus(HttpStatus.FOUND)
                .hasRedirectedUrl("/messages/" + messageId);
        assertMessageVotes(messageId, 0, 0, null);
        assertThat(voteType(messageId)).isNull();
    }

    @Test
    void countsAndCurrentUsersVoteAreRenderedImmediately() throws Exception {
        var messageId = ADMIN_MESSAGE_ID;
        var voterSession = session(login("siva@gmail.com", "secret"));
        vote(voterSession, messageId, "UPVOTE");

        var upvoted = mvc.get()
                .uri("/messages/{messageId}", messageId)
                .session(voterSession)
                .exchange();
        assertThat(upvoted)
                .bodyText()
                .contains("Remove your upvote", "Downvote message")
                .doesNotContain("Your vote");
        assertThat(upvoted.getMvcResult().getResponse().getContentAsString())
                .contains(
                        "text-emerald-600",
                        "title=\"Remove your upvote\"",
                        "action=\"/messages/" + messageId + "/vote/remove\"");

        vote(voterSession, messageId, "DOWNVOTE");
        var downvoted = mvc.get()
                .uri("/messages/{messageId}", messageId)
                .session(voterSession)
                .exchange();
        assertThat(downvoted)
                .bodyText()
                .contains("Remove your downvote", "Upvote message")
                .doesNotContain("Your vote");
        assertThat(downvoted.getMvcResult().getResponse().getContentAsString())
                .contains(
                        "text-amber-600",
                        "title=\"Remove your downvote\"",
                        "action=\"/messages/" + messageId + "/vote/remove\"");
    }

    @Test
    void htmxVoteReturnsOnlyUpdatedVoteControls() throws Exception {
        var messageId = ADMIN_MESSAGE_ID;
        var voterSession = session(login("siva@gmail.com", "secret"));

        var response = mvc.post()
                .uri("/messages/{messageId}/vote", messageId)
                .header("HX-Request", "true")
                .param("voteType", "UPVOTE")
                .session(voterSession)
                .with(csrf())
                .exchange();

        assertThat(response)
                .hasStatusOk()
                .hasViewName("fragments/message-votes :: votes(message=${message}, returnToHome=${returnToHome})")
                .bodyText()
                .contains("Remove your upvote", "Downvote message")
                .doesNotContain("Feedback", "View message");
        assertThat(response.getMvcResult().getResponse().getContentAsString())
                .contains("class=\"message-votes", "hx-target=\"closest .message-votes\"", "hx-swap=\"outerHTML\"");

        var removeResponse = mvc.post()
                .uri("/messages/{messageId}/vote/remove", messageId)
                .header("HX-Request", "true")
                .session(voterSession)
                .with(csrf())
                .exchange();
        assertThat(removeResponse).hasStatusOk().bodyText().contains("Upvote message", "Downvote message");
        assertMessageVotes(messageId, 0, 0, null);
    }

    @Test
    void userCannotVoteOnOwnMessageEvenWhenAnonymous() {
        var userSession = session(login("siva@gmail.com", "secret"));
        var messageId = OWN_MESSAGE_ID;

        assertThat(vote(userSession, messageId, "UPVOTE"))
                .hasStatus(HttpStatus.FORBIDDEN)
                .hasViewName("error/403");
        assertThat(mvc.get()
                        .uri("/messages/{messageId}", messageId)
                        .session(userSession)
                        .exchange())
                .bodyText()
                .doesNotContain("Upvote message", "Downvote message", "Remove your upvote", "Remove your downvote");
        assertThat(voteType(messageId)).isNull();
    }

    @Test
    void userCannotVoteOnDeletedMessage() {
        var messageId = DELETED_MESSAGE_ID;
        var voterSession = session(login("siva@gmail.com", "secret"));

        assertThat(vote(voterSession, messageId, "DOWNVOTE"))
                .hasStatus(HttpStatus.FORBIDDEN)
                .hasViewName("error/403");
        assertThat(removeVote(voterSession, messageId))
                .hasStatus(HttpStatus.FORBIDDEN)
                .hasViewName("error/403");
        assertThat(voteType(messageId)).isNull();
    }

    @Test
    void unauthenticatedUserCannotVote() {
        assertThat(mvc.post()
                        .uri("/messages/{id}/vote", ADMIN_MESSAGE_ID)
                        .param("voteType", "UPVOTE")
                        .with(csrf())
                        .exchange())
                .hasStatus(HttpStatus.FOUND);
    }

    private String voteType(long messageId) {
        return jdbcClient
                .sql("select vote_type from message_votes where message_id = :id and user_id = 2")
                .param("id", messageId)
                .query(String.class)
                .optional()
                .orElse(null);
    }

    private void assertMessageVotes(Long messageId, long upvotes, long downvotes, String currentUserVote) {
        var details = messageService.findMessage(messageId, 2L);
        assertThat(details.upvoteCount()).isEqualTo(upvotes);
        assertThat(details.downvoteCount()).isEqualTo(downvotes);
        assertThat(details.currentUserVote()).isEqualTo(currentUserVote);
    }

    private MvcTestResult vote(MockHttpSession session, Long messageId, String voteType) {
        return mvc.post()
                .uri("/messages/{messageId}/vote", messageId)
                .param("voteType", voteType)
                .session(session)
                .with(csrf())
                .exchange();
    }

    private MvcTestResult removeVote(MockHttpSession session, Long messageId) {
        return mvc.post()
                .uri("/messages/{messageId}/vote/remove", messageId)
                .session(session)
                .with(csrf())
                .exchange();
    }
}
