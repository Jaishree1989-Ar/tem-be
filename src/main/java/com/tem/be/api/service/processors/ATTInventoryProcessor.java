package com.tem.be.api.service.processors;

import com.tem.be.api.dao.inventory.TempATTInventoryDao;
import com.tem.be.api.enums.InvoiceStatus;
import com.tem.be.api.model.InventoryHistory;
import com.tem.be.api.model.TempATTInventory;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.text.CaseUtils;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j2
@Component
public class ATTInventoryProcessor implements InventoryProcessor<TempATTInventory> {

    private final TempATTInventoryDao tempDao;
    private final ObjectMapper objectMapper;

    public ATTInventoryProcessor(TempATTInventoryDao tempDao) {
        this.tempDao = tempDao;
        this.objectMapper = new ObjectMapper();
        objectMapper.setDateFormat(new SimpleDateFormat("MM/dd/yy"));
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public String getProviderName() {
        return "AT&T Mobility";
    }

    @Override
    public List<TempATTInventory> convertAndMapData(List<Map<String, String>> dataRows, InventoryHistory history, Map<String, String> departmentMapping, String filename) {
        return dataRows.stream()
                .filter(row -> row != null && row.values().stream().anyMatch(val -> val != null && !val.isBlank()))
                .map(row -> {
                    Map<String, String> cleanedKeys = normalizeKeys(row);
                    TempATTInventory tempInventory = objectMapper.convertValue(cleanedKeys, TempATTInventory.class);

                    tempInventory.setInventoryHistory(history);
                    tempInventory.setSourceFilename(filename);
                    tempInventory.setStatus(InvoiceStatus.PENDING_APPROVAL);
                    tempInventory.setDeviceStatus(cleanedKeys.get("status"));

                    String billingAccNum = tempInventory.getBillingAccountNumber();
                    if (billingAccNum != null && departmentMapping.containsKey(billingAccNum)) {
                        tempInventory.setDepartment(departmentMapping.get(billingAccNum));
                    }
                    return tempInventory;
                }).toList();
    }

    @Override
    public void saveTempInventory(List<TempATTInventory> tempInventories) {
        tempDao.saveAll(tempInventories);
        log.info("Saved {} temporary AT&T Mobility inventory records.", tempInventories.size());
    }

    private Map<String, String> normalizeKeys(Map<String, String> row) {
        return row.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> CaseUtils.toCamelCase(entry.getKey(), false, ' ', ':', '(', ')'),
                        Map.Entry::getValue
                ));
    }
}
