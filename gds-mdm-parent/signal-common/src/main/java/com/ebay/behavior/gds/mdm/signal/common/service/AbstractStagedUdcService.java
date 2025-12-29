package com.ebay.behavior.gds.mdm.signal.common.service;

import com.ebay.behavior.gds.mdm.common.model.EsPage;
import com.ebay.behavior.gds.mdm.common.model.EsPageable;
import com.ebay.behavior.gds.mdm.common.model.Metadata;
import com.ebay.behavior.gds.mdm.common.model.UdcDataSourceType;
import com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType;
import com.ebay.behavior.gds.mdm.common.service.ElasticsearchService;
import com.ebay.behavior.gds.mdm.common.service.udc.MetadataReadService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;

@Validated
public abstract class AbstractStagedUdcService<M extends Metadata> implements ElasticsearchService<M> {

    @Autowired
    protected MetadataReadService readService;

    protected abstract Class<M> getType();

    protected abstract UdcEntityType getEntityType();

    @Override
    public M getById(@PositiveOrZero long id) {
        return readService.getById(getEntityType(), id, getType());
    }

    @Override
    public EsPage<M> getAll(@NotNull UdcDataSourceType dataSource, @Valid @NotNull EsPageable pageable) {
        return readService.matchQuery(getType(), pageable, dataSource.getValue(), "sources");
    }

    /**
     * Search for StagedSignals using the provided searchBuilderJson.
     * This way of searching is most flexible, since it covers all possible elasticsearch search scenarios.
     *
     * @param searchBuilderJson A json string representing the search query.
     * @return A page of StagedSignals that match the search query.
     */
    @Override
    public EsPage<M> search(@NotBlank String searchBuilderJson) {
        return readService.anyQuery(getType(), searchBuilderJson);
    }

    /**
     * Search for StagedSignals using the provided searchBuilderJson.
     * This way of searching is most flexible, since it covers all possible elasticsearch search scenarios.
     *
     * @param searchBuilder An ES search query builder.
     * @return A page of StagedSignals that match the search query.
     */
    @Override
    public EsPage<M> search(@NotNull SearchSourceBuilder searchBuilder) {
        return readService.anyQuery(getType(), searchBuilder);
    }
}