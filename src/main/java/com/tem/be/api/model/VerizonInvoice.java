package com.tem.be.api.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Date;

/**
 * Represents Verizon invoice entity.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "verizon_invoices")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class VerizonInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Long roleId;

    @CreationTimestamp
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "created_at", updatable = false)
    private Timestamp createdAt;

    @UpdateTimestamp
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "updated_at")
    private Timestamp updatedAt;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "invoice_number")
    private String invoiceNumber;

    @Column(name = "invoice_date")
    @Temporal(TemporalType.DATE)
    @JsonFormat(shape = JsonFormat.Shape.STRING,pattern ="yyyy-MM-dd")
    private Date invoiceDate;

//    @Column(name = "invoice_amount")
    @Column(name = "invoice_amount")
    private String invoiceAmount;

    @Column(name = "account_name")
    private String accountName;

    @Column(name = "corp_group")
    private String corpGroup;

    @Column(name = "billing_city")
    private String billingCity;

    @Column(name = "billing_state")
    private String billingState;

    @Column(name = "global_billing_report")
    private String globalBillingReport;

    @Column(name = "new_account_number")
    private String newAccountNumber;

//    @Column(name = "previous_charges")
    @Column(name = "previous_charges")
    private String previousCharges;

    @Column(name = "paper_status")
    private String paperStatus;

    @Column(name = "payment_due_date")
    @Temporal(TemporalType.DATE)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern ="yyyy-MM-dd")
    private Date paymentDueDate;

//    @Column(name = "account_balance")
    @Column(name = "account_balance")
    private String accountBalance;

    @Column(name = "payment_status")
    private String paymentStatus;

    @Column(name = "open_inquiry_ind")
    private String openInquiryInd;

}
