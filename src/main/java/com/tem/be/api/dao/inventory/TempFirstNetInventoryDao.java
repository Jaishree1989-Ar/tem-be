package com.tem.be.api.dao.inventory;

import com.tem.be.api.model.TempFirstNetInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TempFirstNetInventoryDao extends JpaRepository<TempFirstNetInventory, Long> {

    List<TempFirstNetInventory> findByInventoryHistory_BatchId(String batchId);

    void deleteAllByInventoryHistory_BatchId(String batchId);
}
