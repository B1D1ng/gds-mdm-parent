package com.ebay.behavior.gds.mdm.contract.service;

import com.ebay.behavior.gds.mdm.contract.model.BesSource;
import com.ebay.behavior.gds.mdm.contract.repository.BesSourceRepository;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class BesSourceService
        extends AbstractStreamingComponentService<BesSource> {

    @Autowired
    @Getter
    private BesSourceRepository repository;

    @Override
    protected Class<BesSource> getModelType() {
        return BesSource.class;
    }
}
