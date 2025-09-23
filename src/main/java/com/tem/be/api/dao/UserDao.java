package com.tem.be.api.dao;

import com.tem.be.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for accessing and managing User entities.
 */
@Repository
public interface UserDao extends JpaRepository<User, Long> {
    Optional<User> findByEmailAndIsDeletedFalse(String email);
    List<User> findByIsDeletedFalseOrderByUpdatedAtDesc();
    Optional<User> findByEmail(String email);
}
