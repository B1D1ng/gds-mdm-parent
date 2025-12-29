package com.ebay.behavior.gds.mdm.signal.common.model;

import com.ebay.behavior.gds.mdm.common.model.AbstractAuditable;
import com.ebay.behavior.gds.mdm.common.model.JavaType;
import com.ebay.behavior.gds.mdm.common.serde.LazyObjectSerializer;
import com.ebay.behavior.gds.mdm.common.util.ResourceUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.StringUtils;

import java.util.Set;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@SuperBuilder(toBuilder = true)
@ToString(exclude = "events")
@EqualsAndHashCode(callSuper = true, exclude = "events")
@Entity
@Table(name = "template_question")
public class TemplateQuestion extends AbstractAuditable {

    @NotBlank
    @Column(name = "question")
    private String question;

    @Column(name = "description")
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "answer_java_type")
    private JavaType answerJavaType;

    @Column(name = "answer_property_name")
    private String answerPropertyName;

    @Enumerated(EnumType.STRING)
    @Column(name = "answer_property_placeholder")
    private AnswerPropertyPlaceholder answerPropertyPlaceholder;

    @Column(name = "answer_property_setter_class")
    private String answerPropertySetterClass;

    // An answer not stored in the database, since we reuse this type for both: storing user questions and passing back user answers.
    // For the second flow we store the answer under the relevant property of an UnstagedEvent, and original question record is unaffected.
    @Transient
    private String answer;

    @NotNull
    @Column(name = "is_list")
    private Boolean isList;

    @NotNull
    @Column(name = "is_mandatory")
    private Boolean isMandatory;

    @JsonSerialize(using = LazyObjectSerializer.class)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "template_question_event_map",
            joinColumns = @JoinColumn(name = "question_id", referencedColumnName = ID, insertable = false, updatable = false),
            inverseJoinColumns = @JoinColumn(name = "event_template_id", referencedColumnName = ID, insertable = false, updatable = false)
    )
    private Set<EventTemplate> events;

    @JsonIgnore
    public Object getAnswerObject() {
        String answer = getAnswer();
        if (StringUtils.isBlank(answer)) {
            return null;
        }

        if (!isList) {
            return getAnswerJavaType().convert(answer);
        }

        return ResourceUtils.csvStringToSet(answer).stream()
                .map(StringUtils::trim)
                .map(getAnswerJavaType()::convert)
                .collect(Collectors.toSet());
    }

    @JsonIgnore
    public TemplateQuestion withId(Long id) {
        return this.toBuilder().id(id).build();
    }

    @JsonIgnore
    public TemplateQuestion withRevision(Integer revision) {
        return this.toBuilder().revision(revision).build();
    }
}
