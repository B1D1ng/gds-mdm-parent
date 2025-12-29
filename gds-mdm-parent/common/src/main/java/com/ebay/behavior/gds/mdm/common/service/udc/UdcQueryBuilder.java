package com.ebay.behavior.gds.mdm.common.service.udc;

import com.ebay.behavior.gds.mdm.common.model.EsPageable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.val;
import org.apache.commons.lang3.Validate;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;

import static org.elasticsearch.search.sort.SortOrder.ASC;
import static org.elasticsearch.search.sort.SortOrder.DESC;

@Component
@Validated
public class UdcQueryBuilder {

    public static final String API_VERSION = "apiVersion";
    public static final String KIND = "kind";
    public static final String DATA = "data";
    public static final String SEARCH_KIND = "search";

    @Autowired
    private ObjectMapper mapper;

    public String termQuery(@NotNull @Valid EsPageable pageable, @NotNull Object term, @NotBlank String fieldName) {
        val queryBuilder = QueryBuilders.termQuery(fieldName, term);
        return buildQuery(pageable, queryBuilder);
    }

    public String termsQuery(@NotNull @Valid EsPageable pageable, @NotEmpty Set<?> terms, @NotBlank String fieldName) {
        val queryBuilder = QueryBuilders.termsQuery(fieldName, terms);
        return buildQuery(pageable, queryBuilder);
    }

    public String wildcardQuery(@NotNull @Valid EsPageable pageable, @NotNull String pattern, @NotBlank String fieldName) {
        val queryBuilder = QueryBuilders.wildcardQuery(fieldName, pattern);
        return buildQuery(pageable, queryBuilder);
    }

    public String matchQuery(@NotNull @Valid EsPageable pageable, @NotBlank String text, @NotBlank String fieldName) {
        val queryBuilder = QueryBuilders.matchQuery(fieldName, text);
        return buildQuery(pageable, queryBuilder);
    }

    public String matchAllQuery(@NotNull @Valid EsPageable pageable) {
        val queryBuilder = QueryBuilders.matchAllQuery();
        return buildQuery(pageable, queryBuilder);
    }

    public String multiMatchQuery(@NotNull @Valid EsPageable pageable, @NotBlank String text, @NotNull Operator operator, @NotEmpty String... fieldNames) {
        val queryBuilder = QueryBuilders.multiMatchQuery(text, fieldNames).operator(operator);
        return buildQuery(pageable, queryBuilder);
    }

    public String prefixQuery(@NotNull @Valid EsPageable pageable, @NotBlank String prefix, @NotBlank String fieldName) {
        val queryBuilder = QueryBuilders.prefixQuery(fieldName, prefix);
        return buildQuery(pageable, queryBuilder);
    }

    public String existsQuery(@NotNull @Valid EsPageable pageable, @NotBlank String fieldName) {
        val queryBuilder = QueryBuilders.existsQuery(fieldName);
        return buildQuery(pageable, queryBuilder);
    }

    public String rangeQuery(@NotNull @Valid EsPageable pageable, @NotBlank String fieldName, @NotNull Object from, @NotNull Object to) {
        val queryBuilder = QueryBuilders.rangeQuery(fieldName).from(from).to(to);
        return buildQuery(pageable, queryBuilder);
    }

    public String anyQuery(@NotNull SearchSourceBuilder searchBuilder) {
        return buildUdcQuery(searchBuilder);
    }

    public SearchSourceBuilder toSearchSourceBuilder(@Valid @NotNull EsPageable pageable, @NotNull QueryBuilder queryBuilder) {
        Validate.isTrue(pageable.isPaged(), "Pageable must be paged");

        val searchBuilder = new SearchSourceBuilder();
        searchBuilder.query(queryBuilder);
        searchBuilder.trackTotalHits(true);

        searchBuilder.size(pageable.getSize());
        searchBuilder.from(pageable.getFrom());

        val sort = pageable.getSort();
        if (Objects.nonNull(sort) && sort.isSorted()) {
            sort.forEach(order -> searchBuilder.sort(order.getProperty(), order.getDirection().isAscending() ? ASC : DESC));
        }

        return searchBuilder;
    }

    private String buildQuery(EsPageable pageable, QueryBuilder queryBuilder) {
        val searchBuilder = toSearchSourceBuilder(pageable, queryBuilder);
        return buildUdcQuery(searchBuilder);
    }

    private String buildUdcQuery(SearchSourceBuilder searchBuilder) {
        try {
            val rootNode = mapper.createObjectNode();
            rootNode.put(API_VERSION, "v1");
            rootNode.put(KIND, SEARCH_KIND);

            val queryJson = searchBuilder.toString();
            val queryNode = (ObjectNode) mapper.readTree(queryJson);
            rootNode.set(DATA, queryNode);

            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
}


