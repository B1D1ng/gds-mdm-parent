package com.ebay.behavior.gds.mdm.signal.resource;

import com.ebay.behavior.gds.mdm.common.model.PlatformType;
import com.ebay.behavior.gds.mdm.common.model.external.infohub.Project;
import com.ebay.behavior.gds.mdm.common.model.external.infohub.User;
import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.cos.raptor.error.v3.ErrorMessageV3;

import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpec;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.LOOKUP;
import static org.assertj.core.api.Assertions.assertThat;

class InfohubResourceIT extends AbstractResourceTest {
    @BeforeEach
    void setUp() {
        url = getBaseUrl() + V1 + LOOKUP;
    }

    @Test
    void getAllUsers() {
        var users = requestSpec()
                .when().get(url + "/users?prefix=akaskur")
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getList(".", User.class);

        assertThat(users).isNotEmpty();
        val user = users.get(0);
        assertThat(user.firstName()).isEqualTo("Aparna");
        assertThat(user.lastName()).isEqualTo("Kaskurthy");
        assertThat(user.email()).isEqualTo("akaskurthy@ebay.com");
        assertThat(user.loginName()).isEqualTo("akaskurthy");
    }

    @Test
    void getAllUsers_tooSmallPrefix_400() {
        var errorMessage = requestSpec().when().get(url + "/users?prefix=a")
                .then().statusCode(HttpStatus.BAD_REQUEST.value())
                .extract().body().jsonPath().getObject(".", ErrorMessageV3.class);
        assertThat(errorMessage.getErrors().get(0).getMessage())
                .isEqualTo("KeyPrefix length should be at least 2 chars");
    }

    @Test
    void getAllProjects_notValidUser_400() {
        var errorMessage = requestSpec().when().get(url + "/users?prefix=mmmm")
                .then().statusCode(HttpStatus.BAD_REQUEST.value())
                .extract().body().jsonPath().getObject(".", ErrorMessageV3.class);
        assertThat(errorMessage.getErrors().get(0).getMessage())
                .isEqualTo("User id with mmmm not found");
    }

    @Test
    void getAllProjects() {
        var projects = requestSpec().when().get(url + "/projects?prefix=CJS")
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getList(".", Project.class);

        val project = projects.get(0);
        assertThat(project.projectKey()).isEqualTo(PlatformType.CJS.getValue());
    }

    @Test
    void getAllProjects_tooSmallPrefix_400() {
        var errorMessage = requestSpec().when().get(url + "/projects?prefix=T")
                .then().statusCode(HttpStatus.BAD_REQUEST.value())
                .extract().body().jsonPath().getObject(".", ErrorMessageV3.class);
        assertThat(errorMessage.getErrors().get(0).getMessage())
                .isEqualTo("KeyPrefix length should be at least 2 chars");
    }

    @Test
    void getAllProjects_notValidProject_400() {
        var errorMessage = requestSpec().when().get(url + "/projects?prefix=mmmm")
                .then().statusCode(HttpStatus.BAD_REQUEST.value())
                .extract().body().jsonPath().getObject(".", ErrorMessageV3.class);
        assertThat(errorMessage.getErrors().get(0).getMessage())
                .isEqualTo("Project name with mmmm not found");
    }

    @Test
    void getProjectByKey() {
        var project = requestSpec().when().get(url + "/projects/CJS")
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", Project.class);

        assertThat(project.projectKey()).isEqualTo(PlatformType.CJS.getValue());
    }

    @Test
    void getProjectByKey_invalidProject_400() {
        requestSpec().when().get(url + "/projects/TRT")
                .then().statusCode(HttpStatus.BAD_REQUEST.value());
    }
}
