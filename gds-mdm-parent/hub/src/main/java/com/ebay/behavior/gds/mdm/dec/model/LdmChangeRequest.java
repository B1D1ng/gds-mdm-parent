package com.ebay.behavior.gds.mdm.dec.model;

import com.ebay.behavior.gds.mdm.dec.model.enums.ActionTarget;
import com.ebay.behavior.gds.mdm.dec.model.enums.ActionType;
import com.ebay.behavior.gds.mdm.dec.model.enums.ChangeRequestStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@Entity
@ToString
@EqualsAndHashCode(callSuper = true)
@Table(name = "dec_ldm_change_request")
public class LdmChangeRequest extends DecAuditable {

    @NotNull
    @Column(name = "action_type")
    private ActionType actionType;

    @NotNull
    @Column(name = "action_target")
    private ActionTarget actionTarget;

    @NotNull
    @Column(name = "request_details")
    private String requestDetails;

    @NotNull
    @Builder.Default
    @Column(name = "status")
    private ChangeRequestStatus status = ChangeRequestStatus.DRAFT;

    @Column(name = "log_records")
    private String logRecords;
}
