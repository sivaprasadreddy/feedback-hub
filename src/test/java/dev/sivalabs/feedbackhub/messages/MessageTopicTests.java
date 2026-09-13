package dev.sivalabs.feedbackhub.messages;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

class MessageTopicTests {
    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void usesEnumNameForJsonInteractions() throws Exception {
        assertThat(jsonMapper.readValue("\"WORK_CULTURE\"", MessageTopic.class)).isEqualTo(MessageTopic.WORK_CULTURE);
        assertThat(jsonMapper.writeValueAsString(MessageTopic.WORK_CULTURE)).isEqualTo("\"WORK_CULTURE\"");
    }

    @Test
    void usesEnumNamesInPromptAndDisplayNameForUi() {
        assertThat(MessageTopic.promptValues()).contains("WORK_CULTURE").doesNotContain("Work Culture");
        assertThat(MessageTopic.WORK_CULTURE.displayName()).isEqualTo("Work Culture");
    }
}
