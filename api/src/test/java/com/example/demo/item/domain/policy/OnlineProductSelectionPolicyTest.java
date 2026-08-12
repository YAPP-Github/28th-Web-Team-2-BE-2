package com.example.demo.item.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OnlineProductSelectionPolicyTest {

    private final OnlineProductSelectionPolicy policy = new OnlineProductSelectionPolicy();

    @Test
    void excludesProcessedPotatoProducts() {
        assertThat(policy.isTargetProduct("감자", "감자스프 4종")).isFalse();
        assertThat(policy.isTargetProduct("감자", "[KF365] 감자 1kg")).isTrue();
    }

    @Test
    void keepsProductsForItemsWithoutConfiguredExclusions() {
        assertThat(policy.isTargetProduct("양파", "양파즙")).isTrue();
    }
}
