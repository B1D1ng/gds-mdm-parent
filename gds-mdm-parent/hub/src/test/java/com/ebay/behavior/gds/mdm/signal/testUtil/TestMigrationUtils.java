package com.ebay.behavior.gds.mdm.signal.testUtil;

import com.ebay.behavior.gds.mdm.common.model.CompletionStatus;
import com.ebay.behavior.gds.mdm.common.model.ExpressionType;
import com.ebay.behavior.gds.mdm.signal.common.model.MetadataEvent;
import com.ebay.behavior.gds.mdm.signal.common.model.MetadataField;
import com.ebay.behavior.gds.mdm.signal.common.model.MetadataSignal;
import com.ebay.behavior.gds.mdm.signal.common.model.external.legacymdm.datatype.EventClassifier;
import com.ebay.behavior.gds.mdm.signal.common.model.external.legacymdm.datatype.Field;
import com.ebay.behavior.gds.mdm.signal.common.model.external.legacymdm.datatype.SignalDefinition;
import com.ebay.behavior.gds.mdm.signal.service.migration.LegacyMapper;

import javafx.util.Duration;
import lombok.experimental.UtilityClass;
import lombok.val;

import java.sql.Timestamp;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import static com.ebay.behavior.gds.mdm.common.model.Model.COMMA;
import static com.ebay.behavior.gds.mdm.signal.service.migration.LegacyMapper.LEGACY_TO_NEW_EXPRESSIONTYPE_MAP;
import static com.ebay.behavior.gds.mdm.signal.service.migration.LegacyMapper.mapSignalType;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.CJS_PLATFORM_ID;
import static com.ebay.behavior.gds.mdm.signal.util.ImportUtils.EJS;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.EJS_PLATFORM_ID;
import static com.ebay.behavior.gds.mdm.signal.util.ImportUtils.CJS;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

@UtilityClass
public class TestMigrationUtils {

    public static <S extends MetadataSignal> void assertSignal(SignalDefinition signalDefinition, S signal) {
        assertThat(signal.getName()).isEqualTo(signalDefinition.getName());
        assertThat(signal.getDescription()).isEqualTo(signalDefinition.getDescription());
        val srcEventTypes = signalDefinition.getLogicalDefinition().get(0)
                .getEventClassifiers().stream()
                .map(EventClassifier::getType)
                .collect(toSet());
        assertThat(signal.getType()).isEqualTo(mapSignalType(signalDefinition.getType(), srcEventTypes));
        assertThat(signal.getDomain()).isEqualTo(signalDefinition.getDomain());

        assertThat(signal.getCreateBy()).isEqualTo(signalDefinition.getCreatedUser());
        assertThat(signal.getUpdateBy()).isEqualTo(signalDefinition.getUpdatedUser());

        val createDate = truncDate(signal.getCreateDate());
        assertThat(createDate).isEqualTo(signalDefinition.getCreatedTime());
        assertThat(truncDate(signal.getUpdateDate())).isEqualTo(signalDefinition.getUpdatedTime());
        assertThat(signal.getCompletionStatus()).isEqualTo(CompletionStatus.COMPLETED);

        val logicalDef = signalDefinition.getLogicalDefinition().get(0);
        if (Objects.nonNull(logicalDef.getUuidGenerator())) {
            assertThat(signal.getUuidGeneratorType()).isEqualTo(ExpressionType.JEXL.toString().toLowerCase());
            assertThat(signal.getUuidGeneratorExpression()).isEqualTo(logicalDef.getUuidGenerator().getFormula());
        }
        if (CJS.equalsIgnoreCase(logicalDef.getPlatform())) {
            assertThat(signal.getPlatformId()).isEqualTo(CJS_PLATFORM_ID);
        } else if (EJS.equalsIgnoreCase(logicalDef.getPlatform())) {
            assertThat(signal.getPlatformId()).isEqualTo(EJS_PLATFORM_ID);
        }

        if (Objects.nonNull(logicalDef.getRetentionPeriod())) {
            val millis = Math.round(Duration.valueOf(logicalDef.getRetentionPeriod()).toMillis());
            assertThat(signal.getRetentionPeriod()).isEqualTo(millis);
        }

        assertThat(logicalDef.getEventClassifiers()).hasSizeGreaterThanOrEqualTo(1);
        val oldEvents = logicalDef.getEventClassifiers().stream()
                .collect(toMap(ec -> ec.getName() + "_" + signal.getVersion(), Function.identity()));

        val newEvents = signal.getEvents().stream()
                .map(evt -> (MetadataEvent) evt)
                .collect(toMap(event -> event.getName() + "_" + signal.getVersion(), Function.identity()));
        assertEvents(oldEvents, newEvents);

        assertThat(logicalDef.getFields()).hasSizeGreaterThanOrEqualTo(1);
        val oldFields = logicalDef.getFields().stream()
                .filter(field -> !LegacyMapper.ALIAS.equals(field.getClz()))
                .collect(toMap(field -> field.getName() + "_" + signal.getVersion(), Function.identity()));
        val newFields = signal.getFields().stream()
                .map(field -> (MetadataField) field)
                .collect(toMap(field -> field.getName() + "_" + signal.getVersion(), Function.identity()));
        assertFields(oldFields, newFields);
    }

    private static <F extends MetadataField> void assertFields(Map<String, Field> oldFields, Map<String, F> newFields) {
        for (val key : oldFields.keySet()) {
            assertThat(newFields.containsKey(key)).isTrue();
            val oldField = oldFields.get(key);
            val newField = newFields.get(key);

            assertThat(newField.getName()).isEqualTo(oldField.getName());
            val fieldType = newField.getJavaType().toString().replace("java.lang.", "");
            assertThat(fieldType).isEqualToIgnoringCase(oldField.getType());
            assertThat(newField.getExpression()).isEqualTo(oldField.getFormula());
            assertThat(newField.getExpressionType()).isEqualTo(LEGACY_TO_NEW_EXPRESSIONTYPE_MAP.get(oldField.getClz()));
            assertThat(newField.getIsCached()).isEqualTo(oldField.isCached());

            val eventTypes = String.join(COMMA, oldField.getReadyStates());
            assertThat(newField.getEventTypes()).isEqualTo(eventTypes);
        }
    }

    private static <E extends MetadataEvent> void assertEvents(Map<String, EventClassifier> oldEvents, Map<String, E> newEvents) {
        for (val key : oldEvents.keySet()) {
            assertThat(newEvents.containsKey(key)).isTrue();
            val oldEvent = oldEvents.get(key);
            val newEvent = newEvents.get(key);

            assertThat(newEvent.getName()).isEqualTo(oldEvent.getName());
            assertThat(newEvent.getType()).isEqualTo(oldEvent.getType());
            assertThat(newEvent.getSource().toString()).isEqualTo(oldEvent.getSource());
            if (Objects.isNull(oldEvent.getFsmOrder())) {
                assertThat(newEvent.getFsmOrder()).isEqualTo(1);
            } else {
                assertThat(newEvent.getFsmOrder()).isEqualTo(oldEvent.getFsmOrder());
            }
            assertThat(newEvent.getExpression()).isEqualTo(oldEvent.getFilter());
            assertThat(newEvent.getExpressionType()).isEqualTo(ExpressionType.JEXL);
        }
    }

    private static String truncDate(Timestamp date) {
        return date.toString().substring(0, 19);
    }
}
