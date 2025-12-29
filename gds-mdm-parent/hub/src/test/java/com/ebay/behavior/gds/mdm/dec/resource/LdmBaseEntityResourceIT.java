package com.ebay.behavior.gds.mdm.dec.resource;

import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.dec.model.LdmBaseEntity;
import com.ebay.behavior.gds.mdm.dec.model.LdmEntity;
import com.ebay.behavior.gds.mdm.dec.model.Namespace;
import com.ebay.behavior.gds.mdm.dec.service.LdmBaseEntityService;
import com.ebay.behavior.gds.mdm.dec.service.LdmEntityService;
import com.ebay.behavior.gds.mdm.dec.service.NamespaceService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils.ldmEntityEmpty;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils.namespace;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestUtils.requestSpec;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestUtils.requestSpecWithBody;
import static com.ebay.behavior.gds.mdm.dec.util.ApiConstants.LDM_BASE_ENTITY_METADATA_API;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

class LdmBaseEntityResourceIT extends AbstractResourceTest {

    @Autowired
    private NamespaceService namespaceService;

    @Autowired
    private LdmEntityService service;

    @Autowired
    private LdmBaseEntityService baseEntityService;

    private LdmEntity entity;

    @BeforeEach
    void setUp() {
        url = getBaseUrl() + V1 + LDM_BASE_ENTITY_METADATA_API;
        Namespace namespace = namespace();
        namespace = namespaceService.create(namespace);
        Long namespaceId = namespace.getId();

        entity = ldmEntityEmpty(namespaceId);
        entity = service.create(entity);
    }

    @Test
    void getEntityById() {
        var baseEntityId = entity.getBaseEntityId();
        var persisted = requestSpec().when().get(url + String.format("/%d", baseEntityId))
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", LdmBaseEntity.class);

        assertThat(persisted.getId()).isNotNull();
        assertThat(persisted.getViews()).isNotEmpty();
    }

    @Test
    void updateEntityById() {
        var baseEntityId = entity.getBaseEntityId();
        var baseEntity = baseEntityService.getById(baseEntityId);
        baseEntity.setOwners("NewOwner");

        var updated = requestSpecWithBody(baseEntity)
                .when().put(url + String.format("/%d", baseEntityId))
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", LdmEntity.class);

        assertThat(updated.getId()).isEqualTo(baseEntityId);
        assertThat(updated.getOwners()).isEqualTo("NewOwner");
    }

    @Test
    void getAllEntities() {
        var saved = requestSpec().when().get(url)
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getList(".", LdmBaseEntity.class);

        assertThat(saved.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void getAllEntities_ByName() {
        var baseEntityId = entity.getBaseEntityId();
        var baseEntity = baseEntityService.getById(baseEntityId);
        var saved = requestSpec().when().get(url + "?name=" + baseEntity.getName())
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getList(".", LdmBaseEntity.class);

        assertThat(saved.size()).isGreaterThanOrEqualTo(1);
    }
}