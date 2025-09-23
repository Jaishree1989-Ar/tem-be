package com.tem.be.api.dao.inventory;

import com.tem.be.api.model.InventoryHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryHistoryDao extends JpaRepository<InventoryHistory, Long> {

    /*
        * Retrieves all InventoryHistory records ordered by creation date in descending order.
     */
    List<InventoryHistory> findAllByOrderByCreatedAtDesc();

    Optional<InventoryHistory> findByBatchId(String batchId);
}
