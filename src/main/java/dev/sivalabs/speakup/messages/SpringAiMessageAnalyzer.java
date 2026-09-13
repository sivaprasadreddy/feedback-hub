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
                        Analyze employee feedback. Return between one and three broad, relevant topics in Title Case
                        and choose exactly one sentiment: HAPPY, SAD, ANGRY, or DISAPPOINTED.
                        Prefer stable organization-level topics instead of fine-grained keywords or phrases copied
                        from the feedback. Examples include Work Culture, Engineering, Management, Benefits, Office,
                        HR, Ideas, Learning, Concern, People, and Other. This list is illustrative, not exhaustive.
                        Use Other only when no more meaningful broad topic applies.
                        Use SAD for generally unhappy content that is not clearly anger or disappointment.
                        Do not include explanations or information that is not present in the feedback.
                        """).user(content).call().entity(MessageAnalysis.class);
    }
}
