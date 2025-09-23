package com.tem.be.api.service;

import com.tem.be.api.dao.UserDao;
import com.tem.be.api.exception.CacheServiceException;
import com.tem.be.api.exception.EmailServiceException;
import com.tem.be.api.exception.ResourceNotFoundException;
import com.tem.be.api.model.User;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Log4j2
@Service
@Transactional
public class PasswordResetServiceImpl implements PasswordResetService {

    private final UserDao userDao;
    private final EmailService emailService;
    private final CacheManager cacheManager;
    private final PasswordEncoder passwordEncoder;

    private static final String TEMP_PASSWORD_CACHE = "tempPasswords";
    private static final int TEMP_PASSWORD_EXPIRY_MINUTES = 30;

    @Autowired
    public PasswordResetServiceImpl(UserDao userDao, EmailService emailService,
                                    CacheManager cacheManager, PasswordEncoder passwordEncoder) {
        this.userDao = userDao;
        this.emailService = emailService;
        this.cacheManager = cacheManager;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String requestPasswordReset(String email) {
        log.info("PasswordResetServiceImpl.requestPasswordReset() >> Entered for email: {}", email);

        // Check if user exists with the given email
        User user = userDao.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        // Clear any existing temporary password for this email
        clearTemporaryPassword(email);

        // Generate temporary password
        String temporaryPassword = generateTemporaryPassword();

        // Store temporary password in cache with expiration
        Cache cache = cacheManager.getCache(TEMP_PASSWORD_CACHE);
        if (cache != null) {
            cache.put(email, temporaryPassword);
            log.info("Temporary password stored in cache for email: {}", email);
        } else {
            log.error("Cache not available for storing temporary password");
            throw new CacheServiceException("Cache service unavailable");
        }

        try {
            // Send email with temporary password
            String subject = "Password Reset Request - COS Expense Management";
            String emailBody = buildPasswordResetEmailBody(user.getUserName(), temporaryPassword);
            emailService.sendEmail(email, subject, emailBody);
            log.info("Password reset email sent successfully to: {}", email);
        } catch (Exception e) {
            // If email sending fails, clear the cached password
            clearTemporaryPassword(email);
            log.error("Failed to send password reset email to: {}", email, e);
            throw new EmailServiceException("Failed to send password reset email", e);
        }

        log.info("PasswordResetServiceImpl.requestPasswordReset() >> Exited successfully");
        return "Password reset email sent successfully";
    }

    @Override
    public boolean validateTemporaryPassword(String email, String tempPassword) {
        log.info("PasswordResetServiceImpl.validateTemporaryPassword() >> Entered for email: {}", email);

        if (email == null || email.trim().isEmpty() || tempPassword == null || tempPassword.trim().isEmpty()) {
            log.warn("Invalid input parameters for temporary password validation");
            return false;
        }

        Cache cache = cacheManager.getCache(TEMP_PASSWORD_CACHE);
        if (cache != null) {
            Cache.ValueWrapper wrapper = cache.get(email.trim().toLowerCase());
            if (wrapper != null) {
                String cachedPassword = (String) wrapper.get();
                boolean isValid = tempPassword.equals(cachedPassword);
                log.info("Temporary password validation result for email {}: {}", email, isValid);
                return isValid;
            }
        }

        log.warn("No temporary password found in cache for email: {}", email);
        return false;
    }

    @Override
    public void clearTemporaryPassword(String email) {
        log.info("PasswordResetServiceImpl.clearTemporaryPassword() >> Entered for email: {}", email);

        if (email == null || email.trim().isEmpty()) {
            log.warn("Cannot clear temporary password for null/empty email");
            return;
        }

        Cache cache = cacheManager.getCache(TEMP_PASSWORD_CACHE);
        if (cache != null) {
            cache.evict(email.trim().toLowerCase());
            log.info("Temporary password cleared from cache for email: {}", email);
        } else {
            log.warn("Cache not available for clearing temporary password");
        }

        log.info("PasswordResetServiceImpl.clearTemporaryPassword() >> Exited");
    }

    /**
     * Validates that the new password is different from the temporary password
     *
     * @param tempPassword Temporary password
     * @param newPassword  New password
     * @return true if passwords are different, false if same
     */
    public boolean validateNewPasswordDifferent(String tempPassword, String newPassword) {
        if (tempPassword == null || newPassword == null) {
            return false;
        }
        return !tempPassword.equals(newPassword);
    }

    /**
     * Validates that the new password is not the same as the current password
     *
     * @param email       User's email
     * @param newPassword New password to check
     * @return true if different from current password
     */
    public boolean validateNotSameAsCurrentPassword(String email, String newPassword) {
        try {
            User user = userDao.findByEmailAndIsDeletedFalse(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

            // Check if new password is same as current password
            return !passwordEncoder.matches(newPassword, user.getPassword());
        } catch (Exception e) {
            log.error("Error validating current password for email: {}", email, e);
            return true; // Allow password change if validation fails
        }
    }

    /**
     * Generate secure temporary password
     *
     * @return String
     */
    private String generateTemporaryPassword() {
        log.info("PasswordResetServiceImpl.generateTemporaryPassword() >> Entered");

        // Use a mix of uppercase, lowercase, numbers, and special characters
        String upperCase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowerCase = "abcdefghijklmnopqrstuvwxyz";
        String numbers = "0123456789";
        String specialChars = "!@#$%&*";

        String allChars = upperCase + lowerCase + numbers + specialChars;
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();

        // Ensure at least one character from each category
        password.append(upperCase.charAt(random.nextInt(upperCase.length())));
        password.append(lowerCase.charAt(random.nextInt(lowerCase.length())));
        password.append(numbers.charAt(random.nextInt(numbers.length())));
        password.append(specialChars.charAt(random.nextInt(specialChars.length())));

        // Fill remaining positions with random characters
        for (int i = 4; i < 12; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }

        // Shuffle the password to randomize positions
        char[] passwordArray = password.toString().toCharArray();
        for (int i = passwordArray.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = passwordArray[i];
            passwordArray[i] = passwordArray[j];
            passwordArray[j] = temp;
        }

        String generatedPassword = new String(passwordArray);
        log.info("PasswordResetServiceImpl.generateTemporaryPassword() >> Exited");
        return generatedPassword;
    }

    /**
     * Build password reset email body with HTML formatting
     *
     * @param userName          User's name
     * @param temporaryPassword Generated temporary password
     * @return String HTML email body
     */
    private String buildPasswordResetEmailBody(String userName, String temporaryPassword) {
        log.info("PasswordResetServiceImpl.buildPasswordResetEmailBody() >> Entered");

        String emailBody = String.format("""
                        Dear %s,
                        
                        You have requested a password reset for your COS Expense Management account.
                        
                        Please use the following temporary password to log in:
                        
                        Temporary Password: %s
                        
                        IMPORTANT INSTRUCTIONS:
                        1. This temporary password will expire in %d minutes
                        2. Please log in and change your password immediately for security purposes
                        3. Your new password must be different from this temporary password
                        4. Choose a strong password with at least 6 characters
                        
                        If you did not request this password reset, please ignore this email and contact our support team immediately.
                        
                        For security reasons, do not share this temporary password with anyone.
                        
                        Best regards,
                        COS Expense Management Team
                        
                        ---
                        This is an automated email. Please do not reply to this message.
                        """,
                userName, temporaryPassword, TEMP_PASSWORD_EXPIRY_MINUTES
        );

        log.info("PasswordResetServiceImpl.buildPasswordResetEmailBody() >> Exited");
        return emailBody;
    }

    /**
     * Build HTML password reset email body for better formatting
     *
     * @param userName          User's name
     * @param temporaryPassword Generated temporary password
     * @return String HTML email body
     */
    private String buildHtmlPasswordResetEmailBody(String userName, String temporaryPassword) {
        log.info("PasswordResetServiceImpl.buildHtmlPasswordResetEmailBody() >> Entered");

        String htmlBody = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        .container { font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; }
                        .header { background-color: #f8f9fa; padding: 20px; text-align: center; }
                        .content { padding: 20px; }
                        .temp-password { background-color: #e9ecef; padding: 15px; border-left: 4px solid #007bff; margin: 15px 0; }
                        .important { background-color: #fff3cd; padding: 15px; border: 1px solid #ffeaa7; border-radius: 5px; margin: 15px 0; }
                        .footer { background-color: #f8f9fa; padding: 15px; text-align: center; font-size: 12px; color: #6c757d; }
                        .warning { color: #721c24; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h2>Password Reset Request</h2>
                            <p>COS Expense Management System</p>
                        </div>
                        <div class="content">
                            <p>Dear <strong>%s</strong>,</p>
                            <p>You have requested a password reset for your COS Expense Management account.</p>
                
                            <div class="temp-password">
                                <p><strong>Your Temporary Password:</strong></p>
                                <h3 style="color: #007bff; font-family: monospace;">%s</h3>
                            </div>
                
                            <div class="important">
                                <h4>⚠️ Important Instructions:</h4>
                                <ul>
                                    <li>This temporary password will expire in <strong>%d minutes</strong></li>
                                    <li>Please log in and change your password immediately</li>
                                    <li>Your new password must be different from this temporary password</li>
                                    <li>Choose a strong password with at least 6 characters</li>
                                </ul>
                            </div>
                
                            <p class="warning">
                                <strong>Security Notice:</strong> If you did not request this password reset, 
                                please ignore this email and contact our support team immediately.
                            </p>
                
                            <p>For security reasons, do not share this temporary password with anyone.</p>
                
                            <p>Best regards,<br>
                            <strong>COS Expense Management Team</strong></p>
                        </div>
                        <div class="footer">
                            <p>This is an automated email. Please do not reply to this message.</p>
                        </div>
                    </div>
                </body>
                </html>
                """, userName, temporaryPassword, TEMP_PASSWORD_EXPIRY_MINUTES);

        log.info("PasswordResetServiceImpl.buildHtmlPasswordResetEmailBody() >> Exited");
        return htmlBody;
    }

}