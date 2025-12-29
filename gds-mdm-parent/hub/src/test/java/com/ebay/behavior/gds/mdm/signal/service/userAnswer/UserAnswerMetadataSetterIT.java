package com.ebay.behavior.gds.mdm.signal.service.userAnswer;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.JavaType;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.signal.common.model.AnswerPropertyPlaceholder.CLICK_IDS;
import static com.ebay.behavior.gds.mdm.signal.common.model.AnswerPropertyPlaceholder.MODULE_IDS;
import static com.ebay.behavior.gds.mdm.signal.common.model.AnswerPropertyPlaceholder.PAGE_IDS;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.templateQuestion;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedEvent;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles(IT)
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class UserAnswerMetadataSetterIT {

    @Autowired
    private UserAnswerMetadataSetter setter;

    @Test
    void apply_pageId() {
        var pageId = 2_481_888L;
        var question = templateQuestion()
                .setAnswerJavaType(JavaType.LONG)
                .setAnswerPropertyName("pageIds")
                .setAnswer(String.valueOf(pageId))
                .setAnswerPropertyPlaceholder(PAGE_IDS)
                .setAnswerPropertySetterClass(UserAnswerMetadataSetter.class.getSimpleName())
                .setIsList(true); // we expect a list of page ids, but a single value should work as well

        var placeholder = UserAnswerSetter.toPlaceholder(PAGE_IDS);
        var expression = '[' + placeholder + "].contains(event.context.pageInteractionContext.pageId)";
        var event = unstagedEvent().toBuilder().expression(expression).build();

        setter.apply(question, event);

        assertThat(event.getPageIds()).hasSize(1).containsOnly(pageId);
        assertThat(event.getExpression()).doesNotContain(placeholder);
        assertThat(event.getExpression()).contains(String.valueOf(pageId));
    }

    @Test
    void apply_moduleId() {
        var moduleId = 1_682L;
        var question = templateQuestion()
                .setAnswerJavaType(JavaType.LONG)
                .setAnswerPropertyName("moduleIds")
                .setAnswer(String.valueOf(moduleId))
                .setAnswerPropertyPlaceholder(MODULE_IDS)
                .setAnswerPropertySetterClass(UserAnswerMetadataSetter.class.getSimpleName())
                .setIsList(true); // we expect a list of page ids, but a single value should work as well

        var placeholder = UserAnswerSetter.toPlaceholder(MODULE_IDS);
        var expression = '[' + placeholder + "].contains(event.context.pageInteractionContext.moduleId)";
        var event = unstagedEvent().toBuilder().expression(expression).build();

        setter.apply(question, event);

        assertThat(event.getModuleIds()).hasSize(1).containsOnly(moduleId);
        assertThat(event.getExpression()).doesNotContain(placeholder);
        assertThat(event.getExpression()).contains(String.valueOf(moduleId));
    }

    @Test
    void apply_clickId() {
        var clickId = 3_180L;
        var question = templateQuestion()
                .setAnswerJavaType(JavaType.LONG)
                .setAnswerPropertyName("clickIds")
                .setAnswer(String.valueOf(clickId))
                .setAnswerPropertyPlaceholder(CLICK_IDS)
                .setAnswerPropertySetterClass(UserAnswerMetadataSetter.class.getSimpleName())
                .setIsList(true); // we expect a list of page ids, but a single value should work as well

        var placeholder = UserAnswerSetter.toPlaceholder(CLICK_IDS);
        var expression = '[' + placeholder + "].contains(event.context.pageInteractionContext.clickId)";
        var event = unstagedEvent().toBuilder().expression(expression).build();

        setter.apply(question, event);

        assertThat(event.getClickIds()).hasSize(1).containsOnly(clickId);
        assertThat(event.getExpression()).doesNotContain(placeholder);
        assertThat(event.getExpression()).contains(String.valueOf(clickId));
    }

    @Test
    void apply_clickIdNotFound_error() {
        var clickId = 999_999_998L;
        var question = templateQuestion()
                .setAnswerJavaType(JavaType.LONG)
                .setAnswerPropertyName("clickIds")
                .setAnswer(String.valueOf(clickId))
                .setAnswerPropertyPlaceholder(CLICK_IDS)
                .setAnswerPropertySetterClass(UserAnswerMetadataSetter.class.getSimpleName())
                .setIsList(true);

        var placeholder = UserAnswerSetter.toPlaceholder(CLICK_IDS);
        var expression = '[' + placeholder + "].contains(event.context.pageInteractionContext.clickId)";
        var event = unstagedEvent().toBuilder().expression(expression).build();

        assertThatThrownBy(() -> setter.apply(question, event))
                .isInstanceOf(DataNotFoundException.class)
                .hasMessageContaining("doesn't found");
    }

    @Test
    void apply_withoutAnswerPropertyName_error() {
        var question = templateQuestion()
                .setAnswerJavaType(JavaType.LONG)
                .setAnswerPropertyName("   ")
                .setAnswer(String.valueOf(1L))
                .setAnswerPropertyPlaceholder(CLICK_IDS)
                .setAnswerPropertySetterClass(UserAnswerMetadataSetter.class.getSimpleName())
                .setIsList(true);

        var event = unstagedEvent().toBuilder()
                .expression("event.context.pageInteractionContext.pageId " + UserAnswerSetter.toPlaceholder(CLICK_IDS))
                .build();

        assertThatThrownBy(() -> setter.apply(question, event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("AnswerPropertyName");
    }

    @Test
    void apply_invalidSetterClass_error() {
        var question = templateQuestion()
                .setAnswerJavaType(JavaType.LONG)
                .setAnswerPropertyName("clickIds")
                .setAnswer(String.valueOf(1L))
                .setAnswerPropertyPlaceholder(CLICK_IDS)
                .setAnswerPropertySetterClass("noSuchClass")
                .setIsList(true);

        var event = unstagedEvent().toBuilder().expression("event.context.pageInteractionContext.pageId").build();

        assertThatThrownBy(() -> setter.apply(question, event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid setter class");
    }
}