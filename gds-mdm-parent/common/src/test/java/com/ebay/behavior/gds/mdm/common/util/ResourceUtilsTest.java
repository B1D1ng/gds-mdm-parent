package com.ebay.behavior.gds.mdm.common.util;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.exception.ExternalCallException;
import com.ebay.behavior.gds.mdm.common.testUtil.TestModel;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.search.Search.SEARCH_TERM;
import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.EXACT_MATCH;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.UNKNOWN;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.handleResponse;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.setRequestUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceUtilsTest {

    private static final String STR_URL = "http://localhost";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private UriInfo uriInfo;

    @Mock
    private UriBuilder uriBuilder;

    @Mock
    private Response clientResponse;

    private TestModel model;
    private final String host = "TestSystem";
    private final String path = "/test/path";

    @BeforeEach
    void setUp() {
        Mockito.reset(clientResponse);
        model = TestModel.builder()
                .id(123L).parentId(456L).revision(1).name("test")
                .build();
    }

    @Test
    void created() throws MalformedURLException, URISyntaxException {
        doReturn(uriBuilder).when(uriInfo).getAbsolutePathBuilder();
        var uri = new URL(STR_URL).toURI();
        doReturn(uri).when(uriBuilder).build();

        var response = ResourceUtils.created(uriInfo, model);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED.value());
    }

    @Test
    void getQueryParams() {
        var params = ResourceUtils.getQueryParams("name", "value", EXACT_MATCH);

        assertThat(params.size()).isEqualTo(3);
    }

    @Test
    void getQueryParams_searchTermProvided() {
        var searchTerm = "testTerm";

        Map<String, String> result = ResourceUtils.getQueryParams(searchTerm);

        assertThat(result)
                .isNotNull()
                .hasSize(1)
                .containsEntry(SEARCH_TERM, searchTerm);
    }

    @Test
    void validateUpdate_RequestId_idDoesntMatch() {
        model.setId(123L);
        assertThatThrownBy(() -> ResourceUtils.validateUpdateRequestId(model, 456L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void handleResponse_statusNotInRange_error() {
        var clientResponse = Response.status(Response.Status.BAD_REQUEST).build();

        assertThatThrownBy(() ->
                handleResponse(clientResponse, String.class, path, objectMapper, host))
                .isInstanceOf(ExternalCallException.class)
                .hasMessageContaining("Got unexpected 400 response");
    }

    @Test
    void handleResponse_nullOkResponse_error() {
        when(clientResponse.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        when(clientResponse.readEntity(String.class)).thenReturn(null);

        assertThatThrownBy(() ->
                handleResponse(clientResponse, String.class, path, objectMapper, host))
                .isInstanceOf(ExternalCallException.class)
                .hasMessageContaining("Got null response");
    }

    @Test
    void handleResponse_nullNoContentResponse() {
        when(clientResponse.getStatus()).thenReturn(Response.Status.NO_CONTENT.getStatusCode());
        when(clientResponse.readEntity(String.class)).thenReturn(null);

        var payload = handleResponse(clientResponse, String.class, path, objectMapper, host);

        assertThat(payload).isNull();
    }

    @Test
    void handleResponse_417_error() {
        when(clientResponse.getStatus()).thenReturn(417);

        assertThatThrownBy(() ->
                handleResponse(clientResponse, String.class, path, objectMapper, host))
                .isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void handleResponse_404_error() {
        when(clientResponse.getStatus()).thenReturn(404);

        assertThatThrownBy(() ->
                handleResponse(clientResponse, String.class, path, objectMapper, host))
                .isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void setRequestUser_userProvided() {
        var user = "testUser";
        try (var utilities = mockStatic(RequestContextHolder.class)) {
            var requestAttributes = mock(RequestAttributes.class);
            utilities.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributes);

            setRequestUser(user);

            verify(requestAttributes).setAttribute(anyString(), anyString(), anyInt());
        }
    }

    @Test
    void testCsvStringToSet_withValidCsvString() {
        String csv = "apple, banana, cherry";
        Set<String> expectedSet = Set.of("apple", "banana", "cherry");

        Set<String> resultSet = ResourceUtils.csvStringToSet(csv);

        assertThat(resultSet).containsExactlyInAnyOrderElementsOf(expectedSet);
    }

    @Test
    void testCsvStringToSet_withEmptyOrBlankString() {
        String emptyCsv = "";
        String blankCsv = "   ";

        Set<String> resultSetEmpty = ResourceUtils.csvStringToSet(emptyCsv);
        Set<String> resultSetBlank = ResourceUtils.csvStringToSet(blankCsv);

        assertThat(resultSetEmpty).isEmpty();
        assertThat(resultSetBlank).isEmpty();
    }

    @Test
    void getHostName() {
        try (var inetAddress = mockStatic(InetAddress.class)) {
            var localhostAddress = mock(InetAddress.class);
            inetAddress.when(InetAddress::getLocalHost).thenReturn(localhostAddress);
            when(localhostAddress.getHostName()).thenReturn("expectedHostName");

            var hostName = ResourceUtils.getHostName();

            assertThat(hostName).isEqualTo("expectedHostName");
        }
    }

    @Test
    void getHostName_unknown() {
        try (var inetAddress = mockStatic(InetAddress.class)) {
            inetAddress.when(InetAddress::getLocalHost).thenThrow(new UnknownHostException());

            var hostName = ResourceUtils.getHostName();

            assertThat(hostName).isEqualTo(UNKNOWN);
        }
    }
}