package com.ebay.behavior.gds.mdm.common.resource;

import com.ebay.behavior.gds.mdm.common.model.UserInfo;
import com.ebay.behavior.gds.mdm.common.service.InfohubService;
import com.ebay.behavior.gds.mdm.common.service.UserInfoService;
import com.ebay.behavior.gds.mdm.common.util.ResourceUtils;
import com.ebay.platform.security.sso.domain.UserSession;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

import static com.ebay.behavior.gds.mdm.common.util.SiteSsoConstants.SITE_SSO_SESSION;

@Slf4j
@Path("/")
@RestController
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ConditionalOnProperty(prefix = "sitesso", name = "enable", havingValue = "true", matchIfMissing = false)
public class UserInfoResource {
    @Autowired
    private UserInfoService service;

    @Autowired
    private InfohubService infohubService;

    /**
     * Retrieves user information and sets authentication context.
     *
     * @param request The HTTP request containing session attributes.
     * @return Populated UserInfo object.
     */
    @GET
    @Path("/user-info")
    public UserInfo getUserInfo(@Context HttpServletRequest request) {
        val userInfo = new UserInfo();
        val session = (UserSession) request.getAttribute(SITE_SSO_SESSION);
        val user = service.getUser(session);
        val userRoles = service.getUserRoles(session);
        val authorities = service.getAuthorities(userRoles);

        val maybeInfohubUser = infohubService.findUser(user, infohubService.readAllUsers());
        if (maybeInfohubUser.isPresent()) {
            userInfo.setFirstName(maybeInfohubUser.get().firstName());
            userInfo.setLastName(maybeInfohubUser.get().lastName());
        }

        userInfo.setUsername(user);
        ResourceUtils.setRequestUser(user);
        userInfo.setRoles(userRoles);
        val auth = new UsernamePasswordAuthenticationToken(user, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);

        log.info("User {} with roles {} is authenticated", user, userRoles);
        return userInfo;
    }
}