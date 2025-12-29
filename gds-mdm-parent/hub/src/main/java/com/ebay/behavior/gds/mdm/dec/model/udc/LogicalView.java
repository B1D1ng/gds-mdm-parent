package com.ebay.behavior.gds.mdm.dec.model.udc;

import com.ebay.behavior.gds.mdm.dec.model.enums.ViewType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
@ToString
@EqualsAndHashCode
@Getter
@Setter
public class LogicalView {

    private ViewType viewType;

    private String viewName;

    private String version;

    private String updateBy;

    private Timestamp updateDate;
}
