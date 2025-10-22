package com.tem.be.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents a single detail line item from a Wired report.
 * This entity maps to the 'wired_reports' table in the database.
 */
@Entity
@Table(name = "wired_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WiredReports {

    /**
     * The unique identifier for each record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "carrier")
    private String carrier;

    @Column(name = "section")
    private String section;

    @Column(name = "invoice_number")
    private String invoiceNumber;

    @Column(name = "invoice_date")
    private LocalDate invoiceDate;

    @Column(name = "ban")
    private String ban;

    @Column(name = "subgroup")
    private String subgroup;

    @Column(name = "btn")
    private String btn;

    @Column(name = "btn_description", length = 512)
    private String btnDescription;

    @Column(name = "svc_id")
    private String svcId;

    @Column(name = "item_number")
    private String itemNumber;

    @Column(name = "provider")
    private String provider;

    @Column(name = "contract_ref")
    private String contract;

    @Column(name = "product_id")
    private String productId;

    @Column(name = "feature_name", length = 512)
    private String featureName;

    @Column(name = "description", length = 1024)
    private String description;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "minutes")
    private BigDecimal minutes;

    @Column(name = "contract_rate", precision = 19, scale = 4)
    private BigDecimal contractRate;

    @Column(name = "total_charge", precision = 19, scale = 4)
    private BigDecimal totalCharge;

    @Column(name = "saaf_calculation")
    private String saafCalculation;

    @Column(name = "charge_type")
    private String chargeType;

    @Column(name = "bill_period")
    private String billPeriod;

    @Column(name = "action")
    private String action;

    @Column(name = "sr_number")
    private String srNumber;

    @Column(name = "node")
    private String node;

    @Column(name = "svc_address_1")
    private String svcAddress1;

    @Column(name = "svc_address_2")
    private String svcAddress2;

    @Column(name = "svc_city")
    private String svcCity;

    @Column(name = "svc_state")
    private String svcState;

    @Column(name = "svc_zip")
    private String svcZip;

    @Column(name = "viscode")
    private String viscode;
}
