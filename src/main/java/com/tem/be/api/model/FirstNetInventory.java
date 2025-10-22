package com.tem.be.api.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import javax.persistence.*;
import java.util.Date;

/**
 * Represents a firstnet inventory entity.
 */
@Data
@Entity
@Table(name = "firstnet_inventory")
public class FirstNetInventory implements Inventoryable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_id")
    private Long inventoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_history_id")
    @JsonIgnore
    private InventoryHistory inventoryHistory;

    @Temporal(TemporalType.DATE)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM/dd/yyyy")
    private Date lastUpdatedDate;
    private String foundationAccount;
    private String foundationAccountName;
    private String billingAccountNumber;
    private String billingAccountName;
    private String wirelessNumber;
    private String wirelessUserName;
    @JsonAlias("Status")
    private String deviceStatus;
    @Temporal(TemporalType.DATE)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM/dd/yyyy")
    private Date statusEffectiveDate;
    @Column(name = "department")
    private String department; // Mapped from DEPARTMENT
    private String ratePlanName;
    private String udl2;
    private String assetTag;
    private String udl4;
    private String deviceType;
    private String deviceImei;
    private String deviceMake;
    private String deviceModel;
    private String networkImeiMismatch;
    private String deviceImeiNetwork;
    private String deviceMakeNetwork;
    private String deviceModelNetwork;
    private String operatingSystem;
    private String operatingSystemVersion;
    private String imeiSoftwareVersion;
    private String simType;
    private String simNetwork;
    private String simNumberIccid;
    private String ratePlanSocName;
    private String groupId;
    private String groupPlanSocName;
    private String groupLineSocName;
    private String primaryLine;
    @Temporal(TemporalType.DATE)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM/dd/yyyy")
    private Date activationDate;
    @Temporal(TemporalType.DATE)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM/dd/yyyy")
    private Date lastUpgradeDate;
    private String upgradeInProgress;
    private String contractType;
    @Temporal(TemporalType.DATE)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM/dd/yyyy")
    private Date contractStartDate;
    @Temporal(TemporalType.DATE)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM/dd/yyyy")
    private Date contractEndDate;
    private String contractTerm;
    private String contractStatus;
    private String emailAddress;
    private String primaryPlaceOfUse;
    @Temporal(TemporalType.DATE)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM/dd/yyyy")
    private Date deviceEffectiveDate;
}