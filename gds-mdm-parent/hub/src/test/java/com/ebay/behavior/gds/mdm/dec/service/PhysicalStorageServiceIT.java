package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.dec.model.PhysicalStorage;
import com.ebay.behavior.gds.mdm.dec.model.Pipeline;
import com.ebay.behavior.gds.mdm.dec.model.enums.MappingSaveMode;
import com.ebay.behavior.gds.mdm.dec.model.enums.PlatformEnvironment;
import com.ebay.behavior.gds.mdm.dec.repository.LdmFieldPhysicalStorageMappingRepository;
import com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.CONTAINS;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils.physicalStorage;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils.pipeline;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestUtils.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles(IT)
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class PhysicalStorageServiceIT {

    @Autowired
    private PhysicalStorageService service;

    @Autowired
    private PipelineService pipelineService;

    @Autowired
    private LdmEntityService entityService;

    @Autowired
    private LdmFieldService fieldService;

    @Autowired
    private NamespaceService namespaceService;

    @Autowired
    private LdmFieldPhysicalStorageMappingRepository ldmMappingRepository;

    private PhysicalStorage storage;
    private Long storageId;

    @BeforeEach
    void setUp() {
        storage = physicalStorage();
        storage = service.create(storage);
        storageId = storage.getId();
    }

    @Test
    void getById() {
        var persisted = service.getById(storage.getId());

        assertThat(persisted.getId()).isEqualTo(storage.getId());
    }

    @Test
    void getByIdWithAssociations() {
        var persisted = service.getByIdWithAssociations(storage.getId());

        assertThat(persisted.getId()).isNotNull();
    }

    @Test
    void update() {
        storage.setStorageDetails("Updated Storage Info");
        var updated = service.update(storage);

        assertThat(updated.getId()).isEqualTo(storage.getId());
        assertThat(updated.getStorageDetails()).isEqualTo("Updated Storage Info");
    }

    @Test
    void getAll_Search() {
        assertThatThrownBy(() -> service.getAll(new Search("by", "term", CONTAINS, PageRequest.of(0, 10))))
                .isInstanceOf(NotImplementedException.class);
    }

    @Test
    void getAll() {
        var storage = physicalStorage();
        storage.setStorageEnvironment(PlatformEnvironment.PRE_PRODUCTION);
        service.create(storage);
        var storages = service.getAll("pre_production");
        assertThat(storages.size()).isEqualTo(1);
    }

    @Test
    void getAllByIdWithAssociations() {
        var storage = physicalStorage();
        storage.setStorageEnvironment(PlatformEnvironment.PRODUCTION);
        storage = service.create(storage);
        var storages = service.getAllByIdWithAssociations(Set.of(storage.getId()), "production");
        assertThat(storages.size()).isEqualTo(1);
    }

    @Test
    void delete() {
        var storage1 = physicalStorage();
        storage1.setStorageDetails("New Storage");
        storage1 = service.create(storage1);
        var storageId = storage1.getId();
        assertThat(storageId).isNotNull();

        service.delete(storageId);
        assertThatThrownBy(() -> service.getById(storageId)).isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void create_WithSameAccessTypeAndStorageDetails() {
        var storage1 = physicalStorage();
        storage1.setAccessType(storage.getAccessType());
        storage1.setStorageDetails(storage.getStorageDetails());

        var created = service.create(storage1);

        assertThat(created.getId()).isEqualTo(storage.getId());
    }

    @Test
    void savePipelineMappings_ExistingPipeline() {
        var pipeline = pipeline();
        pipeline = pipelineService.create(pipeline);

        var updated = service.savePipelineMappings(storageId, Set.of(pipeline), null);

        assertThat(updated.getPipelines().size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void savePipelineMappings_NewPipeline() {
        var pipeline = pipeline();

        var updated = service.savePipelineMappings(storageId, Set.of(pipeline), null);

        assertThat(updated.getPipelines().size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void savePipelineMappings_ExistingPipelineId() {
        var pipeline = pipeline();
        pipeline = pipelineService.create(pipeline);
        var pipeline1 = pipeline();
        pipeline1.setPipelineId(pipeline.getPipelineId());

        var updated = service.savePipelineMappings(storageId, Set.of(pipeline1), null);

        assertThat(updated.getPipelines().size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void savePipelineMappings_EmptyPipelines() {
        assertThatThrownBy(() -> service.savePipelineMappings(storageId, Set.of(), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void savePipelineMappings_ReplaceAll() {
        var pipeline = pipeline();
        pipeline = pipelineService.create(pipeline);

        var pipeline1 = pipeline();
        pipeline1 = pipelineService.create(pipeline1);

        service.savePipelineMappings(storageId, Set.of(pipeline, pipeline1), null);

        var samePipeline = pipeline();
        samePipeline.setPipelineId(pipeline.getPipelineId());
        var pipeline2 = pipeline();
        service.savePipelineMappings(storageId, Set.of(samePipeline, pipeline2), null);

        var updated = service.getByIdWithAssociations(storageId);
        assertThat(updated.getPipelines().size()).isEqualTo(2);
        assertThat(updated.getPipelines()).extracting(Pipeline::getPipelineId).contains(samePipeline.getPipelineId());
        assertThat(updated.getPipelines()).extracting(Pipeline::getPipelineId).contains(pipeline2.getPipelineId());
    }

    @Test
    void savePipelineMappings_Upsert() {
        var pipeline = pipeline();
        pipeline = pipelineService.create(pipeline);

        var pipeline1 = pipeline();
        pipeline1 = pipelineService.create(pipeline1);

        service.savePipelineMappings(storageId, Set.of(pipeline, pipeline1), null);

        var pipeline2 = pipeline();
        service.savePipelineMappings(storageId, Set.of(pipeline2), MappingSaveMode.UPSERT);

        var updated = service.getByIdWithAssociations(storageId);
        assertThat(updated.getPipelines().size()).isEqualTo(3);
        assertThat(updated.getPipelines()).extracting(Pipeline::getPipelineId).contains(pipeline.getPipelineId());
        assertThat(updated.getPipelines()).extracting(Pipeline::getPipelineId).contains(pipeline1.getPipelineId());
        assertThat(updated.getPipelines()).extracting(Pipeline::getPipelineId).contains(pipeline2.getPipelineId());
    }

    @Test
    void getAllByLdmEntityId_NotExclusive() {
        var namespace = TestModelUtils.namespace();
        namespace = namespaceService.create(namespace);

        var entity = TestModelUtils.ldmEntityEmpty(namespace.getId());
        entity = entityService.create(entity);

        var field = TestModelUtils.ldmField(entity.getId(), entity.getVersion());
        field = fieldService.create(field);

        var ldmMapping = TestModelUtils.ldmFieldPhysicalMapping(field.getId(), storageId);
        ldmMappingRepository.save(ldmMapping);

        var persisted = service.getAllByLdmEntityId(entity.getId(), false, null);
        assertThat(persisted.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void getAllByLdmEntityId_Exclusive() {
        var namespace = TestModelUtils.namespace();
        namespace = namespaceService.create(namespace);

        // create 1st entity
        var entity = TestModelUtils.ldmEntityEmpty(namespace.getId());
        entity = entityService.create(entity);

        var field = TestModelUtils.ldmField(entity.getId(), entity.getVersion());
        field = fieldService.create(field);

        var ldmMapping = TestModelUtils.ldmFieldPhysicalMapping(field.getId(), storageId);
        ldmMappingRepository.save(ldmMapping);

        // create 2nd entity
        var entity2 = TestModelUtils.ldmEntityEmpty(namespace.getId());
        entity2 = entityService.create(entity2);

        var field2 = TestModelUtils.ldmField(entity2.getId(), entity2.getVersion());
        field2 = fieldService.create(field2);

        var ldmMapping2 = TestModelUtils.ldmFieldPhysicalMapping(field2.getId(), storageId);
        ldmMappingRepository.save(ldmMapping2);

        // check the exclusive storage attached to the 1st entity, should return 0
        var persisted = service.getAllByLdmEntityId(entity.getId(), true, null);
        assertThat(persisted.size()).isEqualTo(0);
    }

    @Test
    void getLdmStorageIds() {
        var namespace = TestModelUtils.namespace();
        namespace = namespaceService.create(namespace);

        // create 1st entity
        var entity = TestModelUtils.ldmEntityEmpty(namespace.getId());
        entity = entityService.create(entity);

        var field = TestModelUtils.ldmField(entity.getId(), entity.getVersion());
        field = fieldService.create(field);

        var ldmMapping = TestModelUtils.ldmFieldPhysicalMapping(field.getId(), storageId);
        ldmMappingRepository.save(ldmMapping);

        // create 2nd entity
        var entity2 = TestModelUtils.ldmEntityEmpty(namespace.getId());
        entity2 = entityService.create(entity2);

        var field2 = TestModelUtils.ldmField(entity2.getId(), entity2.getVersion());
        field2 = fieldService.create(field2);

        var ldmMapping2 = TestModelUtils.ldmFieldPhysicalMapping(field2.getId(), storageId);
        ldmMappingRepository.save(ldmMapping2);

        // check the exclusive storage attached to the 1st entity, should return 0
        var exclusiveStorageIds = service.getLdmStorageIds(entity.getId(), true);
        assertThat(exclusiveStorageIds.size()).isEqualTo(0);

        // check the non-exclusive storage attached to the 1st entity, should be non-empty
        var nonExclusiveStorageIds = service.getLdmStorageIds(entity.getId(), false);
        assertThat(nonExclusiveStorageIds).isNotEmpty();
    }
}