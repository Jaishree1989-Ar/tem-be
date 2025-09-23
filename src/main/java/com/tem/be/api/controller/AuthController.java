package com.tem.be.api.controller;

import com.tem.be.api.dto.LoginRequest;
import com.tem.be.api.dto.ChangePasswordRequest;
import com.tem.be.api.model.User;
import com.tem.be.api.service.UserService;
import com.tem.be.api.utils.ApiResponse;
import com.tem.be.api.utils.RestConstants;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * REST controller for managing auth operations.
 */
@Log4j2
@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    /**
     * @param userService the service handling auth operations
     */
    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Login method
     * Inputs : email and password
     *
     * @param request
     * @return
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<User>> login(@RequestBody LoginRequest request) {
        log.info("AuthController.login() >> Exited");
        User user = userService.login(request.getEmail(), request.getPassword());
        ApiResponse<User> response = new ApiResponse<>(HttpStatus.OK.value(), RestConstants.SUCCESS_STRING, user);
        log.info("AuthController.login() >> Exited");
        return ResponseEntity.ok(response);
    }


    /**
     * Handles the request to change a user's password.
     *
     * @param request The request body containing the email, old password, and new password.
     * @return A ResponseEntity indicating the result of the operation.
     */
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changeStaffPassword(@Valid @RequestBody ChangePasswordRequest request) {
        log.info("Attempting to change password for user with email: {}", request.getEmail());
        String successMessage = userService.changePassword(
                request.getEmail(),
                request.getOldPassword(),
                request.getNewPassword()
        );

        // If the service method completes without an exception, it was successful.
        ApiResponse<String> response = new ApiResponse<>(HttpStatus.OK.value(), successMessage, null);
        log.info("Successfully changed password for user with email: {}", request.getEmail());

        return ResponseEntity.ok(response);
    }
}
