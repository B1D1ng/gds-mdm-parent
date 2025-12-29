package com.ebay.behavior.gds.mdm.common.serde;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.hibernate.Hibernate;

import java.io.IOException;

public class LazyObjectSerializer extends StdSerializer<Object> {

    public LazyObjectSerializer() {
        super(Object.class);
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (Hibernate.isInitialized(value)) {
            gen.writeObject(value);
        } else {
            gen.writeNull();
        }
    }
}