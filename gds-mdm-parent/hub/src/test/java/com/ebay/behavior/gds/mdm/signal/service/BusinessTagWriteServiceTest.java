package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.signal.common.model.HadoopSojEvent;
import com.ebay.behavior.gds.mdm.signal.common.model.external.pmsvc.v1.datatype.PropertyV1;
import com.ebay.behavior.gds.mdm.signal.repository.SojBusinessTagRepository;
import com.ebay.behavior.gds.mdm.signal.repository.SojEventRepository;
import com.ebay.behavior.gds.mdm.signal.repository.SojPlatformTagRepository;
import com.ebay.behavior.gds.mdm.signal.repository.manyToMany.SojEventTagMappingRepository;
import com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils;

import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.signal.common.model.EventSource.SOJ;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.pmsvcTag;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.sojBusinessTag;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.sojEvent;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.sojPlatformTag;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.sojResult;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessTagWriteServiceTest {

    @Mock
    private EventTemplateService eventTemplateService;

    @Mock
    private SojEventRepository sojEventRepository;

    @Mock
    private SojBusinessTagRepository sojBusinessTagRepository;

    @Mock
    private SojPlatformTagRepository sojPlatformTagRepository;

    @Mock
    private SojEventTagMappingRepository mappingRepository;

    @InjectMocks
    private BusinessTagWriteService businessTagWriteService;

    @BeforeEach
    void setUp() {
        reset(sojEventRepository, sojBusinessTagRepository, sojPlatformTagRepository, mappingRepository);
    }

    @Test
    void save() {
        // Given
        var result1 = sojResult("action1", 111L, 11L, 1L, "tag1,tag2,tag3");
        var result2 = sojResult("action2", 222L, 22L, 2L, "tag3,tag4,tag5");
        var hadoopSojEvents = new ArrayList<HadoopSojEvent>();
        hadoopSojEvents.add(result1);
        hadoopSojEvents.add(result2);
        var pmsvcTag1 = pmsvcTag("tag1", "tagName1");
        var pmsvcTag2 = pmsvcTag("tag2", "tagName2");
        var pmsvcTag3 = pmsvcTag("tag3", "tagName3");
        var pmsvcTag4 = pmsvcTag("tag4", "platformTag4");
        var pmsvcTags = new ArrayList<>(List.of(pmsvcTag1, pmsvcTag2, pmsvcTag3, pmsvcTag4));

        // Mock
        val eventTemplate = TestModelUtils.eventTemplate().setSource(SOJ).withId(123L);
        val attribute = TestModelUtils.attributeTemplate(eventTemplate.getId()).setTag("tag4"); // a platform tag
        when(eventTemplateService.findBySource(SOJ)).thenReturn(Set.of(eventTemplate));
        when(eventTemplateService.getAttributes(eventTemplate.getId())).thenReturn(Set.of(attribute));

        var sojEvent = sojEvent("action1", 111L, 11L, 1L);
        sojEvent.setId(1L);
        var existingSojEvents = new ArrayList<>(List.of(sojEvent));
        when(sojEventRepository.findAll()).thenReturn(existingSojEvents);

        var businessTag = sojBusinessTag("tag1");
        businessTag.setId(1L);
        var existingBusinessTags = new ArrayList<>(List.of(businessTag));
        when(sojBusinessTagRepository.findAll()).thenReturn(existingBusinessTags);

        var platformTag = sojPlatformTag("tag4");
        platformTag.setId(1L);
        when(sojPlatformTagRepository.findAll()).thenReturn(new ArrayList<>());

        var existingMappings = new HashSet<String>();
        var mapping = "1;1";
        existingMappings.add(mapping);
        when(mappingRepository.findAllMappings()).thenReturn(existingMappings);

        doReturn(List.of()).when(sojEventRepository).saveAll(anyCollection());
        doReturn(List.of()).when(sojBusinessTagRepository).saveAll(anyCollection());
        doReturn(List.of()).when(sojPlatformTagRepository).saveAll(anyCollection());
        doReturn(List.of()).when(mappingRepository).saveAll(anyCollection());

        // When
        businessTagWriteService.save(hadoopSojEvents, pmsvcTags);

        // Then
        verify(sojEventRepository, times(1)).saveAll(anyCollection());
        verify(sojBusinessTagRepository, times(1)).saveAll(anyCollection());
        verify(sojPlatformTagRepository, times(1)).saveAll(anyCollection());
        verify(mappingRepository, times(1)).saveAll(anyCollection());
    }

    @Test
    void save_invalidSojResult() {
        // Given
        val result1 = sojResult("action", 111L, 11L, -1L, "tag1,tag2,tag3");
        List<HadoopSojEvent> hadoopSojEvents = new ArrayList<>();
        hadoopSojEvents.add(result1);
        val pmsvcTag1 = pmsvcTag("tag1", "tagName1");
        List<PropertyV1> pmsvcTags = new ArrayList<>();
        pmsvcTags.add(pmsvcTag1);

        // Mock
        when(eventTemplateService.findBySource(any())).thenReturn(Collections.emptySet());
        when(sojEventRepository.findAll()).thenReturn(Collections.emptyList());
        when(sojBusinessTagRepository.findAll()).thenReturn(Collections.emptyList());
        when(mappingRepository.findAllMappings()).thenReturn(Collections.emptySet());

        // When
        businessTagWriteService.save(hadoopSojEvents, pmsvcTags);

        // Then
        verify(sojEventRepository, times(0)).saveAll(anySet());
        verify(sojBusinessTagRepository, times(0)).saveAll(anySet());
        verify(mappingRepository, times(0)).saveAll(anySet());
    }
}
