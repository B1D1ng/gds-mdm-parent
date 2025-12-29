package com.ebay.behavior.gds.mdm.common.util;

import com.ebay.behavior.gds.mdm.common.exception.ExternalCallException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ElasticsearchParserTest {

    @Test
    void toSearchResponse_nullJsonResponse() {
        assertThatThrownBy(() -> ElasticsearchParser.toSearchResponse(null))
                .isInstanceOf(ExternalCallException.class);
    }

    @Test
    void toSearchResponse_normalJsonResponse() {
        var json = """
                {
                  "took": 1,
                  "timed_out": false,
                  "_shards": {
                    "total": 1,
                    "successful": 1,
                    "skipped": 0,
                    "failed": 0
                  },
                  "hits": {
                    "total": {
                      "value": 1,
                      "relation": "eq"
                    },
                    "max_score": 1.0,
                    "hits": [
                      {
                        "_index": "test",
                        "_type": "_doc",
                        "_id": "1",
                        "_score": 1.0,
                        "_source": {
                          "field": "value"
                        }
                      }
                    ]
                  }
                }
                """;
        var response = ElasticsearchParser.toSearchResponse(json);

        assertThat(response).isNotNull();
        assertThat(response.getHits().getTotalHits().value).isEqualTo(1);
        assertThat(response.getHits().getAt(0).getSourceAsMap().get("field")).isEqualTo("value");
    }

    @Test
    void toSearchSourceBuilder_validJson() {
        var json = """
                {
                  "from" : 5,
                  "size" : 10,
                  "query": {
                    "term": {
                      "field": {
                        "value": "value"
                      }
                    }
                  }
                }
                """;

        var searchBuilder = ElasticsearchParser.toSearchSourceBuilder(json);

        assertThat(searchBuilder).isNotNull();
        assertThat(searchBuilder.query().getName()).isEqualTo("term");
        assertThat(searchBuilder.from()).isEqualTo(5);
        assertThat(searchBuilder.size()).isEqualTo(10);
    }
}