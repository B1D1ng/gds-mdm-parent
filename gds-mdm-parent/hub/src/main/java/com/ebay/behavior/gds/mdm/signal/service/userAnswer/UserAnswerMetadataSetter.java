package com.ebay.behavior.gds.mdm.signal.service.userAnswer;

import com.ebay.behavior.gds.mdm.signal.common.model.AnswerPropertyPlaceholder;
import com.ebay.behavior.gds.mdm.signal.common.model.TemplateQuestion;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedEvent;
import com.ebay.behavior.gds.mdm.signal.common.service.pmsvc.PmsvcService;

import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.UNCHECKED;

/**
 * Represents the property setter for page/module/click ids.
 * The provided answer under a {@link TemplateQuestion} must be a list of ids (Long), or a single id.
 * All ids are validated against PMSVC service for validity. If valid, ids answer replaces a placeholder under an expression.
 * Relevant id list Event property will be updated with the provided answer as well.
 * A TemplateQuestion must have an answerPropertyName to identify the relevant property under an Event.
 * A TemplateQuestion must have an answerPropertyPlaceholder to identify the relevant placeholder under an expression.
 */
@Component
public class UserAnswerMetadataSetter extends AbstractUserAnswerExpressionSetter {

    @Autowired
    private PmsvcService pmsvcService;

    @Override
    protected void validate(TemplateQuestion question, UnstagedEvent event) {
        Validate.isTrue(question.getAnswerPropertySetterClass().equals(this.getClass().getSimpleName()), "Invalid setter class");
        super.validate(question, event);

        Validate.isTrue(question.getIsList(), "Answer must be a list");
        Validate.isTrue(StringUtils.isNotBlank(question.getAnswerPropertyName()), "AnswerPropertyName must not be blank");

        @SuppressWarnings(UNCHECKED)
        val answer = (Set<Long>) question.getAnswerObject();
        val placeholder = question.getAnswerPropertyPlaceholder();
        Validate.notEmpty(answer, "Metadata list must not be empty");
        answer.forEach(id -> validateMetadataId(id, placeholder));
    }

    private void validateMetadataId(long id, AnswerPropertyPlaceholder placeholder) {
        switch (placeholder) {
            case PAGE_IDS:
                val page = pmsvcService.getPageById(id);
                Validate.isTrue(Objects.nonNull(page), "Page not found");
                break;
            case MODULE_IDS:
                val module = pmsvcService.getModuleById(id);
                Validate.isTrue(Objects.nonNull(module), "Module not found");
                break;
            case CLICK_IDS:
                val click = pmsvcService.getClickById(id);
                Validate.isTrue(Objects.nonNull(click), "Click not found");
                break;
            default:
                throw new IllegalStateException(String.format("Unexpected placeholder value: %s", placeholder));
        }
    }
}
