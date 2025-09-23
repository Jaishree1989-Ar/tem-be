package com.tem.be.api.controller;

import com.tem.be.api.dto.CarrierDTO;
import com.tem.be.api.exception.ResourceNotFoundException;
import com.tem.be.api.model.Carrier;
import com.tem.be.api.service.CarrierService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing carrier related operations.
 */
@Log4j2
@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/carrier")
public class CarrierController {

    private final CarrierService carrierService;

    /**
     * @param carrierService the service handling carrier operations
     */
    @Autowired
    public CarrierController(CarrierService carrierService) {
        this.carrierService = carrierService;
    }

    /**
     * Returns all carriers.
     */
    @GetMapping(value = "/getAll", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Carrier>> getAllCarriers() {
        log.info("CarrierController.getAllCarriers() >> Entered");
        List<Carrier> carriers = carrierService.getAllCarriers();
        log.info("CarrierController.getAllCarriers() >> Exited");
        return new ResponseEntity<>(carriers, HttpStatus.OK);
    }

    /**
     * Returns a carrier by its ID.
     *
     * @param id carrier ID
     */
    @GetMapping(value = "/getById/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Carrier> getById(@PathVariable("id") Long id) {
        log.info("CarrierController.getById() >> Entered with id: {}", id);
        Carrier carrier = carrierService.getCarrierById(id).orElseThrow(() -> new ResourceNotFoundException("Carrier with id " + id + " not found"));
        log.info("CarrierController.getById() >> Exited");
        return new ResponseEntity<>(carrier, HttpStatus.OK);
    }

    /**
     * Creates a new carrier.
     *
     * @param carrierDTO carrier details
     */
    @PostMapping(value = "/createCarrier", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Carrier> createCarrier(@RequestBody CarrierDTO carrierDTO) {
        log.info("CarrierController.createCarrier() >> Entered");
        Carrier car = carrierService.createCarrier(carrierDTO);
        log.info("CarrierController.createCarrier() >> Exited");
        return new ResponseEntity<>(car, HttpStatus.CREATED);
    }

    /**
     * Updates an existing carrier by ID.
     *
     * @param id carrier ID
     * @param carrierDTO updated details
     */
    @PutMapping(value = "/updateById/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Carrier> updateCarrier(@PathVariable("id") Long id, @RequestBody CarrierDTO carrierDTO) {
        log.info("CarrierController.updateCarrier() >> Entered with id: {}", id);
        Carrier updatedCarrier = carrierService.updateCarrier(id, carrierDTO);
        log.info("CarrierController.updateCarrier() >> Exited");
        return new ResponseEntity<>(updatedCarrier, HttpStatus.OK);
    }

    /**
     * Deletes a carrier by ID.
     *
     * @param id carrier ID
     */
    @DeleteMapping("/deleteCarrierById/{id}")
    public ResponseEntity<String> deleteCarrierById(@PathVariable("id") Long id) {
        log.info("CarrierController.deleteCarrierById() >> Entered");
        carrierService.deleteCarrierById(id);
        log.info("CarrierController.deleteCarrierById() >> Exited");
        return new ResponseEntity<>("Carrier deleted successfully!", HttpStatus.OK);
    }


}
