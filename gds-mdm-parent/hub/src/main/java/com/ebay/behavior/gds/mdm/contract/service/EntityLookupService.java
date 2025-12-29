package com.ebay.behavior.gds.mdm.contract.service;

import com.ebay.behavior.gds.mdm.commonSvc.service.AbstractLookupService;
import com.ebay.behavior.gds.mdm.contract.model.EntityLookup;
import com.ebay.behavior.gds.mdm.contract.repository.EntityRepository;

import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class EntityLookupService extends AbstractLookupService<EntityLookup> {

    @Getter(AccessLevel.PROTECTED)
    private final Class<EntityLookup> modelType = EntityLookup.class;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private EntityRepository repository;
}
