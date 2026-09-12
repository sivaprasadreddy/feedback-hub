package dev.sivalabs.speakup.messages;

import java.util.Set;

record MessageAnalysis(Set<String> labels, MessageSentiment sentiment) {}
