package com.ebay.behavior.gds.mdm.common.model.external.udc;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class EsResponse {

    private String apiVersion;
    private String kind;
    private Data data;

    @lombok.Data
    @NoArgsConstructor
    public static class Data {
        private boolean hasMore;
        private String cursor;
        private List<Entity> entities;
    }
}

