package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.dec.model.dto.LdmBootstrapRequest;
import com.ebay.behavior.gds.mdm.dec.model.dto.LdmBootstrapResponse;
import com.ebay.behavior.gds.mdm.dec.model.enums.ViewType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.io.Serializable;
import java.util.Map;

@Slf4j
@Service
@Validated
public class LdmBaseEntityActionService {

    private final LdmBaseEntityService baseEntityService;

    private final LdmEntityService ldmEntityService;

    private final DecCompilerClient decCompilerClient;

    public LdmBaseEntityActionService(LdmBaseEntityService baseEntityService, LdmEntityService ldmEntityService, DecCompilerClient decCompilerClient) {
        this.baseEntityService = baseEntityService;
        this.ldmEntityService = ldmEntityService;
        this.decCompilerClient = decCompilerClient;
    }

    @Transactional(readOnly = true)
    public LdmBootstrapResponse bootstrap(@NotNull Long id, @Valid @NotNull LdmBootstrapRequest request) {
        baseEntityService.getById(id);

        val fromView = ldmEntityService.getByIdCurrentVersion(request.fromViewId());
        if (!fromView.getBaseEntityId().equals(id)) {
            throw new IllegalArgumentException("FromViewId doesn't belong to BaseEntityId.");
        }
        if (!fromView.getViewType().equals(ViewType.RAW)) {
            throw new IllegalArgumentException("FromViewType doesn't match RAW.");
        }

        val toView = ldmEntityService.getByIdCurrentVersion(request.toViewId());
        if (!toView.getBaseEntityId().equals(id)) {
            throw new IllegalArgumentException("ToViewId doesn't belong to BaseEntityId.");
        }
        if (!toView.getViewType().equals(ViewType.SNAPSHOT)) {
            throw new IllegalArgumentException("ToViewType doesn't match SNAPSHOT.");
        }

        val requestMap = getLdmBootstrapMap(id, fromView.getName(), toView.getName());

        return decCompilerClient.post("/bootstrap", null, requestMap, LdmBootstrapResponse.class);
    }

    public static Map<String, ? extends Serializable> getLdmBootstrapMap(Long id, String fromView, String toView) {
        return Map.of("ldm_entity_id", id, "from_view", fromView, "to_view", toView);
    }
}
