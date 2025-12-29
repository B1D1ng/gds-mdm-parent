package com.ebay.behavior.gds.mdm.contract.model;

import com.ebay.behavior.gds.mdm.common.serde.LazyObjectSerializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
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
@ToString(callSuper = true, exclude = {"transformations", "filters"})
@EqualsAndHashCode(callSuper = true, exclude = {"transformations", "filters"})
@Entity
@Table(name = "transformer")
public class Transformer extends Component {
    @JsonSerialize(using = LazyObjectSerializer.class)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "udf_alias", joinColumns = @JoinColumn(name = ID))
    private Set<UdfAlias> udfAliases;

    @JsonSerialize(using = LazyObjectSerializer.class)
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "component")
    private Set<Transformation> transformations;

    @JsonSerialize(using = LazyObjectSerializer.class)
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "component")
    private Set<Filter> filters;
}