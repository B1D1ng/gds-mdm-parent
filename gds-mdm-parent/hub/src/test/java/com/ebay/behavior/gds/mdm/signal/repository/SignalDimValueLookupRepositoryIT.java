package com.ebay.behavior.gds.mdm.signal.repository;

import com.ebay.behavior.gds.mdm.signal.common.model.SignalDimValueLookup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles(IT)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class SignalDimValueLookupRepositoryIT {

    @Autowired
    private SignalDimValueLookupRepository repository;

    private SignalDimValueLookup domain;
    private final String name = "testName";

    @BeforeEach
    void setUp() {
        long dimTypeId = 0L; // domain from data.sql
        domain = SignalDimValueLookup.builder().name(name).readableName(name).dimensionTypeId(dimTypeId).build();
        repository.findByDimensionTypeIdAndName(dimTypeId, name).ifPresent(repository::delete);
    }

    @Test
    void save() {
        var saved = repository.save(domain);

        var id = repository.findById(saved.getId()).get().getId();
        assertThat(id).isEqualTo(saved.getId());
        assertThat(saved.getReadableName()).isEqualTo(domain.getReadableName());
        assertThat(saved.getName()).isEqualTo(domain.getName());
    }

    @Test
    void findAllByDimensionTypeId() {
        domain = repository.save(domain);

        var results = repository.findAllByDimensionTypeId(domain.getDimensionTypeId());
        assertThat(results.size()).isGreaterThanOrEqualTo(1);
        assertThat(results).extracting(ID).contains(domain.getId());
    }

    @Test
    void findByDimensionTypeIdAndNameIn() {
        domain = repository.save(domain);

        var results = repository.findByDimensionTypeIdAndNameIn(domain.getDimensionTypeId(), Set.of(name));
        assertThat(results).hasSize(1);
        assertThat(results).extracting(ID).containsOnly(domain.getId());
    }
}
