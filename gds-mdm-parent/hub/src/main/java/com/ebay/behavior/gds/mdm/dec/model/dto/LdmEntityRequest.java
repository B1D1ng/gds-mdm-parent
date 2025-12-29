package com.ebay.behavior.gds.mdm.dec.model.dto;

import com.ebay.behavior.gds.mdm.dec.model.LdmBaseEntity;
import com.ebay.behavior.gds.mdm.dec.model.LdmEntity;
import com.ebay.behavior.gds.mdm.dec.model.LdmErrorHandlingStorageMapping;
import com.ebay.behavior.gds.mdm.dec.model.LdmField;
import com.ebay.behavior.gds.mdm.dec.model.enums.CodeLanguageType;
import com.ebay.behavior.gds.mdm.dec.model.enums.Environment;
import com.ebay.behavior.gds.mdm.dec.model.enums.LdmStatus;
import com.ebay.behavior.gds.mdm.dec.model.enums.ViewType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.beans.BeanUtils;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class LdmEntityRequest {

    private Long id;

    private Integer version;

    private Integer revision;

    private String name;

    private ViewType viewType;

    private Long baseEntityId;

    private String description;

    private String owners;

    private String jiraProject;

    private String domain;

    private String pk;

    private Long namespaceId;

    private String upstreamLdm;

    private CodeLanguageType codeLanguage;

    private String codeContent;

    private String generatedSql;

    private String ir;

    private String languageFrontendVersion;

    private LdmStatus status = LdmStatus.DRAFT;

    private Environment environment = Environment.UNSTAGED;

    private Long requestId;

    private String team;

    private String teamDl;

    private List<String> udfs;

    private Boolean isDcs;

    private List<String> dcsFields;

    private Set<Long> dcsLdms;

    private String createBy;

    private String updateBy;

    private Timestamp createDate;

    private Timestamp updateDate;

    private List<LdmField> fields;

    private Set<LdmErrorHandlingStorageMapping> errorHandlingStorageMappings;

    private LdmBaseEntity baseEntity;

    public LdmEntity toLdmEntity() {
        LdmEntity entity = new LdmEntity();
        BeanUtils.copyProperties(this, entity, "fields", "errorHandlingStorageMappings", "baseEntity");
        if (this.fields != null) {
            for (int i = 0; i < this.fields.size(); i++) {
                LdmField field = this.fields.get(i);
                field.setOrdinal(i + 1);
            }
            entity.setFields(new HashSet<>(this.fields));
        }
        return entity;
    }
}
