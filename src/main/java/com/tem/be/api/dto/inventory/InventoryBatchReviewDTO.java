package com.tem.be.api.dto.inventory;

import com.tem.be.api.model.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InventoryBatchReviewDTO {
    private InventoryHistory batchDetails;
    private List<? extends TempInventoryBase> inventoryRecords;
}
