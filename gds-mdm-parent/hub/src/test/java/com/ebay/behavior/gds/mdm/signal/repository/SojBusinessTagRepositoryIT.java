package com.ebay.behavior.gds.mdm.signal.repository;

import com.ebay.behavior.gds.mdm.signal.common.model.SojBusinessTag;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.sojBusinessTag;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles(IT)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class SojBusinessTagRepositoryIT {

    @Autowired
    private SojBusinessTagRepository repository;

    private SojBusinessTag model;

    @BeforeEach
    void setUp() {
        model = sojBusinessTag("test");
        repository.findBySojName(model.getSojName()).ifPresent(repository::delete);
    }

    @Test
    void save() {
        var saved = repository.save(model);

        var id = repository.findById(saved.getId()).get().getId();
        assertThat(id).isEqualTo(saved.getId());
        assertThat(saved.getSojName()).isEqualTo(model.getSojName());
    }

    @Test
    void findBySojName() {
        repository.save(model);

        var result = repository.findBySojName(model.getSojName());
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get().getSojName()).isEqualTo(model.getSojName());
    }
}
