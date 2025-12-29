package com.ebay.behavior.gds.mdm.contract.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.contract.model.Filter;
import com.ebay.behavior.gds.mdm.contract.repository.FilterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils.filter;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FilterServiceTest {

    @InjectMocks
    private FilterService service;

    @Mock
    private FilterRepository repository;

    private Filter filter;
    private final static Long TEST_ID = 1L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        filter = filter(getRandomString()).toBuilder().id(TEST_ID).revision(0).build();
    }

    @Test
    void test_create() {
        var newFilter = filter(getRandomString());
        var savedFilter = newFilter.toBuilder().id(TEST_ID).build();

        when(repository.save(newFilter)).thenReturn(savedFilter);

        var result = service.create(newFilter);
        assertThat(result).isEqualTo(savedFilter);
        verify(repository).save(newFilter);
    }

    @Test
    void test_update() {
        var updateFilter = filter.toBuilder().statement(getRandomString()).revision(1).build();

        when(repository.findById(TEST_ID)).thenReturn(Optional.of(filter));
        when(repository.save(updateFilter)).thenReturn(updateFilter);
        var result = service.update(updateFilter);

        verify(repository).findById(TEST_ID);
        verify(repository).save(updateFilter);
        assertThat(result.getStatement()).isEqualTo(updateFilter.getStatement());
        assertThat(result.getRevision()).isEqualTo(1);
        assertThat(result).isEqualTo(updateFilter);
    }

    @Test
    void test_getById() {
        when(repository.findById(TEST_ID)).thenReturn(Optional.of(filter));
        var result = service.getById(TEST_ID);
        assertThat(result).isEqualTo(filter);
        verify(repository).findById(TEST_ID);
    }

    @Test
    void test_getById_notFound() {
        when(repository.findById(TEST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(TEST_ID))
                .isInstanceOf(DataNotFoundException.class);
        verify(repository).findById(TEST_ID);
    }

    @Test
    void test_delete() {
        when(repository.findById(TEST_ID)).thenReturn(Optional.of(filter));
        service.delete(TEST_ID);
        verify(repository).deleteById(TEST_ID);
    }

    @Test
    void test_delete_notFound_withoutException() {
        when(repository.findById(TEST_ID)).thenReturn(Optional.empty());

        service.delete(TEST_ID);

        verify(repository).deleteById(TEST_ID);
    }
}