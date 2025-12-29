package com.ebay.behavior.gds.mdm.contract.util;

import com.ebay.behavior.gds.mdm.common.model.Environment;
import com.ebay.behavior.gds.mdm.contract.model.Component;
import com.ebay.behavior.gds.mdm.contract.model.HiveConfig;
import com.ebay.behavior.gds.mdm.contract.model.HiveSource;
import com.ebay.behavior.gds.mdm.contract.model.Routing;
import com.ebay.behavior.gds.mdm.contract.model.Sink;
import com.ebay.behavior.gds.mdm.contract.model.Source;
import com.ebay.behavior.gds.mdm.contract.model.UnstagedContract;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.experimental.UtilityClass;
import lombok.val;
import org.apache.commons.collections.CollectionUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

@UtilityClass
public class ContractUtils {
    public static Optional<Component> getContractSource(@NotNull @Valid UnstagedContract contract) {
        return getComponentChain(contract).flatMap(components -> components.stream().filter(Source.class::isInstance).findFirst());
    }

    public static Optional<Component> getContractSink(@NotNull @Valid UnstagedContract contract) {
        return getComponentChain(contract).flatMap(components -> components.stream().filter(Sink.class::isInstance).findFirst());
    }

    public static Optional<List<Component>> getComponentChain(@NotNull @Valid UnstagedContract contract) {
        EntityUtils.initContractAssociations(contract, true);

        return Optional.of(contract)
                .map(UnstagedContract::getRoutings)
                .map(Set::iterator)
                .filter(Iterator::hasNext)
                .map(Iterator::next)
                .map(Routing::getComponentChain)
                .filter(CollectionUtils::isNotEmpty);
    }

    public static HiveConfig getHiveConfigByEnv(@NotNull @Valid HiveSource source, @NotNull Environment environment) {
        if (isEmpty(source.getHiveConfigs())) {
            return null;
        }
        for (val config : source.getHiveConfigs()) {
            if (config.getEnv() == environment) {
                return config;
            }
        }
        return null;
    }
}
