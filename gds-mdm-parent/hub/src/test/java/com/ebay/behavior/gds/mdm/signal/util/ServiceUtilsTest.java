package com.ebay.behavior.gds.mdm.signal.util;

import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.common.util.TimeUtils;
import com.ebay.behavior.gds.mdm.signal.common.model.Field;
import com.ebay.behavior.gds.mdm.signal.common.model.FieldTemplate;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedEvent;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedSignal;
import com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils;

import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;
import static com.ebay.behavior.gds.mdm.common.model.VersionedId.MIN_VERSION;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedSignal;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.updateEventRequest;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomLong;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomSmallString;
import static org.assertj.core.api.Assertions.assertThat;

class ServiceUtilsTest {

    @Test
    void copyModelProperties() {
        var source = updateEventRequest().withId(123L).withRevision(2);
        var target = new UnstagedEvent();

        ServiceUtils.copyModelProperties(source, target);

        // copied properties
        assertThat(target.getDescription()).isEqualTo(source.getDescription());
        assertThat(target.getExpression()).isEqualTo(source.getExpression());
        assertThat(target.getExpressionType()).isEqualTo(source.getExpressionType());
        assertThat(target.getFsmOrder()).isEqualTo(source.getFsmOrder());
        assertThat(target.getCardinality()).isEqualTo(source.getCardinality());
        assertThat(target.getRevision()).isEqualTo(source.getRevision());
        assertThat(target.getId()).isNull();

        //not copied properties
        assertThat(target.getCreateBy()).isNull();
        assertThat(target.getUpdateBy()).isNull();
        assertThat(target.getCreateDate()).isNull();
        assertThat(target.getUpdateDate()).isNull();
    }

    @Test
    void copyModelProperties_byInterface() {
        var source = TestModelUtils.unstagedField(VersionedId.of(getRandomLong(), MIN_VERSION)).toBuilder()
                .id(getRandomLong()).revision(2).build();
        source.setCreateBy(getRandomSmallString());
        source.setUpdateBy(getRandomSmallString());
        source.setCreateDate(TimeUtils.toNowSqlTimestamp());
        source.setUpdateDate(TimeUtils.toNowSqlTimestamp());
        var target = new FieldTemplate();

        ServiceUtils.copyModelProperties((Field) source, (Field) target);

        assertThat(target.getName()).isEqualTo(source.getName());
        assertThat(target.getDescription()).isEqualTo(source.getDescription());
        assertThat(target.getTag()).isEqualTo(source.getTag());
        assertThat(target.getJavaType()).isEqualTo(source.getJavaType());
        assertThat(target.getExpression()).isEqualTo(source.getExpression());
        assertThat(target.getExpressionType()).isEqualTo(source.getExpressionType());
        assertThat(target.getIsMandatory()).isEqualTo(source.getIsMandatory());

        assertThat(target.getRevision()).isEqualTo(source.getRevision());
        assertThat(target.getId()).isNull();
        assertThat(target.getCreateBy()).isEqualTo(source.getCreateBy());
        assertThat(target.getUpdateBy()).isEqualTo(source.getUpdateBy());
        assertThat(target.getCreateDate()).isEqualTo(source.getCreateDate());
        assertThat(target.getUpdateDate()).isEqualTo(source.getUpdateDate());
    }

    @Test
    void copyModelProperties_excludeId() {
        var source = updateEventRequest().withId(123L).withRevision(2);
        var target = new UnstagedEvent();

        ServiceUtils.copyModelProperties(source, target, Set.of(ID));

        // copied properties
        assertThat(target.getDescription()).isEqualTo(source.getDescription());
        assertThat(target.getExpression()).isEqualTo(source.getExpression());
        assertThat(target.getExpressionType()).isEqualTo(source.getExpressionType());
        assertThat(target.getFsmOrder()).isEqualTo(source.getFsmOrder());
        assertThat(target.getCardinality()).isEqualTo(source.getCardinality());
        assertThat(target.getRevision()).isEqualTo(source.getRevision());

        // copied null properties
        assertThat(target.getId()).isNull();
        assertThat(target.getCreateBy()).isNull();
        assertThat(target.getUpdateBy()).isNull();
        assertThat(target.getCreateDate()).isNull();
        assertThat(target.getUpdateDate()).isNull();
    }

    @Test
    void copyOverwriteAllProperties_withNull() {
        var source = unstagedSignal(123L);
        source.setId(123L);
        source.setVersion(1);
        var target = new UnstagedSignal();
        target.setName(getRandomSmallString());
        target.setDescription(getRandomSmallString());
        target.setRetentionPeriod(2L);
        target.setUuidGeneratorExpression(getRandomSmallString());
        target.setUuidGeneratorType(getRandomSmallString());

        ServiceUtils.copyOverwriteAllProperties(source, target);

        //copied all properties with null
        assertThat(target.getId()).isEqualTo(source.getId());
        assertThat(target.getName()).isEqualTo(source.getName());
        assertThat(target.getDescription()).isEqualTo(source.getDescription());
        assertThat(target.getRetentionPeriod()).isEqualTo(source.getRetentionPeriod());
        assertThat(target.getUuidGeneratorExpression()).isEqualTo(source.getUuidGeneratorExpression()).isNull();
        assertThat(target.getUuidGeneratorType()).isEqualTo(source.getUuidGeneratorType()).isNull();

    }

    @Test
    void copySignalProperties_ignoredProperties() {
        var source = unstagedSignal(123L);
        source.setId(123L);
        source.setRevision(2);
        source.setVersion(1);
        source.setCreateDate(TimeUtils.toNowSqlTimestamp());
        source.setUpdateDate(TimeUtils.toNowSqlTimestamp());
        var target = new UnstagedSignal();

        ServiceUtils.copySignalProperties(source, target);

        // not copied properties
        assertThat(target.getId()).isNull();
        assertThat(target.getVersion()).isNull();
        assertThat(target.getRevision()).isNull();
        assertThat(target.getCreateBy()).isNull();
        assertThat(target.getUpdateBy()).isNull();
        assertThat(target.getCreateDate()).isNull();
        assertThat(target.getUpdateDate()).isNull();
    }

    @Test
    void setModelProperty() {
        var model = new UnstagedEvent();

        val numericValue = getRandomLong();
        ServiceUtils.setModelProperty(model, ID, numericValue);
        assertThat(model.getId()).isEqualTo(numericValue);

        val strValue = getRandomSmallString();
        ServiceUtils.setModelProperty(model, "name", strValue);
        assertThat(model.getName()).isEqualTo(strValue);

        val set = Set.of(1L, 2L, 3L);
        ServiceUtils.setModelProperty(model, "pageIds", set);
        assertThat(model.getPageIds()).isEqualTo(set);
    }
}