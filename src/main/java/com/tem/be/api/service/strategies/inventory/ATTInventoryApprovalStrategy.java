package com.tem.be.api.service.strategies.inventory;

import com.tem.be.api.dao.inventory.ATTInventoryDao;
import com.tem.be.api.dao.inventory.TempATTInventoryDao;
import com.tem.be.api.model.ATTInventory;
import com.tem.be.api.model.InventoryHistory;
import com.tem.be.api.model.TempATTInventory;
import com.tem.be.api.utils.CarrierConstants;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class ATTInventoryApprovalStrategy implements InventoryApprovalStrategy {

    private final TempATTInventoryDao tempRepo;
    private final ATTInventoryDao finalRepo;

    @Autowired
    public ATTInventoryApprovalStrategy(TempATTInventoryDao tempRepo, ATTInventoryDao finalRepo) {
        this.tempRepo = tempRepo;
        this.finalRepo = finalRepo;
    }

    @Override
    public String getProviderName() {
        return CarrierConstants.ATT;
    }

    @Override
    public List<TempATTInventory> getTemporaryInventoriesForReview(String batchId) {
        return tempRepo.findByInventoryHistory_BatchId(batchId);
    }

    @Override
    @Transactional
    public void approve(InventoryHistory history) {
        List<TempATTInventory> tempInventories = tempRepo.findByInventoryHistory_BatchId(history.getBatchId());

        List<ATTInventory> finalInventories = tempInventories.stream()
                .map(temp -> convertToFinalInventory(temp, history))
                .toList();

        finalRepo.saveAll(finalInventories);
        tempRepo.deleteAll(tempInventories);
    }

    @Override
    @Transactional
    public void reject(String batchId) {
        tempRepo.deleteAllByInventoryHistory_BatchId(batchId);
    }

    @Override
    public List<ATTInventory> getFinalInventories(String batchId) {
        return finalRepo.findByInventoryHistory_BatchId(batchId);
    }

    private ATTInventory convertToFinalInventory(TempATTInventory temp, InventoryHistory history) {
        ATTInventory finalInventory = new ATTInventory();
        BeanUtils.copyProperties(temp, finalInventory, "tempInventoryId", "inventoryHistory", "createdAt");
        finalInventory.setInventoryHistory(history);
        return finalInventory;
    }
}
