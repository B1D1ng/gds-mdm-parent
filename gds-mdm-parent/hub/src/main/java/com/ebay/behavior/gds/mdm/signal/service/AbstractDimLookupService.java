package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.AbstractLookup;
import com.ebay.behavior.gds.mdm.commonSvc.service.AbstractLookupService;
import com.ebay.behavior.gds.mdm.signal.common.model.AbstractDimLookup;
import com.ebay.behavior.gds.mdm.signal.repository.DimLookupRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.util.CommonValidationUtils.validateForCreate;
import static com.ebay.behavior.gds.mdm.common.util.CommonValidationUtils.validateForUpdate;

public abstract class AbstractDimLookupService<M extends AbstractDimLookup, D extends AbstractLookup> {

    public static final String SIGNAL_PLATFORM_DIM_NAME = "PLATFORM";
    public static final String SIGNAL_DOMAIN_DIM_NAME = "DOMAIN";
    public static final String SIGNAL_TYPE_DIM_NAME = "SIGNAL_TYPE";

    private Long dimensionTypeId;

    protected abstract DimLookupRepository<M> getRepository();

    protected abstract AbstractLookupService<D> getDimensionLookupService();

    protected abstract Class<M> getModelType();

    protected abstract String getDimensionName();

    @Transactional(readOnly = true)
    public long getDimensionTypeId() {
        if (dimensionTypeId != null) {
            return dimensionTypeId;
        }

        dimensionTypeId = getDimensionLookupService().getByName(getDimensionName()).getId();
        return dimensionTypeId;
    }

    @Transactional(readOnly = true)
    public Set<M> getAll() {
        return Set.copyOf(getRepository().findAllByDimensionTypeId(getDimensionTypeId()));
    }

    @Transactional(readOnly = true)
    public Set<M> getAllByName(@NotEmpty Set<String> names) {
        return new HashSet<>(getRepository().findByDimensionTypeIdAndNameIn(getDimensionTypeId(), names));
    }

    @Transactional(readOnly = true)
    public Optional<M> findByName(@NotBlank String name) {
        return getRepository().findByDimensionTypeIdAndName(getDimensionTypeId(), name);
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
