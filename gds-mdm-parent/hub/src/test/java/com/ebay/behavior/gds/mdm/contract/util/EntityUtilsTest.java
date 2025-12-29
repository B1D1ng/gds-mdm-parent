package com.ebay.behavior.gds.mdm.contract.util;

import com.ebay.behavior.gds.mdm.contract.model.Routing;
import com.ebay.behavior.gds.mdm.contract.model.UnstagedContract;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EntityUtilsTest {

    @Test
    void initContractAssociations_withRecursiveTrue_initializesAssociations() {
        // Mock contract and routing
        UnstagedContract contract = mock(UnstagedContract.class);
        Routing routing = mock(Routing.class);

        // Mock routings
        when(contract.getRoutings()).thenReturn(Set.of(routing));

        // Call the method
        EntityUtils.initContractAssociations(contract, true);

        // Verify all interactions
        verify(contract, times(3)).getRoutings();
        verify(routing, times(2)).getComponentChain();
    }

    @Test
    void initContractAssociations_withRecursiveFalse_initializesAssociations() {
        // Mock contract
        UnstagedContract contract = mock(UnstagedContract.class);

        // Mock routings
        when(contract.getRoutings()).thenReturn(Set.of());

        // Call the method
        EntityUtils.initContractAssociations(contract, false);

        // Verify getRoutings was called
        verify(contract, times(1)).getRoutings();
    }
}