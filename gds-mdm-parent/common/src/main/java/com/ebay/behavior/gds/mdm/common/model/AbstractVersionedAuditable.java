package com.ebay.behavior.gds.mdm.common.model;

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
public abstract class AbstractVersionedAuditable extends AbstractVersionedModel implements VersionedAuditable {

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

        if (nonNull(createBy) && nonNull(createDate) && nonNull(updateBy) && nonNull(updateDate)) {
            return;
        }

        val now = toNowSqlTimestamp();
        this.createDate = now;
        this.updateDate = now;

        var user = getRequestUser();
        setCreateBy(user);
        setUpdateBy(user);
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
        var user = getRequestUser();
        setUpdateBy(user);
    }
}
