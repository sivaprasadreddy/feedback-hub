package dev.sivalabs.feedbackhub;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

@Order(3)
class ApplicationTests extends BaseIT {

    @Test
    void contextLoads() {
        assertTrue(true, "Failed to load application context");
    }
}
