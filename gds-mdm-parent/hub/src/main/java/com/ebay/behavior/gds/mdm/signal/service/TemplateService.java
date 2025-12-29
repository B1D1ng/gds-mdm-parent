package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.signal.common.model.TemplateQuestion;

import java.util.Set;

public interface TemplateService {
    Set<TemplateQuestion> getQuestions(long id);
}
