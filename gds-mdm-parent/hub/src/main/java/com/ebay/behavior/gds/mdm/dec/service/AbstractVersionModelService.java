package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.dec.model.AbstractIndex;
import com.ebay.behavior.gds.mdm.dec.model.DecVersionedAuditable;
import com.ebay.behavior.gds.mdm.dec.util.DecAuditUtils;

import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
public abstract class AbstractVersionModelService<M extends DecVersionedAuditable, T extends AbstractIndex> {

    @Autowired
    private EntityManager entityManager;

    protected abstract JpaRepository<M, VersionedId> getRepository();

    protected abstract AbstractIndexService<T> getIndexService();

    protected abstract Class<M> getModelType();

    public abstract List<M> getAllCurrentVersion();

    public abstract M getByIdCurrentVersion(@NotNull Long id);

    public abstract void validateModelForUpdate(@Valid @NotNull M model);

    @Transactional(readOnly = true)
    public M getById(@Valid @NotNull VersionedId id) {
        return getRepository().findById(id).orElseThrow(() -> new DataNotFoundException(getModelType(), String.valueOf(id)));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public M update(@Valid @NotNull M model) {
        val existing = getByIdCurrentVersion(model.getId());
        validateVersionAndRevision(model, existing);
        entityManager.detach(existing);
        // create user and time should be the same as the initial version
        model.setCreateBy(existing.getCreateBy());
        model.setCreateDate(existing.getCreateDate());
        return getRepository().save(model);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public M saveAsNewVersion(@Valid @NotNull M model) {
        val existing = getByIdCurrentVersion(model.getId());
        hasChanges(model, existing); // check if there are changes
        // get new version
        int currentVersion = existing.getVersion();
        int newVersion = currentVersion + 1;
        // update version in index
        getIndexService().updateVersion(model.getId(), newVersion);
        // save new version
        model.setVersion(newVersion);
        // create user and time should be the same as the initial version
        model.setCreateBy(existing.getCreateBy());
        model.setCreateDate(existing.getCreateDate());
        return getRepository().save(model);
    }

    public void validateVersionAndRevision(@Valid @NotNull M model, @Valid @NotNull M existing) {
        validateModelForUpdate(model);
        if (model.getVersion() == null || model.getRevision() == null) {
            throw new IllegalArgumentException("Version and Revision must be provided for update.");
        }
        int currentVersion = existing.getVersion();
        if (model.getVersion() != currentVersion) {
            throw new IllegalArgumentException("Version %s does not match current version %s".formatted(model.getVersion(), currentVersion));
        }
    }

    public boolean hasChanges(M model, M existingModel) {
        if (!DecAuditUtils.getChanges(existingModel, model).hasChanges()) {
            log.info("No changes detected for Model with id: {}", model.getId());
            return false;
        }
        log.info(DecAuditUtils.getChanges(existingModel, model).changesSummary());
        return true;
    }
}
