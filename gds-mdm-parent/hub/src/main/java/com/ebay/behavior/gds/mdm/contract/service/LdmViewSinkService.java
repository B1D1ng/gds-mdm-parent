package com.ebay.behavior.gds.mdm.contract.service;

import com.ebay.behavior.gds.mdm.contract.model.LdmViewSink;
import com.ebay.behavior.gds.mdm.contract.repository.LdmViewSinkRepostory;

import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class LdmViewSinkService extends AbstractComponentService<LdmViewSink> {

    @Getter(AccessLevel.PROTECTED)
    private final Class<LdmViewSink> modelType = LdmViewSink.class;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private LdmViewSinkRepostory repostory;

    @Override
    protected JpaRepository<LdmViewSink, Long> getRepository() {
        return repostory;
    }
}
