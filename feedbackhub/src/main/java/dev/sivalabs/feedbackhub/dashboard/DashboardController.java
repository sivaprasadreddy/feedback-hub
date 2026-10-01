package dev.sivalabs.feedbackhub.dashboard;

import dev.sivalabs.feedbackhub.messages.MessagesAPI;
import dev.sivalabs.feedbackhub.messages.domain.models.MessageStatistics;
import dev.sivalabs.feedbackhub.users.UsersAPI;
import dev.sivalabs.feedbackhub.users.domain.models.SecurityUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class DashboardController {
    private final UsersAPI usersAPI;
    private final MessagesAPI messagesAPI;

    DashboardController(UsersAPI usersAPI, MessagesAPI messagesAPI) {
        this.usersAPI = usersAPI;
        this.messagesAPI = messagesAPI;
    }

    @GetMapping("/dashboard")
    String dashboard(@AuthenticationPrincipal SecurityUser currentUser, Model model) {
        if (currentUser.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"))) {
            MessageStatistics messageStatistics = messagesAPI.getStatistics();
            model.addAttribute("totalMessages", messageStatistics.messageCount());
            model.addAttribute("totalReplies", messageStatistics.replyCount());
            model.addAttribute("totalUsers", usersAPI.countUsers());
        }
        return "user-dashboard";
    }
}
