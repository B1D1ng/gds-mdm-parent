package com.ebay.behavior.gds.mdm.common.model;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public interface Model extends WithId {

    String ID = "id";

    String REVISION = "revision";
    String COMMA = ",";
    long INVALID_ID = -1L;

    Integer getRevision();

    default List<String> toList(String str) {
        if (StringUtils.isBlank(str)) {
            return List.of();
        }
        return Arrays.stream(str.split(COMMA))
                .map(StringUtils::trim)
                .collect(Collectors.toList());
    }
}
