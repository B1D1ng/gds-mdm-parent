package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.commonSvc.service.AbstractLookupService;
import com.ebay.behavior.gds.mdm.signal.common.model.EventTypeLookup;
import com.ebay.behavior.gds.mdm.signal.repository.EventTypeRepository;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static com.ebay.behavior.gds.mdm.signal.util.CacheConstants.EVENT_TYPE_CACHE;

@Service
@Validated
public class EventTypeLookupService extends AbstractLookupService<EventTypeLookup> {

    @Getter(AccessLevel.PROTECTED)
    private final Class<EventTypeLookup> modelType = EventTypeLookup.class;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private EventTypeRepository repository;

    @Override
    @Cacheable(value = EVENT_TYPE_CACHE, sync = true)
    @Transactional(readOnly = true)
    public EventTypeLookup getByName(@NotBlank String name) {
        return findByName(name).orElseThrow(() -> new DataNotFoundException(
                String.format("EventTypeLookup name=%s doesn't found", name)));
    }
}
