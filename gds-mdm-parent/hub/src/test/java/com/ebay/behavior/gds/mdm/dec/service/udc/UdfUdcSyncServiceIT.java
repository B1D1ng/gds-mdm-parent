package com.ebay.behavior.gds.mdm.dec.service.udc;

import com.ebay.behavior.gds.mdm.common.model.Metadata;
import com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType;
import com.ebay.behavior.gds.mdm.commonTestUtil.TestRequestContextUtils;
import com.ebay.behavior.gds.mdm.dec.model.Dataset;
import com.ebay.behavior.gds.mdm.dec.model.DatasetPhysicalStorageMapping;
import com.ebay.behavior.gds.mdm.dec.model.LdmBaseEntity;
import com.ebay.behavior.gds.mdm.dec.model.LdmEntity;
import com.ebay.behavior.gds.mdm.dec.model.LdmField;
import com.ebay.behavior.gds.mdm.dec.model.Namespace;
import com.ebay.behavior.gds.mdm.dec.model.PhysicalStorage;
import com.ebay.behavior.gds.mdm.dec.model.Pipeline;
import com.ebay.behavior.gds.mdm.dec.model.enums.CodeLanguageType;
import com.ebay.behavior.gds.mdm.dec.model.enums.NamespaceType;
import com.ebay.behavior.gds.mdm.dec.model.enums.ViewType;
import com.ebay.behavior.gds.mdm.dec.service.DatasetService;
import com.ebay.behavior.gds.mdm.dec.service.LdmEntityService;
import com.ebay.behavior.gds.mdm.dec.service.NamespaceService;
import com.ebay.behavior.gds.mdm.dec.service.PhysicalStorageService;
import com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType.DATASET;
import static com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType.LDM;
import static com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType.SIGNAL;
import static com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType.TRANSFORMATION;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils.IT_TEST_USER;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils.ldmFieldSignalMappingEmpty;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestUtils.INTEGRATION_TEST;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestUtils.getRandomString;
import static com.ebay.behavior.gds.mdm.dec.util.ApiConstants.MIN_VERSION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ActiveProfiles(IT)
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class UdfUdcSyncServiceIT {

    @Autowired
    private UdcSyncService service;

    @Autowired
    private NamespaceService namespaceService;

    @Autowired
    private LdmEntityService entityService;

    @Autowired
    private DatasetService datasetService;

    @Autowired
    private PhysicalStorageService storageService;

    @MockitoBean
    private MetadataWriteService writeService;
    private Dataset dataset;
    private Long baseLdmEntityId;
    private Long derivedLdmEntityId;
    private final Long signalId = 1L;
    private LdmEntity baseLdmRaw;
    private LdmEntity baseLdmSnapshot;
    private LdmEntity derivedLdmView;

    @BeforeAll
    void setUpAll() {
        TestRequestContextUtils.setUser(IT_TEST_USER);
    }

    @BeforeEach
    void setUp() {
        // set up base LDM entity, views & fields
        Namespace baseNamespace = Namespace.builder().name(getRandomString()).type(NamespaceType.BASE).owners("gds").build();
        baseNamespace = namespaceService.create(baseNamespace);

        LdmField field = TestModelUtils.ldmFieldEmpty();
        var signalMapping = ldmFieldSignalMappingEmpty();
        signalMapping.setId(signalId);
        field.setSignalMapping(Set.of(signalMapping));

        baseLdmRaw = TestModelUtils.ldmEntityEmpty(baseNamespace.getId());
        baseLdmRaw.setFields(Set.of(field));
        baseLdmRaw = entityService.create(baseLdmRaw);
        baseLdmEntityId = baseLdmRaw.getBaseEntityId();

        baseLdmSnapshot = TestModelUtils.ldmEntityEmpty(baseNamespace.getId());
        baseLdmSnapshot.setFields(Set.of(field));
        baseLdmSnapshot.setViewType(ViewType.SNAPSHOT);
        baseLdmSnapshot.setBaseEntityId(baseLdmEntityId);
        entityService.create(baseLdmSnapshot);

        // set up derived LDM entity
        Namespace domainNamespace = Namespace.builder().name(getRandomString()).type(NamespaceType.DOMAIN).owners("domain").build();
        domainNamespace = namespaceService.create(domainNamespace);

        derivedLdmView = LdmEntity.builder().name(getRandomString()).viewType(ViewType.NONE).fields(Set.of(field))
                .namespaceId(domainNamespace.getId()).codeLanguage(CodeLanguageType.PYTHON).codeContent("sample code")
                .upstreamLdm(String.valueOf(baseLdmRaw.getId())).build();
        derivedLdmView = entityService.create(derivedLdmView);
        derivedLdmEntityId = derivedLdmView.getBaseEntityId();

        // set up dataset
        dataset = TestModelUtils.dataset(derivedLdmView.getId(), derivedLdmView.getVersion(), domainNamespace.getId());
        dataset = datasetService.create(dataset);
        dataset.setVersion(MIN_VERSION);
    }

    @Test
    void syncLdm() {
        var expectedEntityId = Metadata.toEntityId(LDM, derivedLdmEntityId);
        when(writeService.upsertLogicalDataModel(any(LdmBaseEntity.class), any(Namespace.class), anySet(), anySet())).thenReturn(expectedEntityId);

        var idWithStatus = service.syncLdm(derivedLdmEntityId);
        assertThat(idWithStatus.getId()).isEqualTo(derivedLdmEntityId);
        assertThat(idWithStatus.getHttpStatusCode()).isEqualTo(200);
        assertThat(idWithStatus.isOk()).isTrue();
    }

    @Test
    void syncLdm_NewVersion() {
        derivedLdmView.setUpstreamLdm(String.valueOf(baseLdmSnapshot.getId()));
        derivedLdmView = entityService.saveAsNewVersion(derivedLdmView, null, false);

        var expectedEntityId = Metadata.toEntityId(LDM, derivedLdmEntityId);
        when(writeService.upsertLogicalDataModel(any(LdmBaseEntity.class), any(Namespace.class), anySet(), anySet())).thenReturn(expectedEntityId);

        var idWithStatus = service.syncLdm(derivedLdmEntityId);
        assertThat(idWithStatus.getId()).isEqualTo(derivedLdmEntityId);
        assertThat(idWithStatus.getHttpStatusCode()).isEqualTo(200);
        assertThat(idWithStatus.isOk()).isTrue();
    }

    @Test
    void syncDataset() {
        // set up storage
        PhysicalStorage storage = TestModelUtils.physicalStorage();
        storage = storageService.create(storage);

        DatasetPhysicalStorageMapping mapping = TestModelUtils.datasetPhysicalStorageMapping(dataset.getId(), dataset.getVersion(), storage.getId());
        datasetService.savePhysicalMappings(dataset.getId(), Set.of(mapping), null);

        Pipeline pipeline = TestModelUtils.pipeline();
        storageService.savePipelineMappings(storage.getId(), Set.of(pipeline), null);

        var expectedEntityId = Metadata.toEntityId(DATASET, dataset.getId());
        var expectedResultMap = Map.of(DATASET, expectedEntityId);

        when(writeService.upsertConsumableDataset(any(Dataset.class), any(Namespace.class), anyLong(), anyString(), any(), any()))
                .thenReturn(expectedResultMap);

        var idWithStatus = service.syncDataset(dataset.getId());
        assertThat(idWithStatus.getId()).isEqualTo(dataset.getId());
        assertThat(idWithStatus.getHttpStatusCode()).isEqualTo(200);
        assertThat(idWithStatus.isOk()).isTrue();
    }

    @Test
    void syncDataset_NewVersion() {
        dataset.setLdmEntityId(baseLdmRaw.getId());
        dataset = datasetService.saveAsNewVersion(dataset);

        var expectedEntityId = Metadata.toEntityId(DATASET, dataset.getId());
        var expectedResultMap = Map.of(DATASET, expectedEntityId);

        when(writeService.upsertConsumableDataset(any(Dataset.class), any(Namespace.class), anyLong(), anyString(), any(), any()))
                .thenReturn(expectedResultMap);

        var idWithStatus = service.syncDataset(dataset.getId());
        assertThat(idWithStatus.getId()).isEqualTo(dataset.getId());
        assertThat(idWithStatus.getHttpStatusCode()).isEqualTo(200);
        assertThat(idWithStatus.isOk()).isTrue();
    }

    @Test
    void syncSignalLineage_WithSignalId() {
        var expectedEntityId = "transformation:123";
        var expectedResultMap = Map.of(TRANSFORMATION, expectedEntityId);
        when(writeService.upsertSignalToLdmLineage(anyLong(), any())).thenReturn(expectedResultMap);

        var idWithStatus = service.syncSignalLineage(baseLdmEntityId, 100L);
        assertThat(idWithStatus.getId()).isEqualTo(baseLdmEntityId);
        assertThat(idWithStatus.getHttpStatusCode()).isEqualTo(200);
        assertThat(idWithStatus.isOk()).isTrue();
    }

    @Test
    void syncSignalLineage_WithoutSignalId() {
        var expectedEntityId = "transformation:123";
        var expectedResultMap = Map.of(TRANSFORMATION, expectedEntityId);
        when(writeService.upsertSignalToLdmLineage(anyLong(), any())).thenReturn(expectedResultMap);

        var idWithStatus = service.syncSignalLineage(baseLdmEntityId, null);
        assertThat(idWithStatus.getId()).isEqualTo(baseLdmEntityId);
        assertThat(idWithStatus.getHttpStatusCode()).isEqualTo(200);
        assertThat(idWithStatus.isOk()).isTrue();
    }

    @Test
    void deleteLdm() {
        var expectedEntityId = Metadata.toEntityId(LDM, derivedLdmEntityId);
        when(writeService.deleteLogicalDataModel(anyLong())).thenReturn(expectedEntityId);

        var result = service.delete(LDM, String.valueOf(derivedLdmEntityId));
        assertThat(result).isEqualTo(expectedEntityId);
    }

    @Test
    void deleteDataset() {
        var expectedEntityId = Metadata.toEntityId(DATASET, dataset.getId());
        when(writeService.deleteConsumableDataset(anyLong())).thenReturn(expectedEntityId);

        var result = service.delete(DATASET, String.valueOf(dataset.getId()));
        assertThat(result).isEqualTo(expectedEntityId);
    }

    @Test
    void deleteLineage() {
        var expectedEntityId = "transformation:123";
        when(writeService.deleteLineage(any(UdcEntityType.class), anyString())).thenReturn(expectedEntityId);

        var result = service.delete(TRANSFORMATION, "123");
        assertThat(result).isEqualTo(expectedEntityId);
    }

    @Test
    void delete_TypeNotSupported() {
        assertThatThrownBy(() -> service.delete(SIGNAL, "123")).isInstanceOf(IllegalArgumentException.class);
    }
}