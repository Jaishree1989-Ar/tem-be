package com.tem.be.api.controller;

import com.tem.be.api.dto.ForgotPasswordRequest;
import com.tem.be.api.dto.ResetPasswordRequest;
import com.tem.be.api.service.PasswordResetService;
import com.tem.be.api.utils.ApiResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Log4j2
@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/auth")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @Autowired
    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    /**
     * Handles the request to initiate a password reset.
     * @param request The request containing the user's email.
     * @return A response entity indicating the result of the operation.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("PasswordResetController.forgotPassword() >> Entered for email: {}", request.getEmail());
        try {
            passwordResetService.requestPasswordReset(request.getEmail());
            ApiResponse<String> response = new ApiResponse<>(HttpStatus.OK.value(),
                    "Password reset email sent successfully. Please check your email.",
                    null);
            log.info("PasswordResetController.forgotPassword() >> Exited successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error during password reset process for email: {}", request.getEmail(), e);
            return new ResponseEntity<>(new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to send reset link. Please try again.", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Handles the request to reset the user's password using a temporary password.
     * @param request The request containing the email, temporary password, and new password.
     * @return A response entity indicating the result of the operation.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("PasswordResetController.resetPassword() >> Entered for email: {}", request.getEmail());
        try {
            passwordResetService.resetPassword(
                    request.getEmail(),
                    request.getTemporaryPassword(),
                    request.getNewPassword()
            );
            ApiResponse<String> response = new ApiResponse<>(HttpStatus.OK.value(),
                    "Password reset successfully. You can now login with your new password.",
                    null);
            log.info("PasswordResetController.resetPassword() >> Password reset successful for email: {}", request.getEmail());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid password reset request for email: {}: {}", request.getEmail(), e.getMessage());
            return new ResponseEntity<>(new ApiResponse<>(HttpStatus.BAD_REQUEST.value(),
                    e.getMessage(), null), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Error during password reset for email: {}", request.getEmail(), e);
            return new ResponseEntity<>(new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to reset password. Please try again.", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}