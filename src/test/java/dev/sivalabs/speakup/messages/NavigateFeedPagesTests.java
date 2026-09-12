package dev.sivalabs.speakup.messages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import dev.sivalabs.speakup.BaseIT;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class NavigateFeedPagesTests extends BaseIT {
    @Autowired
    MessageRepository messageRepository;

    @Test
    void recentFeedReturnsTenMessagesAndNavigatesWithoutDuplicates() throws Exception {
        var session = session(login("siva@gmail.com", "secret"));
        var contents = createMessages(session, 12, "Recent page");

        var firstPage = mvc.get().uri("/?feed=RECENT&page=1").session(session).exchange();
        var secondPage = mvc.get().uri("/?feed=RECENT&page=2").session(session).exchange();

        assertThat(firstPage).hasStatusOk().bodyText().contains("Next", "Page 1");
        assertThat(secondPage).hasStatusOk().bodyText().contains("Previous", "Page 2");
        assertPageTraversal(contents, firstPage, secondPage, 2);
    }

    @Test
    void popularFeedKeepsPopularityAndRecencyOrderingAcrossPages() throws Exception {
        var ownerSession = session(login("admin@gmail.com", "secret"));
        var voterSession = session(login("siva@gmail.com", "secret"));
        var contents = createMessages(ownerSession, 11, "Popular page");
        contents.forEach(content -> vote(voterSession, findMessage(content).getId()));

        var firstPage =
                mvc.get().uri("/?feed=POPULAR&page=1").session(voterSession).exchange();
        var secondPage =
                mvc.get().uri("/?feed=POPULAR&page=2").session(voterSession).exchange();

        assertThat(firstPage).hasStatusOk().bodyText().contains("Popular", "Next");
        assertThat(secondPage).hasStatusOk().bodyText().contains("Popular", "Previous");
        assertPageTraversal(contents, firstPage, secondPage, 1);
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

    private void assertPageTraversal(
            List<String> contents, MvcTestResult firstPage, MvcTestResult secondPage, int expectedSecondPageFixtures)
            throws Exception {
        var firstBody = firstPage.getMvcResult().getResponse().getContentAsString();
        var secondBody = secondPage.getMvcResult().getResponse().getContentAsString();
        var firstPageContents = contents.stream().filter(firstBody::contains).toList();
        var secondPageContents = contents.stream().filter(secondBody::contains).toList();
        assertThat(firstPageContents).hasSize(MessageService.FEED_PAGE_SIZE);
        assertThat(secondPageContents).hasSize(expectedSecondPageFixtures);
        assertThat(firstPageContents).doesNotContainAnyElementsOf(secondPageContents);
        assertThat(firstPageContents)
                .containsExactlyElementsOf(contents.reversed().subList(0, 10).reversed());
    }

    private List<String> createMessages(MockHttpSession session, int count, String label) {
        var contents = new ArrayList<String>();
        for (int i = 0; i < count; i++) {
            var content = label + " " + i + " " + UUID.randomUUID();
            mvc.post()
                    .uri("/messages")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("content", content)
                    .param("postingIdentity", "IDENTIFIED")
                    .session(session)
                    .with(csrf())
                    .exchange();
            contents.add(content);
        }
        return contents;
    }

    private MessageEntity findMessage(String content) {
        return messageRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(message -> message.getContent().equals(content))
                .findFirst()
                .orElseThrow();
    }

    private void vote(MockHttpSession session, Long messageId) {
        mvc.post()
                .uri("/messages/{id}/vote", messageId)
                .param("voteType", "UPVOTE")
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
