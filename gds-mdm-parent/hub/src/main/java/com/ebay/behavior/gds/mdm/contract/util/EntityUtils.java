package com.ebay.behavior.gds.mdm.contract.util;

import com.ebay.behavior.gds.mdm.contract.model.AbstractStreamingComponent;
import com.ebay.behavior.gds.mdm.contract.model.Component;
import com.ebay.behavior.gds.mdm.contract.model.HiveSource;
import com.ebay.behavior.gds.mdm.contract.model.Routing;
import com.ebay.behavior.gds.mdm.contract.model.Transformer;
import com.ebay.behavior.gds.mdm.contract.model.UnstagedContract;

import lombok.experimental.UtilityClass;
import org.hibernate.Hibernate;

import java.util.Optional;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

@UtilityClass
public class EntityUtils {
    public void initContractAssociations(UnstagedContract unstagedContract, boolean recursive) {
        Hibernate.initialize(unstagedContract.getRoutings());
        Hibernate.initialize(unstagedContract.getPipelines());
        if (recursive && isNotEmpty(unstagedContract.getRoutings())) {
            unstagedContract.getRoutings().forEach(rt -> initRoutingAssociations(rt, true));
        }
    }

    public void initRoutingAssociations(Routing routing, boolean recursive) {
        Hibernate.initialize(routing.getComponentChain());
        if (recursive && isNotEmpty(routing.getComponentChain())) {
            routing.getComponentChain().forEach(EntityUtils::initComponentAssociations);
        }
    }

    public void initComponentAssociations(Component component) {
        if (component instanceof AbstractStreamingComponent kafkaComponent) {
            Hibernate.initialize(kafkaComponent.getStreamingConfigs());
        } else if (component instanceof Transformer transformer) {
            Hibernate.initialize(transformer.getTransformations());
            Hibernate.initialize(transformer.getUdfAliases());
            Hibernate.initialize(transformer.getFilters());
        } else if (component instanceof HiveSource hiveSource) {
            Hibernate.initialize(hiveSource.getHiveConfigs());
            Optional.ofNullable(hiveSource.getHiveConfigs())
                    .ifPresent(hcs -> hcs.forEach(hc -> Hibernate.initialize(hc.getHiveStorage())));
        }
    }
}
