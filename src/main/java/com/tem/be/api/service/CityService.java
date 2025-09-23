package com.tem.be.api.service;

import com.tem.be.api.dto.CityDTO;
import com.tem.be.api.model.City;
import com.tem.be.api.model.Department;
import com.tem.be.api.model.User;

import java.util.List;
import java.util.Optional;

public interface CityService {

    List<City> getAllCities();

    Optional<City> getCityById(Long id);

    City createCity(CityDTO cityDTO);

    City updateCity(Long id, CityDTO cityDTO);

    List<Department> getDepartmentsOfACityById(Long id);

    List<User> getUsersOfACityById(Long id);

    void deleteCityById(Long id);
}
