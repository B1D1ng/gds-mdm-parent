package com.ebay.behavior.gds.mdm.udf.util;

import com.ebay.behavior.gds.mdm.udf.common.model.UdcUdf;
import com.ebay.behavior.gds.mdm.udf.common.model.UdcUdfStub;
import com.ebay.behavior.gds.mdm.udf.common.model.Udf;
import com.ebay.behavior.gds.mdm.udf.common.model.UdfStub;

import lombok.experimental.UtilityClass;

import java.util.HashSet;
import java.util.Set;

@UtilityClass
public class UdfMetadataUtils {
    public static UdcUdf convert(Udf udf) {
        Set<UdcUdfStub> udcUdfStubs = new HashSet<>();
        for (UdfStub udfStub : udf.getUdfStubs()) {
            udcUdfStubs.add(
                    convert(udfStub)
            );
        }

        return UdcUdf.builder()
                .udfId(udf.getId())
                .udfName(udf.getName())
                .description(udf.getDescription())
                .language(udf.getLanguage())
                .type(udf.getType())
                .code(udf.getCode())
                .parameters(udf.getParameters())
                .domain(udf.getDomain())
                .owners(udf.getOwners())
                .stubs(udcUdfStubs)
                .createBy(udf.getCreateBy())
                .updateBy(udf.getUpdateBy())
                .createDate(udf.getCreateDate())
                .updateDate(udf.getUpdateDate())
                .functionSourceType(udf.getFunctionSourceType())
                .build();
    }

    public static UdcUdfStub convert(UdfStub udfStub) {
        return UdcUdfStub.builder()
                .stubId(udfStub.getId())
                .stubName(udfStub.getStubName())
                .description(udfStub.getDescription())
                .language(udfStub.getLanguage())
                .stubCode(udfStub.getStubCode())
                .stubParameters(udfStub.getStubParameters())
                .stubRuntimeContext(udfStub.getStubRuntimeContext())
                .stubType(udfStub.getStubType())
                .build();
    }
}
