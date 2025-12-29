package com.ebay.behavior.gds.mdm.common.service;

import com.ebay.behavior.gds.mdm.common.model.Model;

import org.hibernate.id.enhanced.TableGenerator;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

import java.io.Serializable;
import java.util.Properties;

// By now only in use in UnstagedSignal, since legacy UnstagedSignals have ids
public class CustomTableGenerator extends TableGenerator {

    @Override
    public void configure(Type type, Properties params, ServiceRegistry serviceRegistry) {
        params.setProperty(TABLE_PARAM, params.getProperty(TABLE_PARAM, "sequences"));
        params.setProperty(SEGMENT_COLUMN_PARAM, params.getProperty(SEGMENT_COLUMN_PARAM, "sequence_name"));
        params.setProperty(VALUE_COLUMN_PARAM, params.getProperty(VALUE_COLUMN_PARAM, "next_val"));
        super.configure(type, params, serviceRegistry);
    }

    @Override
    public Serializable generate(org.hibernate.engine.spi.SharedSessionContractImplementor session, Object obj) {
        if (!(obj instanceof Model model)) {
            throw new IllegalStateException("CustomTableGenerator can only be used with Model entities");
        }

        Long id = model.getId();
        if (id != null) {
            return id;
        }

        return (Long) super.generate(session, obj);
    }
}
