package com.ebay.behavior.gds.mdm.common.util;

import com.ebay.behavior.gds.mdm.common.exception.ExternalCallException;

import lombok.experimental.UtilityClass;
import lombok.val;
import org.apache.commons.lang3.Validate;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.ExistsQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.PrefixQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.index.query.WildcardQueryBuilder;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.metrics.ParsedScriptedMetric;
import org.elasticsearch.search.aggregations.metrics.ParsedSum;
import org.elasticsearch.search.aggregations.metrics.ParsedTopHits;
import org.elasticsearch.search.aggregations.metrics.ScriptedMetricAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.SumAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.TopHitsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.xcontent.ContextParser;
import org.elasticsearch.xcontent.NamedXContentRegistry;
import org.elasticsearch.xcontent.ParseField;
import org.elasticsearch.xcontent.XContentParser;
import org.elasticsearch.xcontent.json.JsonXContent;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.elasticsearch.xcontent.DeprecationHandler.THROW_UNSUPPORTED_OPERATION;

@UtilityClass
public class ElasticsearchParser {

    private static final List<NamedXContentRegistry.Entry> DEFAULT_NAMED_X_CONTENTS = getDefaultNamedXContents();

    private static List<NamedXContentRegistry.Entry> getDefaultNamedXContents() {
        Map<String, ContextParser<Object, ? extends Aggregation>> map = new HashMap<>();
        map.put(TopHitsAggregationBuilder.NAME, (parser, content) -> ParsedTopHits.fromXContent(parser, (String) content));
        map.put(StringTerms.NAME, (parser, content) -> ParsedStringTerms.fromXContent(parser, (String) content));
        map.put(SumAggregationBuilder.NAME, (parser, content) -> ParsedSum.fromXContent(parser, (String) content));
        map.put(ScriptedMetricAggregationBuilder.NAME, (parser, content) -> ParsedScriptedMetric.fromXContent(parser, (String) content));

        var entries = map.entrySet()
                .stream()
                .map(entry -> new NamedXContentRegistry.Entry(Aggregation.class, new ParseField(entry.getKey()), entry.getValue()))
                .collect(Collectors.toList());

        entries.add(new NamedXContentRegistry.Entry(QueryBuilder.class, new ParseField(TermQueryBuilder.NAME), (parser, context) ->
                TermQueryBuilder.fromXContent(parser)));
        entries.add(new NamedXContentRegistry.Entry(QueryBuilder.class, new ParseField(TermsQueryBuilder.NAME), (parser, context) ->
                TermsQueryBuilder.fromXContent(parser)));
        entries.add(new NamedXContentRegistry.Entry(QueryBuilder.class, new ParseField(MatchQueryBuilder.NAME), (parser, context) ->
                MatchQueryBuilder.fromXContent(parser)));
        entries.add(new NamedXContentRegistry.Entry(QueryBuilder.class, new ParseField(MultiMatchQueryBuilder.NAME), (parser, context) ->
                MultiMatchQueryBuilder.fromXContent(parser)));
        entries.add(new NamedXContentRegistry.Entry(QueryBuilder.class, new ParseField(PrefixQueryBuilder.NAME), (parser, context) ->
                PrefixQueryBuilder.fromXContent(parser)));
        entries.add(new NamedXContentRegistry.Entry(QueryBuilder.class, new ParseField(ExistsQueryBuilder.NAME), (parser, context) ->
                ExistsQueryBuilder.fromXContent(parser)));
        entries.add(new NamedXContentRegistry.Entry(QueryBuilder.class, new ParseField(RangeQueryBuilder.NAME), (parser, context) ->
                RangeQueryBuilder.fromXContent(parser)));
        entries.add(new NamedXContentRegistry.Entry(QueryBuilder.class, new ParseField(WildcardQueryBuilder.NAME), (parser, context) ->
                WildcardQueryBuilder.fromXContent(parser)));
        entries.add(new NamedXContentRegistry.Entry(QueryBuilder.class, new ParseField(BoolQueryBuilder.NAME), (parser, context) ->
                BoolQueryBuilder.fromXContent(parser, 0)));
        return entries;
    }

    public SearchResponse toSearchResponse(String json) {
        try {
            val parser = createParser(json);
            return SearchResponse.fromXContent(parser);
        } catch (Exception ex) {
            throw new ExternalCallException("Failed to parse json: " + json, ex); // ExternalCallException since the json sourced from UDC
        }
    }

    public SearchSourceBuilder toSearchSourceBuilder(String json) {
        try {
            val parser = createParser(json);
            return SearchSourceBuilder.fromXContent(parser);
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    private XContentParser createParser(String json) throws IOException {
        Validate.notBlank(json, "jsonResponse must not be blank");
        val registry = new NamedXContentRegistry(DEFAULT_NAMED_X_CONTENTS);
        return JsonXContent.jsonXContent.createParser(registry, THROW_UNSUPPORTED_OPERATION, json);
    }
}
