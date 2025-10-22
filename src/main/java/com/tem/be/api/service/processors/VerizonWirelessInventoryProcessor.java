package com.tem.be.api.service.processors;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tem.be.api.dao.inventory.TempVerizonWirelessInventoryDao;
import com.tem.be.api.enums.InvoiceStatus;
import com.tem.be.api.model.InventoryHistory;
import com.tem.be.api.model.TempVerizonWirelessInventory;
import com.tem.be.api.utils.CarrierConstants;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.text.CaseUtils;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j2
@Component
public class VerizonWirelessInventoryProcessor implements InventoryProcessor<TempVerizonWirelessInventory> {

    private final TempVerizonWirelessInventoryDao tempDao;
    private final ObjectMapper objectMapper;

    public VerizonWirelessInventoryProcessor(TempVerizonWirelessInventoryDao tempDao) {
        this.tempDao = tempDao;
        this.objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public String getProviderName() {
        return CarrierConstants.VERIZON_WIRELESS;
    }

    @Override
    public List<TempVerizonWirelessInventory> convertAndMapData(List<Map<String, String>> dataRows, InventoryHistory history, Map<String, String> departmentMapping, String filename) {
        return dataRows.stream()
                .filter(row -> row != null && row.values().stream().anyMatch(val -> val != null && !val.isBlank()))
                .map(row -> {
                    Map<String, String> cleanedKeys = normalizeKeys(row);
                    TempVerizonWirelessInventory tempInventory = objectMapper.convertValue(cleanedKeys, TempVerizonWirelessInventory.class);

                    tempInventory.setInventoryHistory(history);
                    tempInventory.setSourceFilename(filename);
                    tempInventory.setStatus(InvoiceStatus.PENDING_APPROVAL);

                    // Apply department mapping based on the "Account number"
                    String accountNumber = tempInventory.getAccountNumber();
                    if (accountNumber != null && departmentMapping.containsKey(accountNumber)) {
                        tempInventory.setDepartment(departmentMapping.get(accountNumber));
                    } else if (accountNumber != null) {
                        log.warn("No department mapping found for Verizon inventory account '{}'. Department will be null.", accountNumber);
                    }

                    return tempInventory;
                }).toList();
    }

    @Override
    public void saveTempInventory(List<TempVerizonWirelessInventory> tempInventories) {
        tempDao.saveAll(tempInventories);
        log.info("Saved {} temporary Verizon Wireless inventory records.", tempInventories.size());
    }

    private Map<String, String> normalizeKeys(Map<String, String> row) {
        return row.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> CaseUtils.toCamelCase(entry.getKey(), false, ' ', '/', '-', '(', ')'),
                        Map.Entry::getValue
                ));
    }
}
