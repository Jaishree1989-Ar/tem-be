package com.tem.be.api.service.strategies.inventory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class InventoryApprovalStrategyFactory {
    private final Map<String, InventoryApprovalStrategy> strategyMap;

    @Autowired
    public InventoryApprovalStrategyFactory(List<InventoryApprovalStrategy> strategies) {
        this.strategyMap = strategies.stream()
                .collect(Collectors.toMap(
                        s -> s.getProviderName().toLowerCase(),
                        Function.identity()
                ));
    }

    public InventoryApprovalStrategy getStrategy(String providerName) {
        if (providerName == null || providerName.isBlank()) {
            throw new IllegalArgumentException("Provider name cannot be null for inventory strategy.");
        }
        InventoryApprovalStrategy strategy = strategyMap.get(providerName.toLowerCase());
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported provider: '" + providerName + "'. No inventory approval strategy found.");
        }
        return strategy;
    }
}
