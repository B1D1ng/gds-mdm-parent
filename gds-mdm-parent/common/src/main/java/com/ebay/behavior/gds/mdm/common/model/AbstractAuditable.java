package com.ebay.behavior.gds.mdm.common.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.val;
import org.javers.core.metamodel.annotation.DiffIgnore;

import java.sql.Timestamp;

import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.getRequestUser;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.HANDLER;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.HIBERNATE_LAZY_INITIALIZER;
import static com.ebay.behavior.gds.mdm.common.util.TimeUtils.toNowSqlTimestamp;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Data
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@MappedSuperclass
@JsonIgnoreProperties(ignoreUnknown = true, value = {HIBERNATE_LAZY_INITIALIZER, HANDLER})
public abstract class AbstractAuditable extends AbstractModel implements Auditable {

    @DiffIgnore
    @Column(name = "create_by")
    private String createBy;

    @DiffIgnore
    @Column(name = "update_by")
    private String updateBy;

    @DiffIgnore
    @Column(name = "create_date")
    private Timestamp createDate;

    @DiffIgnore
    @Column(name = "update_date")
    private Timestamp updateDate;

    /**
     * Automatically set create and update date to current time.
     */
    @PrePersist
    public void onCreate() {
        if (nonNull(createBy) && nonNull(createDate) && nonNull(updateBy) && nonNull(updateDate)) {
            return;
        }

        val now = toNowSqlTimestamp();
        this.createDate = now;
        this.updateDate = now;

        if (isNull(getCreateBy())) {
            var user = getRequestUser();
            setCreateBy(user);
            setUpdateBy(user);
        }
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
