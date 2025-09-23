package com.tem.be.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;

/**
 * Represents a telecom reports entity.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "telecom_reports")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TelecomReports {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "department")
    private String department;

    @Column(name = "carrier")
    private String carrier;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "master_account")
    private String masterAccount;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "name_on_invoice")
    private String nameOnInvoice;

    @Column(name = "account_description")
    private String accountDescription;

    @Column(name = "account_owners")
    private String accountOwners;

    @Column(name = "mailing_address")
    private String mailingAddress;

    @Column(name = "device_class")
    private String deviceClass;

    @Column(name = "device_model")
    private String deviceModel;

    @Column(name = "imei")
    private String imei;

    @Column(name = "sim")
    private String sim;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "circuit_identifier")
    private String circuitIdentifier;

    @Column(name = "service_plan")
    private String servicePlan;

    @Column(name = "status")
    private String status;

//    @Temporal(TemporalType.DATE)
    @Column(name = "activated_on")
    private Date activatedOn;

//    @Temporal(TemporalType.DATE)
    @Column(name = "deactivated_on")
    private Date deactivatedOn;

//    @Temporal(TemporalType.DATE)
    @Column(name = "last_invoice_on")
    private Date lastInvoiceOn;

    @Column(name = "viscode")
    private String viscode;

    @Column(name = "monthly_recurring_cost")
    private Long monthlyRecurringCost;

    @Column(name = "billing_address")
    private String billingAddress;

    @Column(name = "billing_building_location")
    private String billingBuildingLocation;

    @Column(name = "invoice_description")
    private String invoiceDescription;

    @Column(name = "invoice_number")
    private String invoiceNumber;

    @Column(name = "invoice_amount")
    private Long invoiceAmount;

//    @Temporal(TemporalType.DATE)
    @Column(name = "invoice_date")
    private Date invoiceDate;

    @Column(name = "ap_edit_list_number")
    private String apEditListNumber;

    @Column(name = "payment_amount")
    private Long paymentAmount;

    @Column(name = "check_number")
    private String checkNumber;
}
