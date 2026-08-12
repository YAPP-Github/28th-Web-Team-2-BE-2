package com.example.demo.item.infrastructure.config;

import com.example.demo.item.domain.policy.OnlineProductSelectionPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OnlineProductSelectionPolicyConfiguration {

    @Bean
    public OnlineProductSelectionPolicy onlineProductSelectionPolicy() {
        return new OnlineProductSelectionPolicy();
    }
}
