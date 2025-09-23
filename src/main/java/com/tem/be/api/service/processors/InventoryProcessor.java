package com.tem.be.api.service.processors;

import com.tem.be.api.model.InventoryHistory;
import com.tem.be.api.model.TempInventoryBase;

import java.util.List;
import java.util.Map;

/**
 * Strategy interface for processing inventory data for a specific provider.
 *
 * @param <T> The type of the temporary inventory entity (e.g., TempFirstNetInventory).
 */
public interface InventoryProcessor<T extends TempInventoryBase> {

    /**
     * Converts raw data from a file into provider-specific temporary inventory entities.
     *
     * @param dataRows The list of maps representing rows from the file.
     * @param history  The parent InventoryHistory record for this batch.
     * @param departmentMapping A map of account numbers to department names.
     * @return A list of temporary inventory entities ready for persistence.
     */
    List<T> convertAndMapData(List<Map<String, String>> dataRows, InventoryHistory history, Map<String, String> departmentMapping, String filename);

    /**
     * Saves a list of provider-specific temporary inventory entities to the database.
     *
     * @param tempInventories The list of temporary inventories to save.
     */
    void saveTempInventory(List<T> tempInventories);

    /**
     * Returns the name of the provider this processor supports.
     *
     * @return The provider name as a String.
     */
    String getProviderName();
}
