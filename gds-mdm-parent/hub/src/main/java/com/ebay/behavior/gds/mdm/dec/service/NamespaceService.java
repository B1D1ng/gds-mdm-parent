package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.commonSvc.service.AbstractCrudService;
import com.ebay.behavior.gds.mdm.dec.model.Namespace;
import com.ebay.behavior.gds.mdm.dec.repository.NamespaceRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.val;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static com.ebay.behavior.gds.mdm.common.util.CommonValidationUtils.validateForUpdate;

@Service
@Validated
public class NamespaceService extends AbstractCrudService<Namespace> {

    @Getter(AccessLevel.PROTECTED)
    private final Class<Namespace> modelType = Namespace.class;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private NamespaceRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Page<Namespace> getAll(@Valid @NotNull Search search) {
        throw new NotImplementedException("Not implemented by design");
    }

    @Transactional(readOnly = true)
    public List<Namespace> getAll() {
        return repository.findAll();
    }

    @Override
    public Namespace getByIdWithAssociations(long id) {
        throw new NotImplementedException("Not implemented by design");
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Namespace create(@NotNull @Valid Namespace namespace) {
        validateName(namespace.getName());
        return getRepository().save(namespace);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Namespace update(@NotNull @Valid Namespace namespace) {
        validateForUpdate(namespace);
        val existing = getById(namespace.getId());
        if (!namespace.getName().equalsIgnoreCase(existing.getName())) {
            validateName(namespace.getName());
        }
        return getRepository().save(namespace);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void validateName(@NotBlank String name) {
        val existingWithSameName = repository.findAllByName(name);
        if (!existingWithSameName.isEmpty()) {
            throw new IllegalArgumentException("Namespace with name " + name + " already exists");
        }
    }

    @Transactional(readOnly = true)
    public List<Namespace> getAllByName(@NotBlank String name) {
        return repository.findAllByName(name);
    }
}
