package com.ebay.behavior.gds.mdm.signal.service.userAnswer;

import com.ebay.behavior.gds.mdm.signal.common.model.TemplateQuestion;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedEvent;
import com.ebay.behavior.gds.mdm.signal.util.ServiceUtils;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Component;

/**
 * Represents the simple property setter for the answer under a {@link TemplateQuestion}.
 * A TemplateQuestion must have an answerPropertyName to identify the relevant property under an Event.
 */
@Component
public class UserAnswerPropertySetter extends AbstractUserAnswerSetter {

    @Override
    public void apply(@Valid @NotNull TemplateQuestion question, @Valid @NotNull UnstagedEvent event) {
        validate(question, event);
        ServiceUtils.setModelProperty(event, question.getAnswerPropertyName(), question.getAnswerObject());
    }

    @Override
    protected void validate(TemplateQuestion question, UnstagedEvent event) {
        Validate.notBlank(question.getAnswerPropertyName(), "Answer property name must not be blank");
    }
}
