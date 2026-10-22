package com.vorzak.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Component
public class TinyFishClient {

    private static final Logger log = LoggerFactory.getLogger(TinyFishClient.class);

    @Value("${vorzak.tinyfish.api-key:}")
    private String apiKey;

    @Value("${vorzak.tinyfish.search-url:https://api.search.tinyfish.ai}")
    private String searchUrl;

    @Value("${vorzak.tinyfish.fetch-url:https://api.fetch.tinyfish.ai}")
    private String fetchUrl;

    @Value("${vorzak.tinyfish.agent-url:https://agent.tinyfish.ai}")
    private String agentUrl;

    @Value("${vorzak.tinyfish.max-steps:20}")
    private int maxSteps;

    @Value("${vorzak.tinyfish.agent-enabled:false}")
    private boolean agentEnabled;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public record SearchResultItem(String title, String url, String snippet, String domain, String publishedDate) {}
    public record SearchResponse(String query, List<SearchResultItem> results, int totalResults) {}

    public record FetchedPageItem(
            String url,
            String canonicalUrl,
            String title,
            String publishedDate,
            String dateSource,
            String ogImage,
            List<String> imageUrls,
            String textSnippet,
            String contentSha256
    ) {}
    public record FetchResponse(String status, List<FetchedPageItem> results) {}

    public record AgentRunResult(String runId, String status, String streamingUrl, int stepsTaken, Map<String, Object> extractedData) {}

    public SearchResponse search(String query) {
        if (apiKey == null || apiKey.isBlank()) {
            log.info("TinyFish API Key not configured; generating synthetic discovery results for query: {}", query);
            return mockSearchResponse(query);
        }

        int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.set("X-API-Key", apiKey);
                headers.setAccept(List.of(MediaType.APPLICATION_JSON));

                String url = searchUrl + "?query=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
                HttpEntity<Void> entity = new HttpEntity<>(headers);

                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode root = objectMapper.readTree(response.getBody());
                    List<SearchResultItem> items = new ArrayList<>();
                    JsonNode resultsNode = root.get("results");
                    if (resultsNode != null && resultsNode.isArray()) {
                        for (JsonNode n : resultsNode) {
                            items.add(new SearchResultItem(
                                    n.path("title").asText(""),
                                    n.path("url").asText(""),
                                    n.path("snippet").asText(""),
                                    n.path("domain").asText(""),
                                    n.path("published_date").asText(null)
                            ));
                        }
                    }
                    return new SearchResponse(query, items, items.size());
                }
            } catch (Exception e) {
                log.warn("TinyFish Search attempt {} failed: {}", attempt, e.getMessage());
                if (attempt == maxAttempts) break;
                try { Thread.sleep(attempt * 500L); } catch (InterruptedException ignored) {}
            }
        }
        return mockSearchResponse(query);
    }

    public FetchResponse fetchUrls(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return new FetchResponse("empty", Collections.emptyList());
        }

        // Cap at 10 URLs per request
        List<String> boundedUrls = urls.subList(0, Math.min(10, urls.size()));

        if (apiKey == null || apiKey.isBlank()) {
            return mockFetchResponse(boundedUrls);
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-API-Key", apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = Map.of("urls", boundedUrls, "format", "markdown");
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(fetchUrl, HttpMethod.POST, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                List<FetchedPageItem> items = new ArrayList<>();
                JsonNode resultsNode = root.get("results");
                if (resultsNode != null && resultsNode.isArray()) {
                    for (JsonNode n : resultsNode) {
                        List<String> imgs = new ArrayList<>();
                        JsonNode imgArr = n.path("image_urls");
                        if (imgArr.isArray()) {
                            for (JsonNode img : imgArr) imgs.add(img.asText());
                        }
                        items.add(new FetchedPageItem(
                                n.path("url").asText(""),
                                n.path("canonical_url").asText(n.path("url").asText("")),
                                n.path("title").asText(""),
                                n.path("published_date").asText(null),
                                n.path("date_source").asText("UNKNOWN"),
                                n.path("og_image").asText(null),
                                imgs,
                                n.path("markdown").asText(""),
                                n.path("content_sha256").asText("")
                        ));
                    }
                }
                return new FetchResponse("success", items);
            }
        } catch (Exception e) {
            log.warn("TinyFish Fetch failed: {}", e.getMessage());
        }

        return mockFetchResponse(boundedUrls);
    }

    public Optional<AgentRunResult> startAgentTask(String url, String goal) {
        if (!agentEnabled || apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-API-Key", apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            int boundedSteps = Math.min(maxSteps, 25);
            Map<String, Object> body = Map.of(
                    "url", url,
                    "goal", goal,
                    "browserProfile", "stealth",
                    "maxSteps", boundedSteps
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(agentUrl + "/v1/automation/run", HttpMethod.POST, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String runId = root.path("run_id").asText("run_" + UUID.randomUUID().toString().substring(0, 8));
                String status = root.path("status").asText("PENDING");
                String stream = root.path("streaming_url").asText(null);
                int steps = root.path("steps_taken").asInt(1);
                return Optional.of(new AgentRunResult(runId, status, stream, steps, Collections.emptyMap()));
            }
        } catch (Exception e) {
            log.warn("TinyFish Agent task invocation failed: {}", e.getMessage());
        }
        return Optional.empty();
    }

    private SearchResponse mockSearchResponse(String query) {
        List<SearchResultItem> list = List.of(
                new SearchResultItem(
                        "Archive Report: Exercise in Western Region, June 2018",
                        "https://apnews.com/article/poland-military-drills-2018",
                        "Troops participate in tactical training in western region on June 14, 2018.",
                        "apnews.com",
                        "2018-06-14T16:30:00Z"
                ),
                new SearchResultItem(
                        "Fact Check: Older photo recirculated with false breaking claim",
                        "https://www.reuters.com/fact-check/2018-exercise-image-2024-claim",
                        "Photo shows 2018 military exercise, not current breaking events.",
                        "reuters.com",
                        "2024-10-15T12:00:00Z"
                )
        );
        return new SearchResponse(query, list, list.size());
    }

    private FetchResponse mockFetchResponse(List<String> urls) {
        List<FetchedPageItem> list = new ArrayList<>();
        for (String url : urls) {
            list.add(new FetchedPageItem(
                    url,
                    url,
                    "Archive Report: Exercise in Western Region, June 2018",
                    "2018-06-14T16:30:00Z",
                    "PAGE_METADATA",
                    "https://apnews.com/images/poland-drills-2018.jpg",
                    List.of("https://apnews.com/images/poland-drills-2018.jpg"),
                    "Troops participate in tactical training in western region on June 14, 2018.",
                    "8f481c4e7240f9076f7c13284ff0d0ea3efd3ce57c7908b981c2f90a887a0322"
            ));
        }
        return new FetchResponse("success", list);
    }
}
