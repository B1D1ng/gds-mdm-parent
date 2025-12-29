package com.ebay.behavior.gds.mdm.contract.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.commonSvc.service.AbstractCrudService;
import com.ebay.behavior.gds.mdm.contract.model.ContractStatus;
import com.ebay.behavior.gds.mdm.contract.model.Routing;
import com.ebay.behavior.gds.mdm.contract.model.manyToMany.RoutingComponentMapping;
import com.ebay.behavior.gds.mdm.contract.repository.RoutingRepository;
import com.ebay.behavior.gds.mdm.contract.repository.UnstagedContractRepository;
import com.ebay.behavior.gds.mdm.contract.repository.manyToMany.RoutingComponentMappingRepository;
import com.ebay.behavior.gds.mdm.contract.util.EntityUtils;

import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.val;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

@Service
@Validated
public class RoutingService
        extends AbstractCrudService<Routing> {

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private RoutingRepository repository;

    @Autowired
    private UnstagedContractRepository contractRepository;

    @Autowired
    private RoutingComponentMappingRepository mappingRepository;

    @Override
    protected Class<Routing> getModelType() {
        return Routing.class;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void updateComponentsMapping(@PositiveOrZero long routingId, @Nullable List<@NotNull @PositiveOrZero Long> componentIds) {
        val routing = getById(routingId);
        mappingRepository.deleteAllByRoutingId(routing.getId());
        if (isNotEmpty(componentIds)) {
            List<RoutingComponentMapping> mappings = newArrayList();
            for (int i = 0; i < componentIds.size(); i++) {
                mappings.add(new RoutingComponentMapping(routingId, componentIds.get(i), i));
            }
            mappingRepository.saveAll(mappings);
        }
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void delete(long id) {
        val routing = getById(id);
        val contract = contractRepository.findById(VersionedId.of(routing.getContractId(), routing.getContractVersion()))
                .orElseThrow(() -> new DataNotFoundException("Contract not found"));
        Validate.isTrue(ContractStatus.IN_DEVELOPMENT.equals(contract.getStatus()),
                "Cannot delete routing in non-development contract");
        repository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Routing getByIdWithAssociations(long id) {
        return getByIdWithAssociations(id, false);
    }

    @Transactional(readOnly = true)
    public Routing getByIdWithAssociations(@PositiveOrZero long id, boolean recursive) {
        val routing = getById(id);
        EntityUtils.initRoutingAssociations(routing, recursive);
        return routing;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Routing> getAll(@Valid @NotNull Search search) {
        throw new UnsupportedOperationException("Not need to search routing");
    }
}
