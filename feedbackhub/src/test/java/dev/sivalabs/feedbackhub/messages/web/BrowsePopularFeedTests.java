package dev.sivalabs.feedbackhub.messages.web;

import static org.assertj.core.api.Assertions.assertThat;

import dev.sivalabs.feedbackhub.BaseIT;
import org.junit.jupiter.api.Test;

class BrowsePopularFeedTests extends BaseIT {
    private static final String FEWER_VOTES = "Popular fewer upvotes seed";
    private static final String MORE_VOTES = "Popular more upvotes seed";
    private static final String TIE_OLDER = "Popular tie older seed";
    private static final String TIE_NEWER = "Popular tie newer seed";
    private static final String NO_DOWNVOTES_OLDER = "Popular no downvotes older seed";
    private static final String DOWNVOTED_NEWER = "Popular downvoted newer seed";
    private static final String ANONYMOUS_POPULAR = "Anonymous popular seed";

    @Test
    void userCanSwitchFromRecentToPopularOrderedByPersistedUpvotes() throws Exception {
        var voterSession = session(login("siva@gmail.com", "secret"));

        var feed = mvc.get().uri("/?feed=popular").session(voterSession).exchange();
        assertThat(feed)
                .hasStatusOk()
                .hasViewName("index")
                .bodyText()
                .contains("Popular", "Recent", FEWER_VOTES, MORE_VOTES);
        var html = feed.getMvcResult().getResponse().getContentAsString();
        assertThat(html.indexOf(MORE_VOTES)).isLessThan(html.indexOf(FEWER_VOTES));
    }

    @Test
    void equalUpvoteCountsAreOrderedNewestFirst() throws Exception {
        var adminSession = session(login("admin@gmail.com", "secret"));

        var feed = mvc.get().uri("/?feed=popular").session(adminSession).exchange();
        assertThat(feed).bodyText().contains(TIE_OLDER, TIE_NEWER);
        var html = feed.getMvcResult().getResponse().getContentAsString();
        assertThat(html.indexOf(TIE_NEWER)).isLessThan(html.indexOf(TIE_OLDER));
    }

    @Test
    void downvotesDoNotAffectPopularityRanking() throws Exception {
        var voterSession = session(login("siva@gmail.com", "secret"));

        var feed = mvc.get().uri("/?feed=popular").session(voterSession).exchange();
        assertThat(feed).bodyText().contains(NO_DOWNVOTES_OLDER, DOWNVOTED_NEWER);
        var html = feed.getMvcResult().getResponse().getContentAsString();
        assertThat(html.indexOf(DOWNVOTED_NEWER)).isLessThan(html.indexOf(NO_DOWNVOTES_OLDER));
    }

    @Test
    void popularFeedDoesNotExposeAnonymousIdentity() {
        var userSession = session(login("siva@gmail.com", "secret"));

        assertThat(mvc.get().uri("/?feed=popular").session(userSession).exchange())
                .bodyText()
                .contains("Anonymous", ANONYMOUS_POPULAR)
                .doesNotContain("admin@gmail.com");
    }
}
