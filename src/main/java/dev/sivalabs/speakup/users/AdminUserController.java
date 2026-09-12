package dev.sivalabs.speakup.users;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
class AdminUserController {
    private final UserService userService;

    AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    String listUsers(Model model) {
        model.addAttribute("users", userService.findAllUsers());
        return "admin/users";
    }

    @GetMapping("/new")
    String newUserForm(Model model) {
        model.addAttribute("form", new CreateUserForm("", "", null));
        model.addAttribute("roles", Role.values());
        return "admin/users-new";
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
}
