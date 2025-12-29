package com.ebay.behavior.gds.mdm.contract.model;

import com.ebay.behavior.gds.mdm.common.model.Environment;
import com.ebay.behavior.gds.mdm.common.model.Model;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hibernate.annotations.Immutable;

/**
 * Read-only JPA entity for the contract_config_view database view.
 * This view provides an optimized way to search for unstaged contracts.
 * Each row represents a unique contract with aggregated topics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Entity
@Immutable
@Table(name = "contract_config_view")
@IdClass(VersionedId.class)
public class ContractConfigView implements Model {

    @Id
    @Column(name = "id")
    private Long id;

    @Id
    @Column(name = "version")
    private Integer version;

    @Column(name = "revision")
    private Integer revision;

    @Column(name = "name")
    private String name;

    @Column(name = "owners")
    private String owners;

    @Column(name = "entity_type")
    private String entityType;

    @Column(name = "domain")
    private String domain;

    @Column(name = "dl")
    private String dl;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "environment")
    private Environment environment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ContractStatus status;

    @Column(name = "source_type")
    private String sourceType;

    @Column(name = "topic")
    private String topic;

    @Column(name = "table_name")
    private String tableName;
}
