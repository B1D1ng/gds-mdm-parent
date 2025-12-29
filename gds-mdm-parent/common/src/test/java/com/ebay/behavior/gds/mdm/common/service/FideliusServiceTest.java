package com.ebay.behavior.gds.mdm.common.service;

import com.ebay.security.exceptions.EsamsException;
import com.ebay.security.holders.NonKeyHolder;
import com.ebay.security.nameservice.NameService;

import jakarta.ws.rs.InternalServerErrorException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.ebay.security.nameservice.NameService.VERSION_LAST_ENABLED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FideliusServiceTest {

    private final String path = "testPath";

    @Mock
    private NameService nameService;

    @Mock
    private NonKeyHolder nonKeyHolder;

    @Spy
    @InjectMocks
    private FideliusService service;

    @Test
    void getSecret() throws EsamsException {
        var secret = "testValue";
        when(nameService.getNonKey(path, VERSION_LAST_ENABLED)).thenReturn(nonKeyHolder);
        when(nonKeyHolder.getNonKey()).thenReturn(secret);

        var result = service.getSecret(path);

        assertThat(result).isEqualTo(secret);
    }

    @Test
    void getSecret_esamsException() throws EsamsException {
        var ex = new EsamsException("Test exception");

        when(nameService.getNonKey(path, VERSION_LAST_ENABLED)).thenThrow(ex);

        assertThatThrownBy(() -> service.getSecret(path))
                .isInstanceOf(InternalServerErrorException.class)
                .hasCause(ex);
    }

    @Test
    void getAuthRequest() throws EsamsException {
        var userPath = "userPath";
        var passPath = "passPath";
        var expectedUserSecret = "userValue";
        var expectedPassSecret = "passValue";
        var userHolder = new NonKeyHolder(userPath, VERSION_LAST_ENABLED);
        var passHolder = new NonKeyHolder(passPath, VERSION_LAST_ENABLED);
        userHolder.setNonKey(expectedUserSecret);
        passHolder.setNonKey(expectedPassSecret);

        when(nameService.getNonKey(userPath, VERSION_LAST_ENABLED)).thenReturn(userHolder);
        when(nameService.getNonKey(passPath, VERSION_LAST_ENABLED)).thenReturn(passHolder);

        var result = service.getAuthRequest(userPath, passPath);

        assertThat(result.account()).isEqualTo(expectedUserSecret);
        assertThat(result.appName()).isEqualTo(FideliusService.APP_NAME);
        assertThat(result.password()).isEqualTo(expectedPassSecret);
    }
}