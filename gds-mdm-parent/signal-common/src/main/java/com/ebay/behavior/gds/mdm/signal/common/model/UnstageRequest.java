package com.ebay.behavior.gds.mdm.signal.common.model;

import com.ebay.behavior.gds.mdm.common.model.ExpressionType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnstageRequest {

    @NotNull
    @PositiveOrZero
    private Long parentId;

    @NotNull
    @PositiveOrZero
    private Long srcEntityId;

    @Positive
    private Integer srcVersion;

    @NotBlank
    private String name;

    @NotBlank
    private String description;

    private ExpressionType uuidGeneratorType;

    private String uuidGeneratorExpression;

    private Set<@Valid TemplateQuestion> answeredQuestions;

    public static UnstageRequest ofTemplate(Long parentId, Long templateId, String name, String description, Set<TemplateQuestion> questions) {
        return new UnstageRequest(parentId, templateId, null, name, description, null, null, questions);
    }

    public static UnstageRequest ofUnstaged(Long parentId, Long unstagedId, Integer signalVersion, String name, String description,
                                            Set<TemplateQuestion> questions) {
        return new UnstageRequest(parentId, unstagedId, signalVersion, name, description, null, null, questions);
    }

    @JsonIgnore
    public boolean isVersioned() {
        return srcVersion != null;
    }

    @JsonIgnore
    public boolean isNonVersioned() {
        return !isVersioned();
    }
}
