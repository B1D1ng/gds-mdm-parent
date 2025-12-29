package com.ebay.behavior.gds.mdm.contract.model;

import jakarta.validation.constraints.NotNull;
import lombok.val;

import static com.ebay.behavior.gds.mdm.contract.util.ContractUtils.getContractSource;

public enum ContractSourceType {
    KAFKA(KafkaSource.class), BES(BesSource.class), HIVE(HiveSource.class);

    private final Class<? extends Component> type;

    ContractSourceType(Class<? extends Component> component) {
        type = component;
    }

    public static ContractSourceType fromContract(@NotNull UnstagedContract contract) {
        val contractType = getContractSource(contract)
                .map(Component::getClass)
                .orElseThrow(() -> new IllegalArgumentException("No such contract type: " + contract));
        for (ContractSourceType type : ContractSourceType.values()) {
            if (type.type.equals(contractType)) {
                return type;
            }
        }
        throw new IllegalArgumentException("No such contract type: " + contractType);
    }
}
