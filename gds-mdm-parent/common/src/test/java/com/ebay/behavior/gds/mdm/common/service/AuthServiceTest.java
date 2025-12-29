package com.ebay.behavior.gds.mdm.common.service;

import com.ebay.behavior.gds.mdm.common.model.external.muse.Data;
import com.ebay.behavior.gds.mdm.common.model.external.muse.UserDetails;

import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static com.ebay.behavior.gds.mdm.common.service.AuthService.MUSE_APP_HEADER;
import static com.ebay.behavior.gds.mdm.common.service.AuthService.VALIDATE_API_PATH;
import static jakarta.ws.rs.core.HttpHeaders.ACCEPT;
import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Spy
    @InjectMocks
    AuthService service;

    @Mock
    private WebTarget target;

    @Mock
    private Invocation.Builder builder;

    @Test
    void getUser_validToken() {
        var userName = RandomStringUtils.randomAlphanumeric(8);
        var token = "MSID_ghyjPnICEZxt05mgbRSi5kWYlhGfH748Sg4vl6IcPIIdSodT";

        when(target.path(VALIDATE_API_PATH)).thenReturn(target);
        when(target.request()).thenReturn(builder);
        when(builder.header(AUTHORIZATION, token)).thenReturn(builder);
        when(builder.header(MUSE_APP_HEADER, "cjsportal,gdsgovtool")).thenReturn(builder);
        when(builder.header(ACCEPT, APPLICATION_JSON_VALUE)).thenReturn(builder);
        when(builder.get(UserDetails.class)).thenReturn(new UserDetails(HttpStatus.OK.toString(), new Data(userName)));

        var user = service.getUser(token);

        assertThat(user).isEqualTo(userName);
    }
}