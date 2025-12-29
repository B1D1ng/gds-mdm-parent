package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.common.model.JavaType;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.signal.common.model.EventSource;
import com.ebay.behavior.gds.mdm.signal.common.model.SojBusinessTag;
import com.ebay.behavior.gds.mdm.signal.common.model.SojEvent;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedAttribute;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedEvent;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedField;
import com.ebay.behavior.gds.mdm.signal.model.manyToMany.SojEventTagMapping;
import com.ebay.behavior.gds.mdm.signal.repository.SojEventRepository;
import com.ebay.behavior.gds.mdm.signal.repository.manyToMany.SojEventTagMappingRepository;

import io.vavr.Tuple;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.ExpressionType.JEXL;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

@Slf4j
@Service
@Validated
public class BusinessFieldService {

    @Autowired
    private UnstagedEventService eventService;

    @Autowired
    private UnstagedAttributeService attributeService;

    @Autowired
    private UnstagedFieldService fieldService;

    @Autowired
    private SojEventRepository sojEventRepository;

    @Autowired
    private SojEventTagMappingRepository sojTagMappingRepository;

    public Set<UnstagedField> simulateBusinessFields(@NotNull @Valid VersionedId signalId, UnstagedEvent event, boolean addEventFlow) {
        return createBusinessFields(signalId, event, addEventFlow, true);
    }

    public Set<UnstagedField> createBusinessFields(@NotNull @Valid VersionedId signalId, long eventId, boolean addEventFlow) {
        return createBusinessFields(signalId, eventService.getById(eventId), addEventFlow, false);
    }

    private Set<UnstagedField> createBusinessFields(VersionedId signalId, UnstagedEvent event, boolean addEventFlow, boolean dryRun) {
        if (!EventSource.SOJ.equals(event.getSource())) { // Only SOJ events have business tags
            return Set.of();
        }

        // Get SOJ events by metadata
        var sojEvents = new ArrayList<SojEvent>();
        // TODO: eactn should be one of the soj event filter conditions
        val pageIds = event.getPageIds();
        if (isNotEmpty(pageIds)) { // Page signals
            sojEvents.addAll(sojEventRepository.findByPageIdIn(pageIds));
        } else { // Module signals
            val moduleIds = event.getModuleIds();
            val clickIds = event.getClickIds();
            if (isNotEmpty(moduleIds) && isNotEmpty(clickIds)) {
                sojEvents.addAll(sojEventRepository.findByModuleIdInAndClickIdIn(moduleIds, clickIds));
            } else if (isNotEmpty(moduleIds)) {
                sojEvents.addAll(sojEventRepository.findByModuleIdIn(moduleIds));
            } else {
                sojEvents.addAll(sojEventRepository.findByClickIdIn(clickIds));
            }
        }

        val sojTags = sojEvents.stream()
                .flatMap(soj -> sojTagMappingRepository.findBySojEventId(soj.getId()).stream()
                        .map(SojEventTagMapping::getSojTag))
                .collect(toSet());

        return createBusinessFields(signalId, event, sojTags, addEventFlow, dryRun);
    }

    private Set<UnstagedField> createBusinessFields(VersionedId signalId, UnstagedEvent event, Set<SojBusinessTag> sojTags,
                                                    boolean addEventFlow, boolean dryRun) {
        if (CollectionUtils.isEmpty(sojTags)) {
            return Set.of();
        }

        var attributes = sojTags.stream()
                .map(sojTag -> toBusinessAttribute(sojTag, event.getId()))
                .collect(toSet());

        if (!dryRun) {
            attributes = new HashSet<>(attributeService.createAll(attributes));
        }

        return attributes.stream()
                .map(attr -> Tuple.of(attr, toBusinessField(signalId, event.getType(), attr)))
                .peek(attrWithField -> {
                    val field = attrWithField._2;
                    if (addEventFlow) { // If a new field is added during an event addition, that means the only event type is the one being added
                        field.setEventTypes(event.getType());
                    }
                })
                .peek(attrWithField -> {
                    if (!dryRun) {
                        fieldService.create(attrWithField._2, Set.of(attrWithField._1.getId()));
                    }
                })
                .map(attrWithField -> attrWithField._2)
                .collect(toSet());
    }

    private UnstagedAttribute toBusinessAttribute(SojBusinessTag sojTag, long eventId) {
        val attr = UnstagedAttribute.builder()
                .eventId(eventId)
                .tag(sojTag.getSojName())
                .description(sojTag.getDescription())
                .schemaPath(sojTag.getSchemaPath())
                .build();
        try {
            attr.setJavaType(JavaType.fromValue(sojTag.getDataType()));
        } catch (IllegalArgumentException ex) {
            log.error("Invalid JavaType: {}", sojTag.getDataType(), ex);
            attr.setJavaType(JavaType.STRING);
        }
        return attr;
    }

    private UnstagedField toBusinessField(VersionedId signalId, String eventType, UnstagedAttribute attribute) {
        return UnstagedField.builder()
                .signalId(signalId.getId())
                .signalVersion(signalId.getVersion())
                .name(attribute.getTag())
                .description(attribute.getDescription())
                .tag(attribute.getTag())
                .javaType(attribute.getJavaType())
                .avroSchema(attribute.getJavaType().toSchema())
                .expression(attribute.getSchemaPath())
                .expressionType(JEXL)
                .isMandatory(false)
                .isCached(false)
                .eventTypes(eventType)
                .build();
    }
}
