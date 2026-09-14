package dev.sivalabs.feedbackhub.users;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
class AccountController {
    private final AccountService accountService;

    AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/account")
    String account(Model model) {
        populateAccount(model);
        return "account";
    }

    @PostMapping("/account/profile-picture")
    String uploadProfilePicture(
            @RequestParam("profilePicture") MultipartFile profilePicture, RedirectAttributes redirectAttributes) {
        try {
            accountService.saveProfilePicture(
                    AuthUtils.getCurrentUserIdOrThrow(), profilePicture.getContentType(), profilePicture.getBytes());
            redirectAttributes.addFlashAttribute("successMessage", "Profile picture updated successfully.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to read the selected image.");
        }
        return "redirect:/account";
    }

    @GetMapping("/account/profile-picture")
    ResponseEntity<byte[]> profilePicture() {
        var picture = accountService.getProfilePicture(AuthUtils.getCurrentUserIdOrThrow());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(picture.getContentType()))
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .body(picture.getContent());
    }

    @PostMapping("/account/password")
    String changePassword(
            @Valid @ModelAttribute("passwordForm") ChangePasswordForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (!form.newPassword().equals(form.confirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "New passwords do not match");
        }
        if (bindingResult.hasErrors()) {
            populateAccount(model);
            return "account";
        }
        try {
            accountService.changePassword(
                    AuthUtils.getCurrentUserIdOrThrow(),
                    new ChangePasswordCmd(form.currentPassword(), form.newPassword()));
        } catch (InvalidCurrentPasswordException e) {
            bindingResult.rejectValue("currentPassword", "password.invalid", e.getMessage());
            populateAccount(model);
            return "account";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Password changed successfully.");
        return "redirect:/account";
    }

    private void populateAccount(Model model) {
        model.addAttribute("account", accountService.getAccount(AuthUtils.getCurrentUserIdOrThrow()));
        if (!model.containsAttribute("passwordForm")) {
            model.addAttribute("passwordForm", new ChangePasswordForm("", "", ""));
        }
    }

    record ChangePasswordForm(
            @NotBlank(message = "Current password is required")
            String currentPassword,

            @NotBlank(message = "New password is required")
            @Size(min = 8, max = 72, message = "New password must be between 8 and 72 characters")
            String newPassword,

            @NotBlank(message = "Password confirmation is required")
            String confirmPassword) {}
}
