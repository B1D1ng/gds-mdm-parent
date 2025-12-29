package com.ebay.behavior.gds.mdm.signal.service.userAnswer;

import com.ebay.behavior.gds.mdm.signal.common.model.AnswerPropertyPlaceholder;
import com.ebay.behavior.gds.mdm.signal.common.model.TemplateQuestion;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedEvent;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Represent types that set the answer of a {@link TemplateQuestion} on an {@link UnstagedEvent} as a property.
 * The answer provided by a user in "create new Signal" flow.
 */
public interface UserAnswerSetter {

    static String toPlaceholder(AnswerPropertyPlaceholder hint) {
        return String.format("${%s}", hint.name());
    }

    void apply(@Valid @NotNull TemplateQuestion question, @Valid @NotNull UnstagedEvent unstagedEvent);
}
