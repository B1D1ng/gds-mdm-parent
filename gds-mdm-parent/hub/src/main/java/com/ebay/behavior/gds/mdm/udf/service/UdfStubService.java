package com.ebay.behavior.gds.mdm.udf.service;

import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.commonSvc.service.AbstractCrudService;
import com.ebay.behavior.gds.mdm.commonSvc.service.CrudService;
import com.ebay.behavior.gds.mdm.commonSvc.service.SearchService;
import com.ebay.behavior.gds.mdm.udf.common.model.UdfStub;
import com.ebay.behavior.gds.mdm.udf.common.model.UdfStubVersions;
import com.ebay.behavior.gds.mdm.udf.common.util.UdfParamUtil;
import com.ebay.behavior.gds.mdm.udf.repository.UdfStubRepository;

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
public class UdfStubService
        extends AbstractCrudService<UdfStub>
        implements CrudService<UdfStub>, SearchService<UdfStub> {

    @Getter(AccessLevel.PROTECTED)
    private final Class<UdfStub> modelType = UdfStub.class;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private UdfStubRepository repository;

    @Autowired
    private UdfStubVersionService udfStubVersionService;

    @Override
    public Page<UdfStub> getAll(@Valid @NotNull Search search) {
        throw new NotImplementedException("Not implemented by design");
    }

    @Override
    public UdfStub getByIdWithAssociations(long id) {
        throw new NotImplementedException("Not implemented by design");
    }

    @Transactional(readOnly = true)
    public List<UdfStub> getByNames(Set<String> names, Boolean withAssociations) {
        var udfStubList = repository.findByStubNameIn(names);
        if (withAssociations) {
            for (UdfStub udfStub: udfStubList) {
                Hibernate.initialize(udfStub.getUdfStubVersions());
            }
        }
        return udfStubList;
    }

    @Transactional(readOnly = true)
    public List<UdfStub> getByIds(Set<Long> ids, Boolean withAssociations) {
        var udfStubList = findAllById(ids);
        if (withAssociations) {
            for (UdfStub udfStub: udfStubList) {
                Hibernate.initialize(udfStub.getUdfStubVersions());
            }
        }
        return udfStubList;
    }

    @SuppressWarnings("PMD.CognitiveComplexity")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UdfStub create(@NotNull UdfStub udfStub, @NotEmpty Set<String> nameSet) {
        if (!UdfParamUtil.isValid(udfStub.getStubParameters())) {
            throw new IllegalArgumentException("UDF Stub parameters are invalid");
        }
        var udfStubList = getByNames(nameSet, true);
        UdfStub persisted;
        UdfStubVersions udfStubVersion;
        UdfStub existedStub = null;
        if (udfStubList.isEmpty()) {
            persisted = create(udfStub);
            udfStubVersion = UdfStubVersions.setupUdfStubVersions(persisted, 1L);
        } else {
            for (UdfStub stubExisted : udfStubList) {
                if (stubExisted.getStubName().equals(udfStub.getStubName()) && stubExisted.getLanguage().equals(udfStub.getLanguage())) {
                    existedStub = stubExisted;
                }
            }
            if (existedStub == null) {
                persisted = create(udfStub);
                udfStubVersion = UdfStubVersions.setupUdfStubVersions(persisted, 1L);
            } else {
                persisted = existedStub;
                udfStub.setId(persisted.getId());
                Long latestVersion = persisted.getUdfStubVersions().stream().max(Comparator.comparing(UdfStubVersions::getId)).get().getStubVersion();
                udfStubVersion = UdfStubVersions.setupUdfStubVersions(udfStub, latestVersion + 1);
            }
        }
        udfStubVersion = udfStubVersionService.create(udfStubVersion);
        if (!udfStubList.isEmpty() && existedStub != null) {
            persisted.setCurrentVersionId(udfStubVersion.getId());
            persisted.setCurrentUdfVersionId(udfStub.getCurrentUdfVersionId());
            persisted.setUdfId(udfStub.getUdfId());
            persisted.setStubRuntimeContext(udfStub.getStubRuntimeContext());
            persisted.setStubParameters(udfStub.getStubParameters());
            persisted.setCreateBy(udfStub.getCreateBy() != null ? udfStub.getCreateBy() : "gmstest"); // temp fallback for null createBy in request
            persisted.setUpdateBy(udfStub.getUpdateBy() != null ? udfStub.getUpdateBy() : "gmstest"); // temp fallback for null updateBy in request
            persisted.setStubCode(udfStub.getStubCode());
            persisted.setOwners(udfStub.getOwners());
            persisted.setStubType(udfStub.getStubType());
            persisted.setDescription(udfStub.getDescription());
            persisted = update(persisted);
        }
        return persisted;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UdfStub update(UdfStub udfStub, Long id) {
        if (!UdfParamUtil.isValid(udfStub.getStubParameters())) {
            throw new IllegalArgumentException("UDF Stub parameters are invalid");
        }
        var udfStubList = getByIds(Set.of(id), true);
        UdfStub persisted = null;
        UdfStubVersions udfStubVersion;
        if (!udfStubList.isEmpty()) {
            UdfStub persistedUdfStub = udfStubList.get(0);
            Set<UdfStubVersions> vers = persistedUdfStub.getUdfStubVersions();
            udfStubVersion = UdfStubVersions.of(udfStub, persistedUdfStub.getCurrentVersionId());
            if (vers != null && !vers.isEmpty()) {
                for (UdfStubVersions ver : vers) {
                    if (ver.getId().equals(udfStub.getCurrentVersionId())) {
                        udfStubVersion.setRevision(ver.getRevision());
                        udfStubVersion.setCreateBy(ver.getCreateBy());
                        udfStubVersion.setCreateDate(ver.getCreateDate());
                        udfStubVersion.setUpdateBy(ver.getUpdateBy());
                        udfStubVersion.setUpdateDate(ver.getUpdateDate());
                    }
                }
            }
            persisted = update(udfStub);
            udfStubVersionService.update(udfStubVersion);
        }
        return persisted;
    }
}
