package dev.sivalabs.speakup.messages;

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
        return chatClient.prompt().system("""
                        Analyze employee feedback. Return between one and five short, relevant topic labels in
                        lowercase and choose exactly one sentiment: HAPPY, SAD, ANGRY, or DISAPPOINTED.
                        Use SAD for generally unhappy content that is not clearly anger or disappointment.
                        Do not include explanations or information that is not present in the feedback.
                        """).user(content).call().entity(MessageAnalysis.class);
    }
}
