package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.common.model.PlatformType;
import com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.INTEGRATION_TEST;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomSmallString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles(IT)
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class PlatformLookupServiceIT {

    @Autowired
    private PlatformLookupService service;

    @Test
    void create() {
        var name = getRandomSmallString();
        var platform = TestModelUtils.platform(name);
        service.create(platform);

        var persisted = service.getByName(name);

        assertThat(persisted.getId()).isNotNull();
        assertThat(persisted.getName()).isEqualTo(name);
    }

    @Test
    void create_invalid_error() {
        var platform = TestModelUtils.platform(getRandomSmallString());
        platform.setId(123L);

        assertThatThrownBy(() -> service.create(platform))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be null");
    }

    @Test
    void create_nameInUse_error() {
        var platform = TestModelUtils.platform(getRandomSmallString());
        platform.setName(PlatformType.CJS.name());

        assertThatThrownBy(() -> service.create(platform))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getAll() {
        var platforms = service.getAll();

        assertThat(platforms.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void getById() {
        var platform = TestModelUtils.platform(getRandomSmallString());
        platform = service.create(platform);

        var persisted = service.getById(platform.getId());

        assertThat(platform.getId()).isEqualTo(persisted.getId());
        assertThat(platform.getName()).isEqualTo(persisted.getName());
    }
}