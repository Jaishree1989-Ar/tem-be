package com.tem.be.api.model;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.*;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "temp_att_invoices")
public class TempATTInvoice extends TempInvoiceBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invoice_id")
    private Long invoiceId;

    @JsonAlias("Foundation account")
    @Column(name = "foundation_account")
    private String foundationAccount;

    @JsonAlias("Foundation account name")
    @Column(name = "foundation_account_name")
    private String foundationAccountName;

    @JsonAlias("Account number")
    @Column(name = "account_number")
    private String accountNumber;

    @JsonAlias("Account and descriptions")
    @Column(name = "account_and_descriptions")
    private String accountAndDescriptions;

    @JsonAlias("Billing account name")
    @Column(name = "billing_account_name")
    private String billingAccountName;

    @JsonAlias("Wireless number and descriptions")
    @Column(name = "wireless_number_and_descriptions")
    private String wirelessNumberAndDescriptions;

    @JsonAlias("Wireless number")
    @Column(name = "wireless_number")
    private String wirelessNumber;

    @JsonAlias("DEPARTMENT NAME") // From AT&T sample
    @Column(name = "department")
    private String department;

    @JsonAlias("User name")
    @Column(name = "user_name")
    private String userName;

    @JsonAlias("VIS CODE") // From AT&T sample
    @Column(name = "vis_code")
    private String visCode;

    @JsonAlias("ASSET TAG") // From AT&T sample
    @Column(name = "asset_tag")
    private String assetTag;

    @JsonAlias("Market cycle end date")
    @Temporal(TemporalType.DATE)
    @Column(name = "market_cycle_end_date")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM/dd/yyyy")
    private Date marketCycleEndDate;

    @JsonAlias("Invoice date")
    @Temporal(TemporalType.DATE)
    @Column(name = "invoice_date")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM/dd/yyyy")
    private Date invoiceDate;

    @JsonAlias("Rate code")
    @Column(name = "rate_code")
    private String rateCode;

    @JsonAlias("Rate plan name")
    @Column(name = "rate_plan_name")
    private String ratePlanName;

    @JsonAlias("Group ID")
    @Column(name = "group_id")
    private String groupId;

    @JsonAlias("Total current charges")
    @Column(name = "total_current_charges")
    private String totalCurrentCharges;

    @JsonAlias("Total monthly charges")
    @Column(name = "total_monthly_charges")
    private String totalMonthlyCharges;

    @JsonAlias("Total activity since last bill")
    @Column(name = "total_activity_since_last_bill")
    private String totalActivitySinceLastBill;

    @JsonAlias("Total taxes")
    @Column(name = "total_taxes")
    private String totalTaxes;

    @JsonAlias("Total company fees and surcharges")
    @Column(name = "total_company_fees_and_surcharges")
    private String totalCompanyFeesAndSurcharges;

    @JsonAlias("Total KB usage")
    @Column(name = "total_kb_usage")
    private String totalKbUsage;

    @JsonAlias("Total minutes usage")
    @Column(name = "total_minutes_usage")
    private String totalMinutesUsage;

    @JsonAlias("Total messages")
    @Column(name = "total_messages")
    private String totalMessages;

    @JsonAlias("Total FAN level charges")
    @Column(name = "total_fan_level_charges")
    private String totalFanLevelCharges;

    @JsonAlias("Total adjustments")
    @Column(name = "total_adjustments")
    private String totalAdjustments;

    @Column(name = "total_reoccurring_charges", precision = 19, scale = 4)
    private BigDecimal totalReoccurringCharges;
}
