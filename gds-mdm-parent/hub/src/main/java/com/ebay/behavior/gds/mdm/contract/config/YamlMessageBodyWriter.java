package com.ebay.behavior.gds.mdm.contract.config;

import com.ebay.behavior.gds.mdm.common.model.Auditable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;

@Provider
@Produces(MediaType.TEXT_PLAIN)
@Component
public class YamlMessageBodyWriter implements MessageBodyWriter<Object> {

    private final ObjectMapper mapper;

    public YamlMessageBodyWriter() {
        mapper = new ObjectMapper(YAMLFactory.builder()
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                .enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE)
                .build());

        mapper.addMixIn(Auditable.class, ExcludeFields.class);
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return Auditable.class.isAssignableFrom(type);
    }

    @Override
    @SuppressWarnings("PMD.AvoidUncheckedExceptionsInSignatures")
    public void writeTo(Object obj, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
                        OutputStream entityStream) throws IOException, WebApplicationException {
        try (Writer writer = new OutputStreamWriter(entityStream)) {
            mapper.writeValue(writer, obj);
        }
    }

    @SuppressWarnings("PMD.CommentDefaultAccessModifier")
    public abstract static class ExcludeFields {
        @JsonIgnore
        abstract String getId();

        @JsonIgnore
        abstract String getRevision();

        @JsonIgnore
        abstract String getCreateBy();

        @JsonIgnore
        abstract String getCreateDate();

        @JsonIgnore
        abstract String getUpdateBy();

        @JsonIgnore
        abstract String getUpdateDate();

        @JsonIgnore
        abstract String getOperations();

        @JsonIgnore
        abstract String getContractId();

        @JsonIgnore
        abstract String getContractVersion();

        @JsonIgnore
        abstract String getMetadataId();

        @JsonIgnore
        abstract String getComponentId();

        @JsonSerialize(using = PropertiesSerializer.class)
        abstract String getProperties();
    }

    public static class PropertiesSerializer extends JsonSerializer<String> {
        private final ObjectMapper mapper = new ObjectMapper();

        @Override
        public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeObject(mapper.readValue(value, new TypeReference<Map<String, Object>>() {
            }));
        }
    }
}
