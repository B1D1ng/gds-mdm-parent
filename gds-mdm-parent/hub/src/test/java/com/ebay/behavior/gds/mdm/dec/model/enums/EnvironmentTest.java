package com.ebay.behavior.gds.mdm.dec.model.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EnvironmentTest {

    @Test
    void isTransitionAllowedTo() {
        Environment oldEnv = Environment.UNSTAGED;
        Environment newEnv = Environment.STAGING;
        assertThat(oldEnv.isTransitionAllowedTo(newEnv)).isTrue();

        newEnv = Environment.PRODUCTION;
        assertThat(oldEnv.isTransitionAllowedTo(newEnv)).isFalse();
    }
}
