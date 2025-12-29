package com.ebay.behavior.gds.mdm.commonSvc.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.Auditable;
import com.ebay.behavior.gds.mdm.common.util.CommonValidationUtils;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.util.CommonValidationUtils.validateForCreate;
import static com.ebay.behavior.gds.mdm.common.util.CommonValidationUtils.validateForUpdate;

public abstract class AbstractCrudService<M extends Auditable> extends AbstractSearchService<M> implements CrudService<M>, SearchService<M> {

    protected abstract JpaRepository<M, Long> getRepository();

    protected abstract Class<M> getModelType();

    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Override
    public M create(@NotNull @Valid M model) {
        validateForCreate(model);
        try {
            return getRepository().save(model);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException(ex); // DataIntegrityViolation means FK constraint violation because of bad parentId inside the model
        }
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Override
    public List<M> createAll(@NotEmpty Set<@Valid M> models) {
        models.forEach(CommonValidationUtils::validateForCreate);
        try {
            return getRepository().saveAllAndFlush(models);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException(ex); // DataIntegrityViolation means FK constraint violation because of bad parentId inside the model
        }
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Override
    public M update(@NotNull @Valid M model) {
        validateForUpdate(model);
        getById(model.getId()); // Ensure signal exists before update

        return getRepository().save(model);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Override
    public void delete(long id) {
        getRepository().deleteById(id);
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<M> findById(long id) {
        return getRepository().findById(id);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Override
    public M getById(long id) {
        return getRepository().findById(id)
                .orElseThrow(() -> new DataNotFoundException(getModelType(), id));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Override
    public List<M> findAllById(@NotNull Set<Long> ids) {
        return getRepository().findAllById(ids);
    }
}
