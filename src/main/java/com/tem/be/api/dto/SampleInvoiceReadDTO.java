package com.tem.be.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SampleInvoiceReadDTO {
    private String issueDate;
    private String accountNumber;
    private String foundationAccount;
    private String invoice;
}
