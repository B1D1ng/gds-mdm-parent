package com.ebay.behavior.gds.mdm.signal.service.userAnswer;

import com.ebay.behavior.gds.mdm.signal.common.model.TemplateQuestion;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedEvent;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.val;

public abstract class AbstractUserAnswerSetter implements UserAnswerSetter {

    @Override
    public abstract void apply(@Valid @NotNull TemplateQuestion question, @Valid @NotNull UnstagedEvent unstagedEvent);

    protected abstract void validate(@Valid @NotNull TemplateQuestion question, @Valid @NotNull UnstagedEvent unstagedEvent);

    protected void updateExpression(UnstagedEvent event, TemplateQuestion question) {
        val answer = question.getAnswer();
        val hint = question.getAnswerPropertyPlaceholder();
        val expression = event.getExpression();
        val placeholder = UserAnswerSetter.toPlaceholder(hint);
        val updatedExpression = expression.replace(placeholder, answer);
        event.setExpression(updatedExpression);
    }
}
