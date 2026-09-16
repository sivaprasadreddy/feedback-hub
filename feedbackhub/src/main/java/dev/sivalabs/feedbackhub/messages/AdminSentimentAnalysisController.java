package dev.sivalabs.feedbackhub.messages;

import java.time.LocalDate;
import java.util.Arrays;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/sentiment-analysis")
class AdminSentimentAnalysisController {
    private final MessageService messageService;

    AdminSentimentAnalysisController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    String sentimentAnalysis(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        try {
            model.addAttribute("sentimentCounts", messageService.getSentimentCounts(startDate, endDate));
        } catch (IllegalArgumentException e) {
            model.addAttribute("dateError", e.getMessage());
            model.addAttribute(
                    "sentimentCounts",
                    Arrays.stream(MessageSentiment.values())
                            .map(sentiment -> new SentimentCount(sentiment, 0, 0))
                            .toList());
        }
        return "admin/sentiment-analysis";
    }
}
