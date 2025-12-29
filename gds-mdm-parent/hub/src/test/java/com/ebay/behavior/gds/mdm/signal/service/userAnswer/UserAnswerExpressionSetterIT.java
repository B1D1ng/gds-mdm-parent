package com.ebay.behavior.gds.mdm.signal.service.userAnswer;

import com.ebay.behavior.gds.mdm.common.model.JavaType;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.signal.common.model.AnswerPropertyPlaceholder.BUSINESS_OUTCOME_ACTION;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.templateQuestion;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedEvent;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.INTEGRATION_TEST;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomSmallString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles(IT)
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class UserAnswerExpressionSetterIT {

    @Autowired
    private UserAnswerExpressionSetter setter;

    @Test
    void apply_withoutPropertyName() {
        var action = getRandomSmallString();
        var question = templateQuestion()
                .setAnswerPropertyName(null)
                .setAnswerJavaType(JavaType.LONG)
                .setAnswer(action)
                .setAnswerPropertyPlaceholder(BUSINESS_OUTCOME_ACTION)
                .setAnswerPropertySetterClass(UserAnswerExpressionSetter.class.getSimpleName())
                .setIsList(false);

        var placeholder = UserAnswerSetter.toPlaceholder(BUSINESS_OUTCOME_ACTION);
        var expression = '[' + placeholder + "].contains(event.context.pageInteractionContext.pageId)";
        var event = unstagedEvent().toBuilder().expression(expression).build();

        setter.apply(question, event);

        assertThat(event.getExpression()).doesNotContain(placeholder);
        assertThat(event.getExpression()).contains(action);
    }

    @Test
    void apply_withPropertyName() {
        var action = getRandomSmallString();
        var question = templateQuestion()
                .setAnswerPropertyName("githubRepositoryUrl")
                .setAnswerJavaType(JavaType.STRING)
                .setAnswer(action)
                .setAnswerPropertyPlaceholder(BUSINESS_OUTCOME_ACTION)
                .setAnswerPropertySetterClass(UserAnswerExpressionSetter.class.getSimpleName())
                .setIsList(false);

        var placeholder = UserAnswerSetter.toPlaceholder(BUSINESS_OUTCOME_ACTION);
        var expression = '[' + placeholder + "].contains(event.context.pageInteractionContext.pageId)";
        var event = unstagedEvent().toBuilder().expression(expression).build();

        setter.apply(question, event);

        assertThat(event.getExpression()).doesNotContain(placeholder);
        assertThat(event.getExpression()).contains(action);
    }

    @Test
    void apply_withoutExpressionPlaceholder_error() {
        var action = getRandomSmallString();
        var question = templateQuestion()
                .setAnswerPropertyName(null)
                .setAnswerJavaType(JavaType.STRING)
                .setAnswer(action)
                .setAnswerPropertyPlaceholder(BUSINESS_OUTCOME_ACTION)
                .setAnswerPropertySetterClass(UserAnswerExpressionSetter.class.getSimpleName())
                .setIsList(false);

        var event = unstagedEvent().toBuilder().expression("event.context.pageInteractionContext.pageId").build();

        assertThatThrownBy(() -> setter.apply(question, event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must contain placeholder");
    }

    @Test
    void apply_withoutAnswer_error() {
        var question = templateQuestion()
                .setAnswerPropertyName("githubRepositoryUrl")
                .setAnswerJavaType(JavaType.LONG)
                .setAnswer(null)
                .setAnswerPropertyPlaceholder(BUSINESS_OUTCOME_ACTION)
                .setAnswerPropertySetterClass(UserAnswerExpressionSetter.class.getSimpleName())
                .setIsList(false);

        var event = unstagedEvent().toBuilder().expression("event.context.pageInteractionContext.pageId").build();

        assertThatThrownBy(() -> setter.apply(question, event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Answer");
    }

    @Test
    void apply_InvalidSetterClass_error() {
        var question = templateQuestion()
                .setAnswerPropertyName("prop")
                .setAnswerJavaType(JavaType.LONG)
                .setAnswer(null)
                .setAnswerPropertyPlaceholder(BUSINESS_OUTCOME_ACTION)
                .setAnswerPropertySetterClass("wrongClass")
                .setIsList(false);

        var event = unstagedEvent().toBuilder().expression("event.context.pageInteractionContext.pageId").build();

        assertThatThrownBy(() -> setter.apply(question, event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid setter class");
    }
}