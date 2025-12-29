package com.ebay.behavior.gds.mdm.contract.service;

import com.ebay.behavior.gds.mdm.contract.model.KafkaSource;
import com.ebay.behavior.gds.mdm.contract.repository.KafkaSourceRepository;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class KafkaSourceService
        extends AbstractStreamingComponentService<KafkaSource> {

    @Autowired
    @Getter
    private KafkaSourceRepository repository;

    @Override
    protected Class<KafkaSource> getModelType() {
        return KafkaSource.class;
    }
}
