package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.commonSvc.service.AbstractCrudService;
import com.ebay.behavior.gds.mdm.dec.model.LdmBaseEntity;
import com.ebay.behavior.gds.mdm.dec.repository.LdmBaseEntityRepository;
import com.ebay.behavior.gds.mdm.dec.repository.PhysicalAssetLdmMappingRepository;
import com.ebay.behavior.gds.mdm.dec.util.EntityUtils;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.val;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Service
@Validated
public class LdmBaseEntityService extends AbstractCrudService<LdmBaseEntity> {

    @Getter(AccessLevel.PROTECTED)
    private final Class<LdmBaseEntity> modelType = LdmBaseEntity.class;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private LdmBaseEntityRepository repository;

    @Autowired
    private PhysicalAssetLdmMappingRepository mappingRepository;

    @Autowired
    private LdmReadService ldmReadService;

    @Transactional(readOnly = true)
    public void validateName(@NotBlank String name, @NotNull Long namespaceId) {
        val sameNameEntities = repository.findByNameAndNamespaceId(name, namespaceId);
        if (!sameNameEntities.isEmpty()) {
            throw new IllegalArgumentException("Entity with name " + name + " already exists in namespace " + namespaceId);
        }
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public LdmBaseEntity create(@Valid @NotNull LdmBaseEntity entity) {
        // validate if name already exists
        validateName(entity.getName(), entity.getNamespaceId());
        return repository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LdmBaseEntity> getAll(@Valid @NotNull Search search) {
        throw new NotImplementedException("Not implemented by design");
    }

    @Transactional(readOnly = true)
    public List<LdmBaseEntity> getAll() {
        return getAll(false);
    }

    @Transactional(readOnly = true)
    public List<LdmBaseEntity> getAll(Boolean excludeTextFields) {
        var entities = repository.findAll();
        entities.forEach(entity -> entity.setViews(ldmReadService.getByEntityId(entity.getId())));
        if (Boolean.TRUE.equals(excludeTextFields)) {
            for (val baseEntity : entities) {
                for (val ldm : baseEntity.getViews()) {
                    EntityUtils.excludeTextFields(ldm);
                }
            }
        }
        return entities;
    }

    @Override
    @Transactional(readOnly = true)
    public LdmBaseEntity getByIdWithAssociations(long id) {
        return getByIdWithAssociations(id, null);
    }

    @Transactional(readOnly = true)
    public LdmBaseEntity getByIdWithAssociations(long id, String env) {
        val entity = repository.findById(id).orElseThrow(() -> new DataNotFoundException(LdmBaseEntity.class, id));
        val views = ldmReadService.getByEntityIdWithAssociations(id, env);
        for (val view : views) {
            EntityUtils.copyBasicInfoFromBaseEntity(view, entity);
        }
        entity.setViews(views);
        return entity;
    }

    @Transactional(readOnly = true)
    public List<LdmBaseEntity> getByNameAndNamespaceId(@NotNull String name, @NotNull Long namespaceId) {
        return repository.findByNameAndNamespaceId(name, namespaceId);
    }

    @Transactional(readOnly = true)
    public List<LdmBaseEntity> searchByNameAndNamespace(String name, String namespaceName) {
        if (StringUtils.isNotBlank(name) && StringUtils.isNotBlank(namespaceName)) {
            return repository.findByNameAndNamespaceName(name, namespaceName);
        } else if (StringUtils.isNotBlank(name)) {
            return repository.findByName(name);
        }
        return repository.findByNamespaceName(namespaceName);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void delete(long id) {
        // delete all ldm mappings associated with the asset
        val mappings = mappingRepository.findByLdmBaseEntityId(id);
        if (!mappings.isEmpty()) {
            mappingRepository.deleteAll(mappings);
        }
        super.delete(id);
    }
}
