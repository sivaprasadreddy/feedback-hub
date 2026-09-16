package dev.sivalabs.feedbackhub.messages;

import static dev.sivalabs.feedbackhub.shared.PaginationUtils.parsePage;

import dev.sivalabs.feedbackhub.users.AuthUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/messages")
class AdminMessageController {
    private static final Logger LOG = LoggerFactory.getLogger(AdminMessageController.class);
    private final MessageService messageService;
    private final MessagePdfExportService messagePdfExporter;

    AdminMessageController(MessageService messageService, MessagePdfExportService messagePdfExporter) {
        this.messageService = messageService;
        this.messagePdfExporter = messagePdfExporter;
    }

    @GetMapping("/{messageId}/export.pdf")
    ResponseEntity<byte[]> exportMessage(@PathVariable Long messageId) {
        var pdf = messagePdfExporter.export(messageService.findMessageExportData(messageId));
        var disposition = ContentDisposition.attachment()
                .filename("feedback-message-" + messageId + ".pdf")
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentLength(pdf.length)
                .body(pdf);
    }

    @GetMapping
    String listMessages(@RequestParam(defaultValue = "1") String page, Model model) {
        var messagesPage = messageService.findMessagesForAdmin(parsePage(page));
        model.addAttribute("page", messagesPage);
        model.addAttribute("messages", messagesPage.data());
        return "admin/messages";
    }

    @PostMapping("/{messageId}/delete")
    String deleteMessage(@PathVariable Long messageId, RedirectAttributes redirectAttributes) {
        messageService.deleteMessageAsAdmin(messageId, AuthUtils.getCurrentUserIdOrThrow());
        redirectAttributes.addFlashAttribute("successMessage", "Message deleted successfully.");
        return "redirect:/admin/messages";
    }

    @PostMapping("/{messageId}/analyze")
    String analyzeMessage(
            @PathVariable Long messageId,
            @RequestHeader(name = "HX-Request", defaultValue = "false") boolean htmxRequest,
            Model model,
            RedirectAttributes redirectAttributes) {
        var previousMessage = messageService.findMessageForAdmin(messageId);
        try {
            messageService.analyzeMessage(messageId);
        } catch (RuntimeException e) {
            LOG.error("Failed to analyze message with id {}", messageId, e);
            var errorMessage = "Something went wrong while analyzing this message. Please try again.";
            if (htmxRequest) {
                model.addAttribute("message", previousMessage);
                model.addAttribute("analysisError", errorMessage);
                return "fragments/admin-message :: message(message=${message})";
            }
            redirectAttributes.addFlashAttribute("errorMessage", errorMessage);
            return "redirect:/admin/messages";
        }
        if (htmxRequest) {
            model.addAttribute("message", messageService.findMessageForAdmin(messageId));
            return "fragments/admin-message :: message(message=${message})";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Message analyzed successfully.");
        return "redirect:/admin/messages";
    }
}
