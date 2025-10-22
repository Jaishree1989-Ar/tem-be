package com.tem.be.api.dto;

import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateViscodeRequest {

    @NotBlank(message = "New viscode cannot be blank")
    private String newViscode;

    @NotBlank(message = "Carrier cannot be blank")
    private String carrier;
}