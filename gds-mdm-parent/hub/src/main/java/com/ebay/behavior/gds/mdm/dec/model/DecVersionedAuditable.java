package com.ebay.behavior.gds.mdm.dec.model;

import com.ebay.behavior.gds.mdm.common.model.AbstractVersionedModel;
import com.ebay.behavior.gds.mdm.common.model.VersionedAuditable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.val;

import java.sql.Timestamp;
import java.util.Objects;

import static com.ebay.behavior.gds.mdm.common.model.VersionedId.MIN_VERSION;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.getRequestUser;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.HANDLER;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.HIBERNATE_LAZY_INITIALIZER;
import static com.ebay.behavior.gds.mdm.common.util.TimeUtils.toNowSqlTimestamp;
import static java.util.Objects.nonNull;

@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@MappedSuperclass
@JsonIgnoreProperties(ignoreUnknown = true, value = {HIBERNATE_LAZY_INITIALIZER, HANDLER})
public abstract class DecVersionedAuditable extends AbstractVersionedModel implements VersionedAuditable {

    @Column(name = "create_by")
    private String createBy;

    @Column(name = "update_by")
    private String updateBy;

    @Column(name = "create_date")
    private Timestamp createDate;

    @Column(name = "update_date")
    private Timestamp updateDate;

    /**
     * Automatically set create and update date to current time.
     */
    @PrePersist
    public void onCreate() {
        if (Objects.isNull(getVersion())) {
            setVersion(MIN_VERSION);
        }

        if (allAuditFieldsPresent()) {
            return;
        }

        val now = toNowSqlTimestamp();

        if (hasCreateAndUpdateButNoUpdateDate()) {
            this.updateDate = now;
            return;
        }

        if (hasCreateFieldsOnly()) {
            this.updateBy = createBy;
            this.updateDate = createDate;
            return;
        }

        this.createDate = now;
        this.updateDate = now;

        if (nonNull(createBy)) {
            this.updateBy = createBy;
            return;
        }

        var user = getRequestUser();
        setCreateBy(user);
        setUpdateBy(user);
    }

    private boolean allAuditFieldsPresent() {
        return nonNull(createBy) && nonNull(createDate) && nonNull(updateBy) && nonNull(updateDate);
    }

    private boolean hasCreateAndUpdateButNoUpdateDate() {
        return nonNull(createBy) && nonNull(createDate) && nonNull(updateBy);
    }

    private boolean hasCreateFieldsOnly() {
        return nonNull(createBy) && nonNull(createDate);
    }

    /**
     * Automatically set update date to current time.
     */
    @PreUpdate
    public void onUpdate() {
        if (nonNull(updateBy) && nonNull(updateDate)) {
            return;
        }

        this.updateDate = toNowSqlTimestamp();

        if (nonNull(updateBy)) {
            return;
        }

        var user = getRequestUser();
        setUpdateBy(user);
    }
}
