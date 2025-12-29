package com.ebay.behavior.gds.mdm.signal.common.service.pmsvc;

import com.ebay.behavior.gds.mdm.signal.common.model.external.pmsvc.v1.request.SearchIn;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.EXACT_MATCH;
import static com.ebay.behavior.gds.mdm.signal.common.model.external.pmsvc.v1.request.SearchIn.ID;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.util.ReflectionTestUtils.setField;

class PmsvcServiceTest {

    private static final List<TmsExtractor<?>> extractors = List.of(new TmsActionExtractor());

    private static final PmsvcService service = new PmsvcService();

    @BeforeAll
    static void setUpAll() {
        setField(service, "extractors", extractors);
        service.init();
    }

    @Test
    void searchFamilies_noClassFound_error() {
        assertThatThrownBy(() -> service.searchFamilies("AB", SearchIn.NAME, EXACT_MATCH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No extractor found for type");
    }

    @Test
    void validateSearchTerm_error() {
        assertThatThrownBy(() -> service.searchClicks("AB", ID, EXACT_MATCH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("searchTerm must be a number");
    }
}