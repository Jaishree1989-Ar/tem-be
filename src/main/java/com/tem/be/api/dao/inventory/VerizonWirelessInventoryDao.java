package com.tem.be.api.dao.inventory;

import com.tem.be.api.model.VerizonWirelessInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface VerizonWirelessInventoryDao extends JpaRepository<VerizonWirelessInventory, Long>, JpaSpecificationExecutor<VerizonWirelessInventory> {
    List<VerizonWirelessInventory> findByInventoryHistory_BatchId(String batchId);

    @Query("SELECT DISTINCT i.department FROM VerizonWirelessInventory i WHERE i.department IS NOT NULL AND i.department <> '' ORDER BY i.department ASC")
    List<String> findDistinctDepartments();
}
