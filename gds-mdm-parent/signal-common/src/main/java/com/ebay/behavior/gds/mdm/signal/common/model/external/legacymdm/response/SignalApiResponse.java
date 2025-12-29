package com.ebay.behavior.gds.mdm.signal.common.model.external.legacymdm.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SignalApiResponse {
    private static final String SUCCESS_MSG = "SUCCESS";
    private Integer code;

    private String msg;

    private Boolean status;

    private SignalPageResponse data;

    public static SignalApiResponse ok(SignalPageResponse data) {
        SignalApiResponse response = new SignalApiResponse();
        response.setCode(200);
        response.setMsg(SUCCESS_MSG);
        response.setStatus(true);
        response.setData(data);
        return response;
    }

    public static SignalApiResponse error(String errorMsg) {
        SignalApiResponse response = new SignalApiResponse();
        response.setCode(500);
        response.setMsg(errorMsg);
        response.setStatus(false);
        return response;
    }
}