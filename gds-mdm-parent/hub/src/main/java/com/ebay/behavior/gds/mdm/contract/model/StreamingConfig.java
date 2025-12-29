package com.ebay.behavior.gds.mdm.contract.model;

import com.ebay.behavior.gds.mdm.common.model.AbstractAuditable;
import com.ebay.behavior.gds.mdm.common.serde.LazyObjectSerializer;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.util.Set;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@SuperBuilder(toBuilder = true)
@ToString(exclude = "component")
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "streaming_config")
public class StreamingConfig extends AbstractAuditable {

    @NotNull
    @PositiveOrZero
    @Column(name = "component_id")
    private Long componentId;

    @Column(name = "group_id")
    private String groupId;

    @Column(name = "env")
    @NotBlank
    private String env;

    @Column(name = "schema_id")
    private Long schemaId;

    @Column(name = "format")
    private String format;

    @Column(name = "scan_startup_mode")
    private String scanStartupMode;

    @Column(name = "stream_name")
    private String streamName;

    @Column(name = "properties")
    private String properties;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "streaming_config_topic", joinColumns = @JoinColumn(name = ID))
    @Column(name = "topic")
    private Set<String> topics;

    @Column(name = "topic_pattern")
    private String topicPattern;

    @JsonBackReference
    @JsonSerialize(using = LazyObjectSerializer.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "component_id", insertable = false, updatable = false)
    private Component component;
}