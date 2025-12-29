package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedSignal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedSignal;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class UnstagedSignalServiceTest {

    @Spy
    private UnstagedSignalService service;

    private UnstagedSignal signal;

    @BeforeEach
    void setUp() {
        signal = unstagedSignal(1L);
    }

    @Test
    void create_idNotNullButVersionIsNull_error() {
        signal.setId(1L);
        signal.setVersion(null);

        assertThatThrownBy(() -> service.create(signal))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Version must not be null");
    }
}