package dev.sivalabs.feedbackhub.users;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
class AdminUserController {
    private final UserService userService;

    AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    String listUsers(
            @RequestParam(required = false) Role role, @RequestParam(required = false) Boolean active, Model model) {
        model.addAttribute("users", userService.findUsers(new UserFilterQuery(role, active)));
        model.addAttribute("roles", Role.values());
        model.addAttribute("selectedRole", role);
        model.addAttribute("selectedActive", active);
        return "admin/users";
    }

    @GetMapping("/new")
    String newUserForm(Model model) {
        model.addAttribute("form", new CreateUserForm("", "", null));
        model.addAttribute("roles", Role.values());
        return "admin/users-new";
    }

    @GetMapping("/import")
    String importUsersForm() {
        return "admin/users-import";
    }

    @PostMapping("/import")
    String importUsers(@RequestParam("file") MultipartFile file, Model model, RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            model.addAttribute("errors", java.util.List.of(new ImportUserError(1, "Select a CSV file to import")));
            return "admin/users-import";
        }
        final String csv;
        try {
            csv = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            model.addAttribute("errors", java.util.List.of(new ImportUserError(1, "Could not read the CSV file")));
            return "admin/users-import";
        }
        var result = userService.importUsers(csv);
        if (!result.successful()) {
            model.addAttribute("errors", result.errors());
            return "admin/users-import";
        }
        redirectAttributes.addFlashAttribute(
                "successMessage", result.importedCount() + " users imported successfully.");
        return "redirect:/admin/users";
    }

    @PostMapping
    String createUser(
            @Valid @ModelAttribute("form") CreateUserForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("roles", Role.values());
            return "admin/users-new";
        }
        try {
            userService.createUser(
                    new CreateUserCmd(form.name().trim(), form.email().trim(), form.role()));
        } catch (DuplicateEmailException ex) {
            bindingResult.rejectValue("email", "email.duplicate", ex.getMessage());
            model.addAttribute("roles", Role.values());
            return "admin/users-new";
        }
        redirectAttributes.addFlashAttribute("successMessage", "User created successfully.");
        return "redirect:/admin/users";
    }

    @PostMapping("/{userId}/edit")
    String editUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "") String role,
            @RequestParam(defaultValue = "false") boolean active,
            RedirectAttributes redirectAttributes) {
        final Role assignedRole;
        try {
            assignedRole = Role.valueOf(role);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Role must be ADMIN or USER.");
            return "redirect:/admin/users";
        }
        userService.editUser(AuthUtils.getCurrentUserIdOrThrow(), userId, new EditUserCmd(assignedRole, active));
        redirectAttributes.addFlashAttribute("successMessage", "User updated successfully.");
        return "redirect:/admin/users";
    }

    record CreateUserForm(
            @NotBlank(message = "Name is required") String name,

            @NotBlank(message = "Email is required") @Email(message = "Email address must be valid")
            String email,

            @NotNull(message = "Role is required") Role role) {}
}
