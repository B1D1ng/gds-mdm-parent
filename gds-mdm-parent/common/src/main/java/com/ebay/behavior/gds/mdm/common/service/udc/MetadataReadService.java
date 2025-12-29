package com.ebay.behavior.gds.mdm.common.service.udc;

import com.ebay.behavior.gds.mdm.common.model.EsPage;
import com.ebay.behavior.gds.mdm.common.model.EsPageable;
import com.ebay.behavior.gds.mdm.common.model.Metadata;
import com.ebay.behavior.gds.mdm.common.model.external.udc.Entity;
import com.ebay.behavior.gds.mdm.common.model.external.udc.EntityHistory;
import com.ebay.behavior.gds.mdm.common.model.external.udc.LineageResponse;
import com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.util.Set;

public interface MetadataReadService {

    // Versioned store API
    EntityHistory getHistoryById(@NotBlank String entityId);

    // GraphDB API
    Entity getEntityById(@NotBlank String entityId);

    Entity getEntityById(@NotNull UdcEntityType type, @PositiveOrZero long id);

    LineageResponse.LineageData getLineageById(@NotBlank String lineageId);

    LineageResponse.LineageData getLineageById(@NotEmpty Set<String> lineageId);

    <M extends Metadata> M getById(@NotNull UdcEntityType entityType, @PositiveOrZero long id, @NotNull Class<M> type);

    // ElasticSearch API

    /**
     * A term query searches for documents that have fields containing an exact term.
     * The term compared directly with the unprocessed field values.
     */
    <M extends Metadata> EsPage<M> termQuery(@NotNull Class<M> type, @NotNull @Valid EsPageable pageable,
                                             @NotNull Object term, @NotBlank String fieldName);

    /**
     * A terms query searches for documents that have fields containing any of the exact terms specified.
     * Terms compared directly with the unprocessed field values.
     */
    <M extends Metadata> EsPage<M> termsQuery(@NotNull Class<M> type, @NotNull @Valid EsPageable pageable,
                                              @NotEmpty Set<?> terms, @NotBlank String fieldName);

    /**
     * A wildcard query in Elasticsearch operates by matching documents that have fields containing terms that match a specified pattern.
     * The pattern can include wildcard characters such as * (matches zero or more characters) and ? (matches exactly one character).
     * The pattern compared directly with the unprocessed field values.
     */
    <M extends Metadata> EsPage<M> wildcardQuery(@NotNull Class<M> type, @NotNull @Valid EsPageable pageable,
                                                 @NotBlank String pattern, @NotBlank String fieldName);

    /**
     * A match query searches for documents that have fields containing terms that match a specified text.
     * The text is compared against the processed field values.
     */
    <M extends Metadata> EsPage<M> matchQuery(@NotNull Class<M> type, @NotNull @Valid EsPageable pageable,
                                              @NotBlank String text, @NotBlank String fieldName);

    /**
     * A match_all query matches all documents and does not analyze or process any input text.
     * It is typically used to retrieve all documents in an index.
     */
    <M extends Metadata> EsPage<M> matchAllQuery(@NotNull Class<M> type, @NotNull @Valid EsPageable pageable);

    /**
     * A multi_match query searches for documents that have fields containing terms that match a specified text across multiple fields.
     * The text is compared against the processed field values.
     */
    <M extends Metadata> EsPage<M> multiMatchQuery(@NotNull Class<M> type, @NotNull @Valid EsPageable pageable,
                                                   @NotBlank String text, @NotNull Operator operator, @NotEmpty String... fieldNames);

    /**
     * A prefix query searches for documents that have fields containing terms with a specified prefix.
     * The prefix is compared against the processed field values.
     */
    <M extends Metadata> EsPage<M> prefixQuery(@NotNull Class<M> type, @NotNull @Valid EsPageable pageable,
                                               @NotBlank String prefix, @NotBlank String fieldName);

    /**
     * An exists query checks for the presence of a field in a document.
     */
    <M extends Metadata> EsPage<M> existsQuery(@NotNull Class<M> type, @NotNull @Valid EsPageable pageable, @NotBlank String fieldName);

    /**
     * A range query searches for documents that have fields containing values within a specified range.
     * The range is compared against the processed field values.
     */
    <M extends Metadata> EsPage<M> rangeQuery(@NotNull Class<M> type, @NotNull @Valid EsPageable pageable,
                                              @NotBlank String fieldName, @NotNull Object from, @NotNull Object to);

    /**
     * Searches for documents using a specified json representation of ElasticSearch SearchSourceBuilder.
     * The json must be a valid representation of a SearchSourceBuilder.
     * That gives the flexibility to use any query type supported by ElasticSearch.
     */
    <M extends Metadata> EsPage<M> anyQuery(@NotNull Class<M> type, @NotBlank String searchBuilderJson);

    /**
     * Searches for documents using ElasticSearch SearchSourceBuilder.
     * That gives the flexibility to use any query type supported by ElasticSearch.
     */
    <M extends Metadata> EsPage<M> anyQuery(@NotNull Class<M> type, @NotNull SearchSourceBuilder searchBuilder);
}
