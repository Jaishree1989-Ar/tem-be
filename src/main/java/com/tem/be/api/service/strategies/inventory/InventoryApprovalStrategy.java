package com.tem.be.api.service.strategies.inventory;

import com.tem.be.api.model.InventoryHistory;
import com.tem.be.api.model.Inventoryable;
import com.tem.be.api.model.TempInventoryBase;

import java.util.List;

/**
 * Strategy interface for handling the review and approval workflow for a specific inventory provider.
 * Each implementation encapsulates the logic for fetching, converting, and cleaning up
 * inventory data for its designated provider.
 */
public interface InventoryApprovalStrategy {

    /**
     * Retrieves the temporary inventory records associated with a batch for user review.
     *
     * @param batchId The unique ID of the batch.
     * @return A list of temporary inventory entities.
     */
    List<? extends TempInventoryBase> getTemporaryInventoriesForReview(String batchId);

    /**
     * Executes the approval logic for a given batch. This includes converting
     * temporary inventory records to final inventory records, saving them, and cleaning up the
     * temporary records.
     *
     * @param history The parent InventoryHistory object for the batch being approved.
     */
    void approve(InventoryHistory history);

    /**
     * Deletes the temporary inventory records associated with a rejected batch.
     *
     * @param batchId The unique ID of the batch to reject.
     */
    void reject(String batchId);

    /**
     * Retrieves the final, approved inventory records for a given batch.
     *
     * @param batchId The unique ID of the batch.
     * @return A list of final inventory entities (e.g., FirstNetInventory, ATTInventory).
     */
    List<? extends Inventoryable> getFinalInventories(String batchId);

    /**
     * Returns the name of the provider this strategy supports (e.g., "FirstNet", "AT&T Mobility").
     *
     * @return The provider name as a String.
     */
    String getProviderName();
}
