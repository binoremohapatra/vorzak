package com.vorzak.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Component
public class SourceTierClassifier {

    public enum Tier {
        FACT_CHECKER,
        WIRE,
        MAJOR_OUTLET,
        OTHER,
        SOCIAL,
        UNKNOWN
    }

    private final List<String> factCheckers = new ArrayList<>();
    private final List<String> wires = new ArrayList<>();
    private final List<String> majorOutlets = new ArrayList<>();
    private final List<String> social = new ArrayList<>();

    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource("source-tiers.json");
            if (resource.exists()) {
                try (InputStream is = resource.getInputStream()) {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode root = mapper.readTree(is);

                    populateList(root.get("fact_checkers"), factCheckers);
                    populateList(root.get("wires"), wires);
                    populateList(root.get("major_outlets"), majorOutlets);
                    populateList(root.get("social"), social);
                }
            }
        } catch (Exception e) {
            // Fallback defaults
            factCheckers.addAll(List.of("snopes.com", "politifact.com", "factcheck.org", "fullfact.org"));
            wires.addAll(List.of("reuters.com", "apnews.com", "afp.com"));
            majorOutlets.addAll(List.of("bbc.com", "nytimes.com", "washingtonpost.com"));
            social.addAll(List.of("twitter.com", "x.com", "facebook.com", "reddit.com"));
        }
    }

    private void populateList(JsonNode node, List<String> target) {
        if (node != null && node.isArray()) {
            for (JsonNode item : node) {
                target.add(item.asText().toLowerCase().trim());
            }
        }
    }

    public Tier classify(String urlString) {
        if (urlString == null || urlString.isBlank()) {
            return Tier.UNKNOWN;
        }

        String lowerUrl = urlString.toLowerCase();
        String host = extractHost(lowerUrl);

        // Check path-sensitive fact checkers first (e.g. reuters.com/fact-check)
        for (String pattern : factCheckers) {
            if (lowerUrl.contains(pattern)) return Tier.FACT_CHECKER;
        }

        // Check wires
        for (String pattern : wires) {
            if (host.endsWith(pattern) || lowerUrl.contains(pattern)) return Tier.WIRE;
        }

        // Check major outlets
        for (String pattern : majorOutlets) {
            if (host.endsWith(pattern) || lowerUrl.contains(pattern)) return Tier.MAJOR_OUTLET;
        }

        // Check social networks
        for (String pattern : social) {
            if (host.endsWith(pattern) || lowerUrl.contains(pattern)) return Tier.SOCIAL;
        }

        return Tier.OTHER;
    }

    private String extractHost(String url) {
        try {
            URI uri = URI.create(url);
            String h = uri.getHost();
            return h != null ? h : "";
        } catch (Exception e) {
            return "";
        }
    }
}
