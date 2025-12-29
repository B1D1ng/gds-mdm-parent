package com.ebay.behavior.gds.mdm.contract.model;

import com.ebay.behavior.gds.mdm.common.serde.LazyObjectSerializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Set;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@Embeddable
public class Schema {

    @Column(name = "schema_subject_name")
    private String schemaSubjectName;

    @JsonSerialize(using = LazyObjectSerializer.class)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "contract_column", joinColumns = @JoinColumn(name = "id"))
    private Set<ColumnInfo> columns;
}
