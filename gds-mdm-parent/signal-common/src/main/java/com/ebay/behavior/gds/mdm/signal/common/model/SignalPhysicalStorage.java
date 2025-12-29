package com.ebay.behavior.gds.mdm.signal.common.model;

import com.ebay.behavior.gds.mdm.common.model.AbstractAuditable;
import com.ebay.behavior.gds.mdm.common.model.Auditable;
import com.ebay.behavior.gds.mdm.common.model.Environment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

@Data
@Accessors(chain = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@Entity
@EqualsAndHashCode(callSuper = true)
@Table(name = "signal_physical_storage")
public class SignalPhysicalStorage extends AbstractAuditable implements Auditable {

    @NotBlank
    @Column(name = "description")
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "environment")
    private Environment environment;

    @Column(name = "kafka_topic")
    private String kafkaTopic;

    @Column(name = "kafka_schema")
    private String kafkaSchema;

    @Column(name = "hive_table_name")
    private String hiveTableName;

    @Column(name = "done_file_path")
    private String doneFilePath;
}
