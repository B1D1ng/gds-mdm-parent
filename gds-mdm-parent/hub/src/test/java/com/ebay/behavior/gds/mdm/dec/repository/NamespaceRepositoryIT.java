package com.ebay.behavior.gds.mdm.dec.repository;

import com.ebay.behavior.gds.mdm.dec.model.Namespace;
import com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles(IT)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class NamespaceRepositoryIT {

    @Autowired
    private NamespaceRepository repository;

    private Namespace model;

    @BeforeEach
    void setUp() {
        model = TestModelUtils.namespace();
    }

    @Test
    void save() {
        var saved = repository.save(model);

        var id = repository.findById(saved.getId()).get().getId();
        assertThat(id).isEqualTo(saved.getId());
    }
}
