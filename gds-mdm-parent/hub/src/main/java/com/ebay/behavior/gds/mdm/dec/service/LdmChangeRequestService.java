package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.commonSvc.service.AbstractCrudService;
import com.ebay.behavior.gds.mdm.dec.model.LdmBaseEntity;
import com.ebay.behavior.gds.mdm.dec.model.LdmChangeRequest;
import com.ebay.behavior.gds.mdm.dec.model.LdmEntity;
import com.ebay.behavior.gds.mdm.dec.model.Namespace;
import com.ebay.behavior.gds.mdm.dec.model.dto.LdmChangeRequestLogRecord;
import com.ebay.behavior.gds.mdm.dec.model.dto.LdmEntityRequest;
import com.ebay.behavior.gds.mdm.dec.model.enums.ActionTarget;
import com.ebay.behavior.gds.mdm.dec.model.enums.ActionType;
import com.ebay.behavior.gds.mdm.dec.model.enums.ChangeRequestStatus;
import com.ebay.behavior.gds.mdm.dec.repository.LdmChangeRequestRepository;
import com.ebay.behavior.gds.mdm.dec.repository.NamespaceRepository;
import com.ebay.behavior.gds.mdm.dec.util.DecAuditUtils;
import com.ebay.behavior.gds.mdm.dec.util.EntityUtils;
import com.ebay.behavior.gds.mdm.dec.util.TextUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.getRequestUser;
import static com.ebay.behavior.gds.mdm.common.util.TimeUtils.toNowSqlTimestamp;

@Service
@Validated
public class LdmChangeRequestService extends AbstractCrudService<LdmChangeRequest> {

    @Getter(AccessLevel.PROTECTED)
    private final Class<LdmChangeRequest> modelType = LdmChangeRequest.class;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private LdmChangeRequestRepository repository;

    @Autowired
    private LdmEntityService entityService;

    @Autowired
    private NamespaceRepository namespaceRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LdmBaseEntityService baseEntityService;

    @Autowired
    private LdmReadService readService;

    @Override
    @Transactional(readOnly = true)
    public Page<LdmChangeRequest> getAll(@Valid @NotNull Search search) {
        throw new NotImplementedException("Not implemented by design");
    }

    @Transactional(readOnly = true)
    public List<LdmChangeRequest> getAll() {
        return repository.findAll();
    }

    @Override
    public LdmChangeRequest getByIdWithAssociations(long id) {
        throw new NotImplementedException("Not implemented by design");
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public LdmChangeRequest update(@Valid @NotNull LdmChangeRequest request) {
        LdmChangeRequest existing = getById(request.getId());
        request.setStatus(existing.getStatus());
        return repository.save(request);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public LdmChangeRequest approve(@NotNull Long id) {
        LdmChangeRequest request = getById(id);
        if (request.getStatus() != ChangeRequestStatus.DRAFT && request.getStatus() != ChangeRequestStatus.PENDING_APPROVAL) {
            throw new IllegalArgumentException(String.format("This request has been processed with status %s.", request.getStatus()));
        }
        ActionTarget actionTarget = request.getActionTarget();
        return switch (actionTarget) {
            case LDM_VIEW -> approveLdmView(request);
            case NAMESPACE -> approveNamespace(request);
        };
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public LdmChangeRequest approveLdmView(@Valid @NotNull LdmChangeRequest request) {
        LdmEntity changedData = TextUtils.readJson(objectMapper, request.getRequestDetails(), LdmEntityRequest.class).toLdmEntity();
        if (changedData == null) {
            throw new IllegalArgumentException("Invalid change request");
        }

        ActionType actionType = request.getActionType();
        if (Objects.requireNonNull(actionType) == ActionType.CREATE) {
            Long baseEntityId = changedData.getBaseEntityId();
            LdmBaseEntity baseEntity = baseEntityService.getById(baseEntityId);
            changedData.setRequestId(request.getId());
            changedData.setName(EntityUtils.getLdmName(baseEntity.getName(), changedData.getViewType()));
            changedData.setNamespaceId(baseEntity.getNamespaceId());
            entityService.create(changedData);
            return approveRequest(request);
        } else if (Objects.requireNonNull(actionType) == ActionType.UPDATE) {
            // apply change to current ldm version
            LdmEntity entity = readService.getByIdWithAssociationsCurrentVersion(changedData.getId());
            entityManager.detach(entity);
            BeanUtils.copyProperties(entity, changedData, DecAuditUtils.getIgnoredProperties(changedData, Set.of("updateDate")));
            changedData.setName(entity.getName());
            changedData.setViewType(entity.getViewType());
            changedData.setNamespaceId(entity.getNamespaceId());
            changedData.setBaseEntityId(entity.getBaseEntityId());
            // save new version
            entityService.saveAsNewVersion(changedData, request.getId(), false);
            return approveRequest(request);
        } else {
            throw new UnsupportedOperationException("ActionType Not Supported");
        }
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public LdmChangeRequest approveNamespace(@Valid @NotNull LdmChangeRequest request) {
        Namespace changedData = TextUtils.readJson(objectMapper, request.getRequestDetails(), Namespace.class);
        if (changedData == null) {
            throw new IllegalArgumentException("Invalid change request");
        }

        ActionType actionType = request.getActionType();
        if (actionType == ActionType.CREATE || actionType == ActionType.UPDATE) {
            namespaceRepository.save(changedData);
            return approveRequest(request);
        } else {
            throw new UnsupportedOperationException("ActionType Not Supported");
        }
    }

    private LdmChangeRequest approveRequest(LdmChangeRequest request) {
        // update request status
        request.setStatus(ChangeRequestStatus.APPROVED);
        updateLogRecordsAfterApprove(request);
        return repository.save(request);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public LdmChangeRequest reject(@NotNull Long id, @Valid @NotNull LdmChangeRequestLogRecord approveLog) {
        LdmChangeRequest request = getById(id);
        if (request.getStatus() != ChangeRequestStatus.DRAFT && request.getStatus() != ChangeRequestStatus.PENDING_APPROVAL) {
            throw new IllegalArgumentException(String.format("This request has been processed with status %s.", request.getStatus()));
        }
        request.setStatus(ChangeRequestStatus.REJECTED);
        updateLogRecord(request, approveLog);
        return repository.save(request);
    }

    public void updateLogRecordsAfterApprove(LdmChangeRequest request) {
        LdmChangeRequestLogRecord logRecord = LdmChangeRequestLogRecord.builder()
                .userName(getRequestUser())
                .createdTime(toNowSqlTimestamp())
                .status(ChangeRequestStatus.APPROVED)
                .build();
        updateLogRecord(request, logRecord);
    }

    public void updateLogRecord(LdmChangeRequest request, LdmChangeRequestLogRecord logRecord) {
        List<LdmChangeRequestLogRecord> approveLogList =
                TextUtils.readJson(objectMapper, request.getLogRecords(), new TypeReference<List<LdmChangeRequestLogRecord>>() {
                });
        if (approveLogList == null) {
            approveLogList = new ArrayList<>();
        }
        approveLogList.add(logRecord);
        request.setLogRecords(TextUtils.writeJson(objectMapper, approveLogList));
    }

    @Override
    public LdmChangeRequest create(@Valid @NotNull LdmChangeRequest request) {
        if (request.getActionTarget() == ActionTarget.LDM_VIEW) {
            String requestDetails = request.getRequestDetails();
            LdmEntity entity = TextUtils.readJson(objectMapper, requestDetails, LdmEntityRequest.class).toLdmEntity();
            if (request.getActionType() == ActionType.CREATE) {
                // validate if baseEntityId + viewType exists
                if (entity.getBaseEntityId() == null || entity.getViewType() == null) {
                    throw new IllegalArgumentException("BaseEntityId and ViewType are required");
                }
                // validate base entity existence
                baseEntityService.getById(entity.getBaseEntityId());
            } else if (request.getActionType() == ActionType.UPDATE) { // validate for existence for update operation
                LdmEntity existed = entityService.getByIdCurrentVersion(entity.getId());
                if (entity.getBaseEntityId() != null && !Objects.equals(existed.getBaseEntityId(), entity.getBaseEntityId())) {
                    throw new IllegalArgumentException("BaseEntityId is not allowed to change");
                }
                if (entity.getViewType() != null && existed.getViewType() != entity.getViewType()) {
                    throw new IllegalArgumentException("ViewType is not allowed to change");
                }
            }
            return super.create(request);
        }
        return super.create(request);
    }
}
