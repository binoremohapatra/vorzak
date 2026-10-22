package com.vorzak.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vorzak.model.EventEntity;
import com.vorzak.repository.EventRepository;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class InvestigationSseManager {

    private static final Logger log = LoggerFactory.getLogger(InvestigationSseManager.class);

    private final Map<UUID, List<SseEmitter>> emittersByInvestigation = new ConcurrentHashMap<>();
    private final EventRepository eventRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public InvestigationSseManager(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public SseEmitter subscribe(UUID investigationId, String lastEventId) {
        // 30-minute timeout for investigation SSE stream
        SseEmitter emitter = new SseEmitter(1800000L);

        emittersByInvestigation.computeIfAbsent(investigationId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(investigationId, emitter));
        emitter.onTimeout(() -> removeEmitter(investigationId, emitter));
        emitter.onError(e -> removeEmitter(investigationId, emitter));

        // Catch up on past events if lastEventId is provided or for initial hydration
        List<EventEntity> history = eventRepository.findByInvestigationIdOrderByTsAsc(investigationId);
        boolean replayRemaining = (lastEventId == null || lastEventId.isBlank());

        for (EventEntity ev : history) {
            if (!replayRemaining && ev.getId().toString().equals(lastEventId)) {
                replayRemaining = true;
                continue;
            }
            if (replayRemaining) {
                try {
                    emitter.send(SseEmitter.event()
                            .id(ev.getId().toString())
                            .name(ev.getType())
                            .data(ev.getPayload() != null ? ev.getPayload() : "{}"));
                } catch (IOException e) {
                    removeEmitter(investigationId, emitter);
                    break;
                }
            }
        }

        return emitter;
    }

    public void emit(UUID investigationId, String eventType, Map<String, Object> payload) {
        String jsonPayload = "{}";
        try {
            jsonPayload = objectMapper.writeValueAsString(payload);
        } catch (Exception ignored) {}

        // Save event record
        EventEntity eventEntity = new EventEntity(UUID.randomUUID(), investigationId, eventType, jsonPayload);
        eventRepository.save(eventEntity);

        List<SseEmitter> list = emittersByInvestigation.get(investigationId);
        if (list == null || list.isEmpty()) return;

        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event()
                        .id(eventEntity.getId().toString())
                        .name(eventType)
                        .data(jsonPayload));
            } catch (Exception e) {
                removeEmitter(investigationId, emitter);
            }
        }
    }

    @Scheduled(fixedRate = 15000)
    public void sendHeartbeats() {
        for (Map.Entry<UUID, List<SseEmitter>> entry : emittersByInvestigation.entrySet()) {
            UUID invId = entry.getKey();
            for (SseEmitter emitter : entry.getValue()) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("HEARTBEAT")
                            .data(Map.of("heartbeat", System.currentTimeMillis())));
                } catch (Exception e) {
                    removeEmitter(invId, emitter);
                }
            }
        }
    }

    private void removeEmitter(UUID investigationId, SseEmitter emitter) {
        List<SseEmitter> list = emittersByInvestigation.get(investigationId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                emittersByInvestigation.remove(investigationId);
            }
        }
    }

    @PreDestroy
    public void cleanup() {
        emittersByInvestigation.clear();
    }
}
