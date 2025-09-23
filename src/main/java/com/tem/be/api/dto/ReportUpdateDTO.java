package com.tem.be.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data Transfer Object (DTO) representing wireless report update(department, viscode and name on invoice) information.
 * Used to transfer reports related data between layers of the application.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReportUpdateDTO {
    private String department;
    private String viscode;
    private String nameOnInvoice;

}
