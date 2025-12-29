package com.ebay.behavior.gds.mdm.contract.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.contract.model.LdmViewSink;
import com.ebay.behavior.gds.mdm.contract.model.manyToMany.RoutingComponentMapping;
import com.ebay.behavior.gds.mdm.contract.repository.LdmViewSinkRepostory;
import com.ebay.behavior.gds.mdm.contract.repository.manyToMany.RoutingComponentMappingRepository;

import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomString;
import static com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils.ldmViewSink;
import static com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

class LdmSinkServiceTest {

    @InjectMocks
    private LdmViewSinkService service;

    @Mock
    private LdmViewSinkRepostory repostory;
    @Mock
    private RoutingComponentMappingRepository mappingRepository;

    private static final Long TEST_ID = 1L;
    private LdmViewSink ldmViewSink;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        ldmViewSink = ldmViewSink(getRandomString());
    }

    @Test
    void test_create_valid() {
        val created = ldmViewSink.toBuilder().id(TEST_ID).build();
        when(repostory.save(ldmViewSink)).thenReturn(created);
        val result = service.create(ldmViewSink);

        assertThat(result).isEqualTo(created);
        verify(repostory).save(ldmViewSink);
    }

    @Test
    void test_create_wrongType() {
        val newLdmViewSink = ldmViewSink.toBuilder().type("ldmViewSink").build();

        assertThatThrownBy(() -> service.create(newLdmViewSink))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("type mismatch from LdmViewSink to ldmViewSink");

        verify(repostory, never()).save(any());
    }

    @Test
    void test_create_dataIntegrityViolation() {
        when(repostory.save(ldmViewSink)).thenThrow(new DataIntegrityViolationException("Data integrity violation"));

        assertThatThrownBy(() -> service.create(ldmViewSink))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Data integrity violation");
    }

    @Test
    void test_getById() {
        val created = ldmViewSink.toBuilder().id(TEST_ID).build();
        when(repostory.findById(TEST_ID)).thenReturn(Optional.of(created));
        val result = service.getById(TEST_ID);

        assertThat(result).isEqualTo(created);
        verify(repostory).findById(TEST_ID);
    }

    @Test
    void test_getById_notFound() {
        when(repostory.findById(TEST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(TEST_ID))
                .isInstanceOf(DataNotFoundException.class)
                .hasMessageContaining("LdmViewSink", String.valueOf(TEST_ID));

        verify(repostory).findById(TEST_ID);
    }

    @Test
    void test_update() {
        val created = ldmViewSink.toBuilder().id(TEST_ID).revision(0).build();
        val updateName = getRandomString();
        val updated = created.toBuilder().name(updateName).revision(1).build();

        when(repostory.findById(TEST_ID)).thenReturn(Optional.of(created));
        when(repostory.save(updated)).thenReturn(updated);

        val result = service.update(updated);
        assertThat(result).isEqualTo(updated);
        verify(repostory).findById(TEST_ID);
        verify(repostory).save(updated);
    }

    @Test
    void test_delete_exists_withoutRouting() {
        val created = ldmViewSink.toBuilder().id(TEST_ID).build();
        when(repostory.findById(TEST_ID)).thenReturn(Optional.of(created));
        when(mappingRepository.findByComponentId(TEST_ID)).thenReturn(List.of());

        service.delete(TEST_ID);

        verify(repostory, times(2)).findById(TEST_ID);
        verify(repostory).deleteById(TEST_ID);
    }

    @Test
    void test_delete_exists_withRouting() {
        val created = ldmViewSink.toBuilder().id(TEST_ID).build();
        when(repostory.findById(TEST_ID)).thenReturn(Optional.of(created));
        val routing = routing("routinxxg1").toBuilder().id(100L).build();
        val mapping = RoutingComponentMapping.builder().routingId(routing.getId()).componentId(TEST_ID).routing(routing).build();
        when(mappingRepository.findByComponentId(TEST_ID)).thenReturn(List.of(mapping));

        assertThatThrownBy(() -> service.delete(TEST_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot delete component with associated routings.");

        verify(repostory, times(2)).findById(TEST_ID);
        verify(repostory, never()).deleteById(TEST_ID);
    }
}
