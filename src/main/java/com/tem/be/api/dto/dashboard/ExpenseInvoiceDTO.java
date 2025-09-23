package com.tem.be.api.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExpenseInvoiceDTO {
    private String userName;
    private String wirelessNumber;
    private BigDecimal total;
}
