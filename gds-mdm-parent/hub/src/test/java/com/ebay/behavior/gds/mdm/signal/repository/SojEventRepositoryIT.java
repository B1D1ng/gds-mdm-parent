package com.ebay.behavior.gds.mdm.signal.repository;

import com.ebay.behavior.gds.mdm.signal.common.model.SojEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.sojEvent;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles(IT)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class SojEventRepositoryIT {

    @Autowired
    private SojEventRepository repository;

    private SojEvent model;

    @BeforeEach
    void setUp() {
        model = sojEvent("test", 123L, 1L, 2L);
        repository.findByActionAndPageIdAndModuleIdAndClickId(model.getAction(), model.getPageId(), model.getModuleId(), model.getClickId())
                .ifPresent(repository::delete);
    }

    @Test
    void save() {
        var saved = repository.save(model);

        var id = repository.findById(saved.getId()).get().getId();
        assertThat(id).isEqualTo(saved.getId());
        assertThat(saved.getAction()).isEqualTo(model.getAction());
        assertThat(saved.getPageId()).isEqualTo(model.getPageId());
        assertThat(saved.getModuleId()).isEqualTo(model.getModuleId());
        assertThat(saved.getClickId()).isEqualTo(model.getClickId());
    }

    @Test
    void findByActionAndPageIdAndModuleIdAndClickId() {
        repository.save(model);

        var result = repository.findByActionAndPageIdAndModuleIdAndClickId(model.getAction(), model.getPageId(), model.getModuleId(), model.getClickId());
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get().getAction()).isEqualTo(model.getAction());
        assertThat(result.get().getPageId()).isEqualTo(model.getPageId());
        assertThat(result.get().getModuleId()).isEqualTo(model.getModuleId());
        assertThat(result.get().getClickId()).isEqualTo(model.getClickId());
    }

    @Test
    void findByPageIdIn() {
        repository.save(model);

        var result = repository.findByPageIdIn(Set.of(model.getPageId()));
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.iterator().next().getPageId()).isEqualTo(model.getPageId());
    }

    @Test
    void findByModuleIdIn() {
        repository.save(model);

        var result = repository.findByModuleIdIn(Set.of(model.getModuleId()));
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.iterator().next().getModuleId()).isEqualTo(model.getModuleId());
    }

    @Test
    void findByClickIdIn() {
        repository.save(model);

        var result = repository.findByClickIdIn(Set.of(model.getClickId()));
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.iterator().next().getClickId()).isEqualTo(model.getClickId());
    }

    @Test
    void findByModuleIdInAndClickIdIn() {
        repository.save(model);

        var result = repository.findByModuleIdInAndClickIdIn(Set.of(model.getModuleId()), Set.of(model.getClickId()));
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.iterator().next().getModuleId()).isEqualTo(model.getModuleId());
        assertThat(result.iterator().next().getClickId()).isEqualTo(model.getClickId());
    }
}
