package com.tem.be.api.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DepartmentExpenseDTO {
    private String department;
    private List<ExpenseInvoiceDTO> invoices;
    private BigDecimal total;
}
