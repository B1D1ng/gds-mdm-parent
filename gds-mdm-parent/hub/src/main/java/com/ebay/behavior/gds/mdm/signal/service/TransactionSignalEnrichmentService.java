package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.common.model.JavaType;
import com.ebay.behavior.gds.mdm.signal.common.model.AttributeTemplate;
import com.ebay.behavior.gds.mdm.signal.common.model.FieldTemplate;
import com.ebay.behavior.gds.mdm.signal.common.model.TemplateFieldDefinition;
import com.ebay.behavior.gds.mdm.signal.common.model.TemplateFieldConfiguration;
import com.ebay.behavior.gds.mdm.signal.common.model.TemplateEnrichedRecord;

import javax.annotation.PostConstruct;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.ebay.behavior.gds.mdm.common.model.ExpressionType.JEXL;
import static com.ebay.behavior.gds.mdm.signal.service.SignalTemplateActionService.attribute;
import static com.ebay.behavior.gds.mdm.signal.service.SignalTemplateActionService.field;
import static java.util.Locale.US;

@Slf4j
@Component
@Validated
public class TransactionSignalEnrichmentService {
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AttributeTemplateService attributeService;

    @Autowired
    private FieldTemplateService fieldService;

    private TemplateFieldConfiguration fieldsConfig;

    @PostConstruct
    public void loadConfiguration() {
        try {
            val resource = new ClassPathResource("transaction-fields-config.json");
            val fieldDefinitions = objectMapper.readValue(resource.getInputStream(), new TypeReference<List<TemplateFieldDefinition>>() {});
            fieldsConfig = new TemplateFieldConfiguration(fieldDefinitions);
            log.info("Loaded {} transaction field definitions from configuration", fieldsConfig.getFields().size());
        } catch (IOException e) {
            log.error("Failed to load transaction fields configuration", e);
            throw new IllegalStateException("Failed to load transaction fields configuration", e);
        }
    }

    public TemplateEnrichedRecord enrichSignal(@PositiveOrZero long signalId, @PositiveOrZero long sourceId, @PositiveOrZero long typeId,
                                                   Optional<List<TemplateFieldDefinition>> customFields) {
        Optional<List<TemplateFieldDefinition>> safeCustomFields = customFields != null ? customFields : Optional.empty();
        val configToUse = safeCustomFields
                .filter(fields -> !fields.isEmpty())
                .map(TemplateFieldConfiguration::new)
                .orElse(fieldsConfig);

        return enrich(signalId, sourceId, typeId, configToUse);
    }

    @VisibleForTesting
    private TemplateEnrichedRecord enrich(@PositiveOrZero long signalId, @PositiveOrZero long sourceId, @PositiveOrZero long typeId,
                                           @NotNull @Valid TemplateFieldConfiguration configuration) {
        val attributeIds = createAttributes(sourceId, configuration);
        val fields = createFields(signalId, configuration);
        createFieldTemplates(fields, attributeIds, typeId);
        return new TemplateEnrichedRecord(attributeIds, fields);
    }

    private List<Long> createAttributes(long sourceId, TemplateFieldConfiguration configuration) {
        val attributesToCreate = configuration.getFields().stream()
                .map(fieldDef -> attribute(sourceId, fieldDef.getAttributeName(), fieldDef.getSchemaPath(),
                        JavaType.valueOf(fieldDef.getJavaType().toUpperCase(US))))
                .collect(Collectors.toSet());

        return attributeService.createAll(attributesToCreate).stream()
                .map(AttributeTemplate::getId)
                .toList();
    }

    private List<FieldTemplate> createFields(long signalId, TemplateFieldConfiguration configuration) {
        return configuration.getFields().stream()
                .map(fieldDef -> field(signalId, fieldDef.getFieldName(), fieldDef.getDescription(),
                        JavaType.valueOf(fieldDef.getJavaType().toUpperCase(US)), fieldDef.getSchemaPath(), JEXL, fieldDef.isMandatory()))
                .toList();
    }

    private void createFieldTemplates(List<FieldTemplate> fields, List<Long> attributeIds, long typeId) {
        // Note: FieldTemplateService.createAll() is not implemented by design due to complex associations
        for (int i = 0; i < fields.size(); i++) {
            val fieldTemplate = fields.get(i);
            if (i == 0) {
                fieldService.create(fieldTemplate, Set.of(attributeIds.get(0)), Set.of(typeId));
            } else if (i < attributeIds.size()) {
                fieldService.create(fieldTemplate, Set.of(attributeIds.get(i)), null);
            } else {
                fieldService.create(fieldTemplate, Set.of(), null);
            }
        }
    }
}