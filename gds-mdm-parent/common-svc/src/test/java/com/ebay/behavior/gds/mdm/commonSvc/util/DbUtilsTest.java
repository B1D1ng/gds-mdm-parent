package com.ebay.behavior.gds.mdm.commonSvc.util;

import com.ebay.behavior.gds.mdm.common.model.Auditable;

import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

class DbUtilsTest {

    @Test
    void getPage_withOneElement() {
        var model = new TestModel();

        var page = DbUtils.getPage(model);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0)).isEqualTo(model);
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void getPage_withTwoElements() {
        var model1 = new TestModel();
        var model2 = new TestModel();

        var page = DbUtils.getPage(model1, model2);

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent()).containsExactly(model1, model2);
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    void getAuditablePageable() {
        int pageNumber = 1;
        int pageSize = 10;

        val pageable = DbUtils.getAuditablePageable(pageNumber, pageSize);

        assertThat(pageable.getPageNumber()).isEqualTo(pageNumber);
        assertThat(pageable.getPageSize()).isEqualTo(pageSize);
        assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, Auditable.UPDATE_DATE));
    }
}