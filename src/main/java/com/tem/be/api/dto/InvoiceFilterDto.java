package com.tem.be.api.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.util.List;

/**
 * DTO to hold all possible filter criteria for searching invoices.
 * Spring will automatically map request parameters to the fields of this object.
 */
@Data
public class InvoiceFilterDto {

    private List<String> departments;
    private List<String> deviceClasses;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private Date startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private Date endDate;

    private String keyword;
}
