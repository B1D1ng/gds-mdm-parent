package com.ebay.behavior.gds.mdm.dec.repository;

import com.ebay.behavior.gds.mdm.common.model.Model;
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
class LdmEntityRepositoryIT {

    @Autowired
    private LdmEntityRepository repository;

    @Autowired
    private LdmEntityIndexRepository indexRepository;

    @Autowired
    private NamespaceRepository namespaceRepository;

    @Autowired
    private LdmBaseEntityService baseEntityService;

    private LdmEntity model;
    private Namespace ns;
    private LdmBaseEntity baseEntity;

    @BeforeEach
    void setUp() {
        var namespace = TestModelUtils.namespace();
        ns = namespaceRepository.save(namespace);

        baseEntity = TestModelUtils.ldmBaseEntity(namespace.getId());
        baseEntity = baseEntityService.create(baseEntity);

        LdmEntityIndex index = TestModelUtils.ldmEntityIndex(baseEntity.getId());
        var savedIndex = indexRepository.save(index);
        model = TestModelUtils.ldmEntity(savedIndex.getId(), savedIndex.getCurrentVersion(),
                getRandomString(), savedIndex.getViewType(), ns.getId(), baseEntity.getId());
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

        // save another entity
        LdmEntityIndex index2 = TestModelUtils.ldmEntityIndex(baseEntity.getId());
        var savedIndex2 = indexRepository.save(index2);
        var model2 = TestModelUtils.ldmEntity(savedIndex2.getId(), savedIndex2.getCurrentVersion(),
                getRandomString(), savedIndex2.getViewType(), ns.getId(), baseEntity.getId());
        var saved2 = repository.save(model2);

        var ids = repository.findAllCurrentVersion().stream().map(Model::getId).toList();
        assertThat(ids).contains(saved.getId(), saved2.getId());
    }
}
