package com.ebay.behavior.gds.mdm.signal.common.model.audit;

import com.ebay.behavior.gds.mdm.common.model.ExpressionType;
import com.ebay.behavior.gds.mdm.common.model.JavaType;
import com.ebay.behavior.gds.mdm.common.model.audit.AbstractHistoryAuditable;
import com.ebay.behavior.gds.mdm.common.serde.AvroSchemaDeserializer;
import com.ebay.behavior.gds.mdm.common.serde.AvroSchemaSerializer;
import com.ebay.behavior.gds.mdm.signal.common.model.converter.AvroSchemaConverter;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import org.apache.avro.Schema;

import static com.ebay.behavior.gds.mdm.common.model.search.Search.NAME;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "field_history")
public class UnstagedFieldHistory extends AbstractHistoryAuditable {

    @NotNull
    @PositiveOrZero
    @Column(name = "signal_id")
    private Long signalId;

    @NotBlank
    @Column(name = NAME)
    private String name;

    @NotBlank
    @Column(name = "description")
    private String description;

    @Column(name = "tag")
    private String tag;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "java_type")
    private JavaType javaType;

    @NotNull
    @Column(name = "avro_schema")
    @Convert(converter = AvroSchemaConverter.class)
    @JsonSerialize(using = AvroSchemaSerializer.class)
    @JsonDeserialize(using = AvroSchemaDeserializer.class)
    private Schema avroSchema;

    @Column(name = "expression")
    private String expression;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "expression_type")
    private ExpressionType expressionType;

    @NotNull
    @Column(name = "is_mandatory")
    private Boolean isMandatory;

    @Column(name = "is_cached")
    private Boolean isCached;
}
