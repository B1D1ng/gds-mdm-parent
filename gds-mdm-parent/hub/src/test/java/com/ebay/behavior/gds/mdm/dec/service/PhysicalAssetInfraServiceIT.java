package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.dec.model.PhysicalAssetInfra;
import com.ebay.behavior.gds.mdm.dec.model.enums.PropertyType;

import jakarta.validation.ConstraintViolationException;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.CONTAINS;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestPhsicalAssetInfraUtils.physicalAssetInfra;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestUtils.INTEGRATION_TEST;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestUtils.getRandomLong;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles(IT)
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class PhysicalAssetInfraServiceIT {

    @Autowired
    private PhysicalAssetInfraService service;

    private PhysicalAssetInfra infra;
    private Long infraId;

    @BeforeEach
    void setUp() {
        infra = physicalAssetInfra();
        // Clean up any existing entries with same unique fields
        service.getAllByInfraTypeAndPropertyTypeAndEnvironment(
                        infra.getInfraType(),
                        infra.getPropertyType(),
                        infra.getPlatformEnvironment())
                .forEach(existing -> service.delete(existing.getId()));
        infra = service.create(infra);
        infraId = infra.getId();
    }

    @Test
    void getById() {
        var persisted = service.getById(infraId);
        assertThat(persisted.getId()).isEqualTo(infraId);
    }

    @Test
    void getByIdWithAssociations() {
        var persisted = service.getByIdWithAssociations(infraId);
        assertThat(persisted.getId()).isEqualTo(infraId);
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
        assertThat(result).contains(infra);
    }

    @Test
    void create() {
        var newInfra = physicalAssetInfra();
        service.getAllByInfraTypeAndPropertyTypeAndEnvironment(
                        newInfra.getInfraType(),
                        newInfra.getPropertyType(),
                        newInfra.getPlatformEnvironment())
                .forEach(existing -> service.delete(existing.getId()));
        var created = service.create(newInfra);
        assertThat(created.getId()).isNotNull();
        assertThat(created.getInfraType()).isEqualTo(newInfra.getInfraType());
        assertThat(created.getPropertyType()).isEqualTo(newInfra.getPropertyType());
        assertThat(created.getPropertyDetails()).isEqualTo(newInfra.getPropertyDetails());
    }

    @Test
    void update() {
        String newDetails = "Updated Details";
        infra.setPropertyDetails(newDetails);
        var updated = service.update(infra);

        assertThat(updated.getId()).isEqualTo(infraId);
        assertThat(updated.getPropertyDetails()).isEqualTo(newDetails);
    }

    @Test
    void delete() {
        service.delete(infraId);
        assertThatThrownBy(() -> service.getById(infraId))
                .isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void getAllByInfraType() {
        var result = service.getAllByInfraType(infra.getInfraType());
        assertThat(result).isNotEmpty();
        assertThat(result).contains(infra);
    }

    @Test
    void getAllByPropertyType() {
        var result = service.getAllByPropertyType(infra.getPropertyType());
        assertThat(result).isNotEmpty();
        assertThat(result).contains(infra);
    }

    @Test
    void getAllByPlatformEnvironment() {
        var result = service.getAllByPlatformEnvironment(infra.getPlatformEnvironment());
        assertThat(result).isNotEmpty();
        assertThat(result).contains(infra);
    }

    @Test
    void getAllByInfraTypeAndPropertyType() {
        var result = service.getAllByInfraTypeAndPropertyType(infra.getInfraType(), infra.getPropertyType());
        assertThat(result).isNotEmpty();
        assertThat(result).contains(infra);
    }

    @Test
    void getAllByInfraTypeAndPropertyTypeAndEnvironment() {
        var result = service.getAllByInfraTypeAndPropertyTypeAndEnvironment(
                infra.getInfraType(), infra.getPropertyType(), infra.getPlatformEnvironment());
        assertThat(result).isNotEmpty();
        assertThat(result).contains(infra);
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
    void create_NullInfra() {
        assertThatThrownBy(() -> service.create(null))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void create_MissingRequiredFields() {
        var invalidInfra = PhysicalAssetInfra.builder()
                .propertyDetails("Some details")
                .build();

        assertThatThrownBy(() -> service.create(invalidInfra))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void update_NonExistentId() {
        var property = physicalAssetInfra();
        property.setId(getRandomLong());

        assertThatThrownBy(() -> service.update(property))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void update_NullInfra() {
        assertThatThrownBy(() -> service.update(null))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void getAllByPropertyType_NonExistentType() {
        assertThatThrownBy(() -> service.getAllByPropertyType(PropertyType.valueOf("NON_EXISTENT_TYPE"))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void update_WithNoChanges() {
        var updated = service.update(infra);

        assertThat(updated.getId()).isEqualTo(infraId);
        assertThat(updated).isEqualTo(infra);
    }
}