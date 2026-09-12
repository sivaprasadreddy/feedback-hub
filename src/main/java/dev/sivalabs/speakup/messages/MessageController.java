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
        return "messages/view";
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
