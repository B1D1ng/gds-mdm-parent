package com.ebay.behavior.gds.mdm.contract.service;

import com.ebay.behavior.gds.mdm.contract.model.KafkaSink;
import com.ebay.behavior.gds.mdm.contract.repository.KafkaSinkRepository;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class KafkaSinkService
        extends AbstractStreamingComponentService<KafkaSink> {

    @Autowired
    @Getter
    private KafkaSinkRepository repository;

    @Override
    protected Class<KafkaSink> getModelType() {
        return KafkaSink.class;
    }
}
