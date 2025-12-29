package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.signal.common.model.AttributeTemplate;
import com.ebay.behavior.gds.mdm.signal.common.model.EventSource;
import com.ebay.behavior.gds.mdm.signal.common.model.HadoopSojEvent;
import com.ebay.behavior.gds.mdm.signal.common.model.SojBusinessTag;
import com.ebay.behavior.gds.mdm.signal.common.model.SojEvent;
import com.ebay.behavior.gds.mdm.signal.common.model.SojPlatformTag;
import com.ebay.behavior.gds.mdm.signal.common.model.SojTag;
import com.ebay.behavior.gds.mdm.signal.common.model.external.pmsvc.v1.datatype.PropertyV1;
import com.ebay.behavior.gds.mdm.signal.model.manyToMany.SojEventTagMapping;
import com.ebay.behavior.gds.mdm.signal.repository.SojBusinessTagRepository;
import com.ebay.behavior.gds.mdm.signal.repository.SojEventRepository;
import com.ebay.behavior.gds.mdm.signal.repository.SojPlatformTagRepository;
import com.ebay.behavior.gds.mdm.signal.repository.manyToMany.SojEventTagMappingRepository;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.Model.COMMA;
import static com.ebay.behavior.gds.mdm.common.model.Model.INVALID_ID;
import static java.util.stream.Collectors.toSet;

@Slf4j
@Service
@Validated
public class BusinessTagWriteService {

    private record CacheWrapper(HashMap<String, PropertyV1> pmsvcTagCache, HashMap<String, SojEvent> sojEventCache,
                                HashMap<String, SojBusinessTag> businessTagCache, HashMap<String, SojPlatformTag> platformTagCache, Set<String> mappingCache) {
    }

    @Autowired
    private EventTemplateService eventTemplateService;

    @Autowired
    private SojEventRepository sojEventRepository;

    @Autowired
    private SojBusinessTagRepository sojBusinessTagRepository;

    @Autowired
    private SojPlatformTagRepository sojPlatformTagRepository;

    @Autowired
    private SojEventTagMappingRepository mappingRepository;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void save(List<HadoopSojEvent> hadoopSojEvents, List<PropertyV1> pmsvcTags) {
        val sojEventsToSave = new HashSet<SojEvent>();
        val businessTagsToSave = new HashMap<String, SojBusinessTag>();
        val platformTagsToSave = new HashMap<String, SojPlatformTag>();
        val mappingsToSave = new HashSet<SojEventTagMapping>();

        val cacheWrapper = initCaches(pmsvcTags);
        val templatePlatformTags = getTemplatePlatformTags();

        for (val hadoopSojEvent : hadoopSojEvents) {
            if (isInvalidSojEvent(hadoopSojEvent)) {
                continue;
            }

            val persistedSojEvent = computeEventIfAbsent(hadoopSojEvent, cacheWrapper.sojEventCache(), sojEventsToSave);
            val sojTags = hadoopSojEvent.getTags().split(COMMA);

            for (val rawTag : sojTags) {
                val hadoopTag = rawTag.startsWith("!") ? rawTag.substring(1) : rawTag;
                val pmsvcTag = cacheWrapper.pmsvcTagCache().get(hadoopTag);

                if (Objects.isNull(pmsvcTag)) {
                    continue;
                }

                // Enrich platform raw hadoop tag, and save it
                if (templatePlatformTags.contains(hadoopTag) || cacheWrapper.platformTagCache().containsKey(hadoopTag)) {
                    computeTagIfAbsent(hadoopTag, pmsvcTag, cacheWrapper.platformTagCache(), platformTagsToSave, SojPlatformTag.class);
                } else {  // Enrich business raw hadoop tag, and save it
                    val businessTag = computeTagIfAbsent(hadoopTag, pmsvcTag, cacheWrapper.businessTagCache(), businessTagsToSave, SojBusinessTag.class);
                    computeMappingIfAbsent(persistedSojEvent, businessTag, cacheWrapper.mappingCache, mappingsToSave);
                }
            }
        }

        saveBatch(sojEventsToSave, businessTagsToSave, platformTagsToSave, mappingsToSave);
        cleanCache(cacheWrapper, hadoopSojEvents, sojEventsToSave, businessTagsToSave, platformTagsToSave, mappingsToSave);
    }

    private CacheWrapper initCaches(List<PropertyV1> pmsvcTags) {
        val pmsvcTagCache = new HashMap<String, PropertyV1>();
        pmsvcTags.forEach(tag -> pmsvcTagCache.put(tag.getSojName(), tag));
        pmsvcTags.clear();

        // Cache business and platform soj events, tags and mappings
        val sojEvents = sojEventRepository.findAll();
        val sojEventCache = new HashMap<String, SojEvent>();
        sojEvents.forEach(event -> sojEventCache.put(event.toKey(), event));
        sojEvents.clear();

        val businessTags = sojBusinessTagRepository.findAll();
        val businessTagCache = new HashMap<String, SojBusinessTag>();
        businessTags.forEach(tag -> businessTagCache.put(tag.getSojName(), tag));
        businessTags.clear();

        val platformTags = sojPlatformTagRepository.findAll();
        val platformTagCache = new HashMap<String, SojPlatformTag>();
        platformTags.forEach(tag -> platformTagCache.put(tag.getSojName(), tag));
        platformTags.clear();

        val mappingCache = mappingRepository.findAllMappings();

        return new CacheWrapper(pmsvcTagCache, sojEventCache, businessTagCache, platformTagCache, mappingCache);
    }

    private Set<String> getTemplatePlatformTags() {
        log.info("Fetching platform tags...");
        return eventTemplateService.findBySource(EventSource.SOJ).stream()
                .flatMap(eventTemplate -> eventTemplateService.getAttributes(eventTemplate.getId()).stream())
                .map(AttributeTemplate::getTag)
                .collect(toSet());
    }

    private SojEvent computeEventIfAbsent(HadoopSojEvent hadoopSojEvent, Map<String, SojEvent> cache, Set<SojEvent> toSave) {
        val key = hadoopSojEvent.toKey();
        if (cache.containsKey(key)) {
            return cache.get(key);
        }

        val sojEvent = new SojEvent(hadoopSojEvent);
        toSave.add(sojEvent);
        return sojEvent;
    }

    private <T extends SojTag> T computeTagIfAbsent(String sojTag, PropertyV1 pmsvcTag, Map<String, T> cache, Map<String, T> toSave, Class<T> type) {
        if (cache.containsKey(sojTag)) {
            return cache.get(sojTag);
        }
        if (toSave.containsKey(sojTag)) {
            return toSave.get(sojTag);
        }
        try {
            Constructor<T> constructor = type.getConstructor(PropertyV1.class);
            T tag = constructor.newInstance(pmsvcTag);
            toSave.put(sojTag, tag);
            return tag;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to create tag instance", ex);
        }
    }

    private void computeMappingIfAbsent(SojEvent sojEvent, SojBusinessTag sojTag, Set<String> mappings, Set<SojEventTagMapping> toSave) {
        if (sojEvent.getId() != null && sojTag.getId() != null) {
            val key = sojEvent.getId() + ";" + sojTag.getId();
            if (mappings.contains(key)) {
                return;
            }
        }
        val mapping = new SojEventTagMapping(sojEvent, sojTag);
        toSave.add(mapping);
    }

    private void saveBatch(Set<SojEvent> sojEvents, Map<String, SojBusinessTag> businessTags,
                           Map<String, SojPlatformTag> platformTags, Set<SojEventTagMapping> mappings) {
        log.info("Soj event to save: " + sojEvents.size());
        if (!sojEvents.isEmpty()) {
            sojEventRepository.saveAll(sojEvents);
        }

        log.info("Soj business tag to save: " + businessTags.size());
        if (!businessTags.isEmpty()) {
            sojBusinessTagRepository.saveAll(businessTags.values());
        }

        log.info("Soj event tag mapping to save: " + mappings.size());
        if (!mappings.isEmpty()) {
            mappingRepository.saveAll(mappings);
        }

        log.info("Soj platform tag to save: " + platformTags.size());
        if (!platformTags.isEmpty()) {
            sojPlatformTagRepository.saveAll(platformTags.values());
        }
    }

    private Boolean isInvalidSojEvent(HadoopSojEvent event) {
        Long invalidId = INVALID_ID;
        return invalidId.equals(event.getPageId()) || invalidId.equals(event.getModuleId()) || invalidId.equals(event.getClickId());
    }

    private void cleanCache(CacheWrapper cacheWrapper, List<HadoopSojEvent> hadoopSojEvents, Set<SojEvent> eventsToSave,
                            Map<String, SojBusinessTag> businessTagsToSave, Map<String, SojPlatformTag> platformTagsToSave,
                            Set<SojEventTagMapping> mappingsToSave) {
        hadoopSojEvents.clear();
        eventsToSave.clear();
        businessTagsToSave.clear();
        platformTagsToSave.clear();
        mappingsToSave.clear();

        cacheWrapper.pmsvcTagCache.clear();
        cacheWrapper.sojEventCache.clear();
        cacheWrapper.businessTagCache.clear();
        cacheWrapper.platformTagCache.clear();
        cacheWrapper.mappingCache.clear();
    }
}
