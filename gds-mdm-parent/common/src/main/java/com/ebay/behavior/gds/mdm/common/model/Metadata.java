package com.ebay.behavior.gds.mdm.common.model;

import com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Metadata interface represents all the objects we store in the DG Portal (staged Signals, Fields, Events and Attributes).
 */
public interface Metadata {

    TypeReference<Map<String, Object>> MAP_TYPE_REF = new TypeReference<>() {
    };

    Long getId();

    @JsonIgnore
    UdcEntityType getEntityType();

    @JsonIgnore
    Map<String, Object> toMetadataMap(ObjectMapper objectMapper);

    static String toEntityId(@NotNull UdcEntityType type, @NotBlank String id) {
        return String.format("%s:%s", type.getValue().toLowerCase(Locale.US), id);
    }

    static String toEntityId(@NotNull UdcEntityType type, @NotNull Long id) {
        return toEntityId(type, String.valueOf(id));
    }

    @JsonIgnore
    default Map<String, Object> getIdMap() {
        return Map.of(getEntityType().getIdName(), getId());
    }

    @JsonIgnore
    default void validateAssociationsNotNull(Object... property) {
        for (Object prop : property) {
            if (null == prop) {
                throw new IllegalArgumentException(String.format(
                        "Some associated collections of %s are nulls. Please use getByIdWithAssociationsRecursive() to correctly load the signal.",
                        getEntityType().getValue()));
            }
        }
    }

    @JsonIgnore
    default <M extends Metadata> Map<String, Object> toMap(ObjectMapper objectMapper, M src) {
        Map<String, Object> dst = objectMapper.convertValue(src, MAP_TYPE_REF);
        dst.values().removeIf(Objects::isNull);
        dst.put(src.getEntityType().getIdName(), src.getId());
        return dst;
    }

    @JsonIgnore
    default void computeTimestamps(Map<String, Object> dst, Auditable auditable) {
        dst.compute("createDate", (k, v) -> auditable.getCreateDate() != null ? auditable.getCreateDate().getTime() : null);
        dst.compute("updateDate", (k, v) -> auditable.getUpdateDate() != null ? auditable.getUpdateDate().getTime() : null);
    }
}
