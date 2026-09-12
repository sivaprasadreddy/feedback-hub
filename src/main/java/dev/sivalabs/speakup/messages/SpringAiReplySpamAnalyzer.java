package dev.sivalabs.speakup.messages;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
class SpringAiReplySpamAnalyzer implements ReplySpamAnalyzer {
    private final ChatClient chatClient;

    SpringAiReplySpamAnalyzer(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public ReplySpamAnalysis analyze(String content) {
        return chatClient.prompt().system("""
                        Classify an employee feedback reply as spam or not spam. Spam includes unsolicited
                        advertising, scams, repeated promotional content, irrelevant links, and meaningless
                        bulk content. Disagreement, criticism, short replies, and strongly worded workplace
                        feedback are not spam merely because they are negative. Return only the structured
                        classification.
                        """).user(content).call().entity(ReplySpamAnalysis.class);
    }
}
