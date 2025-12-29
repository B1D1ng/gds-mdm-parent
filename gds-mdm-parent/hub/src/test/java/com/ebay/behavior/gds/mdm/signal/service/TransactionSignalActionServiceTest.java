package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.common.model.JavaType;
import com.ebay.behavior.gds.mdm.signal.common.model.EventTemplate;
import com.ebay.behavior.gds.mdm.signal.common.model.SignalTemplate;
import com.ebay.behavior.gds.mdm.signal.common.model.FieldTemplate;
import com.ebay.behavior.gds.mdm.signal.common.model.TemplateQuestion;
import com.ebay.behavior.gds.mdm.signal.common.model.TemplateEnrichedRecord;
import com.ebay.behavior.gds.mdm.signal.common.model.EventTypeLookup;
import com.ebay.behavior.gds.mdm.signal.common.model.AttributeTemplate;
import com.ebay.behavior.gds.mdm.signal.common.model.TemplateFieldDefinition;
import java.util.Optional;

import org.assertj.core.util.Strings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.signal.service.SignalTemplateActionService.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static com.ebay.behavior.gds.mdm.signal.common.model.EventSource.TRANSACTION_EBAYLIVE_ATTRIBUTED_STREAM;

@ExtendWith(MockitoExtension.class)
class TransactionSignalActionServiceTest {

    @Mock
    private SignalTemplateService signalTemplateService;

    @Mock
    private EventTemplateService eventTemplateService;

    @Mock
    private TemplateQuestionService templateQuestionService;

    @Mock
    private EventTypeLookupService eventTypeLookupService;

    @Mock
    private AttributeTemplateService attributeTemplateService;

    @Mock
    private FieldTemplateService fieldTemplateService;

    @Mock
    private TransactionSignalEnrichmentService transactionSignalEnrichmentService;

    @Mock
    private PlatformLookupService platformService;

    @InjectMocks
    private SignalTemplateActionService signalTemplateActionService;

    // Test data
    private EventTemplate mockTransactionEvent;
    private SignalTemplate mockSignalTemplate;
    private TemplateQuestion mockQuestion;
    private TemplateEnrichedRecord mockEnrichmentResult;
    public static final Long TRANSACTION_PLATFORM_ID = 4L;

    @BeforeEach
    void setUp() {
        // Setup mock platform lookup service
        when(platformService.getPlatformId(TRANSACTION_SIGNAL)).thenReturn(TRANSACTION_PLATFORM_ID);

        // Setup mock transaction event
        mockTransactionEvent = EventTemplate.builder()
                .id(1L)
                .type(TRANSACTION_EVENT)
                .name("Transaction event")
                .description("Transaction processing event")
                .source(TRANSACTION_EBAYLIVE_ATTRIBUTED_STREAM)
                .expression("event?.eventInfo.source == ${TRANSACTION_SOURCE_TYPE}")
                .isMandatory(true)
                .build();

        // Setup mock signal template
        mockSignalTemplate = SignalTemplate.builder()
                .id(1L)
                .type(TRANSACTION_SIGNAL)
                .name(TRANSACTION_SIGNAL_NAME)
                .description(TRANSACTION_SIGNAL_NAME + " signal template")
                .platformId(TRANSACTION_PLATFORM_ID)
                .build();

        // Setup mock question
        mockQuestion = TemplateQuestion.builder()
                .id(1L)
                .question("What is the source stream for this transaction signal?")
                .description("What is the source stream for this transaction signal?")
                .build();

        // Setup mock enrichment result with sample fields
        List<FieldTemplate> mockFields = Arrays.asList(
                FieldTemplate.builder()
                        .id(1L)
                        .name("signal.gdsSourceTs")
                        .description("GDS source timestamp")
                        .javaType(JavaType.LONG)
                        .isMandatory(true)
                        .build(),
                FieldTemplate.builder()
                        .id(2L)
                        .name("orderId")
                        .description("Transaction order identifier")
                        .javaType(JavaType.STRING)
                        .expression("event.transactionContext.orderId")
                        .isMandatory(true)
                        .build(),
                FieldTemplate.builder()
                        .id(3L)
                        .name("transactionId")
                        .description("Transaction identifier")
                        .javaType(JavaType.LONG)
                        .expression("event.transactionContext.transactionId")
                        .isMandatory(true)
                        .build(),
                FieldTemplate.builder()
                        .id(4L)
                        .name("buyerUserId")
                        .description("Buyer user identifier")
                        .javaType(JavaType.LONG)
                        .expression("event.transactionContext.buyer.userId")
                        .isMandatory(true)
                        .build(),
                FieldTemplate.builder()
                        .id(5L)
                        .name("primaryCategoryDetails")
                        .description("Primary category details")
                        .javaType(JavaType.LIST)
                        .expression("event.transactionContext.primaryCategoryDetails")
                        .isMandatory(false)
                        .build()
        );

        mockEnrichmentResult = new TemplateEnrichedRecord(
                Arrays.asList(1L, 2L, 3L, 4L, 5L),
                mockFields
        );
    }

    @Test
    void testTransactionSignalCreation_ThroughRecreateMethod() {
        // Given: Mock dependencies for recreate("TRANSACTION") calling transactionSignal() internally
        when(signalTemplateService.findByType(TRANSACTION_SIGNAL)).thenReturn(Optional.empty()); // No existing signal
        when(eventTemplateService.create(any(EventTemplate.class))).thenReturn(mockTransactionEvent);
        when(templateQuestionService.create(any(TemplateQuestion.class), eq(Set.of(1L)))).thenReturn(mockQuestion);
        when(signalTemplateService.create(any(SignalTemplate.class))).thenReturn(mockSignalTemplate);
        when(eventTypeLookupService.getByName(TRANSACTION_EVENT)).thenReturn(mockEventType(1L, TRANSACTION_EVENT));
        when(attributeTemplateService.create(any(AttributeTemplate.class))).thenReturn(createMockAttributeTemplate(1L));
        when(fieldTemplateService.create(any(FieldTemplate.class), any(), any())).thenReturn(createMockFieldTemplate(1L));
        when(transactionSignalEnrichmentService.enrichSignal(anyLong(), anyLong(), anyLong(), any(Optional.class))).thenReturn(mockEnrichmentResult);
        when(signalTemplateService.getByIdWithAssociationsRecursive(1L)).thenReturn(mockFullSignalTemplate());

        // When: Call recreate for TRANSACTION signal (which internally calls transactionSignal())
        SignalTemplate result = signalTemplateActionService.recreate(TRANSACTION_SIGNAL, Optional.empty());

        // Then: Verify event template creation
        verify(eventTemplateService).create(argThat(eventTemplate ->
            TRANSACTION_EVENT.equals(eventTemplate.getType()) &&
            "Transaction event".equals(eventTemplate.getName()) &&
            "Transaction processing event".equals(eventTemplate.getDescription()) &&
            TRANSACTION_EBAYLIVE_ATTRIBUTED_STREAM.equals(eventTemplate.getSource()) &&
            "event?.eventInfo.source == ${TRANSACTION_SOURCE_TYPE}".equals(eventTemplate.getExpression()) &&
            Boolean.TRUE.equals(eventTemplate.getIsMandatory())
        ));

        // Verify template question creation
        verify(templateQuestionService).create(argThat(question ->
            "What is the source stream for this transaction signal?".equals(question.getQuestion())
        ), eq(Set.of(1L)));

        // Verify signal template creation
        verify(signalTemplateService).create(argThat(signal ->
            TRANSACTION_SIGNAL.equals(signal.getType()) &&
            TRANSACTION_SIGNAL_NAME.equals(signal.getName()) &&
            Strings.concat(TRANSACTION_SIGNAL_NAME + " signal template").equals(signal.getDescription()) &&
            TRANSACTION_PLATFORM_ID.equals(signal.getPlatformId())
        ));

        // Verify enricher delegation with expected parameters
        verify(transactionSignalEnrichmentService).enrichSignal(
            eq(1L), // signalId
            eq(1L), // transactionSourceId
            eq(1L), // transactionTypeId
            eq(Optional.empty()) // customFieldsConfig - empty Optional for default behavior
        );

        assertThat(result).isNotNull();
    }

    @Test
    void testTransactionSignalCreation_AlignedWithEnricher() {
        // Given: Mock dependencies
        when(signalTemplateService.findByType(TRANSACTION_SIGNAL)).thenReturn(Optional.empty());
        when(eventTemplateService.create(any(EventTemplate.class))).thenReturn(mockTransactionEvent);
        when(templateQuestionService.create(any(TemplateQuestion.class), any())).thenReturn(mockQuestion);
        when(signalTemplateService.create(any(SignalTemplate.class))).thenReturn(mockSignalTemplate);
        when(eventTypeLookupService.getByName(TRANSACTION_EVENT)).thenReturn(mockEventType(1L, TRANSACTION_EVENT));
        when(attributeTemplateService.create(any(AttributeTemplate.class))).thenReturn(createMockAttributeTemplate(1L));
        when(fieldTemplateService.create(any(FieldTemplate.class), any(), any())).thenReturn(createMockFieldTemplate(1L));
        when(transactionSignalEnrichmentService.enrichSignal(anyLong(), anyLong(), anyLong(), any(Optional.class)))
                .thenReturn(mockEnrichmentResult);
        when(signalTemplateService.getByIdWithAssociationsRecursive(1L)).thenReturn(mockFullSignalTemplate());

        // When: Call recreate method
        SignalTemplate result = signalTemplateActionService.recreate(TRANSACTION_SIGNAL, Optional.empty());

        // Then: Verify that enricher is called with correct parameters (alignment with TransactionSignalEnricher)
        verify(transactionSignalEnrichmentService).enrichSignal(
            eq(1L), // signalId from created signal
            eq(1L), // transactionSourceId from created event
            eq(1L), // transactionTypeId from lookup
            eq(Optional.empty()) // customFieldsConfig - empty Optional for default behavior
        );

        // Verify final result retrieval with associations
        verify(signalTemplateService).getByIdWithAssociationsRecursive(1L);

        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(TRANSACTION_SIGNAL);
        assertThat(result.getName()).isEqualTo(TRANSACTION_SIGNAL_NAME);
    }

    @Test
    void testTransactionSignalCreation_NoDatabaseDependency() {
        // Given: All dependencies are mocked (no real database calls)
        when(signalTemplateService.findByType(anyString())).thenReturn(Optional.empty());
        when(eventTemplateService.create(any(EventTemplate.class))).thenReturn(mockTransactionEvent);
        when(templateQuestionService.create(any(TemplateQuestion.class), any())).thenReturn(mockQuestion);
        when(signalTemplateService.create(any(SignalTemplate.class))).thenReturn(mockSignalTemplate);
        when(eventTypeLookupService.getByName(anyString())).thenReturn(mockEventType(1L, TRANSACTION_EVENT));
        when(attributeTemplateService.create(any(AttributeTemplate.class))).thenReturn(createMockAttributeTemplate(1L));
        when(fieldTemplateService.create(any(FieldTemplate.class), any(), any())).thenReturn(createMockFieldTemplate(1L));
        when(transactionSignalEnrichmentService.enrichSignal(anyLong(), anyLong(), anyLong(), any(Optional.class)))
                .thenReturn(mockEnrichmentResult);
        when(signalTemplateService.getByIdWithAssociationsRecursive(anyLong())).thenReturn(mockFullSignalTemplate());

        // When: Call recreate method
        SignalTemplate result = signalTemplateActionService.recreate(TRANSACTION_SIGNAL, Optional.empty());

        // Then: Verify all dependencies were mocked and no real database calls occurred
        assertThat(result).isNotNull();

        // Verify all service interactions were with mocks
        verify(eventTemplateService, times(1)).create(any());
        verify(templateQuestionService, times(1)).create(any(), any());
        verify(signalTemplateService, times(1)).create(any());
        verify(eventTypeLookupService, times(1)).getByName(anyString());
        verify(transactionSignalEnrichmentService, times(1)).enrichSignal(
                anyLong(), anyLong(), anyLong(), any(Optional.class));
        verify(signalTemplateService, times(1)).getByIdWithAssociationsRecursive(anyLong());

        verifyNoMoreInteractions(eventTemplateService, templateQuestionService, signalTemplateService,
                                eventTypeLookupService, transactionSignalEnrichmentService);
    }

    @Test
    void testTransactionSignalCreation_WithCustomFieldsConfig() {
        // Given: Mock dependencies for recreate method with custom fields config
        TemplateFieldDefinition customField = new TemplateFieldDefinition();
        customField.setAttributeName("customOrderId");
        customField.setFieldName("customOrderId");
        customField.setDescription("Custom order identifier");
        customField.setJavaType("STRING");
        customField.setSchemaPath("event.transactionContext.customOrderId");
        customField.setMandatory(true);

        List<TemplateFieldDefinition> customFieldsConfig = Arrays.asList(customField);

        when(signalTemplateService.findByType(TRANSACTION_SIGNAL)).thenReturn(Optional.empty());
        when(eventTemplateService.create(any(EventTemplate.class))).thenReturn(mockTransactionEvent);
        when(templateQuestionService.create(any(TemplateQuestion.class), any())).thenReturn(mockQuestion);
        when(signalTemplateService.create(any(SignalTemplate.class))).thenReturn(mockSignalTemplate);
        when(eventTypeLookupService.getByName(TRANSACTION_EVENT)).thenReturn(mockEventType(1L, TRANSACTION_EVENT));
        when(attributeTemplateService.create(any(AttributeTemplate.class))).thenReturn(createMockAttributeTemplate(1L));
        when(fieldTemplateService.create(any(FieldTemplate.class), any(), any())).thenReturn(createMockFieldTemplate(1L));
        when(signalTemplateService.getByIdWithAssociationsRecursive(1L)).thenReturn(mockFullSignalTemplate());

        // When: Call recreate method with custom fields config
        SignalTemplate result = signalTemplateActionService.recreate(TRANSACTION_SIGNAL, Optional.of(customFieldsConfig));

        // Then: Verify that enrichSignal is called with custom config
        verify(transactionSignalEnrichmentService).enrichSignal(
            eq(1L), // signalId
            eq(1L), // transactionSourceId
            eq(1L), // transactionTypeId
            any(Optional.class) // Optional<List<TemplateFieldDefinition>> - should be non-empty for custom config
        );

        assertThat(result).isNotNull();
    }

    // Helper methods for creating mock objects
    private EventTypeLookup mockEventType(Long id, String name) {
        EventTypeLookup eventType = new EventTypeLookup();
        eventType.setId(id);
        eventType.setName(name);
        return eventType;
    }

    private AttributeTemplate createMockAttributeTemplate(Long id) {
        return AttributeTemplate.builder()
                .id(id)
                .tag("mockTag")
                .description("Mock attribute")
                .javaType(JavaType.STRING)
                .schemaPath("mock.path")
                .build();
    }

    private FieldTemplate createMockFieldTemplate(Long id) {
        return FieldTemplate.builder()
                .id(id)
                .name("mockField")
                .description("Mock field")
                .javaType(JavaType.STRING)
                .expression("mock.expression")
                .isMandatory(false)
                .build();
    }

    private SignalTemplate mockFullSignalTemplate() {
        return SignalTemplate.builder()
                .id(1L)
                .type(TRANSACTION_SIGNAL)
                .name(TRANSACTION_SIGNAL_NAME)
                .description(TRANSACTION_SIGNAL_NAME + " signal template")
                .platformId(TRANSACTION_PLATFORM_ID)
                .fields(Set.copyOf(mockEnrichmentResult.fields()))
                .build();
    }

}
