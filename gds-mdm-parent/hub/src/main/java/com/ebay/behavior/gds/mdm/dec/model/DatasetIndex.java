package com.ebay.behavior.gds.mdm.dec.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Data
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@ToString
@EqualsAndHashCode(callSuper = true)
@Table(name = "dec_dataset_index")
public class DatasetIndex extends AbstractIndex {

    @NotNull
    @Column(name = "name")
    private String name;
}
