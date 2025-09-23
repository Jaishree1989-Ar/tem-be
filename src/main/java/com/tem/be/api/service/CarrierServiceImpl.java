package com.tem.be.api.service;

import com.tem.be.api.dao.CarrierDao;
import com.tem.be.api.dto.CarrierDTO;
import com.tem.be.api.exception.ResourceAlreadyExistsException;
import com.tem.be.api.exception.ResourceNotFoundException;
import com.tem.be.api.model.Carrier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for carrier related operations.
 */
@Service
@Transactional
public class CarrierServiceImpl implements CarrierService {

    private final CarrierDao carrierDao;

    @Autowired
    public CarrierServiceImpl(CarrierDao carrierDao) {
        this.carrierDao = carrierDao;
    }

    @Override
    public List<Carrier> getAllCarriers() {
        return carrierDao.findByIsDeletedFalse();
    }

    @Override
    public Optional<Carrier> getCarrierById(Long id) {
        return carrierDao.findById(id);
    }

    @Override
    public Carrier createCarrier(CarrierDTO carrierDTO) {
        Optional<Carrier> existingCarrier = carrierDao.findByCarrierNameAndIsDeletedFalse(carrierDTO.getCarrierName());
        if (existingCarrier.isPresent()) {
            throw new ResourceAlreadyExistsException("Carrier already exists with the name: " + carrierDTO.getCarrierName());
        }
        return carrierDao.save(
                new Carrier(carrierDTO.getCarrierName(), carrierDTO.getCarrierNumber(), carrierDTO.getDescription(), carrierDTO.getInfo())
        );
    }

    @Override
    public Carrier updateCarrier(Long id, CarrierDTO carrierDTO) {
        Carrier carrier = carrierDao.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("Carrier not found with id  " + id));
        if(!carrier.getCarrierName().equals(carrierDTO.getCarrierName())){
            Optional<Carrier> existingCarrier = carrierDao.findByCarrierNameAndIsDeletedFalse(carrierDTO.getCarrierName());
            if (existingCarrier.isPresent()) {
                throw new ResourceAlreadyExistsException("Carrier already exists with the name: " + carrierDTO.getCarrierName());
            }
        }
        carrier.setCarrierName(carrierDTO.getCarrierName());
        carrier.setCarrierNumber(carrierDTO.getCarrierNumber());
        carrier.setDescription(carrierDTO.getDescription());
        carrier.setInfo(carrierDTO.getInfo());
        return carrierDao.save(carrier);
    }

    @Override
    public void deleteCarrierById(Long id) {
        Carrier existingCarrier = carrierDao.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("Carrier not found with id  " + id));
        existingCarrier.setIsDeleted(true);
        carrierDao.save(existingCarrier);
    }
}
