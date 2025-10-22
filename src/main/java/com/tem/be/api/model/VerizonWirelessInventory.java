package com.tem.be.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import javax.persistence.*;
import java.util.Date;

@Data
@Entity
@Table(name = "verizon_wireless_inventory")
public class VerizonWirelessInventory implements Inventoryable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_id")
    private Long inventoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_history_id")
    @JsonIgnore
    private InventoryHistory inventoryHistory;

    // Department field, populated via mapping
    private String department;

    // All 83 fields from the file mapping
    private String accountName;
    private String accountNumber;
    @Temporal(TemporalType.DATE)
    private Date billCycleDate;
    private String costCenter;
    private String emailAddress;
    private String pricePlanId;
    private String profileId;
    @Lob
    private String profileName;
    private String userId;
    private String userName;
    private String wirelessNumber;
    @Lob
    private String connectedDevice;
    @Lob
    private String cstmDtOvolWirelessnumberDeviceEuimid;
    private String currentDeviceId4gOnly;
    private String deviceSim4g;
    private String deviceManufacturer;
    private String deviceModel;
    private String deviceType;
    private String earlyUpgradeIndicator;
    @Temporal(TemporalType.DATE)
    private Date ne2Date;
    private String parentWirelessNumber;
    private String sim;
    private String simType;
    @Lob
    private String serialNumberDualSimDevicesOnly;
    private String shippedDeviceId;
    @Temporal(TemporalType.DATE)
    private Date upgradeEligibilityDate;
    @Temporal(TemporalType.DATE)
    private Date activationDate;
    private String autoPortIndicator;
    @Temporal(TemporalType.DATE)
    private Date deviceChangeLatestDate;
    @Lob
    private String deviceChangeReasonDescription;
    private String minTiedToWirelessNumber;
    @Lob
    private String preferredRoamList;
    private String preferredRoamListLastUpdate;
    @Lob
    private String wirelessNumberDeactivateDescription;
    @Temporal(TemporalType.DATE)
    private Date wirelessNumberDisconnectDate;
    @Temporal(TemporalType.DATE)
    private Date wirelessNumberResumeDate;
    private String wirelessNumberStatus;
    @Temporal(TemporalType.DATE)
    private Date wirelessNumberSuspendDate;
    @Lob
    private String wirelessNumberSuspendDescription;
    private String dataAccessCharge;
    @Lob
    private String dataPlan;
    private String dataPlanAllowance;
    private String dataPlanCode;
    @Lob
    private String pricePlanDescription;
    private String usageAndPurchaseCharges;
    private String voiceAccessCharge;
    private String voiceAllowance;
    @Lob
    private String accountCharges;
    private String economicAdjustmentCharge;
    private String equipmentCharges;
    private String internationalCharges;
    private String monthlyAccessCharges;
    private String monthlyNonRecurringCharges;
    @Lob
    private String otherChargesAndCredits;
    @Lob
    private String phones;
    @Lob
    private String purchaseCharges;
    @Lob
    private String taxesAndSurcharges;
    @Lob
    private String thirdPartyCharges;
    private String totalAdditionalCharges;
    private String totalCurrentCharges;
    private String billableMinutes;
    @Lob
    private String longDistanceOtherCharges;
    private String minutes;
    private String totalAllowanceMinutes;
    @Lob
    private String totalCallDetail;
    private String usedMinutes;
    @Lob
    private String additionalChargesData;
    @Lob
    private String additionalServicesDataUsage;
    @Lob
    private String additionalServicesMessagingUsage;
    private String currentDataCharges;
    private String dataChargesHome;
    private String dataOverageCharges;
    private String dataUsage;
    private String delayedDataCharges;
    private String totalDataUsageCharges;
    private String messagingCharges;
    private String mobileToMobileAllowanceMinutes;
    private String mobileToMobileMinutes;
    private String mobileToMobileMinutesTotal;
    private String mobileToMobileUsedMinutes;
}
