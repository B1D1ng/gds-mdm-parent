package com.ebay.behavior.gds.mdm.common.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.domain.Sort;

import java.util.List;

// Needed to deserialize a Sort object (under EsPage)
@SuppressWarnings("PMD.UnusedFormalParameter")
public class SortMixin {

    @JsonCreator
    public SortMixin(@JsonProperty("orders") List<Sort.Order> orders) {
    }
}
