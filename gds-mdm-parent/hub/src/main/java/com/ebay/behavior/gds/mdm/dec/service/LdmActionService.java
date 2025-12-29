package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.dec.model.LdmEntity;
import com.ebay.behavior.gds.mdm.dec.model.dto.LdmRollbackRequest;
import com.ebay.behavior.gds.mdm.dec.model.dto.StatusUpdateRequest;
import com.ebay.behavior.gds.mdm.dec.model.enums.LdmStatus;
import com.ebay.behavior.gds.mdm.dec.repository.LdmEntityRepository;
import com.ebay.behavior.gds.mdm.dec.util.EntityUtils;

import jakarta.persistence.EntityManager;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.util.TimeUtils.toNowSqlTimestamp;
import static com.ebay.behavior.gds.mdm.dec.util.ApiConstants.MIN_VERSION;
import static com.ebay.behavior.gds.mdm.dec.util.EntityUtils.collectDownstreamLdm;

@Slf4j
@Service
@Validated
public class LdmActionService {

    @Autowired
    private LdmEntityRepository repository;

    @Autowired
    private LdmEntityIndexService indexService;

    @Autowired
    private LdmFieldService fieldService;

    @Autowired
    private DatasetService datasetService;

    @Autowired
    private LdmEntityService entityService;

    @Autowired
    private LdmReadService readService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private LdmErrorHandlingStorageMappingService errorHandlingStorageMappingService;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void delete(@NotNull Long id) {
        repository.findByIdCurrentVersion(id).orElseThrow(() -> new DataNotFoundException(LdmEntity.class, String.valueOf(id)));// ensure entity exists

        // delete all derived ldms & datasets
        Set<Long> downstreamLdmIdSet = new HashSet<>();
        collectDownstreamLdm(Set.of(id), downstreamLdmIdSet, repository);
        if (!downstreamLdmIdSet.isEmpty()) {
            downstreamLdmIdSet.forEach(ldmId -> {
                // delete datasets
                datasetService.deleteByLdmEntityId(ldmId);
                // delete ldm
                deleteLdm(ldmId);
            });
        }

        datasetService.deleteByLdmEntityId(id);
        deleteLdm(id);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteLdm(@NotNull Long id) {
        // delete all fields associated
        fieldService.deleteByLdmEntityId(id);

        // delete all error handling storage mappings associated with the LDM entity
        val historicalVersions = repository.findAllById(id);
        for (LdmEntity version : historicalVersions) {
            val errorMappings = errorHandlingStorageMappingService.getAllByLdmEntityIdAndVersion(id, version.getVersion());
            if (!errorMappings.isEmpty()) {
                errorMappings.forEach(m -> errorHandlingStorageMappingService.delete(m.getId()));
            }
        }

        // delete all historical versions
        if (!historicalVersions.isEmpty()) {
            repository.deleteAll(historicalVersions);
        }

        // delete index
        indexService.delete(id);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public LdmEntity updateStatus(@NotNull Long id, @NotNull LdmStatus status, StatusUpdateRequest request) {
        LdmEntity entity = entityService.getByIdCurrentVersion(id);
        if (request != null) {
            if (!Objects.equals(request.id(), id)) {
                throw new IllegalArgumentException("Request ID does not match the Entity ID");
            }
            val newStatus = LdmStatus.valueOf(request.status().toUpperCase(Locale.US));
            entity.setStatus(newStatus);
            if (StringUtils.isNotBlank(request.updateBy())) {
                entity.setUpdateBy(request.updateBy());
            }
            val updateTime = request.updateDate() != null ? request.updateDate() : toNowSqlTimestamp();
            entity.setUpdateDate(updateTime);
        } else { // will be deprecated after downstream migrate to use above
            entity.setStatus(status);
            entity.setUpdateDate(toNowSqlTimestamp());
        }
        return repository.save(entity);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public LdmEntity rollback(@NotNull Long id, @NotNull LdmRollbackRequest request) {
        Integer targetVersion = getVersion(id, request.version());
        LdmEntity targetVersionEntity = readService.getByIdWithAssociations(VersionedId.of(id, targetVersion), null);
        LdmEntity newEntity = EntityUtils.copyLdm(targetVersionEntity, entityManager, request.updateBy());
        return entityService.saveAsNewVersion(newEntity, null, true);
    }

    private Integer getVersion(Long id, Integer version) {
        if (version != null && version < MIN_VERSION) {
            throw new IllegalArgumentException(String.format("Invalid version %d", version));
        }

        if (version != null) {
            return version;
        }

        LdmEntity entity = readService.getByIdCurrentVersion(id);
        int currentVersion = entity.getVersion();
        if (currentVersion == MIN_VERSION) {
            throw new IllegalArgumentException(String.format("Can't rollback for version %d", currentVersion));
        }
        return currentVersion - 1;
    }
}
