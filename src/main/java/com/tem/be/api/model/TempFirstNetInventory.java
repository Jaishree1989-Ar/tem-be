package com.tem.be.api.model;

import com.tem.be.api.utils.MultiDateDeserializer;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "temp_firstnet_inventory")
public class TempFirstNetInventory extends TempInventoryBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "temp_inventory_id")
    private Long tempInventoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_history_id", nullable = false)
    @JsonIgnore
    private InventoryHistory inventoryHistory;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Timestamp createdAt;

    // Columns mirror FirstNetInventory
    @Temporal(TemporalType.DATE)
    @JsonDeserialize(using = MultiDateDeserializer.class)
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
    @JsonDeserialize(using = MultiDateDeserializer.class)
    private Date statusEffectiveDate;
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
    @JsonDeserialize(using = MultiDateDeserializer.class)
    private Date activationDate;
    @Temporal(TemporalType.DATE)
    @JsonDeserialize(using = MultiDateDeserializer.class)
    private Date lastUpgradeDate;
    private String upgradeInProgress;
    private String contractType;
    @Temporal(TemporalType.DATE)
    @JsonDeserialize(using = MultiDateDeserializer.class)
    private Date contractStartDate;
    @Temporal(TemporalType.DATE)
    @JsonDeserialize(using = MultiDateDeserializer.class)
    private Date contractEndDate;
    private String contractTerm;
    private String contractStatus;
    private String emailAddress;
    private String primaryPlaceOfUse;
    @Temporal(TemporalType.DATE)
    @JsonDeserialize(using = MultiDateDeserializer.class)
    private Date deviceEffectiveDate;
}
