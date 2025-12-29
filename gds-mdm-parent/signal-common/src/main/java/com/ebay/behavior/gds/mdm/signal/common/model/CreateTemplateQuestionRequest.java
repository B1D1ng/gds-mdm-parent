package com.ebay.behavior.gds.mdm.signal.common.model;

import java.util.Set;

public record CreateTemplateQuestionRequest(TemplateQuestion question, Set<Long> eventTemplateIds) {
}
