package com.ebay.behavior.gds.mdm.common.testUtil;

import com.ebay.behavior.gds.mdm.common.util.ResourceUtils;

import lombok.experimental.UtilityClass;
import lombok.val;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Objects;

/**
 * Utility class to mock a RequestContextHolder for testing purposes.
 */
@UtilityClass
public class TestRequestContextUtils {

    /**
     * Sets the user into the request context.
     *
     * @param user A username.
     */
    public void setUser(String user) {
        setRequestAttributes();
        ResourceUtils.setRequestUser(user);
    }

    public void setRequestAttributes() {
        val attributes = RequestContextHolder.getRequestAttributes();
        if (Objects.isNull(attributes)) {
            val request = new MockHttpServletRequest();
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        }
    }
}
