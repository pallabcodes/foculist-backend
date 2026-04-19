package com.yourorg.platform.foculist.sync.clean.domain.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Field-level LWW merge engine for JSON payloads.
 * <p>
 * Each field in the payload is treated as an independent
 * {@link LwwRegister}. When merging two payloads:
 * <ul>
 *   <li>Fields present in both → LWW wins (later timestamp)</li>
 *   <li>Fields in remote only → added to result</li>
 *   <li>Fields in local only → kept in result</li>
 * </ul>
 * <p>
 * Expected payload shape per field:
 * <pre>
 * {
 *   "fieldName": { "value": ..., "timestamp": "ISO-8601", "writerId": "..." },
 *   ...
 * }
 * </pre>
 * <p>
 * If a field doesn't carry CRDT metadata, it's treated as a plain value
 * with {@link Instant#EPOCH} timestamp (always overwritten by any CRDT-aware write).
 */
public class LwwMergeEngine {

    private final ObjectMapper objectMapper;

    public LwwMergeEngine(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Merge two JSON payloads field by field recursively using LWW semantics.
     *
     * @param localJson  the local (current) payload
     * @param remoteJson the remote (incoming) payload
     * @return the merged JSON string
     */
    public String merge(String localJson, String remoteJson) {
        Map<String, Object> local = parsePayload(localJson);
        Map<String, Object> remote = parsePayload(remoteJson);
        Map<String, Object> merged = mergeMaps(local, remote);
        return toJson(merged);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mergeMaps(Map<String, Object> local, Map<String, Object> remote) {
        Map<String, Object> result = new LinkedHashMap<>(local);

        for (Map.Entry<String, Object> entry : remote.entrySet()) {
            String field = entry.getKey();
            Object remoteVal = entry.getValue();

            if (result.containsKey(field)) {
                Object localVal = result.get(field);

                if (isRegister(localVal) && isRegister(remoteVal)) {
                    LwwRegister<Object> localReg = toRegister(field, localVal);
                    LwwRegister<Object> remoteReg = toRegister(field, remoteVal);
                    result.put(field, fromRegister(localReg.merge(remoteReg)));
                } else if (localVal instanceof Map && remoteVal instanceof Map) {
                    // Recursive merge for nested structures
                    result.put(field, mergeMaps((Map<String, Object>) localVal, (Map<String, Object>) remoteVal));
                } else {
                    // Fallback to simple LWW if structures don't match or aren't registers
                    LwwRegister<Object> localReg = toRegister(field, localVal);
                    LwwRegister<Object> remoteReg = toRegister(field, remoteVal);
                    result.put(field, fromRegister(localReg.merge(remoteReg)));
                }
            } else {
                result.put(field, isRegister(remoteVal) ? remoteVal : fromRegister(toRegister(field, remoteVal)));
            }
        }
        return result;
    }

    private boolean isRegister(Object val) {
        return val instanceof Map && ((Map<?, ?>) val).containsKey("timestamp");
    }

    private LwwRegister<Object> toRegister(String field, Object raw) {
        if (raw instanceof Map<?, ?> map) {
            Object value = map.get("value");
            String tsStr = map.containsKey("timestamp") ? map.get("timestamp").toString() : null;
            String writerId = map.containsKey("writerId") ? map.get("writerId").toString() : "unknown";
            Instant ts = tsStr != null ? Instant.parse(tsStr) : Instant.EPOCH;
            return new LwwRegister<>(value, ts, writerId);
        }
        // Plain value without CRDT metadata — epoch timestamp means it loses to any CRDT write
        return new LwwRegister<>(raw, Instant.EPOCH, "legacy");
    }

    private Map<String, Object> fromRegister(LwwRegister<Object> register) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("value", register.value());
        result.put("timestamp", register.timestamp().toString());
        result.put("writerId", register.writerId());
        return result;
    }

    private Map<String, Object> parsePayload(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new SyncDomainException("Failed to parse payload for merge: " + e.getMessage());
        }
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new SyncDomainException("Failed to serialize merged payload: " + e.getMessage());
        }
    }
}
