package com.ebay.behavior.gds.mdm.signal.common.model.converter;

import com.ebay.behavior.gds.mdm.common.model.UdcDataSourceType;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class UdcDataSourceTypeConverter implements AttributeConverter<UdcDataSourceType, String> {

    @Override
    public String convertToDatabaseColumn(UdcDataSourceType udcDataSourceType) {
        if (udcDataSourceType == null) {
            return null;
        }
        return udcDataSourceType.getValue();
    }

    @Override
    public UdcDataSourceType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return UdcDataSourceType.fromValue(dbData);
    }
}
