package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.commonSvc.service.AuditService;
import com.ebay.behavior.gds.mdm.commonSvc.service.CrudService;
import com.ebay.behavior.gds.mdm.commonSvc.service.SearchService;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedAttribute;
import com.ebay.behavior.gds.mdm.signal.common.model.UpdateUnstagedAttributeRequest;
import com.ebay.behavior.gds.mdm.signal.common.model.audit.UnstagedAttributeHistory;
import com.ebay.behavior.gds.mdm.signal.common.model.search.AttributeSearchBy;
import com.ebay.behavior.gds.mdm.signal.model.manyToMany.UnstagedFieldAttributeMapping;
import com.ebay.behavior.gds.mdm.signal.repository.UnstagedAttributeRepository;
import com.ebay.behavior.gds.mdm.signal.repository.audit.UnstagedAttributeHistoryRepository;
import com.ebay.behavior.gds.mdm.signal.repository.manyToMany.UnstagedFieldAttributeMappingRepository;
import com.ebay.behavior.gds.mdm.signal.util.ServiceUtils;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Set;

import static java.util.stream.Collectors.toSet;

@Service
@Validated
public class UnstagedAttributeService
        extends AbstractCrudAndAuditService<UnstagedAttribute, UnstagedAttributeHistory>
        implements CrudService<UnstagedAttribute>, SearchService<UnstagedAttribute>, AuditService<UnstagedAttributeHistory> {

    @Getter(AccessLevel.PROTECTED)
    private final Class<UnstagedAttribute> modelType = UnstagedAttribute.class;

    @Getter(AccessLevel.PROTECTED)
    private final Class<UnstagedAttributeHistory> historyModelType = UnstagedAttributeHistory.class;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private UnstagedAttributeRepository repository;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private UnstagedAttributeHistoryRepository historyRepository;

    @Autowired
    private UnstagedFieldAttributeMappingRepository mappingRepository;

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UnstagedAttribute update(@NotNull @Valid UnstagedAttribute attribute) {
        throw new NotImplementedException("Unstaged UnstagedAttribute cannot be updated by design. Delete and create new instead");
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UnstagedAttribute update(@NotNull @Valid UpdateUnstagedAttributeRequest request) {
        var attribute = getById(request.getId());
        ServiceUtils.copyModelProperties(request, attribute);
        return super.update(attribute);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void forceDelete(long id) {
        getById(id);
        val mappingIds = mappingRepository.findByAttributeId(id).stream()
                .map(UnstagedFieldAttributeMapping::getId)
                .collect(toSet());
        mappingRepository.deleteAllById(mappingIds);
        super.delete(id);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public boolean tryDelete(long id) {
        try {
            delete(id);
            return true;
        } catch (IllegalStateException ignored) {
            return false;
        }
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void delete(long id) {
        getById(id);

        if (!mappingRepository.findByAttributeId(id).isEmpty()) {
            throw new IllegalStateException("Cannot delete UnstagedAttribute with associated UnstagedField(s)");
        }

        super.delete(id);
    }

    @SuppressWarnings("PMD.MissingOverride")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void delete(Set<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }

        for (val id : ids) {
            if (!mappingRepository.findByAttributeId(id).isEmpty()) {
                throw new IllegalStateException("Cannot delete UnstagedAttribute with associated UnstagedField(s)");
            }
        }

        repository.deleteAllById(ids);
    }

    @Override
    @Transactional(readOnly = true)
    public UnstagedAttribute getByIdWithAssociations(long id) {
        val attribute = getById(id);
        Hibernate.initialize(attribute.getEvent());
        return attribute;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UnstagedAttribute> getAll(@Valid @NotNull Search search) {
        val searchBy = AttributeSearchBy.valueOf(search.getSearchBy());

        return switch (searchBy) {
            case TAG -> findByTag(search);
            case DESCRIPTION -> findByDescription(search);
        };
    }

    private Page<UnstagedAttribute> findByTag(Search search) {
        return findByTerm(search, repository::findByTag, repository::findByTagStartingWith, repository::findByTagContaining);
    }

    private Page<UnstagedAttribute> findByDescription(Search search) {
        return findByTerm(search, repository::findByDescription, repository::findByDescriptionStartingWith, repository::findByDescriptionContaining);
    }
}