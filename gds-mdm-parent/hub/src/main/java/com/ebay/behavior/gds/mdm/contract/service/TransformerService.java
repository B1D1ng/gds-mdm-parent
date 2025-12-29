package com.ebay.behavior.gds.mdm.contract.service;

import com.ebay.behavior.gds.mdm.contract.model.Filter;
import com.ebay.behavior.gds.mdm.contract.model.Transformation;
import com.ebay.behavior.gds.mdm.contract.model.Transformer;
import com.ebay.behavior.gds.mdm.contract.repository.FilterRepository;
import com.ebay.behavior.gds.mdm.contract.repository.TransformationRepository;
import com.ebay.behavior.gds.mdm.contract.repository.TransformerRepository;

import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Set;

@Service
@Validated
public class TransformerService
        extends AbstractComponentService<Transformer> {

    @Getter(AccessLevel.PROTECTED)
    private final Class<Transformer> modelType = Transformer.class;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private TransformerRepository repository;

    @Autowired
    private TransformationRepository transformationRepository;

    @Autowired
    private FilterRepository filterRepository;

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void delete(long id) {
        transformationRepository.deleteAllByComponentId(id);
        filterRepository.deleteAllByComponentId(id);
        super.delete(id);
    }

    @Transactional(readOnly = true)
    public Set<Transformation> getTransformations(long id) {
        getById(id); // Ensure the transformer exists
        return Set.copyOf(transformationRepository.findByComponentId(id));
    }

    @Transactional(readOnly = true)
    public Set<Filter> getFilters(long id) {
        getById(id); // Ensure the transformer exists
        return Set.copyOf(filterRepository.findByComponentId(id));
    }
}
