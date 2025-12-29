package com.ebay.behavior.gds.mdm.common.service.udc;

import com.ebay.behavior.gds.mdm.common.model.EsPageable;
import com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.index.query.Operator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType.EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class UdcQueryBuilderTest {

    @Spy
    private ObjectMapper mapper = new ObjectMapper();

    @Spy
    @InjectMocks
    private UdcQueryBuilder udcQueryBuilder;

    private final EsPageable pageable = EsPageable.of(0, 10);

    @Test
    void termQuery() {
        var term = "123";
        var fieldName = UdcEntityType.SIGNAL.getIdName();

        var result = udcQueryBuilder.termQuery(pageable, term, fieldName);

        assertThat(result).contains("\"term\"");
        assertThat(result).contains("\"value\" : \"123\"");
    }

    @Test
    void termQuery_unpaged_error() {
        assertThatThrownBy(() -> udcQueryBuilder.termQuery(EsPageable.unpaged(), "term", "name"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be paged");
    }

    @Test
    void termsQuery() {
        Set<Long> terms = Set.of(123L);
        var fieldName = EVENT.getIdName();

        var result = udcQueryBuilder.termsQuery(pageable, terms, fieldName);

        assertThat(result).contains("\"terms\"");
        assertThat(result).contains("\"" + EVENT.getIdName() + "\" : [ 123 ]");
    }

    @Test
    void wildcardQuery() {
        var pattern = "*abc";
        var fieldName = "name";

        var result = udcQueryBuilder.wildcardQuery(pageable, pattern, fieldName);

        assertThat(result).contains("\"name\"");
        assertThat(result).contains("\"wildcard\" : \"*abc\"");
    }

    @Test
    void matchQuery() {
        var text = "search text";
        var fieldName = "description";

        var result = udcQueryBuilder.matchQuery(pageable, text, fieldName);

        assertThat(result).contains("\"match\"");
        assertThat(result).contains("\"query\" : \"search text\"");
    }

    @Test
    void matchAllQuery() {
        var result = udcQueryBuilder.matchAllQuery(pageable);

        assertThat(result).contains("\"match_all\"");
    }

    @Test
    void multiMatchQuery_or() {
        var text = "search text";
        String[] fieldNames = {"description", "title"};

        var result = udcQueryBuilder.multiMatchQuery(pageable, text, Operator.OR, fieldNames);

        assertThat(result).contains("\"multi_match\"");
        assertThat(result).contains("\"operator\" : \"OR\"");
        assertThat(result).contains("\"query\" : \"search text\"");
        assertThat(result).contains("\"fields\" : [ \"description^1.0\", \"title^1.0\" ]");
    }

    @Test
    void multiMatchQuery_and() {
        var text = "search text";
        String[] fieldNames = {"description", "title"};

        var result = udcQueryBuilder.multiMatchQuery(pageable, text, Operator.AND, fieldNames);

        assertThat(result).contains("\"multi_match\"");
        assertThat(result).contains("\"operator\" : \"AND\"");
        assertThat(result).contains("\"query\" : \"search text\"");
        assertThat(result).contains("\"fields\" : [ \"description^1.0\", \"title^1.0\" ]");
    }

    @Test
    void prefixQuery() {
        var prefix = "pre";
        var fieldName = "description";

        var result = udcQueryBuilder.prefixQuery(pageable, prefix, fieldName);

        assertThat(result).contains("\"prefix\"");
        assertThat(result).contains("\"value\" : \"pre\"");
    }

    @Test
    void existsQuery() {
        var fieldName = "description";

        var result = udcQueryBuilder.existsQuery(pageable, fieldName);

        assertThat(result).contains("\"exists\"");
        assertThat(result).contains("\"field\" : \"description\"");
    }

    @Test
    void rangeQuery() {
        var fieldName = "timestamp";
        var from = "2023-01-01";
        var to = "2023-12-31";

        var result = udcQueryBuilder.rangeQuery(pageable, fieldName, from, to);

        assertThat(result).contains("\"range\"");
        assertThat(result).contains("\"to\" : \"2023-12-31\"");
        assertThat(result).contains("\"from\" : \"2023-01-01\"");
    }
}