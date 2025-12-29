package com.ebay.behavior.gds.mdm.common.service;

import com.ebay.behavior.gds.mdm.common.exception.ExternalCallException;
import com.ebay.behavior.gds.mdm.common.model.external.infohub.ProjectById;
import com.ebay.behavior.gds.mdm.common.model.external.infohub.ProjectView;
import com.ebay.behavior.gds.mdm.common.model.external.infohub.Users;
import com.ebay.behavior.gds.mdm.common.service.token.EsAuthTokenGenerator;

import jakarta.ws.rs.client.WebTarget;
import lombok.val;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

@Disabled
@ExtendWith(MockitoExtension.class)
class InfohubServiceTest {

    @Mock
    private ProjectView projectView;

    @Mock
    private Users users;

    @Mock
    private EsAuthTokenGenerator tokenGenerator;

    @Mock
    private WebTarget target;

    @Spy
    @InjectMocks
    private InfohubService service;

    @Test
    void readAllProjects_errorResponse_error() {
        doThrow(new ExternalCallException("test error")).when(service).get(InfohubService.PROJECTS_PATH, ProjectView.class);

        assertThatThrownBy(() -> service.readAllProjects())
                .isInstanceOf(ExternalCallException.class);
    }

    @Test
    void readAllProjects_nullInResponse_emptyList() {
        doReturn(projectView).when(service).get(InfohubService.PROJECTS_PATH, ProjectView.class);

        var objects = service.readAllProjects();

        assertThat(objects).isEmpty();
    }

    @Test
    void getProjectByKey() {
        val key = "key";
        val path = "/project/" + key + "/view.service";
        doThrow(new ExternalCallException("test error")).when(service).get(path, ProjectById.class);

        assertThatThrownBy(() -> service.getProjectByKey(key))
                .isInstanceOf(ExternalCallException.class);
    }

    @Test
    void readAllUsers() {
        doThrow(new ExternalCallException("test error")).when(service).get(InfohubService.USERS_PATH, Users.class);

        assertThatThrownBy(() -> service.readAllUsers())
                .isInstanceOf(ExternalCallException.class);
    }


    @Test
    void readAllUsers_nullInResponse_emptyList() {
        doReturn(users).when(service).get(InfohubService.USERS_PATH, Users.class);

        var objects = service.readAllUsers();

        assertThat(objects).isEmpty();
    }
}