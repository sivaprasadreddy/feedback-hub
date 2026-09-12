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

class ViewRepliesTests extends BaseIT {
    @Autowired
    MessageRepository messageRepository;

    @Autowired
    ReplyRepository replyRepository;

    @Test
    void repliesAppearUnderMessageWithAuthorTimestampsAndEngagementInformation() throws Exception {
        var userSession = session(login("siva@gmail.com", "secret"));
        var message = createMessage(userSession, "Discussion " + UUID.randomUUID());
        var identifiedContent = "First visible reply " + UUID.randomUUID();
        createReply(userSession, message.getId(), identifiedContent, "IDENTIFIED");
        var adminSession = session(login("admin@gmail.com", "secret"));
        var anonymousContent = "Second anonymous reply " + UUID.randomUUID();
        createReply(adminSession, message.getId(), anonymousContent, "ANONYMOUS");

        var result = mvc.get()
                .uri("/messages/{id}", message.getId())
                .session(userSession)
                .exchange();

        assertThat(result)
                .hasStatusOk()
                .hasViewName("messages/view")
                .bodyText()
                .contains(
                        "Replies",
                        "Siva",
                        identifiedContent,
                        "Anonymous",
                        anonymousContent,
                        "Created",
                        "Updated",
                        "Upvotes",
                        "Downvotes")
                .doesNotContain("Your vote", "No vote")
                .doesNotContain("Admin", "admin@gmail.com");
        var html = result.getMvcResult().getResponse().getContentAsString();
        assertThat(html.indexOf(identifiedContent)).isLessThan(html.indexOf(anonymousContent));
        assertThat(html).contains("2026-");
    }

    @Test
    void deletedReplyShowsPlaceholderWithoutExposingOriginalContent() {
        var userSession = session(login("siva@gmail.com", "secret"));
        var message = createMessage(userSession, "Deleted reply discussion " + UUID.randomUUID());
        var content = "Sensitive deleted reply " + UUID.randomUUID();
        createReply(userSession, message.getId(), content, "IDENTIFIED");
        var reply = findReply(message.getId(), content);
        reply.setStatus(ReplyStatus.DELETED);
        replyRepository.saveAndFlush(reply);

        assertThat(mvc.get()
                        .uri("/messages/{id}", message.getId())
                        .session(userSession)
                        .exchange())
                .bodyText()
                .contains(MessageService.DELETED_REPLY_CONTENT)
                .doesNotContain(content);
        assertThat(replyRepository.findById(reply.getId())).isPresent();
    }

    @Test
    void onlyDirectRepliesForCurrentMessageAreDisplayed() {
        var userSession = session(login("siva@gmail.com", "secret"));
        var firstMessage = createMessage(userSession, "First discussion " + UUID.randomUUID());
        var secondMessage = createMessage(userSession, "Second discussion " + UUID.randomUUID());
        var directReply = "Direct reply " + UUID.randomUUID();
        var otherReply = "Other message reply " + UUID.randomUUID();
        createReply(userSession, firstMessage.getId(), directReply, "IDENTIFIED");
        createReply(userSession, secondMessage.getId(), otherReply, "IDENTIFIED");

        assertThat(mvc.get()
                        .uri("/messages/{id}", firstMessage.getId())
                        .session(userSession)
                        .exchange())
                .bodyText()
                .contains(directReply)
                .doesNotContain(otherReply);
        assertThat(findReply(firstMessage.getId(), directReply).getMessage().getId())
                .isEqualTo(firstMessage.getId());
    }

    @Test
    void unauthenticatedUserCannotViewReplies() {
        assertThat(mvc.get().uri("/messages/1").exchange()).hasStatus(HttpStatus.FOUND);
    }

    private MessageEntity createMessage(MockHttpSession session, String content) {
        assertThat(mvc.post()
                        .uri("/messages")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("content", content)
                        .param("postingIdentity", "IDENTIFIED")
                        .session(session)
                        .with(csrf())
                        .exchange())
                .hasStatus(HttpStatus.FOUND);
        return messageRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(message -> message.getContent().equals(content))
                .findFirst()
                .orElseThrow();
    }

    private void createReply(MockHttpSession session, Long messageId, String content, String postingIdentity) {
        assertThat(mvc.post()
                        .uri("/messages/{id}/replies", messageId)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("content", content)
                        .param("postingIdentity", postingIdentity)
                        .session(session)
                        .with(csrf())
                        .exchange())
                .hasStatus(HttpStatus.FOUND);
    }

    private ReplyEntity findReply(Long messageId, String content) {
        return replyRepository.findAllByMessageIdOrderByCreatedAtAsc(messageId).stream()
                .filter(reply -> reply.getContent().equals(content))
                .findFirst()
                .orElseThrow();
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
