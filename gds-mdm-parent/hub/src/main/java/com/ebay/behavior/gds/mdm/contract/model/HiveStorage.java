package com.ebay.behavior.gds.mdm.contract.model;

import com.ebay.behavior.gds.mdm.common.model.AbstractAuditable;
import com.ebay.behavior.gds.mdm.contract.util.StringListConverter;

import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@SuperBuilder(toBuilder = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "hive_storage")
public class HiveStorage extends AbstractAuditable {

    @NotNull
    @Column(name = "db_name")
    private String dbName;

    @NotNull
    @Column(name = "table_name")
    private String tableName;

    @NotNull
    @Column(name = "data_center")
    @Convert(converter = StringListConverter.class)
    private List<String> dataCenter;

    @NotNull
    @Column(name = "format")
    private String format;

    @Column(name = "primary_keys")
    @Convert(converter = StringListConverter.class)
    private List<String> primaryKeys;

    @Column(name = "partition_columns")
    @Convert(converter = StringListConverter.class)
    private List<String> partitionColumns;

    @Column(name = "done_file_type")
    @Enumerated(EnumType.STRING)
    private DoneFileType doneFileType;

    @Column(name = "done_file_path")
    private String doneFilePath;
}