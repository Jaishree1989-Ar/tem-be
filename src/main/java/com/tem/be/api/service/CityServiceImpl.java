package com.tem.be.api.service;

import com.tem.be.api.dao.CityDao;
import com.tem.be.api.dto.CityDTO;
import com.tem.be.api.exception.ResourceAlreadyExistsException;
import com.tem.be.api.exception.ResourceNotFoundException;
import com.tem.be.api.model.City;
import com.tem.be.api.model.Department;
import com.tem.be.api.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for city related operations.
 */
@Service
@Transactional
public class CityServiceImpl implements CityService {

    private final CityDao cityDao;
    private static final String CITY_NOT_FOUND_MSG = "City not found with id ";

    @Autowired
    public CityServiceImpl(CityDao cityDao) {
        this.cityDao = cityDao;
    }

    @Override
    public List<City> getAllCities() {
        return cityDao.findByIsDeletedFalse();
    }

    @Override
    public Optional<City> getCityById(Long id) {
        return cityDao.findById(id);
    }

    @Override
    public City createCity(CityDTO cityDTO) {
        Optional<City> existingCity = cityDao.findByCityNameAndIsDeletedFalse(cityDTO.getCityName());
        if (existingCity.isPresent()) {
            throw new ResourceAlreadyExistsException("City already exists with the name: " + cityDTO.getCityName());
        }
        return cityDao.save(new City(cityDTO.getCityName()));
    }

    @Override
    public City updateCity(Long id, CityDTO cityDTO) {
        City city = cityDao.findById(id).orElseThrow(() ->
                new ResourceNotFoundException(CITY_NOT_FOUND_MSG + id));
        if (!city.getCityName().equals(cityDTO.getCityName())){
            Optional<City> existingCity = cityDao.findByCityNameAndIsDeletedFalse(cityDTO.getCityName());
            if (existingCity.isPresent()) {
                throw new ResourceAlreadyExistsException("City already exists with the name: " + cityDTO.getCityName());
            }
        }

        city.setCityName(cityDTO.getCityName());
        return cityDao.save(city);
    }

    @Override
    public List<Department> getDepartmentsOfACityById(Long id) {
        City city = cityDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(CITY_NOT_FOUND_MSG + id));
        return city.getDepartments();
    }

    @Override
    public List<User> getUsersOfACityById(Long id) {
        City city = cityDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(CITY_NOT_FOUND_MSG + id));
        return city.getUsers();
    }

    @Override
    public void deleteCityById(Long id) {
        City existingCity =cityDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(CITY_NOT_FOUND_MSG + id));
        existingCity.setIsDeleted(true);
        cityDao.save(existingCity);
    }
}
