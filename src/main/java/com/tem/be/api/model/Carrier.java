package com.tem.be.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

/**
 * Represents a carrier entity.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "carrier_details")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Carrier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "carrier_id")
    private Long carrierId;

    @Column(name = "carrier_name")
    private String carrierName;

    @Column(name = "carrier_number")
    private String carrierNumber;

    @Column(name = "description")
    private String description;

    @Column(name = "info")
    private String info;

    @Column(name = "is_deleted")
    private Boolean isDeleted = Boolean.FALSE;

    public Carrier(String carrierName, String carrierNumber, String description, String info) {
        this.carrierName = carrierName;
        this.carrierNumber = carrierNumber;
        this.description = description;
        this.info = info;
    }
}
