package com.ebay.behavior.gds.mdm.signal.service;

import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Function;

import static com.ebay.behavior.gds.mdm.common.model.ExpressionType.JEXL;
import static com.ebay.behavior.gds.mdm.common.model.ExpressionType.LITERAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class UnstagedEventServiceTest {

    @Spy
    private UnstagedEventService service;

    private final Function<Long, Object> idGetter = id -> 1L;
    private final String pageAnchor = "${PAGE_ID}";
    private final String moduleAnchor = "${MODULE_ID}";
    private final String clickAnchor = "${CLICK_ID}";
    private String expression;

    @BeforeEach
    void setUp() {
        expression = "[" + pageAnchor + "].contains(event.context.pageInteractionContext.pageId) and"
                + " [" + moduleAnchor + "].contains(event.context.pageInteractionContext.moduleId) and"
                + " [" + clickAnchor + "].contains(event.context.pageInteractionContext.clickId)";
    }

    @Test
    void getAssetIds_noIdsInExpression1() {
        val ids = service.getAssetIds(service.pageIdPattern, idGetter, "page", "abc123");

        assertThat(ids).isEmpty();
    }

    @Test
    void getAssetIds_noIdsInExpression2() {
        expression = expression.replace(pageAnchor, "abc");

        val ids = service.getAssetIds(service.pageIdPattern, idGetter, "page", expression);

        assertThat(ids).isEmpty();
    }

    @Test
    void getAssetIds_singleIdInExpression() {
        expression = expression.replace(pageAnchor, "123");

        val ids = service.getAssetIds(service.pageIdPattern, idGetter, "page", expression);

        assertThat(ids).containsExactly(123L);
    }

    @Test
    void getAssetIds_multipleIdsInExpression() {
        expression = expression.replace(pageAnchor, " 123, 456,789,876 ");

        val ids = service.getAssetIds(service.pageIdPattern, idGetter, "page", expression);

        assertThat(ids).containsExactlyInAnyOrder(123L, 456L, 789L, 876L);
    }

    @Test
    void getAssetIds_badExpression_emptyIds() {
        expression = expression.replace(pageAnchor, " 123,abc,789");

        val ids = service.getAssetIds(service.pageIdPattern, idGetter, "page", expression);

        assertThat(ids).isEmpty();
    }

    @Test
    void getAssetIds_pageAndModuleAndClick() {
        expression = expression.replace(pageAnchor, " 123");
        expression = expression.replace(moduleAnchor, "456");
        expression = expression.replace(clickAnchor, "789");

        val pageIds = service.getAssetIds(service.pageIdPattern, idGetter, "page", expression);
        assertThat(pageIds).containsOnly(123L);

        val moduleIds = service.getAssetIds(service.moduleIdPattern, idGetter, "module", expression);
        assertThat(moduleIds).containsOnly(456L);

        val clickIds = service.getAssetIds(service.clickIdPattern, idGetter, "click", expression);
        assertThat(clickIds).containsOnly(789L);
    }

    @Test
    void evaluateExpressionChange_notJexlExpression_error() {
        assertThatThrownBy(() -> service.evaluateExpressionUpdate(1L, expression, LITERAL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only JEXL");
    }

    @Test
    void evaluateExpressionChange_invalidExpression_error() {
        assertThatThrownBy(() -> service.evaluateExpressionUpdate(1L, "bbb bbb", JEXL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid JEXL");
    }
}