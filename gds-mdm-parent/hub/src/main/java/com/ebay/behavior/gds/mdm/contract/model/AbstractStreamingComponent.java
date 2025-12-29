package com.ebay.behavior.gds.mdm.contract.model;

import com.ebay.behavior.gds.mdm.common.serde.LazyObjectSerializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.FetchType;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
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
@ToString(callSuper = true, exclude = "streamingConfigs")
@MappedSuperclass
@EqualsAndHashCode(callSuper = true, exclude = "streamingConfigs")
public abstract class AbstractStreamingComponent extends Component {
    @Column(name = "connector_type")
    private String connectorType;

    @Column(name = "metadata_id")
    private String metadataId;

    @JsonSerialize(using = LazyObjectSerializer.class)
    @Embedded
    private Schema schema;

    @JsonSerialize(using = LazyObjectSerializer.class)
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "component")
    private Set<StreamingConfig> streamingConfigs;
}