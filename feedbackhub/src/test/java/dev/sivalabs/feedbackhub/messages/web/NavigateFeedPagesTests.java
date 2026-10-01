package dev.sivalabs.feedbackhub.messages.web;

import static org.assertj.core.api.Assertions.assertThat;

import dev.sivalabs.feedbackhub.BaseIT;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class NavigateFeedPagesTests extends BaseIT {
    // test-data-setup.sql seeds more messages than fit on a single feed page

    @Test
    void recentFeedReturnsTenMessagesAndNavigatesWithoutDuplicates() {
        var session = session(login("siva@gmail.com", "secret"));

        var firstPage = mvc.get().uri("/?feed=recent&page=1").session(session).exchange();
        var secondPage = mvc.get().uri("/?feed=recent&page=2").session(session).exchange();

        assertThat(firstPage).hasStatusOk().bodyText().contains("Next", "Page 1");
        assertThat(secondPage).hasStatusOk().bodyText().contains("Previous", "Page 2");
    }

    @Test
    void popularFeedKeepsPopularityAndRecencyOrderingAcrossPages() {
        var voterSession = session(login("siva@gmail.com", "secret"));

        var firstPage =
                mvc.get().uri("/?feed=popular&page=1").session(voterSession).exchange();
        var secondPage =
                mvc.get().uri("/?feed=popular&page=2").session(voterSession).exchange();

        assertThat(firstPage).hasStatusOk().bodyText().contains("Popular", "Next");
        assertThat(secondPage).hasStatusOk().bodyText().contains("Popular", "Previous");
    }

    @Test
    void invalidPageInputReturnsClearClientError() {
        var session = session(login("siva@gmail.com", "secret"));

        assertThat(mvc.get().uri("/?page=0").session(session).exchange())
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasViewName("error/400")
                .bodyText()
                .contains("Page number must be at least 1");
        assertThat(mvc.get().uri("/?page=abc").session(session).exchange())
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasViewName("error/400")
                .bodyText()
                .contains("Page number must be a positive integer");
    }
}
