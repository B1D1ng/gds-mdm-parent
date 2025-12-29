package com.ebay.behavior.gds.mdm.signal.resource;

import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.signal.common.model.FieldGroup;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedSignal;
import com.ebay.behavior.gds.mdm.signal.service.PlanService;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedFieldGroupService;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedFieldService;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedSignalService;
import com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.search.Search.TAG;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedField;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedSignal;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomString;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpec;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpecWithBody;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.DEFINITION;
import static org.assertj.core.api.Assertions.assertThat;

class UnstagedFieldGroupResourceIT extends AbstractResourceTest {

    private UnstagedSignal signal;

    @Autowired
    private UnstagedSignalService signalService;

    @Autowired
    private UnstagedFieldService fieldService;

    @Autowired
    private UnstagedFieldGroupService service;

    @Autowired
    private PlanService planService;

    @BeforeEach
    void setUp() {
        var plan = TestModelUtils.plan();
        var planId = planService.create(plan).getId();

        url = getBaseUrl() + V1 + DEFINITION + "/signal";
        signal = unstagedSignal(planId);
    }

    @Test
    void update() {
        var signalId = signalService.create(signal).getSignalId();
        var field = unstagedField(signalId);
        field = fieldService.create(field, Set.of());

        var expression = getRandomString();
        var group = service.getByKey(signalId, field.getGroupKey());
        group.setExpression(expression);

        requestSpecWithBody(group)
                .when().patch(url + '/' + signalId.getId() + "/field-group")
                .then().statusCode(HttpStatus.OK.value());
    }

    @Test
    void delete_ByTag() {
        var signalId = signalService.create(signal).getSignalId();
        var field = unstagedField(signalId);
        field = fieldService.create(field, Set.of());
        var tag = field.getTag();
        service.getByTag(signalId, tag);

        requestSpec()
                .queryParam(TAG, tag)
                .when().delete(url + '/' + signalId.getId() + "/field-group")
                .then().statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    void delete_byKey() {
        var signalId = signalService.create(signal).getSignalId();
        var field = unstagedField(signalId);
        field = fieldService.create(field, Set.of());
        var key = field.getGroupKey();
        service.getByKey(signalId, key);

        requestSpec()
                .queryParam("key", key)
                .when().delete(url + '/' + signalId.getId() + "/field-group")
                .then().statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    void getFieldGroups() {
        var signalId = signalService.create(signal).getSignalId();
        var field = unstagedField(signalId);
        fieldService.create(field, Set.of());

        var fields = requestSpec()
                .when().get(url + '/' + signalId.getId() + "/field-group")
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getList(".", FieldGroup.class);

        assertThat(fields).isNotEmpty();
    }
}