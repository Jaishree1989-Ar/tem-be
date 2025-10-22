package com.tem.be.api.service.strategies.inventory;

import com.tem.be.api.dao.inventory.TempVerizonWirelessInventoryDao;
import com.tem.be.api.dao.inventory.VerizonWirelessInventoryDao;
import com.tem.be.api.model.InventoryHistory;
import com.tem.be.api.model.TempVerizonWirelessInventory;
import com.tem.be.api.model.VerizonWirelessInventory;
import com.tem.be.api.utils.CarrierConstants;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class VerizonWirelessInventoryApprovalStrategy implements InventoryApprovalStrategy {

    private final TempVerizonWirelessInventoryDao tempRepo;
    private final VerizonWirelessInventoryDao finalRepo;

    public VerizonWirelessInventoryApprovalStrategy(TempVerizonWirelessInventoryDao tempRepo, VerizonWirelessInventoryDao finalRepo) {
        this.tempRepo = tempRepo;
        this.finalRepo = finalRepo;
    }

    @Override
    public String getProviderName() {
        return CarrierConstants.VERIZON_WIRELESS;
    }

    @Override
    public List<TempVerizonWirelessInventory> getTemporaryInventoriesForReview(String batchId) {
        return tempRepo.findByInventoryHistory_BatchId(batchId);
    }

    @Override
    @Transactional
    public void approve(InventoryHistory history) {
        List<TempVerizonWirelessInventory> tempInventories = tempRepo.findByInventoryHistory_BatchId(history.getBatchId());
        List<VerizonWirelessInventory> finalInventories = tempInventories.stream()
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
    public List<VerizonWirelessInventory> getFinalInventories(String batchId) {
        return finalRepo.findByInventoryHistory_BatchId(batchId);
    }

    private VerizonWirelessInventory convertToFinalInventory(TempVerizonWirelessInventory temp, InventoryHistory history) {
        VerizonWirelessInventory finalInventory = new VerizonWirelessInventory();
        BeanUtils.copyProperties(temp, finalInventory, "tempInventoryId", "inventoryHistory", "createdAt", "updatedAt");
        finalInventory.setInventoryHistory(history);
        return finalInventory;
    }
}
