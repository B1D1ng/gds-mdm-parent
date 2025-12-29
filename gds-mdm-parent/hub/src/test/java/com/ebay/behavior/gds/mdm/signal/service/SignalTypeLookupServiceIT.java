package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.signal.common.model.SignalTypeLookup;
import com.ebay.behavior.gds.mdm.signal.repository.manyToMany.SignalTypeDimensionMappingRepository;
import com.ebay.behavior.gds.mdm.signal.repository.manyToMany.SignalTypePhysicalStorageMappingRepository;

import lombok.val;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.signalType;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.INTEGRATION_TEST;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomSmallString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles(IT)
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class SignalTypeLookupServiceIT {

    @Autowired
    private SignalTypeLookupService service;

    @Autowired
    private SignalTypePhysicalStorageMappingRepository mappingRepository;

    @Autowired
    private SignalTypeDimensionMappingRepository dimMappingRepository;

    private SignalTypeLookup lookup;
    private final long storageId = 123L; // physicalStorage id 123 is defined under data.sql
    private final long dimId = 0L; // dimension id 0 is defined under data.sql, signal_dim_type_lookup, domain dimension

    @BeforeAll
    void setUpAll() {
        val signalTypeLookup = signalType();
        var name = signalTypeLookup.getName();
        service.findByName(name).ifPresent(lookup -> {
            service.deleteByName(name);
            service.create(signalTypeLookup);
        });

        lookup = service.getByName(name);
    }

    @Test
    void create_invalid_error() {
        var lookup1 = signalType().toBuilder().id(123L).build();
        assertThatThrownBy(() -> service.create(lookup1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be null");
    }

    @Test
    void create_nameInUse_error() {
        assertThatThrownBy(() -> service.create(lookup))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getAll() {
        var signalTypes = service.getAll();

        assertThat(signalTypes.size()).isGreaterThanOrEqualTo(1);
        assertThat(signalTypes).extracting(SignalTypeLookup::getName).contains(lookup.getName());
    }

    @Test
    void getById() {
        var persisted = service.getByName(lookup.getName());
        assertThat(persisted.getId()).isNotNull();
        assertThat(persisted.getName()).isEqualTo(lookup.getName());

        var result = service.getById(persisted.getId());

        assertThat(result.getId()).isEqualTo(persisted.getId());
        assertThat(result.getName()).isEqualTo(persisted.getName());
    }

    @Test
    void createPhysicalStorageMapping() {
        var mappingsBefore = mappingRepository.findBySignalTypeId(lookup.getId());
        assertThat(mappingsBefore).isEmpty();

        service.createPhysicalStorageMapping(lookup.getId(), storageId);

        var mappingsAfter = mappingRepository.findBySignalTypeId(lookup.getId());
        assertThat(mappingsAfter).hasSize(1);
    }

    @Test
    void deletePhysicalStorageMapping() {
        var lookup1 = signalType().toBuilder().name(getRandomSmallString()).readableName(getRandomSmallString()).build();
        lookup1 = service.create(lookup1);
        service.createPhysicalStorageMapping(lookup1.getId(), storageId);
        var mappingsBefore = mappingRepository.findBySignalTypeId(lookup1.getId());
        assertThat(mappingsBefore).hasSize(1);

        service.deletePhysicalStorageMapping(lookup1.getId(), storageId);

        var mappingsAfter = mappingRepository.findBySignalTypeId(lookup1.getId());
        assertThat(mappingsAfter).isEmpty();
    }

    @Test
    void createDimensionMapping() {
        var lookup1 = signalType().toBuilder().name(getRandomSmallString()).readableName(getRandomSmallString()).build();
        lookup1 = service.create(lookup1);

        var mappingsBefore = dimMappingRepository.findBySignalTypeId(lookup1.getId());
        assertThat(mappingsBefore).isEmpty();

        service.createDimensionMapping(lookup1.getId(), dimId, true);

        var mappingsAfter = dimMappingRepository.findBySignalTypeId(lookup1.getId());
        assertThat(mappingsAfter).hasSize(1);
    }

    @Test
    void updateDimensionMapping() {
        var lookup1 = signalType().toBuilder().name(getRandomSmallString()).readableName(getRandomSmallString()).build();
        lookup1 = service.create(lookup1);

        service.createDimensionMapping(lookup1.getId(), dimId, true);
        service.updateDimensionMapping(lookup1.getId(), dimId, false);

        var mappingsAfter = dimMappingRepository.findBySignalTypeId(lookup1.getId());
        assertThat(mappingsAfter).hasSize(1);
        assertThat(mappingsAfter.iterator().next().getIsMandatory()).isEqualTo(false);
    }

    @Test
    void deleteDimensionMapping() {
        var lookup1 = signalType().toBuilder().name(getRandomSmallString()).readableName(getRandomSmallString()).build();
        lookup1 = service.create(lookup1);

        service.createDimensionMapping(lookup1.getId(), dimId, true);

        var mappingsBefore = dimMappingRepository.findBySignalTypeId(lookup1.getId());
        assertThat(mappingsBefore).hasSize(1);

        service.deleteDimensionMapping(lookup1.getId(), dimId);

        var mappingsAfter = dimMappingRepository.findBySignalTypeId(lookup1.getId());
        assertThat(mappingsAfter).isEmpty();
    }

    @Test
    void getDimensionsById() {
        var lookup1 = signalType().toBuilder().name(getRandomSmallString()).readableName(getRandomSmallString()).build();
        lookup1 = service.create(lookup1);
        service.createDimensionMapping(lookup1.getId(), dimId, true);

        var dimensions = service.getDimensionsBySignalTypeId(lookup1.getId());
        assertThat(dimensions).hasSize(1);
    }
}
