package com.tem.be.api.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class WiredReportsFilterDto {

    private String carrier;

    /**
     * The start date for the report's date range.
     * The format should be YYYY-MM-DD.
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    /**
     * The end date for the report's date range.
     * The format should be YYYY-MM-DD.
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate  endDate;

}