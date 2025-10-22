package com.tem.be.api.service;

import com.tem.be.api.dao.UserDao;
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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    private static final int TEMP_PASSWORD_LENGTH = 6;

    @Autowired
    public PasswordResetServiceImpl(UserDao userDao, EmailService emailService,
                                    CacheManager cacheManager, PasswordEncoder passwordEncoder) {
        this.userDao = userDao;
        this.emailService = emailService;
        this.cacheManager = cacheManager;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void requestPasswordReset(String email) {
        log.info("Attempting to request password reset for email: {}", email);
        User user = userDao.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        clearTemporaryPassword(email);

        String temporaryPassword = generateTemporaryPassword();
        Cache cache = cacheManager.getCache(TEMP_PASSWORD_CACHE);
        if (cache == null) {
            throw new IllegalStateException("Cache '" + TEMP_PASSWORD_CACHE + "' not found.");
        }
        cache.put(email, temporaryPassword);

        String subject = "Password Reset Request - COS Expense Management";
        String emailBody = buildHtmlPasswordResetEmailBody(user.getUserName(), temporaryPassword);
        emailService.sendEmail(email, subject, emailBody, true);
        log.info("Password reset email sent successfully to: {}", email);
    }

    @Override
    public void resetPassword(String email, String tempPassword, String newPassword) {
        if (!validateTemporaryPassword(email, tempPassword)) {
            throw new IllegalArgumentException("Invalid or expired temporary password.");
        }
        if (tempPassword.equals(newPassword)) {
            throw new IllegalArgumentException("New password must be different from the temporary password.");
        }

        User user = userDao.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("New password cannot be the same as your current password.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userDao.save(user);

        clearTemporaryPassword(email);
        log.info("Password for user {} has been successfully reset.", email);
    }

    @Override
    public boolean validateTemporaryPassword(String email, String tempPassword) {
        if (email == null || tempPassword == null) return false;
        Cache cache = cacheManager.getCache(TEMP_PASSWORD_CACHE);
        if (cache == null) return false;
        Cache.ValueWrapper wrapper = cache.get(email);
        return wrapper != null && tempPassword.equals(wrapper.get());
    }

    @Override
    public void clearTemporaryPassword(String email) {
        if (email == null) return;
        Cache cache = cacheManager.getCache(TEMP_PASSWORD_CACHE);
        if (cache != null) {
            cache.evict(email);
            log.info("Temporary password cleared from cache for email: {}", email);
        }
    }

    private String generateTemporaryPassword() {
        String upperCase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowerCase = "abcdefghijklmnopqrstuvwxyz";
        String numbers = "0123456789";
        String specialChars = "!@#$%&*";
        String allChars = upperCase + lowerCase + numbers + specialChars;
        SecureRandom random = new SecureRandom();

        return IntStream.range(0, TEMP_PASSWORD_LENGTH)
                .mapToObj(i -> String.valueOf(allChars.charAt(random.nextInt(allChars.length()))))
                .collect(Collectors.joining());
    }

    private String buildHtmlPasswordResetEmailBody(String userName, String temporaryPassword) {
        int expiryMinutes = TEMP_PASSWORD_EXPIRY_MINUTES;

        return "<div style=\"font-family: Arial, 'Helvetica Neue', Helvetica, sans-serif; background-color: #f4f4f4; padding: 0; margin: 0; margin-top:4px;\">" +
                "<div style=\"background-color: #ffffff; padding: 30px; margin-top: 30px; box-shadow: 0px 0px 10px rgba(0,0,0,0.1); max-width: 600px; margin-left: auto; margin-right: auto; border-radius: 8px;\">" +
                "<div style=\"text-align: center; margin-bottom: 20px;\">" +
                "<h2 style=\"color: #333333; margin-top: 10px; font-size: 24px;\">Password Reset Request</h2>" +
                "</div>" +
                "<h3 style=\"color:#333333; font-size: 18px;\">Hello, " + userName + " </h3>" +
                "<p style=\"color:#555555; line-height: 1.6;\">You recently requested to reset your password for your <strong>COS Expense Management</strong> account. Please use the temporary password below to log in.</p>" +
                "<p style=\"color:#555555; text-align:center; margin-top: 25px;\">Your temporary password is:</p>" +
                "<div style=\"font-size: 22px; font-weight: bold; color: #ffffff; background-color: #007bff; border: none; padding: 15px 30px; border-radius: 8px; text-align:center; width: fit-content; margin: 10px auto; letter-spacing: 4px;\">" +
                temporaryPassword +
                "</div>" +
                "<div style=\"text-align:center; margin-top:20px; color:#555555;\">" +
                "This temporary password is valid for <strong>" + expiryMinutes + " minutes</strong>. Please change your password immediately after logging in." +
                "</div>" +
                "<div style=\"color:#6c757d; font-size:13px; text-align:center; margin-top:30px; border-top: 1px solid #eeeeee; padding-top: 20px;\">" +
                "<strong>Note:</strong> If you did not request a password reset, please ignore this email or contact support.<br>" +
                "<p style=\"margin-top: 20px;\">Thanks,<br><a href=\"https://www.akrti-tailoring.com/terms\" style=\"color:#007bff; text-decoration:none;\">The COS Team</a></p>" +
                "</div>" +
                "</div>" +
                "</div>";

    }

}