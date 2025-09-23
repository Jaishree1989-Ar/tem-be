package com.tem.be.api.dao.inventory;

import com.tem.be.api.model.FirstNetInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FirstNetInventoryDao extends JpaRepository<FirstNetInventory, Long>, JpaSpecificationExecutor<FirstNetInventory> {
    List<FirstNetInventory> findByInventoryHistory_BatchId(String batchId);

    @Query("SELECT DISTINCT f.department FROM FirstNetInventory f WHERE f.department IS NOT NULL")
    List<String> findDistinctDepartments();

}
