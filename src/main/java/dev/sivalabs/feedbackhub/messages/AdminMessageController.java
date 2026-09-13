package dev.sivalabs.feedbackhub.messages;

import dev.sivalabs.feedbackhub.shared.BadRequestException;
import dev.sivalabs.feedbackhub.users.AuthUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/messages")
class AdminMessageController {
    private final MessageService messageService;

    AdminMessageController(MessageService messageService) {
        this.messageService = messageService;
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

    private int parsePage(String page) {
        try {
            var pageNo = Integer.parseInt(page);
            if (pageNo < 1) {
                throw new BadRequestException("Page number must be at least 1");
            }
            return pageNo;
        } catch (NumberFormatException e) {
            throw new BadRequestException("Page number must be a positive integer");
        }
    }
}
