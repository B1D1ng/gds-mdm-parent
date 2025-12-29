package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.dec.model.LdmEntity;
import com.ebay.behavior.gds.mdm.dec.model.enums.ViewType;
import com.ebay.behavior.gds.mdm.dec.repository.LdmEntityRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static com.ebay.behavior.gds.mdm.dec.util.ApiConstants.DELTA_CHANGE_STREAM_SUFFIX;

/**
 * Service for validating LDM entities.
 */
@Slf4j
@Service
@Validated
public class LdmEntityValidationService {

    @Autowired
    private LdmEntityRepository repository;

    /**
     * Validates the name of an LDM entity.
     *
     * @param name the name to validate
     * @param viewType the view type
     * @param namespaceId the namespace ID
     */
    @Transactional(readOnly = true)
    public void validateName(@NotBlank String name, @NotNull ViewType viewType, @NotNull Long namespaceId) {
        // to enhance in future versions: remove view type
        val sameNameEntities = repository.findAllByNameAndTypeAndNamespaceIdCurrentVersion(name, viewType, namespaceId);
        if (!sameNameEntities.isEmpty()) {
            throw new IllegalArgumentException("Entity with name " + name + " and type " + viewType + " already exists in namespace " + namespaceId);
        }
    }

    /**
     * Validates an LDM entity for update.
     *
     * @param model the LDM entity to validate
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void validateModelForUpdate(@Valid @NotNull LdmEntity model) {
        // This method is intentionally left empty as per the original implementation
        // It can be extended in the future if needed
    }

    /**
     * Validates the version and revision of an LDM entity.
     *
     * @param entity the entity to validate
     * @param existing the existing entity
     */
    @Transactional(readOnly = true)
    public void validateVersionAndRevision(@Valid @NotNull LdmEntity entity,@Valid @NotNull LdmEntity existing) {
        if (!entity.getVersion().equals(existing.getVersion())) {
            throw new IllegalArgumentException("Version mismatch. Expected: " + existing.getVersion() + ", Actual: " + entity.getVersion());
        }
        if (entity.getRevision() != null && existing.getRevision() != null && !entity.getRevision().equals(existing.getRevision())) {
            throw new IllegalArgumentException("Revision mismatch. Expected: " + existing.getRevision() + ", Actual: " + entity.getRevision());
        }
    }

    @Transactional(readOnly = true)
    public void validateDcsModel(@Valid @NotNull LdmEntity entity) {
        if (CollectionUtils.isEmpty(entity.getDcsFields())) {
            throw new IllegalArgumentException("DcsFields is null or empty");
        }
        // check if entity name is ended with _DeltaChangeStream
        if (!entity.getName().endsWith(DELTA_CHANGE_STREAM_SUFFIX)) {
            throw new IllegalArgumentException("Entity name should end with " + DELTA_CHANGE_STREAM_SUFFIX);
        }
        // check if upstreamLdm is null or empty
        List<String> upstreamLdmIds = entity.getUpstreamLdmIds();
        if (upstreamLdmIds.isEmpty()) {
            throw new IllegalArgumentException("UpstreamLdm is null or empty");
        }
        // check if upstream ldm is more than one
        if (upstreamLdmIds.size() > 1) {
            throw new IllegalArgumentException("UpstreamLdm should contain only one element");
        }
    }
}
