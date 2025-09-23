package com.tem.be.api.service.strategies.inventory;

import com.tem.be.api.dao.inventory.FirstNetInventoryDao;
import com.tem.be.api.dao.inventory.TempFirstNetInventoryDao;
import com.tem.be.api.model.FirstNetInventory;
import com.tem.be.api.model.InventoryHistory;
import com.tem.be.api.model.TempFirstNetInventory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class FirstNetInventoryApprovalStrategy implements InventoryApprovalStrategy {

    private final TempFirstNetInventoryDao tempRepo;
    private final FirstNetInventoryDao finalRepo;

    @Autowired
    public FirstNetInventoryApprovalStrategy(TempFirstNetInventoryDao tempRepo, FirstNetInventoryDao finalRepo) {
        this.tempRepo = tempRepo;
        this.finalRepo = finalRepo;
    }

    @Override
    public String getProviderName() {
        return "FirstNet"; // Matches the provider name in the database
    }

    @Override
    public List<TempFirstNetInventory> getTemporaryInventoriesForReview(String batchId) {
        return tempRepo.findByInventoryHistory_BatchId(batchId);
    }

    @Override
    @Transactional
    public void approve(InventoryHistory history) {
        List<TempFirstNetInventory> tempInventories = tempRepo.findByInventoryHistory_BatchId(history.getBatchId());

        List<FirstNetInventory> finalInventories = tempInventories.stream()
                .map(temp -> convertToFinalInventory(temp, history))
                .toList();

        finalRepo.saveAll(finalInventories);
        tempRepo.deleteAll(tempInventories); // Clean up temp data
    }

    @Override
    @Transactional
    public void reject(String batchId) {
        tempRepo.deleteAllByInventoryHistory_BatchId(batchId);
    }

    @Override
    public List<FirstNetInventory> getFinalInventories(String batchId) {
        return finalRepo.findByInventoryHistory_BatchId(batchId);
    }

    private FirstNetInventory convertToFinalInventory(TempFirstNetInventory temp, InventoryHistory history) {
        FirstNetInventory finalInventory = new FirstNetInventory();
        // Ignore properties that don't exist in the final entity or are set manually
        BeanUtils.copyProperties(temp, finalInventory, "tempInventoryId", "inventoryHistory", "createdAt");
        finalInventory.setInventoryHistory(history);
        return finalInventory;
    }
}
