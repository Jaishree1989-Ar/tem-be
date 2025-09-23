package com.tem.be.api.dto.inventory;

import com.tem.be.api.model.InventoryHistory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApprovedInventoryBatchDTO {
    private InventoryHistory batchDetails;
    private List<?> inventoryRecords;
}
