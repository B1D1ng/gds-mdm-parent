package com.ebay.behavior.gds.mdm.common.service;

import com.ebay.behavior.gds.mdm.common.model.EsPage;
import com.ebay.behavior.gds.mdm.common.model.EsPageable;
import com.ebay.behavior.gds.mdm.common.model.Metadata;
import com.ebay.behavior.gds.mdm.common.model.UdcDataSourceType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.elasticsearch.search.builder.SearchSourceBuilder;

public interface ElasticsearchService<M extends Metadata> {

    M getById(@PositiveOrZero long id);

    EsPage<M> getAll(@NotNull UdcDataSourceType dataSource, @Valid @NotNull EsPageable pageable);

    /**
     * Search for StagedSignals using the provided searchBuilderJson.
     * This way of searching is most flexible, since it covers all possible elasticsearch search scenarios.
     *
     * @param searchBuilderJson A json string representing the search query.
     * @return A page of StagedSignals that match the search query.
     */
    EsPage<M> search(@NotBlank String searchBuilderJson);

    /**
     * Search for StagedSignals using the provided searchBuilderJson.
     * This way of searching is most flexible, since it covers all possible elasticsearch search scenarios.
     *
     * @param searchBuilder An ES search query builder.
     * @return A page of StagedSignals that match the search query.
     */
    EsPage<M> search(@NotNull SearchSourceBuilder searchBuilder);
}
