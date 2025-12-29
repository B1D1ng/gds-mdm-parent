package com.ebay.behavior.gds.mdm.signal.mockResource;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.exception.UdcException;
import com.ebay.behavior.gds.mdm.common.model.Metadata;
import com.ebay.behavior.gds.mdm.common.model.UdcDataSourceType;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.common.model.external.udc.Entity;
import com.ebay.behavior.gds.mdm.common.service.udc.MetadataReadService;
import com.ebay.behavior.gds.mdm.signal.common.model.Plan;
import com.ebay.behavior.gds.mdm.signal.common.model.PlanStatus;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedSignal;
import com.ebay.behavior.gds.mdm.signal.common.service.MetadataWriteService;
import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.signal.service.PlanService;
import com.ebay.behavior.gds.mdm.signal.service.StagedSignalService;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedAttributeService;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedEventService;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedFieldService;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedSignalService;

import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.CompletionStatus.COMPLETED;
import static com.ebay.behavior.gds.mdm.common.model.Environment.PRODUCTION;
import static com.ebay.behavior.gds.mdm.common.model.Environment.STAGING;
import static com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType.SIGNAL;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.DATA_SOURCE;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.plan;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.stagedSignal;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedAttribute;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedEvent;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedField;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedSignal;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomLong;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomString;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpec;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpecWithBody;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.UDC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.EXPECTATION_FAILED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

class StagingUdcInjectionResourceIT extends AbstractResourceTest {

    @Autowired
    private PlanService planService;

    @Autowired
    private UnstagedEventService unstagedEventService;

    @Autowired
    private UnstagedFieldService unstagedFieldService;

    @Autowired
    private UnstagedAttributeService unstagedAttributeService;

    @Autowired
    private UnstagedSignalService unstagedSignalService;

    @Autowired
    private StagedSignalService stagedSignalService;

    @MockitoBean
    private MetadataWriteService metadataWriteService;

    @MockitoBean
    private MetadataReadService metadataReadService;

    private Plan plan;
    private final UdcDataSourceType dataSource = UdcDataSourceType.TEST;

    @BeforeEach
    void setUp() {
        url = getBaseUrl() + V1 + UDC;
        Mockito.reset(metadataWriteService);
        Mockito.reset(metadataReadService);
    }

    private VersionedId createSignal() {
        plan = plan().setStatus(PlanStatus.CREATED);
        plan = planService.create(plan);
        var planId = plan.getId();

        var event = unstagedEvent().toBuilder()
                .name("testName")
                .description("testDescription")
                .build();
        event = unstagedEventService.create(event);
        var eventId = event.getId();

        var attribute = unstagedAttribute(eventId).toBuilder().tag("testTag").build();
        attribute = unstagedAttributeService.create(attribute);
        var attributeId = attribute.getId();

        var signal = unstagedSignal(planId).toBuilder()
                .name("testName")
                .description("testDescription")
                .completionStatus(COMPLETED)
                .environment(STAGING)
                .build();
        var signalId = unstagedSignalService.create(signal).getSignalId();
        unstagedSignalService.getById(signalId);

        var field = unstagedField(signalId).toBuilder()
                .name("testName")
                .description("testDescription")
                .tag("testTag")
                .build();
        unstagedFieldService.create(field, Set.of(attributeId));

        return signal.getSignalId();
    }

    @Test
    void get_badEntityId_417() {
        val entityId = getRandomString();
        when(metadataReadService.getEntityById(entityId)).thenThrow(new DataNotFoundException("not found"));

        requestSpec()
                .when().get(url + '/' + entityId)
                .then().statusCode(EXPECTATION_FAILED.value())
                .and().contentType(APPLICATION_JSON_VALUE);
    }

    @Test
    void get_badSignalId_417() {
        val signalId = getRandomLong();
        when(metadataReadService.getEntityById(SIGNAL, signalId)).thenThrow(new DataNotFoundException("not found"));

        requestSpec()
                .when().get(url + "/signal/" + signalId)
                .then().statusCode(EXPECTATION_FAILED.value())
                .and().contentType(APPLICATION_JSON_VALUE);
    }

    @Test
    void injectAndImport() {
        var signalId = createSignal();
        var signal = unstagedSignalService.getByIdWithAssociationsRecursive(signalId);
        plan.setStatus(PlanStatus.STAGING);
        planService.update(plan);

        var expectedEntityId = Metadata.toEntityId(SIGNAL, signalId.getId());
        when(metadataWriteService.upsert(any(UnstagedSignal.class), eq(dataSource))).thenReturn(expectedEntityId);

        val entityId = requestSpecWithBody(signal)
                .queryParam(DATA_SOURCE, dataSource.getValue())
                .when().post(url)
                .then().statusCode(CREATED.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().asPrettyString();

        assertThat(entityId).isNotBlank();
    }

    @Test
    void injectAndImport_udcError_500() {
        var signal = unstagedSignal(1L).toBuilder()
                .name("testName")
                .description("testDescription")
                .completionStatus(COMPLETED)
                .environment(STAGING)
                .build();
        when(metadataWriteService.upsert(any(UnstagedSignal.class), eq(dataSource))).thenThrow(new UdcException("requestId", "message"));

        requestSpecWithBody(signal)
                .queryParam(DATA_SOURCE, dataSource.getValue())
                .when().post(url)
                .then().statusCode(INTERNAL_SERVER_ERROR.value())
                .and().contentType(APPLICATION_JSON_VALUE);
    }

    @Test
    void updateToProductionEnvironment() {
        plan = plan().setStatus(PlanStatus.CREATED);
        plan = planService.create(plan);

        var stagedSignal = stagedSignal(plan.getId()).toBuilder()
                .id(getRandomLong())
                .version(1)
                .name("testName")
                .description("testDescription")
                .completionStatus(COMPLETED)
                .environment(STAGING)
                .build();

        var signalId = stagedSignal.getSignalId();
        stagedSignalService.create(stagedSignal);

        val updatedId = requestSpecWithBody(signalId)
                .when().patch(url)
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().as(VersionedId.class);

        val updated = stagedSignalService.getById(updatedId);
        assertThat(updatedId).isEqualTo(signalId);
        assertThat(updated.getEnvironment()).isEqualTo(PRODUCTION);
    }

    @Test
    void getByEntityId() {
        var entityId = "entityId";
        val entity = new Entity().setEntityType(SIGNAL).setGraphPk("graphPk");
        when(metadataReadService.getEntityById(entityId)).thenReturn(entity);

        val persistedEntity = requestSpec()
                .when().get(url + '/' + entityId)
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", Entity.class);

        assertThat(persistedEntity).isEqualTo(entity);
    }

    @Test
    void getBySignalId() {
        var signalId = 1L;
        val entity = new Entity().setEntityType(SIGNAL).setGraphPk("graphPk");
        when(metadataReadService.getEntityById(SIGNAL, signalId)).thenReturn(entity);

        val persistedEntity = requestSpec()
                .when().get(url + "/signal/" + signalId)
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", Entity.class);

        assertThat(persistedEntity).isEqualTo(entity);
    }
}