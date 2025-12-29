package com.ebay.behavior.gds.mdm.signal.common.model;

import com.ebay.behavior.gds.mdm.common.model.AbstractModel;
import com.ebay.behavior.gds.mdm.common.serde.LazyObjectSerializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.util.Set;

@Data
@Accessors(chain = true)
@SuperBuilder(toBuilder = true)
@ToString(exclude = {"tags"})
@EqualsAndHashCode(callSuper = true, exclude = {"tags"})
@NoArgsConstructor
@Entity
@Table(name = "soj_event")
public class SojEvent extends AbstractModel {

    @NotBlank
    @Column(name = "action")
    private String action;

    @NotNull
    @Column(name = "page_id")
    private Long pageId;

    @Column(name = "module_id")
    private Long moduleId;

    @Column(name = "click_id")
    private Long clickId;

    @JsonSerialize(using = LazyObjectSerializer.class)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "soj_event_tag_map",
            joinColumns = @JoinColumn(name = "soj_event_id", referencedColumnName = "id", insertable = false, updatable = false),
            inverseJoinColumns = @JoinColumn(name = "soj_tag_id", referencedColumnName = "id", insertable = false, updatable = false)
    )
    private Set<SojBusinessTag> tags;

    public SojEvent(String action, Long pageId, Long moduleId, Long clickId) {
        this.action = action;
        this.pageId = pageId;
        this.moduleId = moduleId;
        this.clickId = clickId;
    }

    public SojEvent(HadoopSojEvent event) {
        this.action = event.getEactn();
        this.pageId = event.getPageId();
        this.moduleId = event.getModuleId();
        this.clickId = event.getClickId();
    }

    public String toKey() {
        return String.format("%s;%s;%s;%s", action, pageId, moduleId, clickId);
    }
}
