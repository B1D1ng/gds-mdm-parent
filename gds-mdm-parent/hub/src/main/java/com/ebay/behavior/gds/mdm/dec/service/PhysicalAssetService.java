package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.commonSvc.service.AbstractCrudService;
import com.ebay.behavior.gds.mdm.dec.model.PhysicalAsset;
import com.ebay.behavior.gds.mdm.dec.model.PhysicalAssetInfra;
import com.ebay.behavior.gds.mdm.dec.model.enums.DecEnvironment;
import com.ebay.behavior.gds.mdm.dec.model.enums.PhysicalAssetType;
import com.ebay.behavior.gds.mdm.dec.model.manyToMany.PhysicalAssetInfraMapping;
import com.ebay.behavior.gds.mdm.dec.model.manyToMany.PhysicalAssetLdmMapping;
import com.ebay.behavior.gds.mdm.dec.repository.PhysicalAssetInfraMappingRepository;
import com.ebay.behavior.gds.mdm.dec.repository.PhysicalAssetInfraRepository;
import com.ebay.behavior.gds.mdm.dec.repository.PhysicalAssetLdmMappingRepository;
import com.ebay.behavior.gds.mdm.dec.repository.PhysicalAssetRepository;

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
import java.util.Set;
import java.util.stream.Collectors;

import static com.ebay.behavior.gds.mdm.common.util.CommonValidationUtils.validateForUpdate;

@Service
@Validated
public class PhysicalAssetService extends AbstractCrudService<PhysicalAsset> {

    @Getter(AccessLevel.PROTECTED)
    private final Class<PhysicalAsset> modelType = PhysicalAsset.class;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private PhysicalAssetRepository repository;

    @Autowired
    private PhysicalAssetInfraRepository assetInfraRepository;

    @Autowired
    private PhysicalAssetLdmMappingRepository mappingRepository;

    @Autowired
    private PhysicalAssetRepository assetRepository;

    @Autowired
    private PhysicalAssetInfraMappingRepository infraMappingRepository;

    @Autowired
    private PhysicalStorageService storageService;

    @Autowired
    private LdmBaseEntityService ldmBaseEntityService;

    @Override
    @Transactional(readOnly = true)
    public Page<PhysicalAsset> getAll(@Valid @NotNull Search search) {
        throw new NotImplementedException("Not implemented by design");
    }

    @Transactional(readOnly = true)
    public List<PhysicalAsset> getAll() {
        return repository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public PhysicalAsset getByIdWithAssociations(long id) {
        val asset = repository.findById(id).orElseThrow(() -> new DataNotFoundException(PhysicalAsset.class, id));
        initializeLdmIds(asset);
        initializePhysicalAssetInfraMappings(asset);
        return asset;
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public PhysicalAsset create(@NotNull @Valid PhysicalAsset physicalAsset) {
        validateName(physicalAsset.getAssetType(), physicalAsset.getAssetName());
        return getRepository().save(physicalAsset);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public PhysicalAsset update(@NotNull @Valid PhysicalAsset physicalAsset) {
        validateForUpdate(physicalAsset);
        val existing = getById(physicalAsset.getId());
        if (!physicalAsset.getAssetName().equalsIgnoreCase(existing.getAssetName()) || physicalAsset.getAssetType() != existing.getAssetType()) {
            validateName(physicalAsset.getAssetType(), physicalAsset.getAssetName());
        }
        return getRepository().save(physicalAsset);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void delete(long id) {
        getById(id); // ensure asset exists
        // delete all ldm mappings associated with the asset
        val mappings = mappingRepository.findByPhysicalAssetId(id);
        if (!mappings.isEmpty()) {
            mappingRepository.deleteAll(mappings);
        }
        // delete all storage associated with the asset
        val relatedStorages = storageService.getAllByPhysicalAssetId(id);
        relatedStorages.forEach(s -> storageService.delete(s.getId()));
        super.delete(id);
    }

    @Transactional(readOnly = true)
    public List<PhysicalAsset> getAllWithAssociationsByLdmIdAndPlatform(@NotNull Long ldmId, @NotNull DecEnvironment decEnvironment) {
        List<PhysicalAssetLdmMapping> mappings = mappingRepository.findByLdmBaseEntityId(ldmId);
        List<PhysicalAsset> assets = mappings.stream()
                .map(PhysicalAssetLdmMapping::getPhysicalAsset)
                .filter(asset -> asset.getDecEnvironment() == decEnvironment).collect(Collectors.toList());
        assets.forEach(this::initializeLdmIds);
        assets.forEach(this::initializePhysicalAssetInfraMappings);
        return assets;
    }

    private void initializePhysicalAssetInfraMappings(PhysicalAsset asset) {
        val infraMappings = infraMappingRepository.findByPhysicalAssetId(asset.getId());
        val infras = infraMappings.stream().map(PhysicalAssetInfraMapping::getPhysicalAssetInfra).collect(Collectors.toSet());
        asset.setAssetInfras(infras);
    }

    @Transactional(readOnly = true)
    public List<PhysicalAsset> getAllWithAssociations() {
        val assets = repository.findAll();
        assets.forEach(this::initializeLdmIds);
        assets.forEach(this::initializePhysicalAssetInfraMappings);
        return assets;
    }

    @Transactional(readOnly = true)
    public List<PhysicalAsset> getAllWithAssociationsByLdmId(@NotNull Long ldmId) {
        val mappings = mappingRepository.findByLdmBaseEntityId(ldmId);
        val assets = mappings.stream().map(m -> m.getPhysicalAsset()).collect(Collectors.toList());
        assets.forEach(this::initializeLdmIds);
        assets.forEach(this::initializePhysicalAssetInfraMappings);
        return assets;
    }

    private void initializeLdmIds(PhysicalAsset asset) {
        val ldms = mappingRepository.findByPhysicalAssetId(asset.getId());
        val ldmIds = ldms.stream().map(m -> m.getLdmBaseEntity().getId()).collect(Collectors.toSet());
        asset.setLdmIds(ldmIds);
    }

    private void validateName(@NotNull PhysicalAssetType assetType, @NotBlank String name) {
        val existingWithSameName = repository.findAllByAssetTypeAndAssetName(assetType, name);
        if (!existingWithSameName.isEmpty()) {
            throw new IllegalArgumentException(String.format("PhysicalAsset with name %s already exists for asset type %s", name, assetType));
        }
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public PhysicalAsset savePhysicalAssetLdmMappings(@NotNull Long assetId, @NotNull Set<Long> ldmIds) {
        // get all existing mapped pipelines for the storage
        val existingMappings = mappingRepository.findByPhysicalAssetId(assetId);
        val existingLdmIds = existingMappings.stream()
                .map(mapping -> mapping.getLdmBaseEntity().getId())
                .collect(Collectors.toSet());

        // collect the mappings which should be deleted as they are not in the new set
        val deletedMappings = existingMappings.stream().filter(mapping -> !ldmIds.contains(mapping.getLdmBaseEntity().getId())).collect(Collectors.toSet());
        val newMappingLdmIds = ldmIds.stream().filter(id -> !existingLdmIds.contains(id)).collect(Collectors.toSet());

        mappingRepository.deleteAll(deletedMappings);
        newMappingLdmIds.forEach(ldmId -> createMapping(assetId, ldmId));

        return getByIdWithAssociations(assetId);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void createMapping(long assetId, long ldmId) {
        val asset = getById(assetId);
        val ldm = ldmBaseEntityService.getById(ldmId);
        val mapping = new PhysicalAssetLdmMapping(asset, ldm);
        mappingRepository.save(mapping);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public PhysicalAssetInfra createPhysicalAssetInfra(PhysicalAssetInfra assetInfra) {
        val existing = assetInfraRepository.findByInfraTypeAndPropertyTypeAndPlatformEnvironment(
                assetInfra.getInfraType(), assetInfra.getPropertyType(), assetInfra.getPlatformEnvironment());
        if (!existing.isEmpty()) {
            throw new IllegalArgumentException(String.format("PhysicalAssetInfra with type %s, propertyType %s and environment %s already exists",
                    assetInfra.getInfraType(), assetInfra.getPropertyType(), assetInfra.getPlatformEnvironment()));
        }
        return assetInfraRepository.save(assetInfra);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void createPhysicalAssetInfraMappings(@NotNull Long assetId, @NotNull Long assetInfraId) {
        val asset = assetRepository.findById(assetId).orElseThrow(() -> new DataNotFoundException(PhysicalAsset.class, assetId));
        val infra = assetInfraRepository.findById(assetInfraId)
                .orElseThrow(() -> new DataNotFoundException(PhysicalAssetInfra.class, assetInfraId));

        val existingMapping = infraMappingRepository.findByPhysicalAssetAndPhysicalAssetInfra(asset, infra);
        if (existingMapping.isPresent()) {
            throw new IllegalArgumentException(String.format("Mapping already exists for assetId %d and assetInfraId %d", assetId, assetInfraId));
        }
        val mapping = new PhysicalAssetInfraMapping(asset, infra);
        infraMappingRepository.save(mapping);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public PhysicalAsset createPhysicalAssetInfraMappings(@NotNull Long assetId, @NotNull Set<Long> infraIds) {
        // Get existing mappings and process in a single stream
        val existingMappings = infraMappingRepository.findByPhysicalAssetId(assetId);

        // Collect existing infra IDs
        val existingInfraIds = existingMappings.stream()
                .map(mapping -> mapping.getPhysicalAssetInfra().getId())
                .collect(Collectors.toSet());

        // Collect mappings to delete in a single stream operation
        val mappingsToDelete = existingMappings.stream()
                .filter(mapping -> !infraIds.contains(mapping.getPhysicalAssetInfra().getId()))
                .collect(Collectors.toSet());

        // Identify new infra IDs to add
        val newInfraIds = infraIds.stream()
                .filter(id -> !existingInfraIds.contains(id))
                .collect(Collectors.toSet());

        // Delete removed mappings
        infraMappingRepository.deleteAll(mappingsToDelete);

        // Add new mappings
        newInfraIds.forEach(infraId -> createPhysicalAssetInfraMappings(assetId, infraId));

        return getByIdWithAssociations(assetId);
    }

    @Transactional(readOnly = true)
    public List<PhysicalAssetInfra> getPhysicalAssetInfrasByAssetId(@NotNull Long assetId) {
        val assetInfraMappings = infraMappingRepository.findByPhysicalAssetId(assetId);
        return assetInfraMappings.stream().map(PhysicalAssetInfraMapping::getPhysicalAssetInfra).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PhysicalAssetInfra getPhysicalAssetInfrasByInfraId(@NotNull Long infraId) {
        return assetInfraRepository.findById(infraId).orElseThrow(() -> new DataNotFoundException(PhysicalAssetInfra.class, infraId));
    }
}