package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.common.model.Auditable;
import com.ebay.behavior.gds.mdm.common.model.audit.AuditLogParams;
import com.ebay.behavior.gds.mdm.common.model.audit.AuditRecord;
import com.ebay.behavior.gds.mdm.common.model.audit.HistoryAuditable;
import com.ebay.behavior.gds.mdm.commonSvc.repository.HistoryRepository;
import com.ebay.behavior.gds.mdm.commonSvc.service.AbstractCrudService;
import com.ebay.behavior.gds.mdm.commonSvc.service.AuditService;
import com.ebay.behavior.gds.mdm.commonSvc.service.CrudService;
import com.ebay.behavior.gds.mdm.commonSvc.service.SearchService;
import com.ebay.behavior.gds.mdm.signal.util.AuditUtils;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.Validate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.audit.ChangeType.CREATED;
import static com.ebay.behavior.gds.mdm.common.model.audit.ChangeType.UPDATED;
import static com.ebay.behavior.gds.mdm.common.util.CommonValidationUtils.validateForCreate;
import static com.ebay.behavior.gds.mdm.common.util.CommonValidationUtils.validateForUpdate;
import static com.ebay.behavior.gds.mdm.signal.util.AuditUtils.saveAndAudit;

public abstract class AbstractCrudAndAuditService<M extends Auditable, H extends HistoryAuditable>
        extends AbstractCrudService<M>
        implements CrudService<M>, SearchService<M>, AuditService<H> {

    protected abstract HistoryRepository<H> getHistoryRepository();

    protected abstract Class<H> getHistoryModelType();

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public M create(@NotNull @Valid M model) {
        validateForCreate(model);
        try {
            return saveAndAudit(model, getRepository(), getHistoryRepository(), CREATED, null, getHistoryModelType());
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException(ex); // DataIntegrityViolation means FK constraint violation because of bad parentId inside the model
        }
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public M update(@NotNull @Valid M model) {
        validateForUpdate(model);
        getById(model.getId()); // Ensure signal exists before update
        return saveAndAudit(model, getRepository(), getHistoryRepository(), UPDATED, null, getHistoryModelType());
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void delete(long id) {
        AuditUtils.deleteAndAudit(getById(id), getRepository(), getHistoryRepository(), getHistoryModelType());
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Override
    public List<M> findAllById(@NotNull Set<Long> ids) {
        return getRepository().findAllById(ids);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditRecord<H>> getAuditLog(@Valid @NotNull AuditLogParams params) {
        Validate.isTrue(params.isNonVersioned(), "AuditLogParams must have no version to get an audit log");
        getById(params.getId());
        return AuditUtils.getAuditLog(getHistoryRepository(), params);
    }
}
