package com.ebay.behavior.gds.mdm.dec.repository;

import com.ebay.behavior.gds.mdm.common.model.Model;
import com.ebay.behavior.gds.mdm.dec.model.Dataset;
import com.ebay.behavior.gds.mdm.dec.model.DatasetIndex;
import com.ebay.behavior.gds.mdm.dec.model.LdmBaseEntity;
import com.ebay.behavior.gds.mdm.dec.model.LdmEntity;
import com.ebay.behavior.gds.mdm.dec.model.LdmEntityIndex;
import com.ebay.behavior.gds.mdm.dec.model.Namespace;
import com.ebay.behavior.gds.mdm.dec.service.LdmBaseEntityService;
import com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestUtils.getRandomString;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles(IT)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class DatasetRepositoryIT {

    @Autowired
    private DatasetRepository repository;

    @Autowired
    private DatasetIndexRepository indexRepository;

    @Autowired
    private LdmEntityRepository ldmEntityRepository;

    @Autowired
    private LdmEntityIndexRepository ldmEntityIndexRepository;

    @Autowired
    private NamespaceRepository namespaceRepository;

    @Autowired
    private LdmBaseEntityService baseEntityService;

    private Dataset model;
    private Dataset model2;

    @BeforeEach
    void setUp() {
        Namespace namespace = TestModelUtils.namespace();
        var savedNamespace = namespaceRepository.save(namespace);

        LdmBaseEntity baseEntity = TestModelUtils.ldmBaseEntity(savedNamespace.getId());
        baseEntity = baseEntityService.create(baseEntity);

        LdmEntityIndex entityIndex = TestModelUtils.ldmEntityIndex(baseEntity.getId());
        var savedEntityIndex = ldmEntityIndexRepository.save(entityIndex);

        LdmEntity entity = TestModelUtils.ldmEntity(savedEntityIndex.getId(), savedEntityIndex.getCurrentVersion(),
                getRandomString(), savedEntityIndex.getViewType(), savedNamespace.getId(), baseEntity.getId());
        var savedEntity = ldmEntityRepository.save(entity);

        DatasetIndex index = TestModelUtils.datasetIndex();
        var savedIndex = indexRepository.save(index);
        model = TestModelUtils.dataset(savedIndex.getId(), savedIndex.getCurrentVersion(), savedEntity.getId(), savedEntity.getVersion());

        DatasetIndex index2 = TestModelUtils.datasetIndex();
        var savedIndex2 = indexRepository.save(index2);
        model2 = TestModelUtils.dataset(savedIndex2.getId(), savedIndex2.getCurrentVersion(), savedEntity.getId(), savedEntity.getVersion());
    }

    @Test
    void findByIdCurrentVersion() {
        var saved = repository.save(model);

        var id = repository.findByIdCurrentVersion(saved.getId()).get().getId();
        assertThat(id).isEqualTo(saved.getId());
    }

    @Test
    void findAllCurrentVersion() {
        var saved = repository.save(model);

        // save another dataset
        var saved2 = repository.save(model2);
        var ids = repository.findAllCurrentVersion().stream().map(Model::getId).toList();

        assertThat(ids).contains(saved.getId(), saved2.getId());
    }
}
