package com.ebay.behavior.gds.mdm.common.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class EsPage<T> {

    @Valid
    @NotNull
    private final EsPageable pageable;

    private final List<T> content;

    private final long totalElements;

    @JsonCreator
    public EsPage(EsPageable pageable, List<T> content, long totalElements) {
        Validate.isTrue(Objects.nonNull(pageable), "Pageable must not be null");
        if (Objects.nonNull(content)) {
            Validate.isTrue(content.size() <= totalElements, "Content size must be less than or equal to total elements");
        } else {
            Validate.isTrue(totalElements == 0, "Content size must be 0 if content is null");
        }

        this.pageable = pageable;
        this.content = content;
        this.totalElements = totalElements;
    }

    public EsPage(EsPageable pageable, List<T> content) {
        this(pageable, content, Optional.ofNullable(content).map(List::size).orElse(0));
    }

    public int getNumberOfElements() {
        return Optional.ofNullable(content).map(List::size).orElse(0);
    }

    public void forEach(Consumer<? super T> action) {
        CollectionUtils.emptyIfNull(content).forEach(action);
    }
}
