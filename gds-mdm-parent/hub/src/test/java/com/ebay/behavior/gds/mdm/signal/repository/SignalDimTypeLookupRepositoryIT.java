package com.ebay.behavior.gds.mdm.signal.repository;

import com.ebay.behavior.gds.mdm.signal.common.model.SignalDimTypeLookup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.signalDimTypeLookup;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles(IT)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class SignalDimTypeLookupRepositoryIT {

    @Autowired
    private SignalDimTypeLookupRepository repository;

    private SignalDimTypeLookup signalDimType;

    @BeforeEach
    void setUp() {
        signalDimType = signalDimTypeLookup("test");
        repository.findByName(signalDimType.getName()).ifPresent(repository::delete);
    }

    @Test
    void save() {
        var saved = repository.save(signalDimType);

        var id = repository.findById(saved.getId()).get().getId();
        assertThat(id).isEqualTo(saved.getId());
        assertThat(saved.getReadableName()).isEqualTo(signalDimType.getReadableName());
        assertThat(saved.getName()).isEqualTo(signalDimType.getName());
    }

    @Test
    void findAll() {
        repository.save(signalDimType);

        var results = repository.findAll();
        assertThat(results.size()).isGreaterThanOrEqualTo(1);
        assertThat(results).contains(signalDimType);
    }
}
