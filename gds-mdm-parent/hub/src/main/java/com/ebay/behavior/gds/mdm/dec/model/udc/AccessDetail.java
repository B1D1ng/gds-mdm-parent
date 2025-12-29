package com.ebay.behavior.gds.mdm.dec.model.udc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
@ToString
@EqualsAndHashCode
@Getter
@Setter
public class AccessDetail {

    private String accessMode;

    private String entityName;

    private String latency;

    private String retention;

    private String autoRetry;

    private String account;

    private String frequency;

    private String backendSystemParameters;

    private String pipelineId;
}
