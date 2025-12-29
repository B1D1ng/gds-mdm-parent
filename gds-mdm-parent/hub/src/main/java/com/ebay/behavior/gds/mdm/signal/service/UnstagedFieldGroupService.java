package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.RevisionedId;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.signal.common.model.FieldGroup;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedField;
import com.ebay.behavior.gds.mdm.signal.repository.UnstagedFieldRepository;
import com.ebay.behavior.gds.mdm.signal.util.FieldGroupUtils;
import com.ebay.behavior.gds.mdm.signal.util.ServiceUtils;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.ebay.behavior.gds.mdm.signal.common.model.Field.EVENT_TYPES;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;

@Service
@Validated
public class UnstagedFieldGroupService {

    @Autowired
    private UnstagedFieldRepository repository;

    @Autowired
    private UnstagedSignalService signalService;

    @Autowired
    private UnstagedFieldService fieldService;

    /**
     * Gets all Fields connected to the signal definition.
     * De-duplicate Fields by a tag.
     */
    @Transactional(readOnly = true)
    public Set<@Valid FieldGroup<UnstagedField>> getAll(@NotNull @Valid VersionedId signalId) {
        return FieldGroupUtils.getAllFieldGroups(signalService.getFields(signalId));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void update(@NotNull @Valid FieldGroup<UnstagedField> fieldGroup) {
        val revisionedIds = fieldGroup.getRevisionedIds();
        Validate.isTrue(CollectionUtils.isNotEmpty(revisionedIds), "ids must not be empty");

        val ids = revisionedIds.stream().map(RevisionedId::getId).collect(toSet());
        val fields = fieldService.findAllById(ids);
        if (CollectionUtils.isEmpty(fields)) {
            throw new DataNotFoundException(UnstagedField.class, ids);
        }

        val idToRevision = revisionedIds.stream()
                .collect(Collectors.toMap(RevisionedId::getId, RevisionedId::getRevision));

        fields.forEach(field -> {
            val eventTypes = field.getEventTypes();
            ServiceUtils.copyModelProperties(fieldGroup, field, Set.of(EVENT_TYPES));
            field.setEventTypes(eventTypes);
            field.setRevision(idToRevision.get(field.getId()));
        });

        repository.saveAll(fields);
    }

    @Deprecated // replaced with getByKey
    @Transactional(readOnly = true)
    public @Valid FieldGroup<UnstagedField> getByTag(@NotNull @Valid VersionedId signalId, @NotBlank String tag) {
        return findByTag(signalId, tag)
                .orElseThrow(() -> new DataNotFoundException(FieldGroup.class, tag));
    }

    @Transactional(readOnly = true)
    public @Valid FieldGroup<UnstagedField> getByKey(@NotNull @Valid VersionedId signalId, @NotBlank String key) {
        return findByKey(signalId, key)
                .orElseThrow(() -> new DataNotFoundException(FieldGroup.class, key));
    }

    @Deprecated // replaced with findByKey
    @Transactional(readOnly = true)
    public Optional<@Valid FieldGroup<UnstagedField>> findByTag(@NotNull @Valid VersionedId signalId, @NotBlank String tag) {
        val fields = signalService.getFields(signalId);
        Validate.isTrue(Objects.nonNull(fields), "fields cannot be null");
        Validate.notBlank(tag, "tag cannot be blank");

        val fieldsByTag = fields.stream().collect(groupingBy(UnstagedField::getTag));

        if (!fieldsByTag.containsKey(tag)) {
            return Optional.empty();
        }

        return Optional.of(new FieldGroup<>(fieldsByTag.get(tag)));
    }

    @Transactional(readOnly = true)
    public Optional<@Valid FieldGroup<UnstagedField>> findByKey(@NotNull @Valid VersionedId signalId, @NotBlank String key) {
        val fields = signalService.getFields(signalId);
        Validate.isTrue(Objects.nonNull(fields), "fields cannot be null");
        Validate.notBlank(key, "key cannot be blank");

        val fieldsByKey = fields.stream().collect(groupingBy(UnstagedField::getGroupKey));

        if (!fieldsByKey.containsKey(key)) {
            return Optional.empty();
        }

        return Optional.of(new FieldGroup<>(fieldsByKey.get(key)));
    }

    @Deprecated // replaced with deleteByKey
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteByTag(@NotNull @Valid VersionedId signalId, @NotBlank String tag) {
        val fieldGroup = getByTag(signalId, tag);
        Validate.isTrue(!fieldGroup.getIsMandatory(), "Cannot delete a mandatory field group with tag: %s", tag);

        val ids = fieldGroup.getRevisionedIds().stream()
                .map(RevisionedId::getId)
                .collect(toSet());
        fieldService.delete(ids);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteByKey(@NotNull @Valid VersionedId signalId, @NotBlank String key) {
        val fieldGroup = getByKey(signalId, key);
        Validate.isTrue(!fieldGroup.getIsMandatory(), "Cannot delete a mandatory field group with key: %s", key);

        val ids = fieldGroup.getRevisionedIds().stream()
                .map(RevisionedId::getId)
                .collect(toSet());
        fieldService.delete(ids);
    }
}