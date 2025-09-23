package com.tem.be.api.service;

import com.tem.be.api.dto.CarrierDTO;
import com.tem.be.api.model.Carrier;

import java.util.List;
import java.util.Optional;

public interface CarrierService {

    List<Carrier> getAllCarriers();

    Optional<Carrier> getCarrierById(Long id);

    Carrier createCarrier(CarrierDTO carrierDTO);

    Carrier updateCarrier(Long id, CarrierDTO carrierDTO);

    void deleteCarrierById(Long id);
}
