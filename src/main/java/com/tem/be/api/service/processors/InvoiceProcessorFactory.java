package com.tem.be.api.service.processors;

import com.tem.be.api.model.TempInvoiceBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class InvoiceProcessorFactory {

    private final Map<String, InvoiceProcessor<? extends TempInvoiceBase>> processorMap;

    @Autowired
    public InvoiceProcessorFactory(List<InvoiceProcessor<? extends TempInvoiceBase>> processors) {
        // Create a map of provider names to their corresponding processor bean
        this.processorMap = processors.stream()
                .collect(Collectors.toMap(
                        p -> p.getProviderName().toLowerCase(),
                        Function.identity()
                ));
    }

    public InvoiceProcessor<? extends TempInvoiceBase> getProcessor(String providerName) {
        if (providerName == null || providerName.isBlank()) {
            throw new IllegalArgumentException("Provider name cannot be null or empty.");
        }

        InvoiceProcessor<? extends TempInvoiceBase> processor = processorMap.get(providerName.toLowerCase());

        if (processor == null) {
            throw new IllegalArgumentException("Unsupported provider: '" + providerName + "'. No matching processor found.");
        }

        return processor;
    }
}
