package com.tem.be.api.service;

import com.tem.be.api.dao.CityDao;
import com.tem.be.api.dao.DepartmentDao;
import com.tem.be.api.dao.RoleDao;
import com.tem.be.api.dao.UserDao;
import com.tem.be.api.dto.UserDTO;
import com.tem.be.api.exception.ResourceAlreadyExistsException;
import com.tem.be.api.exception.ResourceNotFoundException;
import com.tem.be.api.model.City;
import com.tem.be.api.model.Department;
import com.tem.be.api.model.Role;
import com.tem.be.api.model.User;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for user related operations.
 */
@Service
@Transactional
@Log4j2
public class UserServiceImpl implements UserService {

    private final UserDao userDao;
    private final CityDao cityDao;
    private final RoleDao roleDao;
    private final DepartmentDao departmentDao;
    private final PasswordEncoder passwordEncoder;

    private static class ErrorMessages {
        public static final String EMAIL_ALREADY_EXISTS = "Email already exists: %s";
        public static final String ROLE_NOT_FOUND = "Role not found with ID: %d";
        public static final String CITY_NOT_FOUND = "City not found with ID: %d";
        public static final String DEPT_NOT_FOUND = "Department not found with ID: %d";
        public static final String USER_NOT_FOUND_ID = "User not found with ID: %d";
        public static final String USER_NOT_FOUND_EMAIL = "User not found with email: %s";
        public static final String INVALID_PASSWORD = "Invalid password";
        public static final String NEW_PASSWORD_LENGTH = "New password must be at least 8 characters long.";
        public static final String OLD_PASSWORD_INCORRECT = "The old password you entered is incorrect.";
        public static final String PASSWORD_SAME_AS_OLD = "New password cannot be the same as the old password.";
    }

    @Autowired
    public UserServiceImpl(UserDao userDao, CityDao cityDao, RoleDao roleDao, DepartmentDao departmentDao, PasswordEncoder passwordEncoder) {
        this.userDao = userDao;
        this.cityDao = cityDao;
        this.roleDao = roleDao;
        this.departmentDao = departmentDao;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User createUser(UserDTO userDTO) {
        Optional<User> existingUser = userDao.findByEmailAndIsDeletedFalse(userDTO.getEmail());
        if (existingUser.isPresent()) {
            throw new ResourceAlreadyExistsException(String.format(ErrorMessages.EMAIL_ALREADY_EXISTS, userDTO.getEmail()));
        }

        String tempPassword = System.getenv("SECURE_TEMP_PASSWORD");
        if (tempPassword == null || tempPassword.isEmpty()) {
            throw new IllegalStateException("Environment variable SECURE_TEMP_PASSWORD not set.");
        }
        String encodedPassword = passwordEncoder.encode(tempPassword);

        // Fetch related entities
        Role role = roleDao.findById(userDTO.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessages.ROLE_NOT_FOUND, userDTO.getRoleId())));

        City city = cityDao.findById(userDTO.getCityId())
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessages.CITY_NOT_FOUND, userDTO.getCityId())));

        Department department = departmentDao.findById(userDTO.getDeptId())
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessages.DEPT_NOT_FOUND, userDTO.getDeptId())));

        User user = new User(
                userDTO.getUserName(),
                userDTO.getEmail(),
                encodedPassword,
                userDTO.getPhoneNumber(),
                role,
                city,
                department
        );

        return userDao.save(user);
    }

    @Override
    public User updateUserById(Long id, UserDTO userDTO) {
        User existingUser = userDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessages.USER_NOT_FOUND_ID, id)));

        // Check email uniqueness (if email changed)
        if (!existingUser.getEmail().equals(userDTO.getEmail())) {
            Optional<User> userWithEmail = userDao.findByEmailAndIsDeletedFalse(userDTO.getEmail());
            if (userWithEmail.isPresent()) {
                throw new ResourceAlreadyExistsException(String.format(ErrorMessages.EMAIL_ALREADY_EXISTS, userDTO.getEmail()));
            }
        }

        // Fetch role, city, and department
        Role role = roleDao.findById(userDTO.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessages.ROLE_NOT_FOUND, userDTO.getRoleId())));

        City city = cityDao.findById(userDTO.getCityId())
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessages.CITY_NOT_FOUND, userDTO.getCityId())));

        Department department = departmentDao.findById(userDTO.getDeptId())
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessages.DEPT_NOT_FOUND, userDTO.getDeptId())));

        // Update user fields
        existingUser.setUserName(userDTO.getUserName());
        existingUser.setEmail(userDTO.getEmail());// optionally hash it
        existingUser.setPhoneNumber(userDTO.getPhoneNumber());
        existingUser.setRole(role);
        existingUser.setCity(city);
        existingUser.setDepartment(department);

        return userDao.save(existingUser);
    }

    @Override
    public void softDeleteUserById(Long id) {
        User user = userDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessages.USER_NOT_FOUND_ID, id)));

        user.setIsDeleted(true);
        userDao.save(user);
    }

    @Override
    public List<User> getAllActiveUsers() {
        return userDao.findByIsDeletedFalseOrderByUpdatedAtDesc();
    }

    /**
     * Login service logic handling
     *
     * @param email
     * @param password
     * @return
     */
    public User login(String email, String password) {
        return userDao.findByEmail(email)
                .map(user -> {
                    if (passwordEncoder.matches(password, user.getPassword())) {
                        return user;
                    } else {
                        throw new ResourceNotFoundException(ErrorMessages.INVALID_PASSWORD);
                    }
                })
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.USER_NOT_FOUND_EMAIL));
    }

    @Override
    public User getUserByEmail(String email) {
        return userDao.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessages.USER_NOT_FOUND_EMAIL, email)));
    }

    @Override
    public String changePassword(String email, String oldPassword, String newPassword) {
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException(ErrorMessages.NEW_PASSWORD_LENGTH);
        }

        log.info("Attempting to change password for user with email: {}", email);

        // Find the user by email address.
        User user = userDao.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("User not found with email: {}", email);
                    return new ResourceNotFoundException(String.format(ErrorMessages.USER_NOT_FOUND_EMAIL, email));
                });

        String currentPasswordHash = user.getPassword();

        // Verify that the provided old password matches the user's current password.
        if (!passwordEncoder.matches(oldPassword, currentPasswordHash)) {
            log.warn("Password verification failed for user with email: {}", email);
            throw new BadCredentialsException(ErrorMessages.OLD_PASSWORD_INCORRECT);
        }

        // Prevent the user from setting the new password to the same as the old one.
        if (passwordEncoder.matches(newPassword, currentPasswordHash)) {
            log.warn("User with email: {} attempted to change password to the same old password.", email);
            throw new IllegalArgumentException(ErrorMessages.PASSWORD_SAME_AS_OLD);
        }

        user.setOldPassword(currentPasswordHash);

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setLastPasswordChangeOn(LocalDateTime.now());

        // Save the updated user entity to the database.
        userDao.save(user);

        log.info("Successfully changed password for user with email: {}", email);
        return "Password changed successfully.";
    }

    /**
     * Change user password
     *
     * @param email
     * @param newPassword
     */
    @Override
    public void changePasswordForgot(String email, String newPassword) {
        User user = userDao.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessages.USER_NOT_FOUND_EMAIL, email)));

        // Encode the new password
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);

        userDao.save(user);
    }
}
