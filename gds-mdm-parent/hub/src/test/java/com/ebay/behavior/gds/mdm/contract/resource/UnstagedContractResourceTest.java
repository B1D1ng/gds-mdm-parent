package com.ebay.behavior.gds.mdm.contract.resource;

import com.ebay.behavior.gds.mdm.common.model.search.RelationalSearchRequest;
import com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion;
import com.ebay.behavior.gds.mdm.contract.model.ContractConfigView;
import com.ebay.behavior.gds.mdm.contract.service.UnstagedContractService;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnstagedContractResourceTest {

    @Mock
    private UnstagedContractService service;

    @InjectMocks
    private UnstagedContractResource resource;

    @BeforeEach
    void setUp() {
        Mockito.reset(service);
    }

    @Test
    void search_withValidRequest_returnsOkWithPage() {
        // Given
        RelationalSearchRequest request = RelationalSearchRequest.builder()
                .pageSize(20)
                .pageNumber(0)
                .filters(Collections.singletonList(
                        RelationalSearchRequest.Filter.builder()
                                .field("name")
                                .operator(SearchCriterion.CONTAINS)
                                .value("test")
                                .build()
                ))
                .build();

        ContractConfigView view1 = new ContractConfigView()
                .setId(1L)
                .setVersion(1)
                .setName("test-contract-1");

        ContractConfigView view2 = new ContractConfigView()
                .setId(2L)
                .setVersion(1)
                .setName("test-contract-2");

        Page<ContractConfigView> expectedPage = new PageImpl<>(Arrays.asList(view1, view2));

        when(service.search(any(RelationalSearchRequest.class))).thenReturn(expectedPage);

        // When
        Response response = resource.search(false, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.getEntity()).isNotNull();
        assertThat(response.getEntity()).isInstanceOf(Page.class);

        @SuppressWarnings("unchecked")
        Page<ContractConfigView> resultPage = (Page<ContractConfigView>) response.getEntity();
        assertThat(resultPage.getContent()).hasSize(2);
        assertThat(resultPage.getContent().get(0).getName()).isEqualTo("test-contract-1");
        assertThat(resultPage.getContent().get(1).getName()).isEqualTo("test-contract-2");

        verify(service, times(1)).search(request);
    }

    @Test
    void search_withAssociationsTrue_callsServiceAndReturnsOk() {
        // Given
        RelationalSearchRequest request = RelationalSearchRequest.builder()
                .pageSize(10)
                .pageNumber(0)
                .build();

        Page<ContractConfigView> expectedPage = new PageImpl<>(Collections.emptyList());

        when(service.search(any(RelationalSearchRequest.class))).thenReturn(expectedPage);

        // When
        Response response = resource.search(true, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        verify(service, times(1)).search(request);
    }

    @Test
    void search_withEmptyResult_returnsOkWithEmptyPage() {
        // Given
        RelationalSearchRequest request = RelationalSearchRequest.builder()
                .pageSize(20)
                .pageNumber(0)
                .build();

        Page<ContractConfigView> emptyPage = new PageImpl<>(Collections.emptyList());

        when(service.search(any(RelationalSearchRequest.class))).thenReturn(emptyPage);

        // When
        Response response = resource.search(false, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        @SuppressWarnings("unchecked")
        Page<ContractConfigView> resultPage = (Page<ContractConfigView>) response.getEntity();
        assertThat(resultPage.getContent()).isEmpty();

        verify(service, times(1)).search(request);
    }

    @Test
    void search_withMultipleFilters_callsServiceCorrectly() {
        // Given
        RelationalSearchRequest request = RelationalSearchRequest.builder()
                .pageSize(50)
                .pageNumber(1)
                .sort(RelationalSearchRequest.SortRequest.builder()
                        .field("name")
                        .direction(Sort.Direction.DESC)
                        .build())
                .filters(Arrays.asList(
                        RelationalSearchRequest.Filter.builder()
                                .field("name")
                                .operator(SearchCriterion.CONTAINS)
                                .value("contract")
                                .build(),
                        RelationalSearchRequest.Filter.builder()
                                .field("entityType")
                                .operator(SearchCriterion.EXACT_MATCH)
                                .value("ITEM")
                                .build(),
                        RelationalSearchRequest.Filter.builder()
                                .field("status")
                                .operator(SearchCriterion.EXACT_MATCH)
                                .value("IN_DEVELOPMENT")
                                .build()
                ))
                .build();

        ContractConfigView view = new ContractConfigView()
                .setId(10L)
                .setVersion(2)
                .setName("item-contract")
                .setEntityType("ITEM");

        Page<ContractConfigView> expectedPage = new PageImpl<>(Collections.singletonList(view));

        when(service.search(any(RelationalSearchRequest.class))).thenReturn(expectedPage);

        // When
        Response response = resource.search(false, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        @SuppressWarnings("unchecked")
        Page<ContractConfigView> resultPage = (Page<ContractConfigView>) response.getEntity();
        assertThat(resultPage.getContent()).hasSize(1);
        assertThat(resultPage.getContent().get(0).getId()).isEqualTo(10L);
        assertThat(resultPage.getContent().get(0).getVersion()).isEqualTo(2);

        verify(service, times(1)).search(request);
    }

    @Test
    void search_withCustomPageSize_passesRequestToService() {
        // Given
        RelationalSearchRequest request = RelationalSearchRequest.builder()
                .pageSize(100)
                .pageNumber(2)
                .build();

        Page<ContractConfigView> expectedPage = new PageImpl<>(Collections.emptyList());

        when(service.search(any(RelationalSearchRequest.class))).thenReturn(expectedPage);

        // When
        Response response = resource.search(false, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        verify(service, times(1)).search(request);
    }

    @Test
    void search_withSortRequest_passesRequestToService() {
        // Given
        RelationalSearchRequest request = RelationalSearchRequest.builder()
                .pageSize(20)
                .pageNumber(0)
                .sort(RelationalSearchRequest.SortRequest.builder()
                        .field("entityType")
                        .direction(Sort.Direction.ASC)
                        .build())
                .build();

        Page<ContractConfigView> expectedPage = new PageImpl<>(Collections.emptyList());

        when(service.search(any(RelationalSearchRequest.class))).thenReturn(expectedPage);

        // When
        Response response = resource.search(false, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        verify(service, times(1)).search(request);
    }
}
