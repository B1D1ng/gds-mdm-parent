package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedField;
import com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils;

import lombok.val;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.ExpressionType.JEXL;
import static com.ebay.behavior.gds.mdm.common.model.ExpressionType.LITERAL;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedEvent;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedField;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedSignal;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.INTEGRATION_TEST;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomSmallString;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles(IT)
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class UnstagedFieldGroupServiceIT {

    @Autowired
    private PlanService planService;

    @Autowired
    private UnstagedEventService eventService;

    @Autowired
    private UnstagedSignalService signalService;

    @Autowired
    private UnstagedAttributeService attributeService;

    @Autowired
    private UnstagedFieldService fieldService;

    @Autowired
    private UnstagedFieldGroupService service;

    private VersionedId signalId;
    private String tag;
    private UnstagedField field1;

    @BeforeAll
    void setUpAll() {
        var plan = TestModelUtils.plan();
        var planId = planService.create(plan).getId();

        var event = unstagedEvent();
        var eventId = eventService.create(event).getId();

        var signal = unstagedSignal(planId);
        signal = signalService.create(signal);
        signal = signalService.getById(signal.getSignalId());
        signalId = signal.getSignalId();

        var attribute1 = TestModelUtils.unstagedAttribute(eventId);
        var attribute2 = TestModelUtils.unstagedAttribute(eventId);
        var attribute3 = TestModelUtils.unstagedAttribute(eventId);
        var attributeId1 = attributeService.create(attribute1).getId();
        var attributeId2 = attributeService.create(attribute2).getId();
        var attributeId3 = attributeService.create(attribute3).getId();

        tag = getRandomSmallString();
        var name = getRandomSmallString();
        field1 = unstagedField(signalId).toBuilder()
                .tag(tag)
                .name(name)
                .expressionType(JEXL)
                .isMandatory(false)
                .build();
        var field2 = unstagedField(signalId).toBuilder()
                .tag(tag)
                .name(name)
                .expressionType(LITERAL)
                .isMandatory(false)
                .build();
        var field3 = unstagedField(signalId).toBuilder()
                .tag(tag)
                .name(getRandomSmallString())
                .expressionType(LITERAL)
                .isMandatory(false)
                .build();
        fieldService.create(field1, Set.of(attributeId1));
        fieldService.create(field2, Set.of(attributeId2));
        fieldService.create(field3, Set.of(attributeId3));
    }

    @Test
    @Order(1)
    void getAll() {
        var fields = signalService.getFields(signalId);
        assertThat(fields).hasSize(3);

        var fieldGroup = service.getAll(signalId);

        assertThat(fieldGroup).hasSize(2);
    }

    @Test
    @Order(2)
    void update_withoutName() {
        field1 = fieldService.getById(field1.getId());
        var fields = signalService.getFields(signalId).stream().toList();
        assertThat(fields).hasSize(3);

        val updatedValue = getRandomSmallString();
        var fieldGroup = service.getByKey(signalId, field1.getGroupKey());
        fieldGroup.setExpression(updatedValue)
                .setDescription(updatedValue)
                .setExpressionType(JEXL);

        service.update(fieldGroup);

        field1 = fieldService.getById(field1.getId());
        var updated = service.getByKey(signalId, field1.getGroupKey());

        assertThat(updated.getTag()).isEqualTo(tag);
        assertThat(updated.getName()).isEqualTo(field1.getName());
        assertThat(updated.getDescription()).isEqualTo(updatedValue);
        assertThat(updated.getExpression()).isEqualTo(updatedValue);
        assertThat(updated.getExpressionType()).isEqualTo(JEXL);
    }

    @Test
    @Order(3)
    void update_withName() {
        field1 = fieldService.getById(field1.getId());
        var fields = signalService.getFields(signalId).stream().toList();
        assertThat(fields).hasSize(3);

        val updatedValue = getRandomSmallString();
        var fieldGroup = service.getByKey(signalId, field1.getGroupKey());
        fieldGroup.setName(updatedValue)
                .setExpression(updatedValue)
                .setDescription(updatedValue)
                .setExpressionType(JEXL);

        service.update(fieldGroup);

        field1 = fieldService.getById(field1.getId());
        var updated = service.getByKey(signalId, field1.getGroupKey());

        assertThat(updated.getTag()).isEqualTo(tag);
        assertThat(updated.getName()).isEqualTo(updatedValue);
        assertThat(updated.getDescription()).isEqualTo(updatedValue);
        assertThat(updated.getExpression()).isEqualTo(updatedValue);
        assertThat(updated.getExpressionType()).isEqualTo(JEXL);
    }

    @Test
    @Order(4)
    void delete() {
        field1 = fieldService.getById(field1.getId());
        var key = field1.getGroupKey();
        var maybeFieldGroup = service.findByKey(signalId, key);
        assertThat(maybeFieldGroup).isPresent();

        service.deleteByKey(signalId, key);

        maybeFieldGroup = service.findByKey(signalId, key);
        assertThat(maybeFieldGroup).isEmpty();
    }
}
