package com.ebay.behavior.gds.mdm.signal.service.userAnswer;

import com.ebay.behavior.gds.mdm.signal.common.model.TemplateQuestion;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedEvent;
import com.ebay.behavior.gds.mdm.signal.util.ServiceUtils;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.util.Objects;

public abstract class AbstractUserAnswerExpressionSetter extends AbstractUserAnswerSetter {

    @Override
    public void apply(@Valid @NotNull TemplateQuestion question, @Valid @NotNull UnstagedEvent event) {
        validate(question, event);

        val propertyName = question.getAnswerPropertyName();
        if (Objects.nonNull(propertyName)) {
            val answer = question.getAnswerObject();
            ServiceUtils.setModelProperty(event, propertyName, answer);
        }

        updateExpression(event, question);
    }

    @Override
    protected void validate(TemplateQuestion question, UnstagedEvent event) {
        val expression = event.getExpression();

        Validate.isTrue(StringUtils.isNotBlank(question.getAnswer()),
                String.format("Answer must not be blank for question id: %d", question.getId()));
        Validate.isTrue(StringUtils.isNotBlank(expression),
                String.format("Expression must not be blank for question id: %d", question.getId()));
        Validate.isTrue(Objects.nonNull(question.getAnswerPropertyPlaceholder()),
                String.format("Expression placeholder must not be null for question id: %d", question.getId()));

        val placeholder = UserAnswerSetter.toPlaceholder(question.getAnswerPropertyPlaceholder());
        Validate.isTrue(expression.contains(placeholder),
                String.format("Expression [%s] must contain placeholder %s", expression, placeholder));
    }
}
