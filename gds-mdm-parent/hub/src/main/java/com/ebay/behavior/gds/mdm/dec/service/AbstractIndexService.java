package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.commonSvc.service.AbstractCrudService;
import com.ebay.behavior.gds.mdm.dec.model.AbstractIndex;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.val;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Validated
@Service
public abstract class AbstractIndexService<M extends AbstractIndex> extends AbstractCrudService<M> {

    @Override
    @Transactional(readOnly = true)
    public Page<M> getAll(@Valid @NotNull Search search) {
        throw new NotImplementedException("Not implemented by design");
    }

    @Override
    public M getByIdWithAssociations(long id) {
        throw new NotImplementedException("Not implemented by design");
    }

    @Transactional
    public void updateVersion(@NotNull Long id, @NotNull Integer version) {
        val index = getById(id);
        index.setCurrentVersion(version);
        getRepository().save(index);
    }
}
