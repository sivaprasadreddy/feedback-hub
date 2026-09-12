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

class BrowseRecentFeedTests extends BaseIT {
    @Autowired
    MessageRepository messageRepository;

    @Autowired
    MessageVoteRepository messageVoteRepository;

    @Test
    void recentIsDefaultAndMessagesAreOrderedNewestFirst() throws Exception {
        var session = session(login("siva@gmail.com", "secret"));
        var older = createMessage(session, "Older recent " + UUID.randomUUID(), "IDENTIFIED");
        var newer = createMessage(session, "Newer recent " + UUID.randomUUID(), "IDENTIFIED");

        var feed = mvc.get().uri("/").session(session).exchange();
        assertThat(feed).hasStatusOk().hasViewName("index").bodyText().contains("Recent", older, newer);
        var html = feed.getMvcResult().getResponse().getContentAsString();
        assertThat(html.indexOf(newer)).isLessThan(html.indexOf(older));
    }

    @Test
    void feedItemsShowMessageDetailsAndPersistedEngagement() throws Exception {
        var ownerSession = session(login("admin@gmail.com", "secret"));
        var content = createMessage(ownerSession, "Feed details " + UUID.randomUUID(), "IDENTIFIED");
        var message = findMessage(content);
        var voterSession = session(login("siva@gmail.com", "secret"));
        vote(voterSession, message.getId(), "UPVOTE");
        createReply(voterSession, message.getId(), "Feed reply " + UUID.randomUUID());

        var feed = mvc.get().uri("/").session(voterSession).exchange();
        assertThat(feed)
                .bodyText()
                .contains("Admin", content, "Remove your upvote", "1", "Downvote message", "0", "Replies", "1")
                .doesNotContain("Your vote");
        assertThat(feed.getMvcResult().getResponse().getContentAsString())
                .contains(
                        "text-emerald-600",
                        "title=\"Remove your upvote\"",
                        "hx-target=\"closest .message-votes\"",
                        "name=\"returnToHome\" value=\"true\"");
    }

    @Test
    void downvoteIsIndicatedByHighlightedDownvoteIcon() throws Exception {
        var ownerSession = session(login("admin@gmail.com", "secret"));
        var content = createMessage(ownerSession, "Downvoted feed item " + UUID.randomUUID(), "IDENTIFIED");
        var voterSession = session(login("siva@gmail.com", "secret"));
        vote(voterSession, findMessage(content).getId(), "DOWNVOTE");

        var feed = mvc.get().uri("/").session(voterSession).exchange();
        assertThat(feed).bodyText().contains("Remove your downvote").doesNotContain("Your vote");
        assertThat(feed.getMvcResult().getResponse().getContentAsString())
                .contains("text-amber-600", "title=\"Remove your downvote\"");
    }

    @Test
    void userCanVoteSwitchAndRemoveVoteFromHomePage() throws Exception {
        var ownerSession = session(login("admin@gmail.com", "secret"));
        var content = createMessage(ownerSession, "Home voting " + UUID.randomUUID(), "IDENTIFIED");
        var messageId = findMessage(content).getId();
        var voterSession = session(login("siva@gmail.com", "secret"));

        assertThat(voteFromHome(voterSession, messageId, "UPVOTE", false))
                .hasStatus(HttpStatus.FOUND)
                .hasRedirectedUrl("/");
        assertThat(messageVoteRepository.findByMessageIdAndVoterUserId(messageId, 2L))
                .get()
                .extracting(MessageVoteEntity::getVoteType)
                .isEqualTo(VoteType.UPVOTE);

        voteFromHome(voterSession, messageId, "DOWNVOTE", false);
        assertThat(messageVoteRepository.findByMessageIdAndVoterUserId(messageId, 2L))
                .get()
                .extracting(MessageVoteEntity::getVoteType)
                .isEqualTo(VoteType.DOWNVOTE);

        assertThat(voteFromHome(voterSession, messageId, "DOWNVOTE", true))
                .hasStatus(HttpStatus.FOUND)
                .hasRedirectedUrl("/");
        assertThat(messageVoteRepository.findByMessageIdAndVoterUserId(messageId, 2L))
                .isEmpty();
    }

    @Test
    void anonymousAndDeletedMessagesDoNotExposeProtectedContentOrIdentity() {
        var adminSession = session(login("admin@gmail.com", "secret"));
        var anonymousContent = createMessage(adminSession, "Anonymous feed " + UUID.randomUUID(), "ANONYMOUS");
        var deletedContent = createMessage(adminSession, "Deleted feed " + UUID.randomUUID(), "IDENTIFIED");
        var deleted = findMessage(deletedContent);
        deleted.setStatus(MessageStatus.DELETED);
        messageRepository.saveAndFlush(deleted);

        var userSession = session(login("siva@gmail.com", "secret"));
        assertThat(mvc.get().uri("/").session(userSession).exchange())
                .bodyText()
                .contains("Anonymous", anonymousContent, MessageService.DELETED_CONTENT)
                .doesNotContain(deletedContent, "admin@gmail.com");
    }

    @Test
    void unauthenticatedUserCannotBrowseRecentFeed() {
        assertThat(mvc.get().uri("/").exchange()).hasStatus(HttpStatus.FOUND);
    }

    private String createMessage(MockHttpSession session, String content, String identity) {
        assertThat(mvc.post()
                        .uri("/messages")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("content", content)
                        .param("postingIdentity", identity)
                        .session(session)
                        .with(csrf())
                        .exchange())
                .hasStatus(HttpStatus.FOUND);
        return content;
    }

    private MessageEntity findMessage(String content) {
        return messageRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(message -> message.getContent().equals(content))
                .findFirst()
                .orElseThrow();
    }

    private void vote(MockHttpSession session, Long messageId, String voteType) {
        mvc.post()
                .uri("/messages/{id}/vote", messageId)
                .param("voteType", voteType)
                .session(session)
                .with(csrf())
                .exchange();
    }

    private MvcTestResult voteFromHome(MockHttpSession session, Long messageId, String voteType, boolean remove) {
        var request = mvc.post()
                .uri(remove ? "/messages/{id}/vote/remove" : "/messages/{id}/vote", messageId)
                .param("voteType", voteType)
                .param("returnToHome", "true")
                .session(session)
                .with(csrf());
        return request.exchange();
    }

    private void createReply(MockHttpSession session, Long messageId, String content) {
        mvc.post()
                .uri("/messages/{id}/replies", messageId)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("content", content)
                .param("postingIdentity", "IDENTIFIED")
                .session(session)
                .with(csrf())
                .exchange();
    }
}
