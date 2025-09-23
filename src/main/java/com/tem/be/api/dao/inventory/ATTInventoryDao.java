package com.tem.be.api.dao.inventory;

import com.tem.be.api.model.ATTInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ATTInventoryDao extends JpaRepository<ATTInventory, Long>, JpaSpecificationExecutor<ATTInventory> {
    List<ATTInventory> findByInventoryHistory_BatchId(String batchId);

    @Query("SELECT DISTINCT i.department FROM ATTInventory i WHERE i.department IS NOT NULL AND i.department <> '' ORDER BY i.department ASC")
    List<String> findDistinctDepartments();
}
