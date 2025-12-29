package com.ebay.behavior.gds.mdm.contract.service;

import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.contract.client.UdcClient;
import com.ebay.kernel.util.StringUtils;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Slf4j
@Validated
public class ContractSyncUdcService {

    @Autowired
    private UdcClient udcClient;

    @Autowired
    private UnstagedContractService unstagedContractService;

    public String syncContractToUdc(@NotNull @Valid final Long id, final String env) {
        final var contractId = ofLatestVersion(id);
        final var unstagedContract = this.unstagedContractService.getByIdWithAssociations(contractId, true);

        val response = this.udcClient.registerContractToUdc(unstagedContract, env);
        if (StringUtils.equals(response, "")) {
            log.warn("Failed to register contract with UDC");
            // todo: ignore register status crud temporarily
            return "";
        }

        return response;
    }

    private VersionedId ofLatestVersion(final long id) {
        return VersionedId.of(id, this.unstagedContractService.getLatestVersion(id));
    }
}