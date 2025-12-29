package com.ebay.behavior.gds.mdm.contract.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.contract.model.ContractStatus;
import com.ebay.behavior.gds.mdm.contract.model.UnstagedContract;
import com.ebay.behavior.gds.mdm.contract.repository.RoutingRepository;
import com.ebay.behavior.gds.mdm.contract.repository.UnstagedContractRepository;

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
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.INTEGRATION_TEST;
import static com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils.routing;
import static com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils.unstagedContract;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles(IT)
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class UnstagedContractServiceIT {
    @Autowired
    private UnstagedContractRepository repository;

    @Autowired
    private RoutingRepository routingRepository;

    @Autowired
    private UnstagedContractService service;

    private VersionedId contractId;
    private UnstagedContract contract;

    @BeforeAll
    void setup() {
        contract = unstagedContract("testContract");
        contract = service.create(contract);
        contractId = VersionedId.of(contract.getId(), contract.getVersion());
    }

    @Test
    void creteContract() {
        val contract = service.create(unstagedContract("testContract"));
        val contractId = VersionedId.of(contract.getId(), contract.getVersion());
        val persist = service.getById(contractId);

        assertThat(persist.getName()).isEqualTo(contract.getName());
        assertThat(persist.getDescription()).isEqualTo(contract.getDescription());
        assertThat(persist.getDl()).isEqualTo(contract.getDl());
        assertThat(persist.getOwners()).isEqualTo(contract.getOwners());
    }

    @Test
    void createContract_withValidData_createsSuccessfully() {
        var newContract = unstagedContract("newContract");
        var createdContract = service.create(newContract);

        assertThat(createdContract).isNotNull();
        assertThat(createdContract.getId()).isNotNull();
        assertThat(createdContract.getName()).isEqualTo("newContract");
    }

    @Test
    void createContract_withInvalidStatus_throwsException() {
        var newContract = unstagedContract("invalidStatusContract").setStatus(ContractStatus.RELEASED);

        assertThatThrownBy(() -> service.create(newContract))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid status");
    }

    @Test
    void updateContract_withValidData_updatesSuccessfully() {
        var updatedContract = contract.toBuilder().name("updatedName").build();
        var result = service.update(updatedContract);

        assertThat(result.getName()).isEqualTo("updatedName");
        assertThat(result.getRevision()).isEqualTo(contract.getRevision() + 1);
    }

    @Test
    void updateContract_withNonExistentId_throwsException() {
        var nonExistentContract = unstagedContract("nonExistent").setId(9999L);

        assertThatThrownBy(() -> service.update(nonExistentContract))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deleteContract_withNoAssociations_deletesSuccessfully() {
        val contract = repository.save(unstagedContract("testDeleteContract"));
        val contractId = VersionedId.of(contract.getId(), contract.getVersion());

        service.delete(contractId);

        assertThat(repository.findById(contractId)).isEmpty();
    }

    @Test
    void getRoutings_withValidId_returnsRoutings() {
        routingRepository.save(routing("testRouting").setContractId(contractId.getId()).setContractVersion(contractId.getVersion()));
        var routings = service.getRoutings(contractId);

        assertThat(routings).isNotNull();
        assertThat(routings).size().isEqualTo(1);
    }

    @Test
    void getRoutings_withNonExistentId_throwsException() {
        var nonExistentId = VersionedId.of(9999L, 1);

        assertThatThrownBy(() -> service.getRoutings(nonExistentId))
                .isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void getLatestVersion_withValidId_returnsVersion() {
        var latestVersion = service.getLatestVersion(contractId.getId());

        assertThat(latestVersion).isEqualTo(contractId.getVersion());
    }

    @Test
    void getLatestVersion_withNonExistentId_throwsException() {
        assertThatThrownBy(() -> service.getLatestVersion(9999L))
                .isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void getByIdAndLatestVersion_withValidId_returnsContract() {
        var result = service.getByIdAndLatestVersion(contractId.getId());

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(contractId.getId());
    }

    @Test
    void getByIdAndLatestVersion_withNonExistentId_throwsException() {
        assertThatThrownBy(() -> service.getByIdAndLatestVersion(9999L))
                .isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void getByIdWithAssociations_withValidId_returnsContractWithAssociations() {
        var result = service.getByIdWithAssociations(contractId, true);

        assertThat(result).isNotNull();
        assertThat(result.getRoutings()).isEmpty();
        assertThat(result.getPipelines()).isEmpty();
    }

    @Test
    void getByIdWithAssociations_withNonExistentId_throwsException() {
        var nonExistentId = VersionedId.of(9999L, 1);

        assertThatThrownBy(() -> service.getByIdWithAssociations(nonExistentId, true))
                .isInstanceOf(DataNotFoundException.class);
    }
}