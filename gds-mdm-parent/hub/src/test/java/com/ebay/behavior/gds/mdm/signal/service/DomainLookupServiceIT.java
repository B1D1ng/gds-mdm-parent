package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.signal.common.model.SignalDimValueLookup;
import com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.VI_DOMAIN;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.INTEGRATION_TEST;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomSmallString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles(IT)
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class DomainLookupServiceIT {

    @Autowired
    private DomainLookupService service;

    @Test
    void getDimensionTypeId() {
        var dimensionTypeId = service.getDimensionTypeId();
        assertThat(dimensionTypeId).isEqualTo(0); // DOMAIN dimension type ID is 0 under signal_dim_type_lookup table in data.sql
    }

    @Test
    void create() {
        var name = getRandomSmallString();
        var domain = TestModelUtils.domain(name);
        service.create(domain);

        var persisted = service.getByName(name);

        assertThat(persisted.getId()).isNotNull();
        assertThat(persisted.getName()).isEqualTo(name);
        assertThat(persisted.getDimensionTypeId()).isEqualTo(service.getDimensionTypeId());
    }

    @Test
    void create_invalid_error() {
        var domain = TestModelUtils.domain(getRandomSmallString());
        domain.setId(123L);

        assertThatThrownBy(() -> service.create(domain))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be null");
    }

    @Test
    void create_nameInUse_error() {
        var domain = TestModelUtils.domain(getRandomSmallString());
        domain.setName(VI_DOMAIN);

        assertThatThrownBy(() -> service.create(domain))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getAll() {
        var domains = service.getAll();

        assertThat(domains.size()).isGreaterThanOrEqualTo(1);
        assertThat(domains).extracting(SignalDimValueLookup::getDimensionTypeId).containsOnly(service.getDimensionTypeId());
    }

    @Test
    void getById() {
        var domain = TestModelUtils.domain(getRandomSmallString());
        domain = service.create(domain);

        var persisted = service.getById(domain.getId());

        assertThat(domain.getId()).isEqualTo(persisted.getId());
        assertThat(domain.getName()).isEqualTo(persisted.getName());
        assertThat(domain.getDimensionTypeId()).isEqualTo(service.getDimensionTypeId());
    }
}