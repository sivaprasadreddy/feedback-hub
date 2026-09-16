package dev.sivalabs.feedbackhub.messages;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

@Service
class MessagePdfExportService {
    private static final float MARGIN = 54;
    private static final float BODY_SIZE = 10;
    private static final float LINE_HEIGHT = 14;
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm z").withZone(ZoneId.systemDefault());
    private static final PDFont REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDFont BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    byte[] export(MessageExportData data) {
        try (var document = new PDDocument();
                var output = new ByteArrayOutputStream()) {
            var writer = new PdfWriter(document);
            writer.heading("FeedbackHub Message Export");
            writer.text("Message #" + data.message().id(), BOLD, 13, 20);
            writer.labelValue("Author", data.message().visibleAuthor());
            writer.labelValue("Created", formatDate(data.message().createdAt()));
            writer.labelValue("Status", data.message().deleted() ? "Deleted" : "Active");
            if (data.message().analyzed()) {
                writer.labelValue("Sentiment", data.message().sentiment().getDisplayName());
                writer.labelValue("Topics", String.join(", ", data.message().topics()));
            }
            writer.paragraph(data.message().content());

            writer.section("Replies (" + data.replies().size() + ")");
            if (data.replies().isEmpty()) {
                writer.paragraph("No replies.");
            }
            for (int index = 0; index < data.replies().size(); index++) {
                var reply = data.replies().get(index);
                writer.text("Reply " + (index + 1) + " - " + reply.visibleAuthor(), BOLD, 11, 5);
                writer.text(
                        formatDate(reply.createdAt()) + " | " + (reply.deleted() ? "Deleted" : "Active")
                                + (reply.spam() ? " | Marked as spam" : ""),
                        REGULAR,
                        9,
                        8);
                writer.paragraph(reply.content());
            }
            writer.finish();
            document.save(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Could not generate message PDF", ex);
        }
    }

    private static String formatDate(java.time.Instant instant) {
        return instant == null ? "" : DATE_FORMAT.format(instant);
    }

    private static final class PdfWriter {
        private final PDDocument document;
        private PDPage page;
        private PDPageContentStream stream;
        private float y;
        private int pageNumber;

        PdfWriter(PDDocument document) throws IOException {
            this.document = document;
            newPage();
        }

        void heading(String value) throws IOException {
            text(value, BOLD, 20, 8);
            text("Exported from FeedbackHub", REGULAR, 9, 24);
        }

        void section(String value) throws IOException {
            ensureSpace(36);
            text(value, BOLD, 15, 14);
        }

        void labelValue(String label, String value) throws IOException {
            text(label + ": " + value, REGULAR, BODY_SIZE, 3);
        }

        void paragraph(String value) throws IOException {
            for (var line : wrap(value, REGULAR, BODY_SIZE, PDRectangle.A4.getWidth() - (2 * MARGIN))) {
                text(line, REGULAR, BODY_SIZE, 0);
            }
            y -= 12;
        }

        void text(String value, PDFont font, float fontSize, float spaceAfter) throws IOException {
            ensureSpace(LINE_HEIGHT + spaceAfter);
            stream.beginText();
            stream.setFont(font, fontSize);
            stream.newLineAtOffset(MARGIN, y);
            stream.showText(sanitize(value));
            stream.endText();
            y -= LINE_HEIGHT + spaceAfter;
        }

        void finish() throws IOException {
            closePage();
        }

        private void ensureSpace(float required) throws IOException {
            if (y - required < MARGIN) {
                closePage();
                newPage();
            }
        }

        private void newPage() throws IOException {
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            y = page.getMediaBox().getHeight() - MARGIN;
            pageNumber++;
            text("FeedbackHub", BOLD, 9, 12);
        }

        private void closePage() throws IOException {
            stream.beginText();
            stream.setFont(REGULAR, 8);
            stream.newLineAtOffset(page.getMediaBox().getWidth() - MARGIN - 45, 28);
            stream.showText("Page " + pageNumber);
            stream.endText();
            stream.close();
        }

        private static List<String> wrap(String input, PDFont font, float size, float maxWidth) throws IOException {
            var lines = new ArrayList<String>();
            for (var sourceLine : input.replace("\r", "").split("\n", -1)) {
                var current = new StringBuilder();
                for (var word : sourceLine.split("\\s+")) {
                    var candidate = current.isEmpty() ? word : current + " " + word;
                    if (!current.isEmpty() && font.getStringWidth(sanitize(candidate)) / 1000 * size > maxWidth) {
                        lines.add(current.toString());
                        current.setLength(0);
                    }
                    if (!current.isEmpty()) current.append(' ');
                    current.append(word);
                }
                lines.add(current.toString());
            }
            return lines;
        }

        private static String sanitize(String value) {
            return value.replaceAll("[^\\x20-\\x7E]", "?");
        }
    }
}
