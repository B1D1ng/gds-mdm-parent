package com.ebay.behavior.gds.mdm.signal.common.service;

import com.ebay.behavior.gds.mdm.common.model.EsPage;
import com.ebay.behavior.gds.mdm.common.model.EsPageable;
import com.ebay.behavior.gds.mdm.common.model.UdcDataSourceType;
import com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType;
import com.ebay.behavior.gds.mdm.common.service.ElasticsearchService;
import com.ebay.behavior.gds.mdm.signal.common.model.StagedSignal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.val;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import static com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType.SIGNAL;

@Service
@Validated
public class StagedUdcSignalService
        extends AbstractStagedUdcService<StagedSignal>
        implements ElasticsearchService<StagedSignal> {

    @Getter
    private final UdcEntityType entityType = SIGNAL;

    @Getter
    private final Class<StagedSignal> type = StagedSignal.class;

    @Override
    public EsPage<StagedSignal> getAll(@NotNull UdcDataSourceType dataSource, @Valid @NotNull EsPageable pageable) {
        val page = super.getAll(dataSource, pageable);
        // Data quality check
        val cleansed = page.getContent().stream()
                .filter(signal -> signal.getId() != null)
                .filter(signal -> signal.getVersion() != null)
                .toList();

        val numFiltered = page.getContent().size() - cleansed.size();
        return new EsPage<>(pageable, cleansed, page.getTotalElements() - numFiltered);
    }
}
