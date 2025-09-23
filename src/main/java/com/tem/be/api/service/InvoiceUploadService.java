package com.tem.be.api.service;

import com.tem.be.api.dto.DepartmentDistinctDTO;
import com.tem.be.api.dto.InvoiceFilterDto;
import com.tem.be.api.exception.InvoiceProcessingException;
import com.tem.be.api.model.Invoiceable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;

public interface InvoiceUploadService {

    Page<Invoiceable> searchInvoices(String carrier, InvoiceFilterDto filter, Pageable pageable);

    DepartmentDistinctDTO getDistinctDepartments(String carrier);

    String processZipUpload(MultipartFile zipFile, String provider, String uploadedBy) throws IOException;

    String processDetailFile(MultipartFile detailFile, String provider, String uploadedBy) throws InvoiceProcessingException;

    Object  updateRecurringCharges(Long invoiceId, BigDecimal newRecurringCharges, String carrier);
}
