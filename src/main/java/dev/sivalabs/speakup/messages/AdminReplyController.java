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
@RequestMapping("/admin/replies")
class AdminReplyController {
    private final MessageService messageService;

    AdminReplyController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    String listReplies(Model model) {
        model.addAttribute("replies", messageService.findRepliesForModeration());
        return "admin/replies";
    }

    @PostMapping("/{replyId}/moderate")
    String moderateReply(@PathVariable Long replyId, RedirectAttributes redirectAttributes) {
        messageService.moderateReply(replyId, AuthUtils.getCurrentUserIdOrThrow());
        redirectAttributes.addFlashAttribute("successMessage", "Reply moderated successfully.");
        return "redirect:/admin/replies";
    }
}
