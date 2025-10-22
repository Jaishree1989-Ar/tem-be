package com.tem.be.api.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "temp_verizon_wireless_invoices")
public class TempVerizonWirelessInvoice extends TempInvoiceBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invoice_id")
    private Long invoiceId;

    @JsonAlias("Account number")
    @Column(name = "account_number")
    private String accountNumber;

    @JsonAlias("Account status code")
    @Column(name = "account_status_code")
    private String accountStatusCode;

    @JsonAlias("Account status description")
    @Column(name = "account_status_description")
    private String accountStatusDescription;

    @JsonAlias("Bill account create date")
    @Temporal(TemporalType.DATE)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "M/d/yyyy")
    @Column(name = "bill_account_create_date")
    private Date billAccountCreateDate;

    // Business Logic: "Bill address level 1" is treated as the department
    @JsonAlias("Bill address level 1")
    @Column(name = "department")
    private String department;

    @JsonAlias("Bill address level 2")
    @Column(name = "bill_address_level_2")
    private String billAddressLevel2;

    @JsonAlias("Bill address level 3")
    @Column(name = "bill_address_level_3")
    private String billAddressLevel3;

    @JsonAlias("Bill business number")
    @Column(name = "bill_business_number")
    private String billBusinessNumber;

    @JsonAlias("Bill city")
    @Column(name = "bill_city")
    private String billCity;

    @JsonAlias("Bill contact name")
    @Column(name = "bill_contact_name")
    private String billContactName;

    @JsonAlias("Bill contact number")
    @Column(name = "bill_contact_number")
    private String billContactNumber;

    @JsonAlias("Bill state")
    @Column(name = "bill_state")
    private String billState;

    @JsonAlias("Bill zip")
    @Column(name = "bill_zip")
    private String billZip;

    @JsonAlias("Recurring payment method")
    @Column(name = "recurring_payment_method")
    private String recurringPaymentMethod;

    @JsonAlias("Recurring payment setup")
    @Column(name = "recurring_payment_setup")
    private String recurringPaymentSetup;

    @JsonAlias("Account charges and credits")
    @Column(name = "account_charges_and_credits", precision = 19, scale = 4)
    private BigDecimal accountChargesAndCredits;

    @JsonAlias("Account level share")
    @Column(name = "account_level_share")
    private String accountLevelShare;

    @JsonAlias("Adjustments")
    @Column(name = "adjustments", precision = 19, scale = 4)
    private BigDecimal adjustments;

    @JsonAlias("Balance forward")
    @Column(name = "balance_forward", precision = 19, scale = 4)
    private BigDecimal balanceForward;

    @JsonAlias("Equipment charges")
    @Column(name = "equipment_charges", precision = 19, scale = 4)
    private BigDecimal equipmentCharges;

    @JsonAlias("Late fee")
    @Column(name = "late_fee", precision = 19, scale = 4)
    private BigDecimal lateFee;

    @JsonAlias("Monthly charges")
    @Column(name = "monthly_charges", precision = 19, scale = 4)
    private BigDecimal monthlyCharges;

    @JsonAlias("Payments")
    @Column(name = "payments", precision = 19, scale = 4)
    private BigDecimal payments;

    @JsonAlias("Previous balance")
    @Column(name = "previous_balance", precision = 19, scale = 4)
    private BigDecimal previousBalance;

    @JsonAlias("Surcharges and OC&Cs")
    @Column(name = "surcharges_and_occs", precision = 19, scale = 4)
    private BigDecimal surchargesAndOccs;

    @JsonAlias("Taxes, governmental surcharges, and fees")
    @Column(name = "taxes_gov_surcharges_and_fees", precision = 19, scale = 4)
    private BigDecimal taxesGovSurchargesAndFees;

    @JsonAlias("Third-party charges to account")
    @Column(name = "third_party_charges_to_account", precision = 19, scale = 4)
    private BigDecimal thirdPartyChargesToAccount;

    @JsonAlias("Third-party charges to lines")
    @Column(name = "third_party_charges_to_lines", precision = 19, scale = 4)
    private BigDecimal thirdPartyChargesToLines;

    @JsonAlias("Total amount due")
    @Column(name = "total_amount_due", precision = 19, scale = 4)
    private BigDecimal totalAmountDue;

    @JsonAlias("Total current charges")
    @Column(name = "total_current_charges", precision = 19, scale = 4)
    private BigDecimal totalCurrentCharges;

    @JsonAlias("Usage and purchase charges")
    @Column(name = "usage_and_purchase_charges", precision = 19, scale = 4)
    private BigDecimal usageAndPurchaseCharges;

    @JsonAlias("Usage charges - data")
    @Column(name = "usage_charges_data", precision = 19, scale = 4)
    private BigDecimal usageChargesData;

    @JsonAlias("Usage charges - purchases")
    @Column(name = "usage_charges_purchases", precision = 19, scale = 4)
    private BigDecimal usageChargesPurchases;

    @JsonAlias("Usage charges - roaming")
    @Column(name = "usage_charges_roaming", precision = 19, scale = 4)
    private BigDecimal usageChargesRoaming;

    @JsonAlias("Usage charges - voice")
    @Column(name = "usage_charges_voice", precision = 19, scale = 4)
    private BigDecimal usageChargesVoice;

    @JsonAlias("Bill name")
    @Column(name = "bill_name")
    private String billName;

    @JsonAlias("Bill period")
    @Column(name = "bill_period")
    private String billPeriod;

    @Temporal(TemporalType.DATE)
    @Column(name = "bill_period_start")
    private Date billPeriodStart;

    @Temporal(TemporalType.DATE)
    @Column(name = "bill_period_end")
    private Date billPeriodEnd;

    @JsonAlias("Date due")
    @Temporal(TemporalType.DATE)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "M/d/yyyy")
    @Column(name = "date_due")
    private Date dateDue;

    @JsonAlias("Remittance address")
    @Column(name = "remittance_address", length = 512)
    private String remittanceAddress;

    @Column(name = "total_reoccurring_charges", precision = 19, scale = 4)
    private BigDecimal totalReoccurringCharges;
}
