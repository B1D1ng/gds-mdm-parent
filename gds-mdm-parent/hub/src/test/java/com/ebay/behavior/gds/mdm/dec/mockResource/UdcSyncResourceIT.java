package com.ebay.behavior.gds.mdm.dec.mockResource;

import com.ebay.behavior.gds.mdm.common.model.IdWithStatus;
import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.dec.model.Dataset;
import com.ebay.behavior.gds.mdm.dec.model.LdmBaseEntity;
import com.ebay.behavior.gds.mdm.dec.model.LdmEntity;
import com.ebay.behavior.gds.mdm.dec.model.LdmField;
import com.ebay.behavior.gds.mdm.dec.model.Namespace;
import com.ebay.behavior.gds.mdm.dec.model.enums.NamespaceType;
import com.ebay.behavior.gds.mdm.dec.model.enums.ViewType;
import com.ebay.behavior.gds.mdm.dec.service.DatasetService;
import com.ebay.behavior.gds.mdm.dec.service.LdmEntityService;
import com.ebay.behavior.gds.mdm.dec.service.NamespaceService;
import com.ebay.behavior.gds.mdm.dec.service.udc.MetadataWriteService;
import com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType.DATASET;
import static com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType.TRANSFORMATION;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils.ldmFieldSignalMappingEmpty;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestUtils.getRandomLong;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestUtils.getRandomString;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestUtils.requestSpec;
import static com.ebay.behavior.gds.mdm.dec.util.ApiConstants.UDC_API;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doReturn;
import static org.springframework.http.HttpStatus.EXPECTATION_FAILED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

class UdcSyncResourceIT extends AbstractResourceTest {

    @MockitoBean
    private MetadataWriteService udcWriteService;

    @Autowired
    private NamespaceService namespaceService;

    @Autowired
    private LdmEntityService entityService;

    @Autowired
    private DatasetService datasetService;

    private Dataset dataset;
    private Long baseLdmEntityId;
    private Namespace baseNamespace;

    @BeforeEach
    void setUp() {
        url = getBaseUrl() + V1 + UDC_API;

        // set up base LDM entity, views & fields
        Namespace baseNamespace = Namespace.builder().name(getRandomString()).type(NamespaceType.BASE).owners("gds").build();
        baseNamespace = namespaceService.create(baseNamespace);

        LdmField field = TestModelUtils.ldmFieldEmpty();
        var signalMapping = ldmFieldSignalMappingEmpty();
        field.setSignalMapping(Set.of(signalMapping));

        LdmEntity baseLdmRaw = TestModelUtils.ldmEntityEmpty(baseNamespace.getId());
        baseLdmRaw.setFields(Set.of(field));
        baseLdmRaw = entityService.create(baseLdmRaw);
        baseLdmEntityId = baseLdmRaw.getBaseEntityId();

        LdmEntity baseLdmSnapshot = TestModelUtils.ldmEntityEmpty(baseNamespace.getId());
        baseLdmSnapshot.setFields(Set.of(field));
        baseLdmSnapshot.setViewType(ViewType.SNAPSHOT);
        baseLdmSnapshot.setBaseEntityId(baseLdmEntityId);
        baseLdmSnapshot = entityService.create(baseLdmSnapshot);

        // set up derived LDM entity
        Namespace domainNamespace = Namespace.builder().name(getRandomString()).type(NamespaceType.DOMAIN).owners("domain").build();
        domainNamespace = namespaceService.create(domainNamespace);

        // set up dataset
        dataset = TestModelUtils.dataset(baseLdmSnapshot.getId(), baseLdmSnapshot.getVersion(), domainNamespace.getId());
        dataset = datasetService.create(dataset);
    }

    @Test
    void syncLdm() {
        doReturn("testEntityId").when(udcWriteService).upsertLogicalDataModel(any(LdmBaseEntity.class), any(Namespace.class), anySet(), anySet());
        var persisted = requestSpec().when().put(url + "/ldms/" + baseLdmEntityId)
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", IdWithStatus.class);

        assertThat(persisted.getId()).isEqualTo(baseLdmEntityId);
    }

    @Test
    void syncLdm_NotFound() {
        requestSpec().when().put(url + "/ldms/" + getRandomLong())
                .then().statusCode(EXPECTATION_FAILED.value())
                .and().contentType(APPLICATION_JSON_VALUE);
    }

    @Test
    void syncDataset() {
        doReturn(Map.of(DATASET, "testEntityId")).when(udcWriteService).upsertConsumableDataset(any(Dataset.class), any(Namespace.class),
                anyLong(), any(), any(), any());
        var persisted = requestSpec().when().put(url + "/datasets/" + dataset.getId())
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", IdWithStatus.class);

        assertThat(persisted.getId()).isEqualTo(dataset.getId());
    }

    @Test
    void syncDataset_NotFound() {
        requestSpec().when().put(url + "/datasets/" + getRandomLong())
                .then().statusCode(EXPECTATION_FAILED.value())
                .and().contentType(APPLICATION_JSON_VALUE);
    }

    @Test
    void syncLdmSignalLineage() {
        doReturn(Map.of(TRANSFORMATION, "testEntityId")).when(udcWriteService).upsertSignalToLdmLineage(anyLong(), anySet());
        var persisted = requestSpec().when().put(url + "/ldms/" + baseLdmEntityId + "/signal-lineages")
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", IdWithStatus.class);

        assertThat(persisted.getId()).isNotNull();
        assertThat(persisted.isOk()).isTrue();
    }

    @Test
    void syncLdmSignalLineage_NotFound() {
        requestSpec().when().put(url + "/ldms/" + getRandomLong() + "/signal-lineages")
                .then().statusCode(EXPECTATION_FAILED.value())
                .and().contentType(APPLICATION_JSON_VALUE);
    }

    @Test
    void delete() {
        doReturn("testEntityId").when(udcWriteService).deleteConsumableDataset(anyLong());
        requestSpec()
                .when().delete(url + "/DATASET/" + getRandomLong())
                .then().statusCode(HttpStatus.NO_CONTENT.value());
    }
}