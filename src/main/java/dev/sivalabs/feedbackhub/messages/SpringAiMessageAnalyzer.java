package dev.sivalabs.feedbackhub.messages;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
class SpringAiMessageAnalyzer implements MessageAnalyzer {
    private final ChatClient chatClient;

    SpringAiMessageAnalyzer(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public MessageAnalysis analyze(String content) {
        return chatClient
                .prompt()
                .system("""
                        Analyze employee feedback. Return between one and three relevant topics, using only the exact
                        topic names in the following list:

                        %s

                        Do not create, rename, shorten, or combine topic names. Use Other only when no more specific
                        topic from the list applies. Choose exactly one sentiment: HAPPY, SAD, ANGRY, or DISAPPOINTED.
                        Use SAD for generally unhappy content that is not clearly anger or disappointment.
                        Do not include explanations or information that is not present in the feedback.
                        """.formatted(MessageTopic.promptValues()))
                .user(content)
                .call()
                .entity(MessageAnalysis.class);
    }
}
