package com.ebay.behavior.gds.mdm.contract.model;

import com.ebay.behavior.gds.mdm.common.model.AbstractAuditable;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@SuperBuilder(toBuilder = true)
@Entity
@Table(name = "config_storage_mapping")
public class ConfigStorageMapping extends AbstractAuditable {

    @NotNull
    @Column(name = "config_id")
    private Long configId;

    @NotNull
    @Column(name = "storage_id")
    private Long storageId;

    public ConfigStorageMapping(Long configId, Long storageId) {
        this.configId = configId;
        this.storageId = storageId;
    }
}