package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.dec.model.LdmBaseEntity;
import com.ebay.behavior.gds.mdm.dec.model.LdmEntity;
import com.ebay.behavior.gds.mdm.dec.model.LdmEntityIndex;
import com.ebay.behavior.gds.mdm.dec.model.LdmField;
import com.ebay.behavior.gds.mdm.dec.model.Namespace;
import com.ebay.behavior.gds.mdm.dec.model.enums.NamespaceType;
import com.ebay.behavior.gds.mdm.dec.model.enums.ViewType;
import com.ebay.behavior.gds.mdm.dec.repository.LdmEntityRepository;
import com.ebay.behavior.gds.mdm.dec.util.EntityUtils;

import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.util.CommonValidationUtils.validateForUpdate;
import static com.ebay.behavior.gds.mdm.dec.util.ApiConstants.MIN_VERSION;

@Slf4j
@Service
@Validated
public class LdmEntityService extends AbstractVersionModelService<LdmEntity, LdmEntityIndex> {

    @Getter(AccessLevel.PROTECTED)
    private final Class<LdmEntity> modelType = LdmEntity.class;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private LdmEntityRepository repository;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private LdmEntityIndexService indexService;

    @Autowired
    private LdmReadService readService;

    @Autowired
    private LdmFieldService fieldService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private LdmBaseEntityService baseEntityService;

    @Autowired
    private LdmEntityVersioningService versioningService;
    
    @Autowired
    private LdmEntityBasicInfoService basicInfoService;
    
    @Autowired
    private LdmEntityValidationService validationService;

    @Autowired
    private NamespaceService namespaceService;

    // Basic operations
    @Override
    @Transactional(readOnly = true)
    public List<LdmEntity> getAllCurrentVersion() {
        return readService.getAllCurrentVersion();
    }

    @Override
    @Transactional(readOnly = true)
    public LdmEntity getByIdCurrentVersion(@NotNull Long id) {
        return readService.getByIdCurrentVersion(id);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void validateModelForUpdate(@Valid @NotNull LdmEntity model) {
        validationService.validateModelForUpdate(model);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public LdmEntity create(@Valid @NotNull LdmEntity ldmEntity) {
        // handle view type
        ViewType viewType = ldmEntity.getViewType() == null ? ViewType.NONE : ldmEntity.getViewType();
        ldmEntity.setViewType(viewType);
        validateAndInitializeLdm(ldmEntity);
        LdmEntityIndex initializedEntity = indexService.initialize(ldmEntity);
        ldmEntity.setBaseEntityId(initializedEntity.getBaseEntityId());
        return versioningService.saveVersion(initializedEntity.getId(), MIN_VERSION, ldmEntity, ldmEntity.getRequestId());
    }

    @Transactional(readOnly = true)
    public void validateAndInitializeLdm(@Valid @NotNull LdmEntity entity) {
        if (BooleanUtils.isNotTrue(entity.getIsDcs())) { // for non-dcs ldm
            entity.setName(entity.getName().toLowerCase(Locale.US));
            return;
        }
        // for dcs ldm
        validationService.validateDcsModel(entity);
        // get parent ldm
        Long parentLdmId = Long.valueOf(entity.getUpstreamLdmIds().get(0));
        // validate parent ldm existence
        LdmEntity parentLdm = readService.getByIdCurrentVersion(parentLdmId);
        // validate if dcs ldm's namespace is same as its parent ldm
        if (!parentLdm.getNamespaceId().equals(entity.getNamespaceId())) {
            throw new IllegalArgumentException("Namespace of dcs ldm is not the same as its parent ldm");
        }
        // if parent ldm is Base LDM, set its dcs ldm as RAW view type
        Namespace parentLdmNamespace = namespaceService.getById(parentLdm.getNamespaceId());
        if (parentLdmNamespace.getType() == NamespaceType.BASE) {
            entity.setViewType(ViewType.RAW);
        }
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public LdmEntity update(@Valid @NotNull LdmEntity entity) {
        val existing = readService.getByIdCurrentVersion(entity.getId());
        validationService.validateVersionAndRevision(entity, existing);
        validateAndInitializeLdm(entity);
        basicInfoService.handleBasicInfoUpdate(entity, existing.getBaseEntityId(), existing.getViewType());
        if (entity.getFields() != null && !entity.getFields().isEmpty()) {
            fieldService.updateFields(entity.getId(), existing, entity.getFields());
        }
        entityManager.detach(existing);
        // base entity id, create user and time should be the same as the initial version
        entity.setBaseEntityId(existing.getBaseEntityId());
        entity.setCreateBy(existing.getCreateBy());
        entity.setCreateDate(existing.getCreateDate());
        return getRepository().save(entity);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public LdmEntity saveVersion(@NotNull Long entityId, @NotNull Integer newVersion, @Valid @NotNull LdmEntity ldmEntity, Long changeRequestId) {
        return versioningService.saveVersion(entityId, newVersion, ldmEntity, changeRequestId);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public LdmEntity saveAsNewVersion(@Valid @NotNull LdmEntity ldmEntity, Long changeRequestId, boolean isRollback) {
        validateAndInitializeLdm(ldmEntity);
        return versioningService.saveAsNewVersion(ldmEntity, changeRequestId, isRollback);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Set<LdmField> updateFields(@NotNull Long id, @NotNull Set<LdmField> fields) {
        LdmEntity entity = readService.getByIdCurrentVersion(id);
        return fieldService.updateFields(id, entity, fields);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public LdmBaseEntity updateBaseEntity(@Valid @NotNull LdmBaseEntity entity) {
        validateForUpdate(entity);
        LdmBaseEntity existing = baseEntityService.getById(entity.getId());
        // does not allow to change namespace
        boolean namespaceChange = entity.getNamespaceId() != null && !entity.getNamespaceId().equals(existing.getNamespaceId());
        if (namespaceChange) {
            throw new IllegalArgumentException("Namespace can not be changed.");
        }

        // if there's name change, also need update name for all ldm views
        boolean nameChange = entity.getName() != null && !entity.getName().equalsIgnoreCase(existing.getName());
        if (nameChange) {
            // validate if name already exists in namespace
            baseEntityService.validateName(entity.getName(), entity.getNamespaceId());
            // update name for all ldm views
            List<LdmEntity> ldms = readService.getByEntityIdWithAssociations(entity.getId(), null);
            ldms.forEach(ldm -> {
                LdmEntity updatedLdm = EntityUtils.copyLdm(ldm, entityManager, entity.getUpdateBy());
                updatedLdm.setName(EntityUtils.getLdmName(entity.getName(), ldm.getViewType()));
                saveAsNewVersion(updatedLdm, null, false);
            });
        }
        return baseEntityService.update(entity);
    }
}
