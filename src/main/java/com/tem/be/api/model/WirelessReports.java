package com.tem.be.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Represents a wireless entity.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "wireless_reports")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class WirelessReports {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "carrier")
    private String carrier;

    @Column(name = "account")
    private String account;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "name_on_invoice")
    private String nameOnInvoice;

    @Column(name = "department")
    private String department;

    @Column(name = "last_invoice_on")
    private Date lastInvoiceOn;

    @Column(name = "viscode")
    private String viscode;

    @Column(name = "device_class")
    private String deviceClass;

    @Column(name = "device_model")
    private String deviceModel;

    @Column(name = "imei")
    private String imei;

    @Column(name = "sim")
    private String sim;

    @Column(name = "status")
    private String status;

    @Column(name = "eligibility_date")
    private String eligibilityDate;

    @Column(name = "service_plan")
    private String servicePlan;

    @Column(name = "jan25_plan_usage")
    private BigDecimal jan25PlanUsage;

    @Column(name = "feb25_plan_usage")
    private BigDecimal feb25PlanUsage;

    @Column(name = "mar25_plan_usage")
    private BigDecimal mar25PlanUsage;

    @Column(name = "plan_usage_average")
    private BigDecimal planUsageAverage;

    @Column(name = "plan_usage_total")
    private BigDecimal planUsageTotal;

    @Column(name = "jan25_data_usage")
    private BigDecimal jan25DataUsage;

    @Column(name = "feb25_data_usage")
    private BigDecimal feb25DataUsage;

    @Column(name = "mar25_data_usage")
    private BigDecimal mar25DataUsage;

    @Column(name = "data_usage_average")
    private BigDecimal dataUsageAverage;

    @Column(name = "data_usage_total")
    private BigDecimal dataUsageTotal;

    @Column(name = "jan25_messaging_usage")
    private BigDecimal jan25MessagingUsage;

    @Column(name = "feb25_messaging_usage")
    private BigDecimal feb25MessagingUsage;

    @Column(name = "mar25_messaging_usage")
    private BigDecimal mar25MessagingUsage;

    @Column(name = "messaging_usage_average")
    private BigDecimal messagingUsageAverage;

    @Column(name = "messaging_usage_total")
    private BigDecimal messagingUsageTotal;

    @Column(name = "usage_charges_average")
    private BigDecimal usageChargesAverage;

    @Column(name = "usage_charges_total")
    private BigDecimal usageChargesTotal;

    @Column(name = "access_charges_average")
    private BigDecimal accessChargesAverage;

    @Column(name = "access_charges_total")
    private BigDecimal accessChargesTotal;

    @Column(name = "equipment_charges_average")
    private BigDecimal equipmentChargesAverage;

    @Column(name = "equipment_charges_total")
    private BigDecimal equipmentChargesTotal;

    @Column(name = "other_charges_average")
    private BigDecimal otherChargesAverage;

    @Column(name = "other_charges_total")
    private BigDecimal otherChargesTotal;

    @Column(name = "taxes_and_fees_average")
    private BigDecimal taxesAndFeesAverage;

    @Column(name = "taxes_and_fees_total")
    private BigDecimal taxesAndFeesTotal;

    @Column(name = "adjustments_average")
    private BigDecimal adjustmentsAverage;

    @Column(name = "adjustments_total")
    private BigDecimal adjustmentsTotal;

    @Column(name = "jan25_total_charges")
    private BigDecimal jan25TotalCharges;

    @Column(name = "feb25_total_charges")
    private BigDecimal feb25TotalCharges;

    @Column(name = "mar25_total_charges")
    private BigDecimal mar25TotalCharges;

    @Column(name = "monthly_average")
    private BigDecimal monthlyAverage;

    @Column(name = "total")
    private BigDecimal total;
}
