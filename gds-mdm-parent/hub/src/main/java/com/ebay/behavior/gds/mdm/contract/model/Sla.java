package com.ebay.behavior.gds.mdm.contract.model;

import com.ebay.behavior.gds.mdm.contract.util.DurationConverter;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.DurationDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.DurationSerializer;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Convert;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.time.Duration;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@SuperBuilder(toBuilder = true)
@Embeddable
public class Sla {

    @Column(name = "latency")
    private String latency;

    @Column(name = "readiness_time")
    private String readinessTime;

    @Column(name = "frequency")
    @Enumerated(EnumType.STRING)
    private DataFrequency frequency;

    @Column(name = "retention_period")
    @JsonSerialize(using = DurationSerializer.class)
    @JsonDeserialize(using = DurationDeserializer.class)
    @Convert(converter = DurationConverter.class)
    private Duration retentionPeriod;
}