package dev.sivalabs.feedbackhub.messages;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.StructuredOutputValidationAdvisor;
import org.springframework.stereotype.Component;

@Component
class SpringAiMessageAnalyzer implements MessageAnalyzer {
    private final ChatClient chatClient;

    SpringAiMessageAnalyzer(ChatClient.Builder builder) {
        var loggerAdvisor = new SimpleLoggerAdvisor();
        var validationAdvisor = StructuredOutputValidationAdvisor.builder()
                .outputType(MessageAnalysis.class)
                .maxRepeatAttempts(3)
                .build();
        this.chatClient = builder.defaultSystem("""
                        Analyze employee feedback. Return between one and three relevant topics, using only the exact
                        enum values in the following list:

                        %s

                        Do not create, rename, shorten, or combine topic values. Use OTHER only when no more specific
                        topic from the list applies. Choose exactly one sentiment: NEUTRAL, HAPPY, SAD, ANGRY, or DISAPPOINTED.
                        Use SAD for generally unhappy content that is not clearly anger or disappointment.
                        Do not include explanations or information that is not present in the feedback.
                        """.formatted(MessageTopic.promptValues()))
                .defaultAdvisors(validationAdvisor, loggerAdvisor)
                .build();
    }

    @Override
    public MessageAnalysis analyze(String content) {
        return chatClient.prompt().user(content).call().entity(MessageAnalysis.class);
    }
}
