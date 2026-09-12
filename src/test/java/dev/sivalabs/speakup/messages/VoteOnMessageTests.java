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

class VoteOnMessageTests extends BaseIT {
    @Autowired
    MessageRepository messageRepository;

    @Autowired
    MessageVoteRepository messageVoteRepository;

    @Autowired
    MessageService messageService;

    @Test
    void userCanUpvoteAndDownvoteAnotherUsersMessage() {
        var ownerSession = session(login("admin@gmail.com", "secret"));
        var message = createMessage(ownerSession, "ANONYMOUS");
        var voterSession = session(login("siva@gmail.com", "secret"));

        assertThat(vote(voterSession, message.getId(), "UPVOTE"))
                .hasStatus(HttpStatus.FOUND)
                .hasRedirectedUrl("/messages/" + message.getId());
        assertMessageVotes(message.getId(), 1, 0, "UPVOTE");

        assertThat(vote(voterSession, message.getId(), "DOWNVOTE")).hasStatus(HttpStatus.FOUND);
        assertMessageVotes(message.getId(), 0, 1, "DOWNVOTE");
        assertThat(messageVoteRepository.findAll())
                .filteredOn(vote -> vote.getMessage().getId().equals(message.getId()))
                .hasSize(1);
    }

    @Test
    void userCanRemoveExistingVote() {
        var ownerSession = session(login("admin@gmail.com", "secret"));
        var message = createMessage(ownerSession, "ANONYMOUS");
        var voterSession = session(login("siva@gmail.com", "secret"));
        vote(voterSession, message.getId(), "UPVOTE");

        assertThat(removeVote(voterSession, message.getId()))
                .hasStatus(HttpStatus.FOUND)
                .hasRedirectedUrl("/messages/" + message.getId());
        assertMessageVotes(message.getId(), 0, 0, null);
        assertThat(messageVoteRepository.findByMessageIdAndVoterUserId(message.getId(), 2L))
                .isEmpty();
    }

    @Test
    void countsAndCurrentUsersVoteAreRenderedImmediately() throws Exception {
        var ownerSession = session(login("admin@gmail.com", "secret"));
        var message = createMessage(ownerSession, "ANONYMOUS");
        var voterSession = session(login("siva@gmail.com", "secret"));
        vote(voterSession, message.getId(), "UPVOTE");

        var upvoted = mvc.get()
                .uri("/messages/{messageId}", message.getId())
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
                        "action=\"/messages/" + message.getId() + "/vote/remove\"");

        vote(voterSession, message.getId(), "DOWNVOTE");
        var downvoted = mvc.get()
                .uri("/messages/{messageId}", message.getId())
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
                        "action=\"/messages/" + message.getId() + "/vote/remove\"");
    }

    @Test
    void htmxVoteReturnsOnlyUpdatedVoteControls() throws Exception {
        var ownerSession = session(login("admin@gmail.com", "secret"));
        var message = createMessage(ownerSession, "ANONYMOUS");
        var voterSession = session(login("siva@gmail.com", "secret"));

        var response = mvc.post()
                .uri("/messages/{messageId}/vote", message.getId())
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
                .uri("/messages/{messageId}/vote/remove", message.getId())
                .header("HX-Request", "true")
                .session(voterSession)
                .with(csrf())
                .exchange();
        assertThat(removeResponse).hasStatusOk().bodyText().contains("Upvote message", "Downvote message");
        assertMessageVotes(message.getId(), 0, 0, null);
    }

    @Test
    void userCannotVoteOnOwnMessageEvenWhenAnonymous() {
        var userSession = session(login("siva@gmail.com", "secret"));
        var message = createMessage(userSession, "ANONYMOUS");

        assertThat(vote(userSession, message.getId(), "UPVOTE"))
                .hasStatus(HttpStatus.FORBIDDEN)
                .hasViewName("error/403");
        assertThat(mvc.get()
                        .uri("/messages/{messageId}", message.getId())
                        .session(userSession)
                        .exchange())
                .bodyText()
                .doesNotContain("Upvote message", "Downvote message", "Remove your upvote", "Remove your downvote");
        assertThat(messageVoteRepository.findByMessageIdAndVoterUserId(message.getId(), 2L))
                .isEmpty();
    }

    @Test
    void userCannotVoteOnDeletedMessage() {
        var ownerSession = session(login("admin@gmail.com", "secret"));
        var message = createMessage(ownerSession, "ANONYMOUS");
        message.setStatus(MessageStatus.DELETED);
        messageRepository.saveAndFlush(message);
        var voterSession = session(login("siva@gmail.com", "secret"));

        assertThat(vote(voterSession, message.getId(), "DOWNVOTE"))
                .hasStatus(HttpStatus.FORBIDDEN)
                .hasViewName("error/403");
        assertThat(removeVote(voterSession, message.getId()))
                .hasStatus(HttpStatus.FORBIDDEN)
                .hasViewName("error/403");
        assertThat(messageVoteRepository.findByMessageIdAndVoterUserId(message.getId(), 2L))
                .isEmpty();
    }

    @Test
    void unauthenticatedUserCannotVote() {
        assertThat(mvc.post()
                        .uri("/messages/1/vote")
                        .param("voteType", "UPVOTE")
                        .with(csrf())
                        .exchange())
                .hasStatus(HttpStatus.FOUND);
    }

    private void assertMessageVotes(Long messageId, long upvotes, long downvotes, String currentUserVote) {
        var details = messageService.findMessage(messageId, 2L);
        assertThat(details.upvoteCount()).isEqualTo(upvotes);
        assertThat(details.downvoteCount()).isEqualTo(downvotes);
        assertThat(details.currentUserVote()).isEqualTo(currentUserVote);
    }

    private MessageEntity createMessage(MockHttpSession session, String postingIdentity) {
        var content = "Message for voting " + UUID.randomUUID();
        assertThat(mvc.post()
                        .uri("/messages")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("content", content)
                        .param("postingIdentity", postingIdentity)
                        .session(session)
                        .with(csrf())
                        .exchange())
                .hasStatus(HttpStatus.FOUND);
        return messageRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(message -> message.getContent().equals(content))
                .findFirst()
                .orElseThrow();
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
