package com.tem.be.api.controller;

import com.tem.be.api.dto.ForgotPasswordRequest;
import com.tem.be.api.dto.ResetPasswordRequest;
import com.tem.be.api.service.PasswordResetService;
import com.tem.be.api.service.UserService;
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
    private final UserService userService;

    @Autowired
    public PasswordResetController(PasswordResetService passwordResetService, UserService userService) {
        this.passwordResetService = passwordResetService;
        this.userService = userService;
    }

    /**
     * Forgot password method
     * Inputs : email
     * @param request
     * @return
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("PasswordResetController.forgotPassword() >> Entered for email: {}", request.getEmail());

        try {
            String result = passwordResetService.requestPasswordReset(request.getEmail());
            ApiResponse<String> response = new ApiResponse<>(HttpStatus.OK.value(),
                    "Password reset email sent successfully. Please check your email.",
                    result);

            log.info("PasswordResetController.forgotPassword() >> Exited successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error during password reset process for email: {}", request.getEmail(), e);
            return new ResponseEntity<>(new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to send reset link. Please try again.", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Reset password method
     * Validates temporary password and updates user password
     * @param request
     * @return
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("PasswordResetController.resetPassword() >> Entered for email: {}", request.getEmail());

        try {
            // Validate temporary password
            boolean isValidTempPassword = passwordResetService.validateTemporaryPassword(
                    request.getEmail(),
                    request.getTemporaryPassword()
            );

            if (!isValidTempPassword) {
                log.warn("Invalid temporary password provided for email: {}", request.getEmail());
                return new ResponseEntity<>(new ApiResponse<>(HttpStatus.BAD_REQUEST.value(),
                        "Invalid or expired temporary password", null), HttpStatus.BAD_REQUEST);
            }

            // Check if new password is different from temporary password
            if (request.getNewPassword().equals(request.getTemporaryPassword())) {
                log.warn("New password same as temporary password for email: {}", request.getEmail());
                return new ResponseEntity<>(new ApiResponse<>(HttpStatus.BAD_REQUEST.value(),
                        "New password must be different from temporary password", null), HttpStatus.BAD_REQUEST);
            }

            // Change user password
            userService.changePasswordForgot(request.getEmail(), request.getNewPassword());

            // Clear temporary password from cache
            passwordResetService.clearTemporaryPassword(request.getEmail());

            ApiResponse<String> response = new ApiResponse<>(HttpStatus.OK.value(),
                    "Password reset successfully. You can now login with your new password.",
                    "Password updated successfully");

            log.info("PasswordResetController.resetPassword() >> Password reset successful for email: {}", request.getEmail());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error during password reset for email: {}", request.getEmail(), e);
            return new ResponseEntity<>(new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to reset password. Please try again.", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Validate temporary password endpoint (optional - for frontend validation)
     * @param email
     * @param tempPassword
     * @return
     */
    @GetMapping("/validate-temp-password")
    public ResponseEntity<ApiResponse<Boolean>> validateTempPassword(
            @RequestParam String email,
            @RequestParam String tempPassword) {

        log.info("PasswordResetController.validateTempPassword() >> Entered for email: {}", email);

        try {
            boolean isValid = passwordResetService.validateTemporaryPassword(email, tempPassword);

            ApiResponse<Boolean> response = new ApiResponse<>(HttpStatus.OK.value(),
                    isValid ? "Valid temporary password" : "Invalid temporary password",
                    isValid);

            log.info("PasswordResetController.validateTempPassword() >> Exited with result: {}", isValid);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error validating temporary password for email: {}", email, e);
            return new ResponseEntity<>(new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Error validating temporary password", false), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}