package com.vorzak.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Component
public class ImageWorkerClient {

    private static final Logger log = LoggerFactory.getLogger(ImageWorkerClient.class);

    @Value("${vorzak.worker.url:http://localhost:8001}")
    private String workerUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public record DiffRegionDto(int x, int y, int w, int h, int area) {}

    public record ComparisonResultDto(
            String alignmentStatus,
            int inlierCount,
            Double ssimNormalized,
            int diffRegionCount,
            List<DiffRegionDto> regions,
            int phashDistance,
            String diffImageBase64,
            List<String> notes
    ) {}

    public ComparisonResultDto compare(byte[] submittedBytes, String subFilename, byte[] candidateBytes, String candFilename) {
        String endpoint = workerUrl + "/compare";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            ByteArrayResource subResource = new ByteArrayResource(submittedBytes) {
                @Override
                public String getFilename() {
                    return subFilename != null ? subFilename : "submitted.png";
                }
            };

            ByteArrayResource candResource = new ByteArrayResource(candidateBytes) {
                @Override
                public String getFilename() {
                    return candFilename != null ? candFilename : "candidate.png";
                }
            };

            body.add("submitted", subResource);
            body.add("candidate", candResource);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(endpoint, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String status = root.path("alignment_status").asText("NOT_COMPARABLE");
                int inliers = root.path("inlier_count").asInt(0);
                Double ssim = root.hasNonNull("ssim_normalized") ? root.path("ssim_normalized").asDouble() : null;
                int diffCount = root.path("diff_region_count").asInt(0);
                int phash = root.path("phash_distance").asInt(0);
                String diffB64 = root.path("diff_image_base64").asText(null);

                List<DiffRegionDto> regions = new ArrayList<>();
                JsonNode regArr = root.path("regions");
                if (regArr.isArray()) {
                    for (JsonNode r : regArr) {
                        regions.add(new DiffRegionDto(
                                r.path("x").asInt(),
                                r.path("y").asInt(),
                                r.path("w").asInt(),
                                r.path("h").asInt(),
                                r.path("area").asInt()
                        ));
                    }
                }

                List<String> notes = new ArrayList<>();
                JsonNode notesArr = root.path("notes");
                if (notesArr.isArray()) {
                    for (JsonNode n : notesArr) notes.add(n.asText());
                }

                return new ComparisonResultDto(status, inliers, ssim, diffCount, regions, phash, diffB64, notes);
            }
        } catch (Exception e) {
            log.warn("Image Worker communication failed at {}: {}", endpoint, e.getMessage());
        }

        // Return honest NOT_COMPARABLE fallback if worker is unreachable
        return new ComparisonResultDto(
                "NOT_COMPARABLE",
                0,
                null,
                0,
                List.of(),
                99,
                null,
                List.of("Image worker service unreachable or returned error")
        );
    }
}
