package com.tem.be.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "verizon_wireless_invoices")
public class VerizonWirelessInvoice implements Invoiceable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invoice_id")
    private Long invoiceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_history_id")
    @JsonIgnore
    private InvoiceHistory invoiceHistory;

    @Column(name = "invoice_number")
    private String invoiceNumber;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "account_status_code")
    private String accountStatusCode;

    @Column(name = "account_status_description")
    private String accountStatusDescription;

    @Temporal(TemporalType.DATE)
    @Column(name = "bill_account_create_date")
    private Date billAccountCreateDate;

    @Column(name = "department")
    private String department;

    @Column(name = "bill_address_level_2")
    private String billAddressLevel2;

    @Column(name = "bill_address_level_3")
    private String billAddressLevel3;

    @Column(name = "bill_business_number")
    private String billBusinessNumber;

    @Column(name = "bill_city")
    private String billCity;

    @Column(name = "bill_contact_name")
    private String billContactName;

    @Column(name = "bill_contact_number")
    private String billContactNumber;

    @Column(name = "bill_state")
    private String billState;

    @Column(name = "bill_zip")
    private String billZip;

    @Column(name = "recurring_payment_method")
    private String recurringPaymentMethod;

    @Column(name = "recurring_payment_setup")
    private String recurringPaymentSetup;

    @Column(name = "account_charges_and_credits", precision = 19, scale = 4)
    private BigDecimal accountChargesAndCredits;

    @Column(name = "account_level_share")
    private String accountLevelShare;

    @Column(name = "adjustments", precision = 19, scale = 4)
    private BigDecimal adjustments;

    @Column(name = "balance_forward", precision = 19, scale = 4)
    private BigDecimal balanceForward;

    @Column(name = "equipment_charges", precision = 19, scale = 4)
    private BigDecimal equipmentCharges;

    @Column(name = "late_fee", precision = 19, scale = 4)
    private BigDecimal lateFee;

    @Column(name = "monthly_charges", precision = 19, scale = 4)
    private BigDecimal monthlyCharges;

    @Column(name = "payments", precision = 19, scale = 4)
    private BigDecimal payments;

    @Column(name = "previous_balance", precision = 19, scale = 4)
    private BigDecimal previousBalance;

    @Column(name = "surcharges_and_occs", precision = 19, scale = 4)
    private BigDecimal surchargesAndOccs;

    @Column(name = "taxes_gov_surcharges_and_fees", precision = 19, scale = 4)
    private BigDecimal taxesGovSurchargesAndFees;

    @Column(name = "third_party_charges_to_account", precision = 19, scale = 4)
    private BigDecimal thirdPartyChargesToAccount;

    @Column(name = "third_party_charges_to_lines", precision = 19, scale = 4)
    private BigDecimal thirdPartyChargesToLines;

    @Column(name = "total_amount_due", precision = 19, scale = 4)
    private BigDecimal totalAmountDue;

    @Column(name = "total_current_charges", precision = 19, scale = 4)
    private BigDecimal totalCurrentCharges;

    @Column(name = "usage_and_purchase_charges", precision = 19, scale = 4)
    private BigDecimal usageAndPurchaseCharges;

    @Column(name = "usage_charges_data", precision = 19, scale = 4)
    private BigDecimal usageChargesData;

    @Column(name = "usage_charges_purchases", precision = 19, scale = 4)
    private BigDecimal usageChargesPurchases;

    @Column(name = "usage_charges_roaming", precision = 19, scale = 4)
    private BigDecimal usageChargesRoaming;

    @Column(name = "usage_charges_voice", precision = 19, scale = 4)
    private BigDecimal usageChargesVoice;

    @Column(name = "bill_name")
    private String billName;

    @Column(name = "bill_period")
    private String billPeriod;

    @Temporal(TemporalType.DATE)
    @Column(name = "bill_period_start")
    private Date billPeriodStart;

    @Temporal(TemporalType.DATE)
    @Column(name = "bill_period_end")
    private Date billPeriodEnd;

    @Temporal(TemporalType.DATE)
    @Column(name = "date_due")
    private Date dateDue;

    @Column(name = "remittance_address", length = 512)
    private String remittanceAddress;

    @Column(name = "total_reoccurring_charges", precision = 19, scale = 4)
    private BigDecimal totalReoccurringCharges;
}
