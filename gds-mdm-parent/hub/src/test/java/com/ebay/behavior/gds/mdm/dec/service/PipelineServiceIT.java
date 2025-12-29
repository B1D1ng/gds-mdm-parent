package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.dec.model.Pipeline;
import com.ebay.behavior.gds.mdm.dec.model.enums.AccessType;
import com.ebay.behavior.gds.mdm.dec.repository.PhysicalStoragePipelineMappingRepository;
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
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestUtils.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles(IT)
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class PipelineServiceIT {

    @Autowired
    private PipelineService service;

    @Autowired
    private PhysicalStorageService storageService;

    @Autowired
    private PhysicalStoragePipelineMappingRepository mappingRepository;

    private Pipeline pipeline;

    @BeforeEach
    void setUp() {
        pipeline = TestModelUtils.pipeline();
        pipeline = service.create(pipeline);
    }

    @Test
    void getById() {
        var persisted = service.getById(pipeline.getId());

        assertThat(persisted.getId()).isEqualTo(pipeline.getId());
    }

    @Test
    void getByIdWithAssociations() {
        assertThatThrownBy(() -> service.getByIdWithAssociations(pipeline.getId()))
                .isInstanceOf(NotImplementedException.class);
    }

    @Test
    void getAll_Search() {
        assertThatThrownBy(() -> service.getAll(new Search("by", "term", CONTAINS, PageRequest.of(0, 10))))
                .isInstanceOf(NotImplementedException.class);
    }

    @Test
    void delete() {
        var pipeline1 = TestModelUtils.pipeline();
        pipeline1 = service.create(pipeline1);
        var pipelineId = pipeline1.getId();
        assertThat(pipelineId).isNotNull();

        var storage = TestModelUtils.physicalStorage();
        storage = storageService.create(storage);
        storageService.savePipelineMappings(storage.getId(), Set.of(pipeline1), null);
        storageService.delete(storage.getId());
        service.delete(pipelineId);

        assertThatThrownBy(() -> service.getById(pipelineId)).isInstanceOf(DataNotFoundException.class);

        var mappings = mappingRepository.findByPipelineId(pipelineId);
        assertThat(mappings).isEmpty();
    }

    @Test
    void create_duplicatePipelineId() {
        var pipeline2 = TestModelUtils.pipeline();
        pipeline2.setPipelineId(pipeline.getPipelineId());
        assertThatThrownBy(() -> service.create(pipeline2)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_NoStorage() {
        var pipeline2 = TestModelUtils.pipeline();
        pipeline2 = service.create(pipeline2);
        assertThat(pipeline2.getId()).isNotNull();
    }

    @Test
    void update_UpdatePipelineId() {
        pipeline.setPipelineId("newPipelineId");
        assertThatThrownBy(() -> service.update(pipeline)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void update_NoPipeline() {
        service.update(pipeline);
        var persisted = service.getById(pipeline.getId());

        assertThat(persisted.getId()).isEqualTo(pipeline.getId());
    }

    @Test
    void getAll() {
        var pipelines = service.getAll();

        assertThat(pipelines.size()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void getByPipelineId() {
        var pipelines = service.getByPipelineId(pipeline.getPipelineId());

        assertThat(pipelines.size()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void getByStorageId() {
        var storage = TestModelUtils.physicalStorage();
        storage = storageService.create(storage);
        storageService.savePipelineMappings(storage.getId(), Set.of(pipeline), null);

        var samePipeline = TestModelUtils.pipeline();
        samePipeline.setPipelineId(pipeline.getPipelineId());

        var storage2 = TestModelUtils.physicalStorage();
        storage2.setAccessType(AccessType.API);
        storage2 = storageService.create(storage2);
        storageService.savePipelineMappings(storage2.getId(), Set.of(samePipeline), null);

        var exclusivePipelines = service.getByStorageId(storage.getId(), true);
        assertThat(exclusivePipelines.size()).isEqualTo(0);

        var allPipelines = service.getByStorageId(storage.getId(), null);
        assertThat(allPipelines.size()).isGreaterThanOrEqualTo(0);
    }
}