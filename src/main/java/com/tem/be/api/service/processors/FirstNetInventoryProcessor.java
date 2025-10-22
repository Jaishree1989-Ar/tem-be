package com.tem.be.api.service.processors;

import com.tem.be.api.dao.inventory.TempFirstNetInventoryDao;
import com.tem.be.api.enums.InvoiceStatus;
import com.tem.be.api.model.InventoryHistory;
import com.tem.be.api.model.TempFirstNetInventory;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class FirstNetInventoryProcessor implements InventoryProcessor<TempFirstNetInventory> {

    private final TempFirstNetInventoryDao tempDao;
    private final ObjectMapper objectMapper;

    public FirstNetInventoryProcessor(TempFirstNetInventoryDao tempDao) {
        this.tempDao = tempDao;
        this.objectMapper = new ObjectMapper();
        objectMapper.setDateFormat(new SimpleDateFormat("MM/dd/yy"));
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public String getProviderName() {
        return CarrierConstants.FIRSTNET;
    }

    @Override
    public List<TempFirstNetInventory> convertAndMapData(List<Map<String, String>> dataRows, InventoryHistory history, Map<String, String> departmentMapping, String filename) {
        return dataRows.stream()
                .filter(row -> row != null && row.values().stream().anyMatch(val -> val != null && !val.isBlank()))
                .map(row -> {
                    Map<String, String> cleanedKeys = normalizeKeys(row);
                    TempFirstNetInventory tempInventory = objectMapper.convertValue(cleanedKeys, TempFirstNetInventory.class);

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
    public void saveTempInventory(List<TempFirstNetInventory> tempInventories) {
        tempDao.saveAll(tempInventories);
        log.info("Saved {} temporary FirstNet inventory records.", tempInventories.size());
    }

    private Map<String, String> normalizeKeys(Map<String, String> row) {
        return row.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> CaseUtils.toCamelCase(entry.getKey(), false, ' ', ':', '(', ')'),
                        Map.Entry::getValue
                ));
    }
}
