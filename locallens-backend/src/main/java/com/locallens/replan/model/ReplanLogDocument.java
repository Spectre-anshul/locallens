package com.locallens.replan.model;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@Document(collection = "replan_logs")
public class ReplanLogDocument {
    @Id private String id;
    private String tripId;
    private int dayNumber;
    private String triggeredBy; // WEATHER, TRAFFIC, VENUE_CLOSURE, USER_REQUEST
    private String reasonCode;
    private String reasonDetail;
    private List<String> affectedSlots;
    private DiffPayload diff;
    private int previousVersion;
    private int newVersion;
    private int claudePromptTokens;
    private int claudeResponseTokens;
    private long processingTimeMs;
    private Boolean accepted;
    @CreatedDate private Instant createdAt;

    @Data
    public static class DiffPayload {
        private List<SlotDiff> removed;
        private List<SlotDiff> added;
        private List<FieldDiff> modified;
    }

    @Data public static class SlotDiff { private String slotId; private String title; private String startTime; private String endTime; }
    @Data public static class FieldDiff { private String slotId; private String field; private String oldValue; private String newValue; }
}
