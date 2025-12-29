package com.ebay.behavior.gds.mdm.common.util;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.exception.ExternalCallException;
import com.ebay.behavior.gds.mdm.common.model.Model;
import com.ebay.behavior.gds.mdm.common.model.VersionedModel;
import com.ebay.behavior.gds.mdm.common.model.WithId;
import com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion;
import com.ebay.cos.raptor.error.v3.ErrorMessageV3;
import com.ebay.platform.security.sso.domain.UserSession;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.ebay.behavior.gds.mdm.common.model.Model.COMMA;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.SEARCH_BY;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.SEARCH_CRITERION;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.SEARCH_TERM;
import static com.ebay.behavior.gds.mdm.common.util.SiteSsoConstants.SITE_SSO_SESSION;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.UNCHECKED;
import static java.util.stream.Collectors.toSet;
import static org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST;

@Slf4j
@UtilityClass
public class ResourceUtils {

    public static final String V1 = "/v1";
    public static final String APP_PATH = "/cjs/mdm"; // TODO will be renamed to "/gds/mdm" soon, but must match current UI configuration
    public static final String REQUEST_USER = "user";
    public static final String UNKNOWN = "unknown";

    public static Response created(UriInfo uriInfo, WithId model) {
        return created(uriInfo, model, model.getId());
    }

    public static Response created(UriInfo uriInfo, String id) {
        val uriBuilder = uriInfo.getAbsolutePathBuilder();
        uriBuilder.path(id);
        return Response.created(uriBuilder.build()).entity(id).build();
    }

    public static Response created(UriInfo uriInfo, Object model, Long id) {
        val uriBuilder = uriInfo.getAbsolutePathBuilder();
        uriBuilder.path(id.toString());
        return Response.created(uriBuilder.build()).entity(model).build();
    }

    /**
     * In order to create response links (to next/prev page) we need to pass original request parameters.
     * This method creates a map out of parameters.
     *
     * @return A map with parameter names/values.
     */
    public static Map<String, String> getQueryParams(String searchBy, String searchTerm, SearchCriterion searchCriterion) {
        Validate.notBlank(searchBy, "searchBy is blank");
        Validate.notBlank(searchTerm, "searchTerm is blank");
        Validate.notNull(searchCriterion, "searchCriterion is null");

        val tupleStream = Stream.of(
                        Tuple.of(SEARCH_BY, searchBy),
                        Tuple.of(SEARCH_TERM, searchTerm),
                        Tuple.of(SEARCH_CRITERION, searchCriterion.getValue()))
                .filter(pair -> Objects.nonNull(pair._2));
        return getParamsMap(tupleStream);
    }

    public static Map<String, String> getQueryParams(String searchTerm) {
        Validate.notBlank(searchTerm, "searchTerm is blank");

        val tupleStream = Stream.of(Tuple.of(SEARCH_TERM, searchTerm));
        return getParamsMap(tupleStream);
    }

    public static Map<String, String> getParamsMap(Stream<Tuple2<String, String>> tupleStream) {
        return tupleStream
                .filter(pair -> Objects.nonNull(pair._2))
                .collect(Collectors.toUnmodifiableMap(Tuple2::_1, Tuple2::_2));
    }

    public static String getRequestUser() {
        val attributes = RequestContextHolder.getRequestAttributes();
        return Optional.ofNullable(attributes)
                .map(attr -> (String) attr.getAttribute(REQUEST_USER, SCOPE_REQUEST))
                .orElse(getRequestUserFromUserSession());
    }

    public static String getRequestUserFromUserSession() {
        return Optional.ofNullable(getCurrentHttpRequest())
                .map(req -> (UserSession) req.getAttribute(SITE_SSO_SESSION))
                .map(UserSession::getUser)
                .orElse(UNKNOWN);
    }

    public static HttpServletRequest getCurrentHttpRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    public static void setRequestUser(String user) {
        val attributes = RequestContextHolder.getRequestAttributes();
        if (Objects.isNull(attributes)) {
            throw new IllegalStateException("Request attributes are not set. Probably this coe runs outside of a current request thread.");
        }
        RequestContextHolder.getRequestAttributes().setAttribute(REQUEST_USER, user, SCOPE_REQUEST);
    }

    public static void validateUpdateRequestId(Model entity, long id) {
        Validate.isTrue(entity.getId().equals(id), "Entity id and path id do not match");
    }

    public static void validateVersionedUpdateRequest(VersionedModel entity, long id, int version) {
        validateUpdateRequestId(entity, id);
        Validate.isTrue(entity.getVersion().equals(version), "Entity version and path version do not match");
    }

    @SuppressWarnings("checkstyle.MethodTypeParameterName")
    public static <R> R handleResponse(Response clientResponse, Class<R> type, String path, ObjectMapper objectMapper, String host) {
        val statusCode = clientResponse.getStatus();

        if (statusCode < 200 || statusCode > 299) {
            val message = String.format("Got unexpected %d response from %s for path %s", statusCode, host, path);
            log.error(message);

            val maybeErrors = toErrorsV3(clientResponse);
            val errors = maybeErrors.map(ErrorMessageV3::getErrors).stream()
                    .flatMap(List::stream)
                    .map(errorData -> {
                        String msg = errorData.getMessage();
                        if (StringUtils.isBlank(msg)) {
                            msg = errorData.getLongMessage();
                        }
                        if (StringUtils.isBlank(msg)) {
                            msg = errorData.toString();
                        }
                        return msg;
                    })
                    .collect(Collectors.toList());

            errors.add(0, message);

            val is417 = contains(errors, "417");
            val is404 = contains(errors, "404");

            if (is417 || is404) {
                throw new DataNotFoundException(String.join(",\t ", errors));
            }

            throw new ExternalCallException(statusCode, String.join(",\t ", errors));
        }

        val response = readEntity(clientResponse, type, objectMapper);

        if (Objects.isNull(response) && statusCode != 204) {
            val message = String.format("Got null response from %s for path %s", host, path);
            log.error(message);
            throw new ExternalCallException(statusCode, message);
        }

        return response;
    }

    private boolean contains(List<String> errors, String value) {
        return errors.stream()
                .map(error -> StringUtils.contains(error, value))
                .filter(BooleanUtils::isTrue)
                .findFirst().orElse(false);
    }

    private Optional<ErrorMessageV3> toErrorsV3(Response response) {
        try {
            return Optional.ofNullable(response.readEntity(ErrorMessageV3.class));
        } catch (Exception ex) {
            log.warn(String.format("Cannot read response as ErrorMessageV3. Error is: %s", ex.getMessage()));
            return Optional.empty();
        }
    }

    @SneakyThrows
    @SuppressWarnings(UNCHECKED)
    private <T> T readEntity(Response response, Class<T> type, ObjectMapper objectMapper) {
        val json = response.readEntity(String.class);
        if (Objects.isNull(json)) {
            return null;
        }

        if (String.class.equals(type)) {
            return (T) json;
        }

        return objectMapper.readValue(json, type);
    }

    public static Set<String> csvStringToSet(String str) {
        if (StringUtils.isBlank(str)) {
            return Set.of();
        }

        return Arrays.stream(str.split(COMMA))
                .map(StringUtils::trim)
                .collect(toSet());
    }

    public static String getHostName() {
        try {
            val inetAddress = InetAddress.getLocalHost();
            return inetAddress.getHostName();
        } catch (Exception ex) {
            return UNKNOWN;
        }
    }
}
