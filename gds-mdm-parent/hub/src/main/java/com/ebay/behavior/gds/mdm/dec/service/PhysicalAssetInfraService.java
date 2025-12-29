package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.commonSvc.service.AbstractCrudService;
import com.ebay.behavior.gds.mdm.dec.model.PhysicalAssetInfra;
import com.ebay.behavior.gds.mdm.dec.model.enums.InfraType;
import com.ebay.behavior.gds.mdm.dec.model.enums.PlatformEnvironment;
import com.ebay.behavior.gds.mdm.dec.model.enums.PropertyType;
import com.ebay.behavior.gds.mdm.dec.repository.PhysicalAssetInfraRepository;

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

import static com.ebay.behavior.gds.mdm.common.util.CommonValidationUtils.validateForUpdate;

@Service
@Validated
public class PhysicalAssetInfraService extends AbstractCrudService<PhysicalAssetInfra> {

    @Getter(AccessLevel.PROTECTED)
    private final Class<PhysicalAssetInfra> modelType = PhysicalAssetInfra.class;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private PhysicalAssetInfraRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Page<PhysicalAssetInfra> getAll(@Valid @NotNull Search search) {
        throw new NotImplementedException("Not implemented by design");
    }

    @Transactional(readOnly = true)
    public List<PhysicalAssetInfra> getAll() {
        return repository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public PhysicalAssetInfra getByIdWithAssociations(long id) {
        return repository.findById(id).orElseThrow(() -> new DataNotFoundException(PhysicalAssetInfra.class, id));
    }

    @Transactional(readOnly = true)
    public List<PhysicalAssetInfra> getAllByInfraType(InfraType infraType) {
        return repository.findByInfraType(infraType);
    }

    @Transactional(readOnly = true)
    public List<PhysicalAssetInfra> getAllByPropertyType(PropertyType propertyType) {
        return repository.findByPropertyType(propertyType);
    }

    @Transactional(readOnly = true)
    public List<PhysicalAssetInfra> getAllByPlatformEnvironment(PlatformEnvironment environment) {
        return repository.findByPlatformEnvironment(environment);
    }

    @Transactional(readOnly = true)
    public List<PhysicalAssetInfra> getAllByInfraTypeAndPropertyType(InfraType infraType, PropertyType propertyType) {
        return repository.findByInfraTypeAndPropertyType(infraType, propertyType);
    }

    @Transactional(readOnly = true)
    public List<PhysicalAssetInfra> getAllByInfraTypeAndPropertyTypeAndEnvironment(
            InfraType infraType, PropertyType propertyType, PlatformEnvironment environment) {
        return repository.findByInfraTypeAndPropertyTypeAndPlatformEnvironment(infraType, propertyType, environment);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public PhysicalAssetInfra create(@NotNull @Valid PhysicalAssetInfra assetInfra) {
        return getRepository().save(assetInfra);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public PhysicalAssetInfra update(@NotNull @Valid PhysicalAssetInfra assetInfra) {
        validateForUpdate(assetInfra);
        return getRepository().save(assetInfra);
    }
}