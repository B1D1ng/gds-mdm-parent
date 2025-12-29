package com.ebay.behavior.gds.mdm.common.service.udc;

import com.ebay.behavior.gds.mdm.common.config.UdcConfiguration;
import com.ebay.behavior.gds.mdm.common.exception.UdcException;
import com.ebay.behavior.gds.mdm.common.service.token.UdcTokenGenerator;
import com.ebay.datagov.pushingestion.EntityVersionData;
import com.ebay.datagov.pushingestion.PushIngestionClientPlatform;
import com.ebay.datagov.pushingestion.PushIngestionService;
import com.ebay.datagov.pushingestion.PushIngestionServiceFactory;
import com.ebay.datagov.pushingestion.PushIngestionStatus;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "udc", name = "enable", havingValue = "true")
public class UdcIngestionService {

    public static final PushIngestionClientPlatform PLATFORM = PushIngestionClientPlatform.TESS;

    private final UdcConfiguration config;
    private final UdcTokenGenerator tokenGenerator;
    private PushIngestionService ingestionService;

    @PostConstruct
    private void init() {
        val env = config.getEnv();
        val token = tokenGenerator.getToken();
        ingestionService = PushIngestionServiceFactory.createPushIngestionClient(PLATFORM, env, () -> token);
    }

    public String ingest(@NotNull EntityVersionData entity, long id) {
        return ingest(entity, String.valueOf(id));
    }

    public String ingest(@NotNull EntityVersionData entity, @NotBlank String id) {
        val response = ingestionService.ingest(entity);

        if (response.getStatus() == PushIngestionStatus.ACCEPTED) {
            if (entity.isDeleted()) {
                log.debug("Successfully deleted entity: {}, id: {}", entity.getEntityType(), id);
            } else {
                log.debug("Successfully ingested entity: {}, id: {}", entity.getEntityType(), id);
            }
            return response.getPk();
        }

        val errorMessage = String.format("Failed to ingested entity: %s, id: %s", entity.getEntityType(), id);
        log.error(errorMessage);
        throw new UdcException(response.getRequestId(), String.format("%s - %s", response.getErrorMsg(), errorMessage));
    }
}
