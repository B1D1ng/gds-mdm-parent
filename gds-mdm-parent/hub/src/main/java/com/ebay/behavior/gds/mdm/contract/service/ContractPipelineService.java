package com.ebay.behavior.gds.mdm.contract.service;

import com.ebay.behavior.gds.mdm.common.model.Environment;
import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.commonSvc.service.AbstractCrudService;
import com.ebay.behavior.gds.mdm.contract.model.ContractPipeline;
import com.ebay.behavior.gds.mdm.contract.model.DeployScope;
import com.ebay.behavior.gds.mdm.contract.model.UpdatePipelineRequest;
import com.ebay.behavior.gds.mdm.contract.repository.ContractPipelineRepository;
import com.ebay.behavior.gds.mdm.contract.util.ServiceUtils;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.val;
import org.apache.commons.lang3.NotImplementedException;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Optional;

@Service
@Validated
public class ContractPipelineService
        extends AbstractCrudService<ContractPipeline> {

    @Getter(AccessLevel.PROTECTED)
    private final Class<ContractPipeline> modelType = ContractPipeline.class;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private ContractPipelineRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Page<ContractPipeline> getAll(@Valid @NotNull Search search) {
        throw new NotImplementedException("Not implemented by design");
    }

    @Transactional(readOnly = true)
    public Optional<ContractPipeline> findPipelineByContract(@NotNull @PositiveOrZero Long contractId,
                                                             @NotNull @Positive Integer contractVersion,
                                                             @NotNull Environment environment) {
        return repository.findPipeline(contractId, contractVersion, environment);
    }

    @Transactional(readOnly = true)
    public Optional<ContractPipeline> findPipelineByContract(@NotNull @PositiveOrZero Long contractId,
                                                             @NotNull @Positive Integer contractVersion,
                                                             @NotNull Environment environment,
                                                             @NotNull DeployScope deployScope) {
        return repository.findPipeline(contractId, contractVersion, environment, deployScope);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ContractPipeline update(@NotNull @Valid UpdatePipelineRequest request) {
        var pipeline = getById(request.getId());
        ServiceUtils.copyModelProperties(request, pipeline);
        return repository.save(pipeline);
    }

    @Override
    @Transactional(readOnly = true)
    public ContractPipeline getByIdWithAssociations(long id) {
        val pipeline = getById(id);
        Hibernate.initialize(pipeline.getUnstagedContract());
        return pipeline;
    }
}
