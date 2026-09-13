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
@RequestMapping("/admin/replies")
class AdminReplyController {
    private final MessageService messageService;

    AdminReplyController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    String listReplies(@RequestParam(defaultValue = "1") String page, Model model) {
        var repliesPage = messageService.findRepliesForAdmin(parsePage(page));
        model.addAttribute("page", repliesPage);
        model.addAttribute("replies", repliesPage.data());
        return "admin/replies";
    }

    @PostMapping("/{replyId}/delete")
    String deleteReply(@PathVariable Long replyId, RedirectAttributes redirectAttributes) {
        messageService.deleteReplyAsAdmin(replyId, AuthUtils.getCurrentUserIdOrThrow());
        redirectAttributes.addFlashAttribute("successMessage", "Reply deleted successfully.");
        return "redirect:/admin/replies";
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
