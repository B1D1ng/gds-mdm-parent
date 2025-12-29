package com.ebay.behavior.gds.mdm.contract.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.contract.model.Transformer;
import com.ebay.behavior.gds.mdm.contract.repository.FilterRepository;
import com.ebay.behavior.gds.mdm.contract.repository.TransformationRepository;
import com.ebay.behavior.gds.mdm.contract.repository.TransformerRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomString;
import static com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils.filter;
import static com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils.transformation;
import static com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils.transformer;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransformerServiceTest {

    @InjectMocks
    private TransformerService service;

    @Mock
    private TransformerRepository repository;

    @Mock
    private TransformationRepository transformationRepository;

    @Mock
    private FilterRepository filterRepository;

    private Transformer transformer;

    private static final Long TEST_ID = 1L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        transformer = transformer(getRandomString());
    }

    @Test
    void test_create_succeed() {
        var created = transformer.toBuilder().id(TEST_ID).build();
        when(repository.save(transformer)).thenReturn(created);

        var result = service.create(transformer);

        assertThat(result).isEqualTo(created);
        verify(repository).save(transformer);
    }

    @Test
    void test_create_invalidType() {
        var invalidTransformer = transformer.toBuilder().type("InvalidType").build();
        try {
            service.create(invalidTransformer);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo("type mismatch from Transformer to InvalidType");
        }
    }

    @Test
    void test_getById_succeed() {
        var created = transformer.toBuilder().id(TEST_ID).build();
        when(repository.findById(TEST_ID)).thenReturn(Optional.of(created));

        var result = service.getById(TEST_ID);
        assertThat(result).isEqualTo(created);
        verify(repository).findById(TEST_ID);
    }

    @Test
    void test_getById_notFound() {
        when(repository.findById(TEST_ID)).thenReturn(Optional.empty());
        try {
            service.getById(TEST_ID);
        } catch (DataNotFoundException e) {
            assertThat(e.getMessage()).isEqualTo("Transformer id=%d doesn't found".formatted(TEST_ID));
        }
    }

    @Test
    void test_getTransformations_succeed() {
        var created = transformer.toBuilder().id(TEST_ID).revision(0).build();
        var createdTransformation = transformation(getRandomString(), "description", "expression").toBuilder().id(2L).componentId(TEST_ID).build();
        created.setTransformations(Set.of(createdTransformation));

        when(repository.findById(TEST_ID)).thenReturn(Optional.of(created));
        when(transformationRepository.findByComponentId(TEST_ID)).thenReturn(List.of(createdTransformation));
        var result = service.getTransformations(TEST_ID);

        assertThat(result).isEqualTo(Set.of(createdTransformation));
        verify(repository).findById(TEST_ID);
        verify(transformationRepository).findByComponentId(TEST_ID);
    }

    @Test
    void test_getFilters_succeed() {
        var created = transformer.toBuilder().id(TEST_ID).revision(0).build();
        var createdFilter = filter(getRandomString()).toBuilder().id(2L).componentId(TEST_ID).build();
        created.setFilters(Set.of(createdFilter));

        when(repository.findById(TEST_ID)).thenReturn(Optional.of(created));
        when(filterRepository.findByComponentId(TEST_ID)).thenReturn(List.of(createdFilter));

        var result = service.getFilters(TEST_ID);

        assertThat(result).isEqualTo(Set.of(createdFilter));
        verify(repository).findById(TEST_ID);
        verify(filterRepository).findByComponentId(TEST_ID);
    }
}
