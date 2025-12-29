package com.ebay.behavior.gds.mdm.signal.resource;

import com.ebay.behavior.gds.mdm.common.model.external.infohub.Project;
import com.ebay.behavior.gds.mdm.common.service.InfohubService;
import com.ebay.cos.raptor.error.v3.ErrorMessageV3;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.val;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.LOOKUP;
import static com.ebay.behavior.gds.mdm.signal.util.ValidationUtils.validateSearchPrefix;

@Validated
@Path(V1 + LOOKUP)
@RestController
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class InfohubResource {

    public static final String KEY = "key";

    @Autowired
    private InfohubService infohubService;

    @GET
    @Path("/users")
    @Operation(summary = "Search users")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get all matching users",
                    content = {@Content(array = @ArraySchema(schema = @Schema(implementation = Project.class)))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})})
    public Response getAllUsers(@QueryParam("prefix") String userPrefix) {
        validateSearchPrefix(userPrefix);

        val users = infohubService.readAllUsers();
        val filtered = infohubService.filterUsers(userPrefix, users);

        if (CollectionUtils.isEmpty(filtered)) {
            throw new BadRequestException(String.format("User id with %s not found", userPrefix));
        }

        return Response.status(Response.Status.OK).entity(filtered).build();
    }

    @GET
    @Path("/projects")
    @Operation(summary = "Search projects")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get all matching projects",
                    content = {@Content(array = @ArraySchema(schema = @Schema(implementation = Project.class)))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})})
    public Response getAllProjects(@QueryParam("prefix") String projectPrefix) {
        validateSearchPrefix(projectPrefix);

        val projects = infohubService.readAllProjects();
        val filtered = infohubService.filterProjects(projectPrefix, projects);

        if (CollectionUtils.isEmpty(filtered)) {
            throw new BadRequestException(String.format("Project name with %s not found", projectPrefix));
        }

        return Response.status(Response.Status.OK).entity(filtered).build();
    }

    @GET
    @Path("/projects/{key}")
    @Operation(summary = "Get project by key")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get project by key",
                    content = {@Content(schema = @Schema(implementation = Project.class))}),
            @ApiResponse(responseCode = "400", description = "Project not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})})
    public Response getProjectByKey(@PathParam(KEY) String projectKey) {
        val project = infohubService.getProjectByKey(projectKey);
        if (Objects.isNull(project)) {
            throw new BadRequestException("Project not found");
        }
        return Response.status(Response.Status.OK).entity(project).build();
    }
}
