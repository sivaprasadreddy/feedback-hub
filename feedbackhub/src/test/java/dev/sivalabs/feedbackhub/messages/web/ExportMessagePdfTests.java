package dev.sivalabs.feedbackhub.messages.web;

import static org.assertj.core.api.Assertions.assertThat;

import dev.sivalabs.feedbackhub.BaseIT;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

class ExportMessagePdfTests extends BaseIT {
    private static final long EXPORT_MESSAGE_ID = 2031L;
    private static final long LINK_MESSAGE_ID = 2032L;

    @Test
    void adminCanExportMessageAndAllRepliesAsPdf() throws Exception {
        var content = "PDF export seed message 2031";
        var firstReply = "First PDF seed reply 2031";
        var secondReply = "Second anonymous PDF seed reply 2032";

        var result = mvc.get()
                .uri("/admin/messages/{id}/export.pdf", EXPORT_MESSAGE_ID)
                .session(session(login("admin@gmail.com", "secret")))
                .exchange();

        assertThat(result)
                .hasStatusOk()
                .hasContentType(MediaType.APPLICATION_PDF)
                .hasHeader(
                        "Content-Disposition",
                        "attachment; filename=\"feedback-message-" + EXPORT_MESSAGE_ID + ".pdf\"");
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
        assertThat(mvc.get().uri("/admin/messages").session(adminSession).exchange())
                .bodyText()
                .contains("Export PDF", "/admin/messages/" + LINK_MESSAGE_ID + "/export.pdf");

        var userSession = session(login("siva@gmail.com", "secret"));
        assertThat(mvc.get()
                        .uri("/admin/messages/{id}/export.pdf", LINK_MESSAGE_ID)
                        .session(userSession)
                        .exchange())
                .hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.get()
                        .uri("/admin/messages/{id}/export.pdf", LINK_MESSAGE_ID)
                        .exchange())
                .hasStatus(HttpStatus.FOUND);
    }
}
