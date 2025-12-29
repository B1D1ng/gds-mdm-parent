package com.ebay.behavior.gds.mdm.contract.service;

import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.commonSvc.service.AbstractCrudService;
import com.ebay.behavior.gds.mdm.contract.model.Component;
import com.ebay.behavior.gds.mdm.contract.model.Routing;
import com.ebay.behavior.gds.mdm.contract.model.manyToMany.RoutingComponentMapping;
import com.ebay.behavior.gds.mdm.contract.repository.manyToMany.RoutingComponentMappingRepository;
import com.ebay.behavior.gds.mdm.contract.util.EntityUtils;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Validated
public abstract class AbstractComponentService<C extends Component>
        extends AbstractCrudService<C> {

    @Autowired
    private RoutingComponentMappingRepository mappingRepository;

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public C create(@NotNull @Valid C component) {
        validateType(component);
        return super.create(component);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public C update(@NotNull @Valid C component) {
        validateType(component);
        return super.update(component);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<C> createAll(@NotEmpty Set<@Valid C> components) {
        components.forEach(this::validateType);
        return super.createAll(components);
    }

    private void validateType(@NotNull @Valid C component) {
        if (!StringUtils.equals(getModelType().getSimpleName(), component.getType())) {
            throw new IllegalArgumentException("type mismatch from " + getModelType().getSimpleName() + " to " + component.getType());
        }
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void delete(long id) {
        getById(id);

        val routings = getRoutings(id);

        if (!routings.isEmpty()) {
            throw new IllegalArgumentException("Cannot delete component with associated routings.");
        }
        getRepository().deleteById(id);
    }

    @Transactional(readOnly = true)
    public Set<Routing> getRoutings(@PositiveOrZero long id) {
        getById(id); // Ensure the component exists
        return mappingRepository.findByComponentId(id)
                .stream()
                .map(RoutingComponentMapping::getRouting)
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional(readOnly = true)
    public C getByIdWithAssociations(long id) {
        val component = getById(id);
        EntityUtils.initComponentAssociations(component);
        return component;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<C> getAll(@Valid @NotNull Search search) {
        throw new UnsupportedOperationException("Not need to get all components");
    }
}