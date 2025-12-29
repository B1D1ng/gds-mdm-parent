package com.ebay.behavior.gds.mdm.commonSvc.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.AbstractLookup;
import com.ebay.behavior.gds.mdm.commonSvc.repository.LookupRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.util.CommonValidationUtils.validateForCreate;
import static com.ebay.behavior.gds.mdm.common.util.CommonValidationUtils.validateForUpdate;

public abstract class AbstractLookupService<M extends AbstractLookup> {

    protected abstract LookupRepository<M> getRepository();

    protected abstract Class<M> getModelType();

    @Transactional(readOnly = true)
    public Set<M> getAll() {
        return Set.copyOf(getRepository().findAll());
    }

    @Transactional(readOnly = true)
    public Set<M> getAllByName(@NotEmpty Set<String> names) {
        return getRepository().findByNameIn(names);
    }

    @Transactional(readOnly = true)
    public Optional<M> findByName(@NotBlank String name) {
        return getRepository().findByName(name);
    }

    @Transactional(readOnly = true)
    public M getByName(@NotBlank String name) {
        return findByName(name).orElseThrow(() -> new DataNotFoundException(getModelType(), name));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteByName(String name) {
        findByName(name).ifPresent(getRepository()::delete);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public M createIfAbsent(@NotNull @Valid M model) {
        return findByName(model.getName()).orElseGet(() -> create(model));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public M create(@NotNull @Valid M model) {
        validateForCreate(model);
        try {
            return getRepository().save(model);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException(ex); // DataIntegrityViolation means FK constraint violation because of bad parentId inside the model
        }
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public M getById(long id) {
        return getRepository().findById(id)
                .orElseThrow(() -> new DataNotFoundException(getModelType(), id));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public M update(@NotNull @Valid M model) {
        validateForUpdate(model);
        getById(model.getId());
        return getRepository().save(model);
    }
}
