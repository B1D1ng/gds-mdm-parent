package com.ebay.behavior.gds.mdm.common.config;

import com.ebay.behavior.gds.mdm.common.model.UdcDataSourceType;

import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
@ConfigurationPropertiesBinding
public class UdcDataSourceTypeConverter implements Converter<String, UdcDataSourceType> {

    @Override
    public UdcDataSourceType convert(String value) {
        return UdcDataSourceType.fromValue(value);
    }
}
