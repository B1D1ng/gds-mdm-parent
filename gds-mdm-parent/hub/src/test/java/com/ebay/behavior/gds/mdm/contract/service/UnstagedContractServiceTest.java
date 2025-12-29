package com.ebay.behavior.gds.mdm.contract.service;

import com.ebay.behavior.gds.mdm.common.model.search.RelationalSearchRequest;
import com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion;
import com.ebay.behavior.gds.mdm.contract.model.ContractConfigView;
import com.ebay.behavior.gds.mdm.contract.repository.ContractConfigViewRepository;
import com.ebay.behavior.gds.mdm.contract.repository.UnstagedContractRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UnstagedContractServiceTest {

    @InjectMocks
    private UnstagedContractService unstagedContractService;

    @Mock
    private UnstagedContractRepository repository;

    @Mock
    private ContractConfigViewRepository configViewRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getLatestVersion_withValidId_returnsVersion() {
        when(repository.findLatestVersion(anyLong())).thenReturn(Optional.of(2));

        int version = unstagedContractService.getLatestVersion(123L);

        assertThat(version).isEqualTo(2);
    }

    @Test
    void search_withValidRequest_returnsPageOfContractConfigView() {
        // Given
        RelationalSearchRequest request = RelationalSearchRequest.builder()
                .pageSize(20)
                .pageNumber(0)
                .filters(Arrays.asList(
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

        when(configViewRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(expectedPage);

        // When
        Page<ContractConfigView> result = unstagedContractService.search(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getName()).isEqualTo("test-contract-1");
        assertThat(result.getContent().get(1).getName()).isEqualTo("test-contract-2");
        verify(configViewRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void search_withNullSort_defaultsToIdAsc() {
        // Given
        RelationalSearchRequest request = RelationalSearchRequest.builder()
                .pageSize(20)
                .pageNumber(0)
                .sort(null)
                .build();

        Page<ContractConfigView> expectedPage = new PageImpl<>(Collections.emptyList());

        when(configViewRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(expectedPage);

        // When
        Page<ContractConfigView> result = unstagedContractService.search(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(request.getSort()).isNotNull();
        assertThat(request.getSort().getField()).isEqualTo("id");
        assertThat(request.getSort().getDirection()).isEqualTo(Sort.Direction.ASC);
        verify(configViewRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void search_withCustomSort_preservesSort() {
        // Given
        RelationalSearchRequest.SortRequest sortRequest = RelationalSearchRequest.SortRequest.builder()
                .field("name")
                .direction(Sort.Direction.DESC)
                .build();

        RelationalSearchRequest request = RelationalSearchRequest.builder()
                .pageSize(20)
                .pageNumber(0)
                .sort(sortRequest)
                .build();

        Page<ContractConfigView> expectedPage = new PageImpl<>(Collections.emptyList());

        when(configViewRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(expectedPage);

        // When
        Page<ContractConfigView> result = unstagedContractService.search(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(request.getSort().getField()).isEqualTo("name");
        assertThat(request.getSort().getDirection()).isEqualTo(Sort.Direction.DESC);
        verify(configViewRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void search_withMultipleFilters_appliesAllFilters() {
        // Given
        RelationalSearchRequest request = RelationalSearchRequest.builder()
                .pageSize(10)
                .pageNumber(0)
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
                                .build()
                ))
                .build();

        ContractConfigView view = new ContractConfigView()
                .setId(1L)
                .setVersion(1)
                .setName("item-contract")
                .setEntityType("ITEM");

        Page<ContractConfigView> expectedPage = new PageImpl<>(Collections.singletonList(view));

        when(configViewRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(expectedPage);

        // When
        Page<ContractConfigView> result = unstagedContractService.search(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("item-contract");
        assertThat(result.getContent().get(0).getEntityType()).isEqualTo("ITEM");
        verify(configViewRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void search_withEmptyResult_returnsEmptyPage() {
        // Given
        RelationalSearchRequest request = RelationalSearchRequest.builder()
                .pageSize(20)
                .pageNumber(0)
                .build();

        Page<ContractConfigView> expectedPage = new PageImpl<>(Collections.emptyList());

        when(configViewRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(expectedPage);

        // When
        Page<ContractConfigView> result = unstagedContractService.search(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        verify(configViewRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }
}