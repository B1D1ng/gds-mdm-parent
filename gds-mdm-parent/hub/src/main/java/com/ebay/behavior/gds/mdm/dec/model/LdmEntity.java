package com.ebay.behavior.gds.mdm.dec.model;

import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.common.serde.LazyObjectSerializer;
import com.ebay.behavior.gds.mdm.dec.model.enums.CodeLanguageType;
import com.ebay.behavior.gds.mdm.dec.model.enums.Environment;
import com.ebay.behavior.gds.mdm.dec.model.enums.LdmStatus;
import com.ebay.behavior.gds.mdm.dec.model.enums.ViewType;
import com.ebay.behavior.gds.mdm.dec.util.StringListConverter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.javers.core.metamodel.annotation.DiffInclude;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Data
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@ToString(exclude = {"fields", "baseEntity", "errorHandlingStorageMappings"})
@EqualsAndHashCode(callSuper = true, exclude = {"fields", "baseEntity", "errorHandlingStorageMappings"})
@IdClass(VersionedId.class)
@Table(name = "dec_ldm_entity")
public class LdmEntity extends DecVersionedAuditable {

    @Id
    @Column(name = "id")
    private Long id;

    @DiffInclude
    @Column(name = "name")
    private String name;

    @NotNull
    @DiffInclude
    @Enumerated(EnumType.STRING)
    @Column(name = "view_type")
    private ViewType viewType;

    @DiffInclude
    @Column(name = "base_entity_id")
    private Long baseEntityId;

    @DiffInclude
    @Column(name = "description")
    private String description;

    @Transient
    private String owners;

    @Transient
    private String jiraProject;

    @Transient
    private String domain;

    @Transient
    private String pk;

    @NotNull
    @DiffInclude
    @Column(name = "namespace_id")
    private Long namespaceId;

    @DiffInclude
    @Column(name = "upstream_ldm")
    private String upstreamLdm;

    @DiffInclude
    @Enumerated(EnumType.STRING)
    @Column(name = "code_language")
    private CodeLanguageType codeLanguage;

    @DiffInclude
    @Column(name = "code_content")
    private String codeContent;

    @DiffInclude
    @Column(name = "generated_sql")
    private String generatedSql;

    @DiffInclude
    @Column(name = "ir")
    private String ir;

    @DiffInclude
    @Column(name = "language_frontend_version")
    private String languageFrontendVersion;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private LdmStatus status = LdmStatus.DRAFT;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "environment")
    private Environment environment = Environment.UNSTAGED;

    @Column(name = "request_Id")
    private Long requestId;

    @Transient
    private String team;

    @Transient
    private String teamDl;

    @DiffInclude
    @Column(name = "udfs")
    @Convert(converter = StringListConverter.class)
    private List<String> udfs;

    @DiffInclude
    @Column(name = "is_dcs")
    private Boolean isDcs;

    @DiffInclude
    @Column(name = "dcs_fields")
    @Convert(converter = StringListConverter.class)
    private List<String> dcsFields;

    @Transient
    private Set<Long> dcsLdms;

    @DiffInclude
    @JsonManagedReference
    @JsonSerialize(using = LazyObjectSerializer.class)
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "entity")
    private Set<LdmField> fields;

    @DiffInclude
    @JsonManagedReference
    @JsonSerialize(using = LazyObjectSerializer.class)
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "entity")
    private Set<LdmErrorHandlingStorageMapping> errorHandlingStorageMappings;

    @Transient
    private LdmBaseEntity baseEntity;

    @JsonIgnore
    public List<String> getUpstreamLdmIds() {
        String upstreamLdmIds = this.getUpstreamLdm();
        if (upstreamLdmIds == null) {
            return new ArrayList<>();
        }
        return this.toList(upstreamLdmIds);
    }
}
