package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.commonSvc.service.AbstractCrudService;
import com.ebay.behavior.gds.mdm.dec.model.PhysicalAssetInfraGlobalProperty;
import com.ebay.behavior.gds.mdm.dec.model.enums.InfraType;
import com.ebay.behavior.gds.mdm.dec.model.enums.PropertyType;
import com.ebay.behavior.gds.mdm.dec.repository.PhysicalAssetInfraGlobalPropertyRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Optional;

import static com.ebay.behavior.gds.mdm.common.util.CommonValidationUtils.validateForUpdate;

@Service
@Validated
public class PhysicalAssetInfraGlobalPropertyService extends AbstractCrudService<PhysicalAssetInfraGlobalProperty> {

    @Getter(AccessLevel.PROTECTED)
    private final Class<PhysicalAssetInfraGlobalProperty> modelType = PhysicalAssetInfraGlobalProperty.class;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private PhysicalAssetInfraGlobalPropertyRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Page<PhysicalAssetInfraGlobalProperty> getAll(@Valid @NotNull Search search) {
        throw new NotImplementedException("Not implemented by design");
    }

    @Transactional(readOnly = true)
    public List<PhysicalAssetInfraGlobalProperty> getAll() {
        return repository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public PhysicalAssetInfraGlobalProperty getByIdWithAssociations(long id) {
        return repository.findById(id).orElseThrow(() -> new DataNotFoundException(PhysicalAssetInfraGlobalProperty.class, id));
    }

    @Transactional(readOnly = true)
    public List<PhysicalAssetInfraGlobalProperty> getAllByInfraType(@NotNull InfraType infraType) {
        return repository.findByInfraType(infraType);
    }

    @Transactional(readOnly = true)
    public List<PhysicalAssetInfraGlobalProperty> getAllByPropertyType(@NotNull PropertyType propertyType) {
        return repository.findByPropertyType(propertyType);
    }

    @Transactional(readOnly = true)
    public List<PhysicalAssetInfraGlobalProperty> getAllByInfraTypeAndPropertyType(@NotNull InfraType infraType, @NotNull PropertyType propertyType) {
        return repository.findByInfraTypeAndPropertyType(infraType, propertyType);
    }

    @Transactional(readOnly = true)
    public Optional<PhysicalAssetInfraGlobalProperty> getByInfraTypeAndPropertyType(@NotNull InfraType infraType, @NotNull PropertyType propertyType) {
        return repository.findFirstByInfraTypeAndPropertyType(infraType, propertyType);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public PhysicalAssetInfraGlobalProperty create(@NotNull @Valid PhysicalAssetInfraGlobalProperty prop) {
        return getRepository().save(prop);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public PhysicalAssetInfraGlobalProperty update(@NotNull @Valid PhysicalAssetInfraGlobalProperty prop) {
        validateForUpdate(prop);
        return getRepository().save(prop);
    }
}