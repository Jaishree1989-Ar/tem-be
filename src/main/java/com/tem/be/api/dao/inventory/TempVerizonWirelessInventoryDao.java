package com.tem.be.api.dao.inventory;

import com.tem.be.api.model.TempVerizonWirelessInventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TempVerizonWirelessInventoryDao extends JpaRepository<TempVerizonWirelessInventory, Long> {
    List<TempVerizonWirelessInventory> findByInventoryHistory_BatchId(String batchId);

    void deleteAllByInventoryHistory_BatchId(String batchId);
}
