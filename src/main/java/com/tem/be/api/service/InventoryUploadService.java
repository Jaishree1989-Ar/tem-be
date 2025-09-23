package com.tem.be.api.service;

import com.tem.be.api.dto.DepartmentDistinctDTO;
import com.tem.be.api.dto.InventoryFilterDto;
import com.tem.be.api.exception.InventoryProcessingException;
import com.tem.be.api.model.Inventoryable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface InventoryUploadService {
    String processInventoryFile(MultipartFile inventoryFile, String provider, String uploadedBy) throws InventoryProcessingException;

    DepartmentDistinctDTO getDistinctDepartments(String carrier);

    Page<Inventoryable> searchInventories(String carrier, InventoryFilterDto filter, Pageable pageable);
}
