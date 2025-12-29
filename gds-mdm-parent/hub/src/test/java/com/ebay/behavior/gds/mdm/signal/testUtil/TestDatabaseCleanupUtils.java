package com.ebay.behavior.gds.mdm.signal.testUtil;

import com.ebay.behavior.gds.mdm.signal.repository.AttributeTemplateRepository;
import com.ebay.behavior.gds.mdm.signal.repository.EventTemplateRepository;
import com.ebay.behavior.gds.mdm.signal.repository.FieldTemplateRepository;
import com.ebay.behavior.gds.mdm.signal.repository.PlanRepository;
import com.ebay.behavior.gds.mdm.signal.repository.SignalTemplateRepository;
import com.ebay.behavior.gds.mdm.signal.repository.StagedAttributeRepository;
import com.ebay.behavior.gds.mdm.signal.repository.StagedEventRepository;
import com.ebay.behavior.gds.mdm.signal.repository.StagedFieldRepository;
import com.ebay.behavior.gds.mdm.signal.repository.StagedSignalRepository;
import com.ebay.behavior.gds.mdm.signal.repository.TemplateQuestionRepository;
import com.ebay.behavior.gds.mdm.signal.repository.UnstagedAttributeRepository;
import com.ebay.behavior.gds.mdm.signal.repository.UnstagedEventRepository;
import com.ebay.behavior.gds.mdm.signal.repository.UnstagedFieldRepository;
import com.ebay.behavior.gds.mdm.signal.repository.UnstagedSignalRepository;
import com.ebay.behavior.gds.mdm.signal.repository.audit.EventTemplateHistoryRepository;
import com.ebay.behavior.gds.mdm.signal.repository.audit.PlanHistoryRepository;
import com.ebay.behavior.gds.mdm.signal.repository.audit.SignalTemplateHistoryRepository;
import com.ebay.behavior.gds.mdm.signal.repository.audit.UnstagedAttributeHistoryRepository;
import com.ebay.behavior.gds.mdm.signal.repository.audit.UnstagedEventHistoryRepository;
import com.ebay.behavior.gds.mdm.signal.repository.audit.UnstagedFieldHistoryRepository;
import com.ebay.behavior.gds.mdm.signal.repository.audit.UnstagedSignalHistoryRepository;
import com.ebay.behavior.gds.mdm.signal.repository.manyToMany.EventTypeFieldTemplateMappingRepository;
import com.ebay.behavior.gds.mdm.signal.repository.manyToMany.FieldAttributeTemplateMappingRepository;
import com.ebay.behavior.gds.mdm.signal.repository.manyToMany.SignalEventTemplateMappingRepository;
import com.ebay.behavior.gds.mdm.signal.repository.manyToMany.StagedFieldAttributeMappingRepository;
import com.ebay.behavior.gds.mdm.signal.repository.manyToMany.StagedSignalEventMappingRepository;
import com.ebay.behavior.gds.mdm.signal.repository.manyToMany.TemplateQuestionEventMappingRepository;
import com.ebay.behavior.gds.mdm.signal.repository.manyToMany.UnstagedFieldAttributeMappingRepository;
import com.ebay.behavior.gds.mdm.signal.repository.manyToMany.UnstagedSignalEventMappingRepository;
import com.ebay.behavior.gds.mdm.signal.repository.migration.SignalMigrationJobRepository;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;

/**
 * This is not a test, but a utility class used to delete plan and screen in dev db.
 * It runs only manually.
 */
@Disabled
@ActiveProfiles(IT) // remove this line to run with dev profile
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@SuppressWarnings({"PMD.JUnit5TestShouldBePackagePrivate", "PMD.LongVariable"})
public class TestDatabaseCleanupUtils {

    @Autowired
    private EventTypeFieldTemplateMappingRepository eventTypeFieldTemplateMapRepository;

    @Autowired
    private FieldAttributeTemplateMappingRepository fieldAttributeTemplateMapRepository;

    @Autowired
    private SignalEventTemplateMappingRepository signalEventTemplateMapRepository;

    @Autowired
    private TemplateQuestionEventMappingRepository templateQuestionEventMapRepository;

    @Autowired
    private TemplateQuestionRepository templateQuestionRepository;

    @Autowired
    private FieldTemplateRepository fieldTemplateRepository;

    @Autowired
    private AttributeTemplateRepository attributeTemplateRepository;

    @Autowired
    private SignalTemplateRepository signalTemplateRepository;

    @Autowired
    private EventTemplateRepository eventTemplateRepository;

    @Autowired
    private UnstagedFieldAttributeMappingRepository unstagedFieldAttributeMappingRepository;

    @Autowired
    private UnstagedSignalEventMappingRepository unstagedSignalEventMappingRepository;

    @Autowired
    private UnstagedFieldRepository unstagedFieldRepository;

    @Autowired
    private UnstagedAttributeRepository unstagedAttributeRepository;

    @Autowired
    private UnstagedEventRepository unstagedEventRepository;

    @Autowired
    private UnstagedSignalRepository unstagedSignalRepository;

    @Autowired
    private StagedFieldAttributeMappingRepository stagedFieldAttributeMappingRepository;

    @Autowired
    private StagedSignalEventMappingRepository stagedSignalEventMappingRepository;

    @Autowired
    private StagedFieldRepository stagedFieldRepository;

    @Autowired
    private StagedAttributeRepository stagedAttributeRepository;

    @Autowired
    private StagedEventRepository stagedEventRepository;

    @Autowired
    private StagedSignalRepository stagedSignalRepository;

    @Autowired
    private SignalMigrationJobRepository signalMigrationJobRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private PlanHistoryRepository planHistoryRepository;

    @Autowired
    private SignalTemplateHistoryRepository signalTemplateHistoryRepository;

    @Autowired
    private UnstagedSignalHistoryRepository unstagedSignalHistoryRepository;

    @Autowired
    private UnstagedEventHistoryRepository unstagedEventHistoryRepository;

    @Autowired
    private EventTemplateHistoryRepository eventTemplateHistoryRepository;

    @Autowired
    private UnstagedFieldHistoryRepository unstagedFieldHistoryRepository;

    @Autowired
    private UnstagedAttributeHistoryRepository unstagedAttributeHistoryRepository;

    /**
     * Deletes all test plan and screen in dev db.
     * Do not run, until you know what you are doing!
     */
    @Test
    @Disabled
    void deleteAll() {
        // History tables without templates
        unstagedFieldHistoryRepository.deleteAllInBatch();
        unstagedAttributeHistoryRepository.deleteAllInBatch();
        unstagedEventHistoryRepository.deleteAllInBatch();
        unstagedSignalHistoryRepository.deleteAllInBatch();
        planHistoryRepository.deleteAllInBatch();

        // Template tables
        boolean deleteTemplates = false;
        if (deleteTemplates) {
            signalTemplateHistoryRepository.deleteAllInBatch();
            eventTemplateHistoryRepository.deleteAllInBatch();
            eventTypeFieldTemplateMapRepository.deleteAllInBatch();
            fieldAttributeTemplateMapRepository.deleteAllInBatch();
            signalEventTemplateMapRepository.deleteAllInBatch();
            templateQuestionEventMapRepository.deleteAllInBatch();
            templateQuestionRepository.deleteAllInBatch();
            fieldTemplateRepository.deleteAllInBatch();
            attributeTemplateRepository.deleteAllInBatch();
            eventTemplateRepository.deleteAllInBatch();
            signalTemplateRepository.deleteAllInBatch();
        }

        // Staged tables
        stagedSignalEventMappingRepository.deleteAllInBatch();
        stagedFieldAttributeMappingRepository.deleteAllInBatch();
        stagedFieldRepository.deleteAllInBatch();
        stagedAttributeRepository.deleteAllInBatch();
        stagedEventRepository.deleteAllInBatch();
        stagedSignalRepository.deleteAllInBatch();

        // Unstaged tables
        unstagedSignalEventMappingRepository.deleteAllInBatch();
        unstagedFieldAttributeMappingRepository.deleteAllInBatch();
        unstagedFieldRepository.deleteAllInBatch();
        unstagedAttributeRepository.deleteAllInBatch();
        unstagedEventRepository.deleteAllInBatch();
        unstagedSignalRepository.deleteAllInBatch();

        signalMigrationJobRepository.deleteAllInBatch();
        planRepository.deleteAllInBatch();
    }
}

/*
-- History tables
plan_history

event_history
signal_history
field_history
attribute_history

-- Template tables
event_template_history
signal_template_history
field_attribute_template_map
signal_event_template_map
template_question_event_map
event_type_field_template_map

template_question
field_template
attribute_template
event_template
signal_template

-- Staged tables
staged_field_attribute_map
staged_signal_event_map
staged_field
staged_attribute
staged_event
staged_signal

-- Unstaged tables
field_attribute_map
signal_event_map
field
attribute
event
signal_definition

signal_migration_job
plan
 */
