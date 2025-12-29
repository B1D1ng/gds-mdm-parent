package com.ebay.behavior.gds.mdm.common.service.udc;

import com.ebay.behavior.gds.mdm.common.model.external.WithAckAndErrorMessage;
import com.ebay.behavior.gds.mdm.common.service.token.UdcTokenGenerator;
import com.ebay.behavior.gds.mdm.common.testUtil.TestModel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class UdcReadServiceImplTest {

    @Mock
    private UdcTokenGenerator tokenGenerator;

    @Mock
    private WithAckAndErrorMessage response;

    @Spy
    @InjectMocks
    private UdcReadServiceImpl service;

    @Test
    void createQueryParams() {
        var queryParams = service.createQueryParams("key", "value");

        assertThat(queryParams).hasSize(1);
        assertThat(queryParams.get(0).key()).isEqualTo("key");
        assertThat(queryParams.get(0).value()).isEqualTo("value");
    }

    @Test
    void anyQuery_termQueryWithoutSize_error() {
        var json = """
                {
                "from": 0,
                  "query": {
                    "term": {
                      "field": {
                        "value": "value"
                      }
                    }
                  }
                }
                """;

        assertThatThrownBy(() -> service.anyQuery(TestModel.class, json))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("\"size\" must be greater than zero");

    }

    @Test
    void anyQuery_termQueryWithoutFrom_error() {
        var json = """
                {
                "size": 10,
                  "query": {
                    "term": {
                      "field": {
                        "value": "value"
                      }
                    }
                  }
                }
                """;

        assertThatThrownBy(() -> service.anyQuery(TestModel.class, json))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("\"from\" must be greater than or equal to zero");

    }
}