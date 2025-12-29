package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.dec.model.LdmBaseEntity;
import com.ebay.behavior.gds.mdm.dec.model.LdmEntity;
import com.ebay.behavior.gds.mdm.dec.model.LdmEntityIndex;
import com.ebay.behavior.gds.mdm.dec.model.Namespace;
import com.ebay.behavior.gds.mdm.dec.repository.LdmEntityIndexRepository;
import com.ebay.behavior.gds.mdm.dec.repository.NamespaceRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static com.ebay.behavior.gds.mdm.dec.util.ApiConstants.MIN_VERSION;

@Service
@Validated
public class LdmEntityIndexService extends AbstractIndexService<LdmEntityIndex> {

    @Getter(AccessLevel.PROTECTED)
    private final Class<LdmEntityIndex> modelType = LdmEntityIndex.class;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private LdmEntityIndexRepository repository;

    @Autowired
    private LdmBaseEntityService baseEntityService;

    @Autowired
    private NamespaceRepository namespaceRepository;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public LdmEntityIndex initialize(@Valid @NotNull LdmEntity ldmEntity) {
        var name = ldmEntity.getName();
        var namespaceId = ldmEntity.getNamespaceId();
        namespaceRepository.findById(namespaceId).orElseThrow(() -> new DataNotFoundException(Namespace.class, namespaceId));

        Long entityId = ldmEntity.getBaseEntityId();
        // if base entity is null, create one
        if (entityId == null) {
            val sameNameEntities = baseEntityService.getByNameAndNamespaceId(name, namespaceId);
            if (!sameNameEntities.isEmpty()) {
                throw new IllegalArgumentException("Entity with name " + name + " already exists in namespace " + namespaceId);
            }
            var entity = LdmBaseEntity.builder()
                    .name(name)
                    .namespaceId(namespaceId)
                    .description(ldmEntity.getDescription())
                    .pk(ldmEntity.getPk())
                    .owners(ldmEntity.getOwners())
                    .jiraProject(ldmEntity.getJiraProject())
                    .domain(ldmEntity.getDomain())
                    .team(ldmEntity.getTeam())
                    .teamDl(ldmEntity.getTeamDl())
                    .createBy(ldmEntity.getCreateBy())
                    .build();
            var saved = baseEntityService.create(entity);
            entityId = saved.getId();
        } else { // if base entity is not null, check if it exists
            baseEntityService.getById(entityId);
        }

        // create ldm view index
        LdmEntityIndex index =
                LdmEntityIndex.builder()
                        .viewType(ldmEntity.getViewType())
                        .baseEntityId(entityId)
                        .currentVersion(MIN_VERSION)
                        .build();
        return repository.save(index);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void delete(long id) {
        var entityIndex = getById(id);
        super.delete(id);
        // delete base entity if no other views are using it
        var views = repository.findByBaseEntityId(entityIndex.getBaseEntityId());
        if (views.isEmpty()) {
            baseEntityService.delete(entityIndex.getBaseEntityId());
        }
    }
}
