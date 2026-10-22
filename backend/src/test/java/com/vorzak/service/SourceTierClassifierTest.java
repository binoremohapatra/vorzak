package com.vorzak.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SourceTierClassifierTest {

    private SourceTierClassifier classifier;

    @BeforeEach
    void setUp() {
        classifier = new SourceTierClassifier();
        classifier.init();
    }

    @Test
    void testClassifyTiers() {
        assertEquals(SourceTierClassifier.Tier.FACT_CHECKER, classifier.classify("https://www.reuters.com/fact-check/item-123"));
        assertEquals(SourceTierClassifier.Tier.FACT_CHECKER, classifier.classify("https://snopes.com/fact-check/image-claim"));
        assertEquals(SourceTierClassifier.Tier.WIRE, classifier.classify("https://apnews.com/article/breaking-news-456"));
        assertEquals(SourceTierClassifier.Tier.MAJOR_OUTLET, classifier.classify("https://www.bbc.com/news/world-123"));
        assertEquals(SourceTierClassifier.Tier.SOCIAL, classifier.classify("https://twitter.com/user/status/789"));
        assertEquals(SourceTierClassifier.Tier.OTHER, classifier.classify("https://random-blog.xyz/post/1"));
        assertEquals(SourceTierClassifier.Tier.UNKNOWN, classifier.classify(""));
    }
}
