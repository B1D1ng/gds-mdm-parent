package com.ebay.behavior.gds.mdm.signal.service.migration;

import com.ebay.behavior.gds.mdm.common.config.UdcConfiguration;
import com.ebay.behavior.gds.mdm.common.model.Environment;
import com.ebay.behavior.gds.mdm.common.model.ExpressionType;
import com.ebay.behavior.gds.mdm.common.model.JavaType;
import com.ebay.behavior.gds.mdm.common.util.TimeUtils;
import com.ebay.behavior.gds.mdm.signal.common.model.EventSource;
import com.ebay.behavior.gds.mdm.signal.common.model.MetadataSignal;
import com.ebay.behavior.gds.mdm.signal.common.model.SojPlatformTag;
import com.ebay.behavior.gds.mdm.signal.common.model.StagedEvent;
import com.ebay.behavior.gds.mdm.signal.common.model.StagedField;
import com.ebay.behavior.gds.mdm.signal.common.model.StagedSignal;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedEvent;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedField;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedSignal;
import com.ebay.behavior.gds.mdm.signal.common.model.external.legacymdm.LegacySignalRecord;
import com.ebay.behavior.gds.mdm.signal.common.model.external.legacymdm.datatype.EventClassifier;
import com.ebay.behavior.gds.mdm.signal.common.model.external.legacymdm.datatype.Field;
import com.ebay.behavior.gds.mdm.signal.common.model.external.legacymdm.datatype.LogicalDefinition;
import com.ebay.behavior.gds.mdm.signal.common.model.external.legacymdm.datatype.SignalDefinition;
import com.ebay.behavior.gds.mdm.signal.common.model.external.legacymdm.datatype.UuidGenerator;
import com.ebay.behavior.gds.mdm.signal.model.view.StagedSignalProductionView;
import com.ebay.behavior.gds.mdm.signal.model.view.StagedSignalStagingView;
import com.ebay.behavior.gds.mdm.signal.repository.SojPlatformTagRepository;
import com.ebay.behavior.gds.mdm.signal.service.PlatformLookupService;

import com.google.common.annotations.VisibleForTesting;
import jakarta.inject.Inject;
import javafx.util.Duration;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.CompletionStatus.COMPLETED;
import static com.ebay.behavior.gds.mdm.common.model.ExpressionType.JEXL;
import static com.ebay.behavior.gds.mdm.common.model.ExpressionType.LITERAL;
import static com.ebay.behavior.gds.mdm.common.model.Model.COMMA;
import static com.ebay.behavior.gds.mdm.signal.util.LegacyMapperConstants.EVENT_OFFSITE_EVENT;
import static com.ebay.behavior.gds.mdm.signal.util.LegacyMapperConstants.SIGNAL_MODULE_CLICK;
import static com.ebay.behavior.gds.mdm.signal.util.LegacyMapperConstants.SIGNAL_OFFSITE_CLICK;
import static com.ebay.behavior.gds.mdm.signal.util.LegacyMapperConstants.SIGNAL_ONSITE_CLICK;
import static java.util.Locale.US;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang.StringUtils.isNotBlank;

@Slf4j
@Component
@SuppressWarnings("PMD.LongVariable")
public class LegacyMapper extends ModelMapper {

    public static final String ALIAS = "alias";
    public static final Map<String, ExpressionType> LEGACY_TO_NEW_EXPRESSIONTYPE_MAP = Map.of("lit", LITERAL, "jexl", JEXL);
    public static final Map<ExpressionType, String> NEW_TO_LEGACY_EXPRESSIONTYPE_MAP = Map.of(LITERAL, "lit", JEXL, "jexl");
    private static final String JAVA_LANG_PREFIX = "java.lang.";
    private static final DateTimeFormatter LEGACY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final UdcConfiguration udcConfiguration;
    private final SojPlatformTagRepository platformTagRepository;
    private Set<String> platformFields;

    @Inject
    private PlatformLookupService platformService;

    private final Converter<EventClassifier, UnstagedEvent> eventClassifierToUnstagedEventConvertor = context -> {
        val src = context.getSource();
        val event = new UnstagedEvent();
        event.setName(src.getName());
        event.setDescription(src.getName());
        event.setExpression(src.getFilter());
        event.setSource(EventSource.valueOf(src.getSource()));
        event.setFsmOrder(Optional.ofNullable(src.getFsmOrder()).orElse(1));
        event.setType(src.getType());
        event.setExpressionType(JEXL);
        event.setAttributes(Set.of());
        event.setPageIds(Set.of());
        event.setClickIds(Set.of());
        event.setModuleIds(Set.of());
        return event;
    };

    private final Converter<UnstagedEvent, EventClassifier> unstagedEventToEventClassifierConvertor = context -> {
        val src = context.getSource();
        val event = new EventClassifier();
        event.setName(src.getName());
        event.setSource(src.getSource().name());
        event.setFsmOrder(src.getFsmOrder());
        event.setType(src.getType());
        event.setFilter(src.getExpression());
        return event;
    };

    private final Converter<StagedEvent, EventClassifier> stagedEventToEventClassifierConvertor = context -> {
        val src = context.getSource();
        val event = new EventClassifier();
        event.setName(src.getName());
        event.setSource(src.getSource().name());
        event.setFsmOrder(src.getFsmOrder());
        event.setType(src.getType());
        event.setFilter(src.getExpression());
        return event;
    };

    private final Converter<Field, UnstagedField> fieldToUnstagedFieldConvertor = context -> {
        val src = context.getSource();
        val dst = new UnstagedField();
        dst.setName(src.getName());
        dst.setDescription(src.getName());
        dst.setExpression(src.getFormula());
        dst.setExpressionType(LEGACY_TO_NEW_EXPRESSIONTYPE_MAP.get(src.getClz()));
        dst.setJavaType(mapJavaType(src.getType()));
        dst.setIsCached(src.isCached());
        dst.setEventTypes(String.join(COMMA, src.getReadyStates()));
        dst.setIsMandatory(getPlatformFields().contains(src.getName()));
        dst.setAttributes(Set.of());
        dst.setAvroSchema(dst.getJavaType().toSchema());
        return dst;
    };

    private final Converter<UnstagedField, Field> unstagedFieldToFieldConvertor = context -> {
        val src = context.getSource();
        val dst = new Field();
        dst.setName(src.getName());
        dst.setFormula(src.getExpression());
        dst.setClz(NEW_TO_LEGACY_EXPRESSIONTYPE_MAP.get(src.getExpressionType()));
        dst.setType(src.getJavaType().getValue().replace(JAVA_LANG_PREFIX, ""));
        dst.setCached(Boolean.TRUE.equals(src.getIsCached()));
        dst.setReadyStates(Arrays.stream(src.getEventTypes().split(COMMA)).toList());
        return dst;
    };

    private final Converter<StagedField, Field> stagedFieldToFieldConvertor = context -> {
        val src = context.getSource();
        val dst = new Field();
        dst.setName(src.getName());
        dst.setFormula(src.getExpression());
        dst.setClz(NEW_TO_LEGACY_EXPRESSIONTYPE_MAP.get(src.getExpressionType()));
        dst.setType(src.getJavaType().getValue().replace(JAVA_LANG_PREFIX, ""));
        dst.setCached(Boolean.TRUE.equals(src.getIsCached()));
        dst.setReadyStates(Arrays.stream(src.getEventTypes().split(COMMA)).toList());
        return dst;
    };

    private final Converter<SignalDefinition, UnstagedSignal> signalDefinitionToUnstagedSignalConvertor = context -> {
        val src = context.getSource();
        val logicalDefinitions = src.getLogicalDefinition();

        if (CollectionUtils.isEmpty(logicalDefinitions)) {
            return toUnstagedSignal(src, Set.of());
        }

        val logicalDefinition = src.getLogicalDefinition().get(0);

        val eventTypes = logicalDefinition.getEventClassifiers().stream()
                .map(EventClassifier::getType)
                .collect(toSet());

        val dst = toUnstagedSignal(src, eventTypes);
        if (CollectionUtils.isEmpty(src.getLogicalDefinition())) {
            return dst;
        }

        populateLogicalDefinitionFields(dst, logicalDefinition);

        // populate events
        val events = logicalDefinition.getEventClassifiers().stream()
                .map(eventClassifier -> this.map(eventClassifier, UnstagedEvent.class))
                .collect(toSet());

        events.forEach(event -> {
            event.setCreateDate(dst.getCreateDate());
            event.setUpdateDate(dst.getUpdateDate());
            event.setCreateBy(dst.getCreateBy());
            event.setUpdateBy(dst.getUpdateBy());
        });
        dst.setEvents(events);

        // populate fields
        val fields = logicalDefinition.getFields().stream()
                .filter(field -> !ALIAS.equals(field.getClz()))
                .map(field -> this.map(field, UnstagedField.class))
                .collect(toSet());

        fields.forEach(field -> {
            field.setSignalVersion(src.getVersion());
            field.setCreateDate(dst.getCreateDate());
            field.setUpdateDate(dst.getUpdateDate());
            field.setCreateBy(dst.getCreateBy());
            field.setUpdateBy(dst.getUpdateBy());
        });

        dst.setFields(fields);
        return dst;
    };

    private final Converter<StagedSignal, SignalDefinition> stagedSignalToSignalDefinitionConvertor = context ->
            convertSignalToSignalDefinition(context.getSource());

    private final Converter<UnstagedSignal, SignalDefinition> unstagedSignalToSignalDefinitionConvertor = context ->
            convertSignalToSignalDefinition(context.getSource());

    private final Converter<StagedSignalStagingView, SignalDefinition> stagingViewToSignalDefinitionConvertor = context ->
            convertSignalToSignalDefinition(context.getSource());

    private final Converter<StagedSignalProductionView, SignalDefinition> productionViewToSignalDefinitionConvertor = context ->
            convertSignalToSignalDefinition(context.getSource());

    public LegacyMapper(PlatformLookupService platformService,
                        SojPlatformTagRepository platformTagRepository,
                        UdcConfiguration udcConfiguration) {
        this.udcConfiguration = udcConfiguration;
        this.platformTagRepository = platformTagRepository;
        this.platformService = platformService;
    }

    @VisibleForTesting
    @PostConstruct
    public void init() {
        getConfiguration().setAmbiguityIgnored(true);
        createTypeMap(EventClassifier.class, UnstagedEvent.class).setConverter(eventClassifierToUnstagedEventConvertor);
        createTypeMap(UnstagedEvent.class, EventClassifier.class).setConverter(unstagedEventToEventClassifierConvertor);
        createTypeMap(StagedEvent.class, EventClassifier.class).setConverter(stagedEventToEventClassifierConvertor);
        createTypeMap(Field.class, UnstagedField.class).setConverter(fieldToUnstagedFieldConvertor);
        createTypeMap(UnstagedField.class, Field.class).setConverter(unstagedFieldToFieldConvertor);
        createTypeMap(StagedField.class, Field.class).setConverter(stagedFieldToFieldConvertor);
        createTypeMap(SignalDefinition.class, UnstagedSignal.class).setConverter(signalDefinitionToUnstagedSignalConvertor);
        createTypeMap(StagedSignal.class, SignalDefinition.class).setConverter(stagedSignalToSignalDefinitionConvertor);
        createTypeMap(UnstagedSignal.class, SignalDefinition.class).setConverter(unstagedSignalToSignalDefinitionConvertor);
        createTypeMap(StagedSignalStagingView.class, SignalDefinition.class).setConverter(stagingViewToSignalDefinitionConvertor);
        createTypeMap(StagedSignalProductionView.class, SignalDefinition.class).setConverter(productionViewToSignalDefinitionConvertor);
    }

    public List<UnstagedSignal> mapLegacySignalRecord(LegacySignalRecord source, Long planId) {
        return source.getVersions().stream()
                .map(legacySignal -> this.map(legacySignal, UnstagedSignal.class))
                .peek(signal -> {
                    signal.setPlanId(planId);
                    signal.setCompletionStatus(COMPLETED);
                    signal.setEnvironment(Environment.PRODUCTION);
                })
                .toList();
    }

    public static String mapSignalType(String srcSignalType, Set<String> srcEventTypes) {
        if (SIGNAL_MODULE_CLICK.equals(srcSignalType)) {
            if (srcEventTypes.size() == 1 && srcEventTypes.contains(EVENT_OFFSITE_EVENT)) {
                return SIGNAL_OFFSITE_CLICK;
            } else {
                return SIGNAL_ONSITE_CLICK;
            }
        }
        return srcSignalType;
    }

    private JavaType mapJavaType(String srcType) {
        return switch (srcType) {
            case "decimal(38,0)" -> JavaType.BIG_INTEGER; // Defined by data producer
            case "struct" -> JavaType.OBJECT;
            case "map<string,string>" -> JavaType.MAP;
            case "array" -> JavaType.LIST;
            default -> JavaType.fromValue(JAVA_LANG_PREFIX + srcType);
        };
    }

    /**
     * Skip platform fields check in the integration test environment
     * Lazy loaded platform fields with double check locking, since Hibernate creates H2 tables after Converters are registered,
     * which causes Spring Bean creation failure.
     */
    private Set<String> getPlatformFields() {
        if (nonNull(platformFields)) {
            return platformFields;
        }
        synchronized (this) {
            if (nonNull(platformFields)) {
                return platformFields;
            }

            platformFields = platformTagRepository.findAll().stream()
                    .map(SojPlatformTag::getSojName)
                    .collect(toSet());
            return platformFields;
        }
    }

    private UnstagedSignal toUnstagedSignal(SignalDefinition src, Set<String> srcEventTypes) {
        return UnstagedSignal.builder()
                .planId(0L)
                .legacyId(src.getId())
                .version(src.getVersion())
                .name(src.getName())
                .description(src.getDescription())
                .domain(src.getDomain()).owners(src.getCreatedUser())
                .type(mapSignalType(src.getType(), srcEventTypes))
                .completionStatus(COMPLETED)
                .dataSource(udcConfiguration.getDataSource())
                .createBy(src.getCreatedUser())
                .updateBy(src.getUpdatedUser())
                .createDate(Timestamp.valueOf(src.getCreatedTime()))
                .updateDate(Timestamp.valueOf(src.getUpdatedTime()))
                .refVersion(src.getRefVersion())
                .build();
    }

    private void populateLogicalDefinitionFields(UnstagedSignal dst, LogicalDefinition logicalDefinition) {
        if (isNotBlank(logicalDefinition.getRetentionPeriod())) {
            long millis = Math.round(Duration.valueOf(logicalDefinition.getRetentionPeriod()).toMillis());
            dst.setRetentionPeriod(millis);
        }

        dst.setPlatformId(platformService.getPlatformId(logicalDefinition.getPlatform().toUpperCase(US)));
        val generator = logicalDefinition.getUuidGenerator();
        if (nonNull(generator)) {
            dst.setUuidGeneratorExpression(generator.getFormula());
            dst.setUuidGeneratorType(generator.getClz());
        }
        if (CollectionUtils.isNotEmpty(logicalDefinition.getCorrelationIdFormulas())) {
            dst.setCorrelationIdExpression(String.join(COMMA, logicalDefinition.getCorrelationIdFormulas()));
        }
    }

    private <S extends MetadataSignal> SignalDefinition convertSignalToSignalDefinition(S src) {
        val dst = new SignalDefinition()
                .setName(src.getName())
                .setId(nonNull(src.getLegacyId()) ? src.getLegacyId() : src.getId().toString())
                .setVersion(src.getVersion())
                .setDomain(src.getDomain())
                .setDescription(src.getDescription())
                .setStatus(0L)
                .setType(src.getType())
                .setCreatedUser(src.getCreateBy())
                .setRefVersion(src.getRefVersion())
                .setUpdatedUser(src.getUpdateBy());

        if (nonNull(src.getCreateDate())) {
            dst.setCreatedTime(TimeUtils.toString(src.getCreateDate(), LEGACY_FORMATTER));
        }
        if (nonNull(src.getUpdateDate())) {
            dst.setUpdatedTime(TimeUtils.toString(src.getUpdateDate(), LEGACY_FORMATTER));
        }

        val logicalDefinition = new LogicalDefinition()
                .setPlatform(platformService.getPlatformName(src.getPlatformId()))
                .setNeedAccumulation(src.getNeedAccumulation());

        if (nonNull(src.getRetentionPeriod())) {
            val minutes = (int) Duration.millis(src.getRetentionPeriod()).toMinutes();
            logicalDefinition.setRetentionPeriod(minutes + "m");
        }

        if (isNotBlank(src.getUuidGeneratorExpression()) && nonNull(src.getUuidGeneratorType())) {
            logicalDefinition.setUuidGenerator(new UuidGenerator(src.getUuidGeneratorType(), src.getUuidGeneratorExpression()));
        }

        if (isNotBlank(src.getCorrelationIdExpression())) {
            logicalDefinition.setCorrelationIdFormulas(Arrays.stream(src.getCorrelationIdExpression().split(COMMA)).toList());
        }

        List<Field> fields = List.of();
        if (CollectionUtils.isNotEmpty(src.getFields())) {
            fields = src.getFields().stream()
                    .map(field -> this.map(field, Field.class))
                    .toList();
        }
        logicalDefinition.setFields(fields);

        val events = CollectionUtils.emptyIfNull(src.getEvents()).stream()
                .map(event -> this.map(event, EventClassifier.class))
                .toList();

        logicalDefinition.setEventClassifiers(events);
        dst.setLogicalDefinition(List.of(logicalDefinition));
        return dst;
    }
}
