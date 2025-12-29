package com.ebay.behavior.gds.mdm.commonTestUtil;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

/*
 * This type is needed to be able to convert Page type from json to class.
 * It is not possible with Page (since interface) or PageImpl (since no relevant constructor found).
 * This is a workaround way.
 */
@JsonIgnoreProperties(value = "pageable", ignoreUnknown = true)
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass")
public final class PageHelper<T> extends PageImpl<T> {

    private PageHelper(List<T> content, int number, int size, long totalElements) {
        super(content, PageRequest.of(number, size), totalElements);
    }
}
