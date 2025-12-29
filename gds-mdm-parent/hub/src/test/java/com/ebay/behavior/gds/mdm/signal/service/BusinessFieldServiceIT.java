package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedField;
import com.ebay.behavior.gds.mdm.signal.model.manyToMany.SojEventTagMapping;
import com.ebay.behavior.gds.mdm.signal.repository.SojBusinessTagRepository;
import com.ebay.behavior.gds.mdm.signal.repository.SojEventRepository;
import com.ebay.behavior.gds.mdm.signal.repository.manyToMany.SojEventTagMappingRepository;
import com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.sojBusinessTag;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.sojEvent;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedAttribute;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedField;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles(IT)
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class BusinessFieldServiceIT {

    @Autowired
    private BusinessFieldService service;

    @Autowired
    private PlanService planService;

    @Autowired
    private UnstagedAttributeService attributeService;

    @Autowired
    private UnstagedEventService eventService;

    @Autowired
    private UnstagedSignalService signalService;

    @Autowired
    private UnstagedFieldService fieldService;

    @Autowired
    private SojEventRepository sojEventRepository;

    @Autowired
    private SojBusinessTagRepository sojBusinessTagRepository;

    @Autowired
    private SojEventTagMappingRepository sojTagMappingRepository;

    private long planId;

    @BeforeEach
    void setUp() {
        var plan = TestModelUtils.plan();
        planId = planService.create(plan).getId();
    }

    @Test
    void createBusinessFields() {
        var event = TestModelUtils.unstagedEvent().toBuilder().moduleIds(Set.of(10L, 11L)).build();
        var eventId = eventService.create(event).getId();

        var attribute = unstagedAttribute(eventId);
        var attributeId = attributeService.create(attribute).getId();

        var signal = TestModelUtils.unstagedSignal(planId);
        var signalId = signalService.create(signal).getSignalId();

        var field = unstagedField(signalId);
        fieldService.create(field, Set.of(attributeId));

        createSojEventAndTagSetup(List.of(10L, 11L));

        var fields = service.createBusinessFields(signalId, eventId, true);

        assertThat(fields.size()).isEqualTo(6); // 3 old and 3 new
        assertThat(fields).extracting(UnstagedField::getEventTypes).containsOnly(event.getType());
    }

    @Test
    void simulateBusinessFields() {
        var event = TestModelUtils.unstagedEvent().toBuilder().moduleIds(Set.of(10L, 11L)).build();
        event = eventService.create(event);
        var eventId = event.getId();

        var attribute = unstagedAttribute(eventId);
        var attributeId = attributeService.create(attribute).getId();

        var signal = TestModelUtils.unstagedSignal(planId);
        var signalId = signalService.create(signal).getSignalId();

        var field = unstagedField(signalId);
        fieldService.create(field, Set.of(attributeId));

        createSojEventAndTagSetup(List.of(10L, 11L));

        var fields = service.simulateBusinessFields(signalId, event, true);

        assertThat(fields.size()).isEqualTo(3);
        assertThat(fields).extracting(UnstagedField::getEventTypes).containsOnly(event.getType());
    }

    private void createSojEventAndTagSetup(List<Long> moduleIds) {
        var sojEvent1 = sojEvent("EXPC", 111L, moduleIds.get(0), null);
        var sojEvent2 = sojEvent("EXPC", 111L, moduleIds.get(1), null);
        sojEventRepository.saveAll(Set.of(sojEvent1, sojEvent2));

        var sojTag1 = sojBusinessTagRepository.save(sojBusinessTag("tag1"));
        var sojTag2 = sojBusinessTagRepository.save(sojBusinessTag("tag2"));
        var sojTag3 = sojBusinessTagRepository.save(sojBusinessTag("tag3"));

        sojTagMappingRepository.saveAll(Set.of(
                new SojEventTagMapping(sojEvent1, sojTag1),
                new SojEventTagMapping(sojEvent2, sojTag2),
                new SojEventTagMapping(sojEvent2, sojTag3)
        ));
    }
}
