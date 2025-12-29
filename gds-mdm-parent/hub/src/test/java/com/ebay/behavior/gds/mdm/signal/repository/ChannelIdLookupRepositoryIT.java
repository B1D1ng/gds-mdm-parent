package com.ebay.behavior.gds.mdm.signal.repository;

import com.ebay.behavior.gds.mdm.signal.common.model.ChannelIdLookup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.channelId;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles(IT)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ChannelIdLookupRepositoryIT {

    @Autowired
    private ChannelIdLookupRepository repository;

    private ChannelIdLookup channel = channelId();

    @BeforeEach
    void setUp() {
        channel = channelId();
        repository.findByName(channel.getName()).ifPresent(repository::delete);
    }

    @Test
    void save() {
        var saved = repository.save(channel);

        var id = repository.findById(saved.getId()).get().getId();
        assertThat(id).isEqualTo(saved.getId());
        assertThat(saved.getChannelId()).isEqualTo(channel.getChannelId());
        assertThat(saved.getName()).isEqualTo(channel.getName());
    }

    @Test
    void findAll() {
        repository.save(channel);

        var results = repository.findAll();
        assertThat(results.size()).isGreaterThanOrEqualTo(1);
        assertThat(results).contains(channel);
    }
}
