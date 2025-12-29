package com.ebay.behavior.gds.mdm.udf.common.model;

import com.ebay.behavior.gds.mdm.common.model.Metadata;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.val;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Data
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@ToString
@EqualsAndHashCode
public class UdcUdf extends MetadataUdf implements Metadata {

    private Set<UdcUdfStub> stubs;

    @Override
    public Map<String, Object> toMetadataMap(ObjectMapper objectMapper) {
        val udfStubs = getStubs();
        validateAssociationsNotNull(udfStubs);

        val udfStubEntities = udfStubs.stream()
                .map(stub -> ((Metadata) stub).toMetadataMap(objectMapper))
                .toList();

        val dst = toMap(objectMapper, this);
        dst.put("stubs", udfStubEntities);
        dst.put("udfId", getUdfId());
        dst.remove("id"); // remove default pk of EntityType as UDC defined PK as udfId
        computeTimestamps(dst, this);
        dst.values().removeIf(Objects::isNull);
        return dst;
    }
}
