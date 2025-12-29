package com.ebay.behavior.gds.mdm.common.model;

import com.ebay.behavior.gds.mdm.common.testUtil.TestModel;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EsPageTest {

    private final List<TestModel> content = List.of(new TestModel(), new TestModel());
    private final EsPageable pageable = EsPageable.of(1, 10);

    @Test
    void ctor() {
        long totalElements = content.size();

        var page = new EsPage<>(pageable, content, totalElements);

        assertThat(page.getPageable()).isEqualTo(pageable);
        assertThat(page.getContent()).isEqualTo(content);
        assertThat(page.getTotalElements()).isEqualTo(totalElements);
        assertThat(page.getNumberOfElements()).isEqualTo(content.size());
    }

    @Test
    void ctor_nullPageable_error() {
        assertThatThrownBy(() -> new EsPage<>(null, content, content.size()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Pageable must not be null");
    }

    @Test
    void ctor_contentSizeGreaterThanTotalElements_error() {
        long totalElements = 1;
        assertThatThrownBy(() -> new EsPage<>(pageable, content, totalElements))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Content size must be less than or equal to total elements");
    }

    @Test
    void ctor_nullContentNonZeroTotalElements_error() {
        long totalElements = 1;

        assertThatThrownBy(() -> new EsPage<>(pageable, null, totalElements))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Content size must be 0 if content is null");
    }

    @Test
    void ctor_nullContentZeroTotalElements() {
        long totalElements = 0;

        var page = new EsPage<>(pageable, null, totalElements);

        assertThat(page.getPageable()).isEqualTo(pageable);
        assertThat(page.getContent()).isNull();
        assertThat(page.getTotalElements()).isEqualTo(totalElements);
        assertThat(page.getNumberOfElements()).isEqualTo(0);
    }
}