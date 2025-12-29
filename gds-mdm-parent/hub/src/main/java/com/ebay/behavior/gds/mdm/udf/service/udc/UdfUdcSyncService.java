package com.ebay.behavior.gds.mdm.udf.service.udc;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.exception.UdcException;
import com.ebay.behavior.gds.mdm.common.model.IdWithStatus;
import com.ebay.behavior.gds.mdm.udf.common.model.Udf;
import com.ebay.behavior.gds.mdm.udf.service.UdfService;
import com.ebay.behavior.gds.mdm.udf.util.UdfMetadataUtils;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Service
@Validated
public class UdfUdcSyncService {
    @Autowired
    private UdfMetadataWriteService metadataWriteService;

    @Autowired
    private UdfService udfService;

    public IdWithStatus udcSyncUdf(@PositiveOrZero Long udfId) {
        try {
            Udf udf = udfService.getById(udfId, true);
            String udcEntityId = metadataWriteService.upsertUdf(UdfMetadataUtils.convert(udf));
            return IdWithStatus.okStatus(udfId, udcEntityId);
        } catch (DataNotFoundException dex) {
            val error = dex.getMessage();
            log.error(String.format("Failed to find UDF (id: %d). Error: %s", udfId, error));
            return IdWithStatus.failedStatus(udfId, error);
        } catch (UdcException uex) {
            val error = uex.getMessage();
            log.error(String.format("Failed to inject UDF (id: %d). Error: %s", udfId, error));
            return IdWithStatus.failedStatus(udfId, error);
        }
    }
}
