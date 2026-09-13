package dev.sivalabs.feedbackhub.messages;

import java.util.Arrays;
import java.util.stream.Collectors;

enum MessageTopic {
    WORK_CULTURE("Work Culture"),
    PEOPLE_AND_TEAM("People & Team"),
    MANAGEMENT_AND_LEADERSHIP("Management & Leadership"),
    ENGINEERING_AND_TECHNOLOGY("Engineering & Technology"),
    PRODUCT_AND_BUSINESS("Product & Business"),
    PROJECTS_AND_PROCESSES("Projects & Processes"),
    COMMUNICATION("Communication"),
    CAREER_AND_GROWTH("Career & Growth"),
    LEARNING_AND_DEVELOPMENT("Learning & Development"),
    COMPENSATION_AND_REWARDS("Compensation & Rewards"),
    BENEFITS_AND_PERKS("Benefits & Perks"),
    WORK_LIFE_BALANCE("Work-Life Balance"),
    REMOTE_AND_HYBRID_WORK("Remote & Hybrid Work"),
    OFFICE_AND_WORKPLACE("Office & Workplace"),
    HR_AND_POLICIES("HR & Policies"),
    DIVERSITY_AND_INCLUSION("Diversity & Inclusion"),
    RECOGNITION_AND_APPRECIATION("Recognition & Appreciation"),
    IDEAS_AND_SUGGESTIONS("Ideas & Suggestions"),
    EMPLOYEE_EXPERIENCE("Employee Experience"),
    CONCERNS_AND_ISSUES("Concerns & Issues"),
    SAFETY_AND_SECURITY("Safety & Security"),
    ETHICS_AND_COMPLIANCE("Ethics & Compliance"),
    COMMUNITY_AND_SOCIAL("Community & Social"),
    OTHER("Other");

    private final String displayName;

    MessageTopic(String displayName) {
        this.displayName = displayName;
    }

    String displayName() {
        return displayName;
    }

    static String promptValues() {
        return Arrays.stream(values()).map(MessageTopic::name).collect(Collectors.joining("\n"));
    }
}
