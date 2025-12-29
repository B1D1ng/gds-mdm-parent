package com.ebay.behavior.gds.mdm.signal.common.testUtil;

import com.ebay.behavior.gds.mdm.common.model.ExpressionType;
import com.ebay.behavior.gds.mdm.common.model.JavaType;
import com.ebay.behavior.gds.mdm.common.model.SurfaceType;
import com.ebay.behavior.gds.mdm.common.model.UdcDataSourceType;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.signal.common.model.EventSource;
import com.ebay.behavior.gds.mdm.signal.common.model.Plan;
import com.ebay.behavior.gds.mdm.signal.common.model.PlanStatus;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedAttribute;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedEvent;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedField;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedSignal;

import lombok.experimental.UtilityClass;
import lombok.val;

import static com.ebay.behavior.gds.mdm.signal.common.testUtil.TestUtils.getRandomEmail;
import static com.ebay.behavior.gds.mdm.signal.common.testUtil.TestUtils.getRandomSmallString;
import static com.ebay.behavior.gds.mdm.signal.common.testUtil.TestUtils.getRandomString;

@UtilityClass
@SuppressWarnings("PMD.CouplingBetweenObjects")
public class TestModelUtils {

    public static final String IT_ = "IT_";
    private static final String VI_DOMAIN = "VI";

    public static final String CJS = "CJS";
    public static final String EJS = "EJS";
    public static final String ITEM = "ITEM";

    public static final Long CJS_PLATFORM_ID = 1L;
    public static final Long EJS_PLATFORM_ID = 2L;
    public static final Long ITEM_PLATFORM_ID = 3L;

    public static Plan plan() {
        return Plan.builder()
                .name(getRandomString())
                .description(getRandomString())
                .teamDls(getRandomEmail())
                .owners("IT_TEST_USER")
                .jiraProject(IT_ + getRandomString(5))
                .domain(VI_DOMAIN)
                .status(PlanStatus.CREATED)
                .build();
    }

    public static UnstagedEvent unstagedEvent() {
        return UnstagedEvent.builder()
                .name(getRandomString())
                .description(getRandomString())
                .type(getRandomSmallString())
                .source(EventSource.SOJ)
                .surfaceType(SurfaceType.RAPTOR_IO)
                .githubRepositoryUrl("https://github.corp.ebay.com/customer-journey-signal/gds-mdm-parent")
                .expression(getRandomString())
                .expressionType(ExpressionType.LITERAL)
                .build();
    }

    public static UnstagedAttribute unstagedAttribute(long eventId) {
        return UnstagedAttribute.builder()
                .eventId(eventId)
                .tag(getRandomSmallString())
                .description(getRandomString())
                .javaType(JavaType.STRING)
                .schemaPath(getRandomString())
                .isStoreInState(true)
                .build();
    }

    public static UnstagedField unstagedField(VersionedId signalId) {
        val javaType = JavaType.STRING;
        return UnstagedField.builder()
                .signalId(signalId.getId())
                .signalVersion(signalId.getVersion())
                .name(getRandomString())
                .description(getRandomString())
                .tag(getRandomString())
                .javaType(javaType)
                .avroSchema(javaType.toSchema())
                .isMandatory(false)
                .isCached(false)
                .eventTypes("PAGE_SERVE")
                .expression(getRandomString())
                .expressionType(ExpressionType.JEXL)
                .build();
    }

    public static UnstagedSignal unstagedSignal(long planId) {
        return UnstagedSignal.builder()
                .planId(planId)
                .name(getRandomSmallString())
                .description(getRandomString())
                .domain(VI_DOMAIN)
                .retentionPeriod(1L)
                .owners(getRandomSmallString())
                .type(getRandomSmallString())
                .platformId(CJS_PLATFORM_ID)
                .dataSource(UdcDataSourceType.TEST)
                .refVersion(1)
                .build();
    }
}
