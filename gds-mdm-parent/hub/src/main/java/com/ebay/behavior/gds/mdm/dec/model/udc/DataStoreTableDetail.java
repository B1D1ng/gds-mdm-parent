package com.ebay.behavior.gds.mdm.dec.model.udc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Represents the details of a data store table in UDC.
 * containing identity properties to identify the entity of DataStore, RheosKafkaTopic, DataTable in UDC
 */
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
@ToString
@EqualsAndHashCode
@Getter
@Setter
public class DataStoreTableDetail {

    private String dataStoreName;

    private String dataStoreType;

    private String dataTableName;
}
