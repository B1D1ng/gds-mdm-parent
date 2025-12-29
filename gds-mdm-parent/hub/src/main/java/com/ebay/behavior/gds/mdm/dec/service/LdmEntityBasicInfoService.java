package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.dec.model.LdmBaseEntity;
import com.ebay.behavior.gds.mdm.dec.model.LdmEntity;
import com.ebay.behavior.gds.mdm.dec.model.Namespace;
import com.ebay.behavior.gds.mdm.dec.model.enums.NamespaceType;
import com.ebay.behavior.gds.mdm.dec.model.enums.ViewType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.HashMap;
import java.util.Map;

import static com.ebay.behavior.gds.mdm.dec.util.ApiConstants.LDM_DOMAIN;
import static com.ebay.behavior.gds.mdm.dec.util.ApiConstants.LDM_JIRA_PROJECT;
import static com.ebay.behavior.gds.mdm.dec.util.ApiConstants.LDM_NAME;
import static com.ebay.behavior.gds.mdm.dec.util.ApiConstants.LDM_NAMESPACE_ID;
import static com.ebay.behavior.gds.mdm.dec.util.ApiConstants.LDM_OWNERS;
import static com.ebay.behavior.gds.mdm.dec.util.ApiConstants.LDM_PK;
import static com.ebay.behavior.gds.mdm.dec.util.ApiConstants.LDM_TEAM;
import static com.ebay.behavior.gds.mdm.dec.util.ApiConstants.LDM_TEAM_DL;

/**
 * Service for managing basic information of LDM entities.
 */
@Slf4j
@Service
@Validated
public class LdmEntityBasicInfoService {

    @Autowired
    private LdmBaseEntityService baseEntityService;

    @Autowired
    private NamespaceService namespaceService;

    @Autowired
    private LdmEntityValidationService validationService;

    /**
     * Handles updates to basic information of an LDM entity.
     *
     * @param ldmEntity the LDM entity with updated information
     * @param baseEntityId the base entity ID
     * @param existingViewType the existing view type
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void handleBasicInfoUpdate(@Valid @NotNull LdmEntity ldmEntity, Long baseEntityId, ViewType existingViewType) {
        LdmBaseEntity baseEntity = baseEntityService.getById(baseEntityId);
        Namespace namespace = namespaceService.getById(baseEntity.getNamespaceId());
        if (namespace.getType() == NamespaceType.BASE && BooleanUtils.isNotTrue(ldmEntity.getIsDcs())) {
            return;
        }

        Map<String, Boolean> basicInfoChangeMap = new HashMap<>();
        putIfChanged(basicInfoChangeMap, LDM_NAME, ldmEntity.getName(), baseEntity.getName());
        putIfChanged(basicInfoChangeMap, LDM_NAMESPACE_ID, ldmEntity.getNamespaceId(), baseEntity.getNamespaceId());
        putIfChanged(basicInfoChangeMap, LDM_TEAM, ldmEntity.getTeam(), baseEntity.getTeam());
        putIfChanged(basicInfoChangeMap, LDM_TEAM_DL, ldmEntity.getTeamDl(), baseEntity.getTeamDl());
        putIfChanged(basicInfoChangeMap, LDM_OWNERS, ldmEntity.getOwners(), baseEntity.getOwners());
        putIfChanged(basicInfoChangeMap, LDM_DOMAIN, ldmEntity.getDomain(), baseEntity.getDomain());
        putIfChanged(basicInfoChangeMap, LDM_JIRA_PROJECT, ldmEntity.getJiraProject(), baseEntity.getJiraProject());
        putIfChanged(basicInfoChangeMap, LDM_PK, ldmEntity.getPk(), baseEntity.getPk());

        boolean viewTypeChanged = ldmEntity.getViewType() != null && ldmEntity.getViewType() != existingViewType;
        if (basicInfoChangeMap.values().stream().noneMatch(x -> x) && !viewTypeChanged) {
            return;
        }

        if (basicInfoChangeMap.get(LDM_NAME) || basicInfoChangeMap.get(LDM_NAMESPACE_ID) || viewTypeChanged) {
            validationService.validateName(ldmEntity.getName(), ldmEntity.getViewType(), ldmEntity.getNamespaceId());
        }

        updateBasicInfo(ldmEntity, baseEntity, basicInfoChangeMap);
    }

    /**
     * Updates the basic information of a base entity.
     *
     * @param ldmEntity the LDM entity with updated information
     * @param baseEntity the base entity to update
     * @param changeMap the map of changes
     */
    private void updateBasicInfo(LdmEntity ldmEntity, LdmBaseEntity baseEntity, Map<String, Boolean> changeMap) {
        Map<String, Runnable> fieldUpdaters = new HashMap<>();
        fieldUpdaters.put(LDM_NAME, () -> baseEntity.setName(ldmEntity.getName()));
        fieldUpdaters.put(LDM_NAMESPACE_ID, () -> baseEntity.setNamespaceId(ldmEntity.getNamespaceId()));
        fieldUpdaters.put(LDM_TEAM, () -> baseEntity.setTeam(ldmEntity.getTeam()));
        fieldUpdaters.put(LDM_TEAM_DL, () -> baseEntity.setTeamDl(ldmEntity.getTeamDl()));
        fieldUpdaters.put(LDM_OWNERS, () -> baseEntity.setOwners(ldmEntity.getOwners()));
        fieldUpdaters.put(LDM_DOMAIN, () -> baseEntity.setDomain(ldmEntity.getDomain()));
        fieldUpdaters.put(LDM_JIRA_PROJECT, () -> baseEntity.setJiraProject(ldmEntity.getJiraProject()));
        fieldUpdaters.put(LDM_PK, () -> baseEntity.setPk(ldmEntity.getPk()));

        boolean anyChanged = false;
        for (Map.Entry<String, Boolean> entry : changeMap.entrySet()) {
            if (Boolean.TRUE.equals(entry.getValue()) && fieldUpdaters.containsKey(entry.getKey())) {
                fieldUpdaters.get(entry.getKey()).run();
                anyChanged = true;
            }
        }
        if (anyChanged) {
            baseEntityService.update(baseEntity);
        }
    }

    /**
     * Checks if a value has changed and updates the change map accordingly.
     *
     * @param map the change map to update
     * @param key the key for the change map
     * @param newValue the new value
     * @param oldValue the old value
     * @param <T> the type of the value
     */
    private <T> void putIfChanged(Map<String, Boolean> map, String key, T newValue, T oldValue) {
        boolean changed;
        if (newValue == null) {
            changed = false;
        } else if (newValue instanceof String && oldValue instanceof String) {
            changed = !((String) newValue).equalsIgnoreCase((String) oldValue);
        } else {
            changed = !newValue.equals(oldValue);
        }
        map.put(key, changed);
    }
}
