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
                .contains("Admin", content, "Upvotes, your vote", "1", "Downvotes", "0", "Replies", "1")
                .doesNotContain("Your vote", "UPVOTE");
        assertThat(feed.getMvcResult().getResponse().getContentAsString())
                .contains("text-emerald-600", "title=\"Upvotes — your vote\"");
    }

    @Test
    void downvoteIsIndicatedByHighlightedDownvoteIcon() throws Exception {
        var ownerSession = session(login("admin@gmail.com", "secret"));
        var content = createMessage(ownerSession, "Downvoted feed item " + UUID.randomUUID(), "IDENTIFIED");
        var voterSession = session(login("siva@gmail.com", "secret"));
        vote(voterSession, findMessage(content).getId(), "DOWNVOTE");

        var feed = mvc.get().uri("/").session(voterSession).exchange();
        assertThat(feed).bodyText().contains("Downvotes, your vote").doesNotContain("Your vote", "DOWNVOTE");
        assertThat(feed.getMvcResult().getResponse().getContentAsString())
                .contains("text-amber-600", "title=\"Downvotes — your vote\"");
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
