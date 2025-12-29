package com.ebay.behavior.gds.mdm.udf.service.udc;

import com.ebay.behavior.gds.mdm.common.config.UdcConfiguration;
import com.ebay.behavior.gds.mdm.common.model.Metadata;
import com.ebay.behavior.gds.mdm.common.model.UdcDataSourceType;
import com.ebay.behavior.gds.mdm.common.service.token.UdcTokenGenerator;
import com.ebay.behavior.gds.mdm.common.service.udc.MetadataReadService;
import com.ebay.behavior.gds.mdm.common.service.udc.UdcEntityConverter;
import com.ebay.behavior.gds.mdm.common.service.udc.UdcIngestionService;
import com.ebay.behavior.gds.mdm.udf.common.model.UdcUdf;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Map;

@Slf4j
@Service
@Validated
public class UdfUdcWriteServiceImpl implements UdfMetadataWriteService {
    @Autowired
    private UdcConfiguration config;

    @Getter
    @Autowired
    private UdcTokenGenerator tokenGenerator;
    @Autowired
    private MetadataReadService readService;

    @Autowired
    private UdcEntityConverter entityConverter;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UdcIngestionService ingestService;

    private UdcDataSourceType dataSource;

    @PostConstruct
    private void init() {
        dataSource = config.getDataSource();
    }

    @Override
    public String upsertUdf(@Valid @NotNull UdcUdf udf) {
        return upsertMetadata(udf);
    }

    @Override
    public String deleteUdf(@NotBlank String id) {
        return deleteMetadata(
                UdcUdf.builder().udfId(Long.valueOf(id)).build());
    }

    private String upsertMetadata(Metadata metadata) {
        val entity = entityConverter.toEntity(metadata, dataSource);
        deleteUdf(metadata.getId().toString());
        return ingestService.ingest(entity, metadata.getId());
    }

    private String deleteMetadata(Metadata metadata) {
        Map<String, Object> properties = Map.of(metadata.getEntityType().getIdName(), metadata.getId());
        val entity = entityConverter.toEntity(metadata.getEntityType(), dataSource, properties, Map.of());
        entity.setDeleted(true);
        return ingestService.ingest(entity, metadata.getId());
    }
}
