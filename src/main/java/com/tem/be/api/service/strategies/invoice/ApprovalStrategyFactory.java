package com.tem.be.api.service.strategies.invoice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ApprovalStrategyFactory {
    private final Map<String, ApprovalStrategy> strategyMap;

    @Autowired
    public ApprovalStrategyFactory(List<ApprovalStrategy> strategies) {
        this.strategyMap = strategies.stream()
                .collect(Collectors.toMap(
                        s -> s.getProviderName().toLowerCase(),
                        Function.identity()
                ));
    }

    public ApprovalStrategy getStrategy(String providerName) {
        if (providerName == null || providerName.isBlank()) {
            throw new IllegalArgumentException("Provider name cannot be null.");
        }
        ApprovalStrategy strategy = strategyMap.get(providerName.toLowerCase());
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported provider: '" + providerName + "'. No approval strategy found.");
        }
        return strategy;
    }
}
