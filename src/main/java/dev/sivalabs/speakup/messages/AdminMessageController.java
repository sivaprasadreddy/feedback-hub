package dev.sivalabs.speakup.messages;

import dev.sivalabs.speakup.users.AuthUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/messages")
class AdminMessageController {
    private final MessageService messageService;

    AdminMessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    String listMessages(Model model) {
        model.addAttribute("messages", messageService.findMessagesForModeration());
        return "admin/messages";
    }

    @PostMapping("/{messageId}/moderate")
    String moderateMessage(@PathVariable Long messageId, RedirectAttributes redirectAttributes) {
        messageService.moderateMessage(messageId, AuthUtils.getCurrentUserIdOrThrow());
        redirectAttributes.addFlashAttribute("successMessage", "Message moderated successfully.");
        return "redirect:/admin/messages";
    }
}
