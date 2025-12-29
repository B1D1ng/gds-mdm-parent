package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.commonSvc.service.AbstractCrudService;
import com.ebay.behavior.gds.mdm.dec.model.PhysicalAssetAttribute;
import com.ebay.behavior.gds.mdm.dec.model.enums.PhysicalAssetAttributeName;
import com.ebay.behavior.gds.mdm.dec.repository.PhysicalAssetAttributeRepository;
import com.ebay.behavior.gds.mdm.dec.repository.PhysicalAssetRepository;

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

@Service
@Validated
public class PhysicalAssetAttributeService extends AbstractCrudService<PhysicalAssetAttribute> {

    @Getter(AccessLevel.PROTECTED)
    private final Class<PhysicalAssetAttribute> modelType = PhysicalAssetAttribute.class;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private PhysicalAssetAttributeRepository repository;

    @Autowired
    private PhysicalAssetRepository assetRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<PhysicalAssetAttribute> getAll(@Valid @NotNull Search search) {
        throw new NotImplementedException("Not implemented by design");
    }

    @Transactional(readOnly = true)
    public List<PhysicalAssetAttribute> getAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public List<PhysicalAssetAttribute> getAllByAssetId(Long assetId) {
        return repository.findByAssetId(assetId);
    }

    @Override
    @Transactional(readOnly = true)
    public PhysicalAssetAttribute getByIdWithAssociations(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new DataNotFoundException(PhysicalAssetAttribute.class, id));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public PhysicalAssetAttribute createOrUpdateAttribute(Long assetId, PhysicalAssetAttributeName attributeName, String attributeValue) {
        // Check if this attribute already exists
        List<PhysicalAssetAttribute> existingAttributes = repository.findByAssetId(assetId);

        for (PhysicalAssetAttribute existing : existingAttributes) {
            if (existing.getAttributeName() == attributeName) {
                // Update existing attribute
                existing.setAttributeValue(attributeValue);
                return repository.save(existing);
            }
        }

        // Create new attribute
        PhysicalAssetAttribute newAttribute = PhysicalAssetAttribute.builder()
                .attributeName(attributeName)
                .attributeValue(attributeValue)
                .assetId(assetId)
                .build();

        return repository.save(newAttribute);
    }
}
