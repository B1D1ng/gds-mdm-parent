package com.ebay.behavior.gds.mdm.signal.util;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.Auditable;
import com.ebay.behavior.gds.mdm.common.model.VersionedAuditable;
import com.ebay.behavior.gds.mdm.common.model.audit.AbstractHistoryAuditable;
import com.ebay.behavior.gds.mdm.common.model.audit.AuditLogParams;
import com.ebay.behavior.gds.mdm.common.model.audit.AuditRecord;
import com.ebay.behavior.gds.mdm.common.model.audit.ChangeLog;
import com.ebay.behavior.gds.mdm.common.model.audit.ChangeType;
import com.ebay.behavior.gds.mdm.common.model.audit.HistoryAuditable;
import com.ebay.behavior.gds.mdm.common.model.audit.VersionedHistoryAuditable;
import com.ebay.behavior.gds.mdm.commonSvc.repository.HistoryRepository;
import com.ebay.behavior.gds.mdm.commonSvc.repository.VersionedHistoryRepository;
import com.ebay.behavior.gds.mdm.signal.common.model.StagedAttribute;
import com.ebay.behavior.gds.mdm.signal.common.model.StagedEvent;
import com.ebay.behavior.gds.mdm.signal.common.model.StagedField;
import com.ebay.behavior.gds.mdm.signal.common.model.StagedSignal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.val;
import org.apache.commons.lang3.Validate;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Change;
import org.javers.core.diff.Diff;
import org.javers.core.diff.DiffBuilder;
import org.javers.core.diff.changetype.InitialValueChange;
import org.javers.core.diff.changetype.NewObject;
import org.javers.core.diff.changetype.ObjectRemoved;
import org.javers.core.diff.changetype.ReferenceChange;
import org.javers.core.diff.changetype.TerminalValueChange;
import org.javers.core.diff.changetype.ValueChange;
import org.javers.core.diff.changetype.map.MapChange;
import org.javers.core.metamodel.clazz.EntityDefinition;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static com.ebay.behavior.gds.mdm.common.model.Model.REVISION;
import static com.ebay.behavior.gds.mdm.common.model.audit.ChangeType.DELETED;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.NAME;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.TAG;
import static java.util.Objects.nonNull;

@UtilityClass
public class AuditUtils {

    public static final String NEW_OBJECT = "NewObject";
    public static final String OBJECT_REMOVED = "ObjectRemoved";
    public static final String VALUE_CHANGE = "ValueChange";
    public static final String CHANGE_TYPE = "changeType";
    public static final String ENTITY_TYPE = "entityType";
    public static final String ENTITY_ID = "entityId";
    public static final String LEFT = "left";
    public static final String RIGHT = "right";
    public static final String PROPERTY_NAME = "propertyName";
    public static final String CHANGES = "changes";

    public static final Javers JAVERS;

    private static final Predicate<Change> CHANGE_PREDICATE = change ->
            !(change instanceof InitialValueChange)
                    && !(change instanceof ReferenceChange)
                    && !(change instanceof TerminalValueChange)
                    && !(change instanceof MapChange);

    static {
        JAVERS = getJaversBuilder().build();
    }

    private static JaversBuilder getJaversBuilder() {
        val builder = JaversBuilder.javers();
        val reflections = new Reflections("com.ebay.behavior.gds.mdm.app.model.audit", Scanners.SubTypes);
        val subTypes = reflections.getSubTypesOf(AbstractHistoryAuditable.class).stream()
                .filter(type -> !type.getSimpleName().startsWith("Abstract"))
                .toList();

        subTypes.forEach(type -> builder.registerEntity(new EntityDefinition(type, "originalId")));

        builder.registerEntity(new EntityDefinition(StagedAttribute.class, TAG));
        builder.registerEntity(new EntityDefinition(StagedField.class, "fieldKey"));
        builder.registerEntity(new EntityDefinition(StagedEvent.class, NAME));
        builder.registerValueObject(StagedSignal.class);

        return builder;
    }

    @SneakyThrows
    public static <A extends Auditable> JsonNode serializeAuditRecords(ObjectMapper objectMapper, List<AuditRecord<A>> auditRecords) {
        val arrayNode = objectMapper.createArrayNode();

        for (val auditRecord : auditRecords) {
            // Serialize the whole AuditRecord excluding "Diff changes"
            val diff = auditRecord.getDiff();
            auditRecord.setDiff(null);
            val auditRecordJsonNode = objectMapper.valueToTree(auditRecord);

            if (nonNull(diff)) {
                // Custom serialization of the "Diff changes"
                ArrayNode diffJsonNode = convertDiffToJson(diff, objectMapper);

                // Add the "diff" node to the auditRecordJsonNode
                ((ObjectNode) auditRecordJsonNode).set(CHANGES, diffJsonNode);
            }

            // Add the modified auditRecordJsonNode to the array
            arrayNode.add(auditRecordJsonNode);
        }

        return arrayNode;
    }

    /**
     * Converts the Javers Diff object to a custom JSON object
     * Only process NewObject, ObjectRemoved, and ValueChange changes.
     */
    public ArrayNode convertDiffToJson(Diff diff, ObjectMapper objectMapper) {
        ArrayNode resultNode = objectMapper.createArrayNode();

        for (Change change : diff.getChanges()) {
            ObjectNode changeNode = objectMapper.createObjectNode();

            switch (change.getClass().getSimpleName()) {
                case NEW_OBJECT:
                    setNewObjectChange(changeNode, (NewObject) change, objectMapper);
                    break;
                case OBJECT_REMOVED:
                    setObjectRemovedChange(changeNode, (ObjectRemoved) change, objectMapper);
                    break;
                case VALUE_CHANGE:
                    setValueChange(changeNode, (ValueChange) change);
                    break;
            }

            if (!changeNode.isEmpty()) {
                changeNode.put(ENTITY_TYPE, change.getAffectedGlobalId().getTypeName());
                changeNode.set(ENTITY_ID, objectMapper.valueToTree(change.getAffectedObject()).get("id"));
                resultNode.add(changeNode);
            }
        }

        return resultNode;
    }

    private void setNewObjectChange(ObjectNode changeNode, NewObject object, ObjectMapper objectMapper) {
        changeNode.put(CHANGE_TYPE, NEW_OBJECT);
        changeNode.set(LEFT, null);
        changeNode.set(RIGHT, objectMapper.valueToTree(object.getAffectedObject()));
    }

    private void setObjectRemovedChange(ObjectNode changeNode, ObjectRemoved object, ObjectMapper objectMapper) {
        changeNode.put(CHANGE_TYPE, OBJECT_REMOVED);
        changeNode.set(LEFT, objectMapper.valueToTree(object.getAffectedObject()));
        changeNode.set(RIGHT, null);
    }

    private void setValueChange(ObjectNode changeNode, ValueChange object) {
        changeNode.put(CHANGE_TYPE, VALUE_CHANGE);
        changeNode.put(PROPERTY_NAME, object.getPropertyName());
        changeNode.put(LEFT, object.getLeft() == null ? null : object.getLeft().toString());
        changeNode.put(RIGHT, object.getRight() == null ? null : object.getRight().toString());
    }

    /**
     * This is a workaround for the issue with Javers deserialization of diff object.
     * Javers uses its own JSON converter, which has issues with Timestamp deserialization.
     * But without JAVERS converter we cannot deserialize the diff object correctly.
     * Deserialize a list of audit records from JSON string using two ways of deserialization: with objectMapper, and with JAVERS for diff object.
     */
    @SneakyThrows
    public <A extends Auditable> List<AuditRecord<A>> deserializeAuditRecords(String json, ObjectMapper objectMapper, Class<A> historyType) {
        var rootNode = objectMapper.readTree(json);
        if (!rootNode.isArray()) {
            throw new IllegalArgumentException("The provided JSON is not an array");
        }

        List<ChangeLog> changes = null;
        val typeFactory = objectMapper.getTypeFactory();
        List<AuditRecord<A>> auditRecords = new ArrayList<>();

        for (var node : rootNode) {
            var objectNode = (ObjectNode) node;
            if (objectNode.has(CHANGES)) {
                var changeJson = objectNode.get(CHANGES).toString();
                changes = objectMapper.readValue(changeJson, typeFactory.constructCollectionType(List.class, ChangeLog.class));
                objectNode.putNull(CHANGES);
            }

            AuditRecord<A> record = objectMapper.treeToValue(objectNode, typeFactory.constructParametricType(AuditRecord.class, historyType));
            record.setChanges(changes);
            auditRecords.add(record);
        }

        return auditRecords;
    }

    @SneakyThrows
    public static <M extends VersionedAuditable, H extends VersionedHistoryAuditable> H toHistoryRecord(
            M model, ChangeType changeType, String changeReason, Class<H> clazz) {
        val record = toHistoryRecord((Auditable) model, changeType, changeReason, clazz);
        record.setOriginalVersion(model.getVersion());
        return record;
    }

    @SneakyThrows
    public static <M extends Auditable, H extends HistoryAuditable> H toHistoryRecord(M model, ChangeType changeType, String changeReason, Class<H> clazz) {
        H record = clazz.getDeclaredConstructor().newInstance();
        ServiceUtils.copyAuditableProperties(model, record, Set.of(REVISION));

        record.setOriginalId(model.getId());
        record.setOriginalRevision(model.getRevision());
        record.setOriginalCreateDate(model.getCreateDate());
        record.setOriginalUpdateDate(model.getUpdateDate());
        record.setChangeType(changeType);
        record.setChangeReason(changeReason);

        return record;
    }

    public static <M extends Auditable, H extends HistoryAuditable> M saveAndAudit(
            M model, JpaRepository<M, ?> repo, JpaRepository<H, ?> historyRepo, ChangeType changeType, String changeReason, Class<H> clazz) {
        validate(model, repo, historyRepo, changeType, clazz);

        val persisted = repo.saveAndFlush(model);
        val historyRecord = toHistoryRecord(persisted, changeType, changeReason, clazz);
        historyRepo.save(historyRecord);

        return persisted;
    }

    public static <M extends VersionedAuditable, H extends VersionedHistoryAuditable> M saveAndAudit(
            M model, JpaRepository<M, ?> repo, JpaRepository<H, ?> historyRepo, ChangeType changeType, String changeReason, Class<H> clazz) {
        validate(model, repo, historyRepo, changeType, clazz);

        val persisted = repo.saveAndFlush(model);
        val historyRecord = toHistoryRecord(persisted, changeType, changeReason, clazz);
        historyRepo.save(historyRecord);

        return persisted;
    }

    public static <M extends Auditable, H extends HistoryAuditable> void deleteAndAudit(
            M model, JpaRepository<M, Long> repo, HistoryRepository<H> historyRepo, Class<H> clazz) {
        val historyRecord = toHistoryRecord(model, DELETED, null, clazz);
        repo.deleteById(model.getId());
        historyRepo.save(historyRecord);
    }

    public static <M extends VersionedAuditable, H extends VersionedHistoryAuditable, ID> void deleteAndAudit(
            M model, ID modelId, JpaRepository<M, ID> repo, VersionedHistoryRepository<H> historyRepo, Class<H> clazz) {
        val historyRecord = toHistoryRecord(model, DELETED, null, clazz);
        repo.deleteById(modelId);
        historyRepo.save(historyRecord);
    }

    public static <H extends HistoryAuditable> List<AuditRecord<H>> getAuditLog(HistoryRepository<H> historyRepo, AuditLogParams params) {
        return switch (params.getMode()) {
            case BASIC -> getBasicAuditLog(historyRepo, params);
            case FULL -> getFullAuditLog(historyRepo, params);
        };
    }

    private static <H extends HistoryAuditable> List<AuditRecord<H>> getBasicAuditLog(HistoryRepository<H> historyRepo, AuditLogParams params) {
        val id = params.getId();
        return historyRepo.findByOriginalId(id).stream()
                .map(record -> new AuditRecord<H>(record))
                .toList();
    }

    private static <H extends HistoryAuditable> List<AuditRecord<H>> getFullAuditLog(HistoryRepository<H> historyRepo, AuditLogParams params) {
        val id = params.getId();
        val records = historyRepo.findByOriginalId(id);
        records.forEach(record -> record.setId(record.getOriginalId())); // a trick to help Javers to treat all records as the same entity
        records.sort(Comparator.comparingInt(HistoryAuditable::getOriginalRevision));

        if (records.isEmpty()) {
            throw new DataNotFoundException("No history records found with the provided id.");
        }

        val auditRecords = new ArrayList<AuditRecord<H>>();
        for (int i = 0; i < records.size(); i++) {
            val curr = records.get(i);
            val prev = i == 0 ? null : records.get(i - 1);
            val diff = getChanges(prev, curr);
            auditRecords.add(new AuditRecord<>(prev, diff, curr));
        }

        return auditRecords;
    }

    public static <H extends Auditable> Diff getChanges(H prev, H curr) {
        val diff = JAVERS.compare(prev, curr);
        var valueChanges = diff.getChanges(CHANGE_PREDICATE);
        return new DiffBuilder().addChanges(valueChanges).build();
    }

    private static <M extends Auditable, H extends HistoryAuditable> void validate(
            M model, JpaRepository<M, ?> repo, JpaRepository<H, ?> historyRepo, ChangeType changeType, Class<H> clazz) {
        Validate.isTrue(nonNull(model), "Model must not be null.");
        Validate.isTrue(nonNull(repo), "Repository must not be null.");
        Validate.isTrue(nonNull(historyRepo), "History repository must not be null.");
        Validate.isTrue(nonNull(changeType), "Change type must not be null.");
        Validate.isTrue(nonNull(clazz), "History record class must not be null.");
    }
}
