package com.ebay.behavior.gds.mdm.common.service;

import com.ebay.platform.security.sso.core.RoleAuthorizer;
import com.ebay.platform.security.sso.domain.UserSession;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.ForbiddenException;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang.ArrayUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.ebay.behavior.gds.mdm.common.util.SiteSsoConstants.SITE_SSO_APP_NAME;
import static com.ebay.behavior.gds.mdm.common.util.SiteSsoConstants.SITE_SSO_ROLE_PREFIX;
import static com.ebay.behavior.gds.mdm.common.util.SiteSsoConstants.SITE_SSO_SCOPE_NAME;
import static com.ebay.behavior.gds.mdm.common.util.SiteSsoConstants.SITE_SSO_SESSION;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.UNCHECKED;

@Slf4j
@Service
@Validated
public class UserInfoService {

    @Value("${sitesso.enable:false}")
    private boolean sitessoEnabled;

    public String getUser(@NotNull UserSession session) {
        return session.getUser();
    }

    /**
     * Extracts the list of roles from the provided claims map.
     *
     * @param session An object containing user claims, expected to include role-related permissions.
     * @return A list of roles associated with the given claims, or an empty list if no roles are found.
     */
    @SuppressWarnings(UNCHECKED)
    public List<String> getUserRoles(@NotNull UserSession session) {
        if (session.getClaims() == null) {
            log.error("Claims are null for user: {}", session);
            throw new IllegalStateException("User claims cannot be null");
        }

        val claims = session.getClaims();
        Object permsRaw = claims.get(RoleAuthorizer.PERMS);

        if (!(permsRaw instanceof Map)) {
            throw new IllegalStateException("Unexpected format for user permissions");
        }

        Map<String, List<Map<String, Object>>> perms = (Map<String, List<Map<String, Object>>>) permsRaw;
        List<Map<String, Object>> scopeList = perms.get(SITE_SSO_SCOPE_NAME);

        if (scopeList == null) {
            throw new IllegalStateException("Scope " + SITE_SSO_SCOPE_NAME + " not found in user permissions");
        }

        return scopeList.stream()
                .filter(item -> SITE_SSO_APP_NAME.equalsIgnoreCase((String) item.get(RoleAuthorizer.APPLICATION)))
                .flatMap(item -> ((List<String>) item.get(RoleAuthorizer.ROLES)).stream())
                .collect(Collectors.toList());
    }

    public List<SimpleGrantedAuthority> getAuthorities(@NotNull List<String> userRoles) {
        return userRoles.stream()
                .filter(Objects::nonNull)
                .map(role -> new SimpleGrantedAuthority(SITE_SSO_ROLE_PREFIX + role.toUpperCase(Locale.ENGLISH)))
                .collect(Collectors.toList());
    }

    public void allowedIdmRoles(@NotNull HttpServletRequest request, String... allowedUserRoles) {
        if (!sitessoEnabled) {
            throw new IllegalStateException("SSO is not enabled, role check is not applicable");
        }

        if (!ArrayUtils.isNotEmpty(allowedUserRoles)) {
            return;
        }

        val session = (UserSession) request.getAttribute(SITE_SSO_SESSION);
        List<String> userRoles = getUserRoles(session);
        for (val userRole : allowedUserRoles) {
            if (!userRoles.contains(userRole)) {
                throw new ForbiddenException(
                        String.format("Access denied for user [%s]: missing required role [%s]. User roles: %s",
                                session.getUser(), userRole, userRoles)
                );
            }
        }
    }
}
