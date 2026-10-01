package dev.sivalabs.feedbackhub.messages.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import dev.sivalabs.feedbackhub.BaseIT;
import dev.sivalabs.feedbackhub.messages.domain.MessageService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

class BrowseRecentFeedTests extends BaseIT {
    private static final String OLDER_RECENT = "Older recent seed";
    private static final String NEWER_RECENT = "Newer recent seed";
    private static final String FEED_DETAILS = "Feed details seed";
    private static final long FEED_DETAILS_ID = 1103L;
    private static final String DOWNVOTED_FEED_ITEM = "Downvoted feed seed";
    private static final long HOME_VOTING_ID = 1105L;
    private static final String ANONYMOUS_FEED = "Anonymous feed seed";
    private static final String DELETED_FEED = "Deleted feed seed";

    @Test
    void recentIsDefaultAndMessagesAreOrderedNewestFirst() throws Exception {
        var session = session(login("siva@gmail.com", "secret"));

        var feed = mvc.get().uri("/").session(session).exchange();
        assertThat(feed).hasStatusOk().hasViewName("index").bodyText().contains("Recent", OLDER_RECENT, NEWER_RECENT);
        var html = feed.getMvcResult().getResponse().getContentAsString();
        assertThat(html.indexOf(NEWER_RECENT)).isLessThan(html.indexOf(OLDER_RECENT));
    }

    @Test
    void feedItemsShowMessageDetailsAndPersistedEngagement() throws Exception {
        var voterSession = session(login("siva@gmail.com", "secret"));

        var feed = mvc.get().uri("/").session(voterSession).exchange();
        assertThat(feed)
                .bodyText()
                .contains(
                        "Admin",
                        FEED_DETAILS,
                        "Remove your upvote",
                        "1",
                        "Downvote message",
                        "0",
                        "Replies",
                        "1",
                        "View")
                .doesNotContain("Your vote");
        assertThat(feed.getMvcResult().getResponse().getContentAsString())
                .contains(
                        "text-emerald-600",
                        "title=\"Remove your upvote\"",
                        "hx-target=\"closest .message-votes\"",
                        "name=\"returnToHome\" value=\"true\"",
                        "href=\"/messages/" + FEED_DETAILS_ID + "\"");
    }

    @Test
    void downvoteIsIndicatedByHighlightedDownvoteIcon() throws Exception {
        var voterSession = session(login("siva@gmail.com", "secret"));

        var feed = mvc.get().uri("/").session(voterSession).exchange();
        assertThat(feed)
                .bodyText()
                .contains(DOWNVOTED_FEED_ITEM, "Remove your downvote")
                .doesNotContain("Your vote");
        assertThat(feed.getMvcResult().getResponse().getContentAsString())
                .contains("text-amber-600", "title=\"Remove your downvote\"");
    }

    @Test
    void userCanVoteSwitchAndRemoveVoteFromHomePage() throws Exception {
        var voterSession = session(login("siva@gmail.com", "secret"));

        assertThat(voteFromHome(voterSession, HOME_VOTING_ID, "UPVOTE", false))
                .hasStatus(HttpStatus.FOUND)
                .hasRedirectedUrl("/");
        assertThat(findMessageVote(HOME_VOTING_ID, 2L)).hasValue("UPVOTE");

        voteFromHome(voterSession, HOME_VOTING_ID, "DOWNVOTE", false);
        assertThat(findMessageVote(HOME_VOTING_ID, 2L)).hasValue("DOWNVOTE");

        assertThat(voteFromHome(voterSession, HOME_VOTING_ID, "DOWNVOTE", true))
                .hasStatus(HttpStatus.FOUND)
                .hasRedirectedUrl("/");
        assertThat(findMessageVote(HOME_VOTING_ID, 2L)).isEmpty();
    }

    @Test
    void anonymousAndDeletedMessagesDoNotExposeProtectedContentOrIdentity() {
        var userSession = session(login("siva@gmail.com", "secret"));

        assertThat(mvc.get().uri("/").session(userSession).exchange())
                .bodyText()
                .contains("Anonymous", ANONYMOUS_FEED, MessageService.DELETED_CONTENT)
                .doesNotContain(DELETED_FEED, "admin@gmail.com");
    }

    @Test
    void unauthenticatedUserCannotBrowseRecentFeed() {
        assertThat(mvc.get().uri("/").exchange()).hasStatus(HttpStatus.FOUND);
    }

    private Optional<String> findMessageVote(long messageId, long userId) {
        return jdbcClient
                .sql("select vote_type from message_votes where message_id = ? and user_id = ?")
                .params(messageId, userId)
                .query(String.class)
                .optional();
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
}
