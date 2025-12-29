package com.ebay.behavior.gds.mdm.udf.service;

import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.commonSvc.service.AbstractCrudService;
import com.ebay.behavior.gds.mdm.commonSvc.service.CrudService;
import com.ebay.behavior.gds.mdm.commonSvc.service.SearchService;
import com.ebay.behavior.gds.mdm.udf.common.model.Udf;
import com.ebay.behavior.gds.mdm.udf.common.model.UdfVersions;
import com.ebay.behavior.gds.mdm.udf.common.util.UdfParamUtil;
import com.ebay.behavior.gds.mdm.udf.repository.UdfRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@Validated
public class UdfService
        extends AbstractCrudService<Udf>
        implements CrudService<Udf>, SearchService<Udf> {

    @Getter(AccessLevel.PROTECTED)
    private final Class<Udf> modelType = Udf.class;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private UdfRepository repository;

    @Autowired
    private UdfVersionService udfVersionService;

    @Override
    public Page<Udf> getAll(@Valid @NotNull Search search) {
        throw new NotImplementedException("Not implemented by design");
    }

    @Transactional(readOnly = true)
    public Page<Udf> getAll(@NotNull @Valid Pageable pageable) {
        var udfList = repository.findAll(pageable);
        for (Udf udf: udfList) {
            Hibernate.initialize(udf.getUdfVersions());
            Hibernate.initialize(udf.getUdfStubs());
            Hibernate.initialize(udf.getUdfUsages());
        }
        return udfList;
    }

    @Override
    @Transactional(readOnly = true)
    public Udf getByIdWithAssociations(long id) {
        var udf = getById(id);
        Hibernate.initialize(udf.getUdfVersions());
        Hibernate.initialize(udf.getUdfStubs());
        Hibernate.initialize(udf.getUdfUsages());
        return udf;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Udf create(@NotNull Udf udf, @NotEmpty Set<String> nameSet) {
        if (!UdfParamUtil.isValid(udf.getParameters())) {
            throw new IllegalArgumentException("UDF parameters are invalid");
        }
        var udfList = getByNames(nameSet, true);
        Udf persisted;
        UdfVersions udfVersion;
        if (udfList.isEmpty()) {
            persisted = create(udf);
            udfVersion = UdfVersions.setupUdfVersions(persisted, 1L);
        } else {
            persisted = udfList.get(0);
            udf.setId(persisted.getId());
            Long latestVersion = persisted.getUdfVersions().stream().max(Comparator.comparing(UdfVersions::getId)).get().getVersion();
            udfVersion = UdfVersions.setupUdfVersions(udf, latestVersion + 1);
        }
        udfVersion = udfVersionService.create(udfVersion);
        if (!udfList.isEmpty()) {
            persisted.setType(udf.getType());
            persisted.setCode(udf.getCode());
            persisted.setLanguage(udf.getLanguage());
            persisted.setOwners(udf.getOwners());
            persisted.setDomain(udf.getDomain());
            persisted.setFunctionSourceType(udf.getFunctionSourceType());
            persisted.setParameters(udf.getParameters());
            persisted.setCreateBy(udf.getCreateBy() != null ? udf.getCreateBy() : "gmstest"); // temp fallback for null createBy in request
            persisted.setUpdateBy(udf.getUpdateBy() != null ? udf.getUpdateBy() : "gmstest"); // temp fallback for null updateBy in request
            persisted.setDescription(udf.getDescription());
            persisted.setCurrentVersionId(udfVersion.getId());
            persisted = update(persisted);
        }
        return persisted;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Udf update(Udf udf, Long id) {
        if (!UdfParamUtil.isValid(udf.getParameters())) {
            throw new IllegalArgumentException("UDF parameters are invalid");
        }
        var udfList = getByIds(Set.of(id), true);
        Udf persisted = null;
        UdfVersions udfVersion;
        if (!udfList.isEmpty()) {
            Udf persistedUdf = udfList.get(0);
            Set<UdfVersions> vers = persistedUdf.getUdfVersions();
            udfVersion = UdfVersions.of(udf, persistedUdf.getCurrentVersionId());
            if (vers != null && !vers.isEmpty()) {
                for (UdfVersions ver : vers) {
                    if (ver.getId().equals(udf.getCurrentVersionId())) {
                        udfVersion.setRevision(ver.getRevision());
                        udfVersion.setCreateBy(ver.getCreateBy());
                        udfVersion.setCreateDate(ver.getCreateDate());
                        udfVersion.setUpdateBy(ver.getUpdateBy());
                        udfVersion.setUpdateDate(ver.getUpdateDate());
                    }
                }
            }
            persisted = update(udf);
            udfVersionService.update(udfVersion);
        }
        return persisted;
    }

    @Transactional(readOnly = true)
    public List<Udf> getByIds(@NotEmpty Set<Long> ids, @NotNull Boolean withAssociations) {
        var udfList = findAllById(ids);
        if (withAssociations) {
            for (Udf udf: udfList) {
                Hibernate.initialize(udf.getUdfVersions());
                Hibernate.initialize(udf.getUdfStubs());
                Hibernate.initialize(udf.getUdfUsages());
            }
        }
        return udfList;
    }

    @Transactional(readOnly = true)
    public List<Udf> getByNames(@NotEmpty Set<String> names, @NotNull Boolean withAssociations) {
        var udfList = repository.findByNameIn(names);
        if (withAssociations) {
            for (Udf udf: udfList) {
                Hibernate.initialize(udf.getUdfVersions());
                Hibernate.initialize(udf.getUdfStubs());
                Hibernate.initialize(udf.getUdfUsages());
            }
        }
        return udfList;
    }

    @Transactional(readOnly = true)
    public Udf getById(@NotNull Long id, @NotNull Boolean withAssociations) {
        var udf = getById(id);
        if (withAssociations) {
            Hibernate.initialize(udf.getUdfVersions());
            Hibernate.initialize(udf.getUdfStubs());
            Hibernate.initialize(udf.getUdfUsages());
        }
        return udf;
    }
}
