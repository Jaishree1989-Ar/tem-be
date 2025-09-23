package com.tem.be.api.controller;

import com.tem.be.api.dto.UserDTO;
import com.tem.be.api.model.User;
import com.tem.be.api.service.UserService;
import com.tem.be.api.utils.ApiResponse;
import com.tem.be.api.utils.RestConstants;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for managing user related operations.
 */
@Log4j2
@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/user")
public class UserController {
    private final UserService userService;

    /**
     * @param userService the service handling user operations
     */
    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Creates a new user.
     *
     * @param userDTO user details
     */
    @PostMapping(value = "/createUser", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<User> createUser(@RequestBody UserDTO userDTO) {
        log.info("UserController.createUser() >> Entered");
        User userDetails = userService.createUser(userDTO);
        log.info("UserController.createUser() >> Exited");
        return new ResponseEntity<>(userDetails, HttpStatus.CREATED);
    }

    /**
     * Updates an existing user by ID.
     *
     * @param id user ID
     * @param userDTO updated user details
     */
    @PutMapping(value = "/updateUserById/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<User> updateUserById(@PathVariable("id") Long id, @RequestBody UserDTO userDTO) {
        log.info("UserController.updateUserById() >> Entered");
        User updatedUser = userService.updateUserById(id, userDTO);
        log.info("UserController.updateUserById() >> Exited");
        return new ResponseEntity<>(updatedUser, HttpStatus.OK);
    }

    /**
     * Soft deletes a user by ID.
     *
     * @param id user ID
     */
    @DeleteMapping("/deleteUserById/{id}")
        public ResponseEntity<Map<String, String>> softDeleteUser(@PathVariable("id") Long id) {
            log.info("UserController.softDeleteUser() >> Entered");
            userService.softDeleteUserById(id);
            log.info("UserController.softDeleteUser() >> Exited");
            Map<String, String> response = new HashMap<>();
            response.put("message", "User deleted successfully!");
            return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Returns all active users.
     */
    @GetMapping(value = "/getAllUsers", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        log.info("UserController.getAllUsers() >> Entered");
        List<User> users = userService.getAllActiveUsers();
        ApiResponse<List<User>> response = new ApiResponse<>(HttpStatus.OK.value(), "Successfully fetched users", users);
        log.info("UserController.getAllUsers() >> Exited");
        return ResponseEntity.ok(response);
    }

    /**
     * Returns a user by email.
     *
     * @param email user email
     */
    @GetMapping(value = "/getUserByEmail/{email}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<User>> getUserByEmail(@PathVariable("email") String email) {
        log.info("UserController.getUserByEmail() >> Entered");
        User user = userService.getUserByEmail(email);
        ApiResponse<User> response = new ApiResponse<>(HttpStatus.OK.value(), RestConstants.SUCCESS_STRING, user);
        log.info("UserController.getUserByEmail() >> Exited");
        return ResponseEntity.ok(response);
    }

}
