package com.tem.be.api.dao;

import com.tem.be.api.model.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for accessing and managing city entities.
 */
@Repository
public interface CityDao extends JpaRepository<City, Long> {
    Optional<City> findByCityNameAndIsDeletedFalse(String cityName);
    List<City> findByIsDeletedFalse();
}
