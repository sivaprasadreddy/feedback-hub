package dev.sivalabs.speakup.messages;

import dev.sivalabs.speakup.shared.BadRequestException;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
class MessageController {
    private final MessageService messageService;

    MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/")
    String home(
            @RequestParam(defaultValue = "RECENT") FeedType feed,
            @RequestParam(defaultValue = "1") String page,
            Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new CreateMessageForm("", null));
        }
        populateHome(model, feed, parsePage(page));
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
            populateHome(model, FeedType.RECENT, 1);
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
            @PathVariable Long messageId,
            @RequestParam VoteType voteType,
            @RequestParam(defaultValue = "false") boolean returnToHome,
            @RequestHeader(name = "HX-Request", defaultValue = "false") boolean htmxRequest,
            Model model,
            RedirectAttributes redirectAttributes) {
        var currentUserId = AuthUtils.getCurrentUserIdOrThrow();
        messageService.voteOnMessage(messageId, currentUserId, voteType);
        if (htmxRequest) {
            model.addAttribute("message", messageService.findMessage(messageId, currentUserId));
            model.addAttribute("returnToHome", returnToHome);
            return "fragments/message-votes :: votes(message=${message}, returnToHome=${returnToHome})";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Vote recorded successfully.");
        return returnToHome ? "redirect:/" : "redirect:/messages/" + messageId;
    }

    @PostMapping("/messages/{messageId}/vote/remove")
    String removeMessageVote(
            @PathVariable Long messageId,
            @RequestParam(defaultValue = "false") boolean returnToHome,
            @RequestHeader(name = "HX-Request", defaultValue = "false") boolean htmxRequest,
            Model model,
            RedirectAttributes redirectAttributes) {
        var currentUserId = AuthUtils.getCurrentUserIdOrThrow();
        messageService.removeMessageVote(messageId, currentUserId);
        if (htmxRequest) {
            model.addAttribute("message", messageService.findMessage(messageId, currentUserId));
            model.addAttribute("returnToHome", returnToHome);
            return "fragments/message-votes :: votes(message=${message}, returnToHome=${returnToHome})";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Vote removed successfully.");
        return returnToHome ? "redirect:/" : "redirect:/messages/" + messageId;
    }

    @PostMapping("/messages/{messageId}/replies/{replyId}/vote")
    String voteOnReply(
            @PathVariable Long messageId,
            @PathVariable Long replyId,
            @RequestParam VoteType voteType,
            @RequestHeader(name = "HX-Request", defaultValue = "false") boolean htmxRequest,
            Model model,
            RedirectAttributes redirectAttributes) {
        var currentUserId = AuthUtils.getCurrentUserIdOrThrow();
        messageService.voteOnReply(messageId, replyId, currentUserId, voteType);
        if (htmxRequest) {
            model.addAttribute("messageId", messageId);
            model.addAttribute("reply", findReply(messageId, replyId, currentUserId));
            return "fragments/reply-votes :: votes(messageId=${messageId}, reply=${reply})";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Vote recorded successfully.");
        return "redirect:/messages/" + messageId;
    }

    @PostMapping("/messages/{messageId}/replies/{replyId}/vote/remove")
    String removeReplyVote(
            @PathVariable Long messageId,
            @PathVariable Long replyId,
            @RequestHeader(name = "HX-Request", defaultValue = "false") boolean htmxRequest,
            Model model,
            RedirectAttributes redirectAttributes) {
        var currentUserId = AuthUtils.getCurrentUserIdOrThrow();
        messageService.removeReplyVote(messageId, replyId, currentUserId);
        if (htmxRequest) {
            model.addAttribute("messageId", messageId);
            model.addAttribute("reply", findReply(messageId, replyId, currentUserId));
            return "fragments/reply-votes :: votes(messageId=${messageId}, reply=${reply})";
        }
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

    private void populateHome(Model model, FeedType feed, int pageNo) {
        model.addAttribute("postingIdentities", PostingIdentity.values());
        model.addAttribute("selectedFeed", feed);
        var currentUserId = AuthUtils.getCurrentUserIdOrThrow();
        var page = feed == FeedType.POPULAR
                ? messageService.findPopularMessages(currentUserId, pageNo)
                : messageService.findRecentMessages(currentUserId, pageNo);
        model.addAttribute("page", page);
        model.addAttribute("messages", page.data());
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

    private ReplyDto findReply(Long messageId, Long replyId, Long currentUserId) {
        return messageService.findReplies(messageId, currentUserId).stream()
                .filter(reply -> reply.id().equals(replyId))
                .findFirst()
                .orElseThrow();
    }
}
