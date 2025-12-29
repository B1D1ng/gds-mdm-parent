package com.ebay.behavior.gds.mdm.dec.resource;

import com.ebay.behavior.gds.mdm.common.exception.ExternalCallException;
import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.dec.model.LdmBaseEntity;
import com.ebay.behavior.gds.mdm.dec.model.LdmEntity;
import com.ebay.behavior.gds.mdm.dec.model.dto.LdmBootstrapRequest;
import com.ebay.behavior.gds.mdm.dec.model.dto.LdmBootstrapResponse;
import com.ebay.behavior.gds.mdm.dec.model.enums.ViewType;
import com.ebay.behavior.gds.mdm.dec.service.DecCompilerClient;
import com.ebay.behavior.gds.mdm.dec.service.LdmBaseEntityService;
import com.ebay.behavior.gds.mdm.dec.service.LdmEntityService;
import com.ebay.behavior.gds.mdm.dec.service.NamespaceService;
import com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils;
import com.ebay.cos.raptor.error.v3.ErrorMessageV3;

import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestUtils.requestSpecWithBody;
import static com.ebay.behavior.gds.mdm.dec.util.ApiConstants.LDM_BASE_ENTITY_METADATA_API;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

class LdmBaseEntityActionResourceIT extends AbstractResourceTest {

    @MockitoBean
    private DecCompilerClient decCompilerClient;

    @Autowired
    private NamespaceService namespaceService;

    @Autowired
    private LdmBaseEntityService ldmBaseEntityService;

    @Autowired
    private LdmEntityService ldmEntityService;

    private LdmBaseEntity baseEntity;
    private LdmEntity rawView;
    private LdmEntity rawView2;
    private LdmEntity snapshotView;

    @BeforeEach
    void setUp() {
        url = getBaseUrl() + V1 + LDM_BASE_ENTITY_METADATA_API;

        var namespace = TestModelUtils.namespace();
        namespace = namespaceService.create(namespace);

        val baseLdm = TestModelUtils.ldmBaseEntity(namespace.getId());

        this.baseEntity = ldmBaseEntityService.create(baseLdm);

        this.rawView = TestModelUtils.ldmEntity(null, null, "Item - RAW", ViewType.RAW, this.baseEntity.getNamespaceId(), this.baseEntity.getId());
        this.rawView2 = TestModelUtils.ldmEntity(null, null, "Item - RAW - Another", ViewType.RAW, this.baseEntity.getNamespaceId(), this.baseEntity.getId());
        this.snapshotView = TestModelUtils.ldmEntity(null, null, "Item - SNAPSHOT", ViewType.SNAPSHOT, this.baseEntity.getNamespaceId(),
                                                     this.baseEntity.getId());

        this.rawView = ldmEntityService.create(this.rawView);
        this.rawView2 = ldmEntityService.create(this.rawView2);
        this.snapshotView = ldmEntityService.create(this.snapshotView);
    }

    @Test
    void bootstrap_whenBothViewsAreValid_shouldSucceed() {
        when(decCompilerClient.post(eq("/bootstrap"),
                                    eq(null),
                                    any(),
                                    eq(LdmBootstrapResponse.class)))
                .thenReturn(new LdmBootstrapResponse(true));

        val requestBody = new LdmBootstrapRequest(rawView.getId(), snapshotView.getId());
        var response = requestSpecWithBody(requestBody)
                .when().post(url + '/' + baseEntity.getId() + "/action/bootstrap")
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", LdmBootstrapResponse.class);

        assertThat(response.success()).isEqualTo(Boolean.TRUE);
    }

    @Test
    void bootstrap_whenSnapshotViewIsInvalid_shouldThrowIllegalArgumentException() {
        when(decCompilerClient.post(eq("/bootstrap"),
                                    eq(null),
                                    any(),
                                    eq(LdmBootstrapResponse.class)))
                .thenReturn(new LdmBootstrapResponse(true));

        val requestBody = new LdmBootstrapRequest(rawView.getId(), rawView2.getId());
        var response = requestSpecWithBody(requestBody)
                .when().post(url + '/' + baseEntity.getId() + "/action/bootstrap")
                .then().statusCode(BAD_REQUEST.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", ErrorMessageV3.class);

        assertThat(response.getErrors()).isNotEmpty();
    }

    @Test
    void bootstrap_whenExternalServiceError_shouldThrowExternalCallException() {
        when(decCompilerClient.post(eq("/bootstrap"),
                                    eq(null),
                                    any(),
                                    eq(LdmBootstrapResponse.class)))
                .thenThrow(ExternalCallException.class);

        val requestBody = new LdmBootstrapRequest(rawView.getId(), snapshotView.getId());
        var response = requestSpecWithBody(requestBody)
                .when().post(url + '/' + baseEntity.getId() + "/action/bootstrap")
                .then().statusCode(INTERNAL_SERVER_ERROR.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", ErrorMessageV3.class);

        assertThat(response.getErrors()).isNotEmpty();
    }
}