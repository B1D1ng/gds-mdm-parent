package com.ebay.behavior.gds.mdm.signal.common.service.pmsvc;

import com.ebay.behavior.gds.mdm.signal.common.TestApplication;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.CONTAINS;
import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.EXACT_MATCH;
import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.STARTS_WITH;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.signal.common.model.external.pmsvc.v1.request.SearchIn.NAME;
import static com.ebay.behavior.gds.mdm.signal.common.model.external.pmsvc.v1.request.SearchIn.SOJOURNER_NAME;
import static com.ebay.behavior.gds.mdm.signal.common.testUtil.TestUtils.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles(IT)
@Tag(INTEGRATION_TEST)
@SpringBootTest(
        classes = TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class PmsvcServiceIT {

    @Autowired
    private PmsvcService service;

    @Test
    void searchActions_termTooSmall_error() {
        assertThatThrownBy(() -> service.searchActions("AB", NAME, EXACT_MATCH))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("searchTerm must be at least");
    }

    @Test
    void searchActions() {
        var models = service.searchActions("TESTABCA", NAME, EXACT_MATCH);

        assertThat(models).isNotEmpty();
    }

    @Test
    void searchFamily() {
        var models = service.searchFamilies("TEST", NAME, CONTAINS);

        assertThat(models).isNotEmpty();
    }

    @Test
    void searchProperty() {
        var models = service.searchProperties("testproperty0", SOJOURNER_NAME, STARTS_WITH);

        assertThat(models).isNotEmpty();
    }

    @Test
    void searchClicks() {
        var models = service.searchClicks("test_clck", NAME, EXACT_MATCH);

        assertThat(models).isNotEmpty();
    }

    @Test
    void searchPages() {
        var models = service.searchPages("testnew2__DefaultPage", NAME, EXACT_MATCH);

        assertThat(models).isNotEmpty();
    }

    @Test
    void searchModules() {
        var models = service.searchModules("TestMod1", NAME, EXACT_MATCH);

        assertThat(models).isNotEmpty();
    }

    @Test
    void getPageByIds() {
        long id = 2_481_888;

        var model = service.getPageByIds(Set.of(id)).iterator().next();

        assertThat(model.getId()).isEqualTo(id);
    }

    @Test
    void getModuleByIds() {
        long id = 1_682;

        var model = service.getModuleByIds(Set.of(id)).iterator().next();

        assertThat(model.getId()).isEqualTo(id);
    }

    @Test
    void getClickByIds() {
        long id = 3_180;

        var model = service.getClickByIds(Set.of(id)).iterator().next();

        assertThat(model.getId()).isEqualTo(id);
    }

    @Test
    void getPropertyById_notFound() {
        long id = 45_926;

        var model = service.getPropertyById(id);

        assertThat(model.getId()).isEqualTo(id);
    }

    @Test
    void getFamilyById_notFound() {
        long id = 5_000_013_209L;

        var model = service.getFamilyById(id);

        assertThat(model.getId()).isEqualTo(id);
    }

    @Test
    void getActionById_notFound() {
        long id = 5_000_009_738L;

        var model = service.getActionById(id);

        assertThat(model.getId()).isEqualTo(id);
    }
}