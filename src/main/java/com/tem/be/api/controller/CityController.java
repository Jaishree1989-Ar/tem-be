package com.tem.be.api.controller;

import com.tem.be.api.dto.CityDTO;
import com.tem.be.api.exception.ResourceNotFoundException;
import com.tem.be.api.model.City;
import com.tem.be.api.model.Department;
import com.tem.be.api.model.User;
import com.tem.be.api.service.CityService;
import com.tem.be.api.utils.ApiResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing city related operations.
 */
@Log4j2
@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/city")
public class CityController {

    private final CityService cityService;

    /**
     * @param cityService the service handling city operations
     */
    @Autowired
    public CityController(CityService cityService) {
        this.cityService = cityService;
    }

    /**
     * Returns all cities.
     */
    @GetMapping(value = "/getAll", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<City>>> getAllCities() {
        log.info("CityController.getAllCities() >> Entered");
        List<City> cities = cityService.getAllCities();
        ApiResponse<List<City>> response = new ApiResponse<>(HttpStatus.OK.value(), "Cities fetched successfully", cities);
        log.info("CityController.getAllCities() >> Exited");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Returns a city by its ID.
     *
     * @param id city ID
     */
    @GetMapping(value = "/getById/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<City> getById(@PathVariable("id") Long id) {
        log.info("CityController.getById() >> Entered with id: {}", id);
        City city = cityService.getCityById(id).orElseThrow(() -> new ResourceNotFoundException("City with id " + id + " not found"));
        log.info("CityController.getById() >> Exited");
        return new ResponseEntity<>(city, HttpStatus.OK);
    }

    /**
     * Creates a new city.
     *
     * @param cityDTO city details
     */
    @PostMapping(value = "/createCity", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<City> createCity(@RequestBody CityDTO cityDTO) {
        log.info("CityController.createCity() >> Entered");
        City city = cityService.createCity(cityDTO);
        log.info("CityController.createCity() >> Exited");
        return new ResponseEntity<>(city, HttpStatus.CREATED);
    }

    /**
     * Updates an existing city by ID.
     *
     * @param id      city ID
     * @param cityDTO updated city details
     */
    @PutMapping(value = "/updateById/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<City> updateCity(@PathVariable("id") Long id, @RequestBody CityDTO cityDTO) {
        log.info("CityController.updateCity() >> Entered with id: {}", id);
        City updatedCity = cityService.updateCity(id, cityDTO);
        log.info("CityController.updateCity() >> Exited");
        return new ResponseEntity<>(updatedCity, HttpStatus.OK);
    }

    /**
     * Returns all departments for a given city ID.
     *
     * @param id city ID
     */
    @GetMapping(value = "/getDepartmentsOfACityById/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Department>> getDepartmentsOfACityById(@PathVariable("id") Long id) {
        log.info("CityController.getDepartmentsOfACityById() >> Entered with id: {}", id);
        List<Department> departments = cityService.getDepartmentsOfACityById(id);
        log.info("CityController.getDepartmentsOfACityById() >> Exited");
        return new ResponseEntity<>(departments, HttpStatus.OK);
    }

    /**
     * Returns all users for a given city ID.
     *
     * @param id city ID
     */
    @GetMapping(value = "/getUsersOfACityById/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<User>> getUsersOfACityById(@PathVariable("id") Long id) {
        log.info("CityController.getUsersOfACityById() >> Entered with id: {}", id);
        List<User> users = cityService.getUsersOfACityById(id);
        log.info("CityController.getUsersOfACityById() >> Exited");
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    /**
     * Deletes a city by ID.
     *
     * @param id city ID
     */
    @DeleteMapping("/deleteCityById/{id}")
    public ResponseEntity<String> deleteCityById(@PathVariable("id") Long id) {
        log.info("CityController.deleteCityById() >> Entered");
        cityService.deleteCityById(id);
        log.info("CityController.deleteCityById() >> Exited");
        return new ResponseEntity<>("City deleted successfully!", HttpStatus.OK);
    }
}
