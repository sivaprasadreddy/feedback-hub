package dev.sivalabs.feedbackhub.messages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import dev.sivalabs.feedbackhub.BaseIT;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

class BrowsePopularFeedTests extends BaseIT {
    @Autowired
    MessageRepository messageRepository;

    @Test
    void userCanSwitchFromRecentToPopularOrderedByPersistedUpvotes() throws Exception {
        var adminSession = session(login("admin@gmail.com", "secret"));
        var fewerVotes = createMessage(adminSession, "Fewer upvotes " + UUID.randomUUID(), "IDENTIFIED");
        var moreVotes = createMessage(adminSession, "More upvotes " + UUID.randomUUID(), "IDENTIFIED");
        var voterSession = session(login("siva@gmail.com", "secret"));
        vote(voterSession, findMessage(moreVotes).getId(), "UPVOTE");

        var feed = mvc.get().uri("/?feed=popular").session(voterSession).exchange();
        assertThat(feed)
                .hasStatusOk()
                .hasViewName("index")
                .bodyText()
                .contains("Popular", "Recent", fewerVotes, moreVotes);
        var html = feed.getMvcResult().getResponse().getContentAsString();
        assertThat(html.indexOf(moreVotes)).isLessThan(html.indexOf(fewerVotes));
    }

    @Test
    void equalUpvoteCountsAreOrderedNewestFirst() throws Exception {
        var adminSession = session(login("admin@gmail.com", "secret"));
        var older = createMessage(adminSession, "Older equal score " + UUID.randomUUID(), "IDENTIFIED");
        var newer = createMessage(adminSession, "Newer equal score " + UUID.randomUUID(), "IDENTIFIED");

        var feed = mvc.get().uri("/?feed=popular").session(adminSession).exchange();
        var html = feed.getMvcResult().getResponse().getContentAsString();
        assertThat(html.indexOf(newer)).isLessThan(html.indexOf(older));
    }

    @Test
    void downvotesDoNotAffectPopularityRanking() throws Exception {
        var adminSession = session(login("admin@gmail.com", "secret"));
        var unvotedOlder = createMessage(adminSession, "Unvoted older " + UUID.randomUUID(), "IDENTIFIED");
        var downvotedNewer = createMessage(adminSession, "Downvoted newer " + UUID.randomUUID(), "IDENTIFIED");
        var voterSession = session(login("siva@gmail.com", "secret"));
        vote(voterSession, findMessage(downvotedNewer).getId(), "DOWNVOTE");

        var feed = mvc.get().uri("/?feed=popular").session(voterSession).exchange();
        var html = feed.getMvcResult().getResponse().getContentAsString();
        assertThat(html.indexOf(downvotedNewer)).isLessThan(html.indexOf(unvotedOlder));
    }

    @Test
    void popularFeedDoesNotExposeAnonymousIdentity() {
        var adminSession = session(login("admin@gmail.com", "secret"));
        var content = createMessage(adminSession, "Anonymous popular " + UUID.randomUUID(), "ANONYMOUS");
        var userSession = session(login("siva@gmail.com", "secret"));

        assertThat(mvc.get().uri("/?feed=popular").session(userSession).exchange())
                .bodyText()
                .contains("Anonymous", content)
                .doesNotContain("admin@gmail.com");
    }

    private String createMessage(MockHttpSession session, String content, String identity) {
        mvc.post()
                .uri("/messages")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("content", content)
                .param("postingIdentity", identity)
                .session(session)
                .with(csrf())
                .exchange();
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
}
