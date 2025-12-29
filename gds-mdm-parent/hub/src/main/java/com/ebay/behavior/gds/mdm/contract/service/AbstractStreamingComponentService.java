package com.ebay.behavior.gds.mdm.contract.service;

import com.ebay.behavior.gds.mdm.common.model.search.RelationalSearchRequest;
import com.ebay.behavior.gds.mdm.common.model.search.SearchSpecification;
import com.ebay.behavior.gds.mdm.commonSvc.repository.SpecificationRepository;
import com.ebay.behavior.gds.mdm.contract.model.AbstractStreamingComponent;
import com.ebay.behavior.gds.mdm.contract.repository.StreamingConfigRepository;
import com.ebay.behavior.gds.mdm.contract.util.EntityUtils;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;
import static java.util.Objects.isNull;

@Validated
public abstract class AbstractStreamingComponentService<C extends AbstractStreamingComponent>
        extends AbstractComponentService<C> {

    @Autowired
    protected StreamingConfigRepository streamingConfigRepository;

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void delete(long id) {
        getById(id);

        if (!getRoutings(id).isEmpty()) {
            throw new IllegalArgumentException("Cannot delete component with associated routings.");
        }
        streamingConfigRepository.deleteAllByComponentId(id);

        getRepository().deleteById(id);
    }

    @Transactional(readOnly = true)
    public Page<C> search(@Valid @NotNull RelationalSearchRequest request, boolean withAssociations) {
        if (isNull(request.getSort())) {
            request.setSort(new RelationalSearchRequest.SortRequest(ID, Sort.Direction.ASC));
        }
        val specRepo = (SpecificationRepository<C, Long>) getRepository();
        val page = specRepo.findAll(
                SearchSpecification.getSpecification(request, false, getModelType()),
                SearchSpecification.getPageable(request)
        );
        if (withAssociations) {
            page.forEach(EntityUtils::initComponentAssociations);
        }
        return page;
    }
}