package com.ebay.behavior.gds.mdm.contract.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.search.RelationalSearchRequest;
import com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion;
import com.ebay.behavior.gds.mdm.contract.model.BesSource;
import com.ebay.behavior.gds.mdm.contract.model.Routing;
import com.ebay.behavior.gds.mdm.contract.model.manyToMany.RoutingComponentMapping;
import com.ebay.behavior.gds.mdm.contract.repository.BesSourceRepository;
import com.ebay.behavior.gds.mdm.contract.repository.StreamingConfigRepository;
import com.ebay.behavior.gds.mdm.contract.repository.manyToMany.RoutingComponentMappingRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomString;
import static com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils.besSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BesSourceServiceTest {

    @InjectMocks
    private BesSourceService besSourceService;

    @Mock
    private BesSourceRepository repository;

    @Mock
    private StreamingConfigRepository streamingConfigRepository;

    @Mock
    private RoutingComponentMappingRepository mappingRepository;

    private BesSource besSource;
    private static final Long TEST_ID = 123L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        besSource = besSource(getRandomString()).toBuilder().id(TEST_ID).revision(1).build();
    }

    @Test
    void create_validBesSource_returnsCreated() {
        var newBesSource = besSource(getRandomString());
        var savedBesSource = newBesSource.toBuilder().id(TEST_ID).build();

        when(repository.save(newBesSource)).thenReturn(savedBesSource);

        var result = besSourceService.create(newBesSource);

        assertThat(result).isEqualTo(savedBesSource);
        assertThat(result.getId()).isEqualTo(TEST_ID);
        verify(repository).save(newBesSource);
    }

    @Test
    void create_invalidType_throwsException() {
        var invalidBesSource = besSource(getRandomString()).toBuilder().type("WrongType").build();

        assertThatThrownBy(() -> besSourceService.create(invalidBesSource))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("type mismatch");

        verify(repository, never()).save(any());
    }

    @Test
    void create_dataIntegrityViolation_throwsException() {
        var newBesSource = besSource(getRandomString());

        when(repository.save(newBesSource)).thenThrow(new DataIntegrityViolationException("FK violation"));

        assertThatThrownBy(() -> besSourceService.create(newBesSource))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void update_validBesSource_returnsUpdated() {
        var updatedName = getRandomString();
        var updateRequest = besSource.toBuilder().name(updatedName).revision(1).build();

        when(repository.findById(TEST_ID)).thenReturn(Optional.of(besSource));
        when(repository.save(updateRequest)).thenReturn(updateRequest);

        var result = besSourceService.update(updateRequest);

        assertThat(result.getName()).isEqualTo(updatedName);
        verify(repository).findById(TEST_ID);
        verify(repository).save(updateRequest);
    }

    @Test
    void update_invalidType_throwsException() {
        var invalidUpdate = besSource.toBuilder().type("WrongType").build();

        assertThatThrownBy(() -> besSourceService.update(invalidUpdate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("type mismatch");

        verify(repository, never()).save(any());
    }

    @Test
    void update_notFound_throwsException() {
        when(repository.findById(TEST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> besSourceService.update(besSource))
                .isInstanceOf(DataNotFoundException.class);

        verify(repository).findById(TEST_ID);
        verify(repository, never()).save(any());
    }

    @Test
    void getById_existingId_returnsBesSource() {
        when(repository.findById(TEST_ID)).thenReturn(Optional.of(besSource));

        var result = besSourceService.getById(TEST_ID);

        assertThat(result).isEqualTo(besSource);
        verify(repository).findById(TEST_ID);
    }

    @Test
    void getById_nonExistingId_throwsException() {
        when(repository.findById(TEST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> besSourceService.getById(TEST_ID))
                .isInstanceOf(DataNotFoundException.class);

        verify(repository).findById(TEST_ID);
    }

    @Test
    void findById_existingId_returnsOptional() {
        when(repository.findById(TEST_ID)).thenReturn(Optional.of(besSource));

        var result = besSourceService.findById(TEST_ID);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(besSource);
        verify(repository).findById(TEST_ID);
    }

    @Test
    void findById_nonExistingId_returnsEmpty() {
        when(repository.findById(TEST_ID)).thenReturn(Optional.empty());

        var result = besSourceService.findById(TEST_ID);

        assertThat(result).isEmpty();
        verify(repository).findById(TEST_ID);
    }

    @Test
    void delete_withoutRoutings_deletesSuccessfully() {
        when(repository.findById(TEST_ID)).thenReturn(Optional.of(besSource));
        when(mappingRepository.findByComponentId(TEST_ID)).thenReturn(List.of());

        besSourceService.delete(TEST_ID);

        verify(streamingConfigRepository).deleteAllByComponentId(TEST_ID);
        verify(repository).deleteById(TEST_ID);
    }

    @Test
    void delete_withRoutings_throwsException() {
        var routing = Routing.builder().id(1L).name("testRouting").build();
        var mapping = RoutingComponentMapping.builder().routing(routing).build();

        when(repository.findById(TEST_ID)).thenReturn(Optional.of(besSource));
        when(mappingRepository.findByComponentId(TEST_ID)).thenReturn(List.of(mapping));

        assertThatThrownBy(() -> besSourceService.delete(TEST_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot delete component with associated routings");

        verify(streamingConfigRepository, never()).deleteAllByComponentId(anyLong());
        verify(repository, never()).deleteById(anyLong());
    }

    @Test
    void delete_nonExistingId_throwsException() {
        when(repository.findById(TEST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> besSourceService.delete(TEST_ID))
                .isInstanceOf(DataNotFoundException.class);

        verify(streamingConfigRepository, never()).deleteAllByComponentId(anyLong());
        verify(repository, never()).deleteById(anyLong());
    }

    @Test
    void getRoutings_existingId_returnsRoutings() {
        var routing1 = Routing.builder().id(1L).name("routing1").build();
        var routing2 = Routing.builder().id(2L).name("routing2").build();
        var mapping1 = RoutingComponentMapping.builder().routing(routing1).build();
        var mapping2 = RoutingComponentMapping.builder().routing(routing2).build();

        when(repository.findById(TEST_ID)).thenReturn(Optional.of(besSource));
        when(mappingRepository.findByComponentId(TEST_ID)).thenReturn(List.of(mapping1, mapping2));

        var result = besSourceService.getRoutings(TEST_ID);

        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder(routing1, routing2);
        verify(repository).findById(TEST_ID);
        verify(mappingRepository).findByComponentId(TEST_ID);
    }

    @Test
    void getByIdWithAssociations_existingId_returnsBesSourceWithAssociations() {
        when(repository.findById(TEST_ID)).thenReturn(Optional.of(besSource));

        var result = besSourceService.getByIdWithAssociations(TEST_ID);

        assertThat(result).isEqualTo(besSource);
        verify(repository).findById(TEST_ID);
    }

    @Test
    void createAll_validBesSources_returnsCreated() {
        var besSource1 = besSource(getRandomString());
        var besSource2 = besSource(getRandomString());
        var besSourceSet = Set.of(besSource1, besSource2);
        var savedSources = List.of(
                besSource1.toBuilder().id(1L).build(),
                besSource2.toBuilder().id(2L).build()
        );

        when(repository.saveAllAndFlush(besSourceSet)).thenReturn(savedSources);

        var result = besSourceService.createAll(besSourceSet);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isNotNull();
        assertThat(result.get(1).getId()).isNotNull();
        verify(repository).saveAllAndFlush(besSourceSet);
    }

    @Test
    void findAllById_validIds_returnsBesSourceList() {
        var ids = Set.of(1L, 2L);
        var besSource1 = besSource(getRandomString()).toBuilder().id(1L).build();
        var besSource2 = besSource(getRandomString()).toBuilder().id(2L).build();
        var expectedList = List.of(besSource1, besSource2);

        when(repository.findAllById(ids)).thenReturn(expectedList);

        var result = besSourceService.findAllById(ids);

        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder(besSource1, besSource2);
        verify(repository).findAllById(ids);
    }

    @Test
    @SuppressWarnings("unchecked")
    void search_validRequest_returnsPage() {
        var searchRequest = RelationalSearchRequest.builder()
                .filters(List.of(
                        RelationalSearchRequest.Filter.builder()
                                .field("name")
                                .operator(SearchCriterion.CONTAINS)
                                .value("test")
                                .build()
                ))
                .pageNumber(0)
                .pageSize(10)
                .build();

        var besSourceList = List.of(besSource);
        var page = new PageImpl<>(besSourceList);

        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        var result = besSourceService.search(searchRequest, false);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(besSource);
        verify(repository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void search_withAssociations_returnsPageWithAssociations() {
        var searchRequest = RelationalSearchRequest.builder()
                .filters(List.of(
                        RelationalSearchRequest.Filter.builder()
                                .field("name")
                                .operator(SearchCriterion.CONTAINS)
                                .value("test")
                                .build()
                ))
                .pageNumber(0)
                .pageSize(10)
                .build();

        var besSourceList = List.of(besSource);
        var page = new PageImpl<>(besSourceList);

        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        var result = besSourceService.search(searchRequest, true);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(besSource);
        verify(repository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getModelType_returnsCorrectClass() {
        var result = besSourceService.getModelType();

        assertThat(result).isEqualTo(BesSource.class);
    }

    @Test
    void getRepository_returnsCorrectRepository() {
        var result = besSourceService.getRepository();

        assertThat(result).isEqualTo(repository);
    }
}