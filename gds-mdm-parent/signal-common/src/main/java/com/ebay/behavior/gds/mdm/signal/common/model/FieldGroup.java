package com.ebay.behavior.gds.mdm.signal.common.model;

import com.ebay.behavior.gds.mdm.common.model.ExpressionType;
import com.ebay.behavior.gds.mdm.common.model.Model;
import com.ebay.behavior.gds.mdm.common.model.RevisionedId;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class FieldGroup<F extends MetadataField> extends UnstagedFieldProxy implements Model {

    @Valid
    @NotNull
    private VersionedId signalId;

    @NotEmpty
    private Set<@Valid RevisionedId> revisionedIds;

    private String eventTypes;

    @NotBlank
    private String tag;

    @NotBlank
    private String groupKey;

    public FieldGroup(Collection<F> fields) {
        val sortedFields = validate(fields);
        val sample = sortedFields.iterator().next();

        this.signalId = VersionedId.of(sample.getSignalId(), sample.getSignalVersion());
        this.tag = sample.getTag();
        this.groupKey = sample.getGroupKey();

        setName(sample.getName());
        setDescription(sample.getDescription());
        setExpression(sample.getExpression());
        setExpressionType(sample.getExpressionType());
        setIsMandatory(sample.getIsMandatory());
        setRevision(sample.getRevision());

        this.revisionedIds = sortedFields.stream()
                .map(field -> RevisionedId.of(field.getId(), field.getRevision()))
                .collect(toSet());

        this.eventTypes = sortedFields.stream()
                .flatMap(field -> field.toList(field.getEventTypes()).stream())
                .distinct()
                .collect(Collectors.joining(COMMA));
    }

    private List<F> validate(Collection<F> fields) {
        if (CollectionUtils.isEmpty(fields)) {
            throw new IllegalArgumentException("Fields must not be empty");
        }

        // validate fields
        fields.forEach(field -> {
            Validate.notNull(field.getId(), "Field id must not be null");
            Validate.notBlank(field.getTag(), "Field tag must not be blank");
            Validate.notBlank(field.getEventTypes(), "Field eventTypes must not be blank");
        });

        val sample = fields.iterator().next();

        fields.forEach(field -> {
            Validate.isTrue(sample.getTag().equals(field.getTag()), "Field tag must be the same");
            Validate.isTrue(sample.getName().equals(field.getName()), "Field name must be the same");
            Validate.isTrue(sample.getSignalId().equals(field.getSignalId()), "Field signalId must be the same for all group fields");
            Validate.isTrue(sample.getSignalVersion().equals(field.getSignalVersion()), "Field signalVersion must be the same for all group fields");
        });

        return fields.stream()
                .sorted(Comparator.comparingLong(F::getId))
                .toList();
    }

    // must override all getters, since parent type has no validation annotation properties
    @Override
    @NotBlank
    public String getName() {
        return super.getName();
    }

    @Override
    @NotBlank
    public String getDescription() {
        return super.getDescription();
    }

    @Override
    @NotNull
    public ExpressionType getExpressionType() {
        return super.getExpressionType();
    }

    @Override
    @NotNull
    public Boolean getIsMandatory() {
        return super.getIsMandatory();
    }

    @JsonIgnore
    public List<String> getEventTypesAsList() {
        if (Objects.isNull(eventTypes)) {
            return List.of();
        }
        return toList(eventTypes);
    }
}