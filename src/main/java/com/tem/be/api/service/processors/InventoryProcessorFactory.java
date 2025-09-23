package com.tem.be.api.service.processors;

import com.tem.be.api.model.TempInventoryBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class InventoryProcessorFactory {

    private final Map<String, InventoryProcessor<? extends TempInventoryBase>> processorMap;

    @Autowired
    public InventoryProcessorFactory(List<InventoryProcessor<? extends TempInventoryBase>> processors) {
        this.processorMap = processors.stream()
                .collect(Collectors.toMap(
                        p -> p.getProviderName().toLowerCase(),
                        Function.identity()
                ));
    }

    public InventoryProcessor<? extends TempInventoryBase> getProcessor(String providerName) {
        if (providerName == null || providerName.isBlank()) {
            throw new IllegalArgumentException("Provider name cannot be null or empty.");
        }

        InventoryProcessor<? extends TempInventoryBase> processor = processorMap.get(providerName.toLowerCase());

        if (processor == null) {
            throw new IllegalArgumentException("Unsupported provider for inventory: '" + providerName + "'. No matching processor found.");
        }

        return processor;
    }
}
