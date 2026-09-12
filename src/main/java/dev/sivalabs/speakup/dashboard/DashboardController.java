package dev.sivalabs.speakup.dashboard;

import dev.sivalabs.speakup.messages.MessageStatisticsService;
import dev.sivalabs.speakup.users.SecurityUser;
import dev.sivalabs.speakup.users.UserStatisticsService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class DashboardController {
    private final MessageStatisticsService messageStatisticsService;
    private final UserStatisticsService userStatisticsService;

    DashboardController(
            MessageStatisticsService messageStatisticsService, UserStatisticsService userStatisticsService) {
        this.messageStatisticsService = messageStatisticsService;
        this.userStatisticsService = userStatisticsService;
    }

    @GetMapping("/dashboard")
    String dashboard(@AuthenticationPrincipal SecurityUser currentUser, Model model) {
        if (currentUser.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"))) {
            var messageStatistics = messageStatisticsService.getStatistics();
            model.addAttribute("totalMessages", messageStatistics.messageCount());
            model.addAttribute("totalReplies", messageStatistics.replyCount());
            model.addAttribute("totalUsers", userStatisticsService.countUsers());
        }
        return "user-dashboard";
    }
}
