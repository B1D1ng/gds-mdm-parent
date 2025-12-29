package com.ebay.behavior.gds.mdm.common.service.udc;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.exception.ExternalCallException;
import com.ebay.behavior.gds.mdm.common.model.EsPage;
import com.ebay.behavior.gds.mdm.common.model.EsPageable;
import com.ebay.behavior.gds.mdm.common.model.Metadata;
import com.ebay.behavior.gds.mdm.common.model.QueryParam;
import com.ebay.behavior.gds.mdm.common.model.external.udc.Entity;
import com.ebay.behavior.gds.mdm.common.model.external.udc.EntityHistory;
import com.ebay.behavior.gds.mdm.common.model.external.udc.LineageResponse;
import com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType;
import com.ebay.behavior.gds.mdm.common.service.AbstractRestPostClient;
import com.ebay.behavior.gds.mdm.common.service.token.UdcTokenGenerator;
import com.ebay.behavior.gds.mdm.common.util.ElasticsearchParser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Named;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.ws.rs.client.WebTarget;
import lombok.Getter;
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.Model.COMMA;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.AUTOWIRING_INSPECTION;

@Service
@Validated
public class UdcReadServiceImpl extends AbstractRestPostClient implements MetadataReadService {

    public static final String UDC_METADATA_CLIENT_NAME = "udc.metadata";

    public static final String GRAPH_PK_QUERY_PARAM = "graphPK";
    public static final String LINEAGE_QUERY_PARAM = "queryLineage";
    public static final String IDS_QUERY_PARAM = "ids";
    public static final String JSON_RESPONSE_QUERY_PARAM = "jsonResp";

    public static final String ES_PATH = "searchES";
    public static final String GRAPH_DB_PATH = "details";
    public static final String LINEAGE_PATH = "bulk/entity";
    public static final String HISTORY_PATH = "history";

    public static final String TRUE = "true";

    @Getter
    @Autowired
    @Named(UDC_METADATA_CLIENT_NAME)
    @SuppressWarnings(AUTOWIRING_INSPECTION)
    private WebTarget target;

    @Getter
    @Autowired
    private UdcTokenGenerator tokenGenerator;

    @Autowired
    private UdcQueryBuilder queryBuilder;

    @Autowired
    private ObjectMapper objectMapper;

    @Getter
    private final String path = null; // a path under webTarget

    @Override
    public @Valid EntityHistory getHistoryById(@NotBlank String entityId) {
        val queryParams = createQueryParams(GRAPH_PK_QUERY_PARAM, entityId);
        return get(HISTORY_PATH, queryParams, EntityHistory.class);
    }

    @Override
    public @Valid Entity getEntityById(@NotBlank String entityId) {
        val queryParams = createQueryParams(GRAPH_PK_QUERY_PARAM, entityId);
        return get(GRAPH_DB_PATH, queryParams, Entity.class);
    }

    @Override
    public @Valid Entity getEntityById(@NotNull UdcEntityType entityType, @PositiveOrZero long id) {
        val entityId = Metadata.toEntityId(entityType, String.valueOf(id));
        return getEntityById(entityId);
    }

    @Override
    public LineageResponse.LineageData getLineageById(@NotBlank String lineageId) {
        return getLineageEntities(lineageId);
    }

    @Override
    public LineageResponse.LineageData getLineageById(@NotEmpty Set<String> lineageIds) {
        val csvLineageIds = String.join(COMMA, lineageIds);
        return getLineageEntities(csvLineageIds);
    }

    @Override
    public <M extends Metadata> M getById(@NotNull UdcEntityType entityType, @PositiveOrZero long id, @NotNull Class<M> type) {
        val pageable = EsPageable.of(0, 2); // size = 2 needed to identify issues with the data, since we expect only one result
        val page = termQuery(type, pageable, String.valueOf(id), entityType.getIdName());

        if (page.getTotalElements() == 0) {
            throw new DataNotFoundException(type, id);
        }

        val errorMessage = String.format("More than one %s found for id: %s", type.getSimpleName(), id);
        Validate.isTrue(page.getTotalElements() == 1, errorMessage);
        Validate.isTrue(page.getContent().size() == 1, errorMessage);

        return page.getContent().get(0);
    }

    @Override
    public <M extends Metadata> EsPage<M> termQuery(@NotNull Class<M> type, @NotNull @Valid EsPageable pageable,
                                                    @NotNull Object term, @NotBlank String fieldName) {
        val udcJsonQuery = queryBuilder.termQuery(pageable, term, fieldName);
        return query(type, pageable, udcJsonQuery);
    }

    @Override
    public <M extends Metadata> EsPage<M> termsQuery(@NotNull Class<M> type, @NotNull @Valid EsPageable pageable,
                                                     @NotEmpty Set<?> terms, @NotBlank String fieldName) {
        val udcJsonQuery = queryBuilder.termsQuery(pageable, terms, fieldName);
        return query(type, pageable, udcJsonQuery);
    }

    @Override
    public <M extends Metadata> EsPage<M> wildcardQuery(Class<M> type, EsPageable pageable, String pattern, String fieldName) {
        val udcJsonQuery = queryBuilder.wildcardQuery(pageable, pattern, fieldName);
        return query(type, pageable, udcJsonQuery);
    }

    @Override
    public <M extends Metadata> EsPage<M> matchQuery(@NotNull Class<M> type, @NotNull @Valid EsPageable pageable,
                                                     @NotBlank String text, @NotBlank String fieldName) {
        val udcJsonQuery = queryBuilder.matchQuery(pageable, text, fieldName);
        return query(type, pageable, udcJsonQuery);
    }

    @Override
    public <M extends Metadata> EsPage<M> matchAllQuery(@NotNull Class<M> type, @NotNull @Valid EsPageable pageable) {
        val udcJsonQuery = queryBuilder.matchAllQuery(pageable);
        return query(type, pageable, udcJsonQuery);
    }

    @Override
    public <M extends Metadata> EsPage<M> multiMatchQuery(@NotNull Class<M> type, @NotNull @Valid EsPageable pageable,
                                                          @NotBlank String text, @NotNull Operator operator, @NotEmpty String... fieldNames) {
        val udcJsonQuery = queryBuilder.multiMatchQuery(pageable, text, operator, fieldNames);
        return query(type, pageable, udcJsonQuery);
    }

    @Override
    public <M extends Metadata> EsPage<M> prefixQuery(@NotNull Class<M> type, @NotNull @Valid EsPageable pageable,
                                                      @NotBlank String prefix, @NotBlank String fieldName) {
        val udcJsonQuery = queryBuilder.prefixQuery(pageable, prefix, fieldName);
        return query(type, pageable, udcJsonQuery);
    }

    @Override
    public <M extends Metadata> EsPage<M> existsQuery(@NotNull Class<M> type, @NotNull @Valid EsPageable pageable, @NotBlank String fieldName) {
        val udcJsonQuery = queryBuilder.existsQuery(pageable, fieldName);
        return query(type, pageable, udcJsonQuery);
    }

    @Override
    public <M extends Metadata> EsPage<M> rangeQuery(@NotNull Class<M> type, @NotNull @Valid EsPageable pageable,
                                                     @NotBlank String fieldName, @NotNull Object from, @NotNull Object to) {
        val udcJsonQuery = queryBuilder.rangeQuery(pageable, fieldName, from, to);
        return query(type, pageable, udcJsonQuery);
    }

    @Override
    public <M extends Metadata> EsPage<M> anyQuery(@NotNull Class<M> type, @NotBlank String searchBuilderJson) {
        val searchBuilder = ElasticsearchParser.toSearchSourceBuilder(searchBuilderJson);
        return anyQuery(type, searchBuilder);
    }

    @Override
    public <M extends Metadata> EsPage<M> anyQuery(@NotNull Class<M> type, @NotNull SearchSourceBuilder searchBuilder) {
        val pageable = EsPageable.of(searchBuilder.from(), searchBuilder.size(), Sort.unsorted());
        val udcJsonQuery = queryBuilder.anyQuery(searchBuilder);
        return query(type, pageable, udcJsonQuery);
    }

    private <M extends Metadata> EsPage<M> query(Class<M> type, EsPageable pageable, String udcJsonQuery) {
        val entityType = UdcEntityType.fromType(type);
        val responseJson = runEsQuery(entityType, udcJsonQuery);
        val dataJson = extractDataElement(responseJson);
        val searchResponse = ElasticsearchParser.toSearchResponse(dataJson);
        return toPage(pageable, searchResponse, type);
    }

    private String runEsQuery(UdcEntityType type, String udcJsonQuery) {
        val queryParams = createQueryParams(JSON_RESPONSE_QUERY_PARAM, TRUE);
        val path = String.format("%s/%s", ES_PATH, type.getValue());
        return post(path, queryParams, udcJsonQuery, String.class);
    }

    private LineageResponse.LineageData getLineageEntities(String lineageIds) {
        val queryParams = List.of(
                new QueryParam(LINEAGE_QUERY_PARAM, TRUE),
                new QueryParam(IDS_QUERY_PARAM, lineageIds),
                new QueryParam(JSON_RESPONSE_QUERY_PARAM, TRUE));

        var response = get(LINEAGE_PATH, queryParams, LineageResponse.class);
        val lineageData = response.getData();
        val vertices = lineageData.getVertices();

        if (Objects.isNull(vertices) || vertices.isEmpty()) {
            throw new DataNotFoundException(LineageResponse.class, lineageIds);
        }

        return lineageData;
    }

    private <M extends Metadata> EsPage<M> toPage(EsPageable pageable, SearchResponse response, Class<M> type) {
        pageable.nullifySort(); // Nullified since not needed, but causes Jackson deserialization to fail
        val hits = response.getHits();
        val entitiesArray = hits.getHits();
        List<SearchHit> entities = entitiesArray == null ? List.of() : Arrays.asList(entitiesArray);
        long total = Optional.ofNullable(hits.getTotalHits()).map(totalHist -> totalHist.value).orElse(0L);

        if (CollectionUtils.isEmpty(entities)) {
            return new EsPage<>(pageable, List.of(), total);
        }

        val models = entities.stream()
                .map(SearchHit::getSourceAsMap)
                .map(map -> objectMapper.convertValue(map, type))
                .toList();

        return new EsPage<>(pageable, models, total);
    }

    private String extractDataElement(String json) {
        try {
            val rootNode = objectMapper.readTree(json);
            val dataNode = rootNode.path(UdcQueryBuilder.DATA);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(dataNode);
        } catch (JsonProcessingException ex) {
            throw new ExternalCallException(ex);
        }
    }
}
