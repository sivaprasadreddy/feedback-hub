package dev.sivalabs.speakup.messages;

import dev.sivalabs.speakup.users.AuthUtils;
import dev.sivalabs.speakup.users.SecurityUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
class MessageController {
    private final MessageService messageService;

    MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/")
    String home(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new CreateMessageForm("", null));
        }
        populateHome(model);
        return "index";
    }

    @PostMapping("/messages")
    String createMessage(
            @Valid @ModelAttribute("form") CreateMessageForm form,
            BindingResult bindingResult,
            @AuthenticationPrincipal SecurityUser currentUser,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateHome(model);
            return "index";
        }
        messageService.createMessage(new CreateMessageCmd(
                form.content().trim(), currentUser.getId(), form.postingIdentity() == PostingIdentity.ANONYMOUS));
        redirectAttributes.addFlashAttribute("successMessage", "Message posted successfully.");
        return "redirect:/";
    }

    @GetMapping("/messages/{messageId}")
    String viewMessage(@PathVariable Long messageId, Model model) {
        model.addAttribute("message", messageService.findMessage(messageId, AuthUtils.getCurrentUserIdOrThrow()));
        model.addAttribute("replies", messageService.findReplies(messageId, AuthUtils.getCurrentUserIdOrThrow()));
        model.addAttribute("postingIdentities", PostingIdentity.values());
        if (!model.containsAttribute("replyForm")) {
            model.addAttribute("replyForm", new CreateReplyForm("", null));
        }
        return "messages/view";
    }

    @PostMapping("/messages/{messageId}/replies")
    String createReply(
            @PathVariable Long messageId,
            @Valid @ModelAttribute("replyForm") CreateReplyForm form,
            BindingResult bindingResult,
            @AuthenticationPrincipal SecurityUser currentUser,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("message", messageService.findMessage(messageId, currentUser.getId()));
            model.addAttribute("replies", messageService.findReplies(messageId, currentUser.getId()));
            model.addAttribute("postingIdentities", PostingIdentity.values());
            return "messages/view";
        }
        messageService.createReply(new CreateReplyCmd(
                messageId,
                form.content().trim(),
                currentUser.getId(),
                form.postingIdentity() == PostingIdentity.ANONYMOUS));
        redirectAttributes.addFlashAttribute("successMessage", "Reply posted successfully.");
        return "redirect:/messages/" + messageId;
    }

    @GetMapping("/messages/{messageId}/replies/{replyId}/edit")
    String editReplyForm(@PathVariable Long messageId, @PathVariable Long replyId, Model model) {
        model.addAttribute("messageId", messageId);
        model.addAttribute("replyId", replyId);
        model.addAttribute(
                "form", messageService.getReplyEditForm(messageId, replyId, AuthUtils.getCurrentUserIdOrThrow()));
        return "messages/edit-reply";
    }

    @PostMapping("/messages/{messageId}/replies/{replyId}/edit")
    String editReply(
            @PathVariable Long messageId,
            @PathVariable Long replyId,
            @Valid @ModelAttribute("form") EditReplyForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        var currentUserId = AuthUtils.getCurrentUserIdOrThrow();
        messageService.validateReplyCanBeEdited(messageId, replyId, currentUserId);
        if (bindingResult.hasErrors()) {
            model.addAttribute("messageId", messageId);
            model.addAttribute("replyId", replyId);
            return "messages/edit-reply";
        }
        messageService.editReply(
                messageId, replyId, currentUserId, form.content().trim());
        redirectAttributes.addFlashAttribute("successMessage", "Reply updated successfully.");
        return "redirect:/messages/" + messageId;
    }

    @PostMapping("/messages/{messageId}/replies/{replyId}/delete")
    String deleteReply(
            @PathVariable Long messageId, @PathVariable Long replyId, RedirectAttributes redirectAttributes) {
        messageService.deleteReply(messageId, replyId, AuthUtils.getCurrentUserIdOrThrow());
        redirectAttributes.addFlashAttribute("successMessage", "Reply deleted successfully.");
        return "redirect:/messages/" + messageId;
    }

    @PostMapping("/messages/{messageId}/vote")
    String voteOnMessage(
            @PathVariable Long messageId, @RequestParam VoteType voteType, RedirectAttributes redirectAttributes) {
        messageService.voteOnMessage(messageId, AuthUtils.getCurrentUserIdOrThrow(), voteType);
        redirectAttributes.addFlashAttribute("successMessage", "Vote recorded successfully.");
        return "redirect:/messages/" + messageId;
    }

    @PostMapping("/messages/{messageId}/vote/remove")
    String removeMessageVote(@PathVariable Long messageId, RedirectAttributes redirectAttributes) {
        messageService.removeMessageVote(messageId, AuthUtils.getCurrentUserIdOrThrow());
        redirectAttributes.addFlashAttribute("successMessage", "Vote removed successfully.");
        return "redirect:/messages/" + messageId;
    }

    @GetMapping("/messages/{messageId}/edit")
    String editMessageForm(@PathVariable Long messageId, Model model) {
        model.addAttribute("messageId", messageId);
        model.addAttribute("form", messageService.getEditForm(messageId, AuthUtils.getCurrentUserIdOrThrow()));
        return "messages/edit";
    }

    @PostMapping("/messages/{messageId}/edit")
    String editMessage(
            @PathVariable Long messageId,
            @Valid @ModelAttribute("form") EditMessageForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("messageId", messageId);
            return "messages/edit";
        }
        messageService.editMessage(
                messageId, AuthUtils.getCurrentUserIdOrThrow(), form.content().trim());
        redirectAttributes.addFlashAttribute("successMessage", "Message updated successfully.");
        return "redirect:/messages/" + messageId;
    }

    @PostMapping("/messages/{messageId}/delete")
    String deleteMessage(@PathVariable Long messageId, RedirectAttributes redirectAttributes) {
        messageService.deleteMessage(messageId, AuthUtils.getCurrentUserIdOrThrow());
        redirectAttributes.addFlashAttribute("successMessage", "Message deleted successfully.");
        return "redirect:/messages/" + messageId;
    }

    private void populateHome(Model model) {
        model.addAttribute("postingIdentities", PostingIdentity.values());
        model.addAttribute("messages", messageService.findRecentMessages());
    }
}
