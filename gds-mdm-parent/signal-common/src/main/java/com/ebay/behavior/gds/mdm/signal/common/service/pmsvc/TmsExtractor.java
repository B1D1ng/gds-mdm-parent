package com.ebay.behavior.gds.mdm.signal.common.service.pmsvc;

import com.ebay.behavior.gds.mdm.common.model.QueryParam;
import com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion;
import com.ebay.behavior.gds.mdm.common.service.AbstractRestPostClient;
import com.ebay.behavior.gds.mdm.common.service.token.TokenGenerator;
import com.ebay.behavior.gds.mdm.signal.common.model.external.pmsvc.v1.datatype.PmsvcModelV1;
import com.ebay.behavior.gds.mdm.signal.common.model.external.pmsvc.v1.request.TmsSearchRequest;
import com.ebay.behavior.gds.mdm.signal.common.model.external.pmsvc.v1.response.ListResponse;

import lombok.Getter;
import org.apache.commons.lang3.NotImplementedException;

import java.util.List;

public abstract class TmsExtractor<M extends PmsvcModelV1> extends AbstractRestPostClient {

    @Getter
    private final TokenGenerator tokenGenerator = null; // needed for AbstractRestPostClient logic

    protected static final String PMSVC_GINGER_CLIENT_NAME = "pmsvc";

    public TmsSearchRequest createRequest(String searchTerm, String searchBy, SearchCriterion searchCriterion) {
        return new TmsSearchRequest(searchTerm, searchBy, searchCriterion);
    }

    @Override
    public <T> T get(String path, Class<T> type) {
        throw new NotImplementedException("get(path, type) not implemented");
    }

    @Override
    public <T> T get(String path, List<QueryParam> queryParams, Class<T> type) {
        throw new NotImplementedException("get(path, queryParams, type) not implemented");
    }

    public abstract Class<M> getType();

    public abstract Class<? extends ListResponse<M>> getResponseType();
}