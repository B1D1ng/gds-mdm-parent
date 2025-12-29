package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.dec.model.PhysicalAssetInfraGlobalProperty;
import com.ebay.behavior.gds.mdm.dec.model.enums.InfraType;
import com.ebay.behavior.gds.mdm.dec.model.enums.PropertyType;
import com.ebay.behavior.gds.mdm.dec.repository.PhysicalAssetInfraGlobalPropertyRepository;

import jakarta.validation.ConstraintViolationException;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.CONTAINS;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestPhsicalAssetInfraUtils.physicalAssetInfraGlobalProperty;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestUtils.INTEGRATION_TEST;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestUtils.getRandomLong;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles(IT)
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class PhysicalAssetInfraGlobalPropertyServiceIT {

    @Autowired
    private PhysicalAssetInfraGlobalPropertyService service;

    @Autowired
    private PhysicalAssetInfraGlobalPropertyRepository repository;

    private PhysicalAssetInfraGlobalProperty globalProperty;
    private Long globalPropertyId;

    @BeforeEach
    void setUp() {
        // Clear all records from the table to ensure a clean state for each test
        repository.deleteAll();

        // Create a new test entity
        globalProperty = physicalAssetInfraGlobalProperty();
        globalPropertyId = repository.save(globalProperty).getId();
    }

    @Test
    void getByInfraTypeAndPropertyType() {
        Optional<PhysicalAssetInfraGlobalProperty> result = service.getByInfraTypeAndPropertyType(
                globalProperty.getInfraType(), globalProperty.getPropertyType());
        assertThat(result.stream().anyMatch(p -> p.getInfraType().name().equals(globalProperty.getInfraType().name()))).isTrue();
        assertThat(result.stream().anyMatch(p -> p.getPropertyType().name().equals(globalProperty.getPropertyType().name()))).isTrue();
    }

    @Test
    void getById() {
        var persisted = service.getById(globalPropertyId);
        assertThat(persisted.getId()).isEqualTo(globalPropertyId);
    }

    @Test
    void getByIdWithAssociations() {
        var persisted = service.getByIdWithAssociations(globalPropertyId);
        assertThat(persisted.getId()).isEqualTo(globalPropertyId);
    }

    @Test
    void getAll_Search() {
        assertThatThrownBy(() -> service.getAll(new Search("by", "term", CONTAINS, PageRequest.of(0, 10))))
                .isInstanceOf(NotImplementedException.class);
    }

    @Test
    void getAll() {
        var result = service.getAll();
        assertThat(result).isNotEmpty();
        assertThat(result).contains(globalProperty);
    }

    @Test
    void create() {
        var newGlobalProperty = PhysicalAssetInfraGlobalProperty.builder().infraType(InfraType.RHEOS).propertyType(PropertyType.IRIS_ASSET).build();
        service.getByInfraTypeAndPropertyType(newGlobalProperty.getInfraType(), newGlobalProperty.getPropertyType())
                .ifPresent(existing -> service.delete(existing.getId()));
        var created = service.create(newGlobalProperty);
        assertThat(created.getId()).isNotNull();
        assertThat(created.getInfraType()).isEqualTo(newGlobalProperty.getInfraType());
        assertThat(created.getPropertyType()).isEqualTo(newGlobalProperty.getPropertyType());
        assertThat(created.getPropertyDetails()).isEqualTo(newGlobalProperty.getPropertyDetails());
    }

    @Test
    void create_Duplicate() {
        var duplicate = PhysicalAssetInfraGlobalProperty.builder()
                .infraType(globalProperty.getInfraType())
                .propertyType(globalProperty.getPropertyType())
                .build();

        // First we need to check how the service behaves when a duplicate is created
        // The service implementation is letting the database throw the constraint violation exception
        // rather than checking for duplicates itself and throwing IllegalArgumentException
        assertThatThrownBy(() -> service.create(duplicate))
                .isInstanceOfAny(IllegalArgumentException.class,
                        DataIntegrityViolationException.class);
    }

    @Test
    void update() {
        String newDetails = "Updated Details";
        globalProperty.setPropertyDetails(newDetails);
        var updated = service.update(globalProperty);

        assertThat(updated.getId()).isEqualTo(globalPropertyId);
        assertThat(updated.getPropertyDetails()).isEqualTo(newDetails);
    }

    @Test
    void delete() {
        service.delete(globalPropertyId);
        assertThatThrownBy(() -> service.getById(globalPropertyId))
                .isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void getAllByInfraType() {
        var result = service.getAllByInfraType(globalProperty.getInfraType());
        assertThat(result).isNotEmpty();
        assertThat(result).contains(globalProperty);
    }

    @Test
    void getAllByPropertyType() {
        var result = service.getAllByPropertyType(globalProperty.getPropertyType());
        assertThat(result).isNotEmpty();
        assertThat(result).contains(globalProperty);
    }

    @Test
    void getAllByInfraTypeAndPropertyType() {
        var result = service.getAllByInfraTypeAndPropertyType(globalProperty.getInfraType(), globalProperty.getPropertyType());
        assertThat(result).isNotEmpty();
        assertThat(result).contains(globalProperty);
    }

    @Test
    void getByInfraTypeAndPropertyType_NotFound() {
        // First delete all records from the table to ensure no combinations exist
        repository.deleteAll();

        // Use the first enum values - now that the table is empty, no combinations should exist
        InfraType unusedInfraType = InfraType.values()[0];
        PropertyType unusedPropertyType = PropertyType.values()[0];

        Optional<PhysicalAssetInfraGlobalProperty> result = service.getByInfraTypeAndPropertyType(
                unusedInfraType, unusedPropertyType);
        assertThat(result).isEmpty();
    }

    @Test
    void getById_NonExistentId() {
        long nonExistentId = getRandomLong();

        assertThatThrownBy(() -> service.getById(nonExistentId))
                .isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void getByIdWithAssociations_NonExistentId() {
        long nonExistentId = getRandomLong();

        assertThatThrownBy(() -> service.getByIdWithAssociations(nonExistentId))
                .isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void create_NullProperty() {
        assertThatThrownBy(() -> service.create(null))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void create_MissingRequiredFields() {
        var invalidProperty = PhysicalAssetInfraGlobalProperty.builder()
                .propertyDetails("Some details")
                .build();

        assertThatThrownBy(() -> service.create(invalidProperty))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void update_NonExistentId() {
        var property = physicalAssetInfraGlobalProperty();
        property.setId(getRandomLong());

        assertThatThrownBy(() -> service.update(property))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void update_NullProperty() {
        assertThatThrownBy(() -> service.update(null))
                .isInstanceOf(ConstraintViolationException.class);
    }

    // Helper method to get a PropertyType that's not being used in the current test setup
    private PropertyType getUnusedPropertyType() {
        // Find a PropertyType that doesn't match the current globalProperty's PropertyType
        for (PropertyType type : PropertyType.values()) {
            if (!type.equals(globalProperty.getPropertyType())) {
                return type;
            }
        }
        // Fallback to the first type (should never happen as there are multiple enum values)
        return PropertyType.values()[0];
    }

    @Test
    void update_WithNoChanges() {
        var updated = service.update(globalProperty);

        assertThat(updated.getId()).isEqualTo(globalPropertyId);
        assertThat(updated).isEqualTo(globalProperty);
    }
}