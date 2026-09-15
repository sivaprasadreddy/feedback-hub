package dev.sivalabs.feedbackhub.messages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import dev.sivalabs.feedbackhub.BaseIT;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ExportMessagePdfTests extends BaseIT {
    @Autowired
    MessageRepository messageRepository;

    @Test
    void adminCanExportMessageAndAllRepliesAsPdf() throws Exception {
        var userSession = session(login("siva@gmail.com", "secret"));
        var content = "PDF export message " + UUID.randomUUID();
        createMessage(userSession, content, "IDENTIFIED");
        var message = findMessage(content);
        var firstReply = "First PDF reply " + UUID.randomUUID();
        var secondReply = "Second anonymous PDF reply " + UUID.randomUUID();
        createReply(userSession, message.getId(), firstReply, "IDENTIFIED");
        createReply(userSession, message.getId(), secondReply, "ANONYMOUS");

        var result = mvc.get()
                .uri("/admin/messages/{id}/export.pdf", message.getId())
                .session(session(login("admin@gmail.com", "secret")))
                .exchange();

        assertThat(result)
                .hasStatusOk()
                .hasContentType(MediaType.APPLICATION_PDF)
                .hasHeader(
                        "Content-Disposition", "attachment; filename=\"feedback-message-" + message.getId() + ".pdf\"");
        var bytes = result.getMvcResult().getResponse().getContentAsByteArray();
        assertThat(new String(bytes, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
        assertThat(bytes).hasSizeGreaterThan(500);
        try (var document = Loader.loadPDF(bytes)) {
            var text = new PDFTextStripper().getText(document);
            assertThat(text)
                    .contains("FeedbackHub Message Export", content, "Author: Siva", "Replies (2)", firstReply)
                    .contains("Reply 1 - Siva", "Reply 2 - Anonymous", secondReply);
            assertThat(text.indexOf(firstReply)).isLessThan(text.indexOf(secondReply));
            var qaDirectory = Path.of("target", "pdf-qa");
            Files.createDirectories(qaDirectory);
            ImageIO.write(
                    new PDFRenderer(document).renderImageWithDPI(0, 120),
                    "png",
                    qaDirectory.resolve("message-export.png").toFile());
        }

        var qaDirectory = Path.of("target", "pdf-qa");
        Files.createDirectories(qaDirectory);
        Files.write(qaDirectory.resolve("message-export.pdf"), bytes);
    }

    @Test
    void exportLinkIsShownAndNonAdminsCannotExport() {
        var adminSession = session(login("admin@gmail.com", "secret"));
        var content = "PDF link message " + UUID.randomUUID();
        createMessage(adminSession, content, "IDENTIFIED");
        var message = findMessage(content);
        assertThat(mvc.get().uri("/admin/messages").session(adminSession).exchange())
                .bodyText()
                .contains("Export PDF", "/admin/messages/" + message.getId() + "/export.pdf");

        var userSession = session(login("siva@gmail.com", "secret"));
        assertThat(mvc.get()
                        .uri("/admin/messages/{id}/export.pdf", message.getId())
                        .session(userSession)
                        .exchange())
                .hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.get()
                        .uri("/admin/messages/{id}/export.pdf", message.getId())
                        .exchange())
                .hasStatus(HttpStatus.FOUND);
    }

    private void createMessage(MockHttpSession session, String content, String identity) {
        assertThat(mvc.post()
                        .uri("/messages")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("content", content)
                        .param("postingIdentity", identity)
                        .session(session)
                        .with(csrf())
                        .exchange())
                .hasStatus(HttpStatus.FOUND);
    }

    private void createReply(MockHttpSession session, Long messageId, String content, String identity) {
        assertThat(mvc.post()
                        .uri("/messages/{id}/replies", messageId)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("content", content)
                        .param("postingIdentity", identity)
                        .session(session)
                        .with(csrf())
                        .exchange())
                .hasStatus(HttpStatus.FOUND);
    }

    private MessageEntity findMessage(String content) {
        return messageRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(message -> message.getContent().equals(content))
                .findFirst()
                .orElseThrow();
    }
}
