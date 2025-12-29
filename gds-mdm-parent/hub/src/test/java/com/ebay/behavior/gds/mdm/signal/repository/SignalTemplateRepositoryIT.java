package com.ebay.behavior.gds.mdm.signal.repository;

import com.ebay.behavior.gds.mdm.commonSvc.util.DbUtils;
import com.ebay.behavior.gds.mdm.signal.common.model.SignalTemplate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.signalTemplate;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomSmallString;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles(IT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class SignalTemplateRepositoryIT {

    private final Pageable pageable = DbUtils.getAuditablePageable(0, 10);

    @Autowired
    private SignalTemplateRepository repository;

    private String term;
    private SignalTemplate signal;

    @BeforeEach
    void setUp() {
        term = getRandomSmallString();
        signal = signalTemplate();
    }

    @Test
    void create() {
        var saved = repository.save(signal);

        var persisted = repository.findById(saved.getId()).get();

        assertThat(persisted.getId()).isEqualTo(saved.getId());
        assertThat(persisted.getRevision()).isEqualTo(0);
    }

    @Test
    void update() {
        var created = repository.save(signal);
        var createDate = created.getCreateDate();
        var updateDate = created.getUpdateDate();

        created.setName("updated");
        repository.save(created);

        var updated = repository.findById(created.getId()).get();
        assertThat(updated.getName()).isEqualTo("updated");
        assertThat(updated.getRevision()).isEqualTo(1);
        assertThat(updated.getCreateDate()).isEqualTo(createDate);
        assertThat(updated.getUpdateDate()).isAfterOrEqualTo(updateDate);
    }

    @Test
    void delete() {
        var saved = repository.save(signal);

        repository.deleteById(saved.getId());

        var maybeDeleted = repository.findById(saved.getId());
        assertThat(maybeDeleted).isEmpty();
    }

    @Test
    void findByName() {
        var signal1 = signalTemplate().setName(term);
        var signal2 = signalTemplate().setName(term);
        repository.save(signal1);
        repository.save(signal2);

        var result = repository.findByName(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getName()).isEqualTo(term);
        assertThat(result.getContent().get(1).getName()).isEqualTo(term);
    }

    @Test
    void findByNameContaining() {
        var signal1 = signalTemplate().setName("prefix_" + term);
        var signal2 = signalTemplate().setName(term + "_suffix");
        repository.save(signal1);
        repository.save(signal2);

        var result = repository.findByNameContaining(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getName()).contains(term);
        assertThat(result.getContent().get(1).getName()).contains(term);
    }

    @Test
    void findByNameStartingWith() {
        var signal1 = signalTemplate().setName(term + "Signal1");
        var signal2 = signalTemplate().setName(term + "Signal2");
        repository.save(signal1);
        repository.save(signal2);

        var result = repository.findByNameStartingWith(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getName()).startsWith(term);
        assertThat(result.getContent().get(1).getName()).startsWith(term);
    }

    @Test
    void findByType() {
        var signal = signalTemplate().setType(term);
        repository.save(signal);

        var result = repository.findByType(term, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getType()).isEqualTo(term);
    }

    @Test
    void findByTypeStartingWith() {
        var signal1 = signalTemplate().setType(term + "Type1");
        var signal2 = signalTemplate().setType(term + "Type2");
        repository.save(signal1);
        repository.save(signal2);

        var result = repository.findByTypeStartingWith(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getType()).startsWith(term);
        assertThat(result.getContent().get(1).getType()).startsWith(term);
    }

    @Test
    void findByTypeContaining() {
        var signal1 = signalTemplate().setType("Type1" + term);
        var signal2 = signalTemplate().setType(term + "Type2");
        repository.save(signal1);
        repository.save(signal2);

        var result = repository.findByTypeContaining(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getType()).contains(term);
        assertThat(result.getContent().get(1).getType()).contains(term);
    }

    @Test
    void findByDescription() {
        var signal = signalTemplate().setDescription(term);
        repository.save(signal);

        var result = repository.findByDescription(term, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getDescription()).isEqualTo(term);
    }

    @Test
    void findByDescriptionStartingWith() {
        var signal1 = signalTemplate().setDescription(term + "desc1");
        var signal2 = signalTemplate().setDescription(term + "desc2");
        repository.save(signal1);
        repository.save(signal2);

        var result = repository.findByDescriptionStartingWith(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getDescription()).startsWith(term);
        assertThat(result.getContent().get(1).getDescription()).startsWith(term);
    }

    @Test
    void findByDescriptionContaining() {
        var signal1 = signalTemplate().setDescription("Test " + term + " 1");
        var signal2 = signalTemplate().setDescription("Another " + term + " 2");
        repository.save(signal1);
        repository.save(signal2);

        var result = repository.findByDescriptionContaining(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getDescription()).contains(term);
        assertThat(result.getContent().get(1).getDescription()).contains(term);
    }

    @Test
    void findByDomain() {
        var signal = signalTemplate().setDomain(term);
        repository.save(signal);

        var result = repository.findByDomain(term, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getDomain()).isEqualTo(term);
    }

    @Test
    void findByDomainStartingWith() {
        var signal1 = signalTemplate().setDomain(term + "desc1");
        var signal2 = signalTemplate().setDomain(term + "desc2");
        repository.save(signal1);
        repository.save(signal2);

        var result = repository.findByDomainStartingWith(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getDomain()).startsWith(term);
        assertThat(result.getContent().get(1).getDomain()).startsWith(term);
    }

    @Test
    void findByDomainContaining() {
        var signal1 = signalTemplate().setDomain("Test " + term + " 1");
        var signal2 = signalTemplate().setDomain("Another " + term + " 2");
        repository.save(signal1);
        repository.save(signal2);

        var result = repository.findByDomainContaining(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getDomain()).contains(term);
        assertThat(result.getContent().get(1).getDomain()).contains(term);
    }
}
