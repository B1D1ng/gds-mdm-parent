package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.common.model.UdcDataSourceType;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.signal.common.model.PlatformLookup;
import com.ebay.behavior.gds.mdm.signal.common.model.StagedAttribute;
import com.ebay.behavior.gds.mdm.signal.common.model.StagedEvent;
import com.ebay.behavior.gds.mdm.signal.common.model.StagedField;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedAttribute;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedEvent;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedField;
import com.ebay.behavior.gds.mdm.signal.common.service.MetadataWriteService;
import com.ebay.behavior.gds.mdm.signal.repository.StagedSignalRepository;
import com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils;
import com.ebay.behavior.gds.mdm.signal.util.ImportUtils;

import jakarta.persistence.EntityManager;
import lombok.val;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.Environment.PRODUCTION;
import static com.ebay.behavior.gds.mdm.common.model.Environment.STAGING;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.signal.model.SpecialPlanType.IMPORT;
import static com.ebay.behavior.gds.mdm.signal.model.SpecialPlanType.MIGRATION;
import static com.ebay.behavior.gds.mdm.signal.util.ImportUtils.CJS;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.CJS_PLATFORM_ID;
import static com.ebay.behavior.gds.mdm.signal.util.ImportUtils.EJS;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.EJS_PLATFORM_ID;
import static com.ebay.behavior.gds.mdm.signal.util.ImportUtils.ITEM;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.ITEM_PLATFORM_ID;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedAttribute;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedField;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.INTEGRATION_TEST;
import static java.util.Comparator.comparing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ActiveProfiles(IT)
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class SignalImportServiceIT {

    @MockitoBean
    private MetadataWriteService udcWriteService;

    @MockitoSpyBean
    private StagedSignalService stagedSignalService;

    @Autowired
    private UnstagedAttributeService unstagedAttributeService;

    @Autowired
    private UnstagedEventService unstagedEventService;

    @Autowired
    private UnstagedSignalService unstagedSignalService;

    @Autowired
    private UnstagedFieldService unstagedFieldService;

    @Autowired
    private PlanService planService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private SignalImportService service;

    @Autowired
    private StagedSignalRepository stagedSignalRepository;

    @Autowired
    private DomainLookupService domainService;

    @Autowired
    private PlatformLookup platformService;

    private long planId;
    private VersionedId originalId;
    private final String[] excludedProperties = List.of("id", "revision", "createDate", "updateDate",
                    "fields", "events", "attributes",
                    "event", "signal", "signalId", "eventId", "eventSourceId", "signalSourceId",
                    "pageIds", "moduleIds", "clickIds")
            .toArray(new String[0]);

    @BeforeAll
    void setUpAll() {
        var plan = TestModelUtils.plan();
        planId = planService.create(plan).getId();

        var event = TestModelUtils.unstagedEvent();
        var eventId = unstagedEventService.create(event).getId();

        var attribute = unstagedAttribute(eventId);
        var attributeId = unstagedAttributeService.create(attribute).getId();

        var signal = TestModelUtils.unstagedSignal(planId).toBuilder().environment(STAGING).build();
        signal = unstagedSignalService.create(signal);
        originalId = signal.getSignalId();

        var field = unstagedField(originalId);
        unstagedFieldService.create(field, Set.of(attributeId));
    }

    @Test
    @Transactional
    void createPlanIfAbsent() {
        var cjsPlanId1 = ImportUtils.createImportPlanIfAbsent(MIGRATION, platformService, planService, domainService);
        var cjsPlanId2 = ImportUtils.createImportPlanIfAbsent(MIGRATION, platformService, planService, domainService);

        var plan = planService.getById(cjsPlanId1);
        assertThat(cjsPlanId1).isEqualTo(cjsPlanId2);
        assertThat(plan.getPlatformId()).isEqualTo(CJS_PLATFORM_ID);
        assertThat(plan.getName()).containsIgnoringCase(MIGRATION.name());
        assertThat(plan.getName()).containsIgnoringCase(CJS);

        var ejsPlanId1 = ImportUtils.createImportPlanIfAbsent(MIGRATION, platformService, planService, domainService);
        var ejsPlanId2 = ImportUtils.createImportPlanIfAbsent(MIGRATION, platformService, planService, domainService);

        plan = planService.getById(ejsPlanId1);
        assertThat(ejsPlanId1).isEqualTo(ejsPlanId2);
        assertThat(cjsPlanId1).isNotEqualTo(ejsPlanId1);
        assertThat(plan.getPlatformId()).isEqualTo(EJS_PLATFORM_ID);
        assertThat(plan.getName()).containsIgnoringCase(MIGRATION.name());
        assertThat(plan.getName()).containsIgnoringCase(EJS);

        var importPlanId1 = ImportUtils.createImportPlanIfAbsent(IMPORT, platformService, planService, domainService);
        var importPlanId2 = ImportUtils.createImportPlanIfAbsent(IMPORT, platformService, planService, domainService);

        plan = planService.getById(importPlanId1);
        assertThat(importPlanId1).isNotEqualTo(importPlanId2);
        assertThat(importPlanId1).isNotEqualTo(cjsPlanId1);
        assertThat(importPlanId1).isNotEqualTo(ejsPlanId1);
        assertThat(plan.getPlatformId()).isEqualTo(ITEM_PLATFORM_ID);
        assertThat(plan.getName()).containsIgnoringCase(IMPORT.name());
        assertThat(plan.getName()).containsIgnoringCase(ITEM);
    }

    @Test
    @Transactional
    void importUnstagedSignal() {
        var original = unstagedSignalService.getByIdWithAssociationsRecursive(originalId);
        original.setId(original.getId() + 1);

        var cloneSignalId = service.importUnstagedSignal(original);

        var clone = unstagedSignalService.getByIdWithAssociationsRecursive(cloneSignalId);

        assertThat(unstagedSignalService.getEvents(cloneSignalId).size()).isEqualTo(original.getEvents().size());
        assertThat(unstagedSignalService.getFields(cloneSignalId).size()).isEqualTo(original.getFields().size());

        assertThat(clone)
                .usingRecursiveComparison()
                .ignoringFields(excludedProperties)
                .isEqualTo(original);

        var origEvents = original.getEvents().stream().sorted(comparing(UnstagedEvent::getName)).toList();
        var cloneEvents = clone.getEvents().stream().sorted(comparing(UnstagedEvent::getName)).toList();
        assertThat(cloneEvents)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields(excludedProperties)
                .isEqualTo(origEvents);

        var origFields = original.getFields().stream().sorted(comparing(UnstagedField::getTag)).toList();
        var cloneFields = clone.getFields().stream().sorted(comparing(UnstagedField::getTag)).toList();
        assertThat(cloneFields)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields(excludedProperties)
                .isEqualTo(origFields);

        var origAttributes = origFields.stream().flatMap(field -> field.getAttributes().stream().sorted(comparing(UnstagedAttribute::getTag))).toList();
        var cloneAttributes = cloneFields.stream().flatMap(field -> field.getAttributes().stream().sorted(comparing(UnstagedAttribute::getTag))).toList();
        assertThat(cloneAttributes)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields(excludedProperties)
                .isEqualTo(origAttributes);

        origAttributes = origEvents.stream().flatMap(field -> field.getAttributes().stream().sorted(comparing(UnstagedAttribute::getTag))).toList();
        cloneAttributes = cloneEvents.stream().flatMap(field -> field.getAttributes().stream().sorted(comparing(UnstagedAttribute::getTag))).toList();
        for (int i = 0; i < origAttributes.size(); i++) {
            var origAttr = origAttributes.get(i);
            var cloneAttr = cloneAttributes.get(i);
            assertThat(cloneAttr)
                    .usingRecursiveComparison()
                    .ignoringFields(excludedProperties)
                    .isEqualTo(origAttr);
        }
    }

    @Test
    void importStagedSignal() {
        var original = unstagedSignalService.getByIdWithAssociationsRecursive(originalId);
        original.setId(original.getId() + 1);

        var cloneSignalId1 = service.importStagedSignal(original);
        original.setId(original.getId() + 1);
        var cloneSignalId2 = service.importStagedSignal(original);

        assertThat(stagedSignalService.getEvents(cloneSignalId1).size()).isEqualTo(original.getEvents().size());
        assertThat(stagedSignalService.getFields(cloneSignalId1).size()).isEqualTo(original.getFields().size());

        var clone1 = stagedSignalService.getByIdWithAssociationsRecursive(cloneSignalId1);
        var clone2 = stagedSignalService.getByIdWithAssociationsRecursive(cloneSignalId2);

        assertThat(clone1)
                .usingRecursiveComparison()
                .ignoringFields(excludedProperties)
                .isEqualTo(clone2);

        var origEvents = clone1.getEvents().stream().sorted(comparing(StagedEvent::getName)).toList();
        var cloneEvents = clone2.getEvents().stream().sorted(comparing(StagedEvent::getName)).toList();
        assertThat(cloneEvents)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields(excludedProperties)
                .isEqualTo(origEvents);

        var origFields = clone1.getFields().stream().sorted(comparing(StagedField::getTag)).toList();
        var cloneFields = clone2.getFields().stream().sorted(comparing(StagedField::getTag)).toList();
        assertThat(cloneFields)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields(excludedProperties)
                .isEqualTo(origFields);

        var origAttributes = origFields.stream().flatMap(field -> field.getAttributes().stream().sorted(comparing(StagedAttribute::getTag))).toList();
        var cloneAttributes = cloneFields.stream().flatMap(field -> field.getAttributes().stream().sorted(comparing(StagedAttribute::getTag))).toList();
        assertThat(cloneAttributes)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields(excludedProperties)
                .isEqualTo(origAttributes);

        origAttributes = origEvents.stream().flatMap(field -> field.getAttributes().stream().sorted(comparing(StagedAttribute::getTag))).toList();
        cloneAttributes = cloneEvents.stream().flatMap(field -> field.getAttributes().stream().sorted(comparing(StagedAttribute::getTag))).toList();
        for (int i = 0; i < origAttributes.size(); i++) {
            var origAttr = origAttributes.get(i);
            var cloneAttr = cloneAttributes.get(i);
            assertThat(cloneAttr)
                    .usingRecursiveComparison()
                    .ignoringFields(excludedProperties)
                    .isEqualTo(origAttr);
        }
    }

    @Test
    void importUnstagedSignalIfAbsent_found_updateEnv() {
        var original = unstagedSignalService.getByIdWithAssociationsRecursive(originalId);
        entityManager.detach(original);

        original.setEnvironment(PRODUCTION);

        service.importUnstagedSignalIfAbsent(original);

        var clone = unstagedSignalService.getById(originalId);
        assertThat(clone.getEnvironment()).isEqualTo(PRODUCTION);
    }

    @Test
    void importStagedSignalIfAbsent_notFound_created() {
        var original = unstagedSignalService.getByIdWithAssociationsRecursive(originalId);
        entityManager.detach(original);

        original.setName("updated");
        original.setDataSource(UdcDataSourceType.QA);

        service.importStagedSignalIfAbsent(original);

        var clone = stagedSignalService.getById(originalId);
        assertThat(clone.getName()).isEqualTo("updated");
        assertThat(clone.getDataSource()).isEqualTo(UdcDataSourceType.QA);
    }

    @Test
    void importStagedSignalIfAbsent_foundDifferentDataSource_error() {
        var signal = TestModelUtils.stagedSignal(planId).toBuilder()
                .id(originalId.getId())
                .version(originalId.getVersion())
                .environment(STAGING)
                .build();
        val stagedSignal = stagedSignalRepository.findById(originalId).orElseGet(() -> stagedSignalService.create(signal));
        stagedSignal.setEnvironment(STAGING);
        stagedSignal.setDataSource(UdcDataSourceType.STAGED); // different data source
        stagedSignalRepository.saveAndFlush(stagedSignal);

        var original = unstagedSignalService.getByIdWithAssociationsRecursive(originalId);
        entityManager.detach(original);

        assertThatThrownBy(() -> service.importStagedSignalIfAbsent(original))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("different data source");
    }

    @Test
    void importStagedSignalIfAbsent_foundStaging_envUpdated() {
        var signal = TestModelUtils.stagedSignal(planId).toBuilder()
                .id(originalId.getId())
                .version(originalId.getVersion())
                .environment(STAGING)
                .build();
        val stagedSignal = stagedSignalRepository.findById(originalId).orElseGet(() -> stagedSignalService.create(signal));
        stagedSignal.setEnvironment(STAGING);
        stagedSignal.setDataSource(UdcDataSourceType.TEST);
        stagedSignalRepository.saveAndFlush(stagedSignal);

        var original = unstagedSignalService.getByIdWithAssociationsRecursive(originalId);
        entityManager.detach(original);
        original.setEnvironment(PRODUCTION);

        service.importStagedSignalIfAbsent(original);

        var clone = stagedSignalService.getById(originalId);
        assertThat(clone.getEnvironment()).isEqualTo(PRODUCTION);
    }

    @Test
    void importStagedSignalIfAbsent_foundProd_envNotUpdated() {
        Mockito.reset(stagedSignalService);
        var signal = TestModelUtils.stagedSignal(planId).toBuilder()
                .id(originalId.getId())
                .version(originalId.getVersion())
                .environment(PRODUCTION)
                .build();
        val stagedSignal = stagedSignalRepository.findById(originalId).orElseGet(() -> stagedSignalService.create(signal));
        stagedSignal.setEnvironment(PRODUCTION);
        stagedSignal.setDataSource(UdcDataSourceType.TEST);
        stagedSignalRepository.saveAndFlush(stagedSignal);

        var original = unstagedSignalService.getByIdWithAssociationsRecursive(originalId);
        entityManager.detach(original);
        original.setEnvironment(PRODUCTION);

        service.importStagedSignalIfAbsent(original);

        verify(stagedSignalService, never()).updateToProductionEnvironment(originalId);
    }

    @Test
    void injectAndImport() {
        var entityId = "signal:0";
        doReturn(entityId).when(udcWriteService).upsert(any(), any());
        var original = unstagedSignalService.getByIdWithAssociationsRecursive(originalId);
        entityManager.detach(original);

        var injected = service.injectAndImport(UdcDataSourceType.TEST, original);

        assertThat(injected).isEqualTo(entityId);
    }
}
