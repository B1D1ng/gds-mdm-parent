package com.ebay.behavior.gds.mdm.signal.service.userAnswer;

import com.ebay.behavior.gds.mdm.signal.common.model.TemplateQuestion;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedEvent;

import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Component;

/**
 * Represents the property setter for the answer under a {@link TemplateQuestion}, that can also handle replacing a placeholder under an expression.
 * If a TemplateQuestion has an answerPropertyName set, the Event property will be updated with the provided answer.
 * A TemplateQuestion must have an answerPropertyPlaceholder to identify the relevant placeholder under an expression.
 */
@Component
public class UserAnswerExpressionSetter extends AbstractUserAnswerExpressionSetter {

    @Override
    protected void validate(TemplateQuestion question, UnstagedEvent event) {
        Validate.isTrue(question.getAnswerPropertySetterClass().equals(this.getClass().getSimpleName()), "Invalid setter class");
        super.validate(question, event);
    }
}
