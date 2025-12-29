package com.ebay.behavior.gds.mdm.contract.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.contract.model.Transformation;
import com.ebay.behavior.gds.mdm.contract.repository.TransformationRepository;
import com.ebay.behavior.gds.mdm.contract.repository.TransformerRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomString;
import static com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils.transformation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

class TransformationServiceTest {

    @InjectMocks
    private TransformationService service;

    @Mock
    private TransformationRepository repository;
    @Mock
    private TransformerRepository transformerRepository;

    private Transformation transformation;

    private static final long TEST_ID = 1L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        transformation = transformation(getRandomString(), "description", "expression");
    }

    @Test
    void test_create() {
        var created = transformation.toBuilder().id(TEST_ID).build();
        when(repository.save(transformation)).thenReturn(created);

        var result = service.create(transformation);
        assertThat(result).isEqualTo(created);
        verify(repository).save(transformation);
    }

    @Test
    void test_create_throwsException() {
        when(repository.save(transformation)).thenThrow(new DataIntegrityViolationException("FK violation"));

        assertThatThrownBy(() -> service.create(transformation)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void test_getById_found() {
        var created = transformation.toBuilder().id(TEST_ID).build();
        when(repository.findById(TEST_ID)).thenReturn(Optional.of(created));
        var result = service.getById(TEST_ID);
        assertThat(result).isEqualTo(created);
        verify(repository).findById(TEST_ID);
    }

    @Test
    void test_getById_notFound() {
        when(repository.findById(TEST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(TEST_ID))
                .isInstanceOf(DataNotFoundException.class)
                .hasMessageContaining("Transformation id=%d doesn't found".formatted(TEST_ID));
        verify(repository).findById(TEST_ID);
    }

    @Test
    void test_update_dataViolation() {
        var created = transformation.toBuilder().id(TEST_ID).revision(0).componentId(2L).build();
        when(repository.findById(TEST_ID)).thenReturn(Optional.of(created));
        when(repository.save(created)).thenThrow(new DataIntegrityViolationException("FK violation"));

        assertThatThrownBy(() -> service.update(created))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("FK violation");
        verify(repository).findById(TEST_ID);
    }

    @Test
    void test_update_succeed() {
        var created = transformation.toBuilder().id(TEST_ID).revision(0).componentId(2L).build();
        when(repository.findById(TEST_ID)).thenReturn(Optional.of(created));
        var updated = created.toBuilder().field(getRandomString()).build();
        when(repository.save(updated)).thenReturn(updated);

        var result = service.update(updated);
        assertThat(result).isEqualTo(updated);
        verify(repository).findById(TEST_ID);
        verify(repository).save(updated);
    }

    @Test
    void test_delete_succeed() {
        service.delete(TEST_ID);
        verify(repository).deleteById(TEST_ID);
    }
}
