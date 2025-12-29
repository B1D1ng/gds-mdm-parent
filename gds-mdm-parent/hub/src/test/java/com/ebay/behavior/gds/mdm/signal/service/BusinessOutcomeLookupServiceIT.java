package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.signal.common.model.BusinessOutcomeLookup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.businessOutcome;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles(IT)
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class BusinessOutcomeLookupServiceIT {

    @Autowired
    private BusinessOutcomeLookupService service;

    private BusinessOutcomeLookup lookup;

    @BeforeEach
    void setup() {
        lookup = businessOutcome();
    }

    @Test
    void create() {
        var name = lookup.getName();
        service.findByName(name).ifPresent(lookup -> {
            service.deleteByName(name);
            service.create(this.lookup);
        });

        var persisted = service.getByName(name);

        assertThat(persisted.getId()).isNotNull();
        assertThat(persisted.getName()).isEqualTo(name);
    }

    @Test
    void create_invalid_error() {
        lookup = lookup.toBuilder().id(123L).build();
        assertThatThrownBy(() -> service.create(lookup))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be null");
    }

    @Test
    void create_nameInUse_error() {
        assertThatThrownBy(() -> service.create(lookup))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getAll() {
        var businessOutcomes = service.getAll();

        assertThat(businessOutcomes.size()).isGreaterThanOrEqualTo(1);
        assertThat(businessOutcomes).extracting(BusinessOutcomeLookup::getName).contains(lookup.getName());
    }

    @Test
    void getById() {
        var persisted = service.getByName(lookup.getName());

        assertThat(persisted.getId()).isNotNull();
        assertThat(persisted.getName()).isEqualTo(lookup.getName());
        var result = service.getById(persisted.getId());
        assertThat(result.getId()).isEqualTo(persisted.getId());
        assertThat(persisted.getName()).isEqualTo(result.getName());
    }
}
