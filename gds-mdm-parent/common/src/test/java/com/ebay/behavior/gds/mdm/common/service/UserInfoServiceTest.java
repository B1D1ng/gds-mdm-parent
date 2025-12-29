package com.ebay.behavior.gds.mdm.common.service;

import com.ebay.platform.security.sso.core.RoleAuthorizer;
import com.ebay.platform.security.sso.domain.UserSession;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.ForbiddenException;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.ebay.behavior.gds.mdm.common.util.SiteSsoConstants.ROLE_CJS_ADMIN;
import static com.ebay.behavior.gds.mdm.common.util.SiteSsoConstants.ROLE_GOVERNANCE_APPROVER;
import static com.ebay.behavior.gds.mdm.common.util.SiteSsoConstants.SITE_SSO_APP_NAME;
import static com.ebay.behavior.gds.mdm.common.util.SiteSsoConstants.SITE_SSO_SCOPE_NAME;
import static com.ebay.behavior.gds.mdm.common.util.SiteSsoConstants.SITE_SSO_SESSION;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserInfoServiceTest {

    @InjectMocks
    private UserInfoService userInfoService;

    @Mock
    private UserSession userSession;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        Mockito.reset(userSession);
    }

    @Test
    void getUserName_returnsUser() {
        when(userSession.getUser()).thenReturn("testUser");
        String result = userInfoService.getUser(userSession);
        assertThat(result).isEqualTo("testUser");
    }

    @Test
    void getAuthorities_returnsGrantedAuthorities() {
        List<String> roles = new ArrayList<>();
        roles.add("cjs-admin");
        roles.add("tracking-moderator");

        List<SimpleGrantedAuthority> authorities = userInfoService.getAuthorities(roles);
        assertThat(authorities)
                .asInstanceOf(InstanceOfAssertFactories.list(SimpleGrantedAuthority.class))
                .hasSize(2)
                .containsExactlyInAnyOrder(
                        new SimpleGrantedAuthority("ROLE_CJS-ADMIN"),
                        new SimpleGrantedAuthority("ROLE_TRACKING-MODERATOR")
                );
    }

    @Test
    void getUserRoles_validClaims_returnsRoles() {
        Map<String, Object> claims = new HashMap<>();
        Map<String, Object> permsMap = new HashMap<>();
        List<Map<String, Object>> scopeList = new ArrayList<>();
        Map<String, Object> appEntry = new HashMap<>();

        appEntry.put(RoleAuthorizer.APPLICATION, SITE_SSO_APP_NAME);
        appEntry.put(RoleAuthorizer.ROLES, Arrays.asList("governance-approver", "cjs-admin", "tracking-moderator"));

        scopeList.add(appEntry);
        permsMap.put(SITE_SSO_SCOPE_NAME, scopeList);

        claims.put(RoleAuthorizer.PERMS, permsMap);

        when(userSession.getClaims()).thenReturn(claims);

        List<String> result = userInfoService.getUserRoles(userSession);

        assertThat(result)
                .asInstanceOf(InstanceOfAssertFactories.list(String.class))
                .hasSize(3)
                .containsExactlyInAnyOrder("governance-approver", "cjs-admin", "tracking-moderator");
    }

    @Test
    void getUserRoles_noRoles_returnsEmptyList() {
        // Setup claims where app entry exists, but no roles assigned
        Map<String, Object> claims = new HashMap<>();
        Map<String, Object> permsMap = new HashMap<>();
        List<Map<String, Object>> scopeList = new ArrayList<>();
        Map<String, Object> appEntry = new HashMap<>();

        appEntry.put(RoleAuthorizer.APPLICATION, SITE_SSO_APP_NAME);
        appEntry.put(RoleAuthorizer.ROLES, Collections.emptyList()); // No roles

        scopeList.add(appEntry);
        permsMap.put(SITE_SSO_SCOPE_NAME, scopeList);
        claims.put(RoleAuthorizer.PERMS, permsMap);

        when(userSession.getClaims()).thenReturn(claims);

        List<String> result = userInfoService.getUserRoles(userSession);

        assertThat(result)
                .asInstanceOf(InstanceOfAssertFactories.list(String.class))
                .isNotNull()
                .isEmpty();
    }


    @Test
    void getUserRoles_invalidClaims_throwsException() {
        Map<String, Object> invalidClaims = new HashMap<>();
        invalidClaims.put("key", "value");

        when(userSession.getClaims()).thenReturn(invalidClaims);

        assertThatThrownBy(() -> userInfoService.getUserRoles(userSession))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unexpected format for user permissions");
    }

    @Test
    void allowedIdmRoles_rolesPresent_doesNotThrowException() {
        Map<String, Object> claims = new HashMap<>();
        Map<String, Object> permsMap = new HashMap<>();
        List<Map<String, Object>> scopeList = new ArrayList<>();
        Map<String, Object> appEntry = new HashMap<>();

        appEntry.put(RoleAuthorizer.APPLICATION, SITE_SSO_APP_NAME);
        appEntry.put(RoleAuthorizer.ROLES, Arrays.asList(ROLE_GOVERNANCE_APPROVER, ROLE_CJS_ADMIN));

        scopeList.add(appEntry);
        permsMap.put(SITE_SSO_SCOPE_NAME, scopeList);

        claims.put(RoleAuthorizer.PERMS, permsMap);
        ReflectionTestUtils.setField(userInfoService, "sitessoEnabled", true);
        when(userSession.getClaims()).thenReturn(claims);
        when(request.getAttribute(SITE_SSO_SESSION)).thenReturn(userSession);

        userInfoService.allowedIdmRoles(request);
        userInfoService.allowedIdmRoles(request, ROLE_CJS_ADMIN);
        userInfoService.allowedIdmRoles(request, ROLE_CJS_ADMIN, ROLE_GOVERNANCE_APPROVER);
    }

    @Test
    void allowedIdmRoles_roleMissing_doesThrowException() {
        Map<String, Object> claims = new HashMap<>();
        Map<String, Object> permsMap = new HashMap<>();
        List<Map<String, Object>> scopeList = new ArrayList<>();
        Map<String, Object> appEntry = new HashMap<>();

        appEntry.put(RoleAuthorizer.APPLICATION, SITE_SSO_APP_NAME);
        appEntry.put(RoleAuthorizer.ROLES, Arrays.asList(ROLE_GOVERNANCE_APPROVER));

        scopeList.add(appEntry);
        permsMap.put(SITE_SSO_SCOPE_NAME, scopeList);

        claims.put(RoleAuthorizer.PERMS, permsMap);
        ReflectionTestUtils.setField(userInfoService, "sitessoEnabled", true);
        when(userSession.getClaims()).thenReturn(claims);
        when(request.getAttribute(SITE_SSO_SESSION)).thenReturn(userSession);

        assertThatThrownBy(() -> userInfoService.allowedIdmRoles(request, ROLE_CJS_ADMIN))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("missing required role");
    }

    @Test
    void allowedIdmRoles_sitessoDisabled_doesNotThrowException() {
        ReflectionTestUtils.setField(userInfoService, "sitessoEnabled", false);
        assertThatThrownBy(() -> userInfoService.allowedIdmRoles(request, ROLE_CJS_ADMIN))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SSO is not enabled, role check is not applicable");
    }
}
