package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.commonSvc.service.AbstractCrudService;
import com.ebay.behavior.gds.mdm.commonSvc.service.CrudService;
import com.ebay.behavior.gds.mdm.commonSvc.service.SearchService;
import com.ebay.behavior.gds.mdm.signal.common.model.TemplateQuestion;
import com.ebay.behavior.gds.mdm.signal.model.manyToMany.TemplateQuestionEventMapping;
import com.ebay.behavior.gds.mdm.signal.repository.TemplateQuestionRepository;
import com.ebay.behavior.gds.mdm.signal.repository.manyToMany.TemplateQuestionEventMappingRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Set;

@Service
@Validated
public class TemplateQuestionService
        extends AbstractCrudService<TemplateQuestion>
        implements CrudService<TemplateQuestion>, SearchService<TemplateQuestion> {

    @Getter(AccessLevel.PROTECTED)
    private final Class<TemplateQuestion> modelType = TemplateQuestion.class;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private TemplateQuestionRepository repository;

    @Autowired
    private TemplateQuestionEventMappingRepository mappingRepository;

    @Autowired
    private EventTemplateService eventService;

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TemplateQuestion create(@NotNull @Valid TemplateQuestion question) {
        throw new NotImplementedException("Not implemented by design. Use create(TemplateQuestion question, eventTemplateIds) instead");
    }

    /**
     * Creates a new questionTemplate and associates it with the given eventTemplates.
     * Since each event is associated with an event, the question signal is also associated with the event.
     *
     * @param question TemplateQuestion to create.
     * @param eventIds Set of EventTemplate IDs to associate with the question.
     * @return Created TemplateQuestion.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TemplateQuestion create(@NotNull @Valid TemplateQuestion question, @Nullable Set<Long> eventIds) {
        val created = super.create(question);

        if (CollectionUtils.isEmpty(eventIds)) {
            return created;
        }

        // associate question with eventTemplates
        eventIds.forEach(eventId -> createEventMapping(created.getId(), eventId));
        return created;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<TemplateQuestion> createAll(@NotEmpty Set<@Valid TemplateQuestion> questions, @Nullable Set<Long> eventIds) {
        return questions.stream()
                .map(question -> create(question, eventIds))
                .toList();
    }

    @Override
    public List<TemplateQuestion> createAll(@NotEmpty Set<@Valid TemplateQuestion> models) {
        throw new NotImplementedException("Not implemented by design");
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void delete(long id) {
        getById(id);
        super.delete(id);
    }

    @Override
    public Page<TemplateQuestion> getAll(Search search) {
        throw new NotImplementedException("Not implemented by design");
    }

    @Override
    @Transactional(readOnly = true)
    public TemplateQuestion getByIdWithAssociations(long id) {
        val question = getById(id);
        Hibernate.initialize(question.getEvents());
        return question;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TemplateQuestionEventMapping createEventMapping(long questionId, long eventId) {
        val question = getById(questionId);
        val event = eventService.getById(eventId);

        val mapping = new TemplateQuestionEventMapping(question, event);
        return mappingRepository.save(mapping);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteEventMapping(long questionId, long eventId) {
        val mapping = mappingRepository.findByQuestionIdAndEventTemplateId(questionId, eventId)
                .orElseThrow(() -> new DataNotFoundException(TemplateQuestionEventMapping.class, questionId, eventId));
        mappingRepository.deleteById(mapping.getId());
    }
}