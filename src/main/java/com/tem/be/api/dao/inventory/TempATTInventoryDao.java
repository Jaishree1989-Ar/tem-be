package com.tem.be.api.dao.inventory;

import com.tem.be.api.model.TempATTInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TempATTInventoryDao extends JpaRepository<TempATTInventory, Long> {

    List<TempATTInventory> findByInventoryHistory_BatchId(String batchId);

    void deleteAllByInventoryHistory_BatchId(String batchId);
}
