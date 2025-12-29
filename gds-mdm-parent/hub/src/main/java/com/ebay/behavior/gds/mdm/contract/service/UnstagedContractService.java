package com.ebay.behavior.gds.mdm.contract.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.common.model.search.RelationalSearchRequest;
import com.ebay.behavior.gds.mdm.common.model.search.SearchSpecification;
import com.ebay.behavior.gds.mdm.commonSvc.service.AbstractSearchService;
import com.ebay.behavior.gds.mdm.contract.model.ContractConfigView;
import com.ebay.behavior.gds.mdm.contract.model.ContractPipeline;
import com.ebay.behavior.gds.mdm.contract.model.ContractStatus;
import com.ebay.behavior.gds.mdm.contract.model.Routing;
import com.ebay.behavior.gds.mdm.contract.model.UnstagedContract;
import com.ebay.behavior.gds.mdm.contract.model.UpdateContractRequest;
import com.ebay.behavior.gds.mdm.contract.model.search.ContractSearchBy;
import com.ebay.behavior.gds.mdm.contract.repository.ContractConfigViewRepository;
import com.ebay.behavior.gds.mdm.contract.repository.ContractPipelineRepository;
import com.ebay.behavior.gds.mdm.contract.repository.RoutingRepository;
import com.ebay.behavior.gds.mdm.contract.repository.UnstagedContractRepository;
import com.ebay.behavior.gds.mdm.contract.util.EntityUtils;
import com.ebay.behavior.gds.mdm.contract.util.ServiceUtils;

import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Optional;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;
import static com.ebay.behavior.gds.mdm.common.util.CommonValidationUtils.validateForCreate;
import static com.ebay.behavior.gds.mdm.common.util.CommonValidationUtils.validateForUpdate;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toSet;

@Service
@Validated
public class UnstagedContractService
        extends AbstractSearchService<UnstagedContract> {

    @Autowired
    private UnstagedContractRepository repository;

    @Autowired
    private RoutingService routingService;

    @Autowired
    private RoutingRepository routingRepository;

    @Autowired
    private ContractPipelineRepository pipelineRepository;

    @Autowired
    private ContractConfigViewRepository configViewRepository;

    @Transactional(readOnly = true)
    public UnstagedContract getById(@NotNull @Valid VersionedId id) {
        return repository.findById(id)
                .orElseThrow(() -> new DataNotFoundException(UnstagedContract.class, id.toString()));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UnstagedContract create(@NotNull @Valid UnstagedContract unstagedContract) {
        // validate
        validateForCreate(unstagedContract);
        if (!unstagedContract.getStatus().equals(ContractStatus.IN_DEVELOPMENT)) {
            throw new IllegalArgumentException(String.format("Invalid status %s while creating new contract.", unstagedContract.getStatus()));
        }
        return repository.save(unstagedContract);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UnstagedContract update(@NotNull @Valid UnstagedContract unstagedContract) {
        validateForUpdate(unstagedContract);
        getById(VersionedId.of(unstagedContract.getId(), unstagedContract.getVersion())); // Ensure signal exists before update
        return repository.save(unstagedContract);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UnstagedContract updateLatestVersion(@NotNull @Valid UpdateContractRequest request) {
        var contract = getByIdAndLatestVersion(request.getId());
        ServiceUtils.copyModelProperties(request, contract);
        contract.setUpdateBy(null); // Leverage onUpdate of AbstractVersionedAuditable to set the updateBy and updateDate
        contract.setUpdateDate(null);
        return repository.save(contract);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void delete(@NotNull @Valid VersionedId id) {
        val pipelineIds = getPipelines(id).stream().map(ContractPipeline::getId).collect(toSet());
        val routingIds = getRoutings(id).stream().map(Routing::getId).collect(toSet());

        // delete routings
        routingIds.forEach(routingService::delete);

        // delete pipelines
        pipelineIds.forEach(pipelineRepository::deleteById);

        // Finally, delete the signal
        repository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Set<Routing> getRoutings(@NotNull @Valid VersionedId id) {
        getById(id); // Ensure the contract exists
        return Set.copyOf(routingRepository.findByContractIdAndContractVersion(id.getId(), id.getVersion()));
    }

    @Transactional(readOnly = true)
    public int getLatestVersion(@PositiveOrZero long id) {
        return repository.findLatestVersion(id)
                .orElseThrow(() -> new DataNotFoundException(UnstagedContract.class, id));
    }

    /**
     * Deletes the contracts, all its pipeline and routings.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteLatestVersion(@PositiveOrZero long id) {
        val contract = getByIdAndLatestVersion(id);
        delete(VersionedId.of(contract.getId(), contract.getVersion()));
    }

    @Transactional(readOnly = true)
    public UnstagedContract getByIdAndLatestVersion(@PositiveOrZero long id) {
        return repository.findByIdAndLatestVersion(id)
                .orElseThrow(() -> new DataNotFoundException(UnstagedContract.class, id));
    }

    @Transactional(readOnly = true)
    public Set<ContractPipeline> getPipelines(@NotNull @Valid VersionedId id) {
        getById(id); // Ensure the contract exists
        return Set.copyOf(pipelineRepository.findByContractIdAndContractVersion(id.getId(), id.getVersion()));
    }

    @Transactional(readOnly = true)
    public UnstagedContract getByIdWithAssociations(@NotNull @Valid VersionedId id, boolean recursive) {
        val contract = getById(id);
        EntityUtils.initContractAssociations(contract, recursive);
        return contract;
    }

    @Transactional(readOnly = true)
    public Page<UnstagedContract> getAll(@NotNull Optional<String> maybeUser, @Nullable ContractSearchBy searchBy,
                                         @Nullable String searchTerm,
                                         @NotNull @Valid Pageable pageable) {
        if (isNull(searchBy) && isNull(searchTerm)) {
            return maybeUser.map(user -> repository.findAllByCreateByOrOwners(user, pageable))
                    .orElseGet(() -> repository.findAll(pageable));
        }

        return switch (searchBy) {
            case CONTRACT -> getContractByStringSearchTerm(maybeUser, searchTerm, pageable);
            default -> throw new IllegalArgumentException("Not supported yet for searchBy value: " + searchBy);
        };
    }

    private Page<UnstagedContract> getContractByStringSearchTerm(Optional<String> maybeUser, String searchTerm, Pageable pageable) {
        return maybeUser.map(user -> repository.findAllByNameOrDescriptionAndUser(searchTerm, user, pageable))
                .orElseGet(() -> repository.findAllByNameOrDescriptionIgnoreCase(searchTerm, pageable));
    }

    @Transactional(readOnly = true)
    public Page<ContractConfigView> search(@Valid @NotNull RelationalSearchRequest request) {
        if (isNull(request.getSort())) {
            request.setSort(new RelationalSearchRequest.SortRequest(ID, Sort.Direction.ASC));
        }
        return configViewRepository.findAll(
                SearchSpecification.getSpecification(request, false, ContractConfigView.class),
                SearchSpecification.getPageable(request)
        );
    }
}