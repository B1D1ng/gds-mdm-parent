package com.ebay.behavior.gds.mdm.signal.common.model.audit;

import com.ebay.behavior.gds.mdm.common.model.JavaType;
import com.ebay.behavior.gds.mdm.common.model.audit.AbstractHistoryAuditable;

import jakarta.persistence.Column;
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

@Data
@NoArgsConstructor
@Accessors(chain = true)
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "attribute_history")
public class UnstagedAttributeHistory extends AbstractHistoryAuditable {

    @NotNull
    @PositiveOrZero
    @Column(name = "event_id")
    private Long eventId;

    @NotBlank
    @Column(name = "tag")
    private String tag;

    @Column(name = "description")
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "java_type")
    private JavaType javaType;

    @NotBlank
    @Column(name = "schema_path")
    private String schemaPath;

    @Column(name = "is_store_in_state")
    private Boolean isStoreInState;
}
