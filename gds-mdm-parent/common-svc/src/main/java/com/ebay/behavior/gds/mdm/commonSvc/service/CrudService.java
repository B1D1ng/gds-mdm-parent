package com.ebay.behavior.gds.mdm.commonSvc.service;

import com.ebay.behavior.gds.mdm.common.model.Auditable;
import com.ebay.behavior.gds.mdm.common.model.search.Search;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface CrudService<M extends Auditable> {

    Page<M> getAll(@Valid @NotNull Search search);

    M getByIdWithAssociations(long id);

    M create(@NotNull @Valid M model);

    List<M> createAll(@NotEmpty Set<@Valid M> models);

    M update(@NotNull @Valid M model);

    void delete(long id);

    Optional<M> findById(long id);

    M getById(long id);

    List<M> findAllById(@NotNull Set<Long> ids);
}
