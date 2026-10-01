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

class VoteOnReplyTests extends BaseIT {
    // seeded: message 4101 (by admin) with replies 4101 (admin), 4103 (deleted); message 4102 (by admin); message 4103
    // (by admin) with reply 4102 (siva)
    private static final long MESSAGE_ID = 4101L;
    private static final long OTHER_MESSAGE_ID = 4102L;
    private static final long ADMIN_REPLY_ID = 4101L;
    private static final long OWN_REPLY_MESSAGE_ID = 4103L;
    private static final long OWN_REPLY_ID = 4102L;
    private static final long DELETED_REPLY_ID = 4103L;

    @Autowired
    MessageService messageService;

    @Test
    void userCanPlaceChangeAndRemoveVoteOnAnotherUsersReply() {
        var replyId = ADMIN_REPLY_ID;
        var voterSession = session(login("siva@gmail.com", "secret"));

        assertThat(vote(voterSession, MESSAGE_ID, replyId, "UPVOTE"))
                .hasStatus(HttpStatus.FOUND)
                .hasRedirectedUrl("/messages/" + MESSAGE_ID);
        assertReplyVotes(MESSAGE_ID, replyId, 1, 0, "UPVOTE");

        assertThat(vote(voterSession, MESSAGE_ID, replyId, "DOWNVOTE")).hasStatus(HttpStatus.FOUND);
        assertReplyVotes(MESSAGE_ID, replyId, 0, 1, "DOWNVOTE");
        assertThat(voteType(replyId)).isEqualTo("DOWNVOTE");

        assertThat(removeVote(voterSession, MESSAGE_ID, replyId)).hasStatus(HttpStatus.FOUND);
        assertReplyVotes(MESSAGE_ID, replyId, 0, 0, null);
    }

    @Test
    void countsAndCurrentUsersVoteAreRenderedImmediately() throws Exception {
        var replyId = ADMIN_REPLY_ID;
        var voterSession = session(login("siva@gmail.com", "secret"));
        vote(voterSession, MESSAGE_ID, replyId, "UPVOTE");

        var upvoted = mvc.get()
                .uri("/messages/{id}", MESSAGE_ID)
                .session(voterSession)
                .exchange();
        assertThat(upvoted)
                .bodyText()
                .contains("Remove your reply upvote", "Downvote reply", "1")
                .doesNotContain("Your vote");
        assertThat(upvoted.getMvcResult().getResponse().getContentAsString())
                .contains(
                        "text-emerald-600",
                        "title=\"Remove your reply upvote\"",
                        "action=\"/messages/" + MESSAGE_ID + "/replies/" + replyId + "/vote/remove\"");

        vote(voterSession, MESSAGE_ID, replyId, "DOWNVOTE");
        var downvoted = mvc.get()
                .uri("/messages/{id}", MESSAGE_ID)
                .session(voterSession)
                .exchange();
        assertThat(downvoted)
                .bodyText()
                .contains("Remove your reply downvote", "Upvote reply")
                .doesNotContain("Your vote");
        assertThat(downvoted.getMvcResult().getResponse().getContentAsString())
                .contains(
                        "text-amber-600",
                        "title=\"Remove your reply downvote\"",
                        "action=\"/messages/" + MESSAGE_ID + "/replies/" + replyId + "/vote/remove\"");
    }

    @Test
    void htmxVoteReturnsOnlyUpdatedReplyVoteControls() throws Exception {
        var replyId = ADMIN_REPLY_ID;
        var voterSession = session(login("siva@gmail.com", "secret"));

        var response = mvc.post()
                .uri("/messages/{m}/replies/{r}/vote", MESSAGE_ID, replyId)
                .header("HX-Request", "true")
                .param("voteType", "UPVOTE")
                .session(voterSession)
                .with(csrf())
                .exchange();

        assertThat(response)
                .hasStatusOk()
                .hasViewName("fragments/reply-votes :: votes(messageId=${messageId}, reply=${reply})")
                .bodyText()
                .contains("Remove your reply upvote", "Downvote reply")
                .doesNotContain("Replies", "View message");
        assertThat(response.getMvcResult().getResponse().getContentAsString())
                .contains("class=\"reply-votes", "hx-target=\"closest .reply-votes\"", "hx-swap=\"outerHTML\"");

        var removeResponse = mvc.post()
                .uri("/messages/{m}/replies/{r}/vote/remove", MESSAGE_ID, replyId)
                .header("HX-Request", "true")
                .session(voterSession)
                .with(csrf())
                .exchange();
        assertThat(removeResponse).hasStatusOk().bodyText().contains("Upvote reply", "Downvote reply");
        assertReplyVotes(MESSAGE_ID, replyId, 0, 0, null);
    }

    @Test
    void userCannotVoteOnOwnReplyEvenWhenAnonymous() {
        var userSession = session(login("siva@gmail.com", "secret"));
        var replyId = OWN_REPLY_ID;

        assertThat(vote(userSession, OWN_REPLY_MESSAGE_ID, replyId, "UPVOTE"))
                .hasStatus(HttpStatus.FORBIDDEN)
                .hasViewName("error/403");
        assertThat(mvc.get()
                        .uri("/messages/{id}", OWN_REPLY_MESSAGE_ID)
                        .session(userSession)
                        .exchange())
                .bodyText()
                .doesNotContain(
                        "Upvote reply", "Downvote reply", "Remove your reply upvote", "Remove your reply downvote");
        assertThat(voteType(replyId)).isNull();
    }

    @Test
    void userCannotVoteOnDeletedReply() {
        var replyId = DELETED_REPLY_ID;
        var voterSession = session(login("siva@gmail.com", "secret"));

        assertThat(vote(voterSession, MESSAGE_ID, replyId, "DOWNVOTE"))
                .hasStatus(HttpStatus.FORBIDDEN)
                .hasViewName("error/403");
        assertThat(removeVote(voterSession, MESSAGE_ID, replyId))
                .hasStatus(HttpStatus.FORBIDDEN)
                .hasViewName("error/403");
        assertThat(voteType(replyId)).isNull();
    }

    @Test
    void replyMustBelongToMessageAndUserMustBeAuthenticated() {
        var replyId = ADMIN_REPLY_ID;
        var voterSession = session(login("siva@gmail.com", "secret"));

        assertThat(vote(voterSession, OTHER_MESSAGE_ID, replyId, "UPVOTE"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasViewName("error/404");
        assertThat(mvc.post()
                        .uri("/messages/{m}/replies/{r}/vote", MESSAGE_ID, replyId)
                        .param("voteType", "UPVOTE")
                        .with(csrf())
                        .exchange())
                .hasStatus(HttpStatus.FOUND);
    }

    private String voteType(long replyId) {
        return jdbcClient
                .sql("select vote_type from reply_votes where reply_id = :id and user_id = 2")
                .param("id", replyId)
                .query(String.class)
                .optional()
                .orElse(null);
    }

    private void assertReplyVotes(Long messageId, Long replyId, long upvotes, long downvotes, String currentVote) {
        var dto = messageService.findReplies(messageId, 2L).stream()
                .filter(r -> r.id().equals(replyId))
                .findFirst()
                .orElseThrow();
        assertThat(dto.upvoteCount()).isEqualTo(upvotes);
        assertThat(dto.downvoteCount()).isEqualTo(downvotes);
        assertThat(dto.currentUserVote()).isEqualTo(currentVote);
    }

    private MvcTestResult vote(MockHttpSession session, Long messageId, Long replyId, String type) {
        return mvc.post()
                .uri("/messages/{m}/replies/{r}/vote", messageId, replyId)
                .param("voteType", type)
                .session(session)
                .with(csrf())
                .exchange();
    }

    private MvcTestResult removeVote(MockHttpSession session, Long messageId, Long replyId) {
        return mvc.post()
                .uri("/messages/{m}/replies/{r}/vote/remove", messageId, replyId)
                .session(session)
                .with(csrf())
                .exchange();
    }
}
