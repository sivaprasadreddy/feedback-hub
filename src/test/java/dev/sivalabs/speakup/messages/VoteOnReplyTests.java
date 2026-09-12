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
import org.springframework.test.web.servlet.assertj.MvcTestResult;

class VoteOnReplyTests extends BaseIT {
    @Autowired
    MessageRepository messageRepository;

    @Autowired
    ReplyRepository replyRepository;

    @Autowired
    ReplyVoteRepository replyVoteRepository;

    @Autowired
    MessageService messageService;

    @Test
    void userCanPlaceChangeAndRemoveVoteOnAnotherUsersReply() {
        var ownerSession = session(login("admin@gmail.com", "secret"));
        var message = createMessage(ownerSession);
        var reply = createReply(ownerSession, message.getId(), "ANONYMOUS");
        var voterSession = session(login("siva@gmail.com", "secret"));

        assertThat(vote(voterSession, message.getId(), reply.getId(), "UPVOTE"))
                .hasStatus(HttpStatus.FOUND)
                .hasRedirectedUrl("/messages/" + message.getId());
        assertReplyVotes(message.getId(), reply.getId(), 1, 0, "UPVOTE");

        assertThat(vote(voterSession, message.getId(), reply.getId(), "DOWNVOTE"))
                .hasStatus(HttpStatus.FOUND);
        assertReplyVotes(message.getId(), reply.getId(), 0, 1, "DOWNVOTE");
        assertThat(replyVoteRepository.findAll())
                .filteredOn(v -> v.getReply().getId().equals(reply.getId()))
                .hasSize(1);

        assertThat(removeVote(voterSession, message.getId(), reply.getId())).hasStatus(HttpStatus.FOUND);
        assertReplyVotes(message.getId(), reply.getId(), 0, 0, null);
    }

    @Test
    void countsAndCurrentUsersVoteAreRenderedImmediately() throws Exception {
        var ownerSession = session(login("admin@gmail.com", "secret"));
        var message = createMessage(ownerSession);
        var reply = createReply(ownerSession, message.getId(), "IDENTIFIED");
        var voterSession = session(login("siva@gmail.com", "secret"));
        vote(voterSession, message.getId(), reply.getId(), "UPVOTE");

        var upvoted = mvc.get()
                .uri("/messages/{id}", message.getId())
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
                        "action=\"/messages/" + message.getId() + "/replies/" + reply.getId() + "/vote/remove\"");

        vote(voterSession, message.getId(), reply.getId(), "DOWNVOTE");
        var downvoted = mvc.get()
                .uri("/messages/{id}", message.getId())
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
                        "action=\"/messages/" + message.getId() + "/replies/" + reply.getId() + "/vote/remove\"");
    }

    @Test
    void userCannotVoteOnOwnReplyEvenWhenAnonymous() {
        var userSession = session(login("siva@gmail.com", "secret"));
        var message = createMessage(userSession);
        var reply = createReply(userSession, message.getId(), "ANONYMOUS");

        assertThat(vote(userSession, message.getId(), reply.getId(), "UPVOTE"))
                .hasStatusOk()
                .hasViewName("error/403");
        assertThat(mvc.get()
                        .uri("/messages/{id}", message.getId())
                        .session(userSession)
                        .exchange())
                .bodyText()
                .doesNotContain(
                        "Upvote reply", "Downvote reply", "Remove your reply upvote", "Remove your reply downvote");
        assertThat(replyVoteRepository.findByReplyIdAndVoterId(reply.getId(), 2L))
                .isEmpty();
    }

    @Test
    void userCannotVoteOnDeletedReply() {
        var ownerSession = session(login("admin@gmail.com", "secret"));
        var message = createMessage(ownerSession);
        var reply = createReply(ownerSession, message.getId(), "IDENTIFIED");
        reply.setStatus(ReplyStatus.DELETED);
        replyRepository.saveAndFlush(reply);
        var voterSession = session(login("siva@gmail.com", "secret"));

        assertThat(vote(voterSession, message.getId(), reply.getId(), "DOWNVOTE"))
                .hasStatusOk()
                .hasViewName("error/403");
        assertThat(removeVote(voterSession, message.getId(), reply.getId()))
                .hasStatusOk()
                .hasViewName("error/403");
        assertThat(replyVoteRepository.findByReplyIdAndVoterId(reply.getId(), 2L))
                .isEmpty();
    }

    @Test
    void replyMustBelongToMessageAndUserMustBeAuthenticated() {
        var ownerSession = session(login("admin@gmail.com", "secret"));
        var message = createMessage(ownerSession);
        var otherMessage = createMessage(ownerSession);
        var reply = createReply(ownerSession, message.getId(), "IDENTIFIED");
        var voterSession = session(login("siva@gmail.com", "secret"));

        assertThat(vote(voterSession, otherMessage.getId(), reply.getId(), "UPVOTE"))
                .hasStatusOk()
                .hasViewName("error/404");
        assertThat(mvc.post()
                        .uri("/messages/{m}/replies/{r}/vote", message.getId(), reply.getId())
                        .param("voteType", "UPVOTE")
                        .with(csrf())
                        .exchange())
                .hasStatus(HttpStatus.FOUND);
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

    private MessageEntity createMessage(MockHttpSession session) {
        var content = "Vote target " + UUID.randomUUID();
        mvc.post()
                .uri("/messages")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("content", content)
                .param("postingIdentity", "IDENTIFIED")
                .session(session)
                .with(csrf())
                .exchange();
        return messageRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(m -> m.getContent().equals(content))
                .findFirst()
                .orElseThrow();
    }

    private ReplyEntity createReply(MockHttpSession session, Long messageId, String identity) {
        var content = "Reply vote target " + UUID.randomUUID();
        mvc.post()
                .uri("/messages/{id}/replies", messageId)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("content", content)
                .param("postingIdentity", identity)
                .session(session)
                .with(csrf())
                .exchange();
        return replyRepository.findAllByMessageIdOrderByCreatedAtAsc(messageId).stream()
                .filter(r -> r.getContent().equals(content))
                .findFirst()
                .orElseThrow();
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

    private MvcTestResult login(String email, String password) {
        return mvc.post()
                .uri("/login")
                .param("username", email)
                .param("password", password)
                .with(csrf())
                .exchange();
    }

    private static MockHttpSession session(MvcTestResult result) {
        return (MockHttpSession) result.getMvcResult().getRequest().getSession(false);
    }
}
