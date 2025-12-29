package com.ebay.behavior.gds.mdm.signal.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.platform;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomSmallString;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles(IT)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class PlatformLookupRepositoryIT {

    @Autowired
    private PlatformLookupRepository repository;

    @Test
    void save() {
        var platform = platform(getRandomSmallString());

        var saved = repository.save(platform);

        var id = repository.findById(saved.getId()).get().getId();
        assertThat(id).isEqualTo(saved.getId());
        assertThat(saved.getName()).isEqualTo(platform.getName());
    }

    @Test
    void findAll() {
        var platform = platform(getRandomSmallString());
        repository.save(platform);

        var results = repository.findAll();
        assertThat(results.size()).isGreaterThanOrEqualTo(1);
        assertThat(results).contains(platform);
    }
}
